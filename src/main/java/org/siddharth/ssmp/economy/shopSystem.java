package org.siddharth.ssmp.economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.siddharth.ssmp.mechanics.soundSystem;
import org.siddharth.ssmp.ssmp;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.nio.file.Files;

public class shopSystem implements CommandExecutor, Listener {
    private static final String SHOP_TITLE = "§8SSMP §7» §6Shop";
    private static final String CATEGORY_TITLE = "§8SSMP §7» §6";
    private static final int[] CONTENT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private final File shopFolder;
    private final File itemsFile;
    private final YamlConfiguration itemsData;
    private final economy economy;
    private final soundSystem sounds;

    private class shopHolder implements InventoryHolder {
        private final String categoryId;
        private final int page;
        private final Map<Integer, String> entries = new LinkedHashMap<>();
        private Inventory inventory;

        private shopHolder(String categoryId, int page) {
            this.categoryId = categoryId;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public shopSystem(economy economy, ssmp plugin, soundSystem sounds) {
        this.economy = economy;
        this.sounds = sounds;
        shopFolder = new File(plugin.getDataFolder(), "shop");
        itemsFile = new File(shopFolder, "items.yml");

        if (!shopFolder.exists() && !shopFolder.mkdirs()) {
            throw new IllegalStateException("Shop folder could not be created!");
        }

        if (!itemsFile.exists()) {
            try (InputStream resource = plugin.getResource("shop-items.yml")) {
                if (resource == null) {
                    throw new IllegalStateException("shop-items.yml is missing from the plugin resources!");
                }

                Files.copy(resource, itemsFile.toPath());
                plugin.getLogger().info("Default shop configuration has been installed.");
            } catch (IOException e) {
                throw new IllegalStateException("Shop configuration could not be created!", e);
            }
        }

        itemsData = YamlConfiguration.loadConfiguration(itemsFile);
    }

    private void saveData() {
        try {
            itemsData.save(itemsFile);
        } catch (IOException e) {
            throw new IllegalStateException("Shop config could not be saved, maybe the directory is broken?", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Shop can be opened only by players.");
            return true;
        }

        openShop(player);
        return true;
    }

    public void openShop(Player player) {
        openShop(player, 0);
    }

    private void openShop(Player player, int page) {
        ConfigurationSection categories = itemsData.getConfigurationSection("categories");

        if (categories == null || categories.getKeys(false).isEmpty()) {
            player.sendMessage("Shop is empty...");
            play(player, "shop-error");
            return;
        }

        List<String> ids = new ArrayList<>(categories.getKeys(false));
        openMenu(player, null, ids, page, SHOP_TITLE);
    }

    public void openCategory(Player player, String categoryId) {
        openCategory(player, categoryId, 0);
    }

    private void openCategory(Player player, String categoryId, int page) {
        String path = "categories." + categoryId;
        if (!itemsData.isConfigurationSection(path)) {
            player.sendMessage("That category does not exist, maybe you typed it wrong? : "+categoryId);
            play(player, "shop-error");
            return;
        }

        ConfigurationSection items = itemsData.getConfigurationSection("items");
        List<String> ids = new ArrayList<>();

        if (items != null) {
            for (String id : items.getKeys(false)) {
                if (categoryId.equalsIgnoreCase(itemsData.getString("items." + id + ".category", "")) && getMaterial(id) != null && validPrice("items." + id + ".buy") && validPrice("items." + id + ".sell")) {
                    ids.add(id);
                }
            }
        }

        if (ids.isEmpty()) {
            player.sendMessage("Category is empty");
            play(player, "shop-error");
            return;
        }

        String name = itemsData.getString(path + ".name", categoryId);
        openMenu(player, categoryId, ids, page, CATEGORY_TITLE + name);
    }
    private void openMenu(Player player, String categoryId, List<String> ids, int page, String title) {
        int maxPages = Math.max(1, (int) Math.ceil(ids.size() / (double) CONTENT_SLOTS.length));
        page = Math.max(0, Math.min(page, maxPages - 1));

        shopHolder holder = new shopHolder(categoryId, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inventory;

        fillBackground(inventory);
        addPlayerInfo(inventory, player);

        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, ids.size());

        for (int i = start; i < end; i++) {
            String id = ids.get(i);
            int slot = CONTENT_SLOTS[i - start];
            ItemStack item = categoryId == null ? createCategoryItem(id) : createShopItem(id);

            if (item != null) {
                inventory.setItem(slot, item);
                holder.entries.put(slot, id);
            }
        }

        if (categoryId != null) inventory.setItem(45, createButton(Material.ARROW, "§c§lBack", "§7Return to categories"));
        if (page > 0) inventory.setItem(48, createButton(Material.SPECTRAL_ARROW, "§e§lPrevious Page", "§7Go to page " + page));
        if (page < maxPages - 1) inventory.setItem(50, createButton(Material.SPECTRAL_ARROW, "§a§lNext Page", "§7Go to page " + (page + 2)));

        inventory.setItem(49, createButton(Material.PAPER, "§6Page §f" + (page + 1) + "§7/§f" + maxPages, "§7" + ids.size() + " entries available"));

        player.openInventory(inventory);
    }

    private ItemStack createCategoryItem(String id) {
        String path = "categories." + id;
        Material material = getMaterial(itemsData.getString(path + ".material", "CHEST"));
        if (material == null) material = Material.CHEST;

        return createButton(material, "§6" + itemsData.getString(path + ".name", id), "§7Browse this category", "§8Click to open");
    }

    private ItemStack createShopItem(String id) {
        Material material = getMaterial(id);
        if (material == null) return null;

        String path = "items." + id;
        double buy = itemsData.getDouble(path + ".buy");
        double sell = itemsData.getDouble(path + ".sell");

        return createButton(material, "§f" + id.replace('_', ' '),
                "",
                "§aBuy: $" + formatMoney(buy),
                "§cSell: $" + formatMoney(sell),
                "",
                "Left-click to buy",
                "Right-click to sell");
    }

    private void addPlayerInfo(Inventory inventory, Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta == null) return;

        meta.setOwningPlayer(player);
        meta.setDisplayName("§e" + player.getName());
        meta.setLore(List.of("", "§7Balance: §a$" + formatMoney(economy.getPlayerBalance(player))));
        head.setItemMeta(meta);
        inventory.setItem(4, head);
    }
    private void fillBackground(Inventory inventory) {
        ItemStack filler = createButton(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
            inventory.setItem(45 + i, filler);
        }

        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, filler);
            inventory.setItem(row * 9 + 8, filler);
        }
    }

    private ItemStack createButton(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getMaterial(String name) {
        if (name == null || name.isBlank()) return null;
        Material material = Material.matchMaterial(name);
        return material == null || material.isAir() ? null : material;
    }

    private boolean validPrice(String path) {
        if (!itemsData.contains(path)) return false;
        double price = itemsData.getDouble(path);
        return Double.isFinite(price) && price >= 0;
    }

    private String formatMoney(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }

    private void play(Player player, String sound) {
        if (sounds != null) sounds.play(player, sound);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof shopHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;

        int slot = event.getRawSlot();

        if (holder.categoryId != null && slot == 45) {
            openShop(player);
            return;
        }

        if (slot == 48 && holder.page > 0) {
            if (holder.categoryId == null) openShop(player, holder.page - 1);
            else openCategory(player, holder.categoryId, holder.page - 1);
            return;
        }

        if (slot == 53) {
            if (holder.categoryId == null) openShop(player, holder.page + 1);
            else openCategory(player, holder.categoryId, holder.page + 1);
            return;
        }

        String id = holder.entries.get(slot);
        if (id == null) return;

        if (holder.categoryId == null) {
            openCategory(player, id);
            play(player, "shop-success");
            return;
        }

        if (event.getClick() == ClickType.LEFT) buyItem(player, id);
        else if (event.getClick() == ClickType.RIGHT) sellItem(player, id);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof shopHolder) event.setCancelled(true);
    }

    private void buyItem(Player player, String id) {
        String path = "items." + id;
        Material material = getMaterial(id);

        if (material == null || !validPrice(path + ".buy")) {
            player.sendMessage("§cThis item is currently unavailable.");
            play(player, "shop-error");
            return;
        }

        double price = itemsData.getDouble(path + ".buy");
        double balance = economy.getPlayerBalance(player);

        if (!Double.isFinite(balance) || balance < price) {
            player.sendMessage("§cYou don't have that much money in hand for that");
            play(player, "shop-error");
            return;
        }

        ItemStack reward = new ItemStack(material);
        if (!hasSpace(player, reward)) {
            player.sendMessage("§cYou cannot buy that item, your inventory is full.");
            play(player, "shop-error");
            return;
        }

        ItemStack[] previousContents = player.getInventory().getStorageContents();
        economy.removePlayerBalance(player, price);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(reward);

        if (!leftovers.isEmpty()) {
            player.getInventory().setStorageContents(previousContents);
            economy.addPlayerBalance(player, price);
            player.sendMessage("§cThe purchase was unsuccessful, so your money was refunded!");
            play(player, "shop-error");
            return;
        }

        player.sendMessage("§aPurchased §f" + id.replace('_', ' ') + " §afor §f$" + formatMoney(price) + "§a.");
        play(player, "shop-success");
    }

    private void sellItem(Player player, String id) {
        String path = "items." + id;
        Material material = getMaterial(id);

        if (material == null || !validPrice(path + ".sell")) {
            player.sendMessage("§cThis item cannot be sold.");
            play(player, "shop-error");
            return;
        }

        double price = itemsData.getDouble(path + ".sell");
        ItemStack item = new ItemStack(material);
        Map<Integer, ItemStack> leftovers = player.getInventory().removeItem(item);

        if (!leftovers.isEmpty()) {
            player.sendMessage("§cYou don't have that item.");
            play(player, "shop-error");
            return;
        }

        economy.addPlayerBalance(player, price);
        player.sendMessage("§aSold §f1 " + id.replace('_', ' ') + " §afor §f$" + formatMoney(price) + "§a.");
        play(player, "shop-success");
    }

    private boolean hasSpace(Player player, ItemStack item) {
        for (ItemStack contents : player.getInventory().getStorageContents()) {
            if (contents == null || contents.getType().isAir()) return true;
            if (contents.isSimilar(item) && contents.getAmount() < contents.getMaxStackSize()) return true;
        }
        return false;
    }
}