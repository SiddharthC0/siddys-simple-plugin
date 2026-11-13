/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.OfflinePlayer;

public class ssmpeconomy implements Listener {

    private final ssmp plugin;
    private final ssmpsoundsystem sounds;

    private final File mainFolder;
    private final File economyFolder;
    private final File economyStorageFile;
    private final File economyConfigFile;

    private final YamlConfiguration economyData;
    private YamlConfiguration economyConfig;

    public ssmpeconomy(
            ssmp plugin,
            ssmpsoundsystem sounds
    ) {
        this.plugin = plugin;
        this.sounds = sounds;

        mainFolder = plugin.getDataFolder();

        economyFolder = new File(
                mainFolder,
                "economy"
        );

        economyStorageFile = new File(
                economyFolder,
                "economy.yml"
        );

        economyConfigFile = new File(
                economyFolder,
                "config.yml"
        );

        if (!mainFolder.exists()
                && !mainFolder.mkdirs()) {
            throw new RuntimeException(
                    "Failed to create SSMP data folder."
            );
        }

        if (!economyFolder.exists()
                && !economyFolder.mkdirs()) {
            throw new RuntimeException(
                    "Failed to create economy folder."
            );
        }

        createEconomyFiles();

        economyData =
                YamlConfiguration.loadConfiguration(
                        economyStorageFile
                );

        economyConfig =
                YamlConfiguration.loadConfiguration(
                        economyConfigFile
                );
    }

    private void createEconomyFiles() {
        if (!economyStorageFile.exists()) {
            try {
                if (!economyStorageFile.createNewFile()) {
                    throw new IOException(
                            "Could not create economy.yml."
                    );
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create economy.yml.",
                        e
                );
            }
        }

        if (!economyConfigFile.exists()) {
            try {
                if (!economyConfigFile.createNewFile()) {
                    throw new IOException(
                            "Could not create economy config.yml."
                    );
                }

                YamlConfiguration config =
                        YamlConfiguration.loadConfiguration(
                                economyConfigFile
                        );

                addConfigDefaults(config);

                config.save(economyConfigFile);

            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create economy config.yml.",
                        e
                );
            }
        } else {
            economyConfig =
                    YamlConfiguration.loadConfiguration(
                            economyConfigFile
                    );

            addConfigDefaults(economyConfig);

            try {
                economyConfig.save(
                        economyConfigFile
                );
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to update economy config.yml.",
                        e
                );
            }
        }
    }

    private void addConfigDefaults(
            YamlConfiguration config
    ) {
        config.addDefault(
                "economy.starting-balance",
                1000.00
        );

        config.addDefault(
                "economy.allow-negative-balances",
                false
        );

        config.addDefault(
                "economy.minimum-transaction",
                0.01
        );

        config.addDefault(
                "economy.maximum-transaction",
                1000000000.00
        );

        config.addDefault(
                "display.decimal-places",
                2
        );

        config.addDefault(
                "display.use-comma-separator",
                true
        );

        config.addDefault(
                "actionbar.enabled",
                true
        );

        config.addDefault(
                "actionbar.balance",
                "&aBalance: &f$%balance%"
        );

        config.addDefault(
                "actionbar.balance-set",
                "&aBalance set to &f$%balance%"
        );

        config.addDefault(
                "actionbar.money-received",
                "&a+$%amount% &7→ &f$%balance%"
        );

        config.addDefault(
                "actionbar.money-removed",
                "&c-$%amount% &7→ &f$%balance%"
        );

        config.addDefault(
                "actionbar.payment-sent",
                "&c-$%amount% &7→ &f$%balance%"
        );

        config.addDefault(
                "actionbar.payment-received",
                "&a+$%amount% &7→ &f$%balance%"
        );

        config.addDefault(
                "actionbar.insufficient-funds",
                "&cInsufficient funds!"
        );

        config.addDefault(
                "actionbar.invalid-amount",
                "&cInvalid amount."
        );

        config.addDefault(
                "actionbar.transaction-too-small",
                "&cAmount must be at least $%amount%."
        );

        config.addDefault(
                "actionbar.transaction-too-large",
                "&cAmount cannot exceed $%amount%."
        );

        config.addDefault(
                "actionbar.negative-balance",
                "&cThis transaction would create a negative balance."
        );

        config.addDefault(
                "sounds.enabled",
                true
        );

        config.addDefault(
                "sounds.transaction",
                "message"
        );

        config.addDefault(
                "sounds.error",
                "error"
        );

        config.addDefault(
                "sounds.balance",
                "message"
        );

        config.addDefault(
                "messages.balance",
                "&a%player% has &f$%balance%"
        );

        config.addDefault(
                "messages.payment-sent",
                "&aYou paid &f$%amount% &ato &f%player%&a."
        );

        config.addDefault(
                "messages.payment-received",
                "&aYou received &f$%amount% &afrom &f%player%&a."
        );

        config.addDefault(
                "messages.give",
                "&aGave &f$%amount% &ato &f%player%&a."
        );

        config.addDefault(
                "messages.take",
                "&aTook &f$%amount% &afrom &f%player%&a."
        );

        config.addDefault(
                "messages.set",
                "&aSet &f%player%&a's balance to &f$%amount%&a."
        );

        config.addDefault(
                "messages.usage",
                "&eUsage: /%label% <balance|pay|give|take|set>"
        );

        config.addDefault(
                "messages.usage-pay",
                "&eUsage: /%label% pay <player> <amount>"
        );

        config.addDefault(
                "messages.usage-give",
                "&eUsage: /%label% give <player> <amount>"
        );

        config.addDefault(
                "messages.usage-take",
                "&eUsage: /%label% take <player> <amount>"
        );

        config.addDefault(
                "messages.usage-set",
                "&eUsage: /%label% set <player> <amount>"
        );

        config.addDefault(
                "errors.unknown-command",
                "&cUnknown economy command."
        );

        config.addDefault(
                "errors.player-only",
                "&cOnly players can use this command."
        );

        config.addDefault(
                "errors.console-player-required",
                "&cConsole must specify a player."
        );

        config.addDefault(
                "errors.player-not-found",
                "&cThat player is not online."
        );

        config.addDefault(
                "errors.self-payment",
                "&cYou cannot pay yourself."
        );

        config.addDefault(
                "errors.invalid-amount",
                "&cInvalid amount."
        );

        config.addDefault(
                "errors.negative-amount",
                "&cAmount must be greater than 0."
        );

        config.addDefault(
                "errors.insufficient-funds",
                "&cYou do not have enough money."
        );

        config.addDefault(
                "errors.no-permission",
                "&cYou do not have permission to do this."
        );

        config.addDefault(
                "errors.negative-balance",
                "&cThis transaction would create a negative balance."
        );

        config.options().copyDefaults(true);
    }

    public void reload() {
        economyConfig =
                YamlConfiguration.loadConfiguration(
                        economyConfigFile
                );

        addConfigDefaults(economyConfig);

        try {
            economyConfig.save(
                    economyConfigFile
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to save economy configuration.",
                    e
            );
        }
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean(
                "modules.economy.enabled",
                true
        );
    }

    public double getStartingBalance() {
        return economyConfig.getDouble(
                "economy.starting-balance",
                1000.00
        );
    }

    public double getMinimumTransaction() {
        return economyConfig.getDouble(
                "economy.minimum-transaction",
                0.01
        );
    }

    public double getMaximumTransaction() {
        return economyConfig.getDouble(
                "economy.maximum-transaction",
                1000000000.00
        );
    }

    public boolean allowNegativeBalances() {
        return economyConfig.getBoolean(
                "economy.allow-negative-balances",
                false
        );
    }

    public double getPlayerBalance(
            Player player
    ) {
        if (player == null) {
            return 0.00;
        }

        String uuid =
                player.getUniqueId().toString();

        return economyData.getDouble(
                "players." + uuid + ".balance",
                getStartingBalance()
        );
    }

    public boolean canUseAmount(
            double amount
    ) {
        return !Double.isNaN(amount)
                && !Double.isInfinite(amount)
                && amount >= getMinimumTransaction()
                && amount <= getMaximumTransaction();
    }

    public boolean canAfford(
            Player player,
            double amount
    ) {
        return getPlayerBalance(player) >= amount;
    }

    public boolean canRemove(
            Player player,
            double amount
    ) {
        if (allowNegativeBalances()) {
            return true;
        }

        return getPlayerBalance(player) >= amount;
    }

    public void setPlayerBalance(
            Player player,
            double balance
    ) {
        if (!isEnabled()
                || player == null) {
            return;
        }

        String uuid =
                player.getUniqueId().toString();

        economyData.set(
                "players." + uuid + ".balance",
                balance
        );

        saveEconomy();

        sendActionBar(
                player,
                getMessage(
                        "actionbar.balance-set",
                        "balance",
                        formatMoney(balance)
                )
        );

        playSound(
                player,
                "transaction"
        );
    }

    public void addPlayerBalance(
            Player player,
            double amount
    ) {
        if (!isEnabled()
                || player == null) {
            return;
        }

        String uuid =
                player.getUniqueId().toString();

        double currentBalance =
                getPlayerBalance(player);

        double finalBalance =
                currentBalance + amount;

        economyData.set(
                "players." + uuid + ".balance",
                finalBalance
        );

        saveEconomy();

        sendActionBar(
                player,
                getMessage(
                        "actionbar.money-received",
                        "amount",
                        formatMoney(amount),
                        "balance",
                        formatMoney(finalBalance)
                )
        );

        playSound(
                player,
                "transaction"
        );
    }

    public boolean removePlayerBalance(
            Player player,
            double amount
    ) {
        if (!isEnabled()
                || player == null) {
            return false;
        }

        if (!canRemove(player, amount)) {
            sendActionBar(
                    player,
                    getMessage(
                            "actionbar.negative-balance"
                    )
            );

            playSound(
                    player,
                    "error"
            );

            return false;
        }

        String uuid =
                player.getUniqueId().toString();

        double currentBalance =
                getPlayerBalance(player);

        double finalBalance =
                currentBalance - amount;

        economyData.set(
                "players." + uuid + ".balance",
                finalBalance
        );

        saveEconomy();

        sendActionBar(
                player,
                getMessage(
                        "actionbar.money-removed",
                        "amount",
                        formatMoney(amount),
                        "balance",
                        formatMoney(finalBalance)
                )
        );

        playSound(
                player,
                "transaction"
        );

        return true;
    }

    public void sendBalanceActionBar(
            Player player
    ) {
        sendActionBar(
                player,
                getMessage(
                        "actionbar.balance",
                        "balance",
                        formatMoney(
                                getPlayerBalance(player)
                        )
                )
        );

        playSound(
                player,
                "balance"
        );
    }

    public void sendPaymentSent(
            Player player,
            double amount
    ) {
        sendActionBar(
                player,
                getMessage(
                        "actionbar.payment-sent",
                        "amount",
                        formatMoney(amount),
                        "balance",
                        formatMoney(
                                getPlayerBalance(player)
                        )
                )
        );

        playSound(
                player,
                "transaction"
        );
    }

    public void sendPaymentReceived(
            Player player,
            double amount
    ) {
        sendActionBar(
                player,
                getMessage(
                        "actionbar.payment-received",
                        "amount",
                        formatMoney(amount),
                        "balance",
                        formatMoney(
                                getPlayerBalance(player)
                        )
                )
        );

        playSound(
                player,
                "transaction"
        );
    }

    public void sendActionBar(
            Player player,
            String message
    ) {
        if (player == null
                || message == null
                || !economyConfig.getBoolean(
                "actionbar.enabled",
                true
        )) {
            return;
        }

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(
                        ChatColor.translateAlternateColorCodes(
                                '&',
                                message
                        )
                )
        );
    }

    public void playSound(
            Player player,
            String type
    ) {
        if (player == null
                || !economyConfig.getBoolean(
                "sounds.enabled",
                true
        )) {
            return;
        }

        String sound =
                economyConfig.getString(
                        "sounds." + type,
                        "message"
                );

        if (sound == null
                || sound.isBlank()
                || sound.equalsIgnoreCase("none")) {
            return;
        }

        sounds.play(
                player,
                sound
        );
    }

    public String getMessage(
            String path,
            String... replacements
    ) {
        String message =
                economyConfig.getString(
                        path,
                        ""
                );

        if (message == null) {
            return "";
        }

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(
                    "%" + replacements[i] + "%",
                    replacements[i + 1]
            );
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                message
        );
    }

    public String formatMoney(
            double amount
    ) {
        int decimalPlaces =
                Math.max(
                        0,
                        economyConfig.getInt(
                                "display.decimal-places",
                                2
                        )
                );

        boolean commaSeparator =
                economyConfig.getBoolean(
                        "display.use-comma-separator",
                        true
                );

        StringBuilder pattern =
                new StringBuilder(
                        commaSeparator
                                ? "#,##0"
                                : "0"
                );

        if (decimalPlaces > 0) {
            pattern.append(".");

            pattern.append(
                    "0".repeat(decimalPlaces)
            );
        }

        DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance(
                        Locale.US
                );

        DecimalFormat format =
                new DecimalFormat(
                        pattern.toString(),
                        symbols
                );

        return format.format(amount);
    }

    @EventHandler
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {
        if (!isEnabled()) {
            return;
        }

        Player player =
                event.getPlayer();

        UUID uuid =
                player.getUniqueId();

        String path =
                "players." + uuid;

        if (!economyData.contains(
                path + ".balance"
        )) {
            economyData.set(
                    path + ".balance",
                    getStartingBalance()
            );
        }

        economyData.set(
                path + ".name",
                player.getName()
        );

        saveEconomy();

        sendBalanceActionBar(player);
    }

    private void saveEconomy() {
        try {
            economyData.save(
                    economyStorageFile
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to save economy data.",
                    e
            );
        }
    }

    public boolean isEnabledForVault() {
        return isEnabled();
    }

    public int getDecimalPlaces() {
        return Math.max(
                0,
                economyConfig.getInt(
                        "display.decimal-places",
                        2
                )
        );
    }

    public double getPlayerBalance(
            OfflinePlayer player
    ) {
        if (player == null) {
            return 0.0;
        }

        String uuid = player.getUniqueId().toString();

        return economyData.getDouble(
                "players." + uuid + ".balance",
                getStartingBalance()
        );
    }

    public boolean hasAccount(
            OfflinePlayer player
    ) {
        if (player == null) {
            return false;
        }

        return economyData.contains(
                "players."
                        + player.getUniqueId()
                        + ".balance"
        );
    }

    public boolean createAccount(
            OfflinePlayer player
    ) {
        if (!isEnabled() || player == null) {
            return false;
        }

        String path =
                "players." + player.getUniqueId();

        if (!economyData.contains(path + ".balance")) {
            economyData.set(
                    path + ".balance",
                    getStartingBalance()
            );

            economyData.set(
                    path + ".name",
                    player.getName()
            );

            saveEconomy();
        }

        return true;
    }

    public boolean addOfflinePlayerBalance(
            OfflinePlayer player,
            double amount
    ) {
        if (!isEnabled()
                || player == null
                || !canUseAmount(amount)) {
            return false;
        }

        double finalBalance =
                getPlayerBalance(player) + amount;

        if (!Double.isFinite(finalBalance)) {
            return false;
        }

        economyData.set(
                "players."
                        + player.getUniqueId()
                        + ".balance",
                finalBalance
        );

        saveEconomy();

        return true;
    }

    public boolean removeOfflinePlayerBalance(
            OfflinePlayer player,
            double amount
    ) {
        if (!isEnabled()
                || player == null
                || !canUseAmount(amount)
                || !canRemove(player, amount)) {
            return false;
        }

        double finalBalance =
                getPlayerBalance(player) - amount;

        economyData.set(
                "players."
                        + player.getUniqueId()
                        + ".balance",
                finalBalance
        );

        saveEconomy();

        return true;
    }

    public boolean canRemove(
            OfflinePlayer player,
            double amount
    ) {
        return allowNegativeBalances()
                || getPlayerBalance(player) >= amount;
    }

}