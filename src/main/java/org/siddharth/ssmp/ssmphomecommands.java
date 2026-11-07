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

public class ssmphomecommands implements CommandExecutor {

    private final ssmphomes homes;

    public ssmphomecommands(ssmphomes homes) {
        this.homes = homes;
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

        if (!homes.isEnabled()) {
            player.sendMessage(
                    ChatColor.RED + "Homes are currently disabled."
            );
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "sethome":
                if (!player.hasPermission("ssmp.homes.sethome")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to set homes."
                    );
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(
                            ChatColor.RED + "Usage: /sethome <name>"
                    );
                    return true;
                }

                String setName = args[0].toLowerCase();

                if (!setName.matches("[a-z0-9_-]{1,32}")) {
                    player.sendMessage(
                            ChatColor.RED
                                    + "Home names may only contain letters, numbers, _ and -."
                    );
                    return true;
                }

                if (!homes.setHome(player, setName)) {
                    player.sendMessage(
                            ChatColor.RED
                                    + "You have reached your maximum number of homes."
                    );
                    return true;
                }

                player.sendMessage(
                        ChatColor.GREEN
                                + "Home "
                                + ChatColor.AQUA
                                + setName
                                + ChatColor.GREEN
                                + " set."
                );
                return true;

            case "home":
                if (!player.hasPermission("ssmp.homes.use")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to use homes."
                    );
                    return true;
                }

                if (args.length == 0) {
                    homes.openGUI(player);
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(
                            ChatColor.RED + "Usage: /home [name]"
                    );
                    return true;
                }

                String homeName = args[0].toLowerCase();

                if (!homes.teleportHome(player, homeName)) {
                    player.sendMessage(
                            ChatColor.RED
                                    + "That home doesn't exist or its world is unavailable."
                    );
                    return true;
                }

                player.sendMessage(
                        ChatColor.GREEN
                                + "Teleported to "
                                + ChatColor.AQUA
                                + homeName
                                + ChatColor.GREEN
                                + "."
                );
                return true;

            case "homes":
                if (!player.hasPermission("ssmp.homes.use")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to use homes."
                    );
                    return true;
                }

                homes.openGUI(player);
                return true;

            case "delhome":
                if (!player.hasPermission("ssmp.homes.delete")) {
                    player.sendMessage(
                            ChatColor.RED + "You don't have permission to delete homes."
                    );
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(
                            ChatColor.RED + "Usage: /delhome <name>"
                    );
                    return true;
                }

                String deleteName = args[0].toLowerCase();

                if (!homes.deleteHome(player, deleteName)) {
                    player.sendMessage(
                            ChatColor.RED
                                    + "That home doesn't exist."
                    );
                    return true;
                }

                player.sendMessage(
                        ChatColor.GREEN
                                + "Deleted home "
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