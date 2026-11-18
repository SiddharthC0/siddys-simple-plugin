/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */

package org.siddharth.ssmp.controlpanel;

import com.sun.management.OperatingSystemMXBean;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

final class controlPanelAPI {
    private final JavaPlugin plugin;
    private final controlPanelManifest manifest;
    private final Map<String, Long> sessions = Collections.synchronizedMap(new HashMap<>());
    private static final long SESSION_LIFETIME = TimeUnit.MINUTES.toMillis(30);
    private static final int MAX_CONSOLE_HISTORY = 200;
    private static final int MAX_CONSOLE_SUBSCRIBER_QUEUE = 256;

    private volatile Runnable updateCheckAction;
    private volatile boolean closed;

    private final ArrayDeque<String> consoleHistory = new ArrayDeque<>();
    private final List<BlockingQueue<String>> consoleSubscribers = new CopyOnWriteArrayList<>();
    private final DateTimeFormatter consoleTime = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final long startedAt = System.currentTimeMillis();

    private final long[] tickSamples = new long[100];
    private int tickSampleIndex;
    private int tickSampleCount;
    private long tickDurationTotal;
    private long lastTickNanos;

    private volatile double currentTps = 20.0;
    private volatile double currentMspt = 0.0;

    private final BukkitTask monitorTask;
    private final AbstractAppender log4jAppender;

    controlPanelAPI(JavaPlugin plugin, controlPanelManifest manifest, Runnable updateCheckAction) {
        this.plugin = plugin;
        this.manifest = manifest;
        this.updateCheckAction = updateCheckAction;

        log4jAppender = new AbstractAppender("SSMPConsoleAppender", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (closed) return;
                String message = event.getMessage().getFormattedMessage();
                if (message == null || message.isBlank()) return;
                String line = "[" + LocalTime.now().format(consoleTime) + " " + event.getLevel().name() + "]: " + message;
                publishConsoleLine(line);
            }
        };

        log4jAppender.start();

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.getConfiguration().getRootLogger().addAppender(log4jAppender, Level.INFO, null);
        ctx.updateLoggers();

        monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::recordTick, 1L, 1L);
    }

    private void recordTick() {
        long now = System.nanoTime();
        if (lastTickNanos != 0) {
            long elapsed = now - lastTickNanos;
            if (tickSampleCount == tickSamples.length) {
                tickDurationTotal -= tickSamples[tickSampleIndex];
            } else {
                tickSampleCount++;
            }
            tickSamples[tickSampleIndex] = elapsed;
            tickDurationTotal += elapsed;
            tickSampleIndex = (tickSampleIndex + 1) % tickSamples.length;
            double avgNanos = (double) tickDurationTotal / tickSampleCount;
            currentMspt = avgNanos / 1_000_000.0;
            currentTps = Math.min(20.0, 1_000_000_000.0 / avgNanos);
        }
        lastTickNanos = now;
    }

    private void publishConsoleLine(String line) {
        if (closed || line == null) return;
        synchronized (consoleHistory) {
            if (consoleHistory.size() >= MAX_CONSOLE_HISTORY) consoleHistory.removeFirst();
            consoleHistory.addLast(line);
            for (BlockingQueue<String> subscriber : consoleSubscribers) {
                if (!subscriber.offer(line)) {
                    subscriber.poll();
                    subscriber.offer(line);
                }
            }
        }
    }

    void setUpdateCheckAction(Runnable action) {
        this.updateCheckAction = action;
    }

    boolean login(String password) {
        return manifest.verifyPassword(password);
    }

    String newSession() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, System.currentTimeMillis() + SESSION_LIFETIME);
        return token;
    }

    boolean validSession(String token) {
        if (token == null) return false;
        Long expires = sessions.get(token);
        if (expires == null) return false;
        if (expires < System.currentTimeMillis()) {
            sessions.remove(token);
            return false;
        }
        return true;
    }

    void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    String handle(String method, String path, Map<String, String> params) throws Exception {
        boolean postOnly = switch (path) {
            case "/api/save", "/api/add", "/api/password", "/api/update-check", "/api/console/command" -> true;
            default -> false;
        };

        boolean getOnly = switch (path) {
            case "/api/session", "/api/files", "/api/file", "/api/resources", "/api/monitor",
                 "/api/server", "/api/theme", "/api/console/status", "/api/console/poll" -> true;
            default -> false;
        };

        if (!postOnly && !getOnly) throw new IllegalArgumentException("Unknown API endpoint.");
        if (postOnly && !method.equals("POST")) throw new IllegalArgumentException("This endpoint requires POST.");
        if (getOnly && !method.equals("GET")) throw new IllegalArgumentException("This endpoint requires GET.");

        return switch (path) {
            case "/api/session" -> "{\"authenticated\":true}";
            case "/api/files" -> filesJson();
            case "/api/file" -> fileJson(required(params, "path"));
            case "/api/save" -> saveValue(params);
            case "/api/add" -> addValue(params);
            case "/api/password" -> changePassword(params);
            case "/api/update-check" -> runUpdateCheck();
            case "/api/console/status" -> consoleStatusJson();
            case "/api/console/command" -> consoleCommand(params);
            case "/api/console/poll" -> consolePollJson(params);
            case "/api/theme" -> "{\"theme\":\"" + escape(manifest.get("theme", "dark")) + "\"}";
            case "/api/server" -> serverJson();
            case "/api/monitor", "/api/resources" -> resourcesJson();
            default -> throw new IllegalArgumentException("Unknown API endpoint.");
        };
    }

    private String consoleStatusJson() {
        return "{\"enabled\":" + plugin.getConfig().getBoolean("cpanel.console.enabled", false) + "}";
    }

    private String consolePollJson(Map<String, String> params) {
        int since = 0;
        try { since = Integer.parseInt(params.getOrDefault("since", "0")); } catch (NumberFormatException ignored) {}
        synchronized (consoleHistory) {
            String[] lines = consoleHistory.toArray(new String[0]);
            int from = Math.max(0, Math.min(since, lines.length));
            StringBuilder json = new StringBuilder("{\"lines\":[");
            for (int i = from; i < lines.length; i++) {
                if (i > from) json.append(',');
                json.append('"').append(escape(lines[i])).append('"');
            }
            return json.append("],\"next\":").append(lines.length).append("}").toString();
        }
    }

    private String consoleCommand(Map<String, String> params) {
        if (!plugin.getConfig().getBoolean("cpanel.console.enabled", false)) throw new SecurityException("Console access is disabled.");
        String command = required(params, "command").trim();
        if (command.startsWith("/")) command = command.substring(1);
        if (command.isBlank() || command.length() > 512 || command.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Invalid command.");
        String finalCommand = command;
        publishConsoleLine("> " + finalCommand);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        return "{\"queued\":true}";
    }

    void streamConsole(HttpExchange exchange) throws IOException {
        if (!plugin.getConfig().getBoolean("cpanel.console.enabled", false)) throw new SecurityException("Console access is disabled.");
        if (closed) throw new IllegalStateException("Console is shutting down.");

        BlockingQueue<String> subscriber = new LinkedBlockingQueue<>(MAX_CONSOLE_SUBSCRIBER_QUEUE);
        List<String> history;
        synchronized (consoleHistory) {
            history = new ArrayList<>(consoleHistory);
            consoleSubscribers.add(subscriber);
        }

        try {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-transform");
            exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream output = exchange.getResponseBody()) {
                for (String line : history) writeConsoleEvent(output, line);
                output.flush();
                while (plugin.isEnabled() && !closed) {
                    try {
                        String line = subscriber.poll(15, TimeUnit.SECONDS);
                        if (line == null) {
                            output.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                        } else if (line.isEmpty()) {
                            break;
                        } else {
                            writeConsoleEvent(output, line);
                        }
                        output.flush();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            consoleSubscribers.remove(subscriber);
        }
    }

    private void writeConsoleEvent(OutputStream output, String line) throws IOException {
        output.write(("data: {\"line\":\"" + escape(line) + "\"}\n\n").getBytes(StandardCharsets.UTF_8));
    }

    void close() {
        if (closed) return;
        closed = true;
        monitorTask.cancel();

        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            ctx.getConfiguration().getRootLogger().removeAppender("SSMPConsoleAppender");
            ctx.updateLoggers();
            log4jAppender.stop();
        } catch (Exception ignored) {}

        for (BlockingQueue<String> subscriber : consoleSubscribers) {
            subscriber.clear();
            subscriber.offer("");
        }
        consoleSubscribers.clear();
        sessions.clear();
    }

    private String resourcesJson() {
        Runtime runtime = Runtime.getRuntime();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long heapMax = runtime.maxMemory();
        double heapPercent = heapMax > 0 ? heapUsed * 100.0 / heapMax : 0;
        double cpuPercent = -1;
        long systemRamUsed = -1;
        long systemRamMax = -1;
        double systemRamPercent = -1;

        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof OperatingSystemMXBean os) {
            double load = os.getProcessCpuLoad();
            if (load >= 0) cpuPercent = load * 100.0;
            systemRamMax = os.getTotalMemorySize();
            long systemRamFree = os.getFreeMemorySize();
            if (systemRamMax > 0 && systemRamFree >= 0) {
                systemRamUsed = systemRamMax - systemRamFree;
                systemRamPercent = systemRamUsed * 100.0 / systemRamMax;
            }
        }

        return "{\"cpuPercent\":" + decimal(cpuPercent)
                + ",\"ramUsed\":" + heapUsed
                + ",\"ramMax\":" + heapMax
                + ",\"ramPercent\":" + decimal(heapPercent)
                + ",\"systemRamUsed\":" + systemRamUsed
                + ",\"systemRamMax\":" + systemRamMax
                + ",\"systemRamPercent\":" + decimal(systemRamPercent)
                + ",\"tps\":" + decimal(currentTps)
                + ",\"mspt\":" + decimal(currentMspt)
                + ",\"averagePercent\":" + decimal(cpuPercent >= 0 ? (cpuPercent + heapPercent) / 2 : heapPercent)
                + "}";
    }

    private String decimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String filesJson() throws Exception {
        return onMain(() -> {
            File base = plugin.getDataFolder();
            List<String> paths = new ArrayList<>();
            collectYaml(base, base, paths);
            StringBuilder json = new StringBuilder("{\"files\":[");
            for (int i = 0; i < paths.size(); i++) {
                if (i > 0) json.append(',');
                json.append('"').append(escape(paths.get(i))).append('"');
            }
            return json.append("]}").toString();
        });
    }

    private void collectYaml(File base, File current, List<String> paths) {
        File[] children = current.listFiles();
        if (children == null) return;
        Arrays.sort(children, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File child : children) {
            if (child.isDirectory()) {
                collectYaml(base, child, paths);
            } else if (child.isFile() && (child.getName().endsWith(".yml") || child.getName().endsWith(".yaml"))) {
                paths.add(base.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/'));
            }
        }
    }

    private String fileJson(String relative) throws Exception {
        return onMain(() -> {
            File file = resolve(relative);
            if (!file.isFile()) throw new IllegalArgumentException("Configuration file not found.");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            List<Map<String, Object>> entries = new ArrayList<>();
            flatten("", yaml, entries);
            StringBuilder json = new StringBuilder("{\"path\":\"").append(escape(relative)).append("\",\"entries\":[");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) json.append(',');
                Map<String, Object> entry = entries.get(i);
                json.append("{\"key\":\"").append(escape((String) entry.get("key")))
                        .append("\",\"value\":\"").append(escape(String.valueOf(entry.get("value"))))
                        .append("\",\"type\":\"").append(escape((String) entry.get("type")))
                        .append("\",\"section\":").append(entry.get("section")).append('}');
            }
            return json.append("]}").toString();
        });
    }

    private void flatten(String prefix, ConfigurationSection section, List<Map<String, Object>> entries) {
        for (String key : section.getKeys(false)) {
            String full = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                Map<String, Object> heading = new LinkedHashMap<>();
                heading.put("key", full);
                heading.put("value", "");
                heading.put("type", "section");
                heading.put("section", true);
                entries.add(heading);
                flatten(full, child, entries);
            } else {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("key", full);
                entry.put("value", value instanceof List<?> list ? String.join("\n", list.stream().map(String::valueOf).toList()) : String.valueOf(value));
                entry.put("type", value == null ? "string" : value instanceof Boolean ? "boolean" : value instanceof Number ? "number" : value instanceof List<?> ? "list" : "string");
                entry.put("section", false);
                entries.add(entry);
            }
        }
    }

    private String saveValue(Map<String, String> params) throws Exception {
        String relative = required(params, "path");
        String key = required(params, "key");
        String value = params.getOrDefault("value", "");
        String type = params.getOrDefault("type", "string");
        return onMain(() -> {
            File file = resolve(relative);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            yaml.set(key, convert(value, type));
            yaml.save(file);
            return "{\"saved\":true}";
        });
    }

    private String addValue(Map<String, String> params) throws Exception {
        String relative = required(params, "path");
        String key = required(params, "key");
        String value = params.getOrDefault("value", "");
        String type = params.getOrDefault("type", "string");
        return onMain(() -> {
            File file = resolve(relative);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (yaml.contains(key)) throw new IllegalArgumentException("That YAML key already exists.");
            yaml.set(key, convert(value, type));
            yaml.save(file);
            return "{\"saved\":true}";
        });
    }

    private Object convert(String value, String type) {
        return switch (type) {
            case "boolean" -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) throw new IllegalArgumentException("Enter true or false.");
                yield Boolean.parseBoolean(value);
            }
            case "number" -> {
                try {
                    if (value.contains(".") || value.contains("e") || value.contains("E")) yield Double.parseDouble(value);
                    yield Long.parseLong(value);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Enter a valid number.");
                }
            }
            case "list" -> Arrays.stream(value.split("\\R", -1)).filter(line -> !line.isEmpty()).toList();
            case "string" -> value;
            default -> throw new IllegalArgumentException("Invalid YAML value type.");
        };
    }

    private String changePassword(Map<String, String> params) {
        if (!manifest.verifyPassword(required(params, "current"))) throw new SecurityException("Current password is incorrect.");
        try {
            manifest.changePassword(required(params, "next"));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        sessions.clear();
        return "{\"changed\":true}";
    }

    private String runUpdateCheck() {
        Runnable action = updateCheckAction;
        if (action == null) throw new IllegalStateException("Update checker is not configured.");
        Bukkit.getScheduler().runTask(plugin, action);
        return "{\"started\":true}";
    }

    private String serverJson() throws Exception {
        return onMain(() -> {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            World primaryWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            File playerDataFolder = primaryWorld == null
                    ? new File(Bukkit.getWorldContainer(), "playerdata")
                    : new File(primaryWorld.getWorldFolder(), "playerdata");
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".dat"));
            Set<String> playerIds = new HashSet<>();
            if (playerFiles != null) {
                for (File f : playerFiles) playerIds.add(f.getName().substring(0, f.getName().length() - 4));
            }
            StringBuilder json = new StringBuilder("{");
            json.append("\"serverName\":\"").append(escape(Bukkit.getName())).append("\",");
            json.append("\"version\":\"").append(escape(Bukkit.getVersion())).append("\",");
            json.append("\"bukkitVersion\":\"").append(escape(Bukkit.getBukkitVersion())).append("\",");
            json.append("\"javaVersion\":\"").append(escape(System.getProperty("java.version", "Unknown"))).append("\",");
            json.append("\"motd\":\"").append(escape(Bukkit.getMotd())).append("\",");
            json.append("\"onlinePlayers\":").append(onlinePlayers.size()).append(',');
            json.append("\"maxPlayers\":").append(Bukkit.getMaxPlayers()).append(',');
            json.append("\"uptimeMillis\":").append(System.currentTimeMillis() - startedAt).append(',');
            json.append("\"players\":[");
            int index = 0;
            for (Player player : onlinePlayers) {
                playerIds.add(player.getUniqueId().toString());
                if (index++ > 0) json.append(',');
                json.append("{\"name\":\"").append(escape(player.getName()))
                        .append("\",\"uuid\":\"").append(player.getUniqueId())
                        .append("\",\"world\":\"").append(escape(player.getWorld().getName()))
                        .append("\",\"gamemode\":\"").append(player.getGameMode().name()).append("\"}");
            }
            json.append("],\"totalPlayers\":").append(playerIds.size()).append(",\"worlds\":[");
            List<World> worlds = Bukkit.getWorlds();
            for (int i = 0; i < worlds.size(); i++) {
                if (i > 0) json.append(',');
                json.append('"').append(escape(worlds.get(i).getName())).append('"');
            }
            return json.append("]}").toString();
        });
    }

    private File resolve(String relative) throws IOException {
        if (relative == null || relative.isBlank() || relative.contains("..") || relative.startsWith("/") || relative.contains("\\")) throw new SecurityException("Invalid configuration path.");
        File base = plugin.getDataFolder().getCanonicalFile();
        File target = new File(base, relative).getCanonicalFile();
        if (!target.toPath().startsWith(base.toPath())) throw new SecurityException("Invalid configuration path.");
        String name = target.getName().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".yml") && !name.endsWith(".yaml")) throw new SecurityException("Only YAML files can be edited.");
        if (!target.exists() || !target.isFile()) throw new IllegalArgumentException("Configuration file not found.");
        return target;
    }

    private <T> T onMain(Callable<T> callable) throws Exception {
        if (Bukkit.isPrimaryThread()) return callable.call();
        return Bukkit.getScheduler().callSyncMethod(plugin, callable).get(10, TimeUnit.SECONDS);
    }

    private static String required(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key + ".");
        return value;
    }

    static Map<String, String> decodeForm(String body) {
        Map<String, String> result = new HashMap<>();
        if (body == null || body.isEmpty()) return result;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }

    static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replaceAll("[\\x00-\\x1F]", "");
    }
}