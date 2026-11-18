/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.ssmp;

public class commandSystem implements CommandExecutor {

    private final ssmp plugin;
    private final soundSystem sounds;
    private final menuSystem menu;

    public commandSystem(
            ssmp plugin,
            soundSystem sounds,
            menuSystem menu
    ) {
        this.plugin = plugin;
        this.sounds = sounds;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command cmd,
            String label,
            String[] args
    ) {

        if (args.length > 0
                && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("ssmp.admin")) {
                sender.sendMessage(
                        ChatColor.RED +
                                "You do not have permission to do this."
                );

                return true;
            }

            if (plugin.reloadSSMP()) {
                sender.sendMessage(
                        ChatColor.GREEN +
                                "SSMP configuration reloaded successfully."
                );
            } else {
                sender.sendMessage(
                        ChatColor.RED +
                                "SSMP configuration reload failed. Check console."
                );
            }

            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(
                    "[SORRY] This command can only be executed by players."
            );

            return true;
        }

        Player player = (Player) sender;

        menu.open(player);

        return true;
    }
}