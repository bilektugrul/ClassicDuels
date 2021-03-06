package me.despical.classicduels.commands.game;

import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.commands.SubCommand;
import me.despical.classicduels.handlers.ChatManager;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.serializer.LocationSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 12.10.2020
 */
public class CreateCommand extends SubCommand {

	public CreateCommand() {
		super("create");

		setPermission("cd.admin.create");
	}

	@Override
	public String getPossibleArguments() {
		return "<ID>";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public void execute(CommandSender sender, ChatManager chatManager, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(chatManager.getPrefix() + chatManager.colorMessage("Commands.Type-Arena-Name"));
			return;
		}

		Player player = (Player) sender;

		for (Arena arena : ArenaRegistry.getArenas()) {
			if (arena.getId().equalsIgnoreCase(args[0])) {
				player.sendMessage(chatManager.getPrefix() + chatManager.colorRawMessage("&cArena with that ID already exists!"));
				player.sendMessage(chatManager.getPrefix() + chatManager.colorRawMessage("&cUsage: /cd create <ID>"));
				return;
			}
		}

		if (ConfigUtils.getConfig(plugin, "arenas").contains("instances." + args[0])) {
			player.sendMessage(chatManager.getPrefix() + chatManager.colorRawMessage("Instance/Arena already exists! Use another ID or delete it first!"));
		} else {
			createInstanceInConfig(args[0]);
			player.sendMessage(ChatColor.BOLD + "----------------------------------------");
			player.sendMessage(ChatColor.YELLOW + "      Instance " + args[0] + " created!");
			player.sendMessage("");
			player.sendMessage(ChatColor.GREEN + "Edit this arena via " + ChatColor.GOLD + "/cd edit " + args[0] + ChatColor.GREEN + "!");
			player.sendMessage(ChatColor.BOLD + "----------------------------------------");
		}
	}

	private void createInstanceInConfig(String id) {
		String path = "instances." + id + ".";
		FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");

		config.set(path + "endlocation", LocationSerializer.locationToString(Bukkit.getServer().getWorlds().get(0).getSpawnLocation()));
		config.set(path + "firstplayerlocation", LocationSerializer.locationToString(Bukkit.getServer().getWorlds().get(0).getSpawnLocation()));
		config.set(path + "secondplayerlocation", LocationSerializer.locationToString(Bukkit.getServer().getWorlds().get(0).getSpawnLocation()));
		config.set(path + "mapname", id);
		config.set(path + "signs", new ArrayList<>());
		config.set(path + "isdone", false);

		ConfigUtils.saveConfig(plugin, config, "arenas");

		Arena arena = new Arena(id);

		arena.setMapName(config.getString(path + "mapname"));
		arena.setEndLocation(LocationSerializer.locationFromString(config.getString(path + "endlocation")));
		arena.setFirstPlayerLocation(LocationSerializer.locationFromString(config.getString(path + "firstplayerlocation")));
		arena.setSecondPlayerLocation(LocationSerializer.locationFromString(config.getString(path + "secondplayerlocation")));
		arena.setReady(false);

		ArenaRegistry.registerArena(arena);
	}

	@Override
	public List<String> getTutorial() {
		return Collections.singletonList("Create new arena");
	}

	@Override
	public CommandType getType() {
		return CommandType.GENERIC;
	}

	@Override
	public SenderType getSenderType() {
		return SenderType.PLAYER;
	}
}