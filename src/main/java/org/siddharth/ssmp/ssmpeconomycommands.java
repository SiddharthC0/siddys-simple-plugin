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

public class ssmpeconomycommands
        implements CommandExecutor, TabCompleter {

    private final ssmpeconomy economy;

    public ssmpeconomycommands(
            ssmpeconomy economy
    ) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (args.length == 0) {
            send(
                    sender,
                    economy.getMessage(
                            "messages.usage",
                            "label",
                            label
                    )
            );

            return true;
        }

        String subcommand =
                args[0].toLowerCase();

        switch (subcommand) {

            case "balance":
            case "bal":
                return balance(
                        sender,
                        args
                );

            case "pay":
                return pay(
                        sender,
                        args,
                        label
                );

            case "give":
                return give(
                        sender,
                        args,
                        label
                );

            case "take":
                return take(
                        sender,
                        args,
                        label
                );

            case "set":
                return set(
                        sender,
                        args,
                        label
                );

            default:
                send(
                        sender,
                        economy.getMessage(
                                "errors.unknown-command"
                        )
                );

                return true;
        }
    }

    private boolean balance(
            CommandSender sender,
            String[] args
    ) {
        Player target;

        if (args.length >= 2) {
            target =
                    Bukkit.getPlayerExact(
                            args[1]
                    );

            if (target == null) {
                send(
                        sender,
                        economy.getMessage(
                                "errors.player-not-found"
                        )
                );

                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                send(
                        sender,
                        economy.getMessage(
                                "errors.console-player-required"
                        )
                );

                return true;
            }

            target =
                    (Player) sender;
        }

        String message =
                economy.getMessage(
                        "messages.balance",
                        "player",
                        target.getName(),
                        "balance",
                        economy.formatMoney(
                                economy.getPlayerBalance(target)
                        )
                );

        if (target.equals(sender)) {
            economy.sendBalanceActionBar(target);
        } else {
            send(
                    sender,
                    message
            );
        }

        return true;
    }

    private boolean pay(
            CommandSender sender,
            String[] args,
            String label
    ) {
        if (!(sender instanceof Player)) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.player-only"
                    )
            );

            return true;
        }

        if (args.length < 3) {
            send(
                    sender,
                    economy.getMessage(
                            "messages.usage-pay",
                            "label",
                            label
                    )
            );

            return true;
        }

        Player payer =
                (Player) sender;

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (target == null) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.player-not-found"
                    )
            );

            return true;
        }

        if (target.equals(payer)) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.self-payment"
                    )
            );

            economy.sendActionBar(
                    payer,
                    economy.getMessage(
                            "actionbar.invalid-amount"
                    )
            );

            economy.playSound(
                    payer,
                    "error"
            );

            return true;
        }

        Double amount =
                parseAmount(
                        sender,
                        args[2]
                );

        if (amount == null) {
            return true;
        }

        if (!economy.canUseAmount(amount)) {
            handleAmountLimit(
                    sender,
                    amount
            );

            return true;
        }

        if (!economy.canAfford(
                payer,
                amount
        )) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.insufficient-funds"
                    )
            );

            economy.sendActionBar(
                    payer,
                    economy.getMessage(
                            "actionbar.insufficient-funds"
                    )
            );

            economy.playSound(
                    payer,
                    "error"
            );

            return true;
        }

        if (!economy.removePlayerBalance(
                payer,
                amount
        )) {
            return true;
        }

        economy.addPlayerBalance(
                target,
                amount
        );

        economy.sendPaymentSent(
                payer,
                amount
        );

        economy.sendPaymentReceived(
                target,
                amount
        );

        send(
                sender,
                economy.getMessage(
                        "messages.payment-sent",
                        "amount",
                        economy.formatMoney(amount),
                        "player",
                        target.getName()
                )
        );

        send(
                target,
                economy.getMessage(
                        "messages.payment-received",
                        "amount",
                        economy.formatMoney(amount),
                        "player",
                        payer.getName()
                )
        );

        return true;
    }

    private boolean give(
            CommandSender sender,
            String[] args,
            String label
    ) {
        if (!sender.hasPermission(
                "ssmp.economy.give"
        )) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.no-permission"
                    )
            );

            return true;
        }

        if (args.length < 3) {
            send(
                    sender,
                    economy.getMessage(
                            "messages.usage-give",
                            "label",
                            label
                    )
            );

            return true;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (target == null) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.player-not-found"
                    )
            );

            return true;
        }

        Double amount =
                parseAmount(
                        sender,
                        args[2]
                );

        if (amount == null) {
            return true;
        }

        if (!economy.canUseAmount(amount)) {
            handleAmountLimit(
                    sender,
                    amount
            );

            return true;
        }

        economy.addPlayerBalance(
                target,
                amount
        );

        send(
                sender,
                economy.getMessage(
                        "messages.give",
                        "amount",
                        economy.formatMoney(amount),
                        "player",
                        target.getName()
                )
        );

        return true;
    }

    private boolean take(
            CommandSender sender,
            String[] args,
            String label
    ) {
        if (!sender.hasPermission(
                "ssmp.economy.take"
        )) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.no-permission"
                    )
            );

            return true;
        }

        if (args.length < 3) {
            send(
                    sender,
                    economy.getMessage(
                            "messages.usage-take",
                            "label",
                            label
                    )
            );

            return true;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (target == null) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.player-not-found"
                    )
            );

            return true;
        }

        Double amount =
                parseAmount(
                        sender,
                        args[2]
                );

        if (amount == null) {
            return true;
        }

        if (!economy.canUseAmount(amount)) {
            handleAmountLimit(
                    sender,
                    amount
            );

            return true;
        }

        if (!economy.allowNegativeBalances()
                && !economy.canAfford(
                target,
                amount
        )) {

            amount =
                    economy.getPlayerBalance(
                            target
                    );
        }

        if (!economy.removePlayerBalance(
                target,
                amount
        )) {
            return true;
        }

        send(
                sender,
                economy.getMessage(
                        "messages.take",
                        "amount",
                        economy.formatMoney(amount),
                        "player",
                        target.getName()
                )
        );

        return true;
    }

    private boolean set(
            CommandSender sender,
            String[] args,
            String label
    ) {
        if (!sender.hasPermission(
                "ssmp.economy.set"
        )) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.no-permission"
                    )
            );

            return true;
        }

        if (args.length < 3) {
            send(
                    sender,
                    economy.getMessage(
                            "messages.usage-set",
                            "label",
                            label
                    )
            );

            return true;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[1]
                );

        if (target == null) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.player-not-found"
                    )
            );

            return true;
        }

        Double amount =
                parseAmount(
                        sender,
                        args[2]
                );

        if (amount == null) {
            return true;
        }

        if (amount < 0
                && !economy.allowNegativeBalances()) {

            send(
                    sender,
                    economy.getMessage(
                            "errors.negative-balance"
                    )
            );

            return true;
        }

        economy.setPlayerBalance(
                target,
                amount
        );

        send(
                sender,
                economy.getMessage(
                        "messages.set",
                        "amount",
                        economy.formatMoney(amount),
                        "player",
                        target.getName()
                )
        );

        return true;
    }

    private Double parseAmount(
            CommandSender sender,
            String input
    ) {
        try {
            double amount =
                    Double.parseDouble(input);

            if (Double.isNaN(amount)
                    || Double.isInfinite(amount)) {

                send(
                        sender,
                        economy.getMessage(
                                "errors.invalid-amount"
                        )
                );

                return null;
            }

            if (amount <= 0) {
                send(
                        sender,
                        economy.getMessage(
                                "errors.negative-amount"
                        )
                );

                return null;
            }

            return amount;

        } catch (NumberFormatException e) {
            send(
                    sender,
                    economy.getMessage(
                            "errors.invalid-amount"
                    )
            );

            return null;
        }
    }

    private void handleAmountLimit(
            CommandSender sender,
            double amount
    ) {
        if (amount <
                economy.getMinimumTransaction()) {

            send(
                    sender,
                    economy.getMessage(
                            "errors.invalid-amount"
                    )
            );

            return;
        }

        if (amount >
                economy.getMaximumTransaction()) {

            send(
                    sender,
                    economy.getMessage(
                            "errors.invalid-amount"
                    )
            );
        }
    }

    private void send(
            CommandSender sender,
            String message
    ) {
        if (message == null
                || message.isBlank()) {
            return;
        }

        sender.sendMessage(message);
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        List<String> completions =
                new ArrayList<>();

        if (args.length == 1) {
            completions.add("balance");
            completions.add("bal");
            completions.add("pay");

            if (sender.hasPermission(
                    "ssmp.economy.give"
            )) {
                completions.add("give");
            }

            if (sender.hasPermission(
                    "ssmp.economy.take"
            )) {
                completions.add("take");
            }

            if (sender.hasPermission(
                    "ssmp.economy.set"
            )) {
                completions.add("set");
            }

            return filter(
                    completions,
                    args[0]
            );
        }

        if (args.length == 2) {
            String subcommand =
                    args[0].toLowerCase();

            if (subcommand.equals("balance")
                    || subcommand.equals("bal")
                    || subcommand.equals("pay")
                    || subcommand.equals("give")
                    || subcommand.equals("take")
                    || subcommand.equals("set")) {

                for (Player player :
                        Bukkit.getOnlinePlayers()) {

                    completions.add(
                            player.getName()
                    );
                }

                return filter(
                        completions,
                        args[1]
                );
            }
        }

        return completions;
    }

    private List<String> filter(
            List<String> values,
            String input
    ) {
        List<String> result =
                new ArrayList<>();

        for (String value : values) {
            if (value.toLowerCase()
                    .startsWith(
                            input.toLowerCase()
                    )) {

                result.add(value);
            }
        }

        return result;
    }
}