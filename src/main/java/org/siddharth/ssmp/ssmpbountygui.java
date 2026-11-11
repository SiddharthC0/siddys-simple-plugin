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
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ssmpbountygui implements Listener {

    private final ssmpbounties bounties;
    private final ssmpeconomy economy;
    private final File folder;
    private final File configFile;
    private final YamlConfiguration config;

    public ssmpbountygui(
            ssmpbounties bounties,
            ssmpeconomy economy
    ) {
        this.bounties = bounties;
        this.economy = economy;

        folder = new File(
                bounties.getPlugin().getDataFolder(),
                "bountygui"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create bountygui directory."
            );
        }

        configFile = new File(folder, "config.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException(
                            "Could not create bountygui config.yml."
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to create bountygui config.yml.",
                        e
                );
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("gui.title", "&4SSMP Bounties");
        config.addDefault("gui.size", 54);
        config.addDefault("gui.max-entries", 45);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to save bountygui config.yml.",
                    e
            );
        }
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }

        int size = config.getInt("gui.size", 54);

        if (size < 18 || size > 54 || size % 9 != 0) {
            size = 54;
        }

        int maxEntries = Math.max(
                1,
                Math.min(
                        config.getInt("gui.max-entries", 45),
                        size - 9
                )
        );

        Holder gui = new Holder();
        Inventory inventory = Bukkit.createInventory(
                gui,
                size,
                ChatColor.translateAlternateColorCodes(
                        '&',
                        config.getString("gui.title", "&4SSMP Bounties")
                )
        );

        gui.setInventory(inventory);

        int slot = 0;

        for (UUID uuid : bounties.getBountyTargets()) {
            if (slot >= maxEntries) {
                break;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            String name = target.getName() == null
                    ? uuid.toString()
                    : target.getName();

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();

            if (meta == null) {
                continue;
            }

            meta.setDisplayName(ChatColor.RED + name);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current bounty:");
            lore.add(
                    ChatColor.GOLD
                            + (economy == null
                            ? String.valueOf(bounties.getBounty(uuid))
                            : economy.formatMoney(bounties.getBounty(uuid)))
            );
            lore.add("");
            lore.add(ChatColor.GRAY + "Target UID:");
            lore.add(ChatColor.DARK_GRAY + uuid.toString());

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot++, item);
        }

        player.openInventory(inventory);
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