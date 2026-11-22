package org.siddharth.ssmp.commands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.mechanics.PermissionSystem;
import org.siddharth.ssmp.mechanics.RankSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RankCommands implements CommandExecutor, TabCompleter {
    private final RankSystem ranks;
    private final PermissionSystem permissions;
    private static final String PREFIX = ChatColor.DARK_AQUA + "[SSmP] ";
    private static final String ERROR = ChatColor.RED + "";
    private static final String SUCCESS = ChatColor.GREEN + "";
    private static final String INFO = ChatColor.GRAY + "";
    private static final List<String> SUBCOMMANDS = List.of(
            "list",
            "create",
            "delete",
            "give",
            "remove",
            "setprefix",
            "setsuffix",
            "setname",
            "info"
    );
    public RankCommands(RankSystem ranks, PermissionSystem permissions) {
        this.ranks = ranks;
        this.permissions = permissions;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ERROR + "Only players can use rank commands.");
            return true;
        } else if (ranks == null || permissions == null) {
            player.sendMessage(ERROR + "The rank system is currently unavailable.");
            return true;
        } else if (!hasPermission(player, "ssmp.rank.use")) {
            noPermission(player);
            return true;
        } else if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "list": {
                if (requirePermission(player, "ssmp.rank.use")) {
                    listRanks(player);
                }
            }
            case "create": {
                createRank(player, args);
            }
            case "delete": {
                deleteRank(player, args);
            }
            case "give": {
                giveRank(player, args);
            }
            case "remove": {
                removeRank(player, args);
            }
            case "setprefix": {
                setPrefix(player, args);
            }
            case "setsuffix": {
                setSuffix(player, args);
            }
            case "setname": {
                setName(player, args);
            }
            case "info": {
                if (requirePermission(player, "ssmp.rank.use")) {
                    rankInfo(player, args);
                }
            }
            default: {
                player.sendMessage(ERROR + "Unknown rank subcommand: " + args[0]);
                player.sendMessage(INFO + "Use /rank for available commands.");
            }
        }
        return true;
    }

    private void listRanks(Player player) {
        List<String> rankList = ranks.getRanks();
        player.sendMessage(PREFIX + ChatColor.WHITE + "Available Ranks");
        if (rankList == null || rankList.isEmpty()) {
            player.sendMessage(INFO + "No ranks currently exist.");
            return;
        }
        for (String rank : rankList) {
            if (rank == null || rank.isBlank()) {
                continue;
            }
            player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + rank);
        }
        player.sendMessage(INFO + "Total ranks: " + rankList.size());
    }

    private void createRank(Player player, String[] args) {
        if (!requirePermission(player, "ssmp.rank.create")) {
            return;
        }

        if (args.length < 2) {
            usage(player, "/rank create <rank> [display-name]");
            return;
        }

        String rank = normalizeRank(args[1]);

        if (!isValidRankId(rank)) {
            error(player, "Invalid rank ID.");
            player.sendMessage(INFO + "Use only lowercase letters, numbers, underscores and hyphens.");
            return;
        }

        if (ranks.rankExists(rank)) {
            error(player, "That rank already exists.");
            return;
        }

        // For future me: if there is a bug, its 100% this tenary statement
        String displayName = args.length > 2 ? joinArguments(args, 2) : rank;
        if (displayName.isBlank()) {
            error(player, "The display name cannot be empty.");
            return;
        }
        boolean created = ranks.createRank(rank, displayName, "&7[" + displayName + "] ", "");
        if (!created) {
            error(player, "Failed to create the rank.");
            player.sendMessage(INFO + "The rank system rejected the operation.");
            return;
        }
        success(player, "Successfully created rank " + rank);
    }
    private void deleteRank(Player player, String[] args) {
        if (!requirePermission(player, "ssmp.rank.delete")) {
            return;
        } else if (args.length < 2) {
            usage(player, "/rank delete <rank>");
            return;
        }
        String rank = normalizeRank(args[1]);
        if (!ranks.rankExists(rank)) {
            error(player, "That rank does not exist.");
            return;
        } else if (rank.equals("default")) {
            error(player, "The default rank cannot be deleted.");
            return;
        } else if (!ranks.deleteRank(rank)) {
            error(player, "Failed to delete the rank.");
            player.sendMessage(INFO + "The rank may be in use or the operation was rejected.");
            return;
        }
        success(player, "Successfully deleted rank " + rank);
    }

    private void giveRank(Player player, String[] args) {
        if (!requirePermission(player, "ssmp.rank.give")) {
            return;
        } else if (args.length < 3) {
            usage(player, "/rank give <player> <rank>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            error(player, "That player is not online.");
            player.sendMessage(INFO + "Check the player's username and try again.");
            return;
        }
        String rank = normalizeRank(args[2]);
        if (!ranks.rankExists(rank)) {
            error(player, "That rank does not exist.");
            return;
        } else if (rank.equalsIgnoreCase(ranks.getPlayerRank(target))) {
            error(player, "That player already has this rank.");
            return;
        } else if (!ranks.setPlayerRank(target, rank)) {
            error(player, "Failed to assign the rank.");
            return;
        }
        success(player, "Set " + target.getName() + "'s rank to " + rank);
        target.sendMessage(PREFIX + ChatColor.GREEN + "Your rank is now " + ChatColor.WHITE + ranks.getPlayerRankDisplayName(target));
    }

    private void removeRank(Player player, String[] args) {
        if (!requirePermission(player, "ssmp.rank.remove")) {
            return;
        }

        if (args.length < 2) {
            usage(player, "/rank remove <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            error(player, "That player is not online.");
            return;
        }
        if (!ranks.removePlayerRank(target)) {
            error(player, "Could not remove that player's rank.");
            player.sendMessage(INFO + "They may already have the default rank.");
            return;
        }
        success(player, "Removed " + target.getName() + "'s rank.");
        target.sendMessage(PREFIX + ChatColor.GREEN + "Your rank is now " + ChatColor.WHITE + ranks.getPlayerRankDisplayName(target));
    }

    private void setPrefix(Player player, String[] args) {
        if (!requirePermission(player, "ssmp.rank.setprefix")) {
            return;
        }
        if (args.length < 3) {
            usage(player, "/rank setprefix <rank> <prefix>");
            return;
        }
        String rank = normalizeRank(args[1]);
        if (!ranks.rankExists(rank)) {
            error(player, "That rank does not exist.");
            return;
        }
        String prefix = joinArguments(args, 2);
        if (prefix.isBlank()) {
            error(player, "The prefix cannot be empty.");
            return;
        } else if (!ranks.setRankPrefix(rank, prefix)) {
            error(player, "Failed to update the rank prefix.");
            return;
        }
        success(player, "Successfully updated the prefix of " + rank + ".");
    }

    private void setSuffix(Player player, String[] args) {
        if (!requirePermission(player, "ssmp.rank.setsuffix")) {
            return;
        }
        if (args.length < 3) {
            usage(player, "/rank setsuffix <rank> <suffix>");
            return;
        }
        String rank = normalizeRank(args[1]);
        if (!ranks.rankExists(rank)) {
            error(player, "That rank does not exist.");
            return;
        }
        String suffix = joinArguments(args, 2);
        if (suffix.isBlank()) {
            error(player, "The suffix cannot be empty.");
            return;
        } else if (!ranks.setRankSuffix(rank, suffix)) {
            error(player, "Failed to update the rank suffix.");
            return;
        }
        success(player, "Successfully updated the suffix of " + rank + ".");
    }

    private void setName(Player player, String[] args) {
        if (!requirePermission(player, "ssmp.rank.setname")) {
            return;
        }

        if (args.length < 3) {
            usage(player, "/rank setname <rank> <name>");
            return;
        }

        String rank = normalizeRank(args[1]);
        if (!ranks.rankExists(rank)) {
            error(player, "That rank does not exist.");
            return;
        }

        String name = joinArguments(args, 2);
        if (name.isBlank()) {
            error(player, "The display name cannot be empty.");
            return;
        } else if (!ranks.setRankDisplayName(rank, name)) {
            error(player, "Failed to update the rank display name.");
            return;
        }
        success(player, "Successfully updated the display name of " + rank + ".");
    }

    private void rankInfo(Player player, String[] args) {
        if (args.length < 2) {
            usage(player, "/rank info <rank>");
            return;
        }

        String rank = normalizeRank(args[1]);
        if (!ranks.rankExists(rank)) {
            error(player, "That rank does not exist.");
            return;
        }

        player.sendMessage(PREFIX + ChatColor.WHITE + "Rank Information");
        player.sendMessage(INFO + "ID: " + ChatColor.WHITE + rank);
        player.sendMessage(INFO + "Display Name: " + ChatColor.WHITE + ranks.getRankDisplayName(rank));
        player.sendMessage(INFO + "Prefix: " + ranks.getRankPrefix(rank));
        player.sendMessage(INFO + "Suffix: " + ranks.getRankSuffix(rank));
        player.sendMessage(INFO + "Inherits: " + ChatColor.WHITE + ranks.getInheritedRanks(rank));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 0 || !hasPermission(player, "ssmp.rank.use")) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            addSubcommand(suggestions, player, "list", "ssmp.rank.use");
            addSubcommand(suggestions, player, "create", "ssmp.rank.create");
            addSubcommand(suggestions, player, "delete", "ssmp.rank.delete");
            addSubcommand(suggestions, player, "give", "ssmp.rank.give");
            addSubcommand(suggestions, player, "remove", "ssmp.rank.remove");
            addSubcommand(suggestions, player, "setprefix", "ssmp.rank.setprefix");
            addSubcommand(suggestions, player, "setsuffix", "ssmp.rank.setsuffix");
            addSubcommand(suggestions, player, "setname", "ssmp.rank.setname");
            addSubcommand(suggestions, player, "info", "ssmp.rank.use");
        }

        else if (args.length == 2) {
            String subcommand = args[0].toLowerCase(Locale.ROOT);

            switch (subcommand) {
                case "delete", "setprefix", "setsuffix", "setname", "info": {
                    String permission = switch (subcommand) {
                        case "delete" -> "ssmp.rank.delete";
                        case "setprefix" -> "ssmp.rank.setprefix";
                        case "setsuffix" -> "ssmp.rank.setsuffix";
                        case "setname" -> "ssmp.rank.setname";
                        default -> "ssmp.rank.use";
                    };

                    if (hasPermission(player, permission)) {
                        addRanks(suggestions);
                    }
                }

                case "give", "remove": {
                    String permission = subcommand.equals("give")
                            ? "ssmp.rank.give"
                            : "ssmp.rank.remove";

                    if (hasPermission(player, permission)) {
                        addOnlinePlayers(suggestions);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (hasPermission(player, "ssmp.rank.give")) {
                addRanks(suggestions);
            }
        }
        return filterSuggestions(suggestions, args[args.length - 1]);
    }

    private void addSubcommand(List<String> suggestions, Player player, String subcommand, String permission) {
        if (hasPermission(player, permission)) {
            suggestions.add(subcommand);
        }
    }

    private void addRanks(List<String> suggestions) {
        List<String> availableRanks = ranks.getRanks();
        if (availableRanks == null) {
            return;
        }

        for (String rank : availableRanks) {
            if (rank != null && !rank.isBlank()) {
                suggestions.add(rank);
            }
        }
    }

    private void addOnlinePlayers(List<String> suggestions) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            suggestions.add(online.getName());
        }
    }

    private List<String> filterSuggestions(List<String> suggestions, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return suggestions.stream().filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(prefix)).distinct().sorted(Comparator.comparing(String::toLowerCase)).toList();
    }
    private boolean hasPermission(Player player, String permission) {
        return permissions.hasPermission(player, permission);
    }

    private boolean requirePermission(Player player, String permission) {
        if (hasPermission(player, permission)) {
            return true;
        }
        noPermission(player);
        return false;
    }
    private boolean isValidRankId(String rank) {
        return rank != null && rank.matches("[a-z0-9_-]{1,32}");
    }
    private String normalizeRank(String rank) {
        if (rank == null) {
            return "";
        }

        return rank.toLowerCase(Locale.ROOT);
    }

    private String joinArguments(String[] args, int start) {
        if (args == null || start >= args.length) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = start; i < args.length; i++) {
            if (i > start) {
                result.append(" ");
            }

            result.append(args[i]);
        }

        return result.toString().trim();
    }

    private void usage(Player player, String usage) {
        player.sendMessage(ERROR + "Incorrect command usage.");
        player.sendMessage(ChatColor.YELLOW + "The correct usage for the command is: " + ChatColor.WHITE + usage);
    }

    private void error(Player player, String message) {
        player.sendMessage(PREFIX + ERROR + message);
    }

    private void success(Player player, String message) {
        player.sendMessage(PREFIX + SUCCESS + message);
    }

    private void noPermission(Player player) {
        error(player, "You do not have permission to do that.");
    }

    private void sendHelp(Player player) {
        player.sendMessage(PREFIX + ChatColor.WHITE + "Rank Commands");

        if (hasPermission(player, "ssmp.rank.use")) {
            player.sendMessage(INFO + "/rank list");
            player.sendMessage(INFO + "/rank info <rank>");
        }

        if (hasPermission(player, "ssmp.rank.create")) {
            player.sendMessage(INFO + "/rank create <rank> [display-name]");
        }

        if (hasPermission(player, "ssmp.rank.delete")) {
            player.sendMessage(INFO + "/rank delete <rank>");
        }

        if (hasPermission(player, "ssmp.rank.give")) {
            player.sendMessage(INFO + "/rank give <player> <rank>");
        }

        if (hasPermission(player, "ssmp.rank.remove")) {
            player.sendMessage(INFO + "/rank remove <player>");
        }

        if (hasPermission(player, "ssmp.rank.setprefix")) {
            player.sendMessage(INFO + "/rank setprefix <rank> <prefix>");
        }

        if (hasPermission(player, "ssmp.rank.setsuffix")) {
            player.sendMessage(INFO + "/rank setsuffix <rank> <suffix>");
        }

        if (hasPermission(player, "ssmp.rank.setname")) {
            player.sendMessage(INFO + "/rank setname <rank> <name>");
        }
    }
}
