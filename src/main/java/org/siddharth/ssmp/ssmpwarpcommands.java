/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ssmpwarpcommands implements CommandExecutor {

    private final ssmpwarps warps;

    public ssmpwarpcommands(ssmpwarps warps) {
        this.warps = warps;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(
                    ChatColor.RED + "Only players can use this command."
            );
            return true;
        }

        Player player = (Player) sender;

        if (!warps.isEnabled()) {
            player.sendMessage(
                    ChatColor.RED + "Warps are currently disabled."
            );
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "setwarp":
                if (!player.hasPermission("ssmp.warps.set")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to set warps."
                    );
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(
                            ChatColor.RED + "Usage: /setwarp <name>"
                    );
                    return true;
                }

                String setName = args[0].toLowerCase();

                if (!setName.matches("[a-z0-9_-]{1,32}")) {
                    player.sendMessage(
                            ChatColor.RED
                                    + "Warp names may only contain letters, numbers, _ and -."
                    );
                    return true;
                }

                if (!warps.setWarp(
                        setName,
                        player.getLocation()
                )) {
                    player.sendMessage(
                            ChatColor.RED + "Failed to set warp."
                    );
                    return true;
                }

                player.sendMessage(
                        ChatColor.GREEN
                                + "Warp "
                                + ChatColor.AQUA
                                + setName
                                + ChatColor.GREEN
                                + " set."
                );
                return true;

            case "warp":
                if (!player.hasPermission("ssmp.warps.use")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to use warps."
                    );
                    return true;
                }

                if (args.length == 0) {
                    warps.openGUI(player);
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(
                            ChatColor.RED + "Usage: /warp [name]"
                    );
                    return true;
                }

                String warpName = args[0].toLowerCase();

                if (!warps.teleportWarp(
                        player,
                        warpName
                )) {
                    player.sendMessage(
                            ChatColor.RED
                                    + "That warp doesn't exist or its world is unavailable."
                    );
                    return true;
                }

                player.sendMessage(
                        ChatColor.GREEN
                                + "Teleported to "
                                + ChatColor.AQUA
                                + warpName
                                + ChatColor.GREEN
                                + "."
                );
                return true;

            case "warps":
                if (!player.hasPermission("ssmp.warps.use")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to use warps."
                    );
                    return true;
                }

                warps.openGUI(player);
                return true;

            case "delwarp":
                if (!player.hasPermission("ssmp.warps.delete")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to delete warps."
                    );
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(
                            ChatColor.RED + "Usage: /delwarp <name>"
                    );
                    return true;
                }

                String deleteName = args[0].toLowerCase();

                if (!warps.deleteWarp(deleteName)) {
                    player.sendMessage(
                            ChatColor.RED + "That warp doesn't exist."
                    );
                    return true;
                }

                player.sendMessage(
                        ChatColor.GREEN
                                + "Deleted warp "
                                + ChatColor.AQUA
                                + deleteName
                                + ChatColor.GREEN
                                + "."
                );
                return true;
        }

        return false;
    }
}