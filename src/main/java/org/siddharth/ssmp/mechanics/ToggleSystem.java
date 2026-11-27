
package org.siddharth.ssmp.mechanics;

import org.bukkit.entity.Player;

public final class ToggleSystem {

    private final PlayerInformationSystem playerInfo;
    private ScoreboardSystem scoreboardSystem;
    private InfoBarSystem infoBarSystem;

    public ToggleSystem(PlayerInformationSystem playerInfo) {
        this.playerInfo = playerInfo;
    }

    public void setScoreboardSystem(ScoreboardSystem scoreboardSystem) {
        this.scoreboardSystem = scoreboardSystem;
    }

    public void setInfoBarSystem(InfoBarSystem infoBarSystem) {
        this.infoBarSystem = infoBarSystem;
    }

    public boolean isActionBarEnabled(Player player) {
        return isEnabled(player, "interface.actionbar");
    }

    public boolean isScoreboardEnabled(Player player) {
        return isEnabled(player, "interface.scoreboard");
    }

    public boolean isInfoBarEnabled(Player player) {
        return isEnabled(player, "interface.infobar");
    }

    public boolean isSoundEnabled(Player player) {
        return isEnabled(player, "interface.sounds");
    }

    public boolean isEnabled(Player player, String setting) {
        return playerInfo.isEnabled(player, setting);
    }

    public void setEnabled(Player player, String setting, boolean enabled) {
        if (player == null) return;

        playerInfo.setSetting(player, setting, enabled);
        refresh(player, setting);
    }

    public void toggle(Player player, String setting) {
        if (player == null) return;

        boolean enabled = !isEnabled(player, setting);
        setEnabled(player, setting, enabled);
    }

    private void refresh(Player player, String setting) {

        switch (setting) {

            case "interface.scoreboard" -> {
                if (scoreboardSystem != null) {
                    scoreboardSystem.refreshPlayer(player);
                }
            }

            case "interface.infobar" -> {
                if (infoBarSystem != null) {
                    infoBarSystem.updatePlayer(player);
                }
            }
        }
    }
}
