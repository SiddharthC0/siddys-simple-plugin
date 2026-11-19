package org.siddharth.ssmp.commands;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.gui.playerInformationGUI;
public class playerInformationCommands implements CommandExecutor {
    private final playerInformationGUI gui;
    public playerInformationCommands(playerInformationGUI gui) {
        this.gui = gui;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        } else if (!player.hasPermission("ssmp.playerinfo.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to open player information screen.");
            return true;
        }
        gui.open(player, player);
        return true;
    }
}
