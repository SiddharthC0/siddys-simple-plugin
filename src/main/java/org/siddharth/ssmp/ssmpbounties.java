/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ssmpbounties implements Listener {

    private final JavaPlugin plugin;
    private final ssmpeconomy economy;
    private final File folder;
    private final File configFile;
    private final File file;
    private final File playersFile;
    private final YamlConfiguration config;
    private final YamlConfiguration data;
    private final YamlConfiguration players;

    public ssmpbounties(JavaPlugin plugin, ssmpeconomy economy) {
        this.plugin = plugin;
        this.economy = economy;

        folder = new File(plugin.getDataFolder(), "bounties");
        createFolder();

        configFile = new File(folder, "config.yml");
        file = new File(folder, "bounties.yml");
        playersFile = new File(folder, "players.yml");

        migrateLegacyFile(
                new File(plugin.getDataFolder(), "bounties.yml"),
                file
        );

        createFile(configFile);
        createFile(file);
        createFile(playersFile);

        config = YamlConfiguration.loadConfiguration(configFile);
        data = YamlConfiguration.loadConfiguration(file);
        players = YamlConfiguration.loadConfiguration(playersFile);

        config.addDefault("settings.minimum-amount", 1.0);
        config.addDefault("settings.maximum-amount", 0.0);
        config.addDefault("settings.save-on-change", true);
        config.options().copyDefaults(true);
        save(config, configFile);

        if (!data.contains("bounties")) {
            data.createSection("bounties");
            save(data, file);
        }
    }

    private void createFolder() {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create bounties directory."
            );
        }
    }

    private void createFile(File target) {
        if (!target.exists()) {
            try {
                if (!target.createNewFile()) {
                    throw new IOException(
                            "Could not create " + target.getName()
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to create " + target.getName(),
                        e
                );
            }
        }
    }

    private void migrateLegacyFile(File source, File destination) {
        if (destination.exists() || !source.exists()) {
            return;
        }

        try {
            Files.copy(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.COPY_ATTRIBUTES
            );

            plugin.getLogger().info(
                    "Migrated legacy bounty data into bounties/bounties.yml."
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to migrate legacy bounty data.",
                    e
            );
        }
    }

    private void save(
            YamlConfiguration configuration,
            File target
    ) {
        try {
            configuration.save(target);
        } catch (IOException e) {
            plugin.getLogger().severe(
                    "Failed to save " + target.getName() + ": "
                            + e.getMessage()
            );
        }
    }

    private void saveIfEnabled() {
        if (config.getBoolean("settings.save-on-change", true)) {
            save(data, file);
            save(players, playersFile);
        }
    }

    public boolean createBounty(
            Player issuer,
            Player target,
            double amount
    ) {
        if (issuer == null || target == null || economy == null) {
            return false;
        }

        if (issuer.equals(target)
                || amount <= 0
                || !Double.isFinite(amount)) {
            return false;
        }

        double minimum = config.getDouble(
                "settings.minimum-amount",
                1.0
        );

        double maximum = config.getDouble(
                "settings.maximum-amount",
                0.0
        );

        if (amount < minimum || (maximum > 0 && amount > maximum)) {
            return false;
        }

        if (!economy.canAfford(issuer, amount)) {
            return false;
        }

        String path = "bounties." + target.getUniqueId();
        double current = data.getDouble(path + ".amount", 0);

        if (!Double.isFinite(current + amount)) {
            return false;
        }

        if (!economy.removePlayerBalance(issuer, amount)) {
            return false;
        }

        data.set(path + ".amount", current + amount);
        data.set(path + ".name", target.getName());
        data.set(path + ".last-issuer", issuer.getUniqueId().toString());

        saveIfEnabled();

        issuer.sendMessage(
                ChatColor.GREEN + "You placed a bounty of "
                        + economy.formatMoney(amount)
                        + " on " + target.getName() + "."
        );

        target.sendMessage(
                ChatColor.RED + "A bounty has been placed on you!"
        );

        return true;
    }

    public double getBounty(Player player) {
        return player == null
                ? 0
                : getBounty(player.getUniqueId());
    }

    public double getBounty(UUID uuid) {
        return uuid == null
                ? 0
                : data.getDouble("bounties." + uuid + ".amount", 0);
    }

    public List<UUID> getBountyTargets() {
        ConfigurationSection section =
                data.getConfigurationSection("bounties");

        List<UUID> targets = new ArrayList<>();

        if (section == null) {
            return targets;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);

                if (getBounty(uuid) > 0) {
                    targets.add(uuid);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return targets;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player target = event.getEntity();
        Player killer = target.getKiller();

        if (killer == null || killer.equals(target) || economy == null) {
            return;
        }

        double amount = getBounty(target);

        if (amount <= 0) {
            return;
        }

        data.set("bounties." + target.getUniqueId(), null);

        economy.addPlayerBalance(killer, amount);

        String playerPath = "players." + killer.getUniqueId();

        players.set(
                playerPath + ".claimed-total",
                players.getDouble(playerPath + ".claimed-total", 0) + amount
        );

        players.set(
                playerPath + ".claimed-count",
                players.getInt(playerPath + ".claimed-count", 0) + 1
        );

        save(data, file);
        save(players, playersFile);

        killer.sendMessage(
                ChatColor.GOLD + "Bounty claimed! You earned "
                        + economy.formatMoney(amount) + "."
        );

        target.sendMessage(
                ChatColor.RED + "Your bounty was claimed by "
                        + killer.getName() + "."
        );
    }

    public double getClaimedTotal(UUID uuid) {
        return uuid == null
                ? 0
                : players.getDouble(
                "players." + uuid + ".claimed-total",
                0
        );
    }

    public int getClaimedCount(UUID uuid) {
        return uuid == null
                ? 0
                : players.getInt(
                "players." + uuid + ".claimed-count",
                0
        );
    }
    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void save() {
        save(data, file);
        save(players, playersFile);
        save(config, configFile);
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}