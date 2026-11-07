/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ssmprankcommands implements CommandExecutor {

    private final ssmpranks ranks;
    private final ssmppermissions permissions;

    public ssmprankcommands(
            ssmpranks ranks,
            ssmppermissions permissions
    ) {
        this.ranks = ranks;
        this.permissions = permissions;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    ChatColor.RED + "Only players can use rank commands."
            );
            return true;
        }

        if (!permissions.hasPermission(
                player,
                "ssmp.rank.use"
        )) {
            player.sendMessage(
                    ChatColor.RED + "You do not have permission to use rank commands."
            );
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "list" -> listRanks(player);
            case "create" -> createRank(player, args);
            case "delete" -> deleteRank(player, args);
            case "give" -> giveRank(player, args);
            case "remove" -> removeRank(player, args);
            case "setprefix" -> setPrefix(player, args);
            case "setsuffix" -> setSuffix(player, args);
            case "setname" -> setName(player, args);
            case "info" -> rankInfo(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void listRanks(Player player) {
        List<String> rankList = ranks.getRanks();

        player.sendMessage(
                ChatColor.DARK_AQUA + "SSMP Ranks"
        );

        if (rankList.isEmpty()) {
            player.sendMessage(
                    ChatColor.GRAY + "No ranks exist."
            );
            return;
        }

        for (String rank : rankList) {
            player.sendMessage(
                    ChatColor.GRAY
                            + "- "
                            + ChatColor.WHITE
                            + rank
            );
        }
    }

    private void createRank(
            Player player,
            String[] args
    ) {
        if (!permissions.hasPermission(
                player,
                "ssmp.rank.create"
        )) {
            noPermission(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank create <rank> [display-name]"
            );
            return;
        }

        String rank = args[1].toLowerCase();

        if (ranks.rankExists(rank)) {
            player.sendMessage(
                    ChatColor.RED
                            + "That rank already exists."
            );
            return;
        }

        String displayName = rank;

        if (args.length > 2) {
            displayName = joinArguments(args, 2);
        }

        if (ranks.createRank(
                rank,
                displayName,
                "&7[" + displayName + "] ",
                ""
        )) {
            player.sendMessage(
                    ChatColor.GREEN
                            + "Created rank "
                            + ChatColor.WHITE
                            + rank
            );
        }
    }

    private void deleteRank(
            Player player,
            String[] args
    ) {
        if (!permissions.hasPermission(
                player,
                "ssmp.rank.delete"
        )) {
            noPermission(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank delete <rank>"
            );
            return;
        }

        String rank = args[1].toLowerCase();

        if (!ranks.rankExists(rank)) {
            player.sendMessage(
                    ChatColor.RED
                            + "That rank does not exist."
            );
            return;
        }

        if (rank.equals("default")) {
            player.sendMessage(
                    ChatColor.RED
                            + "The default rank cannot be deleted."
            );
            return;
        }

        if (ranks.deleteRank(rank)) {
            player.sendMessage(
                    ChatColor.GREEN
                            + "Deleted rank "
                            + ChatColor.WHITE
                            + rank
            );
        }
    }

    private void giveRank(
            Player player,
            String[] args
    ) {
        if (!permissions.hasPermission(
                player,
                "ssmp.rank.give"
        )) {
            noPermission(player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank give <player> <rank>"
            );
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage(
                    ChatColor.RED
                            + "That player is not online."
            );
            return;
        }

        String rank = args[2].toLowerCase();

        if (!ranks.rankExists(rank)) {
            player.sendMessage(
                    ChatColor.RED
                            + "That rank does not exist."
            );
            return;
        }

        if (ranks.setPlayerRank(target, rank)) {
            player.sendMessage(
                    ChatColor.GREEN
                            + "Set "
                            + ChatColor.WHITE
                            + target.getName()
                            + ChatColor.GREEN
                            + "'s rank to "
                            + ChatColor.WHITE
                            + rank
            );

            target.sendMessage(
                    ChatColor.GREEN
                            + "Your rank is now "
                            + ChatColor.WHITE
                            + ranks.getPlayerRankDisplayName(target)
            );
        }
    }

    private void removeRank(
            Player player,
            String[] args
    ) {
        if (!permissions.hasPermission(
                player,
                "ssmp.rank.remove"
        )) {
            noPermission(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank remove <player>"
            );
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage(
                    ChatColor.RED
                            + "That player is not online."
            );
            return;
        }

        if (ranks.removePlayerRank(target)) {
            player.sendMessage(
                    ChatColor.GREEN
                            + "Removed "
                            + ChatColor.WHITE
                            + target.getName()
                            + ChatColor.GREEN
                            + "'s rank."
            );

            target.sendMessage(
                    ChatColor.GREEN
                            + "Your rank is now "
                            + ChatColor.WHITE
                            + ranks.getPlayerRankDisplayName(target)
            );
        } else {
            player.sendMessage(
                    ChatColor.YELLOW
                            + "That player is already using the default rank."
            );
        }
    }

    private void setPrefix(
            Player player,
            String[] args
    ) {
        if (!permissions.hasPermission(
                player,
                "ssmp.rank.setprefix"
        )) {
            noPermission(player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank setprefix <rank> <prefix>"
            );
            return;
        }

        String rank = args[1].toLowerCase();
        String prefix = joinArguments(args, 2);

        if (!ranks.setRankPrefix(rank, prefix)) {
            player.sendMessage(
                    ChatColor.RED
                            + "That rank does not exist."
            );
            return;
        }

        player.sendMessage(
                ChatColor.GREEN
                        + "Updated the prefix of "
                        + ChatColor.WHITE
                        + rank
        );
    }

    private void setSuffix(
            Player player,
            String[] args
    ) {
        if (!permissions.hasPermission(
                player,
                "ssmp.rank.setsuffix"
        )) {
            noPermission(player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank setsuffix <rank> <suffix>"
            );
            return;
        }

        String rank = args[1].toLowerCase();
        String suffix = joinArguments(args, 2);

        if (!ranks.setRankSuffix(rank, suffix)) {
            player.sendMessage(
                    ChatColor.RED
                            + "That rank does not exist."
            );
            return;
        }

        player.sendMessage(
                ChatColor.GREEN
                        + "Updated the suffix of "
                        + ChatColor.WHITE
                        + rank
        );
    }

    private void setName(
            Player player,
            String[] args
    ) {
        if (!permissions.hasPermission(
                player,
                "ssmp.rank.setname"
        )) {
            noPermission(player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank setname <rank> <name>"
            );
            return;
        }

        String rank = args[1].toLowerCase();
        String name = joinArguments(args, 2);

        if (!ranks.setRankDisplayName(rank, name)) {
            player.sendMessage(
                    ChatColor.RED
                            + "That rank does not exist."
            );
            return;
        }

        player.sendMessage(
                ChatColor.GREEN
                        + "Updated the display name of "
                        + ChatColor.WHITE
                        + rank
        );
    }

    private void rankInfo(
            Player player,
            String[] args
    ) {
        if (args.length < 2) {
            player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rank info <rank>"
            );
            return;
        }

        String rank = args[1].toLowerCase();

        if (!ranks.rankExists(rank)) {
            player.sendMessage(
                    ChatColor.RED
                            + "That rank does not exist."
            );
            return;
        }

        player.sendMessage(
                ChatColor.DARK_AQUA
                        + "Rank: "
                        + ChatColor.WHITE
                        + rank
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Display Name: "
                        + ChatColor.WHITE
                        + ranks.getRankDisplayName(rank)
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Prefix: "
                        + ranks.getRankPrefix(rank)
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Suffix: "
                        + ranks.getRankSuffix(rank)
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Inherits: "
                        + ChatColor.WHITE
                        + ranks.getInheritedRanks(rank)
        );
    }

    private void sendHelp(Player player) {
        player.sendMessage(
                ChatColor.DARK_AQUA
                        + "SSMP Rank Commands"
        );

        player.sendMessage(ChatColor.GRAY + "/rank list");
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank create <rank> [display-name]"
        );
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank delete <rank>"
        );
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank give <player> <rank>"
        );
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank remove <player>"
        );
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank setprefix <rank> <prefix>"
        );
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank setsuffix <rank> <suffix>"
        );
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank setname <rank> <name>"
        );
        player.sendMessage(
                ChatColor.GRAY
                        + "/rank info <rank>"
        );
    }

    private String joinArguments(
            String[] args,
            int start
    ) {
        StringBuilder result = new StringBuilder();

        for (int i = start; i < args.length; i++) {
            if (i > start) {
                result.append(" ");
            }

            result.append(args[i]);
        }

        return result.toString();
    }

    private void noPermission(Player player) {
        player.sendMessage(
                ChatColor.RED
                        + "You do not have permission to do that."
        );
    }
}