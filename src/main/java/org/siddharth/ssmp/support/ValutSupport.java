
/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp.support;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.siddharth.ssmp.economy.economy;
import org.siddharth.ssmp.ssmp;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("deprecation")
public class ValutSupport implements Economy {

    private final ssmp plugin;
    private final economy economy;

    public ValutSupport(ssmp plugin, economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled() && economy.isEnabledForVault();
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
        return Math.max(0, economy.getDecimalPlaces());
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
    public boolean hasAccount(String playerName) {
        return hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null && economy.hasAccount(player);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        return getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return player == null ? 0.0 : economy.getPlayerBalance(player);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return isEnabled()
                && player != null
                && economy.canUseAmount(amount)
                && getBalance(player) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(
            String playerName,
            String worldName,
            double amount
    ) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
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
    public EconomyResponse withdrawPlayer(
            OfflinePlayer player,
            String worldName,
            double amount
    ) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(
            String playerName,
            String worldName,
            double amount
    ) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
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
    public EconomyResponse depositPlayer(
            OfflinePlayer player,
            String worldName,
            double amount
    ) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return player != null && economy.createAccount(player);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    private EconomyResponse success(double amount, OfflinePlayer player) {
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
                getBalance(player),
                EconomyResponse.ResponseType.FAILURE,
                message
        );
    }

    private EconomyResponse notImplemented() {
        return new EconomyResponse(
                0.0,
                0.0,
                EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "SSMP does not support bank accounts."
        );
    }
}
