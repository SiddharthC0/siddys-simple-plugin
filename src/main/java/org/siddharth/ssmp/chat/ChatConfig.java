package org.siddharth.ssmp.chat;
import org.bukkit.configuration.file.YamlConfiguration;
import org.siddharth.ssmp.SiddysSimplePlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;
public class ChatConfig {
    private final SiddysSimplePlugin plugin;
    private final File folder;
    private File configFile;
    private File formattingFile;
    private File hoverFile;
    private File deliveryFile;
    private YamlConfiguration config;
    private YamlConfiguration formatting;
    private YamlConfiguration hover;
    private YamlConfiguration delivery;

    public ChatConfig(SiddysSimplePlugin plugin) {
        this.plugin = plugin;
        folder = new File(plugin.getDataFolder(), "chatformatting");
        initialize();
    }

    private void initialize() {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Could not create chat formatting directory.");
        }
        configFile = new File(folder, "config.yml");
        createFile(configFile);
        formattingFile = new File(folder, "formatting.yml");
        createFile(formattingFile);
        hoverFile = new File(folder, "hover.yml");
        createFile(hoverFile);
        deliveryFile = new File(folder, "delivery.yml");
        createFile(deliveryFile);
        reload();
    }

    private void createFile(File file) {
        if (file.exists()) {
            return;
        }
        try {
            if (!file.createNewFile()) {
                throw new IOException("Could not create " + file.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        formatting = YamlConfiguration.loadConfiguration(formattingFile);
        hover = YamlConfiguration.loadConfiguration(hoverFile);
        delivery = YamlConfiguration.loadConfiguration(deliveryFile);
        boolean changed = false;
        changed |= defaults(config, new String[][]{{"enabled", "true"},
                {"debug", "false"},
                {"fallback-to-vanilla", "true"},
                {"placeholders.internal", "true"},
                {"placeholders.placeholderapi", "true"},
                {"colors.enabled", "true"}
        });

        changed |= defaults(formatting, new String[][]{
                {"format", "%ssmp_player_prefix%%player%%ssmp_player_suffix% &8» &f%message%"},
                {"fallback.player", "%realname%"},
                {"fallback.prefix", ""},
                {"fallback.suffix", ""},
                {"message.translate-colors", "false"}
        });

        changed |= defaults(hover, new String[][]{
                {"enabled", "true"},
                {"player.enabled", "true"},
                {"player.lines", "&d%player%;&7Real username: &f%realname%;&7Rank: &f%ssmp_player_rank_name%"},
                {"click.enabled", "false"},
                {"click.action", "SUGGEST_COMMAND"},
                {"click.value", "/msg %realname% "}
        });

        changed |= defaults(delivery, new String[][]{
                {"mode", "custom"},
                {"include-sender", "true"},
                {"respect-recipients", "true"},
                {"skip-disconnected", "true"},
                {"sounds.enabled", "true"},
                {"sounds.name", "message"},
                {"sounds.exclude-sender", "false"}
        });

        if (changed) {
            saveAll();
        }
    }

    private boolean defaults(YamlConfiguration yaml, String[][] values) {
        boolean changed = false;
        for (String[] entry : values) {
            if (yaml.contains(entry[0])) continue;
            String value = entry[1];

            if (value.equals("true") || value.equals("false")) {
                yaml.set(entry[0], Boolean.parseBoolean(value));
            } else if (value.contains(";") && entry[0].endsWith(".lines")) {
                yaml.set(entry[0], List.of(value.split(";")));
            } else {
                yaml.set(entry[0], value);
            }
            changed = true;
        }
        return changed;
    }

    public void saveAll() {
        save(formatting, formattingFile);
        save(config, configFile);
        save(delivery, deliveryFile);
        save(hover, hoverFile);
    }

    private void save(YamlConfiguration yaml, File file) {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    public YamlConfiguration getConfig() {return config;}
    public YamlConfiguration getFormatting() {return formatting;}
    public YamlConfiguration getHover() {return hover;}
    public YamlConfiguration getDelivery() {return delivery;}
}