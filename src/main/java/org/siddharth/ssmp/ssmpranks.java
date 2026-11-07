/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ssmpranks {

    private final File rankFolder;
    private final File ranksFile;
    private final File playersFile;

    private YamlConfiguration ranks;
    private YamlConfiguration players;

    public ssmpranks(ssmp plugin) {
        rankFolder = new File(plugin.getDataFolder(), "ranks");
        ranksFile = new File(rankFolder, "ranks.yml");
        playersFile = new File(rankFolder, "players.yml");

        createFiles();
    }

    private void createFiles() {
        if (!rankFolder.exists()) {
            rankFolder.mkdirs();
        }

        try {
            if (!ranksFile.exists()) {
                ranksFile.createNewFile();
            }

            if (!playersFile.exists()) {
                playersFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ranks = YamlConfiguration.loadConfiguration(ranksFile);
        players = YamlConfiguration.loadConfiguration(playersFile);

        if (!ranks.contains("ranks.default")) {
            ranks.set("ranks.default.display-name", "Member");
            ranks.set("ranks.default.prefix", "&7[Member] ");
            ranks.set("ranks.default.suffix", "");
            ranks.set("ranks.default.inherits", List.of());

            saveRanks();
        }
    }

    public boolean createRank(
            String rank,
            String displayName,
            String prefix,
            String suffix
    ) {
        if (rank == null || rank.isBlank()) {
            return false;
        }

        rank = rank.toLowerCase();

        if (rankExists(rank)) {
            return false;
        }

        ranks.set(
                "ranks." + rank + ".display-name",
                displayName
        );

        ranks.set(
                "ranks." + rank + ".prefix",
                prefix
        );

        ranks.set(
                "ranks." + rank + ".suffix",
                suffix
        );

        ranks.set(
                "ranks." + rank + ".inherits",
                List.of()
        );

        saveRanks();

        return true;
    }

    public boolean deleteRank(String rank) {
        if (!rankExists(rank) || rank.equalsIgnoreCase("default")) {
            return false;
        }

        ranks.set("ranks." + rank.toLowerCase(), null);

        saveRanks();

        return true;
    }

    public boolean rankExists(String rank) {
        if (rank == null || rank.isBlank()) {
            return false;
        }

        return ranks.contains(
                "ranks." + rank.toLowerCase()
        );
    }

    public List<String> getRanks() {
        return ranks.getConfigurationSection("ranks") == null
                ? List.of()
                : List.copyOf(
                ranks.getConfigurationSection("ranks").getKeys(false)
        );
    }

    public boolean setPlayerRank(Player player, String rank) {
        if (!rankExists(rank)) {
            return false;
        }

        rank = rank.toLowerCase();

        String path =
                "players."
                        + player.getUniqueId()
                        + ".rank";

        players.set(path, rank);

        players.set(
                "players."
                        + player.getUniqueId()
                        + ".name",
                player.getName()
        );

        savePlayers();

        return true;
    }

    public boolean removePlayerRank(Player player) {
        String path =
                "players."
                        + player.getUniqueId()
                        + ".rank";

        if (!players.contains(path)) {
            return false;
        }

        players.set(path, null);

        savePlayers();

        return true;
    }

    public String getPlayerRank(Player player) {
        String rank = players.getString(
                "players."
                        + player.getUniqueId()
                        + ".rank"
        );

        if (rank == null || !rankExists(rank)) {
            return "default";
        }

        return rank;
    }

    public String getRankDisplayName(String rank) {
        if (!rankExists(rank)) {
            return null;
        }

        return ranks.getString(
                "ranks."
                        + rank.toLowerCase()
                        + ".display-name",
                rank
        );
    }

    public String getRankPrefix(String rank) {
        if (!rankExists(rank)) {
            return null;
        }

        return color(
                ranks.getString(
                        "ranks."
                                + rank.toLowerCase()
                                + ".prefix",
                        ""
                )
        );
    }

    public String getRankSuffix(String rank) {
        if (!rankExists(rank)) {
            return null;
        }

        return color(
                ranks.getString(
                        "ranks."
                                + rank.toLowerCase()
                                + ".suffix",
                        ""
                )
        );
    }

    public String getPlayerPrefix(Player player) {
        return getRankPrefix(
                getPlayerRank(player)
        );
    }

    public String getPlayerSuffix(Player player) {
        return getRankSuffix(
                getPlayerRank(player)
        );
    }

    public String getPlayerRankDisplayName(Player player) {
        return getRankDisplayName(
                getPlayerRank(player)
        );
    }

    public boolean setRankDisplayName(
            String rank,
            String displayName
    ) {
        if (!rankExists(rank)) {
            return false;
        }

        ranks.set(
                "ranks."
                        + rank.toLowerCase()
                        + ".display-name",
                displayName
        );

        saveRanks();

        return true;
    }

    public boolean setRankPrefix(
            String rank,
            String prefix
    ) {
        if (!rankExists(rank)) {
            return false;
        }

        ranks.set(
                "ranks."
                        + rank.toLowerCase()
                        + ".prefix",
                prefix
        );

        saveRanks();

        return true;
    }

    public boolean setRankSuffix(
            String rank,
            String suffix
    ) {
        if (!rankExists(rank)) {
            return false;
        }

        ranks.set(
                "ranks."
                        + rank.toLowerCase()
                        + ".suffix",
                suffix
        );

        saveRanks();

        return true;
    }

    public boolean setRankInheritance(
            String rank,
            List<String> inheritedRanks
    ) {
        if (!rankExists(rank)) {
            return false;
        }

        if (inheritedRanks == null) {
            inheritedRanks = List.of();
        }

        for (String inheritedRank : inheritedRanks) {
            if (!rankExists(inheritedRank)) {
                return false;
            }

            if (inheritedRank.equalsIgnoreCase(rank)) {
                return false;
            }
        }

        ranks.set(
                "ranks."
                        + rank.toLowerCase()
                        + ".inherits",
                inheritedRanks
        );

        saveRanks();

        return true;
    }

    public List<String> getInheritedRanks(String rank) {
        if (!rankExists(rank)) {
            return List.of();
        }

        return ranks.getStringList(
                "ranks."
                        + rank.toLowerCase()
                        + ".inherits"
        );
    }

    private void saveRanks() {
        try {
            ranks.save(ranksFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void savePlayers() {
        try {
            players.save(playersFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}