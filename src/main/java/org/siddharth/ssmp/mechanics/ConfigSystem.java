
package org.siddharth.ssmp.mechanics;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.siddharth.ssmp.SiddysSimplePlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ConfigSystem implements Listener {

    private final SiddysSimplePlugin plugin;

    private static final int PAGE_SIZE = 8;
    private static final int COLUMNS = 2;
    private static final int BUTTON_WIDTH = 200;

    private final List<ModuleEntry> modules = List.of(
            new ModuleEntry("ranks", "Ranks", "CORE"),
            new ModuleEntry("permissions", "Permissions", "CORE"),
            new ModuleEntry("economy", "Economy", "CORE"),
            new ModuleEntry("contracts", "Contracts", "STANDALONE"),
            new ModuleEntry("bounties", "Bounties", "STANDALONE"),
            new ModuleEntry("shop", "Shop", "STANDALONE"),
            new ModuleEntry("warps", "Warps", "STANDALONE"),
            new ModuleEntry("kits", "Kits", "STANDALONE"),
            new ModuleEntry("rtp", "Random Teleport", "STANDALONE"),
            new ModuleEntry("cpanel", "Control Panel", "STANDALONE"),
            new ModuleEntry("levels", "Levels", "CORE"),
            new ModuleEntry("scoreboard", "Scoreboard", "STANDALONE"),
            new ModuleEntry("chat", "Chat", "CORE"),
            new ModuleEntry("placeholders", "Placeholders", "CORE"),
            new ModuleEntry("menu", "Menu", "CORE"),
            new ModuleEntry("auction", "Auction House", "STANDALONE"),
            new ModuleEntry("tablist", "Tablist", "STANDALONE"),
            new ModuleEntry("enderchest", "Ender Chest", "STANDALONE"),
            new ModuleEntry("homes", "Homes", "STANDALONE"),
            new ModuleEntry("infobar", "InfoBar", "STANDALONE"),
            new ModuleEntry("nickname", "Nicknames", "CORE")
    );

    private final Map<UUID, Map<String, Boolean>> pendingChanges = new LinkedHashMap<>();

    public ConfigSystem(SiddysSimplePlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (plugin.getCommand("configp") != null) {
            plugin.getCommand("configp").setExecutor((sender, command, label, args) -> {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }

                openMenu(player);
                return true;
            });
        }
    }

    public void openMenu(Player player) {

        if (player.hasPermission("ssmp.config")) {
            pendingChanges.put(player.getUniqueId(), loadCurrentSettings());
        }

        showPage(player, 0);
    }

    private Map<String, Boolean> loadCurrentSettings() {

        Map<String, Boolean> changes = new LinkedHashMap<>();

        for (ModuleEntry module : modules) {
            changes.put(
                    module.id,
                    plugin.getConfig().getBoolean(
                            "modules." + module.id + ".enabled",
                            true
                    )
            );
        }

        return changes;
    }

    private void showPage(Player player, int page) {

        boolean canConfigure = player.hasPermission("ssmp.config");

        if (!canConfigure) {
            pendingChanges.remove(player.getUniqueId());
        }

        Map<String, Boolean> changes = canConfigure
                ? pendingChanges.computeIfAbsent(
                player.getUniqueId(),
                uuid -> loadCurrentSettings()
        )
                : loadCurrentSettings();

        int totalPages = (modules.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        page = Math.max(0, Math.min(page, totalPages - 1));

        int currentPage = page;

        List<ActionButton> buttons = new ArrayList<>();

        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, modules.size());

        for (int i = start; i < end; i++) {

            ModuleEntry module = modules.get(i);
            boolean enabled = changes.getOrDefault(module.id, true);

            Component label = Component.text(
                    (enabled ? "● " : "○ ") + module.name,
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            );

            buttons.add(
                    createButton(
                            label,
                            player,
                            () -> {

                                if (!player.hasPermission("ssmp.config")) {
                                    pendingChanges.remove(player.getUniqueId());
                                    player.closeDialog();
                                    return;
                                }

                                Map<String, Boolean> pending =
                                        pendingChanges.get(player.getUniqueId());

                                if (pending == null) return;

                                pending.put(module.id, !enabled);

                                showPage(player, currentPage);
                            }
                    )
            );
        }

        while (buttons.size() < PAGE_SIZE) {
            buttons.add(
                    createButton(
                            Component.text(" "),
                            player,
                            () -> {}
                    )
            );
        }

        buttons.add(
                currentPage > 0
                        ? createButton(
                        Component.text("← Previous", NamedTextColor.YELLOW),
                        player,
                        () -> showPage(player, currentPage - 1)
                )
                        : createButton(Component.text(" "), player, () -> {})
        );

        buttons.add(
                currentPage < totalPages - 1
                        ? createButton(
                        Component.text("Next →", NamedTextColor.YELLOW),
                        player,
                        () -> showPage(player, currentPage + 1)
                )
                        : createButton(Component.text(" "), player, () -> {})
        );

        if (canConfigure) {

            buttons.add(
                    createButton(
                            Component.text("Save Changes", NamedTextColor.GREEN),
                            player,
                            () -> saveChanges(player)
                    )
            );

            buttons.add(
                    createButton(
                            Component.text("Discard Changes", NamedTextColor.RED),
                            player,
                            () -> discardChanges(player)
                    )
            );

        } else {

            buttons.add(
                    createButton(
                            Component.text("Close", NamedTextColor.GRAY),
                            player,
                            player::closeDialog
                    )
            );

            buttons.add(
                    createButton(Component.text(" "), player, () -> {})
            );
        }

        Component title = Component.text(
                "SSMP Module Manager  |  " + (currentPage + 1) + "/" + totalPages,
                NamedTextColor.GOLD
        );

        Dialog dialog = Dialog.create(builder -> builder
                .empty()
                .base(
                        DialogBase.builder(title)
                                .canCloseWithEscape(true)
                                .build()
                )
                .type(DialogType.multiAction(buttons, null, COLUMNS))
        );

        player.showDialog(dialog);
    }

    private ActionButton createButton(
            Component label,
            Player player,
            Runnable action
    ) {

        return ActionButton.builder(label)
                .width(BUTTON_WIDTH)
                .action(createAction(player, action))
                .build();
    }

    private DialogAction createAction(Player player, Runnable action) {

        UUID uuid = player.getUniqueId();

        return DialogAction.customClick(
                (response, audience) -> {

                    if (!(audience instanceof Player clicker)) return;
                    if (!clicker.getUniqueId().equals(uuid)) return;

                    action.run();
                },
                ClickCallback.Options.builder()
                        .uses(1)
                        .build()
        );
    }

    private void saveChanges(Player player) {

        UUID uuid = player.getUniqueId();

        if (!player.hasPermission("ssmp.config")) {
            pendingChanges.remove(uuid);
            player.closeDialog();
            return;
        }

        Map<String, Boolean> changes = pendingChanges.get(uuid);

        if (changes == null) return;

        for (ModuleEntry module : modules) {
            plugin.getConfig().set(
                    "modules." + module.id + ".enabled",
                    changes.getOrDefault(module.id, true)
            );
        }

        try {
            plugin.saveConfig();
        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Failed to save module configuration: " + exception.getMessage()
            );

            exception.printStackTrace();

            player.sendMessage(
                    Component.text(
                            "Failed to save module configuration.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        player.closeDialog();

        pendingChanges.remove(uuid);

        player.sendMessage(
                Component.text(
                        "SSMP module configuration saved.",
                        NamedTextColor.GREEN
                )
        );

        player.sendMessage(
                Component.text(
                        "Reloading modules...",
                        NamedTextColor.YELLOW
                )
        );

        Bukkit.getScheduler().runTask(plugin, () -> {

            if (plugin.reloadModules()) {

                player.sendMessage(
                        Component.text(
                                "SSMP modules reloaded successfully.",
                                NamedTextColor.GREEN
                        )
                );

            } else {

                player.sendMessage(
                        Component.text(
                                "Module reload failed. Check the console.",
                                NamedTextColor.RED
                        )
                );
            }
        });
    }

    private void discardChanges(Player player) {

        pendingChanges.remove(player.getUniqueId());

        player.closeDialog();

        player.sendMessage(
                Component.text(
                        "Pending module changes discarded.",
                        NamedTextColor.YELLOW
                )
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingChanges.remove(event.getPlayer().getUniqueId());
    }

    private record ModuleEntry(String id, String name, String type) {}
}
