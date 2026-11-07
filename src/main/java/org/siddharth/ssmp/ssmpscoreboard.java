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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ssmpscoreboard implements Listener {

    private final JavaPlugin plugin;
    private final ssmpeconomy economy;

    private final File scoreboardFolder;
    private final File configFile;
    private YamlConfiguration config;

    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private BukkitTask updateTask;

    private static final String OBJECTIVE_NAME = "ssmp";
    private static final String TEAM_PREFIX = "ssmp_line_";

    public ssmpscoreboard(ssmpeconomy economy, JavaPlugin plugin) {
        this.economy = economy;
        this.plugin = plugin;

        scoreboardFolder = new File(
                plugin.getDataFolder(),
                "scoreboard"
        );

        configFile = new File(
                scoreboardFolder,
                "config.yml"
        );

        createConfig();
    }

    private void createConfig() {
        if (!scoreboardFolder.exists()) {
            scoreboardFolder.mkdirs();
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.contains("enabled")) {
            config.set("enabled", true);

            config.set(
                    "title",
                    "&6&lSiddy's SB"
            );

            config.set(
                    "lines.1",
                    "&7Welcome, &f%player%"
            );

            config.set(
                    "lines.2",
                    ""
            );

            config.set(
                    "lines.3",
                    "&fBalance: &a$%balance%"
            );

            config.set(
                    "lines.4",
                    "&fOnline: &e%online%&7/&e%max_players%"
            );

            config.set(
                    "lines.5",
                    ""
            );

            config.set(
                    "lines.6",
                    "&7World: &f%world%"
            );

            config.set(
                    "lines.7",
                    "&7XYZ: &f%x% %y% %z%"
            );

            try {
                config.save(configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void start() {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            createScoreboard(player);
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        updateScoreboard(player);
                    }
                },
                0L,
                20L
        );
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(
                    Bukkit.getScoreboardManager().getNewScoreboard()
            );
        }

        scoreboards.clear();
    }

    public void reload() {
        stop();

        config = YamlConfiguration.loadConfiguration(configFile);

        start();
    }

    private void createScoreboard(Player player) {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        Scoreboard scoreboard =
                Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective =
                scoreboard.registerNewObjective(
                        OBJECTIVE_NAME,
                        "dummy",
                        color(config.getString(
                                "title",
                                "&6&lCAVERNS SMP"
                        ))
                );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        scoreboards.put(
                player.getUniqueId(),
                scoreboard
        );

        player.setScoreboard(scoreboard);

        updateScoreboard(player);
    }

    private void updateScoreboard(Player player) {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        Scoreboard scoreboard =
                scoreboards.get(player.getUniqueId());

        if (scoreboard == null) {
            createScoreboard(player);
            return;
        }

        Objective objective =
                scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective == null) {
            createScoreboard(player);
            return;
        }

        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }

        Map<String, String> values =
                new HashMap<>();

        values.put(
                "%player%",
                player.getName()
        );

        values.put(
                "%balance%",
                String.valueOf(
                        economy.getPlayerBalance(player)
                )
        );

        values.put(
                "%online%",
                String.valueOf(
                        Bukkit.getOnlinePlayers().size()
                )
        );

        values.put(
                "%max_players%",
                String.valueOf(
                        Bukkit.getMaxPlayers()
                )
        );

        values.put(
                "%world%",
                player.getWorld().getName()
        );

        values.put(
                "%health%",
                String.valueOf(
                        Math.max(
                                0,
                                (int) Math.ceil(
                                        player.getHealth()
                                )
                        )
                )
        );

        values.put(
                "%x%",
                String.valueOf(
                        player.getLocation().getBlockX()
                )
        );

        values.put(
                "%y%",
                String.valueOf(
                        player.getLocation().getBlockY()
                )
        );

        values.put(
                "%z%",
                String.valueOf(
                        player.getLocation().getBlockZ()
                )
        );

        int lineNumber = 0;

        for (String key : config.getConfigurationSection("lines").getKeys(false)) {

            String line =
                    config.getString(
                            "lines." + key,
                            ""
                    );

            for (Map.Entry<String, String> value :
                    values.entrySet()) {

                line = line.replace(
                        value.getKey(),
                        value.getValue()
                );
            }

            line = color(line);

            String entry =
                    ChatColor.values()[
                            lineNumber % ChatColor.values().length
                            ].toString();

            String teamName =
                    TEAM_PREFIX + lineNumber;

            Team team =
                    scoreboard.registerNewTeam(teamName);

            team.addEntry(entry);
            team.setPrefix(line);

            objective.getScore(entry)
                    .setScore(
                            config.getConfigurationSection(
                                    "lines"
                            ).getKeys(false).size()
                                    - lineNumber
                    );

            lineNumber++;

            if (lineNumber >= 16) {
                break;
            }
        }

        player.setScoreboard(scoreboard);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> createScoreboard(event.getPlayer())
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboards.remove(
                event.getPlayer().getUniqueId()
        );
    }
}