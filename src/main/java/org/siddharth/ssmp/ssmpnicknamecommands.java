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

public class ssmpnicknamecommands implements CommandExecutor {

    private final ssmpnicknames nicknames;

    public ssmpnicknamecommands(ssmpnicknames nicknames) {
        this.nicknames = nicknames;
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
                    ChatColor.RED +
                            "Only players can use this command."
            );
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(
                    ChatColor.YELLOW +
                            "Usage: /nick <nickname|off|color>"
            );
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {

            if (!nicknames.removeNickname(player)) {
                player.sendMessage(
                        ChatColor.RED +
                                "You don't have permission to remove your nickname."
                );
                return true;
            }

            player.sendMessage(
                    ChatColor.GREEN +
                            "Your nickname has been removed."
            );

            return true;
        }

        if (args[0].equalsIgnoreCase("color")) {

            if (!player.hasPermission(
                    "ssmp.nickname.color"
            )) {
                player.sendMessage(
                        ChatColor.RED +
                                "You don't have permission to change your nickname color."
                );
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(
                        ChatColor.YELLOW +
                                "Usage: /nick color <color|reset>"
                );
                return true;
            }

            if (args[1].equalsIgnoreCase("reset")) {

                nicknames.resetNicknameColor(player);

                player.sendMessage(
                        ChatColor.GREEN +
                                "Your nickname color has been reset."
                );

                return true;
            }

            String color = args[1];

            if (!nicknames.isValidColor(
                    color.startsWith("&")
                            ? color
                            : "&" + color
            )) {
                player.sendMessage(
                        ChatColor.RED +
                                "Invalid nickname color."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                                "Available colors: &0 &1 &2 &3 &4 &5 &6 &7 &8 &9 &a &b &c &d &e &f"
                );

                return true;
            }

            if (!color.startsWith("&")) {
                color = "&" + color;
            }

            nicknames.setNicknameColor(
                    player,
                    color
            );

            player.sendMessage(
                    ChatColor.GREEN +
                            "Your nickname color has been changed."
            );

            return true;
        }

        if (!player.hasPermission(
                "ssmp.nickname.use"
        )) {
            player.sendMessage(
                    ChatColor.RED +
                            "You don't have permission to use nicknames."
            );
            return true;
        }

        String nickname = String.join(
                " ",
                args
        );

        if (!nicknames.isValidNickname(nickname)) {
            player.sendMessage(
                    ChatColor.RED +
                            "Invalid nickname."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                            "Nickname must be 1-" +
                            nicknames.getMaxNicknameLength() +
                            " characters and may only contain letters, numbers, spaces and underscores."
            );

            return true;
        }

        if (!nicknames.setNickname(
                player,
                nickname
        )) {
            player.sendMessage(
                    ChatColor.RED +
                            "Failed to set your nickname."
            );
            return true;
        }

        player.sendMessage(
                ChatColor.GREEN +
                        "Your nickname is now " +
                        nicknames.getFormattedNickname(player)
        );

        return true;
    }
}