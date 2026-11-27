package org.siddharth.ssmp.economy;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.mechanics.SoundSystem;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import org.siddharth.ssmp.mechanics.SoundSystem;

public class Economy implements Listener {
    private final SiddysSimplePlugin plugin;
    private final SoundSystem sounds;
    private final File economyStorageFile;
    private final File economyConfigFile;
    private final YamlConfiguration economyData;
    private YamlConfiguration economyConfig;

    public Economy(SiddysSimplePlugin plugin, SoundSystem sounds) {
        this.plugin = plugin;
        this.sounds = sounds;

        File mainFolder = plugin.getDataFolder();
        File economyFolder = new File(mainFolder, "economy");

        economyStorageFile = new File(economyFolder, "economy.yml");
        economyConfigFile = new File(economyFolder, "config.yml");

        if (!mainFolder.exists() && !mainFolder.mkdirs()) {
            throw new RuntimeException("Failed to create SSMP data folder.");
        }

        if (!economyFolder.exists() && !economyFolder.mkdirs()) {
            throw new RuntimeException("Failed to create economy folder.");
        }

        createEconomyFiles();

        economyData = YamlConfiguration.loadConfiguration(economyStorageFile);
    }

    private void createEconomyFiles() {
        createFile(economyStorageFile, "economy.yml");
        createFile(economyConfigFile, "economy config.yml");

        economyConfig = YamlConfiguration.loadConfiguration(economyConfigFile);
        addConfigDefaults(economyConfig);
        saveConfiguration(economyConfig, economyConfigFile, "economy configuration");
    }

    private void createFile(File file, String name) {
        if (file.exists()) return;

        try {
            if (!file.createNewFile()) {
                throw new IOException("Could not create " + name + ".");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + name + ".", e);
        }
    }

    private void saveConfiguration(YamlConfiguration config, File file, String name) {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + name + ".", e);
        }
    }

    private void addConfigDefaults(YamlConfiguration config) {
        config.addDefaults(Map.ofEntries(
                Map.entry("economy.starting-balance", 100.00),
                Map.entry("economy.allow-negative-balances", false),
                Map.entry("economy.minimum-transaction", 0.01),
                Map.entry("economy.maximum-transaction", 1000000000.00),

                Map.entry("display.decimal-places", 2),
                Map.entry("display.use-comma-separator", true),

                Map.entry("actionbar.enabled", true),
                Map.entry("actionbar.balance", "&aBalance: &f$%balance%"),
                Map.entry("actionbar.balance-set", "&aBalance set to &f$%balance%"),
                Map.entry("actionbar.money-received", "&a+$%amount% &7→ &f$%balance%"),
                Map.entry("actionbar.money-removed", "&c-$%amount% &7→ &f$%balance%"),
                Map.entry("actionbar.payment-sent", "&c-$%amount% &7→ &f$%balance%"),
                Map.entry("actionbar.payment-received", "&a+$%amount% &7→ &f$%balance%"),
                Map.entry("actionbar.insufficient-funds", "&cInsufficient funds!"),
                Map.entry("actionbar.invalid-amount", "&cInvalid amount."),
                Map.entry("actionbar.transaction-too-small", "&cAmount must be at least $%amount%."),
                Map.entry("actionbar.transaction-too-large", "&cAmount cannot exceed $%amount%."),
                Map.entry("actionbar.negative-balance", "&cThis transaction would create a negative balance."),

                Map.entry("sounds.enabled", true),
                Map.entry("sounds.transaction", "message"),
                Map.entry("sounds.error", "error"),
                Map.entry("sounds.balance", "message"),

                Map.entry("messages.balance", "&a%player% has &f$%balance%"),
                Map.entry("messages.payment-sent", "&aYou paid &f$%amount% &ato &f%player%&a."),
                Map.entry("messages.payment-received", "&aYou received &f$%amount% &afrom &f%player%&a."),
                Map.entry("messages.give", "&aGave &f$%amount% &ato &f%player%&a."),
                Map.entry("messages.take", "&aTook &f$%amount% &afrom &f%player%&a."),
                Map.entry("messages.set", "&aSet &f%player%&a's balance to &f$%amount%&a."),
                Map.entry("messages.usage", "&eUsage: /%label% <balance|pay|give|take|set>"),
                Map.entry("messages.usage-pay", "&eUsage: /%label% pay <player> <amount>"),
                Map.entry("messages.usage-give", "&eUsage: /%label% give <player> <amount>"),
                Map.entry("messages.usage-take", "&eUsage: /%label% take <player> <amount>"),
                Map.entry("messages.usage-set", "&eUsage: /%label% set <player> <amount>"),

                Map.entry("errors.unknown-command", "&cUnknown economy command."),
                Map.entry("errors.player-only", "&cOnly players can use this command."),
                Map.entry("errors.console-player-required", "&cConsole must specify a player."),
                Map.entry("errors.player-not-found", "&cThat player is not online."),
                Map.entry("errors.self-payment", "&cYou cannot pay yourself."),
                Map.entry("errors.invalid-amount", "&cInvalid amount."),
                Map.entry("errors.negative-amount", "&cAmount must be greater than 0."),
                Map.entry("errors.insufficient-funds", "&cYou do not have enough money."),
                Map.entry("errors.no-permission", "&cYou do not have permission to do this."),
                Map.entry("errors.negative-balance", "&cThis transaction would create a negative balance.")
        ));
        config.options().copyDefaults(true);
    }

    // Be sure to implement ts future me
    public void reload() {
        economyConfig = YamlConfiguration.loadConfiguration(economyConfigFile);
        addConfigDefaults(economyConfig);
        saveConfiguration(economyConfig, economyConfigFile, "economy configuration");
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("modules.economy.enabled", true);
    }

    public double getStartingBalance() {
        return economyConfig.getDouble("economy.starting-balance", 100.00);
    }

    public double getMinimumTransaction() {
        return economyConfig.getDouble("economy.minimum-transaction", 0.01);
    }

    public double getMaximumTransaction() {
        return economyConfig.getDouble("economy.maximum-transaction", 1000000000.00);
    }

    public boolean allowNegativeBalances() {
        return economyConfig.getBoolean("economy.allow-negative-balances", false);
    }

    public double getPlayerBalance(Player player) {
        if (player == null) return 0.00;

        return economyData.getDouble("players." + player.getUniqueId() + ".balance", getStartingBalance());
    }

    public double getPlayerBalance(OfflinePlayer player) {
        if (player == null) return 0.00;

        return economyData.getDouble("players." + player.getUniqueId() + ".balance", getStartingBalance());
    }

    public boolean canUseAmount(double amount) {
        return Double.isFinite(amount) && amount >= getMinimumTransaction() && amount <= getMaximumTransaction();
    }

    public boolean canAfford(Player player, double amount) {
        return getPlayerBalance(player) >= amount;
    }

    public boolean canRemove(Player player, double amount) {
        return allowNegativeBalances() || getPlayerBalance(player) >= amount;
    }

    public boolean canRemove(OfflinePlayer player, double amount) {
        return allowNegativeBalances() || getPlayerBalance(player) >= amount;
    }

    public void setPlayerBalance(Player player, double balance) {
        if (!isEnabled() || player == null) return;

        economyData.set("players." + player.getUniqueId() + ".balance", balance);
        saveEconomy();
        if (plugin.getToggleSystem().isActionBarEnabled(player)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getMessage("actionbar.balance-set", "balance", formatMoney(balance))));
        }
        if (plugin.getToggleSystem().isSoundEnabled(player)) {
            playSound(player, "transaction");
        }
    }

    public boolean addPlayerBalance(Player player, double amount) {
        if (!isEnabled() || player == null) return false;

        double finalBalance = getPlayerBalance(player) + amount;
        economyData.set("players." + player.getUniqueId() + ".balance", finalBalance);
        saveEconomy();

        if (plugin.getToggleSystem().isActionBarEnabled(player)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getMessage("actionbar.money-received", "amount", formatMoney(amount), "balance", formatMoney(finalBalance))));
        }
        if (plugin.getToggleSystem().isSoundEnabled(player)) {
            playSound(player, "transaction");
        }
        return true;
    }
    public boolean removePlayerBalance(Player player, double amount) {
        if (!isEnabled() || player == null) return false;

        if (!canRemove(player, amount)) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getMessage("actionbar.negative-balance")));
            playSound(player, "menu-error");
            return false;
        }

        double finalBalance = getPlayerBalance(player) - amount;
        economyData.set("players." + player.getUniqueId() + ".balance", finalBalance);
        saveEconomy();

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getMessage("actionbar.money-removed", "amount", formatMoney(amount), "balance", formatMoney(finalBalance))));

        playSound(player, "transaction");
        return true;
    }

    public void sendBalanceActionBar(Player player) {
        if (player == null) return;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getMessage("actionbar.balance", "balance", formatMoney(getPlayerBalance(player)))));

        playSound(player, "menu-success");
    }

    public void sendPaymentSent(Player player, double amount) {
        if (player == null) return;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(getMessage("actionbar.payment-sent", "amount", formatMoney(amount), "balance", formatMoney(getPlayerBalance(player)))));

        playSound(player, "transaction");
    }

    public void sendPaymentReceived(Player player, double amount) {
        if (player == null) return;

        if (plugin.getToggleSystem().isActionBarEnabled(player)) {
            sendActionBar(player, getMessage(
                    "actionbar.payment-received",
                    "amount", formatMoney(amount),
                    "balance", formatMoney(getPlayerBalance(player))
            ));
        }

        if (plugin.getToggleSystem().isSoundEnabled(player)) {
            playSound(player, "transaction");
        }
    }

    // Uhhh if you forked this repo just use this method instead of the big player.spigot() thingy pls
    public void sendActionBar(Player player, String message) {
        if (player == null || message == null || !economyConfig.getBoolean("actionbar.enabled", true)) return;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    public void playSound(Player player, String type) {
        if (player == null || !economyConfig.getBoolean("sounds.enabled", true)) return;

        String sound = economyConfig.getString("sounds." + type, "message");
        if (sound == null || sound.isBlank() || sound.equalsIgnoreCase("none")) return;

        sounds.play(player, sound);
    }

    public String getMessage(String path, String... replacements) {
        String message = economyConfig.getString(path, "");
        if (message == null) return "";

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String formatMoney(double amount) {
        int decimalPlaces = Math.max(0, economyConfig.getInt("display.decimal-places", 2));
        boolean commaSeparator = economyConfig.getBoolean("display.use-comma-separator", true);

        String pattern = (commaSeparator ? "#,##0" : "0") + (decimalPlaces > 0 ? "." + "0".repeat(decimalPlaces) : "");

        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)
        );

        return format.format(amount);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String path = "players." + player.getUniqueId();

        if (!economyData.contains(path + ".balance")) {
            economyData.set(path + ".balance", getStartingBalance());
        }

        economyData.set(path + ".name", player.getName());
        saveEconomy();
        sendBalanceActionBar(player);
    }

    private void saveEconomy() {
        try {
            economyData.save(economyStorageFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save economy data.", e);
        }
    }

    public boolean isEnabledForVault() {
        return isEnabled();
    }

    public int getDecimalPlaces() {
        return Math.max(0, economyConfig.getInt("display.decimal-places", 2));
    }

    public boolean hasAccount(OfflinePlayer player) {
        if (player == null) return false;

        return economyData.contains("players." + player.getUniqueId() + ".balance");
    }

    public boolean createAccount(OfflinePlayer player) {
        if (!isEnabled() || player == null) return false;

        String path = "players." + player.getUniqueId();

        if (!economyData.contains(path + ".balance")) {
            economyData.set(path + ".balance", getStartingBalance());
            economyData.set(path + ".name", player.getName());
            saveEconomy();
        }

        return true;
    }

    public boolean addOfflinePlayerBalance(OfflinePlayer player, double amount) {
        if (!isEnabled() || player == null || !canUseAmount(amount)) return false;

        double finalBalance = getPlayerBalance(player) + amount;
        if (!Double.isFinite(finalBalance)) return false;

        economyData.set("players." + player.getUniqueId() + ".balance", finalBalance);
        saveEconomy();
        return true;
    }

    public boolean removeOfflinePlayerBalance(OfflinePlayer player, double amount) {
        if (!isEnabled() || player == null || !canUseAmount(amount)
                || !canRemove(player, amount)) return false;

        double finalBalance = getPlayerBalance(player) - amount;

        economyData.set("players." + player.getUniqueId() + ".balance", finalBalance);
        saveEconomy();
        return true;
    }
}
