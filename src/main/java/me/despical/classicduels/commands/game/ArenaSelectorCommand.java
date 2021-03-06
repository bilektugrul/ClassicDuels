package me.despical.classicduels.commands.game;

import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaManager;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.commands.SubCommand;
import me.despical.classicduels.handlers.ChatManager;
import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.number.NumberUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 11.10.2020
 */
public class ArenaSelectorCommand extends SubCommand implements Listener {

	private final ChatManager chatManager;

	public ArenaSelectorCommand() {
		super("arenas");

		setPermission("cd.arenas");
		chatManager = plugin.getChatManager();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public String getPossibleArguments() {
		return null;
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public void execute(CommandSender sender, ChatManager chatManager, String[] args) {
		Player player = (Player) sender;

		if (ArenaRegistry.getArenas().isEmpty()) {
			player.sendMessage(chatManager.getPrefix() + chatManager.colorMessage("Commands.No-Free-Arenas"));
			return;
		}

		Inventory inventory = Bukkit.createInventory(player, NumberUtils.serializeInt(ArenaRegistry.getArenas().size()), chatManager.colorMessage("Arena-Selector.Inventory-Title"));

		for (Arena arena : ArenaRegistry.getArenas()) {
			ItemStack itemStack;

			switch (arena.getArenaState()) {
				case WAITING_FOR_PLAYERS:
					itemStack = XMaterial.LIME_WOOL.parseItem();
					break;
				case STARTING:
					itemStack = XMaterial.YELLOW_WOOL.parseItem();
					break;
				default:
					itemStack = XMaterial.RED_WOOL.parseItem();
					break;
			}

			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName(arena.getId());

			ArrayList<String> lore = new ArrayList<>();
			FileConfiguration config = ConfigUtils.getConfig(plugin, "messages");

			for (String string : config.getStringList("Arena-Selector.Item.Lore")) {
				lore.add(formatItem(string, arena));
			}

			itemMeta.setLore(lore);
			itemStack.setItemMeta(itemMeta);
			inventory.addItem(itemStack);
		}

		player.openInventory(inventory);
	}

	private String formatItem(String string, Arena arena) {
		String formatted = string;
		formatted = StringUtils.replace(formatted, "%mapname%", arena.getMapName());

		if (arena.getPlayers().size() == 2) {
			formatted = StringUtils.replace(formatted, "%state%", chatManager.colorMessage("Signs.Game-States.Full-Game"));
		} else {
			formatted = StringUtils.replace(formatted, "%state%", plugin.getSignManager().getGameStateToString().get(arena.getArenaState()));
		}

		formatted = StringUtils.replace(formatted, "%playersize%", String.valueOf(arena.getPlayers().size()));
		formatted = chatManager.colorRawMessage(formatted);
		return formatted;
	}

	@EventHandler
	public void onArenaSelectorMenuClick(InventoryClickEvent e) {
		if (!e.getView().getTitle().equals(chatManager.colorMessage("Arena-Selector.Inventory-Title"))) {
			return;
		}

		if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) {
			return;
		}

		Player player = (Player) e.getWhoClicked();
		player.closeInventory();

		Arena arena = ArenaRegistry.getArena(e.getCurrentItem().getItemMeta().getDisplayName());

		if (arena != null) {
			ArenaManager.joinAttempt(player, arena);
		} else {
			player.sendMessage(chatManager.getPrefix() + chatManager.colorMessage("Commands.No-Arena-Like-That"));
		}
	}

	@Override
	public List<String> getTutorial() {
		return null;
	}

	@Override
	public CommandType getType() {
		return CommandType.HIDDEN;
	}

	@Override
	public SenderType getSenderType() {
		return SenderType.PLAYER;
	}
}