/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import java.util.UUID;

public class ssmphomes implements Listener {

    private final ssmp plugin;
    private final File homesFile;
    private final YamlConfiguration homesData;

    public ssmphomes(ssmp plugin) {
        this.plugin = plugin;

        File folder = new File(
                plugin.getDataFolder(),
                "homes"
        );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        homesFile = new File(
                folder,
                "homes.yml"
        );

        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        homesData =
                YamlConfiguration.loadConfiguration(
                        homesFile
                );
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean(
                "modules.homes.enabled",
                true
        );
    }

    public int getMaxHomes(Player player) {
        return plugin.getConfig().getInt(
                "modules.homes.max-homes",
                3
        );
    }

    public boolean setHome(
            Player player,
            String name
    ) {
        if (!isEnabled()) {
            return false;
        }

        name = name.toLowerCase();

        UUID uuid = player.getUniqueId();

        String base =
                "players." + uuid + ".homes." + name;

        if (!homesData.contains(
                "players." + uuid + ".homes"
        ) && getHomeCount(player) >= getMaxHomes(player)) {
            return false;
        }

        if (!homesData.contains(base)
                && getHomeCount(player) >= getMaxHomes(player)) {
            return false;
        }

        Location location =
                player.getLocation();

        homesData.set(
                base + ".world",
                location.getWorld().getName()
        );

        homesData.set(
                base + ".x",
                location.getX()
        );

        homesData.set(
                base + ".y",
                location.getY()
        );

        homesData.set(
                base + ".z",
                location.getZ()
        );

        homesData.set(
                base + ".yaw",
                location.getYaw()
        );

        homesData.set(
                base + ".pitch",
                location.getPitch()
        );

        save();

        return true;
    }

    public boolean deleteHome(
            Player player,
            String name
    ) {
        String path =
                "players."
                        + player.getUniqueId()
                        + ".homes."
                        + name.toLowerCase();

        if (!homesData.contains(path)) {
            return false;
        }

        homesData.set(path, null);
        save();

        return true;
    }

    public boolean teleportHome(
            Player player,
            String name
    ) {
        String path =
                "players."
                        + player.getUniqueId()
                        + ".homes."
                        + name.toLowerCase();

        if (!homesData.contains(path)) {
            return false;
        }

        String worldName =
                homesData.getString(
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
                        homesData.getDouble(path + ".x"),
                        homesData.getDouble(path + ".y"),
                        homesData.getDouble(path + ".z"),
                        (float) homesData.getDouble(path + ".yaw"),
                        (float) homesData.getDouble(path + ".pitch")
                );

        player.teleport(location);

        return true;
    }

    public int getHomeCount(Player player) {
        return homesData.getConfigurationSection(
                "players."
                        + player.getUniqueId()
                        + ".homes"
        ) == null
                ? 0
                : homesData.getConfigurationSection(
                "players."
                        + player.getUniqueId()
                        + ".homes"
        ).getKeys(false).size();
    }

    public List<String> getHomes(Player player) {
        List<String> homes = new ArrayList<>();

        String path =
                "players."
                        + player.getUniqueId()
                        + ".homes";

        if (homesData.getConfigurationSection(path) == null) {
            return homes;
        }

        homes.addAll(
                homesData.getConfigurationSection(path)
                        .getKeys(false)
        );

        return homes;
    }

    public void openGUI(Player player) {
        List<String> homes =
                getHomes(player);

        int size = Math.max(
                9,
                Math.min(
                        54,
                        ((homes.size() + 8) / 9) * 9
                )
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        size,
                        ChatColor.DARK_AQUA + "SSMP | Homes"
                );

        for (int i = 0; i < homes.size(); i++) {
            String home = homes.get(i);

            ItemStack item =
                    new ItemStack(Material.ENDER_EYE);

            ItemMeta meta =
                    item.getItemMeta();

            meta.setDisplayName(
                    ChatColor.AQUA
                            + home
            );

            List<String> lore =
                    new ArrayList<>();

            lore.add(
                    ChatColor.GRAY
                            + "Click to teleport."
            );

            lore.add(
                    ChatColor.RED
                            + "Shift-click to delete."
            );

            meta.setLore(lore);

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
        ).equals("SSMP | Homes")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

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
                || item.getType() != Material.ENDER_EYE
                || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null
                || !meta.hasDisplayName()) {
            return;
        }

        String name =
                ChatColor.stripColor(
                        meta.getDisplayName()
                );

        if (event.isShiftClick()) {
            if (deleteHome(player, name)) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED+"Deleted home "+name)
                );

                openGUI(player);
            }

            return;
        }

        if (teleportHome(player, name)) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.GREEN + "Teleported to " + name)
            );

            player.closeInventory();
        }
    }

    private void save() {
        try {
            homesData.save(homesFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}