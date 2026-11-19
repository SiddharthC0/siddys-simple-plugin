package org.siddharth.ssmp.commands;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.gui.kitGUI;
import org.siddharth.ssmp.util.coreUtilMethods;
import org.siddharth.ssmp.mechanics.kitSystem;
import org.siddharth.ssmp.ssmp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static java.util.Arrays.copyOfRange;

public class kitCommands implements CommandExecutor, TabCompleter {
    private final ssmp plugin;
    private final kitSystem kits;
    private final kitGUI gui;
    private final coreUtilMethods core;
    public kitCommands(ssmp plugin, kitSystem kits, kitGUI gui) {
        this.plugin = plugin;
        this.kits = kits;
        this.gui = gui;
        this.core = plugin.getCore();
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                gui.open(player, 0);
            } else {
                core.sendMessage(sender, "&cOnly players can use this command...");
            }
            return true;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("create")) {
            if (!sender.hasPermission("ssmp.kits.create")) {
                core.sendMessage(sender, "&cYou don't have permission to create kits.");
                return true;
            }

            if (!(sender instanceof Player player)) {
                core.sendMessage(sender, "&cKits can only be created by ingame players.");
                return true;
            }

            if (args.length < 2) {
                core.sendMessage(sender, "&cUsage: /kit create <name> [display name] : Creates a kit <name> with set display name");
                return true;
            }

            String name = args[1].toLowerCase(Locale.ROOT);
            String displayName;

            if (args.length > 2) {
                displayName = String.join(" ", copyOfRange(args, 2, args.length));
            } else {
                displayName = name;
            }
            if (!kits.isValidName(name)) {
                core.sendMessage(sender, "&cThe kit name is invalid. Kit names may only contain letters, numbers and underscores.");
                return true;
            }

            if (!kits.createKit(player, name, displayName)) {
                core.sendMessage(sender, "&cKit name is invalid. Kit with that name already exists.");
                return true;
            }

            core.sendMessage(sender, "&aKit created: &f" + name);
            core.sendMessage(sender, "&7Permission set:  &f" + kits.getKitPermission(name));
        } else if (subcommand.equals("delete")) {
            if (!sender.hasPermission("ssmp.kits.delete")) {
                core.sendMessage(sender, "&cYou don't have permission to delete kits.");
                return true;
            }

            if (args.length < 2) {
                core.sendMessage(sender, "&cUsage: /kit delete <name> : Deletes the kit with name <name>");
                return true;
            }

            if (!kits.deleteKit(args[1])) {
                core.sendMessage(sender, "&cInvalid kit name. Kit with that name does not exist");
                return true;
            }

            core.sendMessage(sender, "&aDeleted kit &f" + args[1]);
        } else if (subcommand.equals("edit")) {
            if (!sender.hasPermission("ssmp.kits.create")) {
                core.sendMessage(sender, "&cYou don't have permission to edit kits.");
                return true;
            }

            if (!(sender instanceof Player player)) {
                core.sendMessage(sender, "&cKits can only be edited by players");
                return true;
            }

            if (args.length < 2) {
                core.sendMessage(sender, "&cUsage: /kit edit <name> : Edits the items of kit <name>");
                return true;
            }

            if (!kits.editKit(player, args[1])) {
                core.sendMessage(sender, "&cThat kit doesn't exist.");
                return true;
            }

            core.sendMessage(sender, "&aEdited kit &f" + args[1]);
        } else if (subcommand.equals("give")) {
            if (!sender.hasPermission("ssmp.kits.give")) {
                core.sendMessage(sender, "&cYou don't have permission to give kits.");
                return true;
            }

            if (args.length < 3) {
                core.sendMessage(sender, "&cUsage: /kit give <player> <kit> : Gives <player> the kit named <kit>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);

            if (target == null) {
                core.sendMessage(sender, "&cThat player does not exist currently (probably not online)");
                return true;
            }

            kitSystem.KitResult result = kits.giveKit(target, args[2], true, true);

            if (result == kitSystem.KitResult.SUCCESS) {
                core.sendMessage(sender, "&aGave kit &f" + args[2] + " &ato &f" + target.getName());
                core.sendMessage(target, "&aYou received the &f" + kits.getDisplayName(args[2]));
            } else {
                sendResult(sender, result, args[2]);
            }
        } else if (subcommand.equals("list")) {
            List<String> names = kits.getKitNames();

            core.sendMessage(sender, "&bAvailable kits: ");

            if (names.isEmpty()) {
                core.sendMessage(sender, "&7No kits have been created yet.");
            } else {
                core.sendMessage(sender, "&f" + String.join("&7, &f", names));
            }
        } else if (subcommand.equals("reload")) {
            if (!sender.hasPermission("ssmp.admin")) {
                core.sendMessage(sender, "&cYou don't have permission to reload kits.");
                return true;
            }

            kits.reload();
            core.sendMessage(sender, "&aKits has been reloaded.");
        } else {
            if (sender instanceof Player player) {
                sendResult(player, kits.giveKit(player, subcommand), subcommand);
            } else {
                core.sendMessage(sender, "&cOnly players can claim kits..");
            }
        }

        return true;
    }

    private void sendResult(CommandSender sender, kitSystem.KitResult result, String name) {
        if (result == kitSystem.KitResult.SUCCESS) {
            core.sendMessage(sender, "Succesfully claimed kit &f"+name);
        } else if (result == kitSystem.KitResult.NOT_FOUND) {
            core.sendMessage(sender, "&cThat kit doesn't exist.");
        } else if (result == kitSystem.KitResult.NO_PERMISSION) {
            core.sendMessage(sender, "&cYou don't have permission to use that kit.");
        } else if (result == kitSystem.KitResult.COOLDOWN) {
            if (sender instanceof Player player) {
                long secs = kits.getRemainingCooldown(player, name);
                long hours = secs / 3600;
                long minutes = (secs % 3600) / 60;
                long remainingSeconds = secs % 60;

                core.sendMessage(sender, "&cYou must wait &f" + hours + "h "+ minutes + "m " + remainingSeconds + "s &cbefore using this kit again.");
            }
        } else if (result == kitSystem.KitResult.DISABLED) {
            core.sendMessage(sender, "&cThe kit module is disabled.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(List.of("create", "delete", "edit", "give", "list", "reload"));
            suggestions.addAll(kits.getKitNames());
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "delete", "edit":  {
                    suggestions.addAll(kits.getKitNames());
                }
                case "create": {
                    suggestions.add("<name>");
                }
                case "give": {
                    Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            suggestions.addAll(kits.getKitNames());
        }
        String input = args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(input)).distinct().toList();
    }
}