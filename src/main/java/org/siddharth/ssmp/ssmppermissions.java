/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ssmppermissions {

    private final File permissionFolder;
    private final File permissionsFile;
    private final File playersFile;

    private final YamlConfiguration permissions;
    private final YamlConfiguration players;

    private final ssmpranks ranks;

    public ssmppermissions(
            ssmp plugin,
            ssmpranks ranks
    ) {
        this.ranks = ranks;

        permissionFolder = new File(
                plugin.getDataFolder(),
                "permissions"
        );

        permissionsFile = new File(
                permissionFolder,
                "permissions.yml"
        );

        playersFile = new File(
                permissionFolder,
                "players.yml"
        );

        createFiles();

        permissions = YamlConfiguration.loadConfiguration(
                permissionsFile
        );

        players = YamlConfiguration.loadConfiguration(
                playersFile
        );
    }

    private void createFiles() {
        if (!permissionFolder.exists()) {
            permissionFolder.mkdirs();
        }

        try {
            if (!permissionsFile.exists()) {
                permissionsFile.createNewFile();
            }

            if (!playersFile.exists()) {
                playersFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasPermission(
            Player player,
            String permission
    ) {
        if (player == null
                || permission == null
                || permission.isBlank()) {
            return false;
        }

        if (player.isOp()) {
            return true;
        }

        permission = permission.toLowerCase();

        Set<String> denied = new HashSet<>();
        Set<String> granted = new HashSet<>();

        collectPlayerPermissions(
                player,
                granted,
                denied
        );

        collectRankPermissions(
                ranks.getPlayerRank(player),
                granted,
                denied,
                new HashSet<>()
        );

        if (isDenied(permission, denied)) {
            return false;
        }

        return isGranted(permission, granted);
    }
    private void collectPlayerPermissions(
            Player player,
            Set<String> granted,
            Set<String> denied
    ) {
        String path =
                "players."
                        + player.getUniqueId()
                        + ".permissions";

        for (String permission :
                players.getStringList(path)) {

            addPermission(
                    permission,
                    granted,
                    denied
            );
        }
    }

    private void collectRankPermissions(
            String rank,
            Set<String> granted,
            Set<String> denied,
            Set<String> visited
    ) {
        if (rank == null
                || rank.isBlank()
                || visited.contains(rank.toLowerCase())) {
            return;
        }

        rank = rank.toLowerCase();

        if (!ranks.rankExists(rank)) {
            return;
        }

        visited.add(rank);

        String permissionPath =
                "ranks."
                        + rank
                        + ".permissions";

        for (String permission :
                permissions.getStringList(permissionPath)) {

            addPermission(
                    permission,
                    granted,
                    denied
            );
        }

        for (String inheritedRank :
                ranks.getInheritedRanks(rank)) {

            collectRankPermissions(
                    inheritedRank,
                    granted,
                    denied,
                    visited
            );
        }
    }

    private void addPermission(
            String permission,
            Set<String> granted,
            Set<String> denied
    ) {
        if (permission == null || permission.isBlank()) {
            return;
        }

        permission = permission
                .trim()
                .toLowerCase();

        if (permission.startsWith("-")) {
            denied.add(
                    permission.substring(1)
            );
            return;
        }

        granted.add(permission);
    }

    private boolean isDenied(
            String permission,
            Set<String> denied
    ) {
        if (denied.contains(permission)) {
            return true;
        }

        for (String deniedPermission : denied) {
            if (matchesWildcard(
                    deniedPermission,
                    permission
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean isGranted(
            String permission,
            Set<String> granted
    ) {
        if (granted.contains("*")) {
            return true;
        }

        if (granted.contains(permission)) {
            return true;
        }

        for (String grantedPermission : granted) {
            if (matchesWildcard(
                    grantedPermission,
                    permission
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesWildcard(
            String pattern,
            String permission
    ) {
        if (pattern.equals("*")) {
            return true;
        }

        if (pattern.equals(permission)) {
            return true;
        }

        if (!pattern.endsWith(".*")) {
            return false;
        }

        String prefix = pattern.substring(
                0,
                pattern.length() - 2
        );

        return permission.equals(prefix)
                || permission.startsWith(prefix + ".");
    }

    public void addPlayerPermission(
            Player player,
            String permission
    ) {
        if (permission == null
                || permission.isBlank()) {
            return;
        }

        String path =
                "players."
                        + player.getUniqueId()
                        + ".permissions";

        List<String> permissionsList =
                players.getStringList(path);

        String normalized =
                permission.trim().toLowerCase();

        if (!permissionsList.contains(normalized)) {
            permissionsList.add(normalized);

            players.set(
                    path,
                    permissionsList
            );

            savePlayers();
        }
    }

    public void removePlayerPermission(
            Player player,
            String permission
    ) {
        if (permission == null
                || permission.isBlank()) {
            return;
        }

        String path =
                "players."
                        + player.getUniqueId()
                        + ".permissions";

        List<String> permissionsList =
                players.getStringList(path);

        permissionsList.remove(
                permission.trim().toLowerCase()
        );

        players.set(
                path,
                permissionsList
        );

        savePlayers();
    }

    public List<String> getPlayerPermissions(
            Player player
    ) {
        return players.getStringList(
                "players."
                        + player.getUniqueId()
                        + ".permissions"
        );
    }

    public List<String> getRankPermissions(
            String rank
    ) {
        if (!ranks.rankExists(rank)) {
            return List.of();
        }

        return permissions.getStringList(
                "ranks."
                        + rank.toLowerCase()
                        + ".permissions"
        );
    }

    public void setRankPermissions(
            String rank,
            List<String> permissionList
    ) {
        if (!ranks.rankExists(rank)) {
            return;
        }

        permissions.set(
                "ranks."
                        + rank.toLowerCase()
                        + ".permissions",
                permissionList
        );

        savePermissions();
    }

    public void addRankPermission(
            String rank,
            String permission
    ) {
        if (!ranks.rankExists(rank)
                || permission == null
                || permission.isBlank()) {
            return;
        }

        String path =
                "ranks."
                        + rank.toLowerCase()
                        + ".permissions";

        List<String> permissionList =
                permissions.getStringList(path);

        String normalized =
                permission.trim().toLowerCase();

        if (!permissionList.contains(normalized)) {
            permissionList.add(normalized);

            permissions.set(
                    path,
                    permissionList
            );

            savePermissions();
        }
    }

    public void removeRankPermission(
            String rank,
            String permission
    ) {
        if (!ranks.rankExists(rank)
                || permission == null
                || permission.isBlank()) {
            return;
        }

        String path =
                "ranks."
                        + rank.toLowerCase()
                        + ".permissions";

        List<String> permissionList =
                permissions.getStringList(path);

        permissionList.remove(
                permission.trim().toLowerCase()
        );

        permissions.set(
                path,
                permissionList
        );

        savePermissions();
    }

    private void savePermissions() {
        try {
            permissions.save(
                    permissionsFile
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void savePlayers() {
        try {
            players.save(
                    playersFile
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}