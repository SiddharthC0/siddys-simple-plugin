package org.siddharth.ssmp.information;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.siddharth.ssmp.gui.supportGUI;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public final class supportMain implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final supportGUI gui;
    private final Map<String, supportTopic> topics = new LinkedHashMap<>();

    // Because i care about you, i added msgs right here :D
    private String message_sendSupportMessage = "&cNeed assistance? Use /ssmp help to open the help menu";
    private String message_sendSupportLocation = "&cSupport is available through /ssmp support or /support";
    private String message_invalidUsage = "&cYou used the command wrong! Use it like this: /ssmp <name> [subname]";
    public supportMain(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gui = new supportGUI(plugin);
        gui.initialize();
        loadTopics();
    }

    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(message)));
    }

    public void sendSupportMessage(CommandSender sender) {
        sender.sendMessage(color(message_sendSupportMessage));
    }
    public void sendSupportLocation(CommandSender sender) {
        sender.sendMessage(color(message_sendSupportLocation));
    }
    public void openQuickstart(Player player) {
        gui.openQuickstart(player);
    }
    public void openSupportBook(Player player, String title, String content) {
        gui.openBook(player, title, content);
    }
    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    private void loadTopics() {
        topics.clear();
        File file = new File(plugin.getDataFolder(), "support.md");
        try {
            if (!file.exists()) {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                plugin.saveResource("support.md", false);
            }

            if (!file.exists()) {return;}
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            supportTopic currentTopic = null;
            supportSection currentSection = null;
            for (String line : lines) {
                if (line.startsWith("# ")) {
                    String name = line.substring(2).trim().toLowerCase(Locale.ROOT);
                    currentTopic = new supportTopic(name);
                    topics.put(name, currentTopic);
                    currentSection = null;
                    continue;
                }

                if (line.startsWith("## ") && currentTopic != null) {
                    String name = line.substring(3).trim().toLowerCase(Locale.ROOT);
                    currentSection = new supportSection(name);
                    currentTopic.sections.put(name, currentSection);
                    continue;
                }

                if (currentTopic == null) {continue;}
                if (currentSection != null) {
                    currentSection.content.append(line).append('\n');
                } else {
                    currentTopic.content.append(line).append('\n');
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load support documentation: " + e.getMessage());
        }
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The support command requires a player.");
            return true;
        } else if (args.length == 0) {
            openQuickstart(player);
            return true;
        } else if (args.length > 2) {
            player.sendMessage(color(message_invalidUsage));
            return true;
        }

        String topicName = args[0].toLowerCase(Locale.ROOT);
        supportTopic topic = topics.get(topicName);
        if (topic == null) {
            player.sendMessage(color("&cUnknown support topic: &f" + topicName));
            player.sendMessage(color("&7Use tab completion to see available topics."));
            return true;
        }

        if (args.length == 1) {
            StringBuilder content = new StringBuilder(topic.content);

            if (!topic.sections.isEmpty()) {
                content.append("\n&bAvailable sections:\n");
                for (String section : topic.sections.keySet()) {
                    content.append("&7- &f").append(section).append('\n');
                }
            }

            openSupportBook(player, topic.name, content.toString());
            return true;
        }

        String sectionName = args[1].toLowerCase(Locale.ROOT);
        supportSection section = topic.sections.get(sectionName);
        if (section == null) {
            player.sendMessage(color("&cUnknown subsection: &f" + sectionName));
            return true;
        }

        openSupportBook(player, topic.name + " - " + section.name, section.content.toString());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return handle(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (String topic : topics.keySet()) {
                if (topic.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    suggestions.add(topic);
                }
            }
        } else if (args.length == 2) {
            supportTopic topic = topics.get(args[0].toLowerCase(Locale.ROOT));
            if (topic != null) {
                for (String section : topic.sections.keySet()) {
                    if (section.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        suggestions.add(section);
                    }
                }
            }
        }
        return suggestions;
    }

    private static final class supportTopic {
        private final String name;
        private final StringBuilder content = new StringBuilder();
        private final Map<String, supportSection> sections = new LinkedHashMap<>();
        private supportTopic(String name) {
            this.name = name;
        }
    }

    private static final class supportSection {
        private final String name;
        private final StringBuilder content = new StringBuilder();
        private supportSection(String name) {
            this.name = name;
        }
    }
}
