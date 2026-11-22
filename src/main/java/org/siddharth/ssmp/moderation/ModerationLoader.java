
package org.siddharth.ssmp.moderation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModerationLoader {

    public static Map<String, String> load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "moderation-terms.yml");

        if (!file.exists()) {
            plugin.saveResource("moderation-terms.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("terms");

        Map<String, String> terms = new LinkedHashMap<>();

        if (section == null) return terms;

        for (String category : section.getKeys(false)) {
            for (String term : section.getStringList(category)) {
                if (term != null && !term.isBlank()) {
                    terms.put(term, category);
                }
            }
        }

        return terms;
    }
}
