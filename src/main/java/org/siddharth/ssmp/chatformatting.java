/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class chatformatting implements Listener {

    private final ssmp plugin;
    private final ssmpranks ranks;

    private final File formattingFolder;
    private final File formattingFile;
    private final YamlConfiguration formatting;

    private String format;

    private final List<Function<Player, String>> prefixProviders =
            new ArrayList<>();

    private final List<Function<Player, String>> suffixProviders =
            new ArrayList<>();

    private Function<Player, String> nameProvider;

    private final ssmpsoundsystem sounds;

    public chatformatting(
            ssmp plugin,
            ssmpranks ranks,
            ssmpsoundsystem sounds
    ) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.sounds = sounds;

        formattingFolder = new File(
                plugin.getDataFolder(),
                "chatformatting"
        );

        formattingFile = new File(
                formattingFolder,
                "config.yml"
        );

        createFiles();

        formatting = YamlConfiguration.loadConfiguration(
                formattingFile
        );

        format = formatting.getString(
                "format",
                "%ssmp_player_prefix%%player%%ssmp_player_suffix% &8» &f%message%"
        );
    }

    private void createFiles() {
        if (!formattingFolder.exists()) {
            formattingFolder.mkdirs();
        }

        if (!formattingFile.exists()) {
            try {
                formattingFile.createNewFile();

                YamlConfiguration config =
                        YamlConfiguration.loadConfiguration(
                                formattingFile
                        );

                config.set(
                        "format",
                        "%ssmp_player_prefix%%player%%ssmp_player_suffix% &8» &f%message%"
                );

                config.save(formattingFile);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        String prefix = ranks.getPlayerPrefix(player);
        String suffix = ranks.getPlayerSuffix(player);

        if (prefix == null) {
            prefix = "";
        }

        if (suffix == null) {
            suffix = "";
        }

        for (Function<Player, String> provider :
                prefixProviders) {

            String value =
                    provider.apply(player);

            if (value != null) {
                prefix += value;
            }
        }

        for (Function<Player, String> provider :
                suffixProviders) {

            String value =
                    provider.apply(player);

            if (value != null) {
                suffix += value;
            }
        }

        String playerName = player.getName();

        if (nameProvider != null) {
            String providedName =
                    nameProvider.apply(player);

            if (providedName != null
                    && !providedName.isBlank()) {
                playerName = providedName;
            }
        }

        String chatFormat =
                format
                        .replace(
                                "%player%",
                                playerName
                        )
                        .replace(
                                "%ssmp_player_prefix%",
                                prefix
                        )
                        .replace(
                                "%ssmp_player_suffix%",
                                suffix
                        )
                        .replace(
                                "%message%",
                                "%2$s"
                        );

        chatFormat =
                ChatColor.translateAlternateColorCodes(
                        '&',
                        chatFormat
                );

        event.setFormat(chatFormat);

        for (Player recipient : event.getRecipients()) {
            sounds.play(
                    recipient,
                    "message"
            );
        }
    }

    public void setFormat(String format) {
        if (format == null || format.isBlank()) {
            return;
        }

        this.format = format;

        formatting.set(
                "format",
                format
        );

        save();
    }

    public String getFormat() {
        return format;
    }

    public void registerPrefixProvider(
            Function<Player, String> provider
    ) {
        if (provider != null) {
            prefixProviders.add(provider);
        }
    }

    public void registerSuffixProvider(
            Function<Player, String> provider
    ) {
        if (provider != null) {
            suffixProviders.add(provider);
        }
    }

    public void setNameProvider(
            Function<Player, String> provider
    ) {
        this.nameProvider = provider;
    }

    private void save() {
        try {
            formatting.save(
                    formattingFile
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}