
/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */

package org.siddharth.ssmp.cpanel;

import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

final class ssmpManifest {
    private final Path path;
    private final Map<String, String> values = new LinkedHashMap<>();
    private static final int ITERATIONS = 210000;
    private static final int KEY_LENGTH = 256;

    ssmpManifest(Path path) {
        this.path = path;
    }

    synchronized void loadOrCreate(JavaPlugin plugin) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());

        if (!Files.exists(path)) {
            String initialPassword = ssmpCPanel.createInitialPassword();
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            values.put("enabled", "true");
            values.put("host", "127.0.0.1");
            values.put("port", "8765");
            values.put("theme", "dark");
            values.put("password.salt", Base64.getEncoder().encodeToString(salt));

            char[] password = initialPassword.toCharArray();

            try {
                values.put("password.hash", hash(password, salt));
            } catch (Exception exception) {
                throw new IOException("Could not create the initial panel password.", exception);
            } finally {
                java.util.Arrays.fill(password, '\0');
            }

            save();

            plugin.getLogger().warning("== == == == == == Important message == == == == ==");
            plugin.getLogger().warning("[SSMP Control Panel] Initial setup completed.");
            plugin.getLogger().warning("[SSMP Control Panel] INITIAL PASSWORD: " + initialPassword);
            plugin.getLogger().warning("Change this password after your first login.");
            plugin.getLogger().warning("The password will not be printed again.");
            plugin.getLogger().warning("== == == == == == Be careful ok? == == == == ==");

            return;
        }

        values.clear();

        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            int separator = trimmed.indexOf('=');

            if (separator <= 0) continue;

            values.put(
                    trimmed.substring(0, separator).trim(),
                    trimmed.substring(separator + 1).trim()
            );
        }

        values.putIfAbsent("enabled", "true");
        values.putIfAbsent("host", "127.0.0.1");
        values.putIfAbsent("port", "8765");
        values.putIfAbsent("theme", "dark");
        values.putIfAbsent("password.salt", "");
        values.putIfAbsent("password.hash", "");

        save();
    }

    synchronized void save() throws IOException {
        Path parent = path.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder builder = new StringBuilder();

        builder.append("# SSMP Control Panel manifest. Values use key=value format.\n");
        builder.append("# Keep this file private. The password is stored as a salted PBKDF2 hash.\n");

        values.forEach((key, value) ->
                builder.append(key).append('=').append(value).append('\n')
        );

        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

        Files.writeString(temporary, builder.toString(), StandardCharsets.UTF_8);

        try {
            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException exception) {
            Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    synchronized String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    synchronized int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(values.get(key));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    synchronized boolean isEnabled() {
        return Boolean.parseBoolean(values.getOrDefault("enabled", "true"));
    }

    synchronized String getPasswordHash() {
        return values.getOrDefault("password.hash", "");
    }

    synchronized boolean verifyPassword(String password) {
        if (password == null) return false;

        try {
            byte[] salt = Base64.getDecoder().decode(values.get("password.salt"));
            byte[] expected = Base64.getDecoder().decode(values.get("password.hash"));

            char[] characters = password.toCharArray();

            try {
                byte[] actual = Base64.getDecoder().decode(hash(characters, salt));
                return MessageDigest.isEqual(expected, actual);
            } finally {
                java.util.Arrays.fill(characters, '\0');
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    synchronized void changePassword(String password) throws Exception {
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters."
            );
        }

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        char[] characters = password.toCharArray();

        try {
            String encodedHash = hash(characters, salt);

            values.put("password.salt", Base64.getEncoder().encodeToString(salt));
            values.put("password.hash", encodedHash);

            save();
        } finally {
            java.util.Arrays.fill(characters, '\0');
        }
    }

    private static String hash(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        try {
            byte[] encoded = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

            return Base64.getEncoder().encodeToString(encoded);
        } finally {
            spec.clearPassword();
        }
    }
}
