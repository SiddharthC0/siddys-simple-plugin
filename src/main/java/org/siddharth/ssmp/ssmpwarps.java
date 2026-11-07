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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ssmpwarps implements Listener {

    private final ssmp plugin;
    private final File warpsFile;
    private final YamlConfiguration warpsData;

    public ssmpwarps(ssmp plugin) {
        this.plugin = plugin;

        File folder = new File(
                plugin.getDataFolder(),
                "warps"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException(
                    "Failed to create warps folder."
            );
        }

        warpsFile = new File(
                folder,
                "warps.yml"
        );

        if (!warpsFile.exists()) {
            try {
                if (!warpsFile.createNewFile()) {
                    throw new IOException(
                            "Could not create warps.yml."
                    );
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create warps.yml.",
                        e
                );
            }
        }

        warpsData =
                YamlConfiguration.loadConfiguration(
                        warpsFile
                );
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean(
                "modules.warps.enabled",
                true
        );
    }

    public boolean setWarp(
            String name,
            Location location
    ) {
        if (!isEnabled()) {
            return false;
        }

        name = name.toLowerCase();

        String base =
                "warps." + name;

        warpsData.set(
                base + ".world",
                location.getWorld().getName()
        );

        warpsData.set(
                base + ".x",
                location.getX()
        );

        warpsData.set(
                base + ".y",
                location.getY()
        );

        warpsData.set(
                base + ".z",
                location.getZ()
        );

        warpsData.set(
                base + ".yaw",
                location.getYaw()
        );

        warpsData.set(
                base + ".pitch",
                location.getPitch()
        );

        save();

        return true;
    }

    public boolean deleteWarp(String name) {
        String path =
                "warps."
                        + name.toLowerCase();

        if (!warpsData.contains(path)) {
            return false;
        }

        warpsData.set(path, null);
        save();

        return true;
    }

    public boolean teleportWarp(
            Player player,
            String name
    ) {
        String path =
                "warps."
                        + name.toLowerCase();

        if (!warpsData.contains(path)) {
            return false;
        }

        String worldName =
                warpsData.getString(
                        path + ".world"
                );

        if (worldName == null) {
            return false;
        }

        if (Bukkit.getWorld(worldName) == null) {
            return false;
        }

        Location location =
                new Location(
                        Bukkit.getWorld(worldName),
                        warpsData.getDouble(path + ".x"),
                        warpsData.getDouble(path + ".y"),
                        warpsData.getDouble(path + ".z"),
                        (float) warpsData.getDouble(
                                path + ".yaw"
                        ),
                        (float) warpsData.getDouble(
                                path + ".pitch"
                        )
                );

        return player.teleport(location);
    }

    public List<String> getWarps() {
        List<String> warps =
                new ArrayList<>();

        if (warpsData.getConfigurationSection(
                "warps"
        ) == null) {
            return warps;
        }

        warps.addAll(
                warpsData
                        .getConfigurationSection("warps")
                        .getKeys(false)
        );

        return warps;
    }

    public void openGUI(Player player) {
        List<String> warps =
                getWarps();

        int size = Math.max(
                9,
                Math.min(
                        54,
                        ((warps.size() + 8) / 9) * 9
                )
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        size,
                        ChatColor.DARK_AQUA
                                + "SSMP | Warps"
                );

        for (int i = 0; i < warps.size(); i++) {
            String warp = warps.get(i);

            ItemStack item =
                    new ItemStack(
                            Material.COMPASS
                    );

            ItemMeta meta =
                    item.getItemMeta();

            if (meta == null) {
                continue;
            }

            meta.setDisplayName(
                    ChatColor.AQUA
                            + warp
            );

            meta.setLore(List.of(
                    "",
                    ChatColor.GRAY
                            + "Click to teleport."
            ));

            item.setItemMeta(meta);

            inventory.setItem(i, item);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {
        if (!ChatColor.stripColor(
                event.getView().getTitle()
        ).equals("SSMP | Warps")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player)) {
            return;
        }

        if (event.getRawSlot() < 0
                || event.getRawSlot()
                >= event.getView()
                .getTopInventory()
                .getSize()) {
            return;
        }

        ItemStack item =
                event.getCurrentItem();

        if (item == null
                || item.getType() != Material.COMPASS
                || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null
                || !meta.hasDisplayName()) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        String name =
                ChatColor.stripColor(
                        meta.getDisplayName()
                );

        if (teleportWarp(player, name)) {
            player.sendMessage(
                    ChatColor.GREEN
                            + "Teleported to "
                            + ChatColor.AQUA
                            + name
                            + ChatColor.GREEN
                            + "."
            );

            player.closeInventory();
        }
    }

    private void save() {
        try {
            warpsData.save(warpsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}