/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class ssmpplayerinfo {

    private final ssmp plugin;
    private final ssmpeconomy economy;
    private final ssmplevels levels;
    private final ssmpcontracts contracts;
    private final ssmpbounties bounties;
    private final File folder;
    private final File configFile;
    private final YamlConfiguration config;

    public ssmpplayerinfo(
            ssmp plugin,
            ssmpeconomy economy,
            ssmplevels levels,
            ssmpcontracts contracts,
            ssmpbounties bounties
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.levels = levels;
        this.contracts = contracts;
        this.bounties = bounties;

        folder = new File(
                plugin.getDataFolder(),
                "playerinfo"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create playerinfo directory."
            );
        }

        configFile = new File(folder, "config.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException(
                            "Could not create playerinfo config.yml."
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to create playerinfo config.yml.",
                        e
                );
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("display.levels", true);
        config.addDefault("display.economy", true);
        config.addDefault("display.contracts", true);
        config.addDefault("display.bounties", true);
        config.options().copyDefaults(true);

        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to save playerinfo config.yml.",
                    e
            );
        }
    }

    public ssmp getPlugin() {
        return plugin;
    }

    public int getLevel(Player player) {
        return levels == null || player == null
                ? 0
                : levels.getLevel(player);
    }

    public long getXP(Player player) {
        return levels == null || player == null
                ? 0
                : levels.getXP(player);
    }

    public long getXPRequired(Player player) {
        return levels == null || player == null
                ? 0
                : levels.getXPRequired(levels.getLevel(player));
    }

    public double getBalance(Player player) {
        return economy == null || player == null
                ? 0
                : economy.getPlayerBalance(player);
    }

    public String getFormattedBalance(Player player) {
        return economy == null || player == null
                ? "Unavailable"
                : economy.formatMoney(
                economy.getPlayerBalance(player)
        );
    }

    public double getBounty(Player player) {
        return bounties == null || player == null
                ? 0
                : bounties.getBounty(player);
    }

    public int getAcceptedContracts(Player player) {
        return contracts == null || player == null
                ? 0
                : contracts.getAcceptedCount(player);
    }

    public int getCompletedContracts(Player player) {
        return contracts == null || player == null
                ? 0
                : contracts.getCompletedCount(player);
    }

    public boolean hasLevels() {
        return levels != null
                && config.getBoolean("display.levels", true);
    }

    public boolean hasEconomy() {
        return economy != null
                && config.getBoolean("display.economy", true);
    }

    public boolean hasContracts() {
        return contracts != null
                && config.getBoolean("display.contracts", true);
    }

    public boolean hasBounties() {
        return bounties != null
                && config.getBoolean("display.bounties", true);
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}