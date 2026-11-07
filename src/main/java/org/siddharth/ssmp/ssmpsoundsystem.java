/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class ssmpsoundsystem {

    private final File soundsFolder;
    private final File soundsFile;
    private final YamlConfiguration sounds;

    public ssmpsoundsystem(ssmp plugin) {
        soundsFolder = new File(
                plugin.getDataFolder(),
                "sounds"
        );

        soundsFile = new File(
                soundsFolder,
                "config.yml"
        );

        createFiles();

        sounds =
                YamlConfiguration.loadConfiguration(
                        soundsFile
                );
    }

    private void createFiles() {
        if (!soundsFolder.exists()) {
            soundsFolder.mkdirs();
        }

        if (!soundsFile.exists()) {
            try {
                soundsFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(
                        soundsFile
                );

        boolean changed = false;

        changed |= addSound(
                config,
                "menu-open",
                true,
                "minecraft:block.chest.open",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "menu-click",
                true,
                "minecraft:ui.button.click",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "menu-close",
                true,
                "minecraft:block.chest.close",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "auction-click",
                true,
                "minecraft:ui.button.click",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "auction-success",
                true,
                "minecraft:entity.player.levelup",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "auction-error",
                true,
                "minecraft:entity.villager.no",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "shop-open",
                true,
                "minecraft:block.chest.open",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "shop-click",
                true,
                "minecraft:ui.button.click",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "shop-purchase",
                true,
                "minecraft:entity.experience_orb.pickup",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "shop-error",
                true,
                "minecraft:entity.villager.no",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "command-error",
                true,
                "minecraft:entity.villager.no",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "command-success",
                true,
                "minecraft:entity.player.levelup",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "notification",
                true,
                "minecraft:block.note_block.pling",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "menu-error",
                true,
                "minecraft:entity.villager.no",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "shop-success",
                true,
                "minecraft:entity.villager.no",
                1.0,
                1.0
        );

        changed |= addSound(
                config,
                "shop-error",
                true,
                "minecraft:entity.player.levelup",
                1.0,
                1.0
        );

        if (changed) {
            saveConfig(config);
        }
    }

    private boolean addSound(
            YamlConfiguration config,
            String name,
            boolean enabled,
            String sound,
            double volume,
            double pitch
    ) {
        String basePath =
                "sounds." + name;

        boolean changed = false;

        if (!config.contains(
                basePath + ".enabled"
        )) {
            config.set(
                    basePath + ".enabled",
                    enabled
            );

            changed = true;
        }

        if (!config.contains(
                basePath + ".sound"
        )) {
            config.set(
                    basePath + ".sound",
                    sound
            );

            changed = true;
        }

        if (!config.contains(
                basePath + ".volume"
        )) {
            config.set(
                    basePath + ".volume",
                    volume
            );

            changed = true;
        }

        if (!config.contains(
                basePath + ".pitch"
        )) {
            config.set(
                    basePath + ".pitch",
                    pitch
            );

            changed = true;
        }

        return changed;
    }

    public void play(
            Player player,
            String soundName
    ) {
        if (player == null ||
                soundName == null ||
                soundName.isBlank()) {
            return;
        }

        String basePath =
                "sounds." + soundName;

        if (!sounds.contains(
                basePath
        )) {
            return;
        }

        if (!sounds.getBoolean(
                basePath + ".enabled",
                false
        )) {
            return;
        }

        String soundString =
                sounds.getString(
                        basePath + ".sound"
                );

        if (soundString == null ||
                soundString.isBlank()) {

            log(
                    "Sound '" +
                            soundName +
                            "' has no sound configured."
            );

            return;
        }

        NamespacedKey key =
                NamespacedKey.fromString(
                        soundString
                );

        if (key == null) {
            log(
                    "Invalid sound key: " +
                            soundString
            );

            return;
        }

        var sound =
                Registry.SOUNDS.get(key);

        if (sound == null) {
            log(
                    "Sound not found: " +
                            soundString
            );

            return;
        }

        float volume =
                (float) sounds.getDouble(
                        basePath + ".volume",
                        1.0
                );

        float pitch =
                (float) sounds.getDouble(
                        basePath + ".pitch",
                        1.0
                );

        volume =
                Math.max(
                        0.0f,
                        volume
                );

        pitch =
                Math.max(
                        0.0f,
                        pitch
                );

        player.playSound(
                player.getLocation(),
                sound,
                volume,
                pitch
        );
    }

    public void reload() {
        try {
            sounds.load(
                    soundsFile
            );

            createFiles();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveConfig(
            YamlConfiguration config
    ) {
        try {
            config.save(
                    soundsFile
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void log(
            String message
    ) {
        System.out.println(
                "[SSMP] " + message
        );
    }
}