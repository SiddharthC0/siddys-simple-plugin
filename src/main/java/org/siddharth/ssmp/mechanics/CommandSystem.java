
package org.siddharth.ssmp.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.information.SupportMain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CommandSystem implements CommandExecutor, TabCompleter {

    private final SiddysSimplePlugin plugin;
    private final SoundSystem sounds;
    private final MenuSystem menu;
    private final SupportMain support;

    // configure these
    String message_permissionDenied = "You don't have the permission to perform that command";
    String message_configurationReloadFailure = "SiddysPlugin Configuration reload failed.";
    String message_configurationReloadSuccess = "SiddysPlugin Configuration reload completed successfully!";
    String message_playerRunnerAbsence = "Sorry, this command can only be used by in game players.";

    private final List<String> subcommands = Arrays.asList("help", "support", "reload");

    public CommandSystem(SiddysSimplePlugin plugin, SoundSystem sounds, MenuSystem menu, SupportMain support) {
        this.plugin = plugin;
        this.sounds = sounds;
        this.menu = menu;
        this.support = support;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ssmp.admin")) {
                sender.sendMessage(ChatColor.RED + message_permissionDenied);
                return true;
            }

            if (plugin.reloadSSMP()) {
                sender.sendMessage(ChatColor.GREEN + message_configurationReloadSuccess);
            } else {
                sender.sendMessage(ChatColor.RED + message_configurationReloadFailure);
            }

            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("support") || args[0].equalsIgnoreCase("help"))) {
            return support.handle(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(message_playerRunnerAbsence);
            return true;
        }

        menu.open((Player) sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);

            return subcommands.stream()
                    .filter(s -> s.startsWith(input))
                    .filter(s -> !s.equals("reload") || sender.hasPermission("ssmp.admin"))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
