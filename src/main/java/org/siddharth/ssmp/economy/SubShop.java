
package org.siddharth.ssmp.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.siddharth.ssmp.gui.ShopSystemGUI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubShop implements Listener {

    private static final int INVENTORY_SIZE = 27;
    private static final int BACK_SLOT = 18;

    private static final int[] LAYOUT_1 = {13};
    private static final int[] LAYOUT_2 = {12, 14};
    private static final int[] LAYOUT_3 = {11, 13, 15};
    private static final int[] LAYOUT_4 = {10, 12, 14, 16};
    private static final int[] LAYOUT_5 = {9, 11, 13, 15, 17};

    private final ShopSystem shop;
    private final ShopSystemGUI gui;

    private final Map<String, YamlConfiguration> configurations =
            new LinkedHashMap<>();

    private class subShopHolder implements InventoryHolder {
        private final String categoryPath;
        private final String parentCategoryPath;
        private final Map<Integer, String> entries = new LinkedHashMap<>();
        private Inventory inventory;

        private subShopHolder(String categoryPath) {
            this.categoryPath = categoryPath;

            int separator = categoryPath.lastIndexOf('.');
            this.parentCategoryPath = separator < 0
                    ? null
                    : categoryPath.substring(0, separator);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public SubShop(ShopSystem shop, ShopSystemGUI gui) {
        this.shop = shop;
        this.gui = gui;

        Bukkit.getPluginManager().registerEvents(this, shop.getPlugin());

        initialize();
    }

    private void initialize() {
        for (String categoryPath : shop.getCategoryPaths()) {
            if (!shop.hasSubcategories(categoryPath)) continue;

            loadSubdivision(categoryPath);
        }
    }

    private File getSubdivisionFile(String categoryPath) {
        File folder = shop.getCategoryFolder(categoryPath);

        return folder == null
                ? null
                : new File(folder, "subdivision.yml");
    }

    private void loadSubdivision(String categoryPath) {
        File file = getSubdivisionFile(categoryPath);

        if (file == null) return;

        if (!file.exists()) {
            createDefaultSubdivision(categoryPath, file);
        }

        YamlConfiguration data = new YamlConfiguration();

        try {
            data.load(file);
        } catch (IOException | InvalidConfigurationException e) {

            backupBrokenFile(file);
            createDefaultSubdivision(categoryPath, file);

            try {
                data.load(file);
            } catch (IOException | InvalidConfigurationException exception) {
                throw new IllegalStateException(
                        "Could not restore subdivision.yml: " + file.getPath(),
                        exception
                );
            }
        }

        synchronizeSubdivision(categoryPath, file, data);

        configurations.put(categoryPath, data);
    }

    private void backupBrokenFile(File file) {
        File backup = new File(
                file.getParentFile(),
                file.getName() + ".broken"
        );

        int index = 1;

        while (backup.exists()) {
            backup = new File(
                    file.getParentFile(),
                    file.getName() + ".broken." + index++
            );
        }

        try {
            java.nio.file.Files.move(file.toPath(), backup.toPath());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not back up broken subdivision configuration!",
                    e
            );
        }
    }

    private void createDefaultSubdivision(
            String categoryPath,
            File file
    ) {
        List<String> children = shop.getSubcategories(categoryPath);

        if (children.isEmpty()) return;

        YamlConfiguration data = new YamlConfiguration();

        data.set("title", shop.getCategoryName(categoryPath));
        data.set("size", INVENTORY_SIZE);

        int[] slots = getLayout(Math.min(children.size(), 5));

        for (int i = 0; i < Math.min(children.size(), 5); i++) {
            String id = children.get(i);
            String path = "items." + id;

            data.set(path + ".slot", slots[i]);
            data.set(path + ".material", "CHEST");
            data.set(path + ".name", shop.getCategoryName(
                    categoryPath + "." + id
            ));
        }

        save(data, file);
    }

    private void synchronizeSubdivision(
            String categoryPath,
            File file,
            YamlConfiguration data
    ) {
        List<String> children = shop.getSubcategories(categoryPath);
        if (!data.contains("title")) {
            data.set("title", shop.getCategoryName(categoryPath));
        }

        if (!data.contains("size")) {
            data.set("size", INVENTORY_SIZE);
        }

        ConfigurationSection items =
                data.getConfigurationSection("items");

        if (items == null) {
            items = data.createSection("items");
        }

        Set<Integer> occupied = new HashSet<>();

        for (String id : children) {
            String path = "items." + id;

            if (data.contains(path + ".slot")) {
                int slot = data.getInt(path + ".slot");

                if (slot >= 0 && slot < INVENTORY_SIZE) {
                    occupied.add(slot);
                }
            }
        }

        int[] defaults = getLayout(Math.min(children.size(), 5));

        for (int i = 0; i < Math.min(children.size(), 5); i++) {
            String id = children.get(i);
            String path = "items." + id;

            if (!data.contains(path + ".slot")) {
                int slot = findAvailableSlot(defaults[i], occupied);

                data.set(path + ".slot", slot);
                occupied.add(slot);
            }

            if (!data.contains(path + ".material")) {
                data.set(path + ".material", "CHEST");
            }

            if (!data.contains(path + ".name")) {
                data.set(
                        path + ".name",
                        shop.getCategoryName(categoryPath + "." + id)
                );
            }
        }

        save(data, file);
    }

    private int findAvailableSlot(int preferred, Set<Integer> occupied) {
        if (!occupied.contains(preferred)) return preferred;

        for (int slot = 9; slot <= 17; slot++) {
            if (!occupied.contains(slot)) return slot;
        }

        return preferred;
    }

    private int[] getLayout(int amount) {
        return switch (amount) {
            case 1 -> LAYOUT_1;
            case 2 -> LAYOUT_2;
            case 3 -> LAYOUT_3;
            case 4 -> LAYOUT_4;
            case 5 -> LAYOUT_5;
            default -> new int[0];
        };
    }

    private void save(YamlConfiguration data, File file) {
        try {
            data.save(file);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not save subdivision configuration: " +
                            file.getPath(),
                    e
            );
        }
    }

    public void reload() {
        configurations.clear();
        initialize();
    }

    public YamlConfiguration getSubdivisionData(String categoryPath) {
        return configurations.get(categoryPath);
    }

    public void open(Player player, String categoryPath) {
        if (!shop.categoryExists(categoryPath)) {
            player.sendMessage("§cThat category does not exist.");
            play(player, "shop-error");
            return;
        }

        if (!shop.hasSubcategories(categoryPath)) {
            gui.openCategory(player, categoryPath);
            return;
        }

        YamlConfiguration data = configurations.get(categoryPath);

        if (data == null) {
            loadSubdivision(categoryPath);
            data = configurations.get(categoryPath);
        }

        if (data == null) {
            player.sendMessage("§cCould not load this category.");
            play(player, "shop-error");
            return;
        }

        int size = data.getInt("size", INVENTORY_SIZE);

        if (size != 27 && size != 36 && size != 45 && size != 54) {
            size = INVENTORY_SIZE;
        }

        String title = ChatColor.translateAlternateColorCodes(
                '&',
                data.getString(
                        "title",
                        shop.getCategoryName(categoryPath)
                )
        );

        subShopHolder holder = new subShopHolder(categoryPath);

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                title
        );

        holder.inventory = inventory;

        inventory.setItem(
                BACK_SLOT,
                createButton(
                        Material.ARROW,
                        "§c§lBack",
                        "§7Return to the previous menu"
                )
        );

        ConfigurationSection items =
                data.getConfigurationSection("items");

        if (items != null) {
            for (String id : shop.getSubcategories(categoryPath)) {
                String path = "items." + id;

                int slot = data.getInt(path + ".slot", -1);

                if (slot < 0 || slot >= size || slot == BACK_SLOT) {
                    continue;
                }

                Material material = shop.getMaterial(
                        data.getString(path + ".material", "CHEST")
                );

                if (material == null) material = Material.CHEST;

                String name = data.getString(
                        path + ".name",
                        shop.getCategoryName(categoryPath + "." + id)
                );

                ItemStack item = createButton(
                        material,
                        ChatColor.translateAlternateColorCodes('&', name),
                        "§7Browse this category",
                        "§8Click to open"
                );

                inventory.setItem(slot, item);
                holder.entries.put(slot, id);
            }
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof subShopHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= top.getSize()) return;

        if (slot == BACK_SLOT) {
            if (holder.parentCategoryPath == null) {
                gui.openServerShop(player);
            } else {
                open(player, holder.parentCategoryPath);
            }
            return;
        }

        String id = holder.entries.get(slot);

        if (id == null) return;

        String childPath = holder.categoryPath + "." + id;

        if (!shop.categoryExists(childPath)) {
            player.sendMessage("§cThis category is unavailable.");
            play(player, "shop-error");
            return;
        }

        open(player, childPath);
        play(player, "shop-success");
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof subShopHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack createButton(
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    private void play(Player player, String sound) {
        if (shop.getSounds() != null) {
            shop.getSounds().play(player, sound);
        }
    }
}
