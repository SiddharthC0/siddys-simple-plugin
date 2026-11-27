
package org.siddharth.ssmp.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.siddharth.ssmp.economy.ShopSystem;
import org.siddharth.ssmp.economy.SubShop;
import org.siddharth.ssmp.economy.playerShopSystem;
import org.siddharth.ssmp.economy.playerShopSystem.Listing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShopSystemGUI implements Listener {

    private static final String SELECTION_TITLE = "§8SSMP §7» §6Shops";
    private static final String SHOP_TITLE = "§8SSMP §7» §6Server Shop";
    private static final String CATEGORY_TITLE = "§8SSMP §7» §6";
    private static final String MARKETPLACE_TITLE = "§8SSMP §7» §dPlayer Marketplace";
    private static final String MANAGE_TITLE = "§8SSMP §7» §6My Shops";
    private static final String DETAILS_TITLE = "§8SSMP §7» §6Shop Management";

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int[] QUANTITIES = {1, 4, 8, 16, 32, 64, 128};

    private final ShopSystem shop;
    private final SubShop subShop;
    private playerShopSystem playerShops;

    private class shopHolder implements InventoryHolder {
        private final String mode;
        private final String categoryId;
        private final String listingId;
        private final int page;
        private final Map<Integer, String> entries = new LinkedHashMap<>();
        private Inventory inventory;

        private shopHolder(String mode, String categoryId, String listingId, int page) {
            this.mode = mode;
            this.categoryId = categoryId;
            this.listingId = listingId;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public ShopSystemGUI(ShopSystem shop) {
        this.shop = shop;
        this.subShop = new SubShop(shop, this);
    }

    public void setPlayerShopSystem(playerShopSystem playerShops) {
        this.playerShops = playerShops;
    }

    private boolean isSelectionScreenEnabled() {
        return JavaPlugin.getProvidingPlugin(ShopSystemGUI.class)
                .getConfig().getBoolean("shop.selection-screen", true);
    }

    public void openShop(Player player) {
        if (isSelectionScreenEnabled()) {
            openSelection(player);
        } else {
            openServerShop(player);
        }
    }

    private void openSelection(Player player) {
        shopHolder holder = new shopHolder("selection", null, null, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, SELECTION_TITLE);
        holder.inventory = inventory;

        fillBackground(inventory);
        addPlayerInfo(inventory, player);

        inventory.setItem(11, createButton(
                Material.EMERALD,
                "§a§lServer Shop",
                "",
                "§7Browse server categories",
                "§7Buy and sell server items",
                "",
                "§eClick to open"
        ));

        inventory.setItem(15, createButton(
                Material.PLAYER_HEAD,
                "§d§lPlayer Shops",
                "",
                "§7Browse player-created shops",
                "§7Buy items from other players",
                "",
                "§eClick to open"
        ));

        inventory.setItem(22, createButton(
                Material.BOOK,
                "§6Shop Information",
                "§7Server Shop: §aAvailable",
                "§7Player Shops: §aAvailable",
                "",
                "§8Both shops share the SSMP GUI system"
        ));

        player.openInventory(inventory);
    }

    public void openServerShop(Player player) {
        openServerShop(player, 0);
    }

    private void openServerShop(Player player, int page) {
        List<String> ids = shop.getSubcategories(null);

        if (ids.isEmpty()) {
            player.sendMessage("§cThe server shop is empty.");
            play(player, "shop-error");
            return;
        }

        openServerMenu(player, null, ids, page, SHOP_TITLE);
    }

    public void openCategory(Player player, String categoryPath) {
        openCategory(player, categoryPath, 0);
    }

    private void openCategory(Player player, String categoryPath, int page) {
        if (!shop.categoryExists(categoryPath)) {
            player.sendMessage("§cThat category does not exist: " + categoryPath);
            play(player, "shop-error");
            return;
        }

        if (shop.hasSubcategories(categoryPath)) {
            subShop.open(player, categoryPath);
            return;
        }

        List<String> ids = new ArrayList<>();

        for (String id : shop.getItemIds(categoryPath)) {
            Material material = shop.getMaterial(id);
            double buy = shop.getBuyPrice(categoryPath, id);
            double sell = shop.getSellPrice(categoryPath, id);

            if (material != null &&
                    Double.isFinite(buy) && buy >= 0 &&
                    Double.isFinite(sell) && sell >= 0) {
                ids.add(id);
            }
        }

        if (ids.isEmpty()) {
            player.sendMessage("§eThis category is empty.");
            play(player, "shop-error");
            return;
        }

        String name = shop.getCategoryName(categoryPath);
        openServerMenu(player, categoryPath, ids, page, CATEGORY_TITLE + name);
    }

    private void openServerMenu(Player player, String categoryPath, List<String> ids,
                                int page, String title) {
        int maxPages = Math.max(1,
                (int) Math.ceil(ids.size() / (double) CONTENT_SLOTS.length));

        page = Math.max(0, Math.min(page, maxPages - 1));

        String mode = categoryPath == null ? "server_categories" : "server_items";
        shopHolder holder = new shopHolder(mode, categoryPath, null, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inventory;

        fillBackground(inventory);
        addPlayerInfo(inventory, player);

        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, ids.size());

        for (int i = start; i < end; i++) {
            String id = ids.get(i);
            int slot = CONTENT_SLOTS[i - start];

            ItemStack item = categoryPath == null
                    ? createCategoryItem(id)
                    : createShopItem(categoryPath, id);

            if (item != null) {
                inventory.setItem(slot, item);
                holder.entries.put(slot, id);
            }
        }

        if (categoryPath != null) {
            inventory.setItem(45, createButton(
                    Material.ARROW, "§c§lBack", "§7Return to the previous menu"));
        } else {
            inventory.setItem(45, createButton(
                    Material.COMPASS, "§6Shop Selection",
                    "§7Return to the shop selector"));
        }

        if (page > 0) {
            inventory.setItem(48, createButton(
                    Material.SPECTRAL_ARROW, "§e§lPrevious Page",
                    "§7Go to page " + page));
        }

        if (page < maxPages - 1) {
            inventory.setItem(50, createButton(
                    Material.SPECTRAL_ARROW, "§a§lNext Page",
                    "§7Go to page " + (page + 2)));
        }

        inventory.setItem(49, createButton(
                Material.PAPER,
                "§6Page §f" + (page + 1) + "§7/§f" + maxPages,
                "§7" + ids.size() + " entries available"));

        player.openInventory(inventory);
    }

    public void openMarketplace(Player player) {
        openMarketplace(player, 0);
    }

    private void openMarketplace(Player player, int page) {
        if (playerShops == null) {
            player.sendMessage("§cPlayer Shops are not initialized.");
            play(player, "shop-error");
            return;
        }

        List<Listing> listings = playerShops.getAvailableListings();
        openPlayerListings(player, listings, page, MARKETPLACE_TITLE, "marketplace");
    }

    public void openManage(Player player) {
        openManage(player, 0);
    }

    private void openManage(Player player, int page) {
        if (playerShops == null) {
            player.sendMessage("§cPlayer Shops are not initialized.");
            play(player, "shop-error");
            return;
        }

        List<Listing> listings = playerShops.getListings(player.getUniqueId());
        openPlayerListings(player, listings, page, MANAGE_TITLE, "manage");
    }

    private void openPlayerListings(Player player, List<Listing> listings,
                                    int page, String title, String mode) {
        int maxPages = Math.max(1,
                (int) Math.ceil(listings.size() / (double) CONTENT_SLOTS.length));

        page = Math.max(0, Math.min(page, maxPages - 1));

        shopHolder holder = new shopHolder(mode, null, null, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inventory;

        fillBackground(inventory);
        addPlayerInfo(inventory, player);

        int start = page * CONTENT_SLOTS.length;
        int end = Math.min(start + CONTENT_SLOTS.length, listings.size());

        for (int i = start; i < end; i++) {
            Listing listing = listings.get(i);
            int slot = CONTENT_SLOTS[i - start];

            inventory.setItem(slot, createListingItem(listing, mode.equals("manage")));
            holder.entries.put(slot, listing.getId());
        }

        if (listings.isEmpty()) {
            inventory.setItem(22, createButton(
                    Material.BARRIER,
                    mode.equals("manage") ? "§cYou have no shops" : "§eNo listings available",
                    mode.equals("manage")
                            ? "§7Create one using /pshop create"
                            : "§7Player shops will appear here"
            ));
        }

        inventory.setItem(45, createButton(
                mode.equals("marketplace") ? Material.WRITABLE_BOOK : Material.ARROW,
                mode.equals("marketplace") ? "§eManage My Shops" : "§cShop Selection",
                mode.equals("marketplace")
                        ? "§7View and manage your listings"
                        : "§7Return to the shop selector"
        ));

        if (page > 0) {
            inventory.setItem(48, createButton(
                    Material.SPECTRAL_ARROW, "§ePrevious Page",
                    "§7Go to page " + page));
        }

        inventory.setItem(49, createButton(
                Material.PAPER,
                "§6Page §f" + (page + 1) + "§7/§f" + maxPages,
                "§7" + listings.size() + " shops available"));

        if (page < maxPages - 1) {
            inventory.setItem(50, createButton(
                    Material.SPECTRAL_ARROW, "§aNext Page",
                    "§7Go to page " + (page + 2)));
        }

        if (mode.equals("manage")) {
            inventory.setItem(53, createButton(
                    Material.EMERALD,
                    "§aCreate a Shop",
                    "§7Use /pshop create virtual <price>",
                    "§7or /pshop create chest <price>"
            ));
        }

        player.openInventory(inventory);
    }

    private void openDetails(Player player, String id) {
        if (playerShops == null) return;

        Listing listing = playerShops.getListing(id);

        if (listing == null || !playerShops.owns(player, listing)) {
            player.sendMessage("§cYou do not own this listing.");
            play(player, "shop-error");
            return;
        }

        shopHolder holder = new shopHolder("details", null, id, 0);
        Inventory inventory = Bukkit.createInventory(holder, 45, DETAILS_TITLE);
        holder.inventory = inventory;

        fillBackground(inventory);
        inventory.setItem(4, createListingItem(listing, true));

        inventory.setItem(20, createButton(
                Material.HOPPER, "§aRestock ×64",
                "§7Transfer up to 64 matching items",
                "§7from your inventory to your shop.",
                "",
                "§eClick to restock"
        ));

        inventory.setItem(21, createButton(
                Material.CHEST, "§eWithdraw ×64",
                "§7Withdraw up to 64 items",
                "§7from your shop.",
                "",
                "§eClick to withdraw"
        ));

        inventory.setItem(22, createButton(
                listing.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                listing.isEnabled() ? "§cDisable Listing" : "§aEnable Listing",
                "§7Change your listing visibility."
        ));

        inventory.setItem(23, createButton(
                Material.NAME_TAG, "§bChange Price",
                "§7Use /pshop price " + id + " <price>"
        ));

        inventory.setItem(24, createButton(
                Material.TNT, "§cDelete Shop",
                "§7Delete this listing.",
                "§cVirtual stock must fit in your inventory."
        ));

        inventory.setItem(40, createButton(
                Material.ARROW, "§cBack",
                "§7Return to your shops"
        ));

        player.openInventory(inventory);
    }

    private ItemStack createCategoryItem(String categoryPath) {
        ConfigurationSection category = shop.getCategorySection(categoryPath);

        if (category == null) return null;

        Material material = shop.getMaterial(
                category.getString("material", "CHEST"));

        if (material == null) material = Material.CHEST;

        return createButton(
                material,
                "§6" + shop.getCategoryName(categoryPath),
                "§7Browse this category",
                "§8Click to open"
        );
    }

    private ItemStack createShopItem(String categoryPath, String id) {
        Material material = shop.getMaterial(id);
        if (material == null) return null;

        double buy = shop.getBuyPrice(categoryPath, id);
        double sell = shop.getSellPrice(categoryPath, id);

        YamlConfiguration data = shop.getItemsData(categoryPath);
        String itemName = data.getString(
                "items." + id + ".name",
                shop.formatName(id)
        );

        return createButton(
                material,
                "§f" + itemName,
                "",
                "§aBuy: $" + shop.formatMoney(buy),
                "§cSell: $" + shop.formatMoney(sell),
                "",
                "§eLeft-click to buy",
                "§eRight-click to sell"
        );
    }

    private ItemStack createListingItem(Listing listing, boolean management) {
        ItemStack item = listing.getItem().clone();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            List<String> lore = new ArrayList<>();

            lore.add("");
            lore.add("§7Price: §a$" +
                    shop.formatMoney(listing.getPrice()));
            lore.add("§7Stock: §f" + playerShops.getStock(listing));
            lore.add("§7Type: §f" + listing.getType().name());
            lore.add("§7Status: " +
                    (listing.isEnabled() ? "§aActive" : "§cDisabled"));

            OfflinePlayer owner = Bukkit.getOfflinePlayer(listing.getOwner());

            lore.add("§7Seller: §f" +
                    (owner.getName() == null ? "Unknown" : owner.getName()));

            lore.add("");
            lore.add(management
                    ? "§eClick to manage this shop"
                    : "§aClick to purchase");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void addPlayerInfo(Inventory inventory, Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta == null) return;

        meta.setOwningPlayer(player);
        meta.setDisplayName("§e" + player.getName());
        meta.setLore(List.of(
                "",
                "§7Balance: §a$" +
                        shop.formatMoney(shop.getEconomy().getPlayerBalance(player))
        ));

        head.setItemMeta(meta);
        inventory.setItem(4, head);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createButton(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;

            if (row == 0 || column == 0 || column == 8 ||
                    slot >= inventory.getSize() - 9) {
                inventory.setItem(slot, filler);
            }
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof shopHolder holder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player) ||
                event.getRawSlot() < 0 ||
                event.getRawSlot() >= top.getSize()) {
            return;
        }

        int slot = event.getRawSlot();

        switch (holder.mode) {
            case "selection" -> {
                if (slot == 11) {
                    openServerShop(player);
                } else if (slot == 15) {
                    openMarketplace(player);
                }
            }

            case "server_categories", "server_items" -> {
                if (slot == 45) {
                    if (holder.categoryId == null) {
                        openSelection(player);
                    } else {
                        String parent = getParentCategoryPath(holder.categoryId);

                        if (parent == null) {
                            openServerShop(player);
                        } else {
                            subShop.open(player, parent);
                        }
                    }
                    return;
                }

                if (slot == 48 && holder.page > 0) {
                    if (holder.categoryId == null) {
                        openServerShop(player, holder.page - 1);
                    } else {
                        openCategory(player, holder.categoryId, holder.page - 1);
                    }
                    return;
                }

                if (slot == 50) {
                    if (holder.categoryId == null) {
                        openServerShop(player, holder.page + 1);
                    } else {
                        openCategory(player, holder.categoryId, holder.page + 1);
                    }
                    return;
                }

                String id = holder.entries.get(slot);
                if (id == null) return;

                if (holder.mode.equals("server_categories")) {
                    if (shop.hasSubcategories(id)) {
                        subShop.open(player, id);
                    } else {
                        openCategory(player, id);
                    }

                    play(player, "shop-success");
                } else if (event.getClick() == ClickType.LEFT) {
                    openServerPurchaseDialog(player, holder.categoryId, id, 1);
                } else if (event.getClick() == ClickType.RIGHT) {
                    shop.sellItem(player, holder.categoryId, id);
                }
            }

            case "marketplace" -> {
                if (slot == 45) {
                    openManage(player);
                    return;
                }

                if (slot == 48 && holder.page > 0) {
                    openMarketplace(player, holder.page - 1);
                    return;
                }

                if (slot == 50) {
                    openMarketplace(player, holder.page + 1);
                    return;
                }

                String id = holder.entries.get(slot);
                if (id == null || playerShops == null) return;

                Listing listing = playerShops.getListing(id);

                if (listing == null || !listing.isEnabled()) {
                    player.sendMessage("§cThis listing is unavailable.");
                    play(player, "shop-error");
                    return;
                }

                openPlayerPurchaseDialog(player, id, playerShops);
            }

            case "manage" -> {
                if (slot == 45) {
                    openSelection(player);
                    return;
                }

                if (slot == 48 && holder.page > 0) {
                    openManage(player, holder.page - 1);
                    return;
                }

                if (slot == 50) {
                    openManage(player, holder.page + 1);
                    return;
                }

                String id = holder.entries.get(slot);
                if (id != null) openDetails(player, id);
            }

            case "details" -> handleDetailsClick(player, holder, slot);
        }
    }

    private String getParentCategoryPath(String categoryPath) {
        int separator = categoryPath.lastIndexOf('.');

        return separator < 0
                ? null
                : categoryPath.substring(0, separator);
    }

    private void handleDetailsClick(Player player, shopHolder holder, int slot) {
        if (playerShops == null) return;

        String id = holder.listingId;
        Listing listing = playerShops.getListing(id);

        if (listing == null || !playerShops.owns(player, listing)) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 20 -> {
                playerShops.restock(player, id, 64);
                openDetails(player, id);
            }
            case 21 -> {
                playerShops.withdraw(player, id, 64);
                openDetails(player, id);
            }
            case 22 -> {
                playerShops.setEnabled(player, id, !listing.isEnabled());
                openDetails(player, id);
            }
            case 23 -> {
                player.closeInventory();
                player.sendMessage("§eUse /pshop price " + id + " <price>");
            }
            case 24 -> {
                if (playerShops.deleteListing(player, id)) {
                    openManage(player);
                } else {
                    openDetails(player, id);
                }
            }
            case 40 -> openManage(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof shopHolder) {
            event.setCancelled(true);
        }
    }

    private void openServerPurchaseDialog(
            Player player,
            String categoryPath,
            String id,
            int quantity
    ) {
        Material material = shop.getMaterial(id);
        double unitPrice = shop.getBuyPrice(categoryPath, id);

        if (material == null || !Double.isFinite(unitPrice) || unitPrice < 0) {
            player.sendMessage("§cThis item is currently unavailable.");
            play(player, "shop-error");
            return;
        }

        double total = unitPrice * quantity;

        if (!Double.isFinite(total)) {
            player.sendMessage("§cThe total price is invalid.");
            play(player, "shop-error");
            return;
        }

        YamlConfiguration data = shop.getItemsData(categoryPath);
        String itemName = data.getString(
                "items." + id + ".name",
                shop.formatName(id)
        );

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Server Shop\nPrice: $" + shop.formatMoney(unitPrice) +
                                " each\nQuantity: " + quantity +
                                "\nTotal: $" + shop.formatMoney(total)
                ))
        );

        List<ActionButton> actions = new ArrayList<>();

        for (int amount : QUANTITIES) {
            actions.add(ActionButton.create(
                    Component.text((amount == quantity ? "✓ " : "") + "×" + amount),
                    Component.text("Select " + amount),
                    40,
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player target) {
                            openServerPurchaseDialog(target, categoryPath, id, amount);
                        }
                    }, ClickCallback.Options.builder().uses(1).build())
            ));
        }

        actions.add(ActionButton.create(
                Component.text("BUY ×" + quantity + "  $" + shop.formatMoney(total)),
                Component.text("Buy " + quantity + " " + itemName),
                100,
                DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) {
                        shop.buyItem(target, categoryPath, id, quantity);
                    }
                }, ClickCallback.Options.builder().uses(1).build())
        ));

        ActionButton close = ActionButton.create(
                Component.text("CLOSE"),
                Component.text("Close this screen"),
                60,
                null
        );

        Dialog dialog = Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text(itemName))
                        .body(body)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(actions, close, 4))
        );

        player.showDialog(dialog);
    }

    public void openPlayerPurchaseDialog(Player player, String id,
                                         playerShopSystem system) {
        Listing listing = system.getListing(id);

        if (listing == null || !listing.isEnabled()) {
            player.sendMessage("§cThis listing is unavailable.");
            play(player, "shop-error");
            return;
        }

        Material material = listing.getItem().getType();
        ItemMeta itemMeta = listing.getItem().getItemMeta();

        String itemName = itemMeta != null && itemMeta.hasDisplayName()
                ? itemMeta.getDisplayName()
                : material.name().replace('_', ' ');

        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.getOwner());
        String sellerName = seller.getName() == null
                ? "Unknown Seller" : seller.getName();

        openPlayerPurchaseDialog(player, id, system, 1, itemName, sellerName);
    }

    private void openPlayerPurchaseDialog(Player player, String id,
                                          playerShopSystem system, int quantity,
                                          String itemName, String sellerName) {
        Listing listing = system.getListing(id);

        if (listing == null || !listing.isEnabled()) {
            player.sendMessage("§cThis listing is unavailable.");
            play(player, "shop-error");
            return;
        }

        double unitPrice = listing.getPrice();
        double total = unitPrice * quantity;

        if (!Double.isFinite(total) || total < 0) {
            player.sendMessage("§cInvalid transaction amount.");
            play(player, "shop-error");
            return;
        }

        int stock = system.getStock(listing);

        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text(
                        "Player Shop\nSeller: " + sellerName +
                                "\nPrice: $" + shop.formatMoney(unitPrice) +
                                "\nAvailable: " + stock +
                                "\nQuantity: " + quantity +
                                "\nTotal: $" + shop.formatMoney(total)
                ))
        );

        List<ActionButton> actions = new ArrayList<>();

        for (int amount : QUANTITIES) {
            actions.add(ActionButton.create(
                    Component.text((amount == quantity ? "✓ " : "") + "×" + amount),
                    Component.text("Select " + amount),
                    40,
                    DialogAction.customClick((response, audience) -> {
                        if (audience instanceof Player target) {
                            openPlayerPurchaseDialog(
                                    target, id, system, amount, itemName, sellerName);
                        }
                    }, ClickCallback.Options.builder().uses(1).build())
            ));
        }

        actions.add(ActionButton.create(
                Component.text("BUY ×" + quantity + "  $" + shop.formatMoney(total)),
                Component.text("Purchase from " + sellerName),
                100,
                DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player target) {
                        system.buyListing(target, id, quantity);
                    }
                }, ClickCallback.Options.builder().uses(1).build())
        ));

        ActionButton close = ActionButton.create(
                Component.text("CLOSE"),
                Component.text("Close this screen"),
                60,
                null
        );

        Dialog dialog = Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text(itemName))
                        .body(body)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(actions, close, 4))
        );

        player.showDialog(dialog);
    }

    private void play(Player player, String sound) {
        if (shop.getSounds() != null) {
            shop.getSounds().play(player, sound);
        }
    }
}
