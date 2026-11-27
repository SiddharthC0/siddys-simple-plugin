
package org.siddharth.ssmp.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.mechanics.WarpSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WarpCommands implements CommandExecutor, TabCompleter {
    private final WarpSystem warps;

    public WarpCommands(WarpSystem warps) {
        this.warps = warps;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use warps.");
            return true;
        }

        if (warps == null || !warps.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Module warps is currently disabled.");
            return true;
        }

        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "setwarp" -> {
                if (!player.hasPermission("ssmp.warps.set")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to set warps.");
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(ChatColor.RED + "Usage: /setwarp <name>");
                    return true;
                }

                String setName = args[0].toLowerCase(Locale.ROOT);

                if (!setName.matches("[a-z0-9_-]{1,32}")) {
                    player.sendMessage(ChatColor.RED + "Warp names may only contain letters, numbers, underscores and hyphens.");
                    return true;
                }

                if (!warps.setWarp(setName, player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "Failed to set warp.");
                    return true;
                }

                player.sendMessage(ChatColor.GREEN + "Warp " + ChatColor.AQUA + setName + ChatColor.GREEN + " set.");
                return true;
            }

            case "warp" -> {
                if (!player.hasPermission("ssmp.warps.use")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use warps.");
                    return true;
                }

                if (args.length == 0) {
                    warps.openGUI(player);
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(ChatColor.RED + "Usage: /warp [name]");
                    return true;
                }

                String warpName = args[0].toLowerCase(Locale.ROOT);

                if (!warps.teleportWarp(player, warpName)) {
                    player.sendMessage(ChatColor.RED + "That warp doesn't exist or its world is unavailable.");
                    return true;
                }

                player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.AQUA + warpName);
                return true;
            }

            case "warps" -> {
                if (!player.hasPermission("ssmp.warps.use")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use warps.");
                    return true;
                }

                warps.openGUI(player);
                return true;
            }

            case "delwarp" -> {
                if (!player.hasPermission("ssmp.warps.delete")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to delete warps.");
                    return true;
                }

                if (args.length != 1) {
                    player.sendMessage(ChatColor.RED + "Usage: /delwarp <name>");
                    return true;
                }

                String deleteName = args[0].toLowerCase(Locale.ROOT);

                if (!warps.deleteWarp(deleteName)) {
                    player.sendMessage(ChatColor.RED + "That warp doesn't exist.");
                    return true;
                }

                player.sendMessage(ChatColor.GREEN + "Deleted warp " + ChatColor.AQUA + deleteName);
                return true;
            }

            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || warps == null || !warps.isEnabled()) {
            return List.of();
        }

        if (args.length != 1) {
            return List.of();
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);

        if (!commandName.equals("warp") && !commandName.equals("delwarp")) {
            return List.of();
        }

        String permission = commandName.equals("warp")
                ? "ssmp.warps.use"
                : "ssmp.warps.delete";

        if (!player.hasPermission(permission)) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>(warps.getWarps());
        String input = args[0].toLowerCase(Locale.ROOT);

        return suggestions.stream()
                .filter(warp -> warp != null && warp.toLowerCase(Locale.ROOT).startsWith(input))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
