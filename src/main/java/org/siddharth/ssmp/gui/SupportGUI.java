package org.siddharth.ssmp.gui;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupportGUI {
    private final JavaPlugin plugin;
    public SupportGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create SSMP documentation folder.");
            return;
        }

        extractResource("quickStartGuide.pdf"); // make sure that ts is in resources
        extractResource("quickstart.md"); // and this too
    }

    private void extractResource(String name) {
        File file = new File(plugin.getDataFolder(), name);

        if (file.exists()) return;

        try (InputStream input = plugin.getResource(name)) {
            if (input == null) {
                plugin.getLogger().warning("Missing support resource: " + name);
                return;
            }

            Files.copy(input, file.toPath());
            plugin.getLogger().info("Successfully extracted " + name);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to extract " + name + " due to exception " + e.getMessage());
        }
    }

    public void openQuickstart(Player player) {
        File file = new File(plugin.getDataFolder(), "quickstart.md");
        if (!file.exists()) {
            player.sendMessage(ChatColor.RED + "The quickstart guide could not be found.");
            return;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            openBook(player, "SSMP Quickstart", content);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read quickstart guide: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Could not open the quickstart guide.");
        }
    }

    public void openBook(Player player, String title, String content) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {return;}
        meta.setTitle(title.length() > 32 ? title.substring(0, 32) : title);
        meta.setAuthor("Siddy");

        List<String> pages = createPages(content);
        if (pages.isEmpty()) {
            pages.add(ChatColor.GRAY + "Documentation has no content... Maybe the load failed.");
        }
        meta.setPages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    private List<String> createPages(String content) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String originalLine : lines) {
            String line = formatMarkdown(originalLine);
            if (page.length() + line.length() + 1 > 240) {
                if (!page.isEmpty()) {
                    pages.add(page.toString());
                    page.setLength(0);
                }
            }

            if (line.length() > 240) {
                int position = 0;
                while (position < line.length()) {
                    int end = Math.min(position + 240, line.length());
                    pages.add(line.substring(position, end));
                    position = end;
                }

                continue;
            }

            if (!page.isEmpty()) page.append('\n');
            page.append(line);
        }

        if (!page.isEmpty()) {
            pages.add(page.toString());
        }

        return pages;
    }

    private String formatMarkdown(String line) {
        String text = line;

        if (text.startsWith("### ")) {
            text = ChatColor.AQUA + "" + ChatColor.BOLD + text.substring(4);
        } else if (text.startsWith("## ")) {
            text = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + text.substring(3);
        } else if (text.startsWith("# ")) {
            text = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + text.substring(2);
        }

        if (text.startsWith("- ") || text.startsWith("* ")) {
            text = "• " + text.substring(2);
        }

        Pattern boldPattern = Pattern.compile("\\*\\*(.+?)\\*\\*");
        Matcher boldMatcher = boldPattern.matcher(text);
        text = boldMatcher.replaceAll(ChatColor.BOLD + "$1" + ChatColor.RESET);

        Pattern italicPattern = Pattern.compile("\\*(.+?)\\*");
        Matcher italicMatcher = italicPattern.matcher(text);
        text = italicMatcher.replaceAll(ChatColor.ITALIC + "$1" + ChatColor.RESET);

        Pattern codePattern = Pattern.compile("`(.+?)`");
        Matcher codeMatcher = codePattern.matcher(text);
        text = codeMatcher.replaceAll(ChatColor.GRAY + "$1" + ChatColor.RESET);

        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
