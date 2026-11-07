/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ssmpoptimization {

    private final JavaPlugin plugin;

    private final Map<String, Boolean> moduleCache = new HashMap<>();
    private final List<Runnable> shutdownActions = new ArrayList<>();

    private boolean initialized;
    private boolean shutdown;

    public ssmpoptimization(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        refresh();
        initialized = true;

        plugin.getLogger().info("SSMP optimization manager initialized.");
    }

    public void refresh() {
        if (shutdown) {
            return;
        }

        moduleCache.clear();

        FileConfiguration config = plugin.getConfig();

        ConfigurationSection section =
                config.getConfigurationSection("modules");

        if (section == null) {
            return;
        }

        for (String module : section.getKeys(false)) {
            moduleCache.put(
                    module,
                    config.getBoolean(
                            "modules." + module + ".enabled",
                            true
                    )
            );
        }
    }

    public boolean isModuleEnabled(String module) {
        return moduleCache.getOrDefault(
                module,
                plugin.getConfig().getBoolean(
                        "modules." + module + ".enabled",
                        true
                )
        );
    }

    public void registerShutdownAction(Runnable action) {
        if (shutdown) {
            throw new IllegalStateException(
                    "SSMP optimization manager is already shut down."
            );
        }

        if (action != null) {
            shutdownActions.add(action);
        }
    }

    public BukkitTask runTask(Runnable task) {
        ensureActive();

        return plugin.getServer()
                .getScheduler()
                .runTask(plugin, task);
    }

    public BukkitTask runTaskLater(Runnable task, long delay) {
        ensureActive();

        return plugin.getServer()
                .getScheduler()
                .runTaskLater(plugin, task, delay);
    }

    public BukkitTask runTaskTimer(
            Runnable task,
            long delay,
            long period
    ) {
        ensureActive();

        return plugin.getServer()
                .getScheduler()
                .runTaskTimer(plugin, task, delay, period);
    }

    private void ensureActive() {
        if (shutdown || !plugin.isEnabled()) {
            throw new IllegalStateException(
                    "SSMP optimization manager is not active."
            );
        }
    }

    public void shutdown() {
        if (shutdown) {
            return;
        }

        shutdown = true;

        for (int i = shutdownActions.size() - 1; i >= 0; i--) {
            try {
                shutdownActions.get(i).run();
            } catch (RuntimeException e) {
                plugin.getLogger().severe(
                        "Failed to shut down an SSMP module: "
                                + e.getMessage()
                );
            }
        }

        shutdownActions.clear();

        plugin.getServer()
                .getScheduler()
                .cancelTasks(plugin);

        moduleCache.clear();

        plugin.getLogger().info(
                "SSMP optimization manager shut down."
        );
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
