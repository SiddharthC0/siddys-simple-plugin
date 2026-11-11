/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ssmplevels {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, PlayerData> players = new HashMap<>();

    private static final long BASE_XP = 100;

    public ssmplevels(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "levels.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create levels.yml: " + e.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        if (config.getConfigurationSection("players") == null) return;

        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int level = Math.max(1, config.getInt("players." + key + ".level", 1));
                long xp = Math.max(0, config.getLong("players." + key + ".xp", 0));

                players.put(uuid, new PlayerData(level, xp));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid player UUID in levels.yml: " + key);
            }
        }
    }

    public int getLevel(Player player) {
        return getData(player).level;
    }

    public long getXP(Player player) {
        return getData(player).xp;
    }

    public long getXPRequired(int level) {
        if (level < 1) return BASE_XP;

        long required = BASE_XP;

        for (int i = 1; i < level; i++) {
            if (required > Long.MAX_VALUE / 1.5) {
                return Long.MAX_VALUE;
            }

            required = Math.round(required * 1.5);
        }

        return required;
    }

    public int addXP(Player player, long amount) {
        if (amount <= 0) return 0;

        PlayerData data = getData(player);
        data.xp = data.xp > Long.MAX_VALUE - amount
                ? Long.MAX_VALUE
                : data.xp + amount;

        int levelsGained = 0;

        while (data.level < Integer.MAX_VALUE) {
            long required = getXPRequired(data.level);

            if (data.xp < required) break;

            data.xp -= required;
            data.level++;
            levelsGained++;
        }

        save();
        return levelsGained;
    }

    public void removeXP(Player player, long amount) {
        if (amount <= 0) return;

        PlayerData data = getData(player);
        data.xp = Math.max(0, data.xp - Math.min(data.xp, amount));

        save();
    }

    public void setXP(Player player, long amount) {
        PlayerData data = getData(player);
        data.xp = Math.max(0, amount);

        while (data.level < Integer.MAX_VALUE) {
            long required = getXPRequired(data.level);

            if (data.xp < required) break;

            data.xp -= required;
            data.level++;
        }

        save();
    }

    public void setLevel(Player player, int level) {
        PlayerData data = getData(player);
        data.level = Math.max(1, level);
        data.xp = 0;

        save();
    }

    public void save() {
        config.set("players", null);

        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            String path = "players." + entry.getKey();

            config.set(path + ".level", entry.getValue().level);
            config.set(path + ".xp", entry.getValue().xp);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save levels.yml: " + e.getMessage());
        }
    }

    private PlayerData getData(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(1, 0));
    }

    private static class PlayerData {
        private int level;
        private long xp;

        private PlayerData(int level, long xp) {
            this.level = level;
            this.xp = xp;
        }
    }
}
