package org.siddharth.ssmp.mechanics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.siddharth.ssmp.SiddysSimplePlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
public class TPASystem implements CommandExecutor, TabCompleter, Listener {

    private final SiddysSimplePlugin plugin;
    private final NicknameSystem nicknames;
    private final File configFile;
    private final YamlConfiguration config;
    private final Map<UUID, Request> incoming = new HashMap<>();
    private final Map<UUID, Request> outgoing = new HashMap<>();
    private final Map<UUID, Teleport> teleports = new HashMap<>();

    private static class Request {
        private final UUID requester;
        private final UUID target;
        private final boolean teleportHere;
        private BukkitTask expiryTask;

        private Request(UUID requester, UUID target, boolean teleportHere) {
            this.requester = requester;
            this.target = target;
            this.teleportHere = teleportHere;
        }
    }

    private static class Teleport {
        private final UUID player;
        private final Location destination;
        private final Location start;
        private BukkitTask task;
        private int seconds = 3;

        private Teleport(Player player, Location destination) {
            this.player = player.getUniqueId();
            this.destination = destination.clone();
            this.start = player.getLocation().clone();
        }
    }

    public TPASystem(SiddysSimplePlugin plugin, NicknameSystem nicknames) {
        this.plugin = plugin;
        this.nicknames = nicknames;

        File folder = new File(plugin.getDataFolder(), "tpa");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new RuntimeException("Could not create TPA configuration folder.");
        }

        configFile = new File(folder, "config.yml");
        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException("Could not create TPA config.yml.");
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not create TPA config.yml.", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        addDefaults();
        saveConfig();
    }

    private void addDefaults() {
        config.addDefault("enabled", true);
        config.addDefault("request.expiry", 60);
        config.addDefault("teleport.delay", 3);
        config.addDefault("teleport.cancel-on-move", true);
        config.addDefault("teleport.cancel-on-damage", true);
        config.addDefault("teleport.cancel-on-disconnect", true);
        config.addDefault("settings.allow-tpa", true);
        config.addDefault("settings.allow-tpahere", true);
        config.addDefault("settings.allow-cross-world", true);

        config.addDefault("messages.prefix", "&6&lSSMP &8» ");
        config.addDefault("messages.player-only", "&cOnly players can use this command.");
        config.addDefault("messages.disabled", "&cThe TPA system is disabled.");
        config.addDefault("messages.no-permission", "&cYou don't have permission.");
        config.addDefault("messages.usage", "&cUsage: /%command% <player>");
        config.addDefault("messages.player-not-found", "&cNo online player found matching that name or nickname.");
        config.addDefault("messages.ambiguous-player", "&cThat nickname matches multiple online players.");
        config.addDefault("messages.self-request", "&cYou cannot send a request to yourself.");
        config.addDefault("messages.request-sent", "&aTeleport request sent to &e%player%&a.");
        config.addDefault("messages.request-received", "&e%player% &ahas requested to teleport to you. Use &e/tpaccept %player% &aor &e/tpdeny %player%&a.");
        config.addDefault("messages.here-request-sent", "&aTeleport-here request sent to &e%player%&a.");
        config.addDefault("messages.here-request-received", "&e%player% &awants you to teleport to them. Use &e/tpaccept %player% &aor &e/tpdeny %player%&a.");
        config.addDefault("messages.request-expired", "&cYour teleport request has expired.");
        config.addDefault("messages.request-denied", "&cYour teleport request was denied.");
        config.addDefault("messages.request-cancelled", "&cYour outgoing teleport request was cancelled.");
        config.addDefault("messages.no-request", "&cYou don't have a matching pending request.");
        config.addDefault("messages.request-accepted", "&aRequest accepted. Teleporting in &e%seconds% seconds&a.");
        config.addDefault("messages.teleport-countdown", "&aTeleporting in &e%seconds%&a...");
        config.addDefault("messages.teleport-success", "&aTeleport successful!");
        config.addDefault("messages.teleport-cancelled", "&cTeleportation cancelled.");
        config.addDefault("messages.target-busy", "&cThat player already has a pending teleport request.");
        config.addDefault("messages.cross-world-disabled", "&cCross-world teleport requests are disabled.");
        config.addDefault("messages.target-disconnected", "&cThe other player disconnected.");
        config.options().copyDefaults(true);
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save TPA config: " + e.getMessage());
        }
    }

    private String message(String path) {
        String prefix = config.getString("messages.prefix", "&6&lSSMP &8» ");
        String value = config.getString("messages." + path, "&cMissing message: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + value);
    }

    private void send(Player player, String path) {
        player.sendMessage(message(path));
    }

    private void send(Player player, String path, String placeholder, String replacement) {
        player.sendMessage(message(path).replace(placeholder, replacement));
    }

    private Player resolvePlayer(String input, Player sender) {
        if (input == null || input.isBlank()) {
            return null;
        }

        Player exactName = Bukkit.getPlayerExact(input);
        if (exactName != null) {
            return exactName;
        }

        List<Player> matches = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String nickname = nicknames == null ? null : nicknames.getNickname(player);

            if (nickname != null && nickname.equalsIgnoreCase(input)) {
                matches.add(player);
            }
        }

        if (matches.size() == 1) {
            return matches.get(0);
        }

        if (matches.size() > 1) {
            return null;
        }

        List<Player> partialMatches = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String nickname = nicknames == null ? null : nicknames.getNickname(player);

            if (nickname != null && nickname.equalsIgnoreCase(input)) {
                partialMatches.add(player);
            }
        }

        return partialMatches.size() == 1 ? partialMatches.get(0) : null;
    }

    private String displayName(Player player) {
        return nicknames == null ? player.getName() : nicknames.getDisplayNickname(player);
    }

    private boolean enabled(Player player) {
        if (!config.getBoolean("enabled", true)) {
            send(player, "disabled");
            return false;
        }
        return true;
    }

    private void sendRequest(Player requester, Player target, boolean teleportHere) {
        if (!enabled(requester)) return;

        if (requester.equals(target)) {
            send(requester, "self-request");
            return;
        }

        if (!requester.hasPermission("ssmp.tpa.use")) {
            send(requester, "no-permission");
            return;
        }

        if (teleportHere && !config.getBoolean("settings.allow-tpahere", true)) {
            send(requester, "disabled");
            return;
        }

        if (!teleportHere && !config.getBoolean("settings.allow-tpa", true)) {
            send(requester, "disabled");
            return;
        }

        if (!config.getBoolean("settings.allow-cross-world", true)
                && !requester.getWorld().equals(target.getWorld())) {
            send(requester, "cross-world-disabled");
            return;
        }

        if (incoming.containsKey(target.getUniqueId())) {
            send(requester, "target-busy");
            return;
        }

        if (outgoing.containsKey(requester.getUniqueId())) {
            cancelRequest(requester.getUniqueId(), true);
        }

        Request request = new Request(
                requester.getUniqueId(),
                target.getUniqueId(),
                teleportHere
        );

        incoming.put(target.getUniqueId(), request);
        outgoing.put(requester.getUniqueId(), request);

        int expiry = Math.max(1, config.getInt("request.expiry", 60));

        request.expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeRequest(request);
            Player onlineRequester = Bukkit.getPlayer(request.requester);
            if (onlineRequester != null) send(onlineRequester, "request-expired");
        }, expiry * 20L);

        if (teleportHere) {
            send(requester, "here-request-sent", "%player%", displayName(target));
            send(target, "here-request-received", "%player%", displayName(requester));
        } else {
            send(requester, "request-sent", "%player%", displayName(target));
            send(target, "request-received", "%player%", displayName(requester));
        }
    }

    private void removeRequest(Request request) {
        if (request.expiryTask != null) {
            request.expiryTask.cancel();
        }

        incoming.remove(request.target, request);
        outgoing.remove(request.requester, request);
    }

    private void cancelRequest(UUID requester, boolean notify) {
        Request request = outgoing.get(requester);
        if (request == null) return;

        removeRequest(request);

        Player player = Bukkit.getPlayer(requester);
        if (notify && player != null) send(player, "request-cancelled");
    }

    private void acceptRequest(Player target, String requestedPlayer) {
        Request request = incoming.get(target.getUniqueId());

        if (request == null) {
            send(target, "no-request");
            return;
        }

        if (requestedPlayer != null) {
            Player requester = resolvePlayer(requestedPlayer, target);

            if (requester == null || !request.requester.equals(requester.getUniqueId())) {
                send(target, "no-request");
                return;
            }
        }

        Player requester = Bukkit.getPlayer(request.requester);

        if (requester == null) {
            removeRequest(request);
            send(target, "target-disconnected");
            return;
        }

        removeRequest(request);

        Player teleportingPlayer = request.teleportHere ? target : requester;
        Player destinationPlayer = request.teleportHere ? requester : target;

        if (!config.getBoolean("settings.allow-cross-world", true)
                && !teleportingPlayer.getWorld().equals(destinationPlayer.getWorld())) {
            send(target, "cross-world-disabled");
            return;
        }

        startTeleport(teleportingPlayer, destinationPlayer.getLocation());
        send(requester, "request-accepted", "%seconds%",
                String.valueOf(config.getInt("teleport.delay", 3)));
        if (!request.teleportHere) {
            send(target, "request-accepted", "%seconds%",
                    String.valueOf(config.getInt("teleport.delay", 3)));
        }
    }

    private void denyRequest(Player target, String requestedPlayer) {
        Request request = incoming.get(target.getUniqueId());

        if (request == null) {
            send(target, "no-request");
            return;
        }

        if (requestedPlayer != null) {
            Player requester = resolvePlayer(requestedPlayer, target);

            if (requester == null || !request.requester.equals(requester.getUniqueId())) {
                send(target, "no-request");
                return;
            }
        }

        removeRequest(request);

        Player requester = Bukkit.getPlayer(request.requester);
        if (requester != null) send(requester, "request-denied");
        send(target, "request-denied");
    }

    private void startTeleport(Player player, Location destination) {
        cancelTeleport(player.getUniqueId(), false);

        Teleport teleport = new Teleport(player, destination);
        teleports.put(player.getUniqueId(), teleport);

        int delay = Math.max(0, config.getInt("teleport.delay", 3));

        if (delay == 0) {
            finishTeleport(teleport);
            return;
        }

        send(player, "teleport-countdown", "%seconds%", String.valueOf(delay));

        teleport.seconds = delay;
        teleport.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelTeleport(player.getUniqueId(), false);
                return;
            }

            teleport.seconds--;

            if (teleport.seconds <= 0) {
                finishTeleport(teleport);
            } else {
                send(player, "teleport-countdown", "%seconds%",
                        String.valueOf(teleport.seconds));
            }
        }, 20L, 20L);
    }

    private void finishTeleport(Teleport teleport) {
        Player player = Bukkit.getPlayer(teleport.player);

        if (player == null || !player.isOnline()) {
            cancelTeleport(teleport.player, false);
            return;
        }

        if (teleport.task != null) {
            teleport.task.cancel();
        }

        teleports.remove(teleport.player);
        player.teleport(teleport.destination);
        send(player, "teleport-success");
    }

    private void cancelTeleport(UUID playerId, boolean notify) {
        Teleport teleport = teleports.remove(playerId);
        if (teleport == null) return;

        if (teleport.task != null) {
            teleport.task.cancel();
        }

        Player player = Bukkit.getPlayer(playerId);
        if (notify && player != null) send(player, "teleport-cancelled");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!config.getBoolean("teleport.cancel-on-move", true)) return;

        Teleport teleport = teleports.get(event.getPlayer().getUniqueId());
        if (teleport == null || event.getTo() == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getWorld() != to.getWorld()
                || from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            cancelTeleport(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!config.getBoolean("teleport.cancel-on-damage", true)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        cancelTeleport(player.getUniqueId(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        cancelTeleport(uuid, false);
        cancelRequest(uuid, false);

        Request request = incoming.get(uuid);
        if (request != null) {
            removeRequest(request);
            Player requester = Bukkit.getPlayer(request.requester);
            if (requester != null) send(requester, "target-disconnected");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!enabled(player)) return true;

        String name = command.getName().toLowerCase();

        switch (name) {
            case "tpa", "tpahere" -> {
                if (args.length == 0) {
                    send(player, "usage", "%command%", name);
                    return true;
                }

                String targetInput = String.join(" ", args);
                Player target = resolvePlayer(targetInput, player);

                if (target == null) {
                    boolean ambiguous = false;
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        String nickname = nicknames == null ? null : nicknames.getNickname(online);
                        if (nickname != null && nickname.equalsIgnoreCase(targetInput)) {
                            ambiguous = true;
                            break;
                        }
                    }
                    send(player, ambiguous ? "ambiguous-player" : "player-not-found");
                    return true;
                }

                sendRequest(player, target, name.equals("tpahere"));
            }
            case "tpaccept" -> acceptRequest(player, args.length == 0 ? null : String.join(" ", args));
            case "tpdeny" -> denyRequest(player, args.length == 0 ? null : String.join(" ", args));
            case "tpcancel" -> cancelRequest(player.getUniqueId(), true);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        String name = command.getName().toLowerCase();

        if (name.equals("tpa") || name.equals("tpahere")) {
            if (args.length != 1) return Collections.emptyList();

            String input = args[0].toLowerCase();
            Set<String> suggestions = new LinkedHashSet<>();

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;

                if (online.getName().toLowerCase().startsWith(input)) {
                    suggestions.add(online.getName());
                }

                String nickname = nicknames == null ? null : nicknames.getNickname(online);
                if (nickname != null && nickname.toLowerCase().startsWith(input)) {
                    suggestions.add(nickname);
                }
            }

            return new ArrayList<>(suggestions);
        }

        if (name.equals("tpaccept") || name.equals("tpdeny")) {
            Request request = incoming.get(player.getUniqueId());
            if (request == null) return Collections.emptyList();

            Player requester = Bukkit.getPlayer(request.requester);
            if (requester == null) return Collections.emptyList();

            List<String> suggestions = new ArrayList<>();
            suggestions.add(requester.getName());

            String nickname = nicknames == null ? null : nicknames.getNickname(requester);
            if (nickname != null) suggestions.add(nickname);

            String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
            return suggestions.stream()
                    .filter(value -> value.toLowerCase().startsWith(input))
                    .distinct()
                    .toList();
        }

        return Collections.emptyList();
    }

    public void shutdown() {
        for (Request request : new ArrayList<>(incoming.values())) {
            removeRequest(request);
        }

        for (UUID uuid : new ArrayList<>(teleports.keySet())) {
            cancelTeleport(uuid, false);
        }

        incoming.clear();
        outgoing.clear();
        teleports.clear();
    }

    public void reload() {
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(configFile);
        config.setDefaults(loaded);
        config.options().copyDefaults(true);
    }
}
