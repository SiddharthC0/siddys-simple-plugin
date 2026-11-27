package org.siddharth.ssmp.gui;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.siddharth.ssmp.mechanics.InfoBarSystem;
import org.siddharth.ssmp.mechanics.PlayerInformationSystem;
import org.siddharth.ssmp.mechanics.ScoreboardSystem;
import org.siddharth.ssmp.mechanics.ToggleSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class PlayerInformationGUI implements Listener {
    private final PlayerInformationSystem playerInfo;
    private ScoreboardSystem scoreboardSystem;
    private InfoBarSystem infoBarSystem;
    private ToggleSystem toggleSystem;
    public PlayerInformationGUI(PlayerInformationSystem playerInfo) {
        this.playerInfo = playerInfo;
    }
    public void setInfoBarSystem(InfoBarSystem infoBarSystem) {this.infoBarSystem = infoBarSystem;}
    public void setToggleSystem(ToggleSystem toggleSystem) {this.toggleSystem = toggleSystem;}
    public void setScoreboardSystem(ScoreboardSystem scoreboardSystem) {
        this.scoreboardSystem = scoreboardSystem;
    }
    public void open(Player viewer, Player target) {
        if (viewer == null) return;
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(createSettingsButton(viewer, "Interface", () -> openInterface(viewer)));
        buttons.add(createSettingsButton(viewer, "Notifications", () -> viewer.closeDialog()));
        buttons.add(createSettingsButton(viewer, "Chat", () -> viewer.closeDialog()));
        buttons.add(createSettingsButton(viewer, "Social", () -> viewer.closeDialog()));

        if (playerInfo.hasBounties()) {buttons.add(createCommandButton(viewer, "Bounties", "bounty"));}
        if (playerInfo.hasContracts()) {buttons.add(createCommandButton(viewer, "Contracts", "contracts"));}
        buttons.add(
                ActionButton.builder(Component.text("Close", NamedTextColor.GRAY)).width(200).action(createAction(viewer, viewer::closeDialog)).build()
        );

        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.empty()).canCloseWithEscape(true).build()).type(DialogType.multiAction(buttons, null, 2)));
        viewer.showDialog(dialog);
    }

    private void openInterface(Player player) {
        if (player == null) return;
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(createToggleButton(
                player,
                "Scoreboard",
                "interface.scoreboard"
        ));

        buttons.add(createToggleButton(
                player,
                "Action Bar",
                "interface.actionbar"
        ));

        buttons.add(createToggleButton(
                player,
                "InfoBar / Topbar",
                "interface.infobar"
        ));

        buttons.add(createToggleButton(
                player,
                "Sounds",
                "interface.sounds"
        ));

        buttons.add(ActionButton.builder(Component.text("Back", NamedTextColor.GRAY)).width(200).action(createAction(player, () -> open(player, player))).build());
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.empty()).canCloseWithEscape(true).build()).type(DialogType.multiAction(buttons, null, 2)));
        player.showDialog(dialog);
    }

    private ActionButton createToggleButton(Player player, String name, String setting) {
        boolean enabled = playerInfo.isEnabled(player, setting);

        Component text = Component.text(
                name + ": " + (enabled ? "ON" : "OFF"),
                enabled ? NamedTextColor.GREEN : NamedTextColor.RED
        );

        return ActionButton.builder(text)
                .width(200)
                .action(createAction(player, () -> {

                    if (toggleSystem != null) {
                        toggleSystem.toggle(player, setting);
                    } else {
                        playerInfo.toggleSetting(player, setting);
                    }

                    if (setting.equals("interface.scoreboard") && scoreboardSystem != null) {
                        scoreboardSystem.refreshPlayer(player);
                    }

                    if (setting.equals("interface.infobar") && infoBarSystem != null) {
                        infoBarSystem.updatePlayer(player);
                    }

                    openInterface(player);
                }))
                .build();
    }

    private ActionButton createSettingsButton(Player player, String text, Runnable action) {
        return ActionButton.builder(Component.text(text, NamedTextColor.AQUA))
                .width(200)
                .action(createAction(player, action))
                .build();
    }

    private ActionButton createCommandButton(
            Player player,
            String text,
            String command
    ) {
        return ActionButton.builder(Component.text(text, NamedTextColor.AQUA))
                .width(200)
                .action(createAction(player, () -> {
                    player.closeDialog();
                    player.performCommand(command);
                }))
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
}