
package org.siddharth.ssmp.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.siddharth.ssmp.ssmp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class configSystem implements Listener {

    private final ssmp plugin;

    private final List<ModuleEntry> modules = List.of(
            new ModuleEntry("ranks", "Ranks", "CORE"),
            new ModuleEntry("permissions", "Permissions", "CORE"),
            new ModuleEntry("economy", "Economy", "CORE"),
            new ModuleEntry("contracts", "Contracts", "STANDALONE"),
            new ModuleEntry("bounties", "Bounties", "STANDALONE"),
            new ModuleEntry("shop", "Shop", "STANDALONE"),
            new ModuleEntry("warps", "Warps", "STANDALONE"),
            new ModuleEntry("kits", "Kits", "STANDALONE"),
            new ModuleEntry("rtp", "Random Teleport", "STANDALONE"),
            new ModuleEntry("cpanel", "Control Panel", "STANDALONE"),
            new ModuleEntry("levels", "Levels", "CORE"),
            new ModuleEntry("scoreboard", "Scoreboard", "STANDALONE"),
            new ModuleEntry("chat", "Chat", "CORE"),
            new ModuleEntry("placeholders", "Placeholders", "CORE"),
            new ModuleEntry("menu", "Menu", "CORE"),
            new ModuleEntry("auction", "Auction House", "STANDALONE"),
            new ModuleEntry("tablist", "Tablist", "STANDALONE"),
            new ModuleEntry("enderchest", "Ender Chest", "STANDALONE"),
            new ModuleEntry("homes", "Homes", "STANDALONE"),
            new ModuleEntry("infobar", "InfoBar", "STANDALONE"),
            new ModuleEntry("nickname", "Nicknames", "CORE")
    );

    private final int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public configSystem(ssmp plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (plugin.getCommand("configp") != null) {
            plugin.getCommand("configp").setExecutor((sender, command, label, args) -> {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }

                openMenu(player);
                return true;
            });
        }
    }

    public void openMenu(Player player) {

        boolean canConfigure = player.hasPermission("ssmp.config");

        MenuHolder holder = new MenuHolder(canConfigure);

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ChatColor.DARK_GRAY + "SSMP Module Manager"
        );

        holder.inventory = inventory;

        for (int i = 0; i < modules.size(); i++) {

            ModuleEntry module = modules.get(i);

            boolean enabled = plugin.getConfig().getBoolean(
                    "modules." + module.id + ".enabled",
                    true
            );

            holder.changes.put(module.id, enabled);

            inventory.setItem(
                    slots[i],
                    createItem(module, enabled, canConfigure)
            );
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();

        if (infoMeta != null) {

            infoMeta.setDisplayName(ChatColor.GOLD + "Module Manager");

            if (canConfigure) {
                infoMeta.setLore(List.of(
                        ChatColor.GRAY + "Manage Siddy's Simple Plugin modules.",
                        "",
                        ChatColor.GREEN + "Green status: Enabled",
                        ChatColor.RED + "Red status: Disabled",
                        "",
                        ChatColor.YELLOW + "Click modules to toggle.",
                        ChatColor.GRAY + "Changes are saved when you close this menu.",
                        ChatColor.GRAY + "Modules reload automatically."
                ));
            } else {
                infoMeta.setLore(List.of(
                        ChatColor.GRAY + "Siddy's Simple Plugin modules.",
                        "",
                        ChatColor.GREEN + "Green status: Enabled",
                        ChatColor.RED + "Red status: Disabled",
                        "",
                        ChatColor.YELLOW + "Viewing mode",
                        ChatColor.GRAY + "You cannot modify module settings."
                ));
            }

            info.setItemMeta(infoMeta);
        }

        inventory.setItem(49, info);

        player.openInventory(inventory);
    }

    private ItemStack createItem(ModuleEntry module, boolean enabled, boolean canConfigure) {

        ItemStack item = new ItemStack(getMaterial(module.id));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    (enabled ? ChatColor.GREEN : ChatColor.RED) + module.name
            );

            if (canConfigure) {
                meta.setLore(List.of(
                        ChatColor.GRAY + "Module: " + ChatColor.WHITE + module.id,
                        ChatColor.GRAY + "Type: " + ChatColor.AQUA + module.type,
                        "",
                        ChatColor.GRAY + "Status: " +
                                (enabled
                                        ? ChatColor.GREEN + "ENABLED"
                                        : ChatColor.RED + "DISABLED"),
                        "",
                        ChatColor.YELLOW + "Click to toggle."
                ));
            } else {
                meta.setLore(List.of(
                        ChatColor.GRAY + "Module: " + ChatColor.WHITE + module.id,
                        ChatColor.GRAY + "Type: " + ChatColor.AQUA + module.type,
                        "",
                        ChatColor.GRAY + "Status: " +
                                (enabled
                                        ? ChatColor.GREEN + "ENABLED"
                                        : ChatColor.RED + "DISABLED")
                ));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getMaterial(String module) {
        return switch (module) {
            case "ranks" -> Material.GOLDEN_HELMET;
            case "permissions" -> Material.TRIPWIRE_HOOK;
            case "economy" -> Material.GOLD_INGOT;
            case "contracts" -> Material.WRITABLE_BOOK;
            case "bounties" -> Material.TARGET;
            case "shop" -> Material.EMERALD;
            case "warps" -> Material.ENDER_PEARL;
            case "kits" -> Material.CHEST;
            case "rtp" -> Material.COMPASS;
            case "cpanel" -> Material.REDSTONE_LAMP;
            case "levels" -> Material.EXPERIENCE_BOTTLE;
            case "scoreboard" -> Material.PAINTING;
            case "chat" -> Material.NAME_TAG;
            case "placeholders" -> Material.PAPER;
            case "menu" -> Material.CLOCK;
            case "auction" -> Material.DIAMOND;
            case "tablist" -> Material.PLAYER_HEAD;
            case "enderchest" -> Material.ENDER_CHEST;
            case "homes" -> Material.RED_BED;
            case "infobar" -> Material.BELL;
            case "nickname" -> Material.NAME_TAG;
            default -> Material.BARRIER;
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null ||
                event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        if (!holder.canConfigure ||
                !(event.getWhoClicked() instanceof Player player) ||
                !player.hasPermission("ssmp.config")) {
            return;
        }

        int slot = event.getSlot();

        for (int i = 0; i < modules.size(); i++) {

            if (slots[i] != slot) {
                continue;
            }

            ModuleEntry selected = modules.get(i);

            boolean newState = !holder.changes.getOrDefault(
                    selected.id,
                    true
            );

            holder.changes.put(selected.id, newState);

            event.getInventory().setItem(
                    slot,
                    createItem(selected, newState, true)
            );

            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        if (!holder.canConfigure ||
                !(event.getPlayer() instanceof Player player) ||
                !player.hasPermission("ssmp.config")) {
            return;
        }

        boolean changed = false;

        for (ModuleEntry module : modules) {

            boolean previous = plugin.getConfig().getBoolean(
                    "modules." + module.id + ".enabled",
                    true
            );

            boolean current = holder.changes.getOrDefault(
                    module.id,
                    previous
            );

            if (previous != current) {

                plugin.getConfig().set(
                        "modules." + module.id + ".enabled",
                        current
                );

                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        try {
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe(
                    "Failed to save module configuration: " + e.getMessage()
            );
            e.printStackTrace();
            return;
        }

        player.sendMessage(
                ChatColor.GREEN + "SSMP module configuration saved."
        );

        player.sendMessage(
                ChatColor.YELLOW + "Reloading modules..."
        );

        Bukkit.getScheduler().runTask(plugin, () -> {

            if (plugin.reloadModules()) {
                player.sendMessage(
                        ChatColor.GREEN + "SSMP modules have been reloaded successfully."
                );
            } else {
                player.sendMessage(
                        ChatColor.RED + "SSMP module reload failed. Check the console."
                );
            }
        });
    }

    private static class ModuleEntry {

        private final String id;
        private final String name;
        private final String type;

        private ModuleEntry(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }

    private static class MenuHolder implements InventoryHolder {

        private Inventory inventory;
        private final boolean canConfigure;
        private final Map<String, Boolean> changes = new HashMap<>();

        private MenuHolder(boolean canConfigure) {
            this.canConfigure = canConfigure;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
