/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ssmpplayerinfogui implements Listener {

    private final ssmpplayerinfo playerInfo;
    private final File folder;
    private final File configFile;
    private final YamlConfiguration config;

    public ssmpplayerinfogui(ssmpplayerinfo playerInfo) {
        this.playerInfo = playerInfo;

        JavaPlugin plugin = playerInfo.getPlugin();

        folder = new File(
                plugin.getDataFolder(),
                "playerinfogui"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create playerinfogui directory."
            );
        }

        configFile = new File(folder, "config.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException(
                            "Could not create playerinfogui config.yml."
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to create playerinfogui config.yml.",
                        e
                );
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("gui.title", "&8Player Profile: {player}");
        config.addDefault("gui.size", 27);
        config.addDefault("slots.levels", 10);
        config.addDefault("slots.economy", 12);
        config.addDefault("slots.contracts", 14);
        config.addDefault("slots.bounties", 16);
        config.options().copyDefaults(true);

        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to save playerinfogui config.yml.",
                    e
            );
        }
    }

    public void open(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return;
        }

        int size = config.getInt("gui.size", 27);

        if (size < 9 || size > 54 || size % 9 != 0) {
            size = 27;
        }

        String title = config.getString(
                "gui.title",
                "&8Player Profile: {player}"
        );

        title = title.replace("{player}", target.getName());

        Holder gui = new Holder();

        Inventory inventory = Bukkit.createInventory(
                gui,
                size,
                ChatColor.translateAlternateColorCodes('&', title)
        );

        gui.setInventory(inventory);

        if (playerInfo.hasLevels()) {
            inventory.setItem(
                    getSlot("slots.levels", 10, size),
                    createItem(
                            Material.EXPERIENCE_BOTTLE,
                            ChatColor.GREEN + "Level Information",
                            List.of(
                                    ChatColor.GRAY + "Level: "
                                            + ChatColor.WHITE
                                            + playerInfo.getLevel(target),
                                    ChatColor.GRAY + "XP: "
                                            + ChatColor.WHITE
                                            + playerInfo.getXP(target),
                                    ChatColor.GRAY + "Next level: "
                                            + ChatColor.WHITE
                                            + playerInfo.getXPRequired(target)
                            )
                    )
            );
        }

        if (playerInfo.hasEconomy()) {
            inventory.setItem(
                    getSlot("slots.economy", 12, size),
                    createItem(
                            Material.GOLD_INGOT,
                            ChatColor.GOLD + "Economy",
                            List.of(
                                    ChatColor.GRAY + "Balance: "
                                            + ChatColor.WHITE
                                            + playerInfo.getFormattedBalance(target)
                            )
                    )
            );
        }

        if (playerInfo.hasContracts()) {
            inventory.setItem(
                    getSlot("slots.contracts", 14, size),
                    createItem(
                            Material.WRITABLE_BOOK,
                            ChatColor.AQUA + "Contracts",
                            List.of(
                                    ChatColor.GRAY + "Active: "
                                            + ChatColor.WHITE
                                            + playerInfo.getAcceptedContracts(target),
                                    ChatColor.GRAY + "Completed: "
                                            + ChatColor.WHITE
                                            + playerInfo.getCompletedContracts(target)
                            )
                    )
            );
        }

        if (playerInfo.hasBounties()) {
            inventory.setItem(
                    getSlot("slots.bounties", 16, size),
                    createItem(
                            Material.TARGET,
                            ChatColor.RED + "Bounty",
                            List.of(
                                    ChatColor.GRAY + "Current bounty: "
                                            + ChatColor.WHITE
                                            + playerInfo.getBounty(target)
                            )
                    )
            );
        }

        viewer.openInventory(inventory);
    }

    private int getSlot(String path, int defaultSlot, int size) {
        int slot = config.getInt(path, defaultSlot);

        if (slot < 0 || slot >= size) {
            return defaultSlot < size ? defaultSlot : 0;
        }

        return slot;
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(lore));

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder()
                instanceof Holder)) {
            return;
        }

        if (event.getRawSlot() < 0
                || event.getRawSlot()
                >= event.getView().getTopInventory().getSize()) {
            return;
        }

        event.setCancelled(true);
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    private static class Holder implements InventoryHolder {

        private Inventory inventory;

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}