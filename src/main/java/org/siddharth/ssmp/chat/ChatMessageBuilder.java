package org.siddharth.ssmp.chat;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.mechanics.PlaceholderSystem;

import java.util.List;

public class ChatMessageBuilder {
    // Do not change these values from here
    private static final String NAME_TOKEN = "\u0001SSMP_NAME\u0002";
    private static final String MESSAGE_TOKEN = "\u0001SSMP_MESSAGE\u0002";
    private final SiddysSimplePlugin plugin;
    private final ChatConfig config;
    private final ChatProviderManager providers;
    private final PlaceholderSystem placeholders;

    public ChatMessageBuilder(SiddysSimplePlugin plugin, ChatConfig config, ChatProviderManager providers, PlaceholderSystem placeholders) {
        this.plugin = plugin;
        this.config = config;
        this.providers = providers;
        this.placeholders = placeholders;
    }

    public BaseComponent[] build(Player player, String message) {
        String prefix = providers.getPrefix(player);
        String suffix = providers.getSuffix(player);
        String name = providers.getName(player);

        String template = config.getFormatting().getString("format", "%ssmp_player_prefix%%player%%ssmp_player_suffix% &8» &f%message%");

        template = template.replace("%ssmp_player_prefix%", prefix).replace("%ssmp_player_suffix%", suffix).replace("%player%", NAME_TOKEN).replace("%message%", MESSAGE_TOKEN);
        if (config.getConfig().getBoolean("placeholders.internal", true) && placeholders != null) {
            template = placeholders.replace(player, template);
        }else if (config.getConfig().getBoolean("placeholders.placeholderapi", true)) {
            template = expandPapi(player, template);
        }

        String[] parts = template.split(java.util.regex.Pattern.quote(NAME_TOKEN) + "|" + java.util.regex.Pattern.quote(MESSAGE_TOKEN), -1);
        TextComponent root = new TextComponent();
        int cursor = 0;
        while (cursor < template.length()) {
            int nameIndex = template.indexOf(NAME_TOKEN, cursor);
            int messageIndex = template.indexOf(MESSAGE_TOKEN, cursor);
            int next;
            if (nameIndex == -1) next = messageIndex;
            else if (messageIndex == -1) next = nameIndex;
            else next = Math.min(nameIndex, messageIndex);

            if (next == -1) {
                addLegacy(root, template.substring(cursor));
                break;
            }

            addLegacy(root, template.substring(cursor, next));
            if (next == nameIndex) {
                root.addExtra(createPlayerName(player, name));
                cursor = nameIndex + NAME_TOKEN.length();
            } else {
                addMessage(root, message);
                cursor = messageIndex + MESSAGE_TOKEN.length();
            }
        }

        // I am still not confident if my tenary is right
        return root.getExtra() == null ? new BaseComponent[]{root} : root.getExtra().toArray(new BaseComponent[0]);
    }

    private TextComponent createPlayerName(Player player, String name) {
        String displayName = translate(name);
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(displayName));
        if (config.getHover().getBoolean("enabled", true) && config.getHover().getBoolean("player.enabled", true)) {
            List<String> lines = config.getHover().getStringList("player.lines");
            if (!lines.isEmpty()) {
                String hoverText = String.join("\n", lines);
                hoverText = resolve(player, hoverText);
                hoverText = translate(hoverText);
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText)));
            }
        }
        if (config.getHover().getBoolean("click.enabled", false)) {
            String actionName = config.getHover().getString("click.action", "SUGGEST_COMMAND");
            String value = config.getHover().getString("click.value", "/msg %realname% ");

            value = resolve(player, value);
            try {
                ClickEvent.Action action = ClickEvent.Action.valueOf(actionName.toUpperCase());
                component.setClickEvent(new ClickEvent(action, value));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid chat click action: " + actionName);
            }
        }

        return component;
    }

    private void addLegacy(TextComponent root, String text) {
        if (text.isEmpty()) {
            return;
        }
        for (BaseComponent component : TextComponent.fromLegacyText(translate(text))) {
            root.addExtra(component);
        }
    }

    private void addMessage(TextComponent root, String message) {
        if (message == null) {
            message = "";
        }
        if (config.getFormatting().getBoolean("message.translate-colors", false)) {
            addLegacy(root, message);
        } else {
            root.addExtra(new TextComponent(message));
        }
    }

    private String resolve(Player player, String text) {
        if (text == null) {
            return "";
        }
        if (placeholders != null) {
            text = placeholders.replace(player, text);
        }
        return expandPapi(player, text);
    }

    private String expandPapi(Player player, String text) {
        if (!config.getConfig().getBoolean("placeholders.placeholderapi", true)) {
            return text;
        }
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) papi.getMethod("setPlaceholders", Player.class, String.class).invoke(null, player, text);
        } catch (Exception e) {
            plugin.getLogger().warning("PlaceholderAPI expansion failed: " + e.getMessage());
            return text;
        }
    }

    private String translate(String text) {
        if (text == null) {
            return "";
        }
        if (!config.getConfig().getBoolean("colors.enabled", true)) {
            return text;
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }
}