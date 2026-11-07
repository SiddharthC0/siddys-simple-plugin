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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.configuration.file.YamlConfiguration;

public class ssmpnicknames {

    private final ssmp plugin;

    private final File nicknameFolder;
    private final File nicknameFile;
    private final YamlConfiguration nicknames;

    private final Map<UUID, String> nicknameData = new HashMap<>();
    private final Map<UUID, String> colorData = new HashMap<>();

    private static final int MAX_NICKNAME_LENGTH = 16;
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_ ]+$");

    public ssmpnicknames(ssmp plugin) {
        this.plugin = plugin;

        nicknameFolder = new File(
                plugin.getDataFolder(),
                "nicknames"
        );

        nicknameFile = new File(
                nicknameFolder,
                "nicknames.yml"
        );

        createFiles();

        nicknames = YamlConfiguration.loadConfiguration(
                nicknameFile
        );

        load();
    }

    private void createFiles() {
        if (!nicknameFolder.exists()
                && !nicknameFolder.mkdirs()) {
            throw new RuntimeException(
                    "Failed to create nicknames folder."
            );
        }

        if (!nicknameFile.exists()) {
            try {
                if (!nicknameFile.createNewFile()) {
                    throw new IOException(
                            "Could not create nicknames.yml."
                    );
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create nicknames.yml.",
                        e
                );
            }
        }
    }

    private void load() {
        nicknameData.clear();
        colorData.clear();

        if (!nicknames.isConfigurationSection("players")) {
            return;
        }

        for (String uuidString :
                nicknames.getConfigurationSection("players")
                        .getKeys(false)) {

            try {
                UUID uuid = UUID.fromString(uuidString);

                String nickname =
                        nicknames.getString(
                                "players." + uuidString + ".nickname"
                        );

                String color =
                        nicknames.getString(
                                "players." + uuidString + ".color"
                        );

                if (nickname != null && !nickname.isBlank()) {
                    nicknameData.put(
                            uuid,
                            nickname
                    );
                }

                if (color != null && !color.isBlank()) {
                    colorData.put(
                            uuid,
                            color
                    );
                }

            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public String getNickname(Player player) {
        if (player == null) {
            return null;
        }

        return getNickname(player.getUniqueId());
    }

    public String getNickname(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        return nicknameData.get(uuid);
    }

    public String getDisplayNickname(Player player) {
        if (player == null) {
            return "";
        }

        String nickname =
                getNickname(player);

        if (nickname == null || nickname.isBlank()) {
            nickname = player.getName();
        }

        return nickname;
    }

    public String getNicknameColor(Player player) {
        if (player == null) {
            return "";
        }

        return getNicknameColor(
                player.getUniqueId()
        );
    }

    public String getNicknameColor(UUID uuid) {
        if (uuid == null) {
            return "";
        }

        return colorData.getOrDefault(
                uuid,
                ""
        );
    }

    public String getFormattedNickname(Player player) {
        if (player == null) {
            return "";
        }

        String nickname =
                getDisplayNickname(player);

        String color =
                getNicknameColor(player);

        if (color == null || color.isBlank()) {
            return nickname;
        }

        return color + nickname;
    }

    public boolean hasNickname(Player player) {
        if (player == null) {
            return false;
        }

        return nicknameData.containsKey(
                player.getUniqueId()
        );
    }

    public boolean hasNickname(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        return nicknameData.containsKey(uuid);
    }

    public boolean setNickname(
            Player player,
            String nickname
    ) {
        if (player == null
                || nickname == null
                || !player.hasPermission(
                "ssmp.nickname.use"
        )) {
            return false;
        }

        nickname = nickname.trim();

        if (!isValidNickname(nickname)) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        nicknameData.put(
                uuid,
                nickname
        );

        saveNickname(uuid);

        return true;
    }

    public boolean removeNickname(Player player) {
        if (player == null
                || !player.hasPermission(
                "ssmp.nickname.use"
        )) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        nicknameData.remove(uuid);

        removeNicknameFromFile(uuid);

        return true;
    }

    public boolean setNicknameColor(
            Player player,
            String color
    ) {
        if (player == null
                || color == null
                || !player.hasPermission(
                "ssmp.nickname.color"
        )) {
            return false;
        }

        color = normalizeColor(color);

        if (!isValidColor(color)) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        colorData.put(
                uuid,
                color
        );

        saveColor(uuid);

        return true;
    }

    public boolean resetNicknameColor(Player player) {
        if (player == null
                || !player.hasPermission(
                "ssmp.nickname.color"
        )) {
            return false;
        }

        UUID uuid =
                player.getUniqueId();

        colorData.remove(uuid);

        removeColorFromFile(uuid);

        return true;
    }

    public boolean isValidNickname(String nickname) {
        if (nickname == null
                || nickname.isBlank()) {
            return false;
        }

        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            return false;
        }

        if (nickname.contains("§")
                || nickname.contains("&")) {
            return false;
        }

        return NICKNAME_PATTERN.matcher(
                nickname
        ).matches();
    }

    public boolean isValidColor(String color) {
        if (color == null
                || color.isBlank()) {
            return false;
        }

        if (color.length() != 2
                || color.charAt(0) != '&') {
            return false;
        }

        char code =
                Character.toLowerCase(
                        color.charAt(1)
                );

        return "0123456789abcdef".indexOf(code)
                >= 0;
    }

    private String normalizeColor(String color) {
        color = color.trim();

        if (color.startsWith("§")
                && color.length() == 2) {
            color =
                    "&" + color.charAt(1);
        }

        if (color.length() == 1) {
            color =
                    "&" + color;
        }

        return color.toLowerCase();
    }

    private void saveNickname(UUID uuid) {
        String path =
                "players." + uuid + ".nickname";

        nicknames.set(
                path,
                nicknameData.get(uuid)
        );

        save();
    }

    private void saveColor(UUID uuid) {
        String path =
                "players." + uuid + ".color";

        nicknames.set(
                path,
                colorData.get(uuid)
        );

        save();
    }

    private void removeNicknameFromFile(UUID uuid) {
        nicknames.set(
                "players." + uuid + ".nickname",
                null
        );

        cleanupPlayerSection(uuid);

        save();
    }

    private void removeColorFromFile(UUID uuid) {
        nicknames.set(
                "players." + uuid + ".color",
                null
        );

        cleanupPlayerSection(uuid);

        save();
    }

    private void cleanupPlayerSection(UUID uuid) {
        String path =
                "players." + uuid;

        if (nicknames.isConfigurationSection(path)
                && nicknames.getConfigurationSection(path)
                .getKeys(false)
                .isEmpty()) {

            nicknames.set(
                    path,
                    null
            );
        }
    }

    private void save() {
        try {
            nicknames.save(
                    nicknameFile
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to save nicknames.yml.",
                    e
            );
        }
    }

    public void reload() {
        nicknames.getValues(false).clear();

        YamlConfiguration loaded =
                YamlConfiguration.loadConfiguration(
                        nicknameFile
                );

        nicknameData.clear();
        colorData.clear();

        if (!loaded.isConfigurationSection("players")) {
            return;
        }

        for (String uuidString :
                loaded.getConfigurationSection("players")
                        .getKeys(false)) {

            try {
                UUID uuid =
                        UUID.fromString(uuidString);

                String nickname =
                        loaded.getString(
                                "players." + uuidString + ".nickname"
                        );

                String color =
                        loaded.getString(
                                "players." + uuidString + ".color"
                        );

                if (nickname != null
                        && !nickname.isBlank()) {
                    nicknameData.put(
                            uuid,
                            nickname
                    );
                }

                if (color != null
                        && !color.isBlank()) {
                    colorData.put(
                            uuid,
                            color
                    );
                }

            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public File getNicknameFile() {
        return nicknameFile;
    }

    public int getMaxNicknameLength() {
        return MAX_NICKNAME_LENGTH;
    }
}