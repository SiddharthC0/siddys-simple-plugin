
package org.siddharth.ssmp.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.mechanics.PlayerInformationSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerInformationGUI implements Listener {

    private final PlayerInformationSystem playerInfo;
    private final File folder;
    private final File configFile;
    private final YamlConfiguration config;

    private static final int PAGE_WIDTH = 114;

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacySection();

    public PlayerInformationGUI(PlayerInformationSystem playerInfo) {
        this.playerInfo = playerInfo;

        JavaPlugin plugin = playerInfo.getPlugin();

        folder = new File(plugin.getDataFolder(), "playerinfogui");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Couldn't create the playerinfo folder.");
        }

        configFile = new File(folder, "config.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException("Couldn't add playerinfogui to config.yml");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create playerinfogui config.yml because exception: ", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("gui.title", "Player Information Journal");
        config.addDefault("gui.author", "Siddy's Simple Plugin");

        config.addDefault("colors.heading", "&6");
        config.addDefault("colors.label", "&7");
        config.addDefault("colors.value", "&f");
        config.addDefault("colors.highlight", "&e");
        config.addDefault("colors.secondary", "&8");

        config.options().copyDefaults(true);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save playerinfo due to exception: ", e);
        }
    }

    public void open(Player viewer, Player target) {

        if (viewer == null || target == null) {
            return;
        }

        SiddysSimplePlugin plugin = (SiddysSimplePlugin) playerInfo.getPlugin();

        String title = ChatColor.stripColor(
                getColoredString("gui.title", "Player Information ")
        );

        Component profile = SERIALIZER.deserialize(
                TextComponent.toLegacyText(createPlayerPage(target))
        );

        Component features = SERIALIZER.deserialize(
                TextComponent.toLegacyText(createLinksPage())
        );

        List<DialogBody> body = new ArrayList<>();

        body.add(DialogBody.plainMessage(profile));
        body.add(DialogBody.plainMessage(features));

        List<ActionButton> buttons = new ArrayList<>();

        if (playerInfo.hasBounties()) {
            buttons.add(createCommandButton(viewer, "Bounties", "bounty"));
        }

        if (playerInfo.hasContracts()) {
            buttons.add(createCommandButton(viewer, "Contracts", "contracts"));
        }

        buttons.add(
                ActionButton.builder(
                                Component.text("Close", NamedTextColor.GRAY)
                        )
                        .width(200)
                        .action(createAction(viewer, viewer::closeDialog))
                        .build()
        );

        Dialog dialog = Dialog.create(builder -> builder
                .empty()
                .base(
                        DialogBase.builder(
                                        Component.text(title, NamedTextColor.GOLD)
                                )
                                .canCloseWithEscape(true)
                                .body(body)
                                .build()
                )
                .type(DialogType.multiAction(buttons, null, 2))
        );

        viewer.showDialog(dialog);
    }

    private BaseComponent[] createPlayerPage(Player target) {

        List<BaseComponent> components = new ArrayList<>();

        SiddysSimplePlugin plugin = (SiddysSimplePlugin) playerInfo.getPlugin();

        addCenteredLine(
                components,
                "&6&l✦ PLAYER DOSSIER ✦"
        );

        addCenteredLine(
                components,
                "&8━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        addCenteredLine(
                components,
                "&f&l" + target.getName()
        );

        if (plugin.getRanks() != null) {
            addCenteredLine(
                    components,
                    "&6" + plugin.getRanks().getPlayerRankDisplayName(target)
            );
        }

        addCenteredLine(components, "");

        if (playerInfo.hasEconomy()) {

            addCenteredLine(
                    components,
                    "&6&l◆ ECONOMY"
            );

            addCenteredLine(
                    components,
                    "&7Balance  &e" + playerInfo.getFormattedBalance(target)
            );

            addCenteredLine(components, "");
        }

        if (playerInfo.hasBounties()) {

            addCenteredLine(
                    components,
                    "&c&l◆ BOUNTIES"
            );

            addCenteredLine(
                    components,
                    "&7Current bounty  &e" + playerInfo.getBounty(target)
            );

            addCenteredLine(components, "");
        }

        if (playerInfo.hasContracts()) {

            addCenteredLine(
                    components,
                    "&b&l◆ CONTRACTS"
            );

            addCenteredLine(
                    components,
                    "&7Completed  &f" + playerInfo.getCompletedContracts(target)
            );

            addCenteredLine(
                    components,
                    "&7Accepted  &f" + playerInfo.getAcceptedContracts(target)
            );

            addCenteredLine(components, "");
        }

        if (playerInfo.hasLevels()) {

            addCenteredLine(
                    components,
                    "&d&l◆ PROGRESSION"
            );

            addCenteredLine(
                    components,
                    "&7Level  &e" + playerInfo.getLevel(target)
            );

            addCenteredLine(
                    components,
                    "&7Experience  &f" + playerInfo.getXP(target)
            );

            addCenteredLine(components, "");
        }

        addCenteredLine(
                components,
                "&8━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        addCenteredLine(
                components,
                "&6&lSIDDY'S SIMPLE PLUGIN"
        );

        return components.toArray(new BaseComponent[0]);
    }

    private BaseComponent[] createLinksPage() {

        List<BaseComponent> components = new ArrayList<>();

        addCenteredLine(
                components,
                "&6&l✦ AVAILABLE FEATURES"
        );

        addCenteredLine(
                components,
                "&8━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        if (playerInfo.hasBounties()) {

            addCenteredLine(
                    components,
                    "&c&lBOUNTY SYSTEM"
            );

            addCenteredLine(
                    components,
                    "&7Place rewards on other players."
            );

            addCommandLink(
                    components,
                    "/bounty",
                    "/bounty"
            );

            addCenteredLine(components, "");
        }

        if (playerInfo.hasContracts()) {

            addCenteredLine(
                    components,
                    "&b&lCONTRACT SYSTEM"
            );

            addCenteredLine(
                    components,
                    "&7Complete contracts and earn rewards."
            );

            addCommandLink(
                    components,
                    "/contracts",
                    "/contracts"
            );

            addCenteredLine(components, "");
        }

        addCenteredLine(
                components,
                "&8━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        addCenteredLine(
                components,
                "&7Powered by &6SSMP"
        );

        return components.toArray(new BaseComponent[0]);
    }

    private ActionButton createCommandButton(
            Player player,
            String text,
            String command
    ) {

        return ActionButton.builder(
                        Component.text(text, NamedTextColor.AQUA)
                )
                .width(200)
                .action(createAction(
                        player,
                        () -> {
                            player.closeDialog();
                            player.performCommand(command);
                        }
                ))
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

    private void addCommandLink(
            List<BaseComponent> components,
            String text,
            String command
    ) {

        TextComponent link = new TextComponent("  " + text);

        link.setColor(ChatColor.AQUA);
        link.setUnderlined(true);
        link.setBold(true);

        link.setClickEvent(
                new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                        command
                )
        );

        components.add(link);
        components.add(new TextComponent("\n"));
    }

    private void addCenteredLine(
            List<BaseComponent> components,
            String text
    ) {

        String formatted = ChatColor.translateAlternateColorCodes('&', text);

        components.addAll(
                List.of(TextComponent.fromLegacyText(formatted))
        );

        components.add(new TextComponent("\n"));
    }

    private int getPixelWidth(String text) {

        int width = 0;
        boolean bold = false;

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (c == ChatColor.COLOR_CHAR && i + 1 < text.length()) {

                char code = Character.toLowerCase(text.charAt(++i));

                if (code == 'l') {
                    bold = true;
                } else if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) {
                    bold = false;
                }

                continue;
            }

            int charWidth;

            if (c == ' ') {
                charWidth = 4;
            } else if ("!.,:;'|iIl".indexOf(c) >= 0) {
                charWidth = 2;
            } else if ("()[]{}ft".indexOf(c) >= 0) {
                charWidth = 4;
            } else if ("@~".indexOf(c) >= 0) {
                charWidth = 7;
            } else {
                charWidth = 6;
            }

            width += charWidth + (bold ? 1 : 0);
        }

        return width;
    }

    private String getColor(String path, String fallback) {
        return config.getString(path, fallback);
    }

    private String getColoredString(String path, String fallback) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                config.getString(path, fallback)
        );
    }
    public YamlConfiguration getConfig() {
        return config;
    }
}
