
package org.siddharth.ssmp.economy;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.mechanics.SoundSystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShopSystem implements CommandExecutor {

    private final SiddysSimplePlugin plugin;
    private final File shopFolder;
    private final File categoriesFile;
    private final Economy economy;
    private final SoundSystem sounds;

    private final YamlConfiguration categoriesData;
    private final Map<String, File> categoryFolders = new LinkedHashMap<>();
    private final Map<String, YamlConfiguration> itemConfigurations = new LinkedHashMap<>();

    public ShopSystem(Economy economy, SiddysSimplePlugin plugin, SoundSystem sounds) {
        this.plugin = plugin;
        this.economy = economy;
        this.sounds = sounds;

        shopFolder = new File(plugin.getDataFolder(), "shop");
        categoriesFile = new File(shopFolder, "categories.yml");

        createDirectory(shopFolder);

        ensureYamlFile(categoriesFile, "categories.yml", "categories:\n");

        categoriesData = loadYaml(categoriesFile);

        ConfigurationSection categories =
                categoriesData.getConfigurationSection("categories");

        if (categories == null) {
            throw new IllegalStateException(
                    "categories.yml must contain a categories section!"
            );
        }

        createCategoryDirectories(categories, "", shopFolder);
    }

    private void createDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create directory: " + directory.getPath()
            );
        }
    }

    private void ensureYamlFile(File file, String resourcePath, String fallback) {
        createDirectory(file.getParentFile());

        if (!file.exists()) {
            installResource(file, resourcePath, fallback);
            return;
        }

        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            backupBrokenFile(file);
            installResource(file, resourcePath, fallback);
        }
    }

    private void backupBrokenFile(File file) {
        File backup = new File(file.getParentFile(), file.getName() + ".broken");

        int index = 1;

        while (backup.exists()) {
            backup = new File(
                    file.getParentFile(),
                    file.getName() + ".broken." + index++
            );
        }

        try {
            Files.move(file.toPath(), backup.toPath());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not back up broken configuration: " + file.getPath(), e
            );
        }
    }

    private void installResource(File file, String resourcePath, String fallback) {
        try (InputStream resource = plugin.getResource(resourcePath)) {
            if (resource != null) {
                Files.copy(resource, file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.writeString(file.toPath(), fallback);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not install configuration: " + file.getPath(), e
            );
        }
    }

    private YamlConfiguration loadYaml(File file) {
        YamlConfiguration configuration = new YamlConfiguration();

        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException(
                    "Could not load configuration: " + file.getPath(), e
            );
        }

        return configuration;
    }

    private void createCategoryDirectories(
            ConfigurationSection categories,
            String parentPath,
            File parentFolder
    ) {
        for (String id : categories.getKeys(false)) {
            String categoryPath = parentPath.isEmpty()
                    ? id
                    : parentPath + "." + id;

            File categoryFolder = new File(parentFolder, id);

            createDirectory(categoryFolder);

            categoryFolders.put(categoryPath, categoryFolder);

            File itemsFile = new File(categoryFolder, "items.yml");

            String resourcePath = "shop-items/"
                    + categoryPath.replace('.', '/') + "/items.yml";

            ensureYamlFile(itemsFile, resourcePath, "items:\n");

            itemConfigurations.put(categoryPath, loadYaml(itemsFile));

            ConfigurationSection subcategories =
                    categories.getConfigurationSection(id + ".subcategories");

            if (subcategories != null && !subcategories.getKeys(false).isEmpty()) {
                createCategoryDirectories(
                        subcategories,
                        categoryPath,
                        categoryFolder
                );
            }
        }
    }

    public SiddysSimplePlugin getPlugin() {
        return plugin;
    }

    public Economy getEconomy() {
        return economy;
    }

    public SoundSystem getSounds() {
        return sounds;
    }

    public File getShopFolder() {
        return shopFolder;
    }

    public File getCategoriesFile() {
        return categoriesFile;
    }

    public YamlConfiguration getCategoriesData() {
        return categoriesData;
    }

    public Set<String> getCategoryPaths() {
        return Collections.unmodifiableSet(categoryFolders.keySet());
    }

    public File getCategoryFolder(String categoryPath) {
        return categoryFolders.get(categoryPath);
    }

    public boolean categoryExists(String categoryPath) {
        return categoryFolders.containsKey(categoryPath);
    }

    public String getCategoryName(String categoryPath) {
        ConfigurationSection section = getCategorySection(categoryPath);

        if (section == null) {
            return formatName(categoryPath);
        }

        return section.getString("name", formatName(categoryPath));
    }

    public Material getCategoryMaterial(String categoryPath) {
        ConfigurationSection section = getCategorySection(categoryPath);

        if (section == null) return Material.CHEST;

        Material material = getMaterial(
                section.getString("material", "CHEST")
        );

        return material == null ? Material.CHEST : material;
    }


    public ConfigurationSection getCategorySection(String categoryPath) {
        return getCategorySectionDirect(categoryPath);
    }

    private ConfigurationSection getCategorySectionDirect(String categoryPath) {
        if (categoryPath == null || categoryPath.isBlank()) return null;

        StringBuilder path = new StringBuilder("categories");

        for (String part : categoryPath.split("\\.")) {
            path.append(".").append(part).append(".subcategories");
        }

        String result = path.substring(0, path.length() - ".subcategories".length());

        return categoriesData.getConfigurationSection(result);
    }

    public List<String> getSubcategories(String categoryPath) {
        ConfigurationSection section;

        if (categoryPath == null || categoryPath.isBlank()) {
            section = categoriesData.getConfigurationSection("categories");
        } else {
            ConfigurationSection category =
                    getCategorySectionDirect(categoryPath);

            section = category == null
                    ? null
                    : category.getConfigurationSection("subcategories");
        }

        if (section == null) return new ArrayList<>();

        List<String> result = new ArrayList<>();

        for (String id : section.getKeys(false)) {
            String path = categoryPath == null || categoryPath.isBlank()
                    ? id
                    : categoryPath + "." + id;

            if (categoryFolders.containsKey(path)) {
                result.add(id);
            }
        }

        return result;
    }

    public boolean hasSubcategories(String categoryPath) {
        return !getSubcategories(categoryPath).isEmpty();
    }

    public YamlConfiguration getItemsData(String categoryPath) {
        return itemConfigurations.get(categoryPath);
    }

    public YamlConfiguration getItemsData() {
        return itemConfigurations.values().stream()
                .findFirst()
                .orElse(new YamlConfiguration());
    }

    public List<String> getItemIds(String categoryPath) {
        YamlConfiguration data = getItemsData(categoryPath);

        if (data == null) return new ArrayList<>();

        ConfigurationSection items =
                data.getConfigurationSection("items");

        if (items == null) return new ArrayList<>();

        return new ArrayList<>(items.getKeys(false));
    }

    public Material getMaterial(String name) {
        if (name == null || name.isBlank()) return null;

        Material material = Material.matchMaterial(name);

        return material == null || material.isAir() ? null : material;
    }

    public boolean validPrice(String categoryPath, String path) {
        YamlConfiguration data = getItemsData(categoryPath);

        if (data == null || !data.contains(path)) return false;

        double price = data.getDouble(path);

        return Double.isFinite(price) && price >= 0;
    }

    public boolean validPrice(String path) {
        for (String categoryPath : categoryFolders.keySet()) {
            if (validPrice(categoryPath, path)) return true;
        }

        return false;
    }

    public double getBuyPrice(String categoryPath, String id) {
        String path = "items." + id + ".buy";

        return validPrice(categoryPath, path)
                ? getItemsData(categoryPath).getDouble(path)
                : Double.NaN;
    }

    public double getSellPrice(String categoryPath, String id) {
        String path = "items." + id + ".sell";

        return validPrice(categoryPath, path)
                ? getItemsData(categoryPath).getDouble(path)
                : Double.NaN;
    }

    public double getBuyPrice(String id) {
        String category = findItemCategory(id);

        return category == null ? Double.NaN : getBuyPrice(category, id);
    }

    public double getSellPrice(String id) {
        String category = findItemCategory(id);

        return category == null ? Double.NaN : getSellPrice(category, id);
    }

    public String getItemName(String categoryPath, String id) {
        YamlConfiguration data = getItemsData(categoryPath);

        if (data != null) {
            String customName = data.getString(
                    "items." + id + ".name"
            );

            if (customName != null && !customName.isBlank()) {
                return customName;
            }
        }

        return formatName(id);
    }

    public String getItemName(String id) {
        String category = findItemCategory(id);

        return category == null
                ? formatName(id)
                : getItemName(category, id);
    }

    public String formatName(String name) {
        if (name == null || name.isBlank()) return "";

        String normalized = name.replace('_', ' ').toLowerCase();

        StringBuilder result = new StringBuilder();

        for (String word : normalized.trim().split("\\s+")) {
            if (word.isEmpty()) continue;

            if (!result.isEmpty()) result.append(' ');

            result.append(Character.toUpperCase(word.charAt(0)));

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    public String formatMoney(double amount) {
        return BigDecimal.valueOf(amount)
                .stripTrailingZeros()
                .toPlainString();
    }

    private String findItemCategory(String id) {
        if (id == null) return null;

        for (String categoryPath : categoryFolders.keySet()) {
            YamlConfiguration data = getItemsData(categoryPath);

            if (data != null &&
                    data.isConfigurationSection("items." + id)) {
                return categoryPath;
            }
        }

        return null;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command cmd,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Shop can be opened only by players.");
            return true;
        }

        player.sendMessage("§cThe shop GUI has not been connected yet.");
        return true;
    }

    public boolean buyItem(Player player, String id, int quantity) {
        String category = findItemCategory(id);

        return category != null &&
                buyItem(player, category, id, quantity);
    }

    public boolean buyItem(
            Player player,
            String categoryPath,
            String id,
            int quantity
    ) {
        if (quantity != 1 && quantity != 4 && quantity != 8 &&
                quantity != 16 && quantity != 32 &&
                quantity != 64 && quantity != 128) {
            return false;
        }

        Material material = getMaterial(id);
        double unitPrice = getBuyPrice(categoryPath, id);

        if (material == null || !Double.isFinite(unitPrice)) {
            player.sendMessage("§cThis item is currently unavailable.");
            play(player, "menu-error");
            return false;
        }

        double totalPrice = unitPrice * quantity;

        if (!Double.isFinite(totalPrice)) {
            player.sendMessage("§cThe total price is invalid.");
            play(player, "menu-error");
            return false;
        }

        double balance = economy.getPlayerBalance(player);

        if (!Double.isFinite(balance) || balance < totalPrice) {
            player.sendMessage("§cYou don't have enough money for this purchase.");
            play(player, "menu-error");
            return false;
        }

        ItemStack reward = new ItemStack(material, quantity);
        ItemStack[] previousContents =
                player.getInventory().getStorageContents();

        economy.removePlayerBalance(player, totalPrice);

        Map<Integer, ItemStack> leftovers =
                player.getInventory().addItem(reward);

        if (!leftovers.isEmpty()) {
            player.getInventory().setStorageContents(previousContents);
            economy.addPlayerBalance(player, totalPrice);

            player.sendMessage(
                    "§cThe purchase was unsuccessful. Your money has been refunded!"
            );

            play(player, "menu-error");
            return false;
        }

        player.sendMessage(
                "§aPurchased §f" + quantity + "x " +
                        getItemName(categoryPath, id) +
                        " §afor §f$" + formatMoney(totalPrice) + "§a."
        );

        play(player, "menu-success");
        return true;
    }

    public void sellItem(Player player, String id) {
        String category = findItemCategory(id);

        if (category == null) {
            player.sendMessage("§cThis item cannot be sold.");
            play(player, "menu-error");
            return;
        }

        sellItem(player, category, id);
    }

    public void sellItem(Player player, String categoryPath, String id) {
        Material material = getMaterial(id);
        double price = getSellPrice(categoryPath, id);

        if (material == null || !Double.isFinite(price)) {
            player.sendMessage("§cThis item cannot be sold.");
            play(player, "menu-error");
            return;
        }

        ItemStack item = new ItemStack(material);
        Map<Integer, ItemStack> leftovers =
                player.getInventory().removeItem(item);

        if (!leftovers.isEmpty()) {
            player.sendMessage("§cYou don't have that item.");
            play(player, "menu-error");
            return;
        }

        economy.addPlayerBalance(player, price);

        player.sendMessage(
                "§aSold §f1 " + getItemName(categoryPath, id) +
                        " §afor §f$" + formatMoney(price) + "§a."
        );

        play(player, "menu-success");
    }

    private void play(Player player, String sound) {
        if (sounds != null) {
            sounds.play(player, sound);
        }
    }
}
