package me.despical.classicduels.arena;

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.api.StatsStorage;
import me.despical.classicduels.api.events.game.CDGameStartEvent;
import me.despical.classicduels.api.events.game.CDGameStateChangeEvent;
import me.despical.classicduels.arena.manager.ScoreboardManager;
import me.despical.classicduels.arena.options.ArenaOption;
import me.despical.classicduels.handlers.rewards.Reward;
import me.despical.classicduels.kits.KitRegistry;
import me.despical.classicduels.user.User;
import me.despical.classicduels.utils.Debugger;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.miscellaneous.AttributeUtils;
import me.despical.commonsbox.miscellaneous.MiscUtils;
import me.despical.commonsbox.serializer.InventorySerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class Arena extends BukkitRunnable {

	private final Main plugin = JavaPlugin.getPlugin(Main.class);
	private final String id;

	private final List<Player> players = new ArrayList<>();

	private final Map<ArenaOption, Integer> arenaOptions = new EnumMap<>(ArenaOption.class);
	private final Map<GameLocation, Location> gameLocations = new EnumMap<>(GameLocation.class);

	private ArenaState arenaState = ArenaState.INACTIVE;
	private BossBar gameBar;
	private final ScoreboardManager scoreboardManager;
	private String mapName = "";
	private boolean ready;

	public Arena(String id) {
		this.id = id;

		for (ArenaOption option : ArenaOption.values()) {
			arenaOptions.put(option, option.getDefaultValue());
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
			gameBar = Bukkit.createBossBar(plugin.getChatManager().colorMessage("Bossbar.Main-Title"), BarColor.BLUE, BarStyle.SOLID);
		}

		scoreboardManager = new ScoreboardManager(this);
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public void run() {
		if (players.isEmpty() && arenaState == ArenaState.WAITING_FOR_PLAYERS) {
			return;
		}

		Debugger.performance("ArenaTask", "[PerformanceMonitor] [{0}] Running game task", getId());
		long start = System.currentTimeMillis();

		switch (getArenaState()) {
			case WAITING_FOR_PLAYERS:
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					plugin.getServer().setWhitelist(false);
				}

				if (getPlayers().size() < 2) {
					if (getTimer() <= 0) {
						setTimer(45);
						broadcast(plugin.getChatManager().formatMessage(this, plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Waiting-For-Players"), 2));
						break;
					}
				} else {
					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
						gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Waiting-For-Players"));
					}

					broadcast(plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Enough-Players-To-Start"));
					setArenaState(ArenaState.STARTING);
					setTimer(plugin.getConfig().getInt("Starting-Waiting-Time", 5));
					showPlayers();
				}

				setTimer(getTimer() - 1);
				break;
			case STARTING:
				if (players.size() == 2 && getTimer() >= plugin.getConfig().getInt("Starting-Waiting-Time", 5)) {
					setTimer(plugin.getConfig().getInt("Starting-Waiting-Time", 5));
					broadcast(plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Start-In").replace("%time%", String.valueOf(getTimer())));
				}

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Starting-In").replace("%time%", String.valueOf(getTimer())));
					gameBar.setProgress(getTimer() / plugin.getConfig().getDouble("Starting-Waiting-Time", 5));
				}

				for (Player player : getPlayers()) {
					player.setExp((float) (getTimer() / plugin.getConfig().getDouble("Starting-Waiting-Time", 5)));
					player.setLevel(getTimer());
				}

				if (getPlayers().size() < 2) {
					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
						gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Waiting-For-Players"));
						gameBar.setProgress(1.0);
					}

					broadcast(plugin.getChatManager().formatMessage(this, plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Waiting-For-Players"), 2));

					setArenaState(ArenaState.WAITING_FOR_PLAYERS);
					Bukkit.getPluginManager().callEvent(new CDGameStartEvent(this));
					setTimer(15);

					for (Player player : getPlayers()) {
						player.setExp(1);
						player.setLevel(0);
					}

					break;
				}

				if (getTimer() == 0) {
					CDGameStartEvent gameStartEvent = new CDGameStartEvent(this);

					Bukkit.getPluginManager().callEvent(gameStartEvent);
					setArenaState(ArenaState.IN_GAME);

					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
						gameBar.setProgress(1.0);
					}

					setTimer(5);

					if (players.isEmpty()) {
						break;
					}

					teleportAllToStartLocation();

					for (Player player : getPlayers()) {
						AttributeUtils.setAttackCooldown(player, plugin.getConfig().getDouble("Hit-Cooldown-Delay", 4));
						player.getInventory().clear();
						KitRegistry.getBaseKit().giveItems(player);
						player.setGameMode(GameMode.SURVIVAL);

						ArenaUtils.hidePlayersOutsideTheGame(player, this);

						setTimer(plugin.getConfig().getInt("Classic-Gameplay-Time", 900));

						plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.GAMES_PLAYED, 1);

						for (String msg : plugin.getChatManager().getStringList("In-Game.Messages.Lobby-Messages.Game-Started")) {
							MiscUtils.sendCenteredMessage(player, plugin.getChatManager().colorRawMessage(msg).replace("%opponent%", scoreboardManager.getOpponent(plugin.getUserManager().getUser(player))));
						}

						player.updateInventory();
					}
				}

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.In-Game-Info"));
				}

				if (getTimer() <= 5) {
					broadcast(plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Start-In").replace("seconds", getTimer() == 1 ? "second" : "seconds").replace("%time%", String.valueOf(getTimer())));
				}

				setTimer(getTimer() - 1);
				break;
			case IN_GAME:
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					plugin.getServer().setWhitelist(2 <= players.size());
				}

				if (getTimer() <= 0) {
					ArenaManager.stopGame(false, this);
					return;
				}

				if (getPlayersLeft().size() < 2) {
					ArenaManager.stopGame(false, this);
					return;
				}

				setTimer(getTimer() - 1);
				break;
			case ENDING:
				scoreboardManager.stopAllScoreboards();

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					plugin.getServer().setWhitelist(false);
				}

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Game-Ended"));
				}

				List<Player> playersToQuit = new ArrayList<>(getPlayers());

				for (Player player : playersToQuit) {
					plugin.getUserManager().getUser(player).removeScoreboard();
					player.setGameMode(GameMode.SURVIVAL);

					AttributeUtils.resetAttackCooldown(player);

					for (Player playerToShow : Bukkit.getOnlinePlayers()) {
						player.showPlayer(plugin, playerToShow);

						if (!ArenaRegistry.isInArena(playerToShow)) {
							playerToShow.showPlayer(plugin, player);
						}
					}

					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
					player.setWalkSpeed(0.2f);
					player.setFlying(false);
					player.setAllowFlight(false);
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.setFireTicks(0);
					player.setFoodLevel(20);

					doBarAction(BarAction.REMOVE, player);
				}

				teleportAllToEndLocation();

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					players.forEach(player -> InventorySerializer.loadInventory(plugin, player));
				}

				broadcast(plugin.getChatManager().colorMessage("Commands.Teleported-To-The-Lobby"));

				for (User user : plugin.getUserManager().getUsers(this)) {
					user.setSpectator(false);
					user.getPlayer().setCollidable(true);
				}

				plugin.getRewardsFactory().performReward(this, Reward.RewardType.END_GAME);

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					if (ConfigUtils.getConfig(plugin, "bungee").getBoolean("Shutdown-When-Game-Ends")) {
						plugin.getServer().shutdown();
					}
				}

				setArenaState(ArenaState.RESTARTING);
				break;
			case RESTARTING:
				players.clear();
				setArenaState(ArenaState.WAITING_FOR_PLAYERS);

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					ArenaRegistry.shuffleBungeeArena();
					Bukkit.getOnlinePlayers().forEach(player -> ArenaManager.joinAttempt(player, ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena())));
				}

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Waiting-For-Players"));
				}

				break;
			default:
				break;
		}

		Debugger.performance("ArenaTask", "[PerformanceMonitor] [{0}] Game task finished took {1} ms", getId(), System.currentTimeMillis() - start);
	}

	public ScoreboardManager getScoreboardManager() {
		return scoreboardManager;
	}

	/**
	 * Get arena identifier used to get arenas by string.
	 *
	 * @return arena name
	 * @see ArenaRegistry#getArena(String)
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get arena map name.
	 *
	 * @return arena map name, it's not arena id
	 * @see #getId()
	 */
	public String getMapName() {
		return mapName;
	}

	/**
	 * Set arena map name.
	 *
	 * @param mapname new map name, it's not arena id
	 */
	public void setMapName(String mapname) {
		this.mapName = mapname;
	}

	/**
	 * Get timer of arena.
	 *
	 * @return timer of lobby time / time to next wave
	 */
	public int getTimer() {
		return getOption(ArenaOption.TIMER);
	}

	/**
	 * Modify game timer.
	 *
	 * @param timer timer of lobby / time to next wave
	 */
	public void setTimer(int timer) {
		setOptionValue(ArenaOption.TIMER, timer);
	}

	/**
	 * Return game state of arena.
	 *
	 * @return game state of arena
	 * @see ArenaState
	 */
	public ArenaState getArenaState() {
		return arenaState;
	}

	/**
	 * Set game state of arena.
	 *
	 * @param arenaState new game state of arena
	 * @see ArenaState
	 */
	public void setArenaState(ArenaState arenaState) {
		this.arenaState = arenaState;
		CDGameStateChangeEvent gameStateChangeEvent = new CDGameStateChangeEvent(this, getArenaState());
		Bukkit.getPluginManager().callEvent(gameStateChangeEvent);
	}

	/**
	 * Get all players in arena.
	 *
	 * @return set of players in arena
	 */
	public List<Player> getPlayers() {
		return players;
	}

	public void teleportToLobby(Player player) {
		player.setFoodLevel(20);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		player.setWalkSpeed(0.2f);

		Location location = players.size() == 1 ? getFirstPlayerLocation() : getSecondPlayerLocation();

		if (location == null) {
			System.out.print("Lobby location isn't initialized for arena " + id);
			return;
		}

		player.teleport(location);
	}

	public void teleportAllToStartLocation() {
		getPlayersLeft().get(0).teleport(getFirstPlayerLocation());
		getPlayersLeft().get(1).teleport(getSecondPlayerLocation());
	}

	/**
	 * Executes boss bar action for arena
	 *
	 * @param action add or remove a player from boss bar
	 * @param p player
	 */
	public void doBarAction(BarAction action, Player p) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
			return;
		}

		switch (action) {
			case ADD:
				gameBar.addPlayer(p);
				break;
			case REMOVE:
				gameBar.removePlayer(p);
				break;
			default:
				break;
		}
	}

	public Location getFirstPlayerLocation() {
		return gameLocations.get(GameLocation.FIRST_PLAYER);
	}

	public void setFirstPlayerLocation(Location location) {
		gameLocations.put(GameLocation.FIRST_PLAYER, location);
	}

	public Location getSecondPlayerLocation() {
		return gameLocations.get(GameLocation.SECOND_PLAYER);
	}

	public void setSecondPlayerLocation(Location location) {
		gameLocations.put(GameLocation.SECOND_PLAYER, location);
	}

	public void teleportAllToEndLocation() {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && ConfigUtils.getConfig(plugin, "bungee").getBoolean("End-Location-Hub", true)) {
			players.forEach(plugin.getBungeeManager()::connectToHub);
			return;
		}

		Location location = getEndLocation();

		if (location == null) {
			location = getFirstPlayerLocation();
			System.out.print("End location for arena " + id + " isn't initialized!");
		}

		if (location != null) {
			for (Player player : getPlayers()) {
				player.teleport(location);
			}
		}
	}

	public void teleportToEndLocation(Player player) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && ConfigUtils.getConfig(plugin, "bungee").getBoolean("End-Location-Hub", true)) {
			plugin.getBungeeManager().connectToHub(player);
			return;
		}

		Location location = getEndLocation();

		if (location == null) {
			location = getFirstPlayerLocation();
			System.out.print("End location for arena " + id + " isn't initialized!");
		}

		if (location != null) {
			player.teleport(location);
		}
	}

	/**
	 * Get end location of arena.
	 *
	 * @return end location of arena
	 */
	public Location getEndLocation() {
		return gameLocations.get(GameLocation.END);
	}

	/**
	 * Set end location of arena.
	 *
	 * @param endLoc new end location of arena
	 */
	public void setEndLocation(Location endLoc) {
		gameLocations.put(GameLocation.END, endLoc);
	}

	public void broadcast(String msg) {
		players.forEach(p -> p.sendMessage(plugin.getChatManager().colorRawMessage(msg)));
	}

	public void start() {
		Debugger.debug("[{0}] Game instance started", id);
		runTaskTimer(plugin, 20L, 20L);
		setArenaState(ArenaState.RESTARTING);
	}

	void addPlayer(Player player) {
		players.add(player);
	}

	void removePlayer(Player player) {
		if (player != null) {
			players.remove(player);
		}
	}

	public List<Player> getPlayersLeft() {
		return plugin.getUserManager().getUsers(this).stream().filter(user -> !user.isSpectator()).map(User::getPlayer).collect(Collectors.toList());
	}

	void showPlayers() {
		for (Player player : players) {
			for (Player p : players) {
				player.showPlayer(plugin, p);
				p.showPlayer(plugin, player);
			}
		}
	}

	public int getOption(ArenaOption option) {
		return arenaOptions.get(option);
	}

	public void setOptionValue(ArenaOption option, int value) {
		arenaOptions.put(option, value);
	}

	public enum BarAction {
		ADD, REMOVE
	}

	public enum GameLocation {
		END, FIRST_PLAYER, SECOND_PLAYER
	}
}