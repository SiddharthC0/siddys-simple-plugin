
/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp.mechanics;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerInformationSystem {

    private final SiddysSimplePlugin plugin;
    private final ContractSystem contracts;
    private final BountySystem bounties;
    private final File folder;
    private final File configFile;
    private final File playersFolder;
    private final File playersFile;
    private final YamlConfiguration config;
    private final YamlConfiguration players;

    public PlayerInformationSystem(
            SiddysSimplePlugin plugin,
            Economy economy,
            LevelSystem levels,
            ContractSystem contracts,
            BountySystem bounties
    ) {
        this.plugin = plugin;
        this.contracts = contracts;
        this.bounties = bounties;

        folder = new File(plugin.getDataFolder(), "playerinfo");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Failed to create playerinfo directory.");
        }

        configFile = new File(folder, "config.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException("Could not create playerinfo config.yml.");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create playerinfo config.yml.", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("display.contracts", true);
        config.addDefault("display.bounties", true);
        config.options().copyDefaults(true);

        saveConfig();

        playersFolder = new File(plugin.getDataFolder(), "players");

        if (!playersFolder.exists() && !playersFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create players directory.");
        }

        playersFile = new File(playersFolder, "players.yml");

        if (!playersFile.exists()) {
            try {
                if (!playersFile.createNewFile()) {
                    throw new IOException("Could not create players.yml.");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create players.yml.", e);
            }
        }

        players = YamlConfiguration.loadConfiguration(playersFile);
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save playerinfo config.yml.", e);
        }
    }

    private void savePlayers() {
        try {
            players.save(playersFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save players.yml.", e);
        }
    }

    private String getPlayerPath(Player player, String setting) {
        return "players." + player.getUniqueId() + "." + setting;
    }

    public void setSetting(Player player, String setting, boolean value) {
        if (player == null || setting == null || setting.isBlank()) return;

        players.set(getPlayerPath(player, setting), value);
        savePlayers();
    }

    public boolean getSetting(Player player, String setting, boolean defaultValue) {
        if (player == null || setting == null || setting.isBlank()) {
            return defaultValue;
        }

        return players.getBoolean(
                getPlayerPath(player, setting),
                defaultValue
        );
    }

    public boolean isEnabled(Player player, String setting) {
        return getSetting(player, setting, true);
    }

    public void toggleSetting(Player player, String setting) {
        if (player == null || setting == null || setting.isBlank()) return;

        setSetting(
                player,
                setting,
                !getSetting(player, setting, true)
        );
    }

    public boolean hasSetting(Player player, String setting) {
        if (player == null || setting == null || setting.isBlank()) return false;

        return players.contains(getPlayerPath(player, setting));
    }

    public UUID getPlayerUUID(Player player) {
        return player == null ? null : player.getUniqueId();
    }

    public SiddysSimplePlugin getPlugin() {
        return plugin;
    }

    public boolean hasContracts() {
        return contracts != null && config.getBoolean("display.contracts", true);
    }

    public boolean hasBounties() {
        return bounties != null && config.getBoolean("display.bounties", true);
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public YamlConfiguration getPlayers() {
        return players;
    }
}
