package org.siddharth.ssmp.chat;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.ssmp;
import org.siddharth.ssmp.mechanics.soundSystem;
import java.util.Collection;
public class chatDelivery {
    private final ssmp plugin;
    private final chatConfig config;
    private final soundSystem sounds;
    public chatDelivery(ssmp plugin, chatConfig config, soundSystem sounds) {
        this.plugin = plugin;
        this.config = config;
        this.sounds = sounds;
    }

    public void deliver(Player sender, Collection<Player> recipients, BaseComponent[] message) {
        if (recipients == null || message == null) {
            return;
        }
        boolean skipDisconnected = config.getDelivery().getBoolean("skip-disconnected", true);

        for (Player recipient : recipients) {
            if (recipient == null) {
                continue;
            } else if (skipDisconnected && !recipient.isOnline()) {
                continue;
            }
            try {
                recipient.spigot().sendMessage(message);
                if (config.getDelivery().getBoolean("sounds.enabled", true)) {
                    boolean excludeSender = config.getDelivery().getBoolean("sounds.exclude-sender", false);
                    if (!excludeSender || !recipient.getUniqueId().equals(sender.getUniqueId())) {
                        String sound = config.getDelivery().getString("sounds.name", "message");
                        sounds.play(recipient, sound);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not deliver chat to " + recipient.getName() + ": " + e.getMessage());
            }
        }
    }

    public void deliverFallback(Player sender, Collection<Player> recipients, String message) {
        if (recipients == null) {
            return;
        }
        String fallback = "<" + sender.getName() + "> " + message;
        for (Player recipient : recipients) {
            if (recipient == null || !recipient.isOnline()) {
                continue;
            }
            try {
                recipient.sendMessage(fallback);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not deliver fallback chat: " + e.getMessage());
            }
        }
    }
}