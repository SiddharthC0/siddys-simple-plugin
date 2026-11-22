package org.siddharth.ssmp.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.mechanics.HomeSystem;

public class HomeCommands implements CommandExecutor {
    private final HomeSystem homes;
    public HomeCommands(HomeSystem homes) {
        this.homes = homes;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players...");
            return true;
        }

        Player player = (Player) sender;
        if (!homes.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Homes are currently disabled.");
            return true;
        }

        String lCommand = command.getName().toLowerCase();
        if (lCommand.equals("sethome")) {
            if (!player.hasPermission("ssmp.homes.sethome")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to set homes.");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /sethome <name> : To set home at your current coordinates");
                return true;
            }

            String initNameOfHome = args[0].toLowerCase();
            if (!initNameOfHome.matches("[a-z0-9_-]{1,32}")) {
                player.sendMessage(ChatColor.RED + "Name of home is in invalid format, avoid using special characters.");
                return true;
            }
            if (!homes.setHome(player, initNameOfHome)) {
                player.sendMessage(ChatColor.RED + "You have reached max number of homes.");
                return true;
            }
            player.sendMessage(ChatColor.AQUA + initNameOfHome + ChatColor.GREEN + " has been set.");
            return true;
        } else if (lCommand.equals("home")) {
            if (!player.hasPermission("ssmp.homes.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use homes.");
                return true;
            }

            if (args.length == 0) {
                homes.openGUI(player);
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /home [name]");
                return true;
            }

            String nameOfHome = args[0].toLowerCase();
            if (!homes.teleportHome(player, nameOfHome)) {
                player.sendMessage(ChatColor.RED + "That home doesn't exist or its world is unavailable.");
                return true;
            }
            player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.AQUA + nameOfHome);
            return true;
        } else if (lCommand.equals("homes")) {
            if (!player.hasPermission("ssmp.homes.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use homes.");
                return true;
            }
            homes.openGUI(player);
            return true;
        } else if (lCommand.equals("delhome")) {
            if (!player.hasPermission("ssmp.homes.delete")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to delete homes.");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /delhome <name>");
                return true;
            }

            String nameOfHomeToDelete = args[0].toLowerCase();
            if (!homes.deleteHome(player, nameOfHomeToDelete)) {
                player.sendMessage(ChatColor.RED + "That home doesn't exist.");
                return true;
            }
            player.sendMessage(nameOfHomeToDelete + ChatColor.GREEN + " has been deleted.");
            return true;
        }
        return false;
    }
}