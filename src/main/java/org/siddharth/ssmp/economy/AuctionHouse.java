
package org.siddharth.ssmp.economy;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.gui.AuctionHouseGUI;
import org.siddharth.ssmp.mechanics.PermissionSystem;
import org.siddharth.ssmp.mechanics.SoundSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionHouse implements CommandExecutor, TabCompleter {

    private final SiddysSimplePlugin plugin;
    private final Economy economy;
    private final PermissionSystem permissions;
    private final SoundSystem sounds;
    private final File auctionFolder;
    private final File listingsFile;
    private final YamlConfiguration listings;
    private final NamespacedKey listingKey;
    private final AuctionHouseGUI gui;
    private final int listingsPerPage = 36;

    public AuctionHouse(SiddysSimplePlugin plugin, Economy economy, PermissionSystem permissions, SoundSystem sounds) {
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
            throw new RuntimeException("Could not create auction listings file.", e);
        }

        listings = YamlConfiguration.loadConfiguration(listingsFile);
        listingKey = new NamespacedKey(plugin, "auction-listing");
        gui = new AuctionHouseGUI(this);
    }

    public AuctionHouseGUI getGUI() {
        return gui;
    }

    public SiddysSimplePlugin getPlugin() {
        return plugin;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PermissionSystem getPermissions() {
        return permissions;
    }

    public SoundSystem getSounds() {
        return sounds;
    }

    public NamespacedKey getListingKey() {
        return listingKey;
    }

    public int getListingsPerPage() {
        return listingsPerPage;
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("modules.auction.enabled", true);
    }

    public int getMaxAuctionPrice() {
        return plugin.getConfig().getInt("modules.auction.max-auction-price", 10000000);
    }

    private int getNegateBuyPrice() {
        return plugin.getConfig().getInt("modules.auction.negate-buy-price", 0);
    }

    public int getMaxPages() {
        return Math.max(1, plugin.getConfig().getInt("modules.auction.max-pages", 10));
    }

    private List<String> getNonSellableItems() {
        return plugin.getConfig().getStringList("modules.auction.non-sellable-items");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(ChatColor.RED + "The Auction House module is disabled.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use the Auction House.");
            return true;
        }

        if (args.length == 0) {
            if (!permissions.hasPermission(player, "ssmp.auction.use")) {
                error(player, "Your rank does not include the permission to use the Auction House.");
                return true;
            }

            sounds.play(player, "menu-open");
            gui.openAuctionHouse(player, 1);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (!permissions.hasPermission(player, "ssmp.auction.sell")) {
                error(player, "Your rank does not include the permission to sell items in auctions.");
                return true;
            }

            if (args.length > 1) {
                error(player, "Use /ah sell to open the listing dialog.");
                return true;
            }

            gui.openSellDialog(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!permissions.hasPermission(player, "ssmp.auction.remove")) {
                error(player, "Your rank does not have permission to remove auction listings.");
                return true;
            }

            if (args.length < 2) {
                error(player, "Usage: /ah remove <id>");
                return true;
            }

            try {
                removeListing(player, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                error(player, "Invalid listing ID.");
            }

            return true;
        }

        error(player, "Usage: /ah [sell|remove <id>]");
        return true;
    }

    public void createListing(Player player, int price) {
        if (price <= 0) {
            error(player, "You can't sell your item for less than $1!");
            return;
        }

        int maxPrice = getMaxAuctionPrice();
        if (price > maxPrice) {
            error(player, "The maximum auction price is $" + maxPrice);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            error(player, "Your main hand is empty.");
            return;
        }

        if (isNonSellable(item)) {
            error(player, "This item cannot be auctioned.");
            return;
        }

        if (getListingCount() >= getMaxPages() * listingsPerPage) {
            error(player, "The Auction House is currently full.");
            return;
        }

        int id = getNextListingId();
        String path = "listings." + id;

        listings.set(path + ".seller", player.getUniqueId().toString());
        listings.set(path + ".seller-name", player.getName());
        listings.set(path + ".item", item.clone());
        listings.set(path + ".price", price);
        listings.set(path + ".created", System.currentTimeMillis());

        player.getInventory().setItemInMainHand(null);
        saveListings();

        sounds.play(player, "menu-success");
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
            if (blockedItem.equalsIgnoreCase(item.getType().name())) {
                return true;
            }
        }

        return false;
    }

    public void removeListing(Player player, int id) {
        String path = "listings." + id;

        if (!listingExists(id)) {
            error(player, "That auction does not exist.");
            return;
        }

        String seller = listings.getString(path + ".seller");

        if (seller == null || !seller.equals(player.getUniqueId().toString())) {
            error(player, "That is not your auction.");
            return;
        }

        ItemStack item = listings.getItemStack(path + ".item");

        if (item == null || item.getType().isAir()) {
            listings.set(path, null);
            saveListings();
            error(player, "That auction contained an invalid item.");
            return;
        }

        if (!player.getInventory().addItem(item.clone()).isEmpty()) {
            error(player, "You do not have enough inventory space.");
            return;
        }

        listings.set(path, null);
        saveListings();

        sounds.play(player, "menu-success");
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.YELLOW + "Your auction has been removed.")
        );
    }

    public void buyListing(Player buyer, int id) {
        String path = "listings." + id;

        if (!listingExists(id)) {
            error(buyer, "This auction is no longer available.");
            buyer.closeInventory();
            return;
        }

        String sellerString = listings.getString(path + ".seller");
        UUID sellerUuid;

        try {
            sellerUuid = UUID.fromString(sellerString);
        } catch (Exception e) {
            error(buyer, "This auction is invalid.");
            return;
        }

        if (sellerUuid.equals(buyer.getUniqueId())) {
            error(buyer, "You cannot buy your own auction.");
            return;
        }

        double price = listings.getDouble(path + ".price");
        double balance = economy.getPlayerBalance(buyer);
        int negate = getNegateBuyPrice();

        if (balance - price < -negate) {
            error(buyer, "You cannot afford this item.");
            return;
        }

        ItemStack item = listings.getItemStack(path + ".item");

        if (item == null || item.getType().isAir()) {
            error(buyer, "This auction is invalid.");
            return;
        }

        if (!buyer.getInventory().addItem(item.clone()).isEmpty()) {
            error(buyer, "You do not have enough inventory space.");
            return;
        }

        String itemName = getItemName(item);

        listings.set(path, null);
        saveListings();

        economy.removePlayerBalance(buyer, price);

        sounds.play(buyer, "menu-success");
        buyer.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.YELLOW + "You bought this item for $" + price)
        );

        Player seller = Bukkit.getPlayer(sellerUuid);

        if (seller != null) {
            economy.addPlayerBalance(seller, price);
            sounds.play(seller, "menu-success");
            seller.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.GREEN + buyer.getName() + " bought your " + itemName + " for $" + price)
            );
        }

        buyer.closeInventory();
    }

    public void error(Player player, String message) {
        sounds.play(player, "menu-error");
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.RED + message)
        );
    }

    public String getItemName(ItemStack item) {
        if (item == null) {
            return "item";
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }

        String name = item.getType().name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public ItemStack getListingItem(int id) {
        return listings.getItemStack("listings." + id + ".item");
    }

    public int getListingPrice(int id) {
        return listings.getInt("listings." + id + ".price");
    }

    public String getListingSellerName(int id) {
        return listings.getString("listings." + id + ".seller-name", "Unknown");
    }

    public String getListingSeller(int id) {
        return listings.getString("listings." + id + ".seller");
    }

    public List<Integer> getListingIds() {
        List<Integer> ids = new ArrayList<>();

        if (listings.getConfigurationSection("listings") == null) {
            return ids;
        }

        for (String key : listings.getConfigurationSection("listings").getKeys(false)) {
            try {
                ids.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
            }
        }

        ids.sort(Integer::compareTo);
        return ids;
    }

    public int getListingCount() {
        return getListingIds().size();
    }

    public int getMaxPage() {
        int count = getListingCount();

        if (count == 0) {
            return 1;
        }

        int calculatedPages = (int) Math.ceil((double) count / listingsPerPage);
        return Math.min(calculatedPages, getMaxPages());
    }

    public int getNextListingId() {
        int id = 1;

        while (listingExists(id)) {
            id++;
        }

        return id;
    }

    public boolean listingExists(int id) {
        return listings.contains("listings." + id);
    }

    private void saveListings() {
        try {
            listings.save(listingsFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not save auction listings.", e);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!isEnabled()) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("sell");
            suggestions.add("remove");

            return suggestions.stream()
                    .filter(value -> value.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return getListingIds().stream()
                    .map(String::valueOf)
                    .filter(value -> value.startsWith(args[1]))
                    .toList();
        }

        return List.of();
    }
}
