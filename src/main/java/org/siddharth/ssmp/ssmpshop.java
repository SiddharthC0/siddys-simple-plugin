/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ssmpshop implements CommandExecutor, Listener {

    private final File shopFolder;
    private final File itemsFile;
    private final YamlConfiguration itemsData;
    private final ssmpeconomy economy;
    private final ssmpsoundsystem sounds;

    private static final String SHOP_TITLE = "SSMP | Shop";
    private static final String CATEGORY_TITLE = "SSMP | Category | ";

    public ssmpshop(ssmpeconomy economy, ssmp plugin, ssmpsoundsystem sounds) {
        this.economy = economy;
        this.sounds = sounds;

        shopFolder = new File(plugin.getDataFolder(), "shop");
        itemsFile = new File(shopFolder, "items.yml");

        if (!shopFolder.exists()) {
            shopFolder.mkdirs();
        }

        if (!itemsFile.exists()) {
            try {
                itemsFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        itemsData = YamlConfiguration.loadConfiguration(itemsFile);

        if (!itemsData.contains("categories")) {
            itemsData.set("categories.ores.name", "Ores & Minerals");
            itemsData.set("categories.ores.material", "DIAMOND");

            itemsData.set("categories.farming.name", "Farming");
            itemsData.set("categories.farming.material", "WHEAT");

            itemsData.set("categories.building.name", "Building Blocks");
            itemsData.set("categories.building.material", "STONE");

            itemsData.set("items.DIAMOND.category", "ores");
            itemsData.set("items.DIAMOND.buy", 100);
            itemsData.set("items.DIAMOND.sell", 50);

            itemsData.set("items.IRON_INGOT.category", "ores");
            itemsData.set("items.IRON_INGOT.buy", 25);
            itemsData.set("items.IRON_INGOT.sell", 10);

            itemsData.set("items.GOLD_INGOT.category", "ores");
            itemsData.set("items.GOLD_INGOT.buy", 40);
            itemsData.set("items.GOLD_INGOT.sell", 20);

            itemsData.set("items.WHEAT.category", "farming");
            itemsData.set("items.WHEAT.buy", 10);
            itemsData.set("items.WHEAT.sell", 5);

            itemsData.set("items.STONE.category", "building");
            itemsData.set("items.STONE.buy", 5);
            itemsData.set("items.STONE.sell", 2);

            try {
                itemsData.save(itemsFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("[SORRY] This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("shop")) {
            openShop(player);
        }

        return true;
    }

    public void openShop(Player player) {

        ConfigurationSection categories =
                itemsData.getConfigurationSection("categories");

        if (categories == null || categories.getKeys(false).isEmpty()) {
            player.sendMessage("§cThe shop has no categories.");
            sounds.play(player, "shop-error");
            return;
        }

        Inventory inventory =
                Bukkit.createInventory(null, 54, SHOP_TITLE);

        fillBackground(inventory);
        addPlayerInfo(inventory, player);

        List<String> categoryIds =
                new ArrayList<>(categories.getKeys(false));

        List<Integer> slots =
                getCenteredSlots(categoryIds.size());

        for (int i = 0; i < categoryIds.size() && i < slots.size(); i++) {

            String categoryId = categoryIds.get(i);

            String name = itemsData.getString(
                    "categories." + categoryId + ".name",
                    categoryId
            );

            String materialName = itemsData.getString(
                    "categories." + categoryId + ".material",
                    "CHEST"
            );

            Material material =
                    Material.matchMaterial(materialName);

            if (material == null) {
                material = Material.CHEST;
            }

            ItemStack item =
                    new ItemStack(material);

            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName("§6" + name);

                List<String> lore =
                        new ArrayList<>();

                lore.add("");
                lore.add("§7Click to browse");

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(
                    slots.get(i),
                    item
            );
        }

        player.openInventory(inventory);
    }

    public void openCategory(Player player, String categoryId) {

        ConfigurationSection items =
                itemsData.getConfigurationSection("items");

        if (items == null) {
            player.sendMessage(
                    "§cThis category has no items."
            );
            sounds.play(player, "shop-error");
            return;
        }

        List<String> categoryItems =
                new ArrayList<>();

        for (String itemName : items.getKeys(false)) {

            String itemCategory =
                    itemsData.getString(
                            "items." + itemName + ".category"
                    );

            if (categoryId.equalsIgnoreCase(itemCategory)) {
                categoryItems.add(itemName);
            }
        }

        if (categoryItems.isEmpty()) {
            player.sendMessage(
                    "§cThis category has no items."
            );
            sounds.play(player, "shop-error");
            return;
        }

        String categoryName =
                itemsData.getString(
                        "categories." + categoryId + ".name",
                        categoryId
                );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        CATEGORY_TITLE + categoryName
                );

        fillBackground(inventory);
        addPlayerInfo(inventory, player);
        addBackButton(inventory);

        List<Integer> slots =
                getCenteredSlots(categoryItems.size());

        for (int i = 0;
             i < categoryItems.size() && i < slots.size();
             i++) {

            String itemName =
                    categoryItems.get(i);

            Material material =
                    Material.matchMaterial(itemName);

            if (material == null) {
                continue;
            }

            int buyPrice =
                    itemsData.getInt(
                            "items." + itemName + ".buy"
                    );

            int sellPrice =
                    itemsData.getInt(
                            "items." + itemName + ".sell"
                    );

            ItemStack item =
                    new ItemStack(material);

            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {

                meta.setDisplayName(
                        "§f" + itemName
                );

                List<String> lore =
                        new ArrayList<>();

                lore.add("");
                lore.add(
                        "§aBuy: §f$" + buyPrice
                );
                lore.add(
                        "§cSell: §f$" + sellPrice
                );
                lore.add("");
                lore.add(
                        "§7Left click §fto buy"
                );
                lore.add(
                        "§7Right click §fto sell"
                );

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(
                    slots.get(i),
                    item
            );
        }

        player.openInventory(inventory);
    }

    private void addPlayerInfo(
            Inventory inventory,
            Player player
    ) {

        ItemStack head =
                new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta =
                (SkullMeta) head.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.setOwningPlayer(player);

        meta.setDisplayName(
                "§e" + player.getName()
        );

        List<String> lore =
                new ArrayList<>();

        lore.add("");
        lore.add(
                "§7Balance: §a$" +
                        economy.getPlayerBalance(player)
        );

        meta.setLore(lore);

        head.setItemMeta(meta);

        inventory.setItem(0, head);
    }

    private void fillBackground(
            Inventory inventory
    ) {

        ItemStack filler =
                new ItemStack(
                        Material.BLACK_STAINED_GLASS_PANE
                );

        ItemMeta meta =
                filler.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
            inventory.setItem(45 + i, filler);
        }

        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, filler);
            inventory.setItem(row * 9 + 8, filler);
        }
    }

    private List<Integer> getCenteredSlots(
            int amount
    ) {

        List<Integer> slots =
                new ArrayList<>();

        if (amount <= 0) {
            return slots;
        }

        int remaining = amount;
        int row = 1;

        while (remaining > 0 && row <= 4) {

            int itemsThisRow =
                    Math.min(9, remaining);

            int startColumn =
                    (9 - itemsThisRow) / 2;

            for (int column = 0;
                 column < itemsThisRow;
                 column++) {

                int slot =
                        row * 9
                                + startColumn
                                + column;

                slots.add(slot);
            }

            remaining -= itemsThisRow;
            row++;
        }

        return slots;
    }
    private void addBackButton(Inventory inventory) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§c§lBack");
            meta.setLore(List.of(
                    "",
                    "§7Return to the shop"
            ));
            back.setItemMeta(meta);
        }

        inventory.setItem(45, back);
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        String title =
                event.getView().getTitle();

        if (!title.equals(SHOP_TITLE)
                && !title.startsWith(CATEGORY_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        ItemStack clickedItem =
                event.getCurrentItem();

        if (clickedItem == null
                || clickedItem.getType() == Material.AIR) {
            return;
        }
        if (event.getRawSlot() == 45) {
            openShop(player);
            return;
        }

        if (title.equals(SHOP_TITLE)) {

            ConfigurationSection categories =
                    itemsData.getConfigurationSection(
                            "categories"
                    );

            if (categories == null) {
                return;
            }

            for (String categoryId :
                    categories.getKeys(false)) {

                String materialName =
                        itemsData.getString(
                                "categories."
                                        + categoryId
                                        + ".material"
                        );

                if (materialName == null) {
                    continue;
                }

                Material material =
                        Material.matchMaterial(materialName);

                if (material == clickedItem.getType()) {

                    openCategory(
                            player,
                            categoryId
                    );

                    return;
                }
            }

            return;
        }

        String categoryName =
                title.substring(
                        CATEGORY_TITLE.length()
                );

        String categoryId = null;

        ConfigurationSection categories =
                itemsData.getConfigurationSection(
                        "categories"
                );

        if (categories != null) {

            for (String id :
                    categories.getKeys(false)) {

                String name =
                        itemsData.getString(
                                "categories."
                                        + id
                                        + ".name",
                                id
                        );

                if (name.equals(categoryName)) {
                    categoryId = id;
                    break;
                }
            }
        }

        if (categoryId == null) {
            return;
        }

        String itemName =
                clickedItem.getType().name();

        String itemPath =
                "items." + itemName;

        if (!itemsData.contains(itemPath)) {
            return;
        }

        String itemCategory =
                itemsData.getString(
                        itemPath + ".category"
                );

        if (!categoryId.equalsIgnoreCase(itemCategory)) {
            return;
        }

        int buyPrice =
                itemsData.getInt(
                        itemPath + ".buy"
                );

        int sellPrice =
                itemsData.getInt(
                        itemPath + ".sell"
                );

        if (event.isLeftClick()) {

            if (economy.getPlayerBalance(player)
                    < buyPrice) {

                player.sendMessage(
                        "§cYou cannot afford this item."
                );
                sounds.play(player, "shop-error");


                return;
            }

            economy.removePlayerBalance(
                    player,
                    buyPrice
            );

            player.getInventory().addItem(
                    new ItemStack(
                            clickedItem.getType(),
                            1
                    )
            );

            player.sendMessage(
                    "§aBought 1 "
                            + itemName
                            + " for $"
                            + buyPrice
            );
            sounds.play(player, "shop-success");

        } else if (event.isRightClick()) {

            if (!player.getInventory().contains(
                    clickedItem.getType())) {

                player.sendMessage(
                        "§cYou don't have this item."
                );
                sounds.play(player, "shop-error");

                return;
            }

            player.getInventory().removeItem(
                    new ItemStack(
                            clickedItem.getType(),
                            1
                    )
            );

            economy.addPlayerBalance(
                    player,
                    sellPrice
            );

            player.sendMessage(
                    "§aSold 1 "
                            + itemName
                            + " for $"
                            + sellPrice
            );
            sounds.play(player, "shop-success");
        }
    }
}