package org.siddharth.ssmp.mechanics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfoBarSystem implements Listener {

    private final JavaPlugin plugin;
    private PlayerInformationSystem playerInformationSystem;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, String> lastTitles = new HashMap<>();
    private final Set<UUID> suspended = new HashSet<>();

    private File infoBarFolder;
    private File configFile;
    private File dataFile;
    private YamlConfiguration config;
    private YamlConfiguration data;
    private BukkitTask task;

    private boolean available;
    private String separator;
    private String emptyTileAction;
    private double progress;
    private BarColor barColor;
    private BarStyle barStyle;
    private DateTimeFormatter timeFormatter;

    public InfoBarSystem(JavaPlugin plugin) {
        this.plugin = plugin;

        try {
            plugin.saveDefaultConfig();
            loadFiles();
            reload();

            Bukkit.getPluginManager().registerEvents(this, plugin);
            available = true;
            if (config.getBoolean("infobar.enabled", true)) {
                start();
            }

            plugin.getLogger().info("InfoBar initialized successfully.");

        } catch (Exception e) {
            available = false;
            shutdown();
            plugin.getLogger().severe("InfoBar failed to initialize: " + e.getMessage());
        }
    }

    public void setPlayerInformationSystem(PlayerInformationSystem system) {
        this.playerInformationSystem = system;

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }


    private void loadFiles() {
        infoBarFolder = new File(plugin.getDataFolder(), "infobar");

        if (!infoBarFolder.exists() && !infoBarFolder.mkdirs()) {
            throw new IllegalStateException("Could not create InfoBar directory.");
        }

        configFile = new File(infoBarFolder, "config.yml");
        dataFile = new File(infoBarFolder, "data.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException("Could not create InfoBar config.yml.");
                }

                YamlConfiguration defaults = new YamlConfiguration();

                defaults.set("infobar.enabled", true);
                defaults.set("infobar.separator", "  &8┃  ");
                defaults.set("infobar.bossbar.color", "PURPLE");
                defaults.set("infobar.bossbar.style", "SOLID");
                defaults.set("infobar.bossbar.progress", 0.0);
                defaults.set("infobar.time.format", "HH:mm:ss");
                defaults.set("infobar.tiles.tile1.enabled", true);
                defaults.set("infobar.tiles.tile1.content", "&f☀ &7Weather: &f%weather%");
                defaults.set("infobar.tiles.tile2.enabled", true);
                defaults.set("infobar.tiles.tile2.content", "&d◆ &7XYZ: &f%x%, %y%, %z%");
                defaults.set("infobar.tiles.tile3.enabled", true);
                defaults.set("infobar.tiles.tile3.content", "&b◷ &7Time: &f%time%");
                defaults.set("infobar.update-interval", 20);
                defaults.set("infobar.empty-tile-action", "hide");

                defaults.save(configFile);

            } catch (IOException e) {
                throw new IllegalStateException("Could not initialize InfoBar configuration.", e);
            }
        }

        if (!dataFile.exists()) {
            File oldDataFile = new File(plugin.getDataFolder(), "infobar-data.yml");

            if (oldDataFile.exists()) {
                YamlConfiguration oldData = YamlConfiguration.loadConfiguration(oldDataFile);

                try {
                    oldData.save(dataFile);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not migrate InfoBar data.", e);
                }
            } else {
                try {
                    if (!dataFile.createNewFile()) {
                        throw new IOException("Could not create InfoBar data.yml.");
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Could not create InfoBar data.", e);
                }
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        data = YamlConfiguration.loadConfiguration(dataFile);
    }


    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        separator = config.getString("infobar.separator", "  &8┃  "); // don't ask me how i got that bold
        emptyTileAction = config.getString("infobar.empty-tile-action", "hide");
        progress = config.getDouble("infobar.bossbar.progress", 0.0);
        progress = Math.max(0.0, Math.min(1.0, progress));
        barColor = parseEnum(BarColor.class, config.getString("infobar.bossbar.color", "PURPLE"), BarColor.PURPLE);
        barStyle = parseEnum(BarStyle.class, config.getString("infobar.bossbar.style", "SOLID"), BarStyle.SOLID);
        String format = config.getString("infobar.time.format", "HH:mm:ss");

        try {
            timeFormatter = DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException e) {
            timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            plugin.getLogger().warning("Invalid InfoBar time format. Using HH:mm:ss.");
        }
        stopTask();

        if (config.getBoolean("infobar.enabled", true)) {start();}
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        if (value == null) {return fallback;}
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid InfoBar setting: " + value);
            return fallback;
        }
    }

    public void start() {
        if (task != null) {return;}
        long interval = Math.max(1, config.getLong("infobar.update-interval", 20));

        task = Bukkit.getScheduler().runTaskTimer(
                plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        try {
                            updatePlayer(player);
                        } catch (Exception e) {
                            plugin.getLogger().warning(
                                    "InfoBar update failed for " + player.getName() + ": " + e.getMessage()
                            );
                        }
                    }
                },
                0L,
                interval
        );
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void updatePlayer(Player player) {
        if (!available && task == null) {return;}
        UUID uuid = player.getUniqueId();

        if (!config.getBoolean("infobar.enabled", true) || !isPlayerEnabled(player) || suspended.contains(uuid)) {
            hideBar(player);
            return;
        }
        String title = buildTitle(player);
        if (title.isBlank()) {
            if (emptyTileAction.equalsIgnoreCase("hide")) {
                hideBar(player);
                return;
            }
        }

        BossBar bar = bars.computeIfAbsent(uuid, ignored -> Bukkit.createBossBar(title, barColor, barStyle));
        bar.setColor(barColor);
        bar.setStyle(barStyle);
        bar.setProgress(progress);
        if (!title.equals(lastTitles.get(uuid))) {
            bar.setTitle(title);
            lastTitles.put(uuid, title);
        }

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }

        bar.setVisible(true);
    }

    private String buildTitle(Player player) {
        ConfigurationSection tiles = config.getConfigurationSection("infobar.tiles");
        if (tiles == null) {return "";}
        StringBuilder result = new StringBuilder();
        for (String tileId : tiles.getKeys(false)) {
            ConfigurationSection tile = tiles.getConfigurationSection(tileId);
            if (tile == null || !tile.getBoolean("enabled", true)) {continue;}
            String content = tile.getString("content", "");
            if (content == null || content.isBlank()) {continue;}
            content = replacePlaceholders(content, player);
            content = ChatColor.translateAlternateColorCodes('&', content);
            if (result.length() > 0) {
                result.append(ChatColor.translateAlternateColorCodes('&', separator));
            }

            result.append(content);
        }

        return result.toString();
    }

    private String replacePlaceholders(String content, Player player) {
        Map<String, String> values = new HashMap<>();
        values.put("%player%", player.getName());
        values.put("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        values.put("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        values.put("%time%", LocalTime.now().format(timeFormatter));
        values.put("%world%", player.getWorld().getName());
        values.put("%world_time%", formatWorldTime(player.getWorld().getTime()));
        values.put("%weather%", getWeather(player));
        values.put("%biome%", player.getLocation().getBlock().getBiome().name());
        values.put("%x%", String.valueOf(player.getLocation().getBlockX()));
        values.put("%y%", String.valueOf(player.getLocation().getBlockY()));
        values.put("%z%", String.valueOf(player.getLocation().getBlockZ()));
        values.put("%health%", String.valueOf((int) Math.ceil(player.getHealth())));
        values.put("%food%", String.valueOf(player.getFoodLevel()));

        for (Map.Entry<String, String> entry : values.entrySet()) {
            content = Pattern.compile(Pattern.quote(entry.getKey()), Pattern.CASE_INSENSITIVE).matcher(content).replaceAll(Matcher.quoteReplacement(entry.getValue()));
        }
        return content;
    }

    private String formatWorldTime(long ticks) { // remove this logic if you want your plugin to be 'low quality' i.e. show different time
        long hours = (ticks / 1000 + 6) % 24;
        long minutes = (ticks % 1000) * 60 / 1000;

        return String.format("%02d:%02d", hours, minutes);
    }

    private String getWeather(Player player) {
        if (player.getWorld().isThundering()) return "Thunder";
        if (player.getWorld().hasStorm()) return "Rain";
        return "Clear";
    }

    public boolean isPlayerEnabled(Player player) {
        boolean legacyEnabled = data.getBoolean(
                "players." + player.getUniqueId() + ".enabled",
                true
        );

        boolean interfaceEnabled = playerInformationSystem == null
                || playerInformationSystem.isEnabled(player, "interface.infobar");

        return legacyEnabled && interfaceEnabled;
    }

    public void setPlayerEnabled(Player player, boolean enabled) {
        data.set("players." + player.getUniqueId() + ".enabled", enabled);
        saveData();

        if (enabled) {
            updatePlayer(player);
        } else {
            hideBar(player);
        }
    }

    public void togglePlayer(Player player) {
        setPlayerEnabled(player, !isPlayerEnabled(player));
    }

    private void saveData() {
        try {data.save(dataFile);} catch (IOException e) {
            plugin.getLogger().warning("Could not save InfoBar preferences: " + e.getMessage());
        }
    }

    private void hideBar(Player player) {
        BossBar bar = bars.get(player.getUniqueId());

        if (bar != null) {
            bar.removePlayer(player);
        }
    }

    public void suspend(Player player) {
        suspended.add(player.getUniqueId());
        hideBar(player);
    }

    public void resume(Player player) {
        suspended.remove(player.getUniqueId());
        updatePlayer(player);
    }

    public void suspendAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            suspend(player);
        }
    }

    public void resumeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            resume(player);
        }
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = bars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }

        lastTitles.remove(uuid);
        suspended.remove(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                updatePlayer(event.getPlayer());
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {removePlayer(event.getPlayer());}

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {updatePlayer(event.getPlayer());}
    public boolean isAvailable() {return available;}
    public void shutdown() {
        stopTask();
        for (BossBar bar : bars.values()) {
            bar.removeAll();
            bar.setVisible(false);
        }
        bars.clear();
        lastTitles.clear();
        suspended.clear();
    }
}
