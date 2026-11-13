/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class ssmpvaulteconomy extends AbstractEconomy {

    private final ssmp plugin;
    private final ssmpeconomy economy;

    public ssmpvaulteconomy(
            ssmp plugin,
            ssmpeconomy economy
    ) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled()
                && economy.isEnabledForVault();
    }

    @Override
    public String getName() {
        return "SSMP";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return Math.max(
                0,
                economy.getDecimalPlaces()
        );
    }

    @Override
    public String format(double amount) {
        return "$" + economy.formatMoney(amount);
    }

    @Override
    public String currencyNamePlural() {
        return "Dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "Dollar";
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player =
                Bukkit.getOfflinePlayer(playerName);

        return getBalance(player);
    }

    @Override
    public double getBalance(
            OfflinePlayer player
    ) {
        return economy.getPlayerBalance(player);
    }

    @Override
    public boolean has(
            String playerName,
            double amount
    ) {
        return has(
                Bukkit.getOfflinePlayer(playerName),
                amount
        );
    }

    @Override
    public boolean has(
            OfflinePlayer player,
            double amount
    ) {
        return player != null
                && economy.canUseAmount(amount)
                && getBalance(player) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(
            String playerName,
            double amount
    ) {
        return withdrawPlayer(
                Bukkit.getOfflinePlayer(playerName),
                amount
        );
    }

    @Override
    public EconomyResponse withdrawPlayer(
            OfflinePlayer player,
            double amount
    ) {
        if (!isEnabled()) {
            return failure(amount, player, "Economy is disabled.");
        }

        if (player == null || !economy.canUseAmount(amount)) {
            return failure(amount, player, "Invalid transaction amount.");
        }

        if (!economy.removeOfflinePlayerBalance(player, amount)) {
            return failure(amount, player, "Insufficient funds.");
        }

        return success(amount, player);
    }

    @Override
    public EconomyResponse depositPlayer(
            String playerName,
            double amount
    ) {
        return depositPlayer(
                Bukkit.getOfflinePlayer(playerName),
                amount
        );
    }

    @Override
    public EconomyResponse depositPlayer(
            OfflinePlayer player,
            double amount
    ) {
        if (!isEnabled()) {
            return failure(amount, player, "Economy is disabled.");
        }

        if (player == null || !economy.canUseAmount(amount)) {
            return failure(amount, player, "Invalid transaction amount.");
        }

        if (!economy.addOfflinePlayerBalance(player, amount)) {
            return failure(amount, player, "Transaction failed.");
        }

        return success(amount, player);
    }

    @Override
    public boolean hasAccount(String playerName) {
        return hasAccount(
                Bukkit.getOfflinePlayer(playerName)
        );
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null
                && economy.hasAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(
                Bukkit.getOfflinePlayer(playerName)
        );
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return player != null
                && economy.createAccount(player);
    }

    private EconomyResponse success(
            double amount,
            OfflinePlayer player
    ) {
        return new EconomyResponse(
                amount,
                getBalance(player),
                EconomyResponse.ResponseType.SUCCESS,
                null
        );
    }

    private EconomyResponse failure(
            double amount,
            OfflinePlayer player,
            String message
    ) {
        return new EconomyResponse(
                amount,
                player == null ? 0.0 : getBalance(player),
                EconomyResponse.ResponseType.FAILURE,
                message
        );
    }
}
