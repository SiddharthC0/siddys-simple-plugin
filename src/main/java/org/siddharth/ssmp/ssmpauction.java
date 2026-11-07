/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ssmpauction implements Listener, CommandExecutor, TabCompleter {

    private final ssmp plugin;
    private final ssmpeconomy economy;
    private final ssmppermissions permissions;
    private final ssmpsoundsystem sounds;

    private final File auctionFolder;
    private final File listingsFile;
    private final YamlConfiguration listings;

    private final NamespacedKey listingKey;
    private final int listingsPerPage = 36;

    public ssmpauction(
            ssmp plugin,
            ssmpeconomy economy,
            ssmppermissions permissions,
            ssmpsoundsystem sounds
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.permissions = permissions;
        this.sounds = sounds;

        auctionFolder = new File(plugin.getDataFolder(), "auction");
        listingsFile = new File(auctionFolder, "listings.yml");

        if (!auctionFolder.exists()) {
            auctionFolder.mkdirs();
        }

        try {
            if (!listingsFile.exists()) {
                listingsFile.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        listings = YamlConfiguration.loadConfiguration(listingsFile);

        listingKey = new NamespacedKey(
                plugin,
                "auction-listing"
        );
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean(
                "modules.auction.enabled",
                true
        );
    }

    private int getMaxAuctionPrice() {
        return plugin.getConfig().getInt(
                "modules.auction.max-auction-price",
                1000000
        );
    }

    private int getNegateBuyPrice() {
        return plugin.getConfig().getInt(
                "modules.auction.negate-buy-price",
                0
        );
    }

    private int getMaxPages() {
        return plugin.getConfig().getInt(
                "modules.auction.max-pages",
                10
        );
    }

    private List<String> getNonSellableItems() {
        return plugin.getConfig().getStringList(
                "modules.auction.non-sellable-items"
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!isEnabled()) {
            sender.sendMessage(
                    ChatColor.RED + "The Auction House is currently disabled."
            );
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    ChatColor.RED +
                            "Only players can use the Auction House."
            );
            return true;
        }

        if (args.length == 0) {
            if (!permissions.hasPermission(
                    player,
                    "ssmp.auction.use"
            )) {
                error(
                        player,
                        "You do not have permission to use the Auction House."
                );
                return true;
            }

            sounds.play(player, "menu-open");
            openAuctionHouse(player, 1);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (!permissions.hasPermission(
                    player,
                    "ssmp.auction.sell"
            )) {
                error(
                        player,
                        "You do not have permission to sell items."
                );
                return true;
            }

            if (args.length < 2) {
                error(
                        player,
                        "Usage: /ah sell <price>"
                );
                return true;
            }

            int price;

            try {
                price = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                error(player, "Invalid price.");
                return true;
            }

            createListing(player, price);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!permissions.hasPermission(
                    player,
                    "ssmp.auction.remove"
            )) {
                error(
                        player,
                        "You do not have permission to remove auction listings."
                );
                return true;
            }

            if (args.length < 2) {
                error(
                        player,
                        "Usage: /ah remove <id>"
                );
                return true;
            }

            int id;

            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                error(player, "Invalid listing ID.");
                return true;
            }

            removeListing(player, id);
            return true;
        }

        error(
                player,
                "Usage: /ah [sell <price>|remove <id>]"
        );

        return true;
    }

    private void createListing(Player player, int price) {
        if (price <= 0) {
            error(
                    player,
                    "The price must be greater than $0."
            );
            return;
        }

        int maxPrice = getMaxAuctionPrice();

        if (price > maxPrice) {
            error(
                    player,
                    "The maximum auction price is $" + maxPrice + "."
            );
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            error(
                    player,
                    "You must hold an item to auction."
            );
            return;
        }

        if (isNonSellable(item)) {
            error(
                    player,
                    "This item cannot be auctioned."
            );
            return;
        }

        int maxPages = getMaxPages();

        if (getListingCount() >= maxPages * listingsPerPage) {
            error(
                    player,
                    "The Auction House is currently full."
            );
            return;
        }

        int id = getNextListingId();
        String path = "listings." + id;

        listings.set(
                path + ".seller",
                player.getUniqueId().toString()
        );

        listings.set(
                path + ".seller-name",
                player.getName()
        );

        listings.set(
                path + ".item",
                item.clone()
        );

        listings.set(
                path + ".price",
                price
        );

        listings.set(
                path + ".created",
                System.currentTimeMillis()
        );

        player.getInventory().setItemInMainHand(null);

        saveListings();

        sounds.play(
                player,
                "auction-success"
        );

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.GREEN + "Your " + getItemName(item) + " has been listed for $" + price)
        );
    }

    private boolean isNonSellable(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        for (String blockedItem : getNonSellableItems()) {
            if (blockedItem.equalsIgnoreCase(
                    item.getType().name()
            )) {
                return true;
            }
        }

        return false;
    }

    private void removeListing(Player player, int id) {
        String path = "listings." + id;

        if (!listings.contains(path)) {
            error(
                    player,
                    "That auction does not exist."
            );
            return;
        }

        String seller =
                listings.getString(path + ".seller");

        if (seller == null ||
                !seller.equals(
                        player.getUniqueId().toString()
                )) {
            error(
                    player,
                    "That is not your auction."
            );
            return;
        }

        ItemStack item =
                listings.getItemStack(path + ".item");

        if (item == null) {
            listings.set(path, null);
            saveListings();

            error(
                    player,
                    "That auction contained an invalid item."
            );

            return;
        }

        if (!player.getInventory().addItem(item).isEmpty()) {
            error(
                    player,
                    "You do not have enough inventory space."
            );
            return;
        }

        listings.set(path, null);
        saveListings();

        sounds.play(
                player,
                "auction-success"
        );

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.YELLOW + "Your auction has been removed")
        );
    }

    private void openAuctionHouse(
            Player player,
            int page
    ) {
        int maxPage = getMaxPage();

        page = Math.max(
                1,
                Math.min(page, maxPage)
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        ChatColor.DARK_GRAY +
                                "Auction House | " +
                                page
                );

        ItemStack glass =
                createItem(
                        Material.BLACK_STAINED_GLASS_PANE,
                        " "
                );

        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, glass);
            inventory.setItem(45 + slot, glass);
        }

        List<Integer> ids =
                getListingIds();

        int start =
                (page - 1) * listingsPerPage;

        int end =
                Math.min(
                        start + listingsPerPage,
                        ids.size()
                );

        int guiSlot = 9;

        for (int i = start; i < end; i++) {
            int id = ids.get(i);

            ItemStack display =
                    createListingDisplay(id);

            if (display != null) {
                setListingId(
                        display,
                        id
                );

                inventory.setItem(
                        guiSlot,
                        display
                );

                guiSlot++;
            }
        }

        if (ids.isEmpty()) {
            ItemStack empty =
                    createItem(
                            Material.BARRIER,
                            ChatColor.RED +
                                    "No auctions today"
                    );

            ItemMeta meta =
                    empty.getItemMeta();

            if (meta != null) {
                meta.setLore(
                        List.of(
                                ChatColor.GRAY +
                                        "There are currently no items listed."
                        )
                );

                empty.setItemMeta(meta);
            }

            inventory.setItem(22, empty);
        }

        ItemStack head =
                createPlayerHead(player);

        if (head != null) {
            inventory.setItem(
                    45,
                    head
            );
        }

        if (page > 1) {
            inventory.setItem(
                    48,
                    createItem(
                            Material.ARROW,
                            ChatColor.YELLOW +
                                    "Previous Page"
                    )
            );
        }

        inventory.setItem(
                49,
                createItem(
                        Material.PAPER,
                        ChatColor.YELLOW +
                                "Page " +
                                page +
                                "/" +
                                maxPage
                )
        );

        if (page < maxPage) {
            inventory.setItem(
                    50,
                    createItem(
                            Material.ARROW,
                            ChatColor.YELLOW +
                                    "Next Page"
                    )
            );
        }

        player.openInventory(inventory);
    }

    private void openConfirmation(
            Player player,
            int id
    ) {
        String path =
                "listings." + id;

        if (!listings.contains(path)) {
            error(
                    player,
                    "That auction no longer exists."
            );
            return;
        }

        ItemStack item =
                listings.getItemStack(
                        path + ".item"
                );

        if (item == null) {
            error(
                    player,
                    "That auction is invalid."
            );
            return;
        }

        int price =
                listings.getInt(
                        path + ".price"
                );

        String seller =
                listings.getString(
                        path + ".seller-name",
                        "Unknown"
                );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        ChatColor.DARK_GRAY +
                                "Confirm Purchase"
                );

        ItemStack glass =
                createItem(
                        Material.BLACK_STAINED_GLASS_PANE,
                        " "
                );

        for (int slot = 0; slot < 27; slot++) {
            if (slot != 11 &&
                    slot != 13 &&
                    slot != 15) {
                inventory.setItem(
                        slot,
                        glass
                );
            }
        }

        ItemStack display =
                item.clone();

        ItemMeta itemMeta =
                display.getItemMeta();

        if (itemMeta != null) {
            List<String> lore =
                    itemMeta.hasLore()
                            ? new ArrayList<>(
                            itemMeta.getLore()
                    )
                            : new ArrayList<>();

            lore.add("");
            lore.add(
                    ChatColor.GRAY +
                            "Seller: " +
                            seller
            );

            lore.add(
                    ChatColor.GOLD +
                            "Price: $" +
                            price
            );

            itemMeta.setLore(lore);
            display.setItemMeta(itemMeta);
        }

        setListingId(
                display,
                id
        );

        inventory.setItem(
                13,
                display
        );

        inventory.setItem(
                11,
                createItem(
                        Material.RED_WOOL,
                        ChatColor.RED +
                                "Cancel"
                )
        );

        inventory.setItem(
                15,
                createItem(
                        Material.LIME_WOOL,
                        ChatColor.GREEN +
                                "Buy for $" +
                                price
                )
        );

        sounds.play(
                player,
                "auction-click"
        );

        player.openInventory(inventory);
    }

    private ItemStack createListingDisplay(int id) {
        String path =
                "listings." + id;

        ItemStack item =
                listings.getItemStack(
                        path + ".item"
                );

        if (item == null) {
            return null;
        }

        ItemStack display =
                item.clone();

        ItemMeta meta =
                display.getItemMeta();

        if (meta == null) {
            return display;
        }

        List<String> lore =
                meta.hasLore()
                        ? new ArrayList<>(
                        meta.getLore()
                )
                        : new ArrayList<>();

        lore.add("");

        lore.add(
                ChatColor.GRAY +
                        "Seller: " +
                        listings.getString(
                                path + ".seller-name",
                                "Unknown"
                        )
        );

        lore.add(
                ChatColor.GOLD +
                        "Price: $" +
                        listings.getInt(
                                path + ".price"
                        )
        );

        lore.add("");
        lore.add(
                ChatColor.YELLOW +
                        "Click to view"
        );

        meta.setLore(lore);
        display.setItemMeta(meta);

        return display;
    }

    private ItemStack createPlayerHead(
            Player player
    ) {
        ItemStack head =
                new ItemStack(
                        Material.PLAYER_HEAD
                );

        SkullMeta meta =
                (SkullMeta) head.getItemMeta();

        if (meta == null) {
            return head;
        }

        meta.setOwningPlayer(player);

        meta.setDisplayName(
                ChatColor.YELLOW +
                        player.getName()
        );

        List<String> lore =
                new ArrayList<>();

        lore.add("");

        lore.add(
                ChatColor.GRAY +
                        "Balance: " +
                        ChatColor.GREEN +
                        "$" +
                        economy.getPlayerBalance(player)
        );

        meta.setLore(lore);
        head.setItemMeta(meta);

        return head;
    }

    private ItemStack createItem(
            Material material,
            String name
    ) {
        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }

    private void setListingId(
            ItemStack item,
            int id
    ) {
        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer()
                    .set(
                            listingKey,
                            PersistentDataType.INTEGER,
                            id
                    );

            item.setItemMeta(meta);
        }
    }

    private int getListingId(
            ItemStack item
    ) {
        if (item == null ||
                !item.hasItemMeta()) {
            return -1;
        }

        Integer id =
                item.getItemMeta()
                        .getPersistentDataContainer()
                        .get(
                                listingKey,
                                PersistentDataType.INTEGER
                        );

        return id == null ? -1 : id;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {
        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        String title =
                ChatColor.stripColor(
                        event.getView().getTitle()
                );

        if (title.startsWith(
                "Auction House | "
        )) {
            event.setCancelled(true);

            if (event.getRawSlot() < 0 ||
                    event.getRawSlot() >=
                            event.getView()
                                    .getTopInventory()
                                    .getSize()) {
                return;
            }

            ItemStack clicked =
                    event.getCurrentItem();

            if (clicked == null) {
                return;
            }

            if (event.getRawSlot() == 48) {
                int page =
                        getCurrentPage(title);

                if (page > 1) {
                    sounds.play(
                            player,
                            "auction-click"
                    );

                    openAuctionHouse(
                            player,
                            page - 1
                    );
                } else {
                    sounds.play(
                            player,
                            "auction-error"
                    );
                }

                return;
            }

            if (event.getRawSlot() == 50) {
                int page =
                        getCurrentPage(title);

                if (page < getMaxPage()) {
                    sounds.play(
                            player,
                            "auction-click"
                    );

                    openAuctionHouse(
                            player,
                            page + 1
                    );
                } else {
                    sounds.play(
                            player,
                            "auction-error"
                    );
                }

                return;
            }

            int id =
                    getListingId(clicked);

            if (id != -1 &&
                    listingExists(id)) {

                if (!permissions.hasPermission(
                        player,
                        "ssmp.auction.buy"
                )) {
                    error(
                            player,
                            "You do not have permission to buy auctions."
                    );
                    return;
                }

                sounds.play(
                        player,
                        "auction-click"
                );

                openConfirmation(
                        player,
                        id
                );
            }

            return;
        }

        if (title.equals(
                "Confirm Purchase"
        )) {
            event.setCancelled(true);

            if (event.getRawSlot() == 11) {
                sounds.play(
                        player,
                        "auction-click"
                );

                player.closeInventory();
                return;
            }

            if (event.getRawSlot() != 15) {
                return;
            }

            ItemStack display =
                    event.getInventory()
                            .getItem(13);

            int id =
                    getListingId(display);

            if (id == -1) {
                error(
                        player,
                        "This auction is invalid."
                );

                player.closeInventory();
                return;
            }

            buyListing(
                    player,
                    id
            );
        }
    }

    private void buyListing(
            Player buyer,
            int id
    ) {
        String path =
                "listings." + id;

        if (!listings.contains(path)) {
            error(
                    buyer,
                    "This auction is no longer available."
            );

            buyer.closeInventory();
            return;
        }

        String sellerString =
                listings.getString(
                        path + ".seller"
                );

        UUID sellerUuid;

        try {
            sellerUuid =
                    UUID.fromString(
                            sellerString
                    );
        } catch (Exception e) {
            error(
                    buyer,
                    "This auction is invalid."
            );
            return;
        }

        if (sellerUuid.equals(
                buyer.getUniqueId()
        )) {
            error(
                    buyer,
                    "You cannot buy your own auction."
            );
            return;
        }

        double price =
                listings.getDouble(
                        path + ".price"
                );

        double balance =
                economy.getPlayerBalance(
                        buyer
                );

        int negate =
                getNegateBuyPrice();

        if (balance - price < -negate) {
            error(
                    buyer,
                    "You cannot afford this item."
            );
            return;
        }

        ItemStack item =
                listings.getItemStack(
                        path + ".item"
                );

        if (item == null ||
                item.getType().isAir()) {
            error(
                    buyer,
                    "This auction is invalid."
            );
            return;
        }

        if (!buyer.getInventory()
                .addItem(item.clone())
                .isEmpty()) {
            error(
                    buyer,
                    "You do not have enough inventory space."
            );
            return;
        }

        String itemName =
                getItemName(item);

        listings.set(
                path,
                null
        );

        saveListings();

        economy.removePlayerBalance(
                buyer,
                price
        );

        sounds.play(
                buyer,
                "auction-success"
        );

        buyer.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.YELLOW + "You have bought this item for $"+price)
        );

        Player seller =
                Bukkit.getPlayer(
                        sellerUuid
                );

        if (seller != null) {
            economy.addPlayerBalance(
                    seller,
                    price
            );

            sounds.play(
                    seller,
                    "auction-success"
            );
            seller.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.GREEN+buyer.getName()+" has bought you "+itemName+" for $"+price)
            );
        }

        buyer.closeInventory();
    }

    private void error(
            Player player,
            String message
    ) {
        sounds.play(
                player,
                "auction-error"
        );

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.RED + message)
        );
    }

    private String getItemName(
            ItemStack item
    ) {
        if (item == null) {
            return "item";
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null &&
                meta.hasDisplayName()) {
            return ChatColor.stripColor(
                    meta.getDisplayName()
            );
        }

        String name =
                item.getType()
                        .name()
                        .toLowerCase()
                        .replace(
                                '_',
                                ' '
                        );

        return name.substring(0, 1).toUpperCase() +
                name.substring(1);
    }

    private List<Integer> getListingIds() {
        List<Integer> ids =
                new ArrayList<>();

        if (!listings.contains(
                "listings"
        )) {
            return ids;
        }

        if (listings.getConfigurationSection(
                "listings"
        ) == null) {
            return ids;
        }

        for (String key :
                listings
                        .getConfigurationSection(
                                "listings"
                        )
                        .getKeys(false)) {

            try {
                ids.add(
                        Integer.parseInt(key)
                );
            } catch (NumberFormatException ignored) {
            }
        }

        ids.sort(
                Integer::compareTo
        );

        return ids;
    }

    private int getListingCount() {
        return getListingIds().size();
    }

    private int getMaxPage() {
        int count =
                getListingCount();

        if (count == 0) {
            return 1;
        }

        int configuredMaxPages =
                Math.max(
                        1,
                        getMaxPages()
                );

        int calculatedPages =
                (int) Math.ceil(
                        (double) count /
                                listingsPerPage
                );

        return Math.min(
                calculatedPages,
                configuredMaxPages
        );
    }

    private int getNextListingId() {
        int id = 1;

        while (listings.contains(
                "listings." + id
        )) {
            id++;
        }

        return id;
    }

    private boolean listingExists(
            int id
    ) {
        return listings.contains(
                "listings." + id
        );
    }

    private int getCurrentPage(
            String title
    ) {
        try {
            return Integer.parseInt(
                    title.substring(
                            title.lastIndexOf("| ") + 2
                    )
            );
        } catch (Exception e) {
            return 1;
        }
    }

    private void saveListings() {
        try {
            listings.save(
                    listingsFile
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (!isEnabled()) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> suggestions =
                    new ArrayList<>();

            suggestions.add("sell");
            suggestions.add("remove");

            return suggestions.stream()
                    .filter(
                            value ->
                                    value.toLowerCase()
                                            .startsWith(
                                                    args[0]
                                                            .toLowerCase()
                                            )
                    )
                    .toList();
        }

        if (args.length == 2 &&
                args[0].equalsIgnoreCase(
                        "remove"
                )) {
            return getListingIds()
                    .stream()
                    .map(String::valueOf)
                    .filter(
                            value ->
                                    value.startsWith(
                                            args[1]
                                    )
                    )
                    .toList();
        }

        return List.of();
    }
}
