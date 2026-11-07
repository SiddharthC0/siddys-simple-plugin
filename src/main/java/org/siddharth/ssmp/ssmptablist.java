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
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class ssmptablist implements Listener {

    private final ssmp plugin;
    private final ssmpranks ranks;

    private BukkitTask updateTask;

    public ssmptablist(ssmp plugin, ssmpranks ranks) {
        this.plugin = plugin;
        this.ranks = ranks;
    }

    public void start() {
        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateAll,
                0L,
                10L
        );
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean(
                "modules.tablist.enabled",
                true
        )) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void update(Player player) {
        if (!plugin.getConfig().getBoolean(
                "modules.tablist.enabled",
                true
        )) {
            return;
        }

        boolean headerEnabled = plugin.getConfig().getBoolean(
                "modules.tablist.header.enabled",
                true
        );

        boolean footerEnabled = plugin.getConfig().getBoolean(
                "modules.tablist.footer.enabled",
                true
        );

        boolean playerListEnabled = plugin.getConfig().getBoolean(
                "modules.tablist.player-list.enabled",
                true
        );

        String header = headerEnabled
                ? formatLines(
                plugin.getConfig().getStringList(
                        "modules.tablist.header.lines"
                ),
                player
        )
                : "";

        String footer = footerEnabled
                ? formatLines(
                plugin.getConfig().getStringList(
                        "modules.tablist.footer.lines"
                ),
                player
        )
                : "";

        String playerFormat = plugin.getConfig().getString(
                "modules.tablist.player-list.format",
                "%player_prefix%%player_nickname%%player_suffix%"
        );

        String playerListName = format(
                playerFormat,
                player
        );

        player.setPlayerListHeader(header);
        player.setPlayerListFooter(footer);

        if (playerListEnabled) {
            player.setPlayerListName(playerListName);
        }
    }

    private String formatLines(
            List<String> lines,
            Player player
    ) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            result.append(
                    format(lines.get(i), player)
            );

            if (i < lines.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private String format(
            String text,
            Player player
    ) {
        if (text == null) {
            return "";
        }

        String prefix = ranks.getPlayerPrefix(player);
        String suffix = ranks.getPlayerSuffix(player);

        if (prefix == null) {
            prefix = "";
        }

        if (suffix == null) {
            suffix = "";
        }

        String nickname = player.getName();

        text = text
                .replace("%player_name%", player.getName())
                .replace("%player_nickname%", nickname)
                .replace("%player_prefix%", prefix)
                .replace("%player_suffix%", suffix)
                .replace(
                        "%online%",
                        String.valueOf(
                                Bukkit.getOnlinePlayers().size()
                        )
                )
                .replace(
                        "%max_players%",
                        String.valueOf(
                                Bukkit.getMaxPlayers()
                        )
                )
                .replace(
                        "%ping%",
                        String.valueOf(
                                player.getPing()
                        )
                );

        return ChatColor.translateAlternateColorCodes('&', text)
                .replace("§", "§");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(
                plugin,
                () -> update(event.getPlayer())
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTask(
                plugin,
                this::updateAll
        );
    }
}