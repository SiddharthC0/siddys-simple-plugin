package org.siddharth.ssmp.gui;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.siddharth.ssmp.mechanics.playerInformationSystem;
import org.siddharth.ssmp.ssmp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class playerInformationGUI implements Listener {
    private final playerInformationSystem playerInfo;
    private final File folder;
    private final File configFile;
    private final YamlConfiguration config;
    private static final int PAGE_WIDTH = 114;
    public playerInformationGUI(playerInformationSystem playerInfo) {
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
        config.addDefault("gui.title", "Player Information");
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
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return;
        }

        String title = ChatColor.stripColor(getColoredString("gui.title", "Player Information"));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        meta.setTitle(title);
        meta.setAuthor(config.getString("gui.author", "Siddy's Simple Plugin"));
        List<BaseComponent[]> pages = new ArrayList<>();
        pages.add(createPlayerPage(target));
        pages.add(createLinksPage());
        for (BaseComponent[] page : pages) {
            meta.spigot().addPage(page);
        }

        book.setItemMeta(meta);
        viewer.openBook(book);
    }

    private BaseComponent[] createPlayerPage(Player target) {
        List<BaseComponent> components = new ArrayList<>();
        addCenteredLine(components, getColor("colors.heading", "&6") + "&lPLAYER INFORMATION");
        addCenteredLine(components, "");
        addCenteredLine(components, getColor("colors.value", "&f") + "&l" + target.getName());
        addCenteredLine(components, "");
        if (playerInfo.hasEconomy()) {
            addCenteredLine(components, getColor("colors.highlight", "&e") + "&l" + playerInfo.getFormattedBalance(target));
        }

        ssmp plugin = (ssmp) playerInfo.getPlugin();
        if (plugin.getRanks() != null) {
            addCenteredLine(components, getColor("colors.heading", "&6") + plugin.getRanks().getPlayerRankDisplayName(target));
        }

        if (playerInfo.hasBounties()) {
            addCenteredLine(components, "");
            addCenteredLine(components, getColor("colors.label", "&7") + "Bounty: " + getColor("colors.highlight", "&e") + playerInfo.getBounty(target));
        }

        if (playerInfo.hasContracts()) {
            addCenteredLine(components, "");
            addCenteredLine(components, getColor("colors.label", "&7") + "Contracts: " + getColor("colors.value", "&f") +
                    playerInfo.getCompletedContracts(target) + " / " + playerInfo.getAcceptedContracts(target));
        }

        if (playerInfo.hasLevels()) {
            addCenteredLine(components, "");
            addCenteredLine(components, getColor("colors.label", "&7") + "Level: " + getColor("colors.highlight", "&e") + playerInfo.getLevel(target));
            addCenteredLine(components, getColor("colors.label", "&7") + "Experience: " + getColor("colors.value", "&f") +
                            playerInfo.getXP(target));
        }
        return components.toArray(new BaseComponent[0]);
    }

    private BaseComponent[] createLinksPage() {
        List<BaseComponent> components = new ArrayList<>();
        addCenteredLine(components, getColor("colors.heading", "&6") + "&lSSMP FEATURES");
        addCenteredLine(components, "");

        if (playerInfo.hasBounties()) {
            addCenteredLine(components, getColor("colors.heading", "&6") + "&lBOUNTIES");
            addCommandLink(components, "CLICK HERE", "/bounty");
            addCenteredLine(components, "");
        }

        if (playerInfo.hasContracts()) {
            addCenteredLine(components, getColor("colors.heading", "&6") + "&lCONTRACTS");
            addCommandLink(components, "CLICK HERE", "/contracts");
        }

        return components.toArray(new BaseComponent[0]);
    }

    private void addCommandLink(List<BaseComponent> components, String text, String command) {
        TextComponent link = new TextComponent("[ " + text + " ]");
        link.setColor(ChatColor.AQUA);
        link.setUnderlined(true);
        link.setBold(true);
        link.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, command));

        int width = getPixelWidth(link.getText());
        int padding = Math.max(0, (PAGE_WIDTH - width) / 8);

        components.add(new TextComponent(" ".repeat(padding)));
        components.add(link);
        components.add(new TextComponent("\n"));
    }

    // Let me tell you that this works AWFULLY so pls fix it :L
    private void addCenteredLine(List<BaseComponent> components, String text) {
        String formatted = ChatColor.translateAlternateColorCodes('&', text);
        String plain = ChatColor.stripColor(formatted);
        int width = getPixelWidth(plain);
        int padding = Math.max(0, (PAGE_WIDTH - width) / 8);

        components.add(new TextComponent(" ".repeat(padding)));
        components.addAll(List.of(TextComponent.fromLegacyText(formatted)));
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
