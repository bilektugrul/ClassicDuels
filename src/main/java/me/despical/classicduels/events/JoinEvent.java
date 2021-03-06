package me.despical.classicduels.events;

import me.despical.classicduels.ConfigPreferences;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.utils.UpdateChecker;
import me.despical.commonsbox.serializer.InventorySerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class JoinEvent implements Listener {

	private final Main plugin;

	public JoinEvent(Main plugin) {
		this.plugin = plugin;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		plugin.getUserManager().loadStatistics(plugin.getUserManager().getUser(event.getPlayer()));

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena()).teleportToLobby(event.getPlayer());
			return;
		}

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (!ArenaRegistry.isInArena(player)) {
				continue;
			}

			player.hidePlayer(plugin, event.getPlayer());
			event.getPlayer().hidePlayer(plugin, player);
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, event.getPlayer());
		}
	}

	@EventHandler
	public void onJoinCheckVersion(final PlayerJoinEvent event) {
		if (!plugin.getConfig().getBoolean("Update-Notifier.Enabled", true) || !event.getPlayer().hasPermission("oitc.updatenotify")) {
			return;
		}

		Bukkit.getScheduler().runTaskLater(plugin, () -> UpdateChecker.init(plugin, 85356).requestUpdateCheck().whenComplete((result, exception) -> {
			if (!result.requiresUpdate()) {
				return;
			}

			if (result.getNewestVersion().contains("b")) {
				event.getPlayer().sendMessage(plugin.getChatManager().colorRawMessage("&3[ClassicDuels] &bFound a beta update: v" + result.getNewestVersion() + " Download:"));
			} else {
				event.getPlayer().sendMessage(plugin.getChatManager().colorRawMessage("&3[ClassicDuels] &bFound an update: v" + result.getNewestVersion() + " Download:"));
			}

			event.getPlayer().sendMessage(plugin.getChatManager().colorRawMessage("&3>> &bhttps://www.spigotmc.org/resources/classic-duels-1-9-1-16-3.85356/"));
		}), 25);
	}
}