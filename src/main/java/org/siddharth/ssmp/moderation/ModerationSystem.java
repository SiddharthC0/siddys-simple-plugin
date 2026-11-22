package org.siddharth.ssmp.moderation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ModerationSystem implements Listener {
    private record Violation(int count, long lastAt) {}
    private record FilterOutcome(String message, boolean block, boolean matched, Set<String> categories) {}

    private final JavaPlugin plugin;
    private final ModerationDatabase database;
    private final Map<String, String> terms = new ConcurrentHashMap<>();
    private final Map<UUID, Violation> violations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> mutes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bans = new ConcurrentHashMap<>();
    private volatile boolean enabled;
    private volatile boolean notifyModerators;
    private volatile long muteDuration;
    private volatile long violationWindow;
    private volatile int warningLimit;
    private volatile String maskCharacter;
    private volatile int maskLength;
    private volatile ModerationFilter filter;

    public ModerationSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        loadSettings();

        try {
            database = new ModerationDatabase(new File(plugin.getDataFolder(), "moderation/moderation.db"));

            Map<String, String> defaultTerms = ModerationLoader.load(plugin);
            database.importDefaultTerms(defaultTerms);

            ModerationDatabase.Snapshot snapshot = database.loadSnapshot();

            terms.putAll(snapshot.terms());
            snapshot.violations().forEach((uuid, state) -> violations.put(uuid, new Violation(state.count(), state.lastViolation())));
            snapshot.mutes().forEach((uuid, mute) -> mutes.put(uuid, mute.expiresAt()));
            snapshot.bans().forEach((uuid, ban) -> bans.put(uuid, ban.expiresAt()));

            rebuildFilter();
        } catch (SQLException exception) {
            throw new IllegalStateException("SSMP moderation could not start: " + exception.getMessage(), exception);
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadSettings() {
        FileConfiguration config = plugin.getConfig();
        config.addDefault("moderation.enabled", true);
        config.addDefault("moderation.chat.warn-before-mute", 3);
        config.addDefault("moderation.chat.mute-duration", "30m");
        config.addDefault("moderation.chat.violation-window", "24h");
        config.addDefault("moderation.chat.mask-character", "*");
        config.addDefault("moderation.chat.mask-length", 4);
        config.addDefault("moderation.chat.notify-moderators", true);
        config.addDefault("moderation.chat.default-action", "BOTH");
        config.addDefault("moderation.chat.category-actions", new LinkedHashMap<String, String>());
        config.options().copyDefaults(true);
        plugin.saveConfig();

        enabled = config.getBoolean("moderation.enabled", true);
        warningLimit = Math.max(0, config.getInt("moderation.chat.warn-before-mute", 3));
        muteDuration = parseDuration(config.getString("moderation.chat.mute-duration", "30m"), 30 * 60_000L);
        violationWindow = parseDuration(config.getString("moderation.chat.violation-window", "24h"), 24 * 60 * 60_000L);
        notifyModerators = config.getBoolean("moderation.chat.notify-moderators", true);
        maskCharacter = config.getString("moderation.chat.mask-character", "*");
        maskLength = Math.max(1, Math.min(16, config.getInt("moderation.chat.mask-length", 4)));
    }

    private void rebuildFilter() {
        filter = new ModerationFilter(terms);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Long expiry = bans.get(event.getPlayer().getUniqueId());
        if (expiry == null) return;
        if (expiry != 0 && expiry <= System.currentTimeMillis()) {
            bans.remove(event.getPlayer().getUniqueId(), expiry);
            database.unban(event.getPlayer().getUniqueId(), event.getPlayer().getName(), "CONSOLE");
            return;
        }
        String message = expiry == 0 ? "You are permanently banned from this server." : "You are banned for another " + formatDuration(expiry - System.currentTimeMillis()) + ".";
        Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().kickPlayer(ChatColor.RED + message));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if (player.hasPermission("ssmp.moderation.bypass")) return;

        long now = System.currentTimeMillis();
        Long muteExpiry = mutes.get(player.getUniqueId());
        if (muteExpiry != null) {
            if (muteExpiry == 0 || muteExpiry > now) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.RED + "You are muted" + (muteExpiry == 0 ? " permanently." : " for " + formatDuration(muteExpiry - System.currentTimeMillis()) + ".")));
                return;
            }
            mutes.remove(player.getUniqueId(), muteExpiry);
            database.unmute(player.getUniqueId(), player.getName(), "CONSOLE").exceptionally(error -> { logDatabaseFailure("expire mute", error); return null; });
        }

        ModerationFilter.Result result = filter.apply(event.getMessage(), maskCharacter, maskLength);
        if (!result.matched()) return;

        event.setMessage(result.message());
        Violation previous = violations.get(player.getUniqueId());
        int count = previous == null || now - previous.lastAt() > violationWindow ? 1 : previous.count() + 1;
        violations.put(player.getUniqueId(), new Violation(count, now));
        database.saveViolation(player.getUniqueId(), count, now).exceptionally(error -> { logDatabaseFailure("save chat violation", error); return null; });
        database.log(player.getUniqueId(), player.getName(), "CONSOLE", "CHAT_FILTER", "Filtered chat violation " + count + "; categories=" + String.join(",", result.categories())).exceptionally(error -> { logDatabaseFailure("save chat-filter log", error); return null; });

        boolean block = shouldBlock(result.categories());
        if (count > warningLimit) {
            event.setCancelled(true);
            long expiresAt = muteDuration == 0 ? 0 : now + muteDuration;
            mutes.put(player.getUniqueId(), expiresAt);
            database.saveMute(player.getUniqueId(), player.getName(), "CONSOLE", "Automatic mute after repeated chat filter violations", expiresAt).exceptionally(error -> { logDatabaseFailure("save automatic mute", error); return null; });
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "You have been muted for " + (muteDuration == 0 ? "an indefinite period" : formatDuration(muteDuration)) + " after repeated chat filter violations.");
                notifyModerators(ChatColor.RED + player.getName() + " was automatically muted after " + count + " chat filter violations.");
            });
            return;
        }

        if (block) event.setCancelled(true);
        int current = count;
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(ChatColor.YELLOW + "Chat warning " + current + "/" + warningLimit + ": your message matched the chat filter.");
            if (notifyModerators) notifyModerators(ChatColor.GRAY + player.getName() + " triggered the chat filter (" + current + "/" + warningLimit + ").");
            if (block) player.sendMessage(ChatColor.RED + "Your message was blocked by the chat filter.");
        });
    }

    private boolean shouldBlock(Set<String> categories) {
        String fallback = plugin.getConfig().getString("moderation.chat.default-action", "BOTH");
        boolean block = false;
        for (String category : categories) {
            String action = plugin.getConfig().getString("moderation.chat.category-actions." + category, fallback);
            action = action == null ? "BOTH" : action.toUpperCase(Locale.ROOT);
            if (action.equals("BLOCK")) block = true;
        }
        return block;
    }

    public void addTerm(String rawTerm, String rawCategory, String actor, java.util.function.Consumer<String> callback) {
        String term = rawTerm.toLowerCase(Locale.ROOT).trim();
        String category = rawCategory.toUpperCase(Locale.ROOT).trim();
        if (term.isEmpty() || term.length() > 80 || !term.codePoints().anyMatch(Character::isLetterOrDigit)
                || !category.matches("[A-Z0-9_-]{1,24}")) {
            callback.accept("Invalid term or category. Terms must be 1-80 characters; categories may use letters, numbers, _ and -.");
            return;
        }
        database.addTerm(term, category, actor).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) callback.accept("Could not save term: " + rootMessage(error));
            else { terms.put(term, category); rebuildFilter(); callback.accept("Added filter term in category " + category + "."); }
        }));
    }

    public void removeTerm(String rawTerm, java.util.function.Consumer<String> callback) {
        String term = rawTerm.toLowerCase(Locale.ROOT).trim();
        database.removeTerm(term).whenComplete((removed, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) callback.accept("Could not remove term: " + rootMessage(error));
            else if (!removed) callback.accept("That term was not found.");
            else { terms.remove(term); rebuildFilter(); callback.accept("Removed filter term."); }
        }));
    }

    public void reload(java.util.function.Consumer<String> callback) {
        plugin.reloadConfig();
        reloadFromConfig();
        callback.accept("Moderation settings reloaded. Database caches retained.");
    }

    public void reloadFromConfig() {
        loadSettings();
        rebuildFilter();
    }

    public void mute(UUID uuid, String name, String moderator, String reason, long duration, java.util.function.Consumer<String> callback) {
        long expires = duration == 0 ? 0 : System.currentTimeMillis() + duration;
        mutes.put(uuid, expires);
        database.saveMute(uuid, name, moderator, reason, expires).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) callback.accept("Mute could not be saved: " + rootMessage(error));
            else { Player online = Bukkit.getPlayer(uuid); if (online != null) online.sendMessage(ChatColor.RED + "You have been muted. Reason: " + reason); callback.accept("Muted " + name + "."); }
        }));
    }

    public void unmute(UUID uuid, String name, String moderator, java.util.function.Consumer<String> callback) {
        mutes.remove(uuid);
        database.unmute(uuid, name, moderator).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> callback.accept(error == null ? "Unmuted " + name + "." : "Unmute failed: " + rootMessage(error))));
    }

    public void ban(UUID uuid, String name, String moderator, String reason, long duration, java.util.function.Consumer<String> callback) {
        long expires = duration == 0 ? 0 : System.currentTimeMillis() + duration;
        bans.put(uuid, expires);
        database.saveBan(uuid, name, moderator, reason, expires).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) callback.accept("Ban could not be saved: " + rootMessage(error));
            else {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null) online.kickPlayer(ChatColor.RED + "You have been banned. Reason: " + reason);
                callback.accept("Banned " + name + ".");
            }
        }));
    }

    public void unban(UUID uuid, String name, String moderator, java.util.function.Consumer<String> callback) {
        bans.remove(uuid);
        database.unban(uuid, name, moderator).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> callback.accept(error == null ? "Unbanned " + name + "." : "Unban failed: " + rootMessage(error))));
    }

    public void warn(UUID uuid, String name, String moderator, String reason, java.util.function.Consumer<String> callback) {
        database.log(uuid, name, moderator, "WARN", reason).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) callback.accept("Warning could not be saved: " + rootMessage(error));
            else { Player online = Bukkit.getPlayer(uuid); if (online != null) online.sendMessage(ChatColor.YELLOW + "You have received a warning: " + reason); callback.accept("Warned " + name + "."); }
        }));
    }

    public void history(UUID uuid, java.util.function.Consumer<List<ModerationDatabase.History>> callback) {
        database.history(uuid, 10).whenComplete((entries, error) -> Bukkit.getScheduler().runTask(plugin, () -> callback.accept(error == null ? entries : List.of())));
    }

    public Map<String, String> getTerms() {
        return Collections.unmodifiableMap(new TreeMap<>(terms));
    }

    public void resetViolations(UUID uuid, java.util.function.Consumer<String> callback) {
        violations.remove(uuid);
        database.resetViolations(uuid).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> callback.accept(error == null ? "Violation count reset." : "Could not reset violations: " + rootMessage(error))));
    }

    private void notifyModerators(String message) {
        if (!notifyModerators) return;
        for (Player online : Bukkit.getOnlinePlayers()) if (online.hasPermission("ssmp.moderation.notify")) online.sendMessage(message);
    }

    private void logDatabaseFailure(String operation, Throwable throwable) {
        plugin.getLogger().severe("Could not " + operation + ": " + rootMessage(throwable));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    public static long parseDuration(String input, long fallback) {
        if (input == null || input.isBlank()) return fallback;
        String value = input.toLowerCase(Locale.ROOT).trim();
        if (value.equals("permanent") || value.equals("perm")) return 0;
        try {
            long amount = Long.parseLong(value.substring(0, value.length() - 1));
            if (amount < 1) return fallback;
            long multiplier = switch (value.charAt(value.length() - 1)) {
                case 's' -> 1_000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                case 'w' -> 604_800_000L;
                default -> -1L;
            };
            if (multiplier < 0 || amount > Long.MAX_VALUE / multiplier) return fallback;
            return amount * multiplier;
        } catch (RuntimeException exception) { return fallback; }
    }

    public static String formatDuration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        long days = seconds / 86400; long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60; long remaining = seconds % 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + remaining + "s";
        return remaining + "s";
    }

    public void setFilter(ModerationFilter filter) {
        this.filter = filter;
    }

    public ModerationFilter getFilter() {
        return filter;
    }
    public void close() { database.close(); }
}
