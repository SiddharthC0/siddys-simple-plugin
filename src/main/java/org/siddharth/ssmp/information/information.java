// This is the second file you can configure to change the name and all of the plugin. be sure to configure this if you forked
// Its name does NOT match the name in SSMP.java because it includes special feilds
package org.siddharth.ssmp.information;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
public class information {
    private final JavaPlugin plugin;
    private final Path manifest;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public information(JavaPlugin plugin) {
        this.plugin = plugin;
        this.manifest = plugin.getDataFolder().toPath().resolve("ssmp.manifest");
    }

    public void create() {
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
            JsonObject data;
            if (Files.exists(manifest)) {
                try {
                    data = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
                } catch (Exception e) {
                    plugin.getLogger().warning("SSMP manifest is invalid. Existing file was preserved.");
                    return;
                }
            } else {
                data = new JsonObject();
            }
            data.addProperty("plugin", "Siddy's Simple Plugin"); // config this for plugin name (full)
            data.addProperty("developer", "Siddharth C"); // config this for author name (full)
            data.addProperty("version", plugin.getDescription().getVersion()); // do not configure, auto fetch
            data.addProperty("main", plugin.getDescription().getMain());
            data.addProperty("api-version", plugin.getDescription().getAPIVersion());
            data.addProperty("repository", "https://github.com/SiddharthC0/siddys-simple-plugin"); // repo (if present)
            data.addProperty("issue-tracker", "https://github.com/SiddharthC0/siddys-simple-plugin/issues"); // issue tracker
            data.addProperty("modrinth-project-id", "mIr78G7Z"); // modrinth proj id (if any)
            data.addProperty("last-refreshed", java.time.Instant.now().toString());

            JsonObject modules = discoverClasses();
            data.add("modules", modules);
            if (!data.has("notes")) {
                data.add("notes", new JsonArray());
            }
            if (!data.has("release-history")) {
                data.add("release-history", new JsonArray());
            }

            Files.writeString(manifest, gson.toJson(data));
            plugin.getLogger().info("SSMP manifest refreshed.");
        } catch (Exception e) {
            plugin.getLogger().warning("SSMP Manifest failed to create due to exception : " + e.getMessage());
        }
    }

    private JsonObject discoverClasses() throws Exception {
        Map<String, TreeSet<String>> packages = new TreeMap<>();
        String root = "org/siddharth/ssmp/";
        Path source = Path.of(information.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(source)) {
            try (var paths = Files.walk(source.resolve(root))) {
                paths.filter(path -> path.toString().endsWith(".class")).forEach(path -> addClass(packages, root, source.relativize(path).toString().replace('\\', '/')));
            }
        } else {
            try (JarFile jar = new JarFile(source.toFile())) {
                jar.stream().filter(entry -> entry.getName().startsWith(root)).filter(entry -> entry.getName().endsWith(".class")).forEach(entry -> addClass(packages, root, entry.getName()));
            }
        }

        JsonObject result = new JsonObject();
        for (Map.Entry<String, TreeSet<String>> entry : packages.entrySet()) {
            JsonArray classes = new JsonArray();
            entry.getValue().forEach(classes::add);
            result.add(entry.getKey(), classes);
        }

        return result;
    }

    private void addClass(Map<String, TreeSet<String>> packages, String root, String path) {
        if (!path.startsWith(root) || path.contains("$")) {
            return;
        }

        String className = path.substring(root.length(), path.length() - 6);
        if (className.equals("module-info") || className.equals("package-info")) {
            return;
        }

        int separator = className.lastIndexOf('/');
        String packageName = separator == -1 ? "root" : className.substring(0, separator).replace('/', '.');
        String simpleName = className.substring(separator + 1);
        packages.computeIfAbsent(packageName, key -> new TreeSet<>()).add(simpleName);
    }

    private JsonObject createTask(String task, boolean completed) {
        JsonObject object = new JsonObject();
        object.addProperty("task", task);
        object.addProperty("completed", completed);
        return object;
    }
}
