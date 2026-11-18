package org.siddharth.ssmp.commands;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.gui.contractGUI;
public class contractCommands implements CommandExecutor {
    private final contractGUI gui;
    public contractCommands(contractGUI gui) {
        this.gui = gui;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players...");
            return true;
        }
        if (!player.hasPermission("ssmp.contracts.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
