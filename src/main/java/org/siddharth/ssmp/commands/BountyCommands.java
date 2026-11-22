package org.siddharth.ssmp.commands;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.gui.BountyGUI;
public class BountyCommands implements CommandExecutor {
    private final BountyGUI gui;
    public BountyCommands(BountyGUI gui) {
        this.gui = gui;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players...");
            return true;
        }
        if (!player.hasPermission("ssmp.bounties.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        gui.open(player);
        return true;
    }
}