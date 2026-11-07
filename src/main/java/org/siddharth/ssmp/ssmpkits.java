/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ssmpkits {

    private final ssmp plugin;
    private final ssmpcore core;
    private FileConfiguration config;
    private FileConfiguration cooldowns;

    public enum KitResult {
        SUCCESS,
        NOT_FOUND,
        NO_PERMISSION,
        COOLDOWN,
        DISABLED
    }

    public ssmpkits(ssmp plugin) {
        this.plugin = plugin;
        this.core = plugin.getCore();
        load();
    }

    public void load() {
        config = core.getConfig("kits");
        cooldowns = core.getConfig("kit-cooldowns");

        config.addDefault("settings.enabled", true);
        config.addDefault("settings.default-cooldown", 86400);
        config.addDefault("settings.gui-title", "&8SSMP | Kits");
        config.options().copyDefaults(true);

        core.saveConfig("kits", config);

        ConfigurationSection section = config.getConfigurationSection("kits");

        if (section != null) {
            for (String name : section.getKeys(false)) {
                registerKitPermission(name);
            }
        }
    }

    public void reload() {
        load();
    }

    public boolean createKit(Player player, String name) {
        return createKit(player, name, name);
    }

    public boolean createKit(Player player, String name, String displayName) {
        name = normalize(name);

        if (!isValidName(name) || config.contains("kits." + name)) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        String path = "kits." + name;

        config.set(path + ".display-name", displayName);
        config.set(path + ".icon", "CHEST");
        config.set(path + ".cooldown", config.getInt("settings.default-cooldown", 86400));
        config.set(path + ".description", List.of("&7A custom SSMP kit."));
        config.set(path + ".items", null);
        config.set(path + ".armor", null);
        config.set(path + ".offhand", null);

        for (int slot = 0; slot < inventory.getStorageContents().length; slot++) {
            ItemStack item = inventory.getItem(slot);

            if (item != null && !item.getType().isAir()) {
                config.set(path + ".items." + slot, item.clone());
            }
        }

        ItemStack[] armor = inventory.getArmorContents();

        for (int slot = 0; slot < armor.length; slot++) {
            if (armor[slot] != null && !armor[slot].getType().isAir()) {
                config.set(path + ".armor." + slot, armor[slot].clone());
            }
        }

        ItemStack offhand = inventory.getItemInOffHand();

        if (offhand != null && !offhand.getType().isAir()) {
            config.set(path + ".offhand", offhand.clone());
        }

        core.saveConfig("kits", config);
        registerKitPermission(name);

        return true;
    }

    public boolean deleteKit(String name) {
        name = normalize(name);

        if (!config.contains("kits." + name)) {
            return false;
        }

        config.set("kits." + name, null);
        core.saveConfig("kits", config);

        String permissionName = getKitPermission(name);
        Permission permission = Bukkit.getPluginManager().getPermission(permissionName);

        if (permission != null) {
            Bukkit.getPluginManager().removePermission(permission);
        }

        return true;
    }

    public boolean editKit(Player player, String name) {
        name = normalize(name);

        if (!config.contains("kits." + name)) {
            return false;
        }

        config.set("kits." + name + ".items", null);
        config.set("kits." + name + ".armor", null);
        config.set("kits." + name + ".offhand", null);

        PlayerInventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getStorageContents().length; slot++) {
            ItemStack item = inventory.getItem(slot);

            if (item != null && !item.getType().isAir()) {
                config.set("kits." + name + ".items." + slot, item.clone());
            }
        }

        ItemStack[] armor = inventory.getArmorContents();

        for (int slot = 0; slot < armor.length; slot++) {
            if (armor[slot] != null && !armor[slot].getType().isAir()) {
                config.set("kits." + name + ".armor." + slot, armor[slot].clone());
            }
        }

        ItemStack offhand = inventory.getItemInOffHand();

        if (offhand != null && !offhand.getType().isAir()) {
            config.set("kits." + name + ".offhand", offhand.clone());
        }

        core.saveConfig("kits", config);
        return true;
    }

    public KitResult giveKit(Player player, String name) {
        return giveKit(player, name, false, false);
    }

    public KitResult giveKit(Player player, String name, boolean bypassCooldown, boolean bypassPermission) {
        name = normalize(name);
        String path = "kits." + name;

        if (!config.getBoolean("settings.enabled", true)) {
            return KitResult.DISABLED;
        }

        if (!config.contains(path)) {
            return KitResult.NOT_FOUND;
        }

        if (!bypassPermission && !canUseKit(player, name)) {
            return KitResult.NO_PERMISSION;
        }

        if (!bypassCooldown && getRemainingCooldown(player, name) > 0) {
            return KitResult.COOLDOWN;
        }

        ConfigurationSection items = config.getConfigurationSection(path + ".items");

        if (items != null) {
            for (String slot : items.getKeys(false)) {
                ItemStack item = config.getItemStack(path + ".items." + slot);

                if (item != null && !item.getType().isAir()) {
                    giveItem(player, item);
                }
            }
        }

        ConfigurationSection armor = config.getConfigurationSection(path + ".armor");

        if (armor != null) {
            for (String slot : armor.getKeys(false)) {
                ItemStack item = config.getItemStack(path + ".armor." + slot);

                if (item != null && !item.getType().isAir()) {
                    giveItem(player, item);
                }
            }
        }

        ItemStack offhand = config.getItemStack(path + ".offhand");

        if (offhand != null && !offhand.getType().isAir()) {
            giveItem(player, offhand);
        }

        if (!bypassCooldown) {
            cooldowns.set(
                    "cooldowns." + player.getUniqueId() + "." + name,
                    System.currentTimeMillis()
            );

            core.saveConfig("kit-cooldowns", cooldowns);
        }

        return KitResult.SUCCESS;
    }

    private void giveItem(Player player, ItemStack item) {
        var leftovers = player.getInventory().addItem(item.clone());

        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    public boolean canUseKit(Player player, String name) {
        return player.isOp() || (
                player.hasPermission("ssmp.kits.use")
                        && player.hasPermission(getKitPermission(name))
        );
    }

    public void registerKitPermission(String name) {
        String permissionName = getKitPermission(name);

        if (Bukkit.getPluginManager().getPermission(permissionName) == null) {
            Bukkit.getPluginManager().addPermission(
                    new Permission(permissionName, PermissionDefault.FALSE)
            );
        }
    }

    public String getKitPermission(String name) {
        return "ssmp.kits.kit." + normalize(name);
    }

    public long getRemainingCooldown(Player player, String name) {
        long lastUsed = cooldowns.getLong(
                "cooldowns." + player.getUniqueId() + "." + normalize(name),
                0
        );

        long cooldown = getCooldown(name) * 1000L;
        long remaining = cooldown - (System.currentTimeMillis() - lastUsed);

        return Math.max(0, remaining / 1000L);
    }

    public long getCooldown(String name) {
        return config.getLong(
                "kits." + normalize(name) + ".cooldown",
                config.getLong("settings.default-cooldown", 86400)
        );
    }

    public String getDisplayName(String name) {
        return config.getString(
                "kits." + normalize(name) + ".display-name",
                name
        );
    }

    public String getIcon(String name) {
        return config.getString(
                "kits." + normalize(name) + ".icon",
                "CHEST"
        );
    }

    public List<String> getDescription(String name) {
        return config.getStringList(
                "kits." + normalize(name) + ".description"
        );
    }

    public List<String> getKitNames() {
        ConfigurationSection section = config.getConfigurationSection("kits");

        if (section == null) {
            return new ArrayList<>();
        }

        List<String> names = new ArrayList<>(section.getKeys(false));
        Collections.sort(names);

        return names;
    }

    public boolean exists(String name) {
        return config.contains("kits." + normalize(name));
    }

    public boolean isValidName(String name) {
        return name != null && name.matches("[a-z0-9_-]{1,32}");
    }

    public boolean isEnabled() {
        return config.getBoolean("settings.enabled", true);
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}