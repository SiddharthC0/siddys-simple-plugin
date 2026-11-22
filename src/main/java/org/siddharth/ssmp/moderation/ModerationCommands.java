package org.siddharth.ssmp.moderation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
public final class ModerationCommands implements CommandExecutor, TabCompleter {
    private final ModerationSystem moderation;

    public ModerationCommands(ModerationSystem moderation) { this.moderation = moderation; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        String permission = switch (name) {
            case "mute" -> "ssmp.moderation.mute";
            case "unmute" -> "ssmp.moderation.unmute";
            case "warn" -> "ssmp.moderation.warn";
            case "ban" -> "ssmp.moderation.ban";
            case "unban" -> "ssmp.moderation.unban";
            case "filter" -> "ssmp.moderation.filter";
            case "history" -> "ssmp.moderation.history";
            case "resetviolations" -> "ssmp.moderation.reset";
            default -> "ssmp.moderation.admin";
        };
        if (!sender.hasPermission("ssmp.moderation.admin") && !sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        switch (name) {
            case "mute" -> {
                if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /mute <player> <duration|permanent> <reason>"); return true; }
                OfflinePlayer target = findKnownPlayer(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found. They must have joined this server before."); return true; }
                long duration = ModerationSystem.parseDuration(args[1], -1);
                if (duration < 0) { sender.sendMessage(ChatColor.RED + "Invalid duration. Examples: 10m, 2h, 1d, permanent."); return true; }
                String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                moderation.mute(target.getUniqueId(), knownName(target, args[0]), sender.getName(), reason, duration, message -> sender.sendMessage(ChatColor.GREEN + message));
            }
            case "unmute" -> {
                if (args.length != 1) { sender.sendMessage(ChatColor.RED + "Usage: /unmute <player>"); return true; }
                OfflinePlayer target = findKnownPlayer(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                moderation.unmute(target.getUniqueId(), knownName(target, args[0]), sender.getName(), message -> sender.sendMessage(ChatColor.GREEN + message));
            }
            case "ban" -> {
                if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /ban <player> <duration|permanent> <reason>"); return true; }
                OfflinePlayer target = findKnownPlayer(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found. They must have joined this server before."); return true; }
                long duration = ModerationSystem.parseDuration(args[1], -1);
                if (duration < 0) { sender.sendMessage(ChatColor.RED + "Invalid duration. Examples: 1h, 1d, permanent."); return true; }
                String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                moderation.ban(target.getUniqueId(), knownName(target, args[0]), sender.getName(), reason, duration, message -> sender.sendMessage(ChatColor.GREEN + message));
            }
            case "unban" -> {
                if (args.length != 1) { sender.sendMessage(ChatColor.RED + "Usage: /unban <player>"); return true; }
                OfflinePlayer target = findKnownPlayer(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                moderation.unban(target.getUniqueId(), knownName(target, args[0]), sender.getName(), message -> sender.sendMessage(ChatColor.GREEN + message));
            }
            case "warn" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /warn <player> <reason>"); return true; }
                OfflinePlayer target = findKnownPlayer(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                moderation.warn(target.getUniqueId(), knownName(target, args[0]), sender.getName(), reason, message -> sender.sendMessage(ChatColor.GREEN + message));
            }
            case "filter" -> {
                if (args.length == 0) { sender.sendMessage(ChatColor.YELLOW + "/filter add <term> <category> | remove <term> | list | reload"); return true; }
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "add" -> {
                        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /filter add <term> <category>"); return true; }
                        String term = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
                        moderation.addTerm(term, args[args.length - 1], sender.getName(), message -> sender.sendMessage(ChatColor.GREEN + message));
                    }
                    case "remove" -> {
                        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /filter remove <term>"); return true; }
                        moderation.removeTerm(String.join(" ", Arrays.copyOfRange(args, 1, args.length)), message -> sender.sendMessage(ChatColor.GREEN + message));
                    }
                    case "list" -> {
                        Map<String, String> terms = moderation.getTerms();
                        sender.sendMessage(ChatColor.GOLD + "Blocked terms (" + terms.size() + "):");
                        if (terms.isEmpty()) sender.sendMessage(ChatColor.GRAY + "No terms configured.");
                        terms.forEach((term, category) -> sender.sendMessage(ChatColor.GRAY + "- " + term + " [" + category + "]"));
                    }
                    case "reload" -> moderation.reload(message -> sender.sendMessage(ChatColor.GREEN + message));
                    default -> sender.sendMessage(ChatColor.RED + "Usage: /filter add <term> <category> | remove <term> | list | reload");
                }
            }
            case "history" -> {
                if (args.length != 1) { sender.sendMessage(ChatColor.RED + "Usage: /history <player>"); return true; }
                OfflinePlayer target = findKnownPlayer(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                moderation.history(target.getUniqueId(), entries -> {
                    sender.sendMessage(ChatColor.GOLD + "Moderation history: " + knownName(target, args[0]));
                    if (entries.isEmpty()) sender.sendMessage(ChatColor.GRAY + "No records found.");
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    for (ModerationDatabase.History entry : entries) sender.sendMessage(ChatColor.GRAY + "[" + format.format(new Date(entry.createdAt())) + "] " + entry.action() + " — " + entry.reason() + " (" + entry.moderator() + ")");
                });
            }
            case "resetviolations" -> {
                if (args.length != 1) { sender.sendMessage(ChatColor.RED + "Usage: /resetviolations <player>"); return true; }
                OfflinePlayer target = findKnownPlayer(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                moderation.resetViolations(target.getUniqueId(), message -> sender.sendMessage(ChatColor.GREEN + message));
            }
        }
        return true;
    }

    private OfflinePlayer findKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(name)) return offline;
        }
        return null;
    }

    private String knownName(OfflinePlayer player, String fallback) {
        return player.getName() == null ? fallback : player.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && !command.getName().equalsIgnoreCase("filter")) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).collect(Collectors.toList());
        }
        if (command.getName().equalsIgnoreCase("filter") && args.length == 1) return List.of("add", "remove", "list", "reload");
        if ((command.getName().equalsIgnoreCase("mute") || command.getName().equalsIgnoreCase("ban")) && args.length == 2) return List.of("10m", "30m", "1h", "1d", "permanent");
        return Collections.emptyList();
    }
}
