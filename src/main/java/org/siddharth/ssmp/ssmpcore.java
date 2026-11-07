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
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ssmpcore {

    private final JavaPlugin plugin;

    public ssmpcore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // GUI METHODS

    public Inventory createGui(int size, String title) {
        return Bukkit.createInventory(null, size, color(title));
    }

    public Inventory createGui(InventoryHolder holder, int size, String title) {
        return Bukkit.createInventory(holder, size, color(title));
    }

    public ItemStack createGuiItem(Material material) {
        return new ItemStack(material);
    }

    public ItemStack createGuiItem(Material material, String name) {
        return createGuiItem(material, 1, name, List.of());
    }

    public ItemStack createGuiItem(Material material, String name, String... lore) {
        return createGuiItem(material, 1, name, List.of(lore));
    }

    public ItemStack createGuiItem(Material material, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(color(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createGuiItem(ItemStack item, String name, List<String> lore) {
        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(color(lore));
            result.setItemMeta(meta);
        }

        return result;
    }

    public void setGuiItem(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item == null ? null : item.clone());
        }
    }
    public ItemStack createGuiItem(Material material, String name, List<String> lore) {
        return createGuiItem(material, 1, name, lore);
    }

    public void setGuiItem(Inventory inventory, int slot, Material material, String name) {
        setGuiItem(inventory, slot, createGuiItem(material, name));
    }

    public void fillGui(Inventory inventory, ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    public void fillEmptyGuiSlots(Inventory inventory, ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, item.clone());
            }
        }
    }

    public void openGui(Player player, Inventory inventory) {
        player.openInventory(inventory);
    }

    public void closeGui(Player player) {
        player.closeInventory();
    }

    public FileConfiguration createConfig(String fileName) {
        File file = getConfigFile(fileName);

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create " + fileName);
                e.printStackTrace();
            }
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig(String fileName) {
        File file = getConfigFile(fileName);

        if (!file.exists()) {
            return createConfig(fileName);
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration reloadConfig(String fileName) {
        return getConfig(fileName);
    }

    public void saveConfig(String fileName, FileConfiguration config) {
        try {
            config.save(getConfigFile(fileName));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + fileName);
            e.printStackTrace();
        }
    }

    public void setConfig(String fileName, String path, Object value) {
        FileConfiguration config = getConfig(fileName);
        config.set(path, value);
        saveConfig(fileName, config);
    }

    public String getConfigString(String fileName, String path, String defaultValue) {
        return getConfig(fileName).getString(path, defaultValue);
    }

    public int getConfigInt(String fileName, String path, int defaultValue) {
        return getConfig(fileName).getInt(path, defaultValue);
    }

    public double getConfigDouble(String fileName, String path, double defaultValue) {
        return getConfig(fileName).getDouble(path, defaultValue);
    }

    public boolean getConfigBoolean(String fileName, String path, boolean defaultValue) {
        return getConfig(fileName).getBoolean(path, defaultValue);
    }

    public List<String> getConfigStringList(String fileName, String path) {
        return getConfig(fileName).getStringList(path);
    }

    public boolean configContains(String fileName, String path) {
        return getConfig(fileName).contains(path);
    }

    public boolean deleteConfig(String fileName) {
        return getConfigFile(fileName).delete();
    }

    private File getConfigFile(String fileName) {
        String name = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        return new File(plugin.getDataFolder(), name);
    }

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public void sendMessages(CommandSender sender, List<String> messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public List<String> color(List<String> texts) {
        List<String> result = new ArrayList<>();

        for (String text : texts) {
            result.add(color(text));
        }

        return result;
    }

    public String stripColor(String text) {
        return ChatColor.stripColor(color(text));
    }

    public void playSound(Player player, Sound sound) {
        playSound(player, sound, 1.0f, 1.0f);
    }

    public void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void playSound(Player player, String sound, float volume, float pitch) {
        try {
            Sound parsed = Sound.valueOf(sound.toUpperCase(Locale.ROOT));
            playSound(player, parsed, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + sound);
        }
    }

    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public boolean checkPermission(Player player, String permission) {
        if (hasPermission(player, permission)) {
            return true;
        }

        sendMessage(player, "&cYou don't have permission to do that.");
        return false;
    }

    public ItemStack createItem(Material material) {
        return new ItemStack(material);
    }

    public void setItemName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));
            item.setItemMeta(meta);
        }
    }

    public void setItemLore(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setLore(color(lore));
            item.setItemMeta(meta);
        }
    }

    public void setItemAmount(ItemStack item, int amount) {
        if (amount > 0) {
            item.setAmount(amount);
        }
    }

    public boolean isValidMaterial(String material) {
        return Material.matchMaterial(material) != null;
    }

    public Material getMaterial(String material) {
        return Material.matchMaterial(material);
    }

    public Player getOnlinePlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }

    public boolean isOnline(String name) {
        return getOnlinePlayer(name) != null;
    }
}