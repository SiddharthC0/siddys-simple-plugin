
/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ssmppapi extends PlaceholderExpansion {

    private final ssmp plugin;
    private final ssmpplaceholders placeholders;
    private final ssmpranks ranks;
    private final ssmpcontracts contracts;
    private final ssmpbounties bounties;
    private final ssmplevels levels;
    private final ssmpeconomy economy;

    private static final Pattern TASK_PATTERN =
            Pattern.compile("task_(\\d+)_(name|progress|required)");

    public ssmppapi(
            ssmp plugin,
            ssmpplaceholders placeholders,
            ssmpranks ranks,
            ssmpcontracts contracts,
            ssmpbounties bounties,
            ssmplevels levels,
            ssmpeconomy economy
    ) {
        this.plugin = plugin;
        this.placeholders = placeholders;
        this.ranks = ranks;
        this.contracts = contracts;
        this.bounties = bounties;
        this.levels = levels;
        this.economy = economy;
    }

    @Override
    public String getAuthor() {
        return "Siddharth C";
    }

    @Override
    public String getIdentifier() {
        return "ssmp";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (params == null || params.isBlank()) {
            return null;
        }

        String key = params.toLowerCase(Locale.ROOT);
        Player player = offlinePlayer == null
                ? null
                : offlinePlayer.getPlayer();

        String result = getRankPlaceholder(player, key);
        if (result != null) return result;

        result = getContractPlaceholder(player, key);
        if (result != null) return result;

        result = getBountyPlaceholder(player, key);
        if (result != null) return result;

        result = getLevelPlaceholder(player, key);
        if (result != null) return result;

        result = getEconomyPlaceholder(player, key);
        if (result != null) return result;

        if (placeholders != null) {
            String internalKey = switch (key) {
                case "player_rank" -> "ssmp_player_rank";
                case "player_rank_name" -> "ssmp_player_rank_name";
                case "player_prefix" -> "ssmp_player_prefix";
                case "player_suffix" -> "ssmp_player_suffix";
                default -> key;
            };

            return placeholders.get(player, internalKey);
        }

        return null;
    }

    private String getRankPlaceholder(Player player, String key) {
        if (player == null || ranks == null) {
            return null;
        }

        String rank = ranks.getPlayerRank(player);

        return switch (key) {
            case "rank" -> rank;
            case "rank_name" -> ranks.getRankDisplayName(rank);
            case "rank_prefix" -> ranks.getRankPrefix(rank);
            case "rank_suffix" -> ranks.getRankSuffix(rank);
            case "rank_inherits" -> String.join(
                    ", ",
                    ranks.getInheritedRanks(rank)
            );
            default -> null;
        };
    }

    private String getContractPlaceholder(Player player, String key) {
        if (contracts == null) {
            return null;
        }

        if (player != null) {
            switch (key) {
                case "contracts_active":
                    return String.valueOf(contracts.getAcceptedCount(player));
                case "contracts_completed":
                    return String.valueOf(contracts.getCompletedCount(player));
                case "contracts_total":
                    return String.valueOf(contracts.getContracts().size());
            }
        }

        if (!key.startsWith("contract_")) {
            return null;
        }

        for (String id : contracts.getContracts()) {
            String prefix = "contract_" + id.toLowerCase(Locale.ROOT) + "_";

            if (!key.startsWith(prefix)) {
                continue;
            }

            String property = key.substring(prefix.length());

            if (property.equals("name")) {
                return contracts.getContractName(id);
            }

            if (property.equals("description")) {
                return contracts.getContractDescription(id);
            }

            if (property.equals("difficulty")) {
                return contracts.getDifficulty(id);
            }

            if (property.equals("tasks_total")) {
                return String.valueOf(contracts.getTaskCount(id));
            }

            if (property.equals("reward_money")) {
                double reward = contracts.getRewardMoney(id);
                return economy == null
                        ? String.valueOf(reward)
                        : economy.formatMoney(reward);
            }

            if (property.equals("reward_xp")) {
                return String.valueOf(contracts.getRewardXP(id));
            }

            if (property.equals("reward_levels")) {
                return String.valueOf(contracts.getRewardLevelUpgrade(id));
            }

            if (player == null) {
                return null;
            }

            if (property.equals("accepted")) {
                return String.valueOf(contracts.isAccepted(player, id));
            }

            if (property.equals("completed")) {
                return String.valueOf(contracts.isCompleted(player, id));
            }

            if (property.equals("tasks_completed")) {
                int completed = 0;

                for (int i = 0; i < contracts.getTaskCount(id); i++) {
                    if (contracts.getTaskProgress(player, id, i)
                            >= contracts.getTaskAmount(id, i)) {
                        completed++;
                    }
                }

                return String.valueOf(completed);
            }

            if (property.equals("progress")) {
                int total = contracts.getTaskCount(id);

                if (total == 0) {
                    return "0";
                }

                int completed = 0;

                for (int i = 0; i < total; i++) {
                    if (contracts.getTaskProgress(player, id, i)
                            >= contracts.getTaskAmount(id, i)) {
                        completed++;
                    }
                }

                return String.valueOf(
                        Math.round((double) completed / total * 100)
                );
            }

            Matcher matcher = TASK_PATTERN.matcher(property);

            if (matcher.matches()) {
                int index;

                try {
                    index = Integer.parseInt(matcher.group(1)) - 1;
                } catch (NumberFormatException e) {
                    return null;
                }

                if (index < 0 || index >= contracts.getTaskCount(id)) {
                    return null;
                }

                return switch (matcher.group(2)) {
                    case "name" -> contracts.getTaskName(id, index);
                    case "required" -> String.valueOf(
                            contracts.getTaskAmount(id, index)
                    );
                    case "progress" -> String.valueOf(
                            contracts.getTaskProgress(player, id, index)
                    );
                    default -> null;
                };
            }

            return null;
        }

        return null;
    }

    private String getBountyPlaceholder(Player player, String key) {
        if (bounties == null) {
            return null;
        }

        if (key.startsWith("bounty_") && key.endsWith("_amount")) {
            String targetName = key.substring(
                    "bounty_".length(),
                    key.length() - "_amount".length()
            );

            if (targetName.isBlank()) {
                return null;
            }

            try {
                UUID uuid = UUID.fromString(targetName);
                return String.valueOf(bounties.getBounty(uuid));
            } catch (IllegalArgumentException ignored) {
                Player target = Bukkit.getPlayerExact(targetName);

                return target == null
                        ? null
                        : String.valueOf(bounties.getBounty(target));
            }
        }

        if (player == null) {
            return null;
        }

        return switch (key) {
            case "bounty" -> String.valueOf(bounties.getBounty(player));
            case "bounty_claimed_total" -> String.valueOf(
                    bounties.getClaimedTotal(player.getUniqueId())
            );
            case "bounty_claimed_count" -> String.valueOf(
                    bounties.getClaimedCount(player.getUniqueId())
            );
            default -> null;
        };
    }

    private String getLevelPlaceholder(Player player, String key) {
        if (player == null || levels == null) {
            return null;
        }

        int level = levels.getLevel(player);
        long xp = levels.getXP(player);
        long required = levels.getXPRequired(level);
        long remaining = Math.max(0, required - xp);

        return switch (key) {
            case "level" -> String.valueOf(level);
            case "level_xp" -> String.valueOf(xp);
            case "level_required" -> String.valueOf(required);
            case "level_remaining" -> String.valueOf(remaining);
            case "level_progress" -> String.valueOf(
                    required <= 0
                            ? 0
                            : Math.min(100, Math.round(
                            (double) xp / required * 100
                    ))
            );
            default -> null;
        };
    }

    private String getEconomyPlaceholder(Player player, String key) {
        if (economy == null) {
            return null;
        }

        if (key.equals("economy_enabled")) {
            return "true";
        }

        if (key.equals("balance_starting")) {
            return String.valueOf(economy.getStartingBalance());
        }

        if (player == null) {
            return null;
        }

        double balance = economy.getPlayerBalance(player);

        return switch (key) {
            case "balance" -> String.valueOf(balance);
            case "balance_formatted" -> economy.formatMoney(balance);
            default -> null;
        };
    }
}
