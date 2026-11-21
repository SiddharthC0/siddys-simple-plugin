package org.siddharth.ssmp.controlpanel;
import org.bukkit.plugin.java.JavaPlugin;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class controlPanel {
    private final JavaPlugin plugin;
    private final Path panelDirectory;
    private final controlPanelManifest manifest;
    private HttpServer server;
    private ExecutorService executor;
    private controlPanelAPI api;
    private Runnable updateCheckAction = () -> {};

    public controlPanel(JavaPlugin plugin) {
        this.plugin = plugin;
        this.panelDirectory = plugin.getDataFolder().toPath().resolve("cpanel");
        this.manifest = new controlPanelManifest(panelDirectory.resolve("panel.ssmpManifest"));
    }

    public void setUpdateCheckAction(Runnable action) {
        if (action == null) {
            this.updateCheckAction = () -> {};
        } else {
            this.updateCheckAction = action;
        }
        if (api != null) {
            api.setUpdateCheckAction(this.updateCheckAction);
        }
    }

    public void start() {
        if (server != null) {
            return;
        }
        try {
            Files.createDirectories(panelDirectory);
            manifest.loadOrCreate(plugin);
            if (!manifest.isEnabled()) {
                plugin.getLogger().info("[SSMP Control Panel] Disabled in panel.ssmpManifest.");
                return;
            }
            String host = manifest.get("host", "0.0.0.0");
            int port = manifest.getInt("port", 8765);
            if (manifest.getPasswordHash().isBlank()) {
                throw new IllegalStateException("A panel password is mandatory. Set password.hash in panel.ssmpManifest.");
            }

            api = new controlPanelAPI(plugin, manifest, updateCheckAction);
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            server.createContext("/", new controlPanelHandler(plugin, api));
            executor = Executors.newFixedThreadPool(8, runnable -> {
                Thread thread = new Thread(runnable, "SSMP-ControlPanel");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.start();

            plugin.getLogger().info("[SSMP Control Panel] Started at http://" + host + ":" + port);
            if (!host.equals("127.0.0.1") && !host.equalsIgnoreCase("localhost")) {
                plugin.getLogger().warning("[SSMP Control Panel] Non-local binding selected. " + "Use HTTPS through a trusted reverse proxy or secure tunnel before exposing it publicly.");
            }
        } catch (Exception exception) {
            stop();
            plugin.getLogger().log(Level.SEVERE, "[SSMP Control Panel] The panel failed to start due to an exception: ", exception);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            server = null;
        }

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        if (api != null) {
            api.close();
            api = null;
        }
    }

    public static String createInitialPassword() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public boolean isRunning() {
        return server != null;
    }
}
