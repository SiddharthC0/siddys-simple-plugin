package org.siddharth.ssmp.commands;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.mechanics.nicknameSystem;
import java.util.ArrayList;
import java.util.List;
// Dev note: Yes, i did not use switch()

public class nicknameCommands implements CommandExecutor, TabCompleter {
    private final nicknameSystem nicknames;
    private static final List<String> COLORS = List.of(
            "black",
            "dark_blue",
            "dark_green",
            "dark_aqua",
            "dark_red",
            "dark_purple",
            "gold",
            "gray",
            "dark_gray",
            "blue",
            "green",
            "aqua",
            "red",
            "light_purple",
            "yellow",
            "white"
    );

    public nicknameCommands(nicknameSystem nicknames) {
        this.nicknames = nicknames;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can interact with nick system.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /nick <nickname|off|color> : Configures the nickname");
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            if (!nicknames.removeNickname(player)) {
                player.sendMessage(ChatColor.RED + "You don't have permission to turn your nickname off");
                return true;
            }
            player.sendMessage(ChatColor.GREEN + "Your nickname has turned off.");
            return true;
        } else if (args[0].equalsIgnoreCase("color")) {
            if (!player.hasPermission("ssmp.nickname.color")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to change your nickname color.");
                return true;
            } else if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /nick color <color|reset> : Changes the color of your nickname to <color> or just use the arg reset to default it");
                return true;
            } else if (args[1].equalsIgnoreCase("reset")) {
                nicknames.resetNicknameColor(player);
                player.sendMessage(ChatColor.GREEN + "Your nickname color has been reset.");
                return true;
            }
            String color = args[1];
            if (!nicknames.isValidColor(color.startsWith("&") ? color : "&" + color)) {
                player.sendMessage(ChatColor.RED + "The provided nickname color is invalid...");
                player.sendMessage(ChatColor.GRAY + "Available colors: &0 &1 &2 &3 &4 &5 &6 &7 &8 &9 &a &b &c &d &e &f");
                return true;
            }
            if (!color.startsWith("&")) {
                color = "&" + color;
            }
            nicknames.setNicknameColor(player, color);
            player.sendMessage(ChatColor.GREEN + "Your nickname color has been changed.");
            return true;
        } else if (!player.hasPermission("ssmp.nickname.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use nicknames.");
            return true;
        }

        String nickname = String.join(" ", args);
        if (!nicknames.isValidNickname(nickname)) {
            player.sendMessage(ChatColor.RED + "Invalid nickname.");
            player.sendMessage(ChatColor.GRAY + "Nickname must be 1-" + nicknames.getMaxNicknameLength() + " characters and may only contain letters, numbers, spaces and underscores.");
            return true;
        } else if (!nicknames.setNickname(player, nickname)) {
            player.sendMessage(ChatColor.RED + "Failed to set your nickname.");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Your nickname is set to " + nicknames.getFormattedNickname(player));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            if (player.hasPermission("ssmp.nickname.use")) {
                suggestions.add("off");
            }

            if (player.hasPermission("ssmp.nickname.color")) {
                suggestions.add("color");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("color")) {
            if (!player.hasPermission("ssmp.nickname.color")) {
                return List.of();
            }

            suggestions.addAll(COLORS);
            suggestions.add("reset");
        }
        String current = args[args.length - 1].toLowerCase();
        return suggestions.stream()
                .filter(suggestion -> suggestion.startsWith(current))
                .toList();
    }
}
