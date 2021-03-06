package me.despical.classicduels.handlers.setup.components;

import com.github.despical.inventoryframework.GuiItem;
import com.github.despical.inventoryframework.pane.StaticPane;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.arena.ArenaRegistry;
import me.despical.classicduels.arena.ArenaState;
import me.despical.classicduels.handlers.setup.SetupInventory;
import me.despical.classicduels.handlers.sign.ArenaSign;
import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.serializer.LocationSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 02.07.2020
 */
public class ArenaRegisterComponent implements SetupComponent {

	private SetupInventory setupInventory;

	@Override
	public void prepare(SetupInventory setupInventory) {
		this.setupInventory = setupInventory;
	}

	@Override
	public void injectComponents(StaticPane pane) {
		FileConfiguration config = setupInventory.getConfig();
		Main plugin = setupInventory.getPlugin();
		Arena arena = setupInventory.getArena();
		ItemStack registeredItem;

		if (!setupInventory.getArena().isReady()) {
			registeredItem = new ItemBuilder(XMaterial.FIREWORK_ROCKET.parseItem())
				.name("&e&lRegister Arena - Finish Setup")
				.lore("&7Click this when you're done with configuration.")
				.lore("&7It will validate and register arena.")
				.build();
		} else {
			registeredItem = new ItemBuilder(Material.BARRIER)
				.name("&a&lArena Registered - Congratulations")
				.lore("&7This arena is already registered!")
				.lore("&7Good job, you went through whole setup!")
				.lore("&7You can play on this arena now!")
				.enchantment(Enchantment.DURABILITY)
				.flag(ItemFlag.HIDE_ENCHANTS)
				.build();
		}

		pane.addItem(new GuiItem(registeredItem, e -> {
			e.getWhoClicked().closeInventory();

			if (ArenaRegistry.getArena(setupInventory.getArena().getId()).isReady()) {
				e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&a&l✔ &aThis arena was already validated and is ready to use!"));
				return;
			}

			String[] locations = {"endlocation", "firstplayerlocation", "secondplayerlocation"};

			for (String s : locations) {
				if (!config.isSet("instances." + arena.getId() + "." + s) || config.getString("instances." + arena.getId() + "." + s).equals(LocationSerializer.locationToString(Bukkit.getWorlds().get(0).getSpawnLocation()))) {
					e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&c&l✘ &cArena validation failed! Please configure following spawn properly: " + s + " (cannot be world spawn location)"));
					return;
				}
			}

			e.getWhoClicked().sendMessage(plugin.getChatManager().colorRawMessage("&a&l✔ &aValidation succeeded! Registering new arena instance: " + arena.getId()));
			config.set("instances." + arena.getId() + ".isdone", true);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			List<Sign> signsToUpdate = new ArrayList<>();
			ArenaRegistry.unregisterArena(setupInventory.getArena());

			for (ArenaSign arenaSign : plugin.getSignManager().getArenaSigns()) {
				if (arenaSign.getArena().equals(setupInventory.getArena())) {
					signsToUpdate.add(arenaSign.getSign());
				}
			}

			arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
			arena.setReady(true);
			arena.setMapName(config.getString("instances." + arena.getId() + ".mapname"));
			arena.setFirstPlayerLocation(LocationSerializer.locationFromString(config.getString("instances." + arena.getId() + ".firstplayerlocation")));
			arena.setSecondPlayerLocation(LocationSerializer.locationFromString(config.getString("instances." + arena.getId() + ".secondplayerlocation")));
			arena.setEndLocation(LocationSerializer.locationFromString(config.getString("instances." + arena.getId() + ".Endlocation")));

			ArenaRegistry.registerArena(arena);
			arena.start();

			ConfigUtils.saveConfig(plugin, config, "arenas");

			for (Sign s : signsToUpdate) {
				plugin.getSignManager().getArenaSigns().add(new ArenaSign(s, arena));
			}
		}), 8, 0);
	}
}