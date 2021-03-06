package me.despical.classicduels.handlers.setup.components;

import com.github.despical.inventoryframework.GuiItem;
import com.github.despical.inventoryframework.pane.StaticPane;
import me.despical.classicduels.Main;
import me.despical.classicduels.arena.Arena;
import me.despical.classicduels.handlers.setup.SetupInventory;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.serializer.LocationSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Created at 12.10.2020
 */
public class SpawnComponents implements SetupComponent {

	private SetupInventory setupInventory;

	@Override
	public void prepare(SetupInventory setupInventory) {
		this.setupInventory = setupInventory;
	}

	@Override
	public void injectComponents(StaticPane pane) {
		Player player = setupInventory.getPlayer();
		FileConfiguration config = setupInventory.getConfig();
		Arena arena = setupInventory.getArena();
		Main plugin = setupInventory.getPlugin();
		String serializedLocation = LocationSerializer.locationToString(player.getLocation());

		pane.addItem(new GuiItem(new ItemBuilder(Material.REDSTONE_BLOCK)
			.name("&e&lSet First Location")
			.lore("&7Click to set the first location")
			.lore("&7on the place where you are standing")
			.lore("&8(location where first player will")
			.lore("&8be teleported in the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("instances." + arena.getId() + ".firstplayerlocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set("instances." + arena.getId() + ".firstplayerlocation", serializedLocation);
			arena.setEndLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aFirst player's location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 0, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.LAPIS_BLOCK)
			.name("&e&lSet Second Location")
			.lore("&7Click to set the second location")
			.lore("&7on the place where you are standing")
			.lore("&8(location where second player will")
			.lore("&8be teleported in the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("instances." + arena.getId() + ".secondplayerlocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set("instances." + arena.getId() + ".secondplayerlocation", serializedLocation);
			arena.setEndLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aSecond player's location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 1, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.IRON_BLOCK)
			.name("&e&lSet Ending Location")
			.lore("&7Click to set the ending location")
			.lore("&7on the place where you are standing")
			.lore("&8(location where players will")
			.lore("&8be teleported after the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("instances." + arena.getId() + ".endlocation"))
			.build(), e -> {

			e.getWhoClicked().closeInventory();
			config.set("instances." + arena.getId() + ".endlocation", serializedLocation);
			arena.setEndLocation(player.getLocation());
			player.sendMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aEnding location for arena " + arena.getId() + " set at your location!"));
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 2, 0);
	}
}