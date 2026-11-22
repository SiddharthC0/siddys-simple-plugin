package org.siddharth.ssmp.mechanics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.siddharth.ssmp.SiddysSimplePlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class MenuSystem implements Listener {
    private final SiddysSimplePlugin plugin;
    private final SoundSystem sounds;
    private File menuFolder;
    private File menuFile;
    private FileConfiguration config;
    // guide : add the id of the new menu item you wanna add
    private static final String[] MENU_ITEMS = {"shop", "auction", "kits", "homes", "enderchest", "warps", "player", "rtp", "support", "configp"};
    public MenuSystem(SiddysSimplePlugin plugin, SoundSystem sounds) {
        this.plugin = plugin;
        this.sounds = sounds;
        loadConfig();
    }

    private void loadConfig() {
        menuFolder = new File(plugin.getDataFolder(), "menu");
        if (!menuFolder.exists() && !menuFolder.mkdirs()) {
            plugin.getLogger().severe("Could not create SSMP menu folder: " + menuFolder.getAbsolutePath());
            return;
        }

        menuFile = new File(menuFolder, "config.yml");
        if (!menuFile.exists()) {
            plugin.getLogger().info("SSMP menu config does not exist. Creating default menu config.");
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(menuFile);
    }

    private void createDefaultConfig() {
        config = new YamlConfiguration();
        config.set("menu.title", "&3S&bS&3M&bP&8 | &7Menu");
        config.set("menu.size", 54);

        config.set("menu.border.enabled", true);
        config.set("menu.border.material", "BLACK_STAINED_GLASS_PANE");
        config.set("menu.border.name", " ");
        config.set("menu.border.lore", new ArrayList<>());
        config.set("menu.border.top.enabled", true);
        config.set("menu.border.top.slots", "0-8");
        config.set("menu.border.bottom.enabled", true);
        config.set("menu.border.bottom.slots", "45-53");
        config.set("menu.border.left.enabled", true);
        config.set("menu.border.left.slots", "9,18,27,36");
        config.set("menu.border.right.enabled", true);
        config.set("menu.border.right.slots", "17,26,35,44");

        config.set("settings.cancel-clicks", true);
        config.set("settings.close-on-action", true);

        config.set("sounds.open.enabled", true);
        config.set("sounds.open.sound", "menu-open");
        config.set("sounds.click.enabled", true);
        config.set("sounds.click.sound", "menu-click");
        // guide : follow any format and then write your custom action to it... follow the double chest slot
        setItemDefaults("shop", true, 20, "EMERALD", "&a&lShop",
                List.of(
                        "",
                        "&7Explore a wide category",
                        "&7of items to buy or sell.",
                        "",
                        "&8» &aClick to open"
                ), "shop"
        );

        setItemDefaults("auction", true, 22, "LECTERN", "&d&lAuction House",
                List.of(
                        "",
                        "&7Explore items that other",
                        "&7players have put up for sale.",
                        "",
                        "&8» &dClick to open"
                ), "auction"
        );

        setItemDefaults("kits", true, 24, "DIAMOND_CHESTPLATE", "&9&lKits",
                List.of(
                        "",
                        "&7Open the menu of available kits",
                        "&7by clicking this.",
                        "",
                        "&8» &6Click to open kits"
                ), "kits"
        );

        setItemDefaults("homes", true, 29, "ENDER_EYE", "&b&lHomes",
                List.of(
                        "",
                        "&7View and manage your homes.",
                        "",
                        "&8» &bClick to open"
                ), "homes"
        );

        setItemDefaults("enderchest", true, 31, "ENDER_CHEST", "&d&lEnder Chest",
                List.of(
                        "",
                        "&7Open your Ender Chest.",
                        "",
                        "&8» &dClick to open"
                ), "enderchest"
        );

        setItemDefaults("warps", true, 33, "COMPASS", "&9&lWarps",
                List.of(
                        "",
                        "&7Browse available server warps.",
                        "",
                        "&8» &9Click to open"
                ), "warps"
        );

        setItemDefaults("support", true, 38, "WRITTEN_BOOK", "&6&lSupport",
                List.of(
                        "",
                        "&7Opens the quick start guide",
                        "&7by default. ",
                        "",
                        "&8» &9Click to open"
                ), "support"
        );

        setItemDefaults("rtp", true, 40, "ENDER_PEARL", "&3&lRandom Teleport",
                List.of(
                        "",
                        "&7Randomly teleport through a GUI.",
                        "",
                        "&8» &9Click to open"
                ), "rtp"
        );

        setItemDefaults("configp", true, 42, "COMPARATOR", "&3&lModule Menu",
                List.of(
                        "",
                        "&7Opens the module config menu",
                        "&7May differ, if you have the &l&cssmp.config&r&7 permission, it will open editing mode.",
                        "&7But if you don't have the permission, it will open viewing mode",
                        "&8» &9Click to open"
                ), "configp"
        );

        setItemDefaults("player", true, 4, "PLAYER_HEAD", "&b&l%player%",
                List.of(
                        "",
                        "&7Click to open",
                        "&bPlayer Info",
                        ""
                ), "playerinfo"
        );
        saveConfig();
    }
    private void setItemDefaults(String name, boolean enabled, int slot, String material, String displayName, List<String> lore, String action) {
        String path = "items." + name;
        config.set(path + ".enabled", enabled);
        config.set(path + ".slot", slot);
        config.set(path + ".material", material);
        config.set(path + ".name", displayName);
        config.set(path + ".lore", lore);
        config.set(path + ".action", action);
    }

    private void saveConfig() {
        if (config == null || menuFile == null) {
            return;
        }

        try {
            config.save(menuFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save SSMP menu config: " + e.getMessage());
        }
    }

    public void reload() {
        if (menuFile == null) {
            loadConfig();
            return;
        }

        config = YamlConfiguration.loadConfiguration(menuFile);
        plugin.getLogger().info("SSMP menu configuration reloaded.");
    }


    public void open(Player player) {
        if (config == null) {
            plugin.getLogger().severe("Cannot open SSMP menu because the menu configuration is not loaded.");
            return;
        }

        int size = config.getInt("menu.size", 54);
        if (size < 9 || size > 54 || size % 9 != 0) {
            plugin.getLogger().warning("Invalid SSMP menu size " + size + ". Using 54.");
            size = 54;
        }

        String title = color(config.getString("menu.title", "&3S&bS&3M&bP&8 | &7Menu"));
        Inventory inventory = Bukkit.createInventory(player, size, title);
        createBorder(inventory);
        createItems(inventory, player);
        player.openInventory(inventory);
        if (config.getBoolean("sounds.open.enabled", true)) {
            sounds.play(player, config.getString("sounds.open.sound", "menu-open"));
        }
    }

    private void createBorder(Inventory inventory) {
        if (!config.getBoolean("menu.border.enabled", true)) {return;}
        String materialName = config.getString("menu.border.material", "BLACK_STAINED_GLASS_PANE");
        Material material = getMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("SSMP menu border has invalid material: " + materialName);
            return;
        }

        ItemStack border = createItem(material, config.getString("menu.border.name", " "), config.getStringList("menu.border.lore"));
        createBorderSide(inventory, border, "top");
        createBorderSide(inventory, border, "bottom");
        createBorderSide(inventory, border, "left");
        createBorderSide(inventory, border, "right");
    }

    private void createBorderSide(Inventory inventory, ItemStack item, String side) {
        String path = "menu.border." + side;
        if (!config.getBoolean(path + ".enabled", true)) {return;}
        String slots = config.getString(path + ".slots", "");
        List<Integer> parsedSlots = parseSlots(slots);
        for (int slot : parsedSlots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item.clone());
            }
        }
    }

    private void createItems(Inventory inventory, Player player) {
        for (String itemName : MENU_ITEMS) {
            String path = "items." + itemName;
            if (!config.contains(path)) {
                plugin.getLogger().warning("SSMP menu item is missing from config: " + itemName);
                continue;
            }
            if (!config.getBoolean(path + ".enabled", true)) {continue;}
            int slot = config.getInt(path + ".slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                plugin.getLogger().warning("SSMP menu item " + itemName + " has invalid slot: " + slot);
                continue;
            }

            String materialName = config.getString(path + ".material", "STONE");
            Material material = getMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("SSMP menu item " + itemName + " has invalid material: " + materialName);
                continue;
            }

            String name = config.getString(path + ".name", "");
            List<String> lore = config.getStringList(path + ".lore");
            name = replacePlaceholders(name, player);
            List<String> convertedLore = new ArrayList<>();
            for (String line : lore) {
                convertedLore.add(replacePlaceholders(line, player));
            }

            ItemStack item = createItem(material, name, convertedLore);
            inventory.setItem(slot, item);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(color(line));
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (config == null) {
            return;
        }

        String configuredTitle = color(config.getString("menu.title", "&3S&bS&3M&bP&8 | &7Menu"));
        if (!configuredTitle.equals(event.getView().getTitle())) {return;}
        if (config.getBoolean("settings.cancel-clicks", true)) {event.setCancelled(true);}
        if (!(event.getWhoClicked() instanceof Player)) {return;}
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (rawSlot < 0 || rawSlot >= topSize) {return;}
        Player player = (Player) event.getWhoClicked();
        for (String itemName : MENU_ITEMS) {
            String path = "items." + itemName;
            if (!config.contains(path)) {
                continue;
            }

            if (!config.getBoolean(path + ".enabled", true)) {continue;}
            if (config.getInt(path + ".slot", -1) != rawSlot) {continue;}
            String action = config.getString(path + ".action", "none");
            executeAction(player, action);
            return;
        }
    }

    private void executeAction(Player player, String action) {
        if (action == null) {
            return;
        }

        action = action.trim().toLowerCase();
        if (config.getBoolean("sounds.click.enabled", true)) {
            sounds.play(player, config.getString("sounds.click.sound", "menu-click"));
        }
        if (config.getBoolean("settings.close-on-action", true)) {
            player.closeInventory();
        }

        // guide : finally, add your action here as a case of this switch statement, make sure to make it foolproof, but i also have
        // included a built in default system that just runs your action as a command...
        switch (action) {
            case "shop":
                if (!isModuleEnabled("shop")) {
                    return;
                }
                player.performCommand("shop");
                break;

            case "auction":
                if (!isModuleEnabled("auction")) {
                    return;
                }

                player.performCommand("ah");
                break;

            case "support":
                player.performCommand("support");
                plugin.getSupport().openQuickstart(player);
                break;

            case "homes":
                if (!isModuleEnabled("homes") || plugin.getHomes() == null) {
                    return;
                }

                plugin.getHomes().openGUI(player);
                break;

            case "enderchest":
                if (!isModuleEnabled("enderchest") || plugin.getEnderChest() == null) {
                    return;
                }

                if (!player.hasPermission("ssmp.enderchest.use")) {
                    return;
                }

                plugin.getEnderChest().open(player);
                break;

            case "warps":
                if (!isModuleEnabled("warps") || plugin.getWarps() == null) {
                    return;
                }

                plugin.getWarps().openGUI(player);
                break;

            case "kits":
                if (!isModuleEnabled("kits")) {
                    return;
                }
                plugin.getKitGui().open(player, 1);
                break;

            case "rtp":
                if (!isModuleEnabled("rtp")) {
                    return;
                }
                plugin.getRTP().openWorldGui(player);
                break;

            case "playerinfo":
                plugin.getPlayerInfoGui().open(player, player);
                break;

            case "configp":
                plugin.getConfigSystem().openMenu(player);
                break;

            case "none":
            case "":
                break;

            default:
                player.performCommand(action);
        }
    }

    private boolean isModuleEnabled(String module) {
        return plugin.getConfig().getBoolean("modules." + module + ".enabled", true);
    }

    private Material getMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return null;
        }

        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.matchMaterial(materialName.toUpperCase());
        }

        return material;
    }

    private List<Integer> parseSlots(String input) {
        List<Integer> slots = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return slots;
        }
        String[] parts = input.split(",");
        for (String part : parts) {
            part = part.trim();

            if (part.isEmpty()) {
                continue;
            }

            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length != 2) {
                    plugin.getLogger().warning("Invalid SSMP menu slot range: " + part);
                    continue;
                }

                try {
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    if (start > end) {
                        int temp = start;
                        start = end;
                        end = temp;
                    }

                    for (int i = start;
                         i <= end;
                         i++) {
                        slots.add(i);
                    }

                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid SSMP menu slot range: " + part);
                }

            } else {
                try {
                    slots.add(Integer.parseInt(part));
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid SSMP menu slot: " + part);
                }
            }
        }

        return slots;
    }

    private String replacePlaceholders(String text, Player player) {
        if (text == null) {
            return "";
        }

        return text.replace("%player%", player.getName());
    } // this meth is quite ueless because now the plugin uses inbuilt placeholder system.. but useful if placeholder == disabled

    private String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}