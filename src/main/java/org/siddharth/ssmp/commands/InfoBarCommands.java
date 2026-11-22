package org.siddharth.ssmp.commands;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.gui.InfoBarGUI;
import org.siddharth.ssmp.mechanics.InfoBarSystem;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InfoBarCommands implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final InfoBarSystem system;
    private final InfoBarGUI gui;
    public InfoBarCommands(JavaPlugin plugin, InfoBarSystem system, InfoBarGUI gui) {
        this.plugin = plugin;
        this.system = system;
        this.gui = gui;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!system.isAvailable()) {
            sender.sendMessage(color("&cInfoBar is currently unavailable."));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color("&cThis command requires a player."));
                return true;
            }

            gui.open(player);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "on" -> {
                if (!requirePlayer(sender)) return true; // i just realised writing like this is possible :(

                Player player = (Player) sender;
                system.setPlayerEnabled(player, true);
                player.sendMessage(color("&aInfoBar enabled."));
            }

            case "off" -> {if (!requirePlayer(sender)) return true;
                Player player = (Player) sender;
                system.setPlayerEnabled(player, false);
                player.sendMessage(color("&cInfoBar disabled."));
            }
            case "toggle" -> {if (!requirePlayer(sender)) return true;
                Player player = (Player) sender;
                system.togglePlayer(player);
                player.sendMessage(color(system.isPlayerEnabled(player) ? "&aInfoBar enabled." : "&cInfoBar disabled."));
            }
            case "reload" -> {if (!sender.hasPermission("ssmp.infobar.reload")) {
                    sender.sendMessage(color("&cYou don't have permission."));
                    return true;
                }
                try {
                    system.reload();
                    sender.sendMessage(color("&aInfoBar configuration reloaded."));
                } catch (Exception e) {
                    sender.sendMessage(color("&cInfoBar reload failed. Check console."));
                    plugin.getLogger().warning("InfoBar reload failed: " + e.getMessage());
                }
            }
            default -> {sender.sendMessage(color("&cUsage: /infobar [on/off/toggle/reload]"));}
        }

        return true;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage(color("&cThis command requires a player."));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length != 1) return completions;
        String input = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>(List.of("on", "off", "toggle"));
        if (sender.hasPermission("ssmp.infobar.reload")) options.add("reload");
        for (String option : options) if (option.startsWith(input)) completions.add(option);
        return completions;
    }
}
// wow look at how efficient and ez those if statements became and also for