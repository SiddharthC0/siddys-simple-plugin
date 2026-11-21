package org.siddharth.ssmp.mechanics;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.ssmp;
import org.siddharth.ssmp.information.supportMain;
public class commandSystem implements CommandExecutor {
    private final ssmp plugin;
    private final soundSystem sounds;
    private final menuSystem menu;
    private final supportMain support;

    // configure these values to change messages, because i care about u :D
    String message_permissionDenied = "You don't have the permission to perform that command"; // perm denied
    String message_configurationReloadFailure  = "SiddysPlugin Configuration reload failed."; // config reload failed
    String message_configurationReloadSuccess = "SiddysPlugin Configuration reload completed successfully!"; // config reload passed
    String message_playerRunnerAbsence = "Sorry, this command can only be used by in game players."; // console ran player only command

    public commandSystem(ssmp plugin, soundSystem sounds, menuSystem menu, supportMain support) {
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
            if (plugin.reloadSSMP()) {sender.sendMessage(ChatColor.GREEN + message_configurationReloadSuccess);} else {
                sender.sendMessage(ChatColor.RED + message_configurationReloadFailure);
            }

            return true;
        } else if (args.length > 0 && (args[0].equalsIgnoreCase("support") || args[0].equalsIgnoreCase("help"))) {
            return plugin.getSupport().handle(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(message_playerRunnerAbsence);
            return true;
        }

        Player player = (Player) sender;
        menu.open(player);
        return true;
    }
}