package org.siddharth.ssmp.updateChecker;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class updateChecker implements Listener {

    private final JavaPlugin plugin;
    private final File configFile;
    private final String issueTracker = "https://github.com/SiddharthC0/siddys-simple-plugin/issues";

    private List<String> notification = new ArrayList<>();

    public updateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "ssmp-update.txt");
    }

    public void check() {
        if (!isUpdateCheckEnabled()) {
            plugin.getLogger().info("Update checking is disabled.");
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        String currentVersion = plugin.getDescription().getVersion();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient httpclient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/repos/SiddharthC0/siddys-simple-plugin/releases?per_page=1"))
                        .header("User-Agent", "SiddharthC0/siddys-simple-plugin")
                        .header("Accept", "application/vnd.github+json")
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> resp = httpclient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (resp.statusCode() != 200) {
                    plugin.getLogger().warning("Could not check for updates. HTTP Status: " + resp.statusCode());
                    return;
                }

                JsonArray releases = JsonParser.parseString(resp.body()).getAsJsonArray();

                if (releases.isEmpty()) {
                    plugin.getLogger().warning("No GitHub releases found.");
                    return;
                }

                JsonObject latest = releases.get(0).getAsJsonObject();

                String latestVersion = latest.get("tag_name").getAsString();
                String releaseUrl = latest.get("html_url").getAsString();

                int comparison = compareVersions(currentVersion, latestVersion);

                if (comparison < 0) {
                    notification = List.of(
                            ChatColor.GOLD + "[SSMP] " + ChatColor.YELLOW + "A new SSMP version is available!",
                            ChatColor.GRAY + "Current: " + currentVersion + " | Latest: " + latestVersion,
                            ChatColor.GREEN + "Download: " + releaseUrl
                    );

                    plugin.getLogger().info("A new SSMP version is available: " + latestVersion);

                } else if (comparison > 0) {
                    notification = List.of(
                            ChatColor.GOLD + "[SSMP] " + ChatColor.LIGHT_PURPLE + "You're running a development build!",
                            ChatColor.GRAY + "Installed: " + currentVersion + " | Latest release: " + latestVersion,
                            ChatColor.YELLOW + "Report bugs: " + issueTracker
                    );

                    plugin.getLogger().info("SSMP is running a newer development version than the latest GitHub release.");

                } else {
                    plugin.getLogger().info("SSMP is up to date!");
                    return;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    notification.forEach(Bukkit::broadcastMessage);
                });

            } catch (Exception e) {
                plugin.getLogger().warning("Update check failed: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        notification.forEach(event.getPlayer()::sendMessage);
    }

    private boolean isUpdateCheckEnabled() {
        try {
            if (!configFile.exists()) {
                Files.writeString(configFile.toPath(), "checkupdate=true\n");
            }

            for (String line : Files.readAllLines(configFile.toPath())) {
                line = line.trim();

                if (line.startsWith("checkupdate=")) {
                    return Boolean.parseBoolean(line.substring("checkupdate=".length()).trim());
                }
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Could not read update configuration: " + e.getMessage());
        }

        return true;
    }

    private int compareVersions(String current, String latest) {
        current = current.trim().replaceFirst("(?i)^.*?v(?=\\d)", "");
        latest = latest.trim().replaceFirst("(?i)^.*?v(?=\\d)", "");

        current = current.split("[-+]")[0];
        latest = latest.split("[-+]")[0];

        String[] currentParts = current.split("\\.");
        String[] latestParts = latest.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (currentPart != latestPart) {
                return Integer.compare(currentPart, latestPart);
            }
        }

        return 0;
    }
}
