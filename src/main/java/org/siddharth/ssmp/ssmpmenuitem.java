/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ssmpmenuitem implements Listener {

    private final JavaPlugin plugin;
    private final File menuFolder;
    private final File configFile;
    private YamlConfiguration config;

    public ssmpmenuitem(JavaPlugin plugin) {
        this.plugin = plugin;

        menuFolder = new File(
                plugin.getDataFolder(),
                "menuitem"
        );

        configFile = new File(
                menuFolder,
                "config.yml"
        );

        createConfig();
    }

    private void createConfig() {
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.contains("enabled")) {
            config.set("enabled", true);
            config.set("material", "COMPASS");
            config.set("name", "&6&lSSMP Menu");
            config.set("slot", 8);
            config.set("command", "ssmp");
            config.set(
                    "lore",
                    List.of(
                            "&7Right-click to open the SSMP menu"
                    )
            );

            try {
                config.save(configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        giveMenuItem(event.getPlayer());
    }

    private void giveMenuItem(Player player) {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        int slot = config.getInt("slot", 8);

        if (hasMenuItem(player)) {
            return;
        }

        player.getInventory().setItem(slot, createItem());
    }

    private ItemStack createItem() {
        Material material = Material.matchMaterial(
                config.getString("material", "COMPASS")
        );

        if (material == null) {
            material = Material.COMPASS;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(
                    color(
                            config.getString(
                                    "name",
                                    "&6&lSSMP Menu"
                            )
                    )
            );

            meta.setLore(
                    config.getStringList("lore")
                            .stream()
                            .map(this::color)
                            .toList()
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean hasMenuItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMenuItem(item)) {
                return true;
            }
        }

        return false;
    }

    private boolean isMenuItem(ItemStack item) {
        if (item == null) {
            return false;
        }

        Material material = Material.matchMaterial(
                config.getString("material", "COMPASS")
        );

        if (material == null || item.getType() != material) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(
                color(
                        config.getString(
                                "name",
                                "&6&lSSMP Menu"
                        )
                )
        );
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isMenuItem(
                event.getItemDrop().getItemStack()
        )) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        if (isMenuItem(currentItem)
                || isMenuItem(cursorItem)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();

        if (!isMenuItem(item)) {
            return;
        }

        event.setCancelled(true);

        String command = config.getString(
                "command",
                "ssmp"
        );

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        event.getPlayer().performCommand(command);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}