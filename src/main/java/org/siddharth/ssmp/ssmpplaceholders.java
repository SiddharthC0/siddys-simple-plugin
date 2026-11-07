/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ssmpplaceholders {

    private final ssmp plugin;
    private final ssmpranks ranks;
    private final Map<String, Function<Player, String>> placeholders = new HashMap<>();

    private double tps = 20.0;
    private long lastTickTime = System.nanoTime();
    private BukkitTask tpsTask;

    public ssmpplaceholders(ssmp plugin, ssmpranks ranks) {
        this.plugin = plugin;
        this.ranks = ranks;

        registerDefaults();
        startTpsTracker();
    }

    private void startTpsTracker() {
        lastTickTime = System.nanoTime();

        tpsTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    long now = System.nanoTime();
                    long elapsed = now - lastTickTime;
                    lastTickTime = now;

                    if (elapsed <= 0) {
                        return;
                    }

                    double currentTps = 1_000_000_000.0 / elapsed;

                    if (currentTps > 20.0) {
                        currentTps = 20.0;
                    }

                    tps = (tps * 0.8) + (currentTps * 0.2);
                },
                1L,
                1L
        );
    }

    public void stop() {
        if (tpsTask != null) {
            tpsTask.cancel();
            tpsTask = null;
        }
    }

    private void registerDefaults() {
        register("player", Player::getName);
        register("player_name", Player::getName);

        register(
                "player_health",
                player -> String.valueOf(
                        Math.round(player.getHealth() * 10.0) / 10.0
                )
        );

        register(
                "player_max_health",
                player -> String.valueOf(
                        Math.round(
                                player.getAttribute(
                                        org.bukkit.attribute.Attribute.MAX_HEALTH
                                ).getValue() * 10.0
                        ) / 10.0
                )
        );

        register(
                "player_food",
                player -> String.valueOf(
                        player.getFoodLevel()
                )
        );

        register(
                "player_level",
                player -> String.valueOf(
                        player.getLevel()
                )
        );

        register(
                "player_world",
                player -> player.getWorld().getName()
        );

        register(
                "player_x",
                player -> String.valueOf(
                        player.getLocation().getBlockX()
                )
        );

        register(
                "player_y",
                player -> String.valueOf(
                        player.getLocation().getBlockY()
                )
        );

        register(
                "player_z",
                player -> String.valueOf(
                        player.getLocation().getBlockZ()
                )
        );

        register(
                "ssmp_player_rank",
                ranks::getPlayerRank
        );

        register(
                "ssmp_player_rank_name",
                ranks::getPlayerRankDisplayName
        );

        register(
                "ssmp_player_prefix",
                ranks::getPlayerPrefix
        );

        register(
                "ssmp_player_suffix",
                ranks::getPlayerSuffix
        );

        register(
                "server_online",
                player -> String.valueOf(
                        Bukkit.getOnlinePlayers().size()
                )
        );

        register(
                "server_max_players",
                player -> String.valueOf(
                        Bukkit.getMaxPlayers()
                )
        );

        register(
                "server_tps",
                player -> String.valueOf(
                        Math.round(tps * 100.0) / 100.0
                )
        );
    }

    public void register(
            String name,
            Function<Player, String> provider
    ) {
        if (name == null
                || name.isBlank()
                || provider == null) {
            return;
        }

        String key = name
                .replace("%", "")
                .toLowerCase();

        placeholders.put(
                key,
                provider
        );
    }

    public void unregister(
            String name
    ) {
        if (name == null
                || name.isBlank()) {
            return;
        }

        placeholders.remove(
                name
                        .replace("%", "")
                        .toLowerCase()
        );
    }

    public boolean exists(
            String name
    ) {
        if (name == null
                || name.isBlank()) {
            return false;
        }

        return placeholders.containsKey(
                name
                        .replace("%", "")
                        .toLowerCase()
        );
    }

    public String replace(
            Player player,
            String text
    ) {
        if (text == null
                || text.isEmpty()) {
            return text;
        }

        String result = text;

        for (Map.Entry<String, Function<Player, String>> entry :
                placeholders.entrySet()) {

            String value;

            try {
                value = entry.getValue().apply(player);
            } catch (Exception e) {
                value = "";
            }

            if (value == null) {
                value = "";
            }

            result = result.replace(
                    "%" + entry.getKey() + "%",
                    value
            );
        }

        return result;
    }

    public Map<String, Function<Player, String>> getPlaceholders() {
        return Map.copyOf(placeholders);
    }
}