
/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardSystem implements Listener {

    private final JavaPlugin plugin;

    private final File scoreboardFolder;
    private final File configFile;
    private YamlConfiguration config;

    private PlayerInformationSystem playerInformationSystem;

    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();

    private BukkitTask updateTask;

    private static final String OBJECTIVE_NAME = "SiddysSimplePlugin";
    private static final String TEAM_PREFIX = "ssmp_line_";
    private static final String SETTING = "interface.scoreboard";

    public ScoreboardSystem(JavaPlugin plugin) {
        this.plugin = plugin;

        scoreboardFolder = new File(plugin.getDataFolder(), "scoreboard");
        configFile = new File(scoreboardFolder, "config.yml");

        createConfig();
    }

    public void setPlayerInformationSystem(PlayerInformationSystem system) {
        this.playerInformationSystem = system;

        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    private boolean isEnabled(Player player) {
        if (!config.getBoolean("enabled", true)) {
            return false;
        }

        return playerInformationSystem == null
                || playerInformationSystem.isEnabled(player, SETTING);
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

        config.addDefault("enabled", true);
        config.addDefault("title", "&6&lSiddy's SB");
        config.addDefault("lines.1", "&7Welcome, &f%player%");
        config.addDefault("lines.2", "");
        config.addDefault("lines.3", "&fBalance: &a$%ssmp_balance%");
        config.addDefault("lines.4", "&fOnline: &e%online%&7/&e%max_players%");
        config.addDefault("lines.5", "");
        config.addDefault("lines.6", "&7World: &f%world%");
        config.addDefault("lines.7", "&7XYZ: &f%x% %y% %z%");

        config.options().copyDefaults(true);

        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        refreshPlayer(player);
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
            removeScoreboard(player);
        }

        scoreboards.clear();
        previousScoreboards.clear();
    }

    public void reload() {
        stop();
        config = YamlConfiguration.loadConfiguration(configFile);
        start();
    }

    public void refreshPlayer(Player player) {
        if (!isEnabled(player)) {
            removeScoreboard(player);
            return;
        }

        if (!scoreboards.containsKey(player.getUniqueId())) {
            createScoreboard(player);
        } else {
            updateScoreboard(player);
        }
    }

    private void createScoreboard(Player player) {
        if (!isEnabled(player)) {
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                "dummy",
                color(config.getString("title", "&6&lSSMP"))
        );

        objective.numberFormat(
                io.papermc.paper.scoreboard.numbers.NumberFormat.blank()
        );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        previousScoreboards.putIfAbsent(
                player.getUniqueId(),
                player.getScoreboard()
        );

        scoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);

        updateScoreboard(player);
    }

    private void removeScoreboard(Player player) {
        UUID uuid = player.getUniqueId();

        Scoreboard scoreboard = scoreboards.remove(uuid);
        Scoreboard previous = previousScoreboards.remove(uuid);

        if (scoreboard != null && player.getScoreboard() == scoreboard) {
            player.setScoreboard(
                    previous != null
                            ? previous
                            : Bukkit.getScoreboardManager().getMainScoreboard()
            );
        }
    }

    private void updateScoreboard(Player player) {
        if (!isEnabled(player)) {
            removeScoreboard(player);
            return;
        }

        Scoreboard scoreboard = scoreboards.get(player.getUniqueId());

        if (scoreboard == null) {
            createScoreboard(player);
            return;
        }

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective == null) {
            removeScoreboard(player);
            createScoreboard(player);
            return;
        }

        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }

        Map<String, String> values = new HashMap<>();

        values.put("%player%", player.getName());
        values.put("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        values.put("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        values.put("%world%", player.getWorld().getName());

        values.put(
                "%health%",
                String.valueOf(Math.max(0, (int) Math.ceil(player.getHealth())))
        );

        values.put("%x%", String.valueOf(player.getLocation().getBlockX()));
        values.put("%y%", String.valueOf(player.getLocation().getBlockY()));
        values.put("%z%", String.valueOf(player.getLocation().getBlockZ()));

        int lineNumber = 0;

        if (config.getConfigurationSection("lines") == null) {
            return;
        }

        for (String key : config.getConfigurationSection("lines").getKeys(false)) {
            String line = config.getString("lines." + key, "");

            for (Map.Entry<String, String> value : values.entrySet()) {
                line = line.replace(value.getKey(), value.getValue());
            }

            line = line.replace("%balance%", "%ssmp_balance%");
            line = resolvePlaceholders(player, line);
            line = color(line);

            String entry = ChatColor.values()[
                    lineNumber % ChatColor.values().length
                    ].toString();

            String teamName = TEAM_PREFIX + lineNumber;

            Team team = scoreboard.registerNewTeam(teamName);
            team.addEntry(entry);
            team.setPrefix(line);

            objective.getScore(entry).setScore(
                    config.getConfigurationSection("lines").getKeys(false).size()
                            - lineNumber
            );

            lineNumber++;

            if (lineNumber >= 16) {
                break;
            }
        }

        player.setScoreboard(scoreboard);
    }

    private String resolvePlaceholders(Player player, String line) {
        if (plugin instanceof org.siddharth.ssmp.SiddysSimplePlugin ssmp) {
            line = ssmp.getPlaceholderSystem().replace(player, line);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                ClassLoader classLoader = Bukkit.getPluginManager()
                        .getPlugin("PlaceholderAPI")
                        .getClass()
                        .getClassLoader();

                Class<?> placeholderAPI = Class.forName(
                        "me.clip.placeholderapi.PlaceholderAPI",
                        true,
                        classLoader
                );

                Method method = placeholderAPI.getMethod(
                        "setPlaceholders",
                        OfflinePlayer.class,
                        String.class
                );

                Object result = method.invoke(null, player, line);

                if (result instanceof String) {
                    line = (String) result;
                }

            } catch (ReflectiveOperationException ignored) {
            }
        }

        return line;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(
                plugin,
                () -> refreshPlayer(event.getPlayer())
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        scoreboards.remove(uuid);
        previousScoreboards.remove(uuid);
    }
}
