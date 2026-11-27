package org.siddharth.ssmp.economy;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;
import org.bukkit.Location;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.gui.ShopSystemGUI;
import org.siddharth.ssmp.mechanics.SoundSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class playerShopSystem implements Listener, CommandExecutor, TabCompleter {

    public enum ShopType {
        VIRTUAL,
        CHEST
    }

    public static class Listing {
        private final String id;
        private final UUID owner;
        private final ShopType type;
        private final ItemStack item;
        private double price;
        private ItemStack virtualStock;
        private String world;
        private int x;
        private int y;
        private int z;
        private boolean enabled;

        private Listing(String id, UUID owner, ShopType type, ItemStack item,
                        double price, ItemStack virtualStock) {
            this.id = id;
            this.owner = owner;
            this.type = type;
            this.item = item.clone();
            this.price = price;
            this.virtualStock = virtualStock == null ? null : virtualStock.clone();
            this.enabled = true;
        }

        public String getId() {
            return id;
        }

        public UUID getOwner() {
            return owner;
        }

        public ShopType getType() {
            return type;
        }

        public ItemStack getItem() {
            return item.clone();
        }

        public double getPrice() {
            return price;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isChestShop() {
            return type == ShopType.CHEST;
        }

        public ItemStack getVirtualStock() {
            return virtualStock == null ? null : virtualStock.clone();
        }
    }

    private final SiddysSimplePlugin plugin;
    private final Economy economy;
    private final SoundSystem sounds;
    private final ShopSystem shopSystem;
    private final ShopSystemGUI shopGUI;

    private final File folder;
    private final File listingsFile;
    private YamlConfiguration data;

    private final Map<String, Listing> listings = new HashMap<>();
    private final Set<String> lockedListings = new HashSet<>();

    private static final int MAX_LISTINGS = 15;
    private static final double MIN_PRICE = 0.01;
    private static final double MAX_PRICE = 1000000000.0;

    private static final int[] PURCHASE_QUANTITIES = {
            1, 4, 8, 16, 32, 64, 128
    };

    public playerShopSystem(SiddysSimplePlugin plugin, Economy economy,
                            SoundSystem sounds, ShopSystem shopSystem,
                            ShopSystemGUI shopGUI) {
        this.plugin = plugin;
        this.economy = economy;
        this.sounds = sounds;
        this.shopSystem = shopSystem;
        this.shopGUI = shopGUI;

        folder = new File(plugin.getDataFolder(), "economy/playershops");
        listingsFile = new File(folder, "listings.yml");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Could not create player shops directory.");
        }

        if (!listingsFile.exists()) {
            try {
                if (!listingsFile.createNewFile()) {
                    throw new IOException("Could not create listings.yml");
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Could not create player shop storage.", exception);
            }
        }

        data = YamlConfiguration.loadConfiguration(listingsFile);
        loadListings();

        shopGUI.setPlayerShopSystem(this);
    }

    public ShopSystemGUI getGUI() {
        return shopGUI;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ShopSystem getShopSystem() {
        return shopSystem;
    }

    public List<Listing> getListings() {
        return new ArrayList<>(listings.values());
    }

    public Listing getListing(String id) {
        return listings.get(id);
    }

    public List<Listing> getListings(UUID owner) {
        List<Listing> result = new ArrayList<>();

        for (Listing listing : listings.values()) {
            if (listing.owner.equals(owner)) {
                result.add(listing);
            }
        }

        return result;
    }

    public List<Listing> getAvailableListings() {
        List<Listing> result = new ArrayList<>();

        for (Listing listing : listings.values()) {
            if (listing.enabled && getStock(listing) > 0) {
                result.add(listing);
            }
        }

        return result;
    }

    public int getStock(Listing listing) {
        if (listing == null) return 0;

        if (listing.type == ShopType.VIRTUAL) {
            return listing.virtualStock == null
                    ? 0
                    : listing.virtualStock.getAmount();
        }

        Inventory inventory = getChestInventory(listing);
        if (inventory == null) return 0;

        return countItems(inventory, listing.item);
    }

    public boolean createVirtualShop(Player player, ItemStack item, double price) {
        if (!canCreate(player, price, item)) return false;

        ItemStack stock = item.clone();
        String id = UUID.randomUUID().toString();

        Listing listing = new Listing(
                id,
                player.getUniqueId(),
                ShopType.VIRTUAL,
                item,
                price,
                stock
        );

        listings.put(id, listing);
        saveListings();

        player.getInventory().setItemInMainHand(null);
        player.sendMessage("§aVirtual shop created successfully!");
        play(player, "shop-success");

        return true;
    }

    public boolean createChestShop(Player player, Block block, double price) {
        if (block == null || !(block.getState() instanceof Chest chest)) {
            return fail(player, "§cYou must select a chest.");
        }

        if (!canCreate(player, price, null)) return false;

        for (Listing existing : listings.values()) {
            if (existing.type == ShopType.CHEST &&
                    isSameChest(existing, block.getLocation())) {
                return fail(player, "§cThis chest is already registered as a shop.");
            }
        }

        ItemStack sample = null;

        for (ItemStack stack : chest.getBlockInventory().getContents()) {
            if (stack != null && !stack.getType().isAir()) {
                sample = stack.clone();
                break;
            }
        }

        if (sample == null) {
            return fail(player, "§cPut the items you want to sell inside the chest first.");
        }

        for (ItemStack stack : chest.getBlockInventory().getContents()) {
            if (stack != null && !stack.getType().isAir() &&
                    !stack.isSimilar(sample)) {
                return fail(player, "§cA chest shop can only contain one item type.");
            }
        }

        String id = UUID.randomUUID().toString();

        Listing listing = new Listing(
                id,
                player.getUniqueId(),
                ShopType.CHEST,
                sample,
                price,
                null
        );

        Location location = block.getLocation();

        listing.world = location.getWorld().getName();
        listing.x = location.getBlockX();
        listing.y = location.getBlockY();
        listing.z = location.getBlockZ();

        listings.put(id, listing);
        saveListings();

        player.sendMessage("§aChest shop registered successfully!");
        play(player, "shop-success");

        return true;
    }

    private boolean canCreate(Player player, double price, ItemStack item) {
        if (!plugin.getConfig().getBoolean("modules.playershops.enabled", true)) {
            return fail(player, "§cPlayer shops are disabled.");
        }

        if (!Double.isFinite(price) || price < MIN_PRICE || price > MAX_PRICE) {
            return fail(player, "§cInvalid shop price.");
        }

        if (item != null && item.getType().isAir()) {
            return fail(player, "§cHold an item to create a virtual shop.");
        }

        int maximum = plugin.getConfig().getInt(
                "playershops.max-listings-per-player",
                MAX_LISTINGS
        );

        if (getListings(player.getUniqueId()).size() >= maximum) {
            return fail(player, "§cYou have reached your shop listing limit.");
        }

        return true;
    }

    public boolean restock(Player player, String id, int requestedAmount) {
        Listing listing = listings.get(id);

        if (!owns(player, listing) || requestedAmount <= 0) {
            return false;
        }

        if (lockedListings.contains(id)) {
            return fail(player, "§cThis listing is currently busy.");
        }

        int available = countPlayerItems(player, listing.item);

        if (available <= 0) {
            return fail(player, "§cYou don't have the required item.");
        }

        int amount = Math.min(requestedAmount, available);

        if (listing.type == ShopType.VIRTUAL) {
            long current = listing.virtualStock == null
                    ? 0
                    : listing.virtualStock.getAmount();

            if (current + amount > Integer.MAX_VALUE) {
                return fail(player, "§cThe shop stock limit has been reached.");
            }

            ItemStack[] previousPlayerContents =
                    copyContents(player.getInventory().getStorageContents());

            if (!removePlayerItems(player, listing.item, amount)) {
                return fail(player, "§cCould not remove items from your inventory.");
            }

            if (listing.virtualStock == null) {
                listing.virtualStock = listing.item.clone();
                listing.virtualStock.setAmount(amount);
            } else {
                listing.virtualStock.setAmount((int) current + amount);
            }

            saveListings();
            player.sendMessage("§aShop restocked successfully.");
            play(player, "shop-success");
            return true;
        }

        Inventory chest = getChestInventory(listing);

        if (chest == null) {
            return fail(player, "§cYour registered chest is unavailable.");
        }

        ItemStack transfer = listing.item.clone();
        transfer.setAmount(amount);

        ItemStack[] previousPlayerContents =
                copyContents(player.getInventory().getStorageContents());
        ItemStack[] previousChestContents =
                copyContents(chest.getContents());

        if (!removePlayerItems(player, listing.item, amount)) {
            return fail(player, "§cCould not remove items from your inventory.");
        }

        Map<Integer, ItemStack> leftovers = chest.addItem(transfer);

        if (!leftovers.isEmpty()) {
            player.getInventory().setStorageContents(previousPlayerContents);
            chest.setContents(previousChestContents);
            return fail(player, "§cYour shop chest does not have enough space.");
        }

        saveListings();
        player.sendMessage("§aShop restocked successfully.");
        play(player, "shop-success");

        return true;
    }

    public boolean withdraw(Player player, String id, int requestedAmount) {
        Listing listing = listings.get(id);

        if (!owns(player, listing) || requestedAmount <= 0) {
            return false;
        }

        if (lockedListings.contains(id)) {
            return fail(player, "§cThis listing is currently busy.");
        }

        int available = getStock(listing);

        if (available <= 0) {
            return fail(player, "§cYour shop is out of stock.");
        }

        int amount = Math.min(requestedAmount, available);

        ItemStack withdrawn = listing.item.clone();
        withdrawn.setAmount(amount);

        ItemStack[] previousPlayerContents =
                copyContents(player.getInventory().getStorageContents());

        if (listing.type == ShopType.VIRTUAL) {
            ItemStack previousStock = listing.virtualStock == null
                    ? null
                    : listing.virtualStock.clone();

            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(withdrawn);

            if (!leftovers.isEmpty()) {
                player.getInventory().setStorageContents(previousPlayerContents);
                return fail(player, "§cYour inventory does not have enough space.");
            }

            if (!removeVirtualStock(listing, amount)) {
                player.getInventory().setStorageContents(previousPlayerContents);
                listing.virtualStock = previousStock;
                return fail(player, "§cStock changed. Please try again.");
            }
        } else {
            Inventory chest = getChestInventory(listing);

            if (chest == null) {
                return fail(player, "§cYour shop chest is unavailable.");
            }

            ItemStack[] previousChestContents =
                    copyContents(chest.getContents());

            if (!removeItems(chest, listing.item, amount)) {
                return fail(player, "§cCould not withdraw the requested stock.");
            }

            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(withdrawn);

            if (!leftovers.isEmpty()) {
                player.getInventory().setStorageContents(previousPlayerContents);
                chest.setContents(previousChestContents);
                return fail(player, "§cYour inventory does not have enough space.");
            }
        }

        saveListings();
        player.sendMessage("§aStock withdrawn successfully.");
        play(player, "shop-success");

        return true;
    }

    public boolean buyListing(Player buyer, String id, int quantity) {
        Listing listing = listings.get(id);

        if (listing == null || !listing.enabled || quantity <= 0) {
            return fail(buyer, "§cThis listing is unavailable.");
        }

        if (!isValidQuantity(quantity)) {
            return fail(buyer, "§cInvalid purchase quantity.");
        }

        if (!plugin.getConfig().getBoolean("modules.playershops.enabled", true)) {
            return fail(buyer, "§cPlayer shops are disabled.");
        }

        if (buyer.getUniqueId().equals(listing.owner) &&
                !plugin.getConfig().getBoolean(
                        "playershops.allow-self-purchases", false)) {
            return fail(buyer, "§cYou cannot purchase your own listing.");
        }

        if (!Double.isFinite(listing.price) ||
                listing.price < MIN_PRICE ||
                listing.price > MAX_PRICE) {
            return fail(buyer, "§cThis listing has an invalid price.");
        }

        double total = listing.price * quantity;

        if (!Double.isFinite(total) || !economy.canUseAmount(total)) {
            return fail(buyer, "§cInvalid transaction amount.");
        }

        if (lockedListings.contains(id)) {
            return fail(buyer, "§cThis listing is currently being purchased.");
        }

        if (getStock(listing) < quantity) {
            return fail(buyer, "§cThis listing does not have enough stock.");
        }

        if (!economy.canAfford(buyer, total)) {
            return fail(buyer, "§cYou don't have enough money.");
        }

        ItemStack reward = listing.item.clone();
        reward.setAmount(quantity);

        ItemStack[] previousPlayerContents =
                copyContents(buyer.getInventory().getStorageContents());

        ItemStack previousVirtualStock = listing.virtualStock == null
                ? null
                : listing.virtualStock.clone();

        Inventory chest = listing.type == ShopType.CHEST
                ? getChestInventory(listing)
                : null;

        if (listing.type == ShopType.CHEST && chest == null) {
            return fail(buyer, "§cThis shop chest is unavailable.");
        }

        ItemStack[] previousChestContents = chest == null
                ? null
                : copyContents(chest.getContents());

        lockedListings.add(id);

        boolean buyerDebited = false;
        boolean sellerCredited = false;
        boolean stockRemoved = false;

        try {
            if (!economy.removePlayerBalance(buyer, total)) {
                return fail(buyer, "§cPayment could not be processed.");
            }

            buyerDebited = true;

            if (listing.type == ShopType.VIRTUAL) {
                stockRemoved = removeVirtualStock(listing, quantity);
            } else {
                stockRemoved = removeItems(chest, listing.item, quantity);
            }

            if (!stockRemoved) {
                rollbackPurchase(
                        buyer,
                        listing,
                        previousPlayerContents,
                        previousVirtualStock,
                        chest,
                        previousChestContents,
                        buyerDebited,
                        false,
                        total
                );

                buyerDebited = false;
                return fail(buyer, "§cStock changed. Your money has been refunded.");
            }

            Map<Integer, ItemStack> leftovers =
                    buyer.getInventory().addItem(reward);

            if (!leftovers.isEmpty()) {
                rollbackPurchase(
                        buyer,
                        listing,
                        previousPlayerContents,
                        previousVirtualStock,
                        chest,
                        previousChestContents,
                        buyerDebited,
                        false,
                        total
                );

                buyerDebited = false;
                return fail(buyer, "§cYour inventory could not accept the items. Your money has been refunded.");
            }

            boolean paid = economy.addOfflinePlayerBalance(
                    Bukkit.getOfflinePlayer(listing.owner),
                    total
            );

            if (!paid) {
                rollbackPurchase(
                        buyer,
                        listing,
                        previousPlayerContents,
                        previousVirtualStock,
                        chest,
                        previousChestContents,
                        buyerDebited,
                        false,
                        total
                );

                buyerDebited = false;
                return fail(buyer, "§cSeller payment failed. Your money has been refunded.");
            }

            sellerCredited = true;
            saveListings();

            buyer.sendMessage("§aPurchased §f" + quantity + "x " +
                    listing.item.getType().name() + " §afor §f$" +
                    economy.formatMoney(total) + "§a.");

            Player seller = Bukkit.getPlayer(listing.owner);

            if (seller != null) {
                seller.sendMessage("§aYour shop sold §f" + quantity + "x " +
                        listing.item.getType().name() + " §afor §f$" +
                        economy.formatMoney(total) + "§a.");
                play(seller, "shop-success");
            }

            play(buyer, "shop-success");

            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Player shop transaction failed for listing " + id + ": " +
                            exception.getMessage()
            );

            if (!sellerCredited) {
                rollbackPurchase(
                        buyer,
                        listing,
                        previousPlayerContents,
                        previousVirtualStock,
                        chest,
                        previousChestContents,
                        buyerDebited,
                        false,
                        total
                );
            }

            return fail(buyer, "§cThe transaction encountered an error.");
        } finally {
            lockedListings.remove(id);
        }
    }

    private void rollbackPurchase(
            Player buyer,
            Listing listing,
            ItemStack[] previousPlayerContents,
            ItemStack previousVirtualStock,
            Inventory chest,
            ItemStack[] previousChestContents,
            boolean buyerDebited,
            boolean sellerCredited,
            double total
    ) {
        buyer.getInventory().setStorageContents(previousPlayerContents);

        if (listing.type == ShopType.VIRTUAL) {
            listing.virtualStock = previousVirtualStock == null
                    ? null
                    : previousVirtualStock.clone();
        } else if (chest != null && previousChestContents != null) {
            chest.setContents(previousChestContents);
        }

        if (buyerDebited && !sellerCredited) {
            if (!economy.addPlayerBalance(buyer, total)) {
                plugin.getLogger().severe(
                        "CRITICAL: Could not refund " + buyer.getName() +
                                " $" + total + " after a failed player shop transaction."
                );
                buyer.sendMessage(
                        "§cCRITICAL: Your refund could not be processed. Contact an administrator."
                );
            }
        }

        saveListings();
    }

    private boolean isValidQuantity(int quantity) {
        for (int allowed : PURCHASE_QUANTITIES) {
            if (quantity == allowed) return true;
        }

        return false;
    }

    private boolean removeVirtualStock(Listing listing, int amount) {
        if (listing.virtualStock == null ||
                listing.virtualStock.getAmount() < amount) {
            return false;
        }

        int remaining = listing.virtualStock.getAmount() - amount;

        if (remaining <= 0) {
            listing.virtualStock = null;
        } else {
            listing.virtualStock.setAmount(remaining);
        }

        return true;
    }

    private boolean addVirtualStock(Listing listing, int amount) {
        if (amount <= 0) return false;

        long current = listing.virtualStock == null
                ? 0
                : listing.virtualStock.getAmount();

        if (current + amount > Integer.MAX_VALUE) return false;

        if (listing.virtualStock == null) {
            listing.virtualStock = listing.item.clone();
            listing.virtualStock.setAmount(amount);
        } else {
            listing.virtualStock.setAmount((int) current + amount);
        }

        return true;
    }

    public boolean setPrice(Player player, String id, double price) {
        Listing listing = listings.get(id);

        if (!owns(player, listing)) return false;

        if (!Double.isFinite(price) || price < MIN_PRICE || price > MAX_PRICE) {
            return fail(player, "§cInvalid listing price.");
        }

        if (lockedListings.contains(id)) {
            return fail(player, "§cThis listing is currently busy.");
        }

        listing.price = price;
        saveListings();

        player.sendMessage("§aListing price updated to $" +
                economy.formatMoney(price) + ".");
        play(player, "shop-success");

        return true;
    }

    public boolean setEnabled(Player player, String id, boolean enabled) {
        Listing listing = listings.get(id);

        if (!owns(player, listing)) return false;

        if (lockedListings.contains(id)) {
            return fail(player, "§cThis listing is currently busy.");
        }

        listing.enabled = enabled;
        saveListings();

        player.sendMessage(enabled
                ? "§aYour listing is now active."
                : "§eYour listing has been disabled.");

        play(player, "shop-success");

        return true;
    }

    public boolean deleteListing(Player player, String id) {
        Listing listing = listings.get(id);

        if (!owns(player, listing)) return false;

        if (lockedListings.contains(id)) {
            return fail(player, "§cThis listing is currently busy.");
        }

        if (listing.type == ShopType.VIRTUAL &&
                listing.virtualStock != null &&
                listing.virtualStock.getAmount() > 0) {

            ItemStack stock = listing.virtualStock.clone();

            ItemStack[] previousPlayerContents =
                    copyContents(player.getInventory().getStorageContents());

            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(stock);

            if (!leftovers.isEmpty()) {
                player.getInventory().setStorageContents(previousPlayerContents);
                return fail(player, "§cMake room in your inventory before deleting this shop.");
            }
        }

        listings.remove(id);
        saveListings();

        player.sendMessage("§aShop listing deleted.");
        play(player, "shop-success");

        return true;
    }

    public boolean owns(Player player, Listing listing) {
        return player != null &&
                listing != null &&
                listing.owner.equals(player.getUniqueId());
    }

    private int countPlayerItems(Player player, ItemStack reference) {
        int amount = 0;

        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.isSimilar(reference)) {
                amount += stack.getAmount();
            }
        }

        return amount;
    }

    private boolean removePlayerItems(Player player, ItemStack reference, int amount) {
        if (amount <= 0 || countPlayerItems(player, reference) < amount) {
            return false;
        }

        int remaining = amount;
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getStorageContents().length; slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack == null || !stack.isSimilar(reference)) continue;

            int removed = Math.min(remaining, stack.getAmount());
            int newAmount = stack.getAmount() - removed;

            if (newAmount <= 0) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(newAmount);
                inventory.setItem(slot, stack);
            }

            remaining -= removed;

            if (remaining <= 0) return true;
        }

        return false;
    }

    private ItemStack[] copyContents(ItemStack[] contents) {
        ItemStack[] copy = new ItemStack[contents.length];

        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null
                    ? null
                    : contents[i].clone();
        }

        return copy;
    }

    private boolean canFit(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null || item.getType().isAir()) {
            return false;
        }

        int remaining = item.getAmount();
        int maximumStack = Math.min(
                item.getMaxStackSize(),
                inventory.getMaxStackSize()
        );

        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                remaining -= maximumStack;
            } else if (stack.isSimilar(item)) {
                remaining -= Math.max(
                        0,
                        Math.min(maximumStack, stack.getMaxStackSize()) -
                                stack.getAmount()
                );
            }

            if (remaining <= 0) return true;
        }

        return false;
    }

    private boolean removeItems(Inventory inventory, ItemStack reference, int amount) {
        if (inventory == null || amount <= 0 ||
                countItems(inventory, reference) < amount) {
            return false;
        }

        int remaining = amount;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack == null || !stack.isSimilar(reference)) continue;

            int removed = Math.min(remaining, stack.getAmount());
            int newAmount = stack.getAmount() - removed;

            if (newAmount <= 0) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(newAmount);
                inventory.setItem(slot, stack);
            }

            remaining -= removed;

            if (remaining <= 0) return true;
        }

        return false;
    }

    private int countItems(Inventory inventory, ItemStack reference) {
        if (inventory == null || reference == null) return 0;

        int amount = 0;

        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.isSimilar(reference)) {
                amount += stack.getAmount();
            }
        }

        return amount;
    }

    private Inventory getChestInventory(Listing listing) {
        if (listing.world == null) return null;

        World world = Bukkit.getWorld(listing.world);
        if (world == null) return null;

        Block block = world.getBlockAt(listing.x, listing.y, listing.z);

        if (!(block.getState() instanceof Chest chest)) return null;

        return chest.getBlockInventory();
    }

    private boolean isSameChest(Listing listing, Location location) {
        return listing.world != null &&
                location.getWorld() != null &&
                listing.world.equals(location.getWorld().getName()) &&
                listing.x == location.getBlockX() &&
                listing.y == location.getBlockY() &&
                listing.z == location.getBlockZ();
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null || !(block.getState() instanceof Chest)) return;

        for (Listing listing : listings.values()) {
            if (listing.type != ShopType.CHEST ||
                    !isSameChest(listing, block.getLocation())) {
                continue;
            }

            if (listing.owner.equals(player.getUniqueId())) {
                return;
            }

            event.setCancelled(true);

            if (!listing.enabled) {
                player.sendMessage("§cThis shop is currently disabled.");
                play(player, "shop-error");
                return;
            }

            shopGUI.openPlayerPurchaseDialog(player, listing.id, this);
            return;
        }
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        String removeId = null;

        for (Listing listing : listings.values()) {
            if (listing.type != ShopType.CHEST ||
                    !isSameChest(listing, event.getBlock().getLocation())) {
                continue;
            }

            if (!listing.owner.equals(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(
                        "§cYou cannot break someone else's shop chest."
                );
                play(event.getPlayer(), "shop-error");
                return;
            }

            removeId = listing.id;
            break;
        }

        if (removeId != null) {
            listings.remove(removeId);
            saveListings();

            event.getPlayer().sendMessage(
                    "§eYour chest shop has been removed."
            );
        }
    }

    private boolean fail(Player player, String message) {
        player.sendMessage(message);
        play(player, "shop-error");
        return false;
    }

    private void play(Player player, String sound) {
        if (sounds != null) {
            sounds.play(player, sound);
        }
    }

    private void loadListings() {
        ConfigurationSection section = data.getConfigurationSection("listings");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String path = "listings." + id;

            try {
                UUID owner = UUID.fromString(
                        data.getString(path + ".owner")
                );

                ShopType type = ShopType.valueOf(
                        data.getString(path + ".type", "VIRTUAL")
                );

                ItemStack item = data.getItemStack(path + ".item");
                double price = data.getDouble(path + ".price");

                if (item == null ||
                        item.getType().isAir() ||
                        !Double.isFinite(price) ||
                        price < MIN_PRICE ||
                        price > MAX_PRICE) {
                    continue;
                }

                ItemStack stock = data.getItemStack(path + ".stock");

                if (type == ShopType.VIRTUAL &&
                        stock != null &&
                        (stock.getType().isAir() || !stock.isSimilar(item))) {
                    continue;
                }

                Listing listing = new Listing(
                        id,
                        owner,
                        type,
                        item,
                        price,
                        stock
                );

                listing.enabled = data.getBoolean(
                        path + ".enabled",
                        true
                );

                listing.world = data.getString(path + ".chest.world");
                listing.x = data.getInt(path + ".chest.x");
                listing.y = data.getInt(path + ".chest.y");
                listing.z = data.getInt(path + ".chest.z");

                if (type == ShopType.CHEST && listing.world == null) {
                    continue;
                }

                listings.put(id, listing);
            } catch (Exception exception) {
                plugin.getLogger().warning(
                        "Could not load player shop listing " + id + ": " +
                                exception.getMessage()
                );
            }
        }
    }

    private void saveListings() {
        data.set("listings", null);

        for (Listing listing : listings.values()) {
            String path = "listings." + listing.id;

            data.set(path + ".owner", listing.owner.toString());
            data.set(path + ".type", listing.type.name());
            data.set(path + ".item", listing.item);
            data.set(path + ".price", listing.price);
            data.set(path + ".enabled", listing.enabled);

            if (listing.type == ShopType.VIRTUAL) {
                data.set(path + ".stock", listing.virtualStock);
            } else {
                data.set(path + ".chest.world", listing.world);
                data.set(path + ".chest.x", listing.x);
                data.set(path + ".chest.y", listing.y);
                data.set(path + ".chest.z", listing.z);
            }
        }

        try {
            data.save(listingsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Could not save player shop listings: " +
                            exception.getMessage()
            );
        }
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(listingsFile);
        listings.clear();
        lockedListings.clear();
        loadListings();
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use player shops.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("modules.playershops.enabled", true)) {
            return fail(player, "§cPlayer shops are disabled.");
        }

        if (args.length == 0) {
            shopGUI.openMarketplace(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "manage" -> {
                shopGUI.openManage(player);
                return true;
            }

            case "price" -> {
                if (args.length != 3) {
                    player.sendMessage("§eUsage: /pshop price <listing-id> <price>");
                    return true;
                }

                double price;

                try {
                    price = Double.parseDouble(args[2]);
                } catch (NumberFormatException exception) {
                    return fail(player, "§cInvalid price.");
                }

                setPrice(player, args[1], price);
                return true;
            }

            case "create" -> {
                if (args.length != 3) {
                    player.sendMessage("§eUsage: /pshop create <virtual|chest> <price>");
                    return true;
                }

                double price;

                try {
                    price = Double.parseDouble(args[2]);
                } catch (NumberFormatException exception) {
                    return fail(player, "§cInvalid price.");
                }

                switch (args[1].toLowerCase()) {
                    case "virtual" -> createVirtualShop(
                            player,
                            player.getInventory().getItemInMainHand(),
                            price
                    );

                    case "chest" -> {
                        Block target = player.getTargetBlockExact(5);

                        if (target == null) {
                            return fail(player, "§cLook at a chest within 5 blocks.");
                        }

                        createChestShop(player, target, price);
                    }

                    default -> player.sendMessage(
                            "§cShop type must be virtual or chest."
                    );
                }

                return true;
            }

            default -> {
                player.sendMessage("§6§lPlayer Shops");
                player.sendMessage("§e/pshop §7- Browse the marketplace");
                player.sendMessage("§e/pshop manage §7- Manage your shops");
                player.sendMessage("§e/pshop price <id> <price> §7- Change listing price");
                player.sendMessage("§e/pshop create <virtual|chest> <price> §7- Create a shop");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length == 1) {
            result.add("manage");
            result.add("create");
            result.add("price");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                result.add("virtual");
                result.add("chest");
            } else if (args[0].equalsIgnoreCase("price") &&
                    sender instanceof Player player) {
                for (Listing listing : getListings(player.getUniqueId())) {
                    result.add(listing.getId());
                }
            }
        } else if (args.length == 3 &&
                args[0].equalsIgnoreCase("price")) {
            result.add("10");
            result.add("100");
            result.add("1000");
        }

        String input = args[args.length - 1].toLowerCase();

        result.removeIf(value ->
                !value.toLowerCase().startsWith(input)
        );

        return result;
    }

}
