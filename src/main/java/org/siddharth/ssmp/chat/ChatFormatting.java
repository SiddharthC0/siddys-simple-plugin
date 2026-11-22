package org.siddharth.ssmp.chat;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.mechanics.PlaceholderSystem;
import org.siddharth.ssmp.mechanics.RankSystem;
import org.siddharth.ssmp.mechanics.SoundSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class ChatFormatting implements Listener {
    private final SiddysSimplePlugin plugin;
    private final ChatConfig config;
    private final ChatProviderManager providers;
    private final ChatMessageBuilder builder;
    private final ChatDelivery delivery;

    public ChatFormatting(SiddysSimplePlugin plugin, RankSystem ranks, SoundSystem sounds, PlaceholderSystem placeholders) {
        this.plugin = plugin;
        config = new ChatConfig(plugin);
        providers = new ChatProviderManager(plugin, ranks);
        builder = new ChatMessageBuilder(plugin, config, providers, placeholders);
        delivery = new ChatDelivery(plugin, config, sounds);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!config.getConfig().getBoolean("enabled", true)) {
            return;
        }
        Player sender = event.getPlayer();
        String message = event.getMessage();
        boolean respectRecipients = config.getDelivery().getBoolean("respect-recipients", true);

        // May contain bugs at this is my first time using tenary :(
        Collection<Player> recipients = new ArrayList<>(respectRecipients ? event.getRecipients() : List.of());
        boolean includeSender = config.getDelivery().getBoolean("include-sender", true);

        event.setCancelled(true);
        Runnable task = () -> {
            if (!sender.isOnline()) {
                return;
            }
            try {
                if (!respectRecipients) {
                    recipients.addAll(Bukkit.getOnlinePlayers());
                }
                if (includeSender && !recipients.contains(sender)) {
                    recipients.add(sender);
                } else if (!includeSender) {
                    recipients.remove(sender);
                }
                BaseComponent[] components = builder.build(sender, message);
                delivery.deliver(sender, recipients, components);
            } catch (Exception e) {
                plugin.getLogger().severe("Chat formatting failed: " + e.getMessage());
                if (config.getConfig().getBoolean("fallback-to-vanilla", true)) {
                    delivery.deliverFallback(sender, recipients, message);
                }
            }
        };

        if (event.isAsynchronous()) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            task.run();
        }
    }

    public void setFormat(String format) {
        if (format == null || format.isBlank()) {
            return;
        }
        config.getFormatting().set("format", format);
        config.saveAll();
    }

    public String getFormat() {
        return config.getFormatting().getString("format", "%ssmp_player_prefix%%player%%ssmp_player_suffix% &8» &f%message%");
    }

    public void registerPrefixProvider(Function<Player, String> provider) {
        providers.registerPrefixProvider(provider);
    }

    public void registerSuffixProvider(Function<Player, String> provider) {
        providers.registerSuffixProvider(provider);
    }

    public void setNameProvider(Function<Player, String> provider) {
        providers.setNameProvider(provider);
    }

    public void reload() {
        config.reload();
    }

    public ChatConfig getChatConfig() {
        return config;
    }
}