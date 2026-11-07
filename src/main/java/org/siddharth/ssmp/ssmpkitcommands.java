/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ssmpkitcommands implements CommandExecutor, TabCompleter {

    private final ssmp plugin;
    private final ssmpkits kits;
    private final ssmpkitgui gui;
    private final ssmpcore core;

    public ssmpkitcommands(ssmp plugin, ssmpkits kits, ssmpkitgui gui) {
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
                core.sendMessage(sender, "&cOnly players can open the kits GUI.");
            }

            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "create" -> {
                if (!sender.hasPermission("ssmp.kits.create")) {
                    core.sendMessage(sender, "&cYou don't have permission to create kits.");
                    return true;
                }

                if (!(sender instanceof Player player)) {
                    core.sendMessage(sender, "&cOnly players can create kits.");
                    return true;
                }

                if (args.length < 2) {
                    core.sendMessage(sender, "&cUsage: /kit create <name> [display name]");
                    return true;
                }

                String name = args[1].toLowerCase(Locale.ROOT);
                String displayName = args.length > 2
                        ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                        : name;

                if (!kits.isValidName(name)) {
                    core.sendMessage(sender, "&cKit names may only contain letters, numbers, underscores and hyphens.");
                    return true;
                }

                if (!kits.createKit(player, name, displayName)) {
                    core.sendMessage(sender, "&cA kit with that name already exists.");
                    return true;
                }

                core.sendMessage(sender, "&aCreated kit &f" + name + "&a.");
                core.sendMessage(sender, "&7Permission: &f" + kits.getKitPermission(name));
            }

            case "delete" -> {
                if (!sender.hasPermission("ssmp.kits.delete")) {
                    core.sendMessage(sender, "&cYou don't have permission to delete kits.");
                    return true;
                }

                if (args.length < 2) {
                    core.sendMessage(sender, "&cUsage: /kit delete <name>");
                    return true;
                }

                if (!kits.deleteKit(args[1])) {
                    core.sendMessage(sender, "&cThat kit doesn't exist.");
                    return true;
                }

                core.sendMessage(sender, "&aDeleted kit &f" + args[1] + "&a.");
            }

            case "edit" -> {
                if (!sender.hasPermission("ssmp.kits.create")) {
                    core.sendMessage(sender, "&cYou don't have permission to edit kits.");
                    return true;
                }

                if (!(sender instanceof Player player)) {
                    core.sendMessage(sender, "&cOnly players can edit kits.");
                    return true;
                }

                if (args.length < 2) {
                    core.sendMessage(sender, "&cUsage: /kit edit <name>");
                    return true;
                }

                if (!kits.editKit(player, args[1])) {
                    core.sendMessage(sender, "&cThat kit doesn't exist.");
                    return true;
                }

                core.sendMessage(sender, "&aUpdated kit &f" + args[1] + "&a.");
            }

            case "give" -> {
                if (!sender.hasPermission("ssmp.kits.give")) {
                    core.sendMessage(sender, "&cYou don't have permission to give kits.");
                    return true;
                }

                if (args.length < 3) {
                    core.sendMessage(sender, "&cUsage: /kit give <player> <kit>");
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);

                if (target == null) {
                    core.sendMessage(sender, "&cThat player isn't online.");
                    return true;
                }

                ssmpkits.KitResult result = kits.giveKit(target, args[2], true, true);

                if (result == ssmpkits.KitResult.SUCCESS) {
                    core.sendMessage(sender, "&aGave kit &f" + args[2] + " &ato &f" + target.getName() + "&a.");
                    core.sendMessage(target, "&aYou received the &f" + kits.getDisplayName(args[2]) + " &akit.");
                } else {
                    sendResult(sender, result, args[2]);
                }
            }

            case "list" -> {
                List<String> names = kits.getKitNames();

                core.sendMessage(sender, "&bAvailable kits &7(" + names.size() + "):");

                if (names.isEmpty()) {
                    core.sendMessage(sender, "&7No kits have been created yet.");
                } else {
                    core.sendMessage(sender, "&f" + String.join("&7, &f", names));
                }
            }

            case "reload" -> {
                if (!sender.hasPermission("ssmp.admin")) {
                    core.sendMessage(sender, "&cYou don't have permission to reload SSMP kits.");
                    return true;
                }

                kits.reload();
                core.sendMessage(sender, "&aKits configuration reloaded.");
            }

            default -> {
                if (sender instanceof Player player) {
                    sendResult(player, kits.giveKit(player, subcommand), subcommand);
                } else {
                    core.sendMessage(sender, "&cOnly players can claim kits.");
                }
            }
        }

        return true;
    }

    private void sendResult(CommandSender sender, ssmpkits.KitResult result, String name) {
        switch (result) {
            case SUCCESS ->
                    core.sendMessage(sender, "&aSuccessfully claimed kit &f" + name + "&a.");

            case NOT_FOUND ->
                    core.sendMessage(sender, "&cThat kit doesn't exist.");

            case NO_PERMISSION ->
                    core.sendMessage(sender, "&cYou don't have permission to use that kit.");

            case COOLDOWN -> {
                if (sender instanceof Player player) {
                    long seconds = kits.getRemainingCooldown(player, name);
                    long hours = seconds / 3600;
                    long minutes = (seconds % 3600) / 60;
                    long remainingSeconds = seconds % 60;

                    core.sendMessage(sender, "&cYou must wait &f" + hours + "h "
                            + minutes + "m " + remainingSeconds + "s &cbefore using this kit again.");
                }
            }

            case DISABLED ->
                    core.sendMessage(sender, "&cThe kits module is currently disabled.");
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
                case "delete", "edit" -> suggestions.addAll(kits.getKitNames());
                case "create" -> suggestions.add("<name>");
                case "give" -> Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            suggestions.addAll(kits.getKitNames());
        }

        String input = args[args.length - 1].toLowerCase(Locale.ROOT);

        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(input))
                .distinct()
                .toList();
    }
}