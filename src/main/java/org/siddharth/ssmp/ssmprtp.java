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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ssmprtp implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final ssmpcore core;
    private final Random random = new Random();
    private FileConfiguration config;
    private final Map<UUID, String> selectedWorlds = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> searching = new HashSet<>();

    private enum MenuType {
        WORLD,
        DISTANCE
    }

    private static class RtpHolder implements InventoryHolder {
        private final MenuType type;
        private final Map<Integer, String> worldSlots = new HashMap<>();
        private final Map<Integer, Integer> distanceSlots = new HashMap<>();
        private Inventory inventory;

        private RtpHolder(MenuType type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public ssmprtp(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!(plugin instanceof ssmp ssmpPlugin)) {
            throw new IllegalArgumentException("SSMP plugin instance required.");
        }
        this.core = ssmpPlugin.getCore();
        if (core == null) {
            throw new IllegalStateException("SSMP core must be initialized before RTP.");
        }
        File folder = new File(plugin.getDataFolder(), "rtp");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Failed to create RTP configuration folder.");
        }
        loadConfig();
    }

    private void loadConfig() {
        config = core.createConfig("rtp/config");
        addDefaults();
        plugin.getLogger().info("[RTP DEBUG] Config file: "
                + new File(plugin.getDataFolder(), "rtp/config.yml").getAbsolutePath());
        plugin.getLogger().info("[RTP DEBUG] Overworld slot: "
                + config.getInt("gui.world.overworld.slot", -1));
    }

    private void addDefaults() {
        config.addDefault("settings.enabled", true);
        config.addDefault("settings.cooldown", 60);
        config.addDefault("settings.max-attempts", 50);
        config.addDefault("settings.min-y", -64);
        config.addDefault("settings.max-y", 320);

        config.addDefault("worlds.overworld.enabled", true);
        config.addDefault("worlds.overworld.world-name", "world");
        config.addDefault("worlds.overworld.min-distance", 0);
        config.addDefault("worlds.nether.enabled", true);
        config.addDefault("worlds.nether.world-name", "world_nether");
        config.addDefault("worlds.nether.min-distance", 0);
        config.addDefault("worlds.end.enabled", true);
        config.addDefault("worlds.end.world-name", "world_the_end");
        config.addDefault("worlds.end.min-distance", 0);

        config.addDefault("distances", List.of(1000, 5000, 10000, 20000));

        config.addDefault("safety.check-ground", true);
        config.addDefault("safety.check-headroom", true);
        config.addDefault("safety.avoid-water", true);
        config.addDefault("safety.avoid-lava", true);
        config.addDefault("safety.avoid-fire", true);
        config.addDefault("safety.avoid-cactus", true);
        config.addDefault("safety.avoid-magma", true);
        config.addDefault("safety.avoid-powder-snow", true);
        config.addDefault("safety.avoid-suffocation", true);
        config.addDefault("safety.avoid-unloaded-chunks", false);

        config.addDefault("gui.world.title", "&8SSMP | Select World");
        config.addDefault("gui.world.size", 27);
        config.addDefault("gui.world.filler.enabled", true);
        config.addDefault("gui.world.filler.material", "BLACK_STAINED_GLASS_PANE");
        config.addDefault("gui.world.filler.name", " ");
        config.addDefault("gui.world.overworld.slot", 11);
        config.addDefault("gui.world.overworld.material", "GRASS_BLOCK");
        config.addDefault("gui.world.overworld.name", "&aOverworld");
        config.addDefault("gui.world.overworld.lore", List.of("&7Explore the Overworld.", "", "&eClick to select."));
        config.addDefault("gui.world.nether.slot", 13);
        config.addDefault("gui.world.nether.material", "NETHERRACK");
        config.addDefault("gui.world.nether.name", "&cNether");
        config.addDefault("gui.world.nether.lore", List.of("&7Explore the Nether.", "", "&eClick to select."));
        config.addDefault("gui.world.end.slot", 15);
        config.addDefault("gui.world.end.material", "END_STONE");
        config.addDefault("gui.world.end.name", "&5The End");
        config.addDefault("gui.world.end.lore", List.of("&7Explore the End.", "", "&eClick to select."));

        config.addDefault("gui.distance.title", "&8SSMP | Select Distance");
        config.addDefault("gui.distance.size", 27);
        config.addDefault("gui.distance.slots", List.of(10, 12, 14, 16));
        config.addDefault("gui.distance.material", "COMPASS");
        config.addDefault("gui.distance.name", "&b%distance% Blocks");
        config.addDefault("gui.distance.lore", List.of("&7Random teleport within this distance.", "", "&eClick to teleport."));
        config.addDefault("gui.distance.filler.enabled", true);
        config.addDefault("gui.distance.filler.material", "BLACK_STAINED_GLASS_PANE");
        config.addDefault("gui.distance.filler.name", " ");

        config.addDefault("actionbar.enabled", true);
        config.addDefault("actionbar.searching", "&eSearching for a safe location...");
        config.addDefault("actionbar.success", "&aTeleport successful!");
        config.addDefault("actionbar.failed", "&cCould not find a safe location.");
        config.addDefault("actionbar.cooldown", "&cWait %time%s before using RTP again.");

        config.addDefault("sounds.enabled", true);
        config.addDefault("sounds.open.sound", "UI_BUTTON_CLICK");
        config.addDefault("sounds.open.volume", 1.0);
        config.addDefault("sounds.open.pitch", 1.0);
        config.addDefault("sounds.select.sound", "UI_BUTTON_CLICK");
        config.addDefault("sounds.select.volume", 1.0);
        config.addDefault("sounds.select.pitch", 1.2);
        config.addDefault("sounds.searching.sound", "BLOCK_NOTE_BLOCK_PLING");
        config.addDefault("sounds.searching.volume", 1.0);
        config.addDefault("sounds.searching.pitch", 1.0);
        config.addDefault("sounds.success.sound", "ENTITY_PLAYER_LEVELUP");
        config.addDefault("sounds.success.volume", 1.0);
        config.addDefault("sounds.success.pitch", 1.0);
        config.addDefault("sounds.failed.sound", "ENTITY_VILLAGER_NO");
        config.addDefault("sounds.failed.volume", 1.0);
        config.addDefault("sounds.failed.pitch", 1.0);

        config.addDefault("messages.prefix", "&8[&bSSMP&8] ");
        config.addDefault("messages.no-permission", "&cYou don't have permission to use RTP.");
        config.addDefault("messages.disabled", "&cRandom Teleport is currently disabled.");
        config.addDefault("messages.world-unavailable", "&cThat world is unavailable.");
        config.addDefault("messages.invalid-location", "&cUnable to find a safe location.");
        config.addDefault("messages.cooldown", "&cPlease wait %time% seconds.");
        config.addDefault("messages.success", "&aYou have been randomly teleported!");

        config.addDefaults(config);
        config.options().copyDefaults(true);
        core.saveConfig("rtp/config", config);
    }

    public void reload() {
        config = core.reloadConfig("rtp/config");
        addDefaults();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            core.sendMessage(sender, "&cOnly players can use this command.");
            return true;
        }
        if (!config.getBoolean("settings.enabled", true)) {
            message(player, "disabled");
            return true;
        }
        if (!player.hasPermission("ssmp.rtp.use")) {
            message(player, "no-permission");
            return true;
        }
        if (searching.contains(player.getUniqueId())) {
            send(player, "&cYou are already searching for a location.");
            return true;
        }
        openWorldGui(player);
        return true;
    }

    private void openWorldGui(Player player) {
        RtpHolder holder = new RtpHolder(MenuType.WORLD);

        Inventory inventory = core.createGui(
                holder,
                validGuiSize(config.getInt("gui.world.size", 27)),
                config.getString("gui.world.title", "&8SSMP | Select World")
        );

        holder.inventory = inventory;

        fillGui(inventory, "gui.world.filler");

        for (String worldKey : List.of("overworld", "nether", "end")) {
            String path = "gui.world." + worldKey;

            if (!config.getBoolean("worlds." + worldKey + ".enabled", true)) {
                continue;
            }

            Object rawSlot = config.get(path + ".slot");
            int slot = rawSlot instanceof Number number ? number.intValue() : -1;

            if (slot < 0 || slot >= inventory.getSize()) {
                plugin.getLogger().warning("[RTP] Invalid GUI slot for " + worldKey + ": " + slot);
                continue;
            }

            Material material = getMaterial(
                    config.getString(path + ".material"),
                    Material.BARRIER
            );

            String name = config.getString(path + ".name", "&f" + worldKey);
            List<String> lore = config.getStringList(path + ".lore");

            inventory.setItem(
                    slot,
                    core.createGuiItem(material, name, lore)
            );

            holder.worldSlots.put(slot, worldKey);

            plugin.getLogger().info("[RTP] Registered world button: " + worldKey + " at slot " + slot);
        }

        core.openGui(player, inventory);
        playSound(player, "open");
    }

    private void openDistanceGui(Player player) {
        RtpHolder holder = new RtpHolder(MenuType.DISTANCE);

        Inventory inventory = core.createGui(
                holder,
                validGuiSize(config.getInt("gui.distance.size", 27)),
                config.getString("gui.distance.title", "&8SSMP | Select Distance")
        );

        holder.inventory = inventory;

        List<Integer> distances = config.getIntegerList("distances");
        List<Integer> slots = config.getIntegerList("gui.distance.slots");

        Material material = getMaterial(
                config.getString("gui.distance.material", "COMPASS"),
                Material.COMPASS
        );

        List<String> configuredLore = config.getStringList("gui.distance.lore");

        for (int i = 0; i < Math.min(distances.size(), slots.size()); i++) {
            int distance = distances.get(i);
            int slot = slots.get(i);

            if (distance <= 0 || slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            String name = config.getString(
                    "gui.distance.name",
                    "&b%distance% Blocks"
            ).replace("%distance%", String.valueOf(distance));

            List<String> lore = configuredLore.stream()
                    .map(line -> line.replace("%distance%", String.valueOf(distance)))
                    .toList();

            inventory.setItem(
                    slot,
                    core.createGuiItem(material, name, lore)
            );

            holder.distanceSlots.put(slot, distance);
        }

        fillEmptyGui(inventory, "gui.distance.filler");

        core.openGui(player, inventory);
        playSound(player, "open");
    }

    private int validGuiSize(int size) {
        if (size < 9 || size > 54) {
            return 27;
        }
        return ((size + 8) / 9) * 9;
    }

    private void fillGui(Inventory inventory, String path) {
        if (!config.getBoolean(path + ".enabled", true)) {
            return;
        }
        Material material = getMaterial(config.getString(path + ".material", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
        core.fillGui(inventory, core.createGuiItem(material, config.getString(path + ".name", " ")));
    }

    private void fillEmptyGui(Inventory inventory, String path) {
        if (!config.getBoolean(path + ".enabled", true)) {
            return;
        }
        Material material = getMaterial(config.getString(path + ".material", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
        core.fillEmptyGuiSlots(inventory, core.createGuiItem(material, config.getString(path + ".name", " ")));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof RtpHolder holder)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= top.getSize()) {
            return;
        }

        plugin.getLogger().info(
                "[RTP] Click detected | Type: " + holder.type + " | Slot: " + slot
        );

        if (holder.type == MenuType.WORLD) {
            handleWorldSelection(player, holder, slot);
        } else {
            handleDistanceSelection(player, holder, slot);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RtpHolder) {
            event.setCancelled(true);
        }
    }

    private void handleWorldSelection(Player player, RtpHolder holder, int slot) {
        plugin.getLogger().info("[RTP DEBUG] Handling world selection: " + slot);

        String worldKey = holder.worldSlots.get(slot);

        if (worldKey == null) {
            plugin.getLogger().warning("[RTP] No world registered at slot " + slot);
            return;
        }

        if (!config.getBoolean("worlds." + worldKey + ".enabled", true)) {
            plugin.getLogger().warning("[RTP] World is disabled: " + worldKey);
            return;
        }

        String worldName = config.getString("worlds." + worldKey + ".world-name", worldKey);

        plugin.getLogger().info("[RTP DEBUG] Selected: " + worldKey);
        plugin.getLogger().info("[RTP DEBUG] Configured world name: " + worldName);
        plugin.getLogger().info("[RTP DEBUG] Bukkit world loaded: " + (Bukkit.getWorld(worldName) != null));

        selectedWorlds.put(player.getUniqueId(), worldKey);

        playSound(player, "select");

        plugin.getLogger().info("[RTP DEBUG] Opening distance GUI...");
        openDistanceGui(player);
        plugin.getLogger().info("[RTP DEBUG] Distance GUI opened.");
    }

    private void handleDistanceSelection(Player player, RtpHolder holder, int slot) {
        Integer distance = holder.distanceSlots.get(slot);

        if (distance == null) {
            plugin.getLogger().warning("[RTP] No distance registered at slot " + slot);
            return;
        }

        String worldKey = selectedWorlds.get(player.getUniqueId());

        if (worldKey == null) {
            player.closeInventory();
            openWorldGui(player);
            return;
        }

        player.closeInventory();
        playSound(player, "select");

        startSearch(player, worldKey, distance);
    }



    private void startSearch(Player player, String worldKey, int distance) {
        UUID uuid = player.getUniqueId();
        if (searching.contains(uuid)) {
            return;
        }
        long now = System.currentTimeMillis();
        long expiry = cooldowns.getOrDefault(uuid, 0L);
        if (expiry > now) {
            long remaining = (expiry - now + 999L) / 1000L;
            String text = config.getString("messages.cooldown", "&cPlease wait %time% seconds.").replace("%time%", String.valueOf(remaining));
            send(player, text);
            actionBar(player, config.getString("actionbar.cooldown", "&cWait %time%s before using RTP again.").replace("%time%", String.valueOf(remaining)));
            return;
        }

        String worldName = config.getString("worlds." + worldKey + ".world-name", worldKey);
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            message(player, "world-unavailable");
            return;
        }
        int minDistance = Math.max(0, config.getInt("worlds." + worldKey + ".min-distance", 0));
        if (minDistance > distance) {
            send(player, "&cThe configured minimum distance exceeds the selected distance.");
            return;
        }

        searching.add(uuid);
        actionBar(player, config.getString("actionbar.searching", "&eSearching for a safe location..."));
        playSound(player, "searching");
        int maxAttempts = Math.max(1, config.getInt("settings.max-attempts", 50));

        new BukkitRunnable() {
            private int attempts;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    searching.remove(uuid);
                    cancel();
                    return;
                }
                if (attempts >= maxAttempts) {
                    finishFailure(player, uuid);
                    cancel();
                    return;
                }
                attempts++;
                Location destination = findSafeLocation(world, minDistance, distance);
                if (destination == null) {
                    return;
                }
                boolean teleported = player.teleport(destination);
                if (teleported) {
                    int cooldown = Math.max(0, config.getInt("settings.cooldown", 60));
                    if (cooldown > 0) {
                        cooldowns.put(uuid, System.currentTimeMillis() + cooldown * 1000L);
                    }
                    actionBar(player, config.getString("actionbar.success", "&aTeleport successful!"));
                    message(player, "success");
                    playSound(player, "success");
                } else {
                    actionBar(player, config.getString("actionbar.failed", "&cCould not find a safe location."));
                    message(player, "invalid-location");
                    playSound(player, "failed");
                }
                searching.remove(uuid);
                cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void finishFailure(Player player, UUID uuid) {
        searching.remove(uuid);
        actionBar(player, config.getString("actionbar.failed", "&cCould not find a safe location."));
        message(player, "invalid-location");
        playSound(player, "failed");
    }

    private Location findSafeLocation(World world, int minDistance, int maxDistance) {
        Location spawn = world.getSpawnLocation();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(minDistance * (double) minDistance
                + random.nextDouble() * (maxDistance * (double) maxDistance - minDistance * (double) minDistance));
        int x = spawn.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
        int z = spawn.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);

        if (config.getBoolean("safety.avoid-unloaded-chunks", true)
                && !world.isChunkGenerated(x >> 4, z >> 4)) {
            return null;
        }

        int groundY = world.getHighestBlockYAt(x, z);
        int feetY = groundY + 1;
        int minY = Math.max(world.getMinHeight(), config.getInt("settings.min-y", world.getMinHeight()));
        int maxY = Math.min(world.getMaxHeight() - 2, config.getInt("settings.max-y", world.getMaxHeight() - 2));
        if (feetY < minY || feetY > maxY) {
            return null;
        }

        Location location = new Location(world, x + 0.5, feetY, z + 0.5);
        if (!world.getWorldBorder().isInside(location) || !isSafeLocation(location)) {
            return null;
        }
        location.setYaw(random.nextFloat() * 360.0f);
        location.setPitch(0.0f);
        return location;
    }

    private boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        Block ground = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
        Block feet = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Block head = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());

        if (config.getBoolean("safety.check-ground", true)
                && (!ground.getType().isSolid() || ground.isLiquid())) {
            return false;
        }
        if (config.getBoolean("safety.check-headroom", true)
                && (!feet.isPassable() || !head.isPassable())) {
            return false;
        }
        if (config.getBoolean("safety.avoid-suffocation", true)
                && (!feet.isPassable() || !head.isPassable())) {
            return false;
        }
        return !isDangerous(ground.getType())
                && !isDangerous(feet.getType())
                && !isDangerous(head.getType());
    }

    private boolean isDangerous(Material material) {
        if (config.getBoolean("safety.avoid-water", true)
                && (material == Material.WATER || material == Material.BUBBLE_COLUMN)) {
            return true;
        }
        if (config.getBoolean("safety.avoid-lava", true) && material == Material.LAVA) {
            return true;
        }
        if (config.getBoolean("safety.avoid-fire", true)
                && (material == Material.FIRE || material == Material.SOUL_FIRE)) {
            return true;
        }
        if (config.getBoolean("safety.avoid-cactus", true) && material == Material.CACTUS) {
            return true;
        }
        if (config.getBoolean("safety.avoid-magma", true) && material == Material.MAGMA_BLOCK) {
            return true;
        }
        return config.getBoolean("safety.avoid-powder-snow", true) && material == Material.POWDER_SNOW;
    }

    private void actionBar(Player player, String message) {
        if (!config.getBoolean("actionbar.enabled", true) || message == null || message.isEmpty()) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(core.color(message)));
    }

    private void playSound(Player player, String path) {
        if (!config.getBoolean("sounds.enabled", true)) {
            return;
        }
        String sound = config.getString("sounds." + path + ".sound", "UI_BUTTON_CLICK");
        float volume = (float) config.getDouble("sounds." + path + ".volume", 1.0);
        float pitch = (float) config.getDouble("sounds." + path + ".pitch", 1.0);
        core.playSound(player, sound, volume, pitch);
    }

    private Material getMaterial(String name, Material fallback) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        return material == null ? fallback : material;
    }

    private void message(Player player, String path) {
        send(player, config.getString("messages." + path, ""));
    }

    private void send(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        core.sendMessage(player, config.getString("messages.prefix", "") + message);
    }
}
