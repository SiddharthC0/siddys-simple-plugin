
package org.siddharth.ssmp.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.io.File;
import java.util.Map;
public class MenuItemSystem implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey itemKey;

    private File configFile;
    private YamlConfiguration config;

    public MenuItemSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "menu_item");

        loadConfig();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "menuitem");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        configFile = new File(folder, "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("enabled", true);
        config.addDefault("material", "COMPASS");
        config.addDefault("name", "&6&lSSMP Menu");
        config.addDefault("slot", 8);
        config.addDefault("command", "ssmp");
        config.addDefault("lore", java.util.List.of(
                "&7Right-click to open the SSMP menu"
        ));

        config.options().copyDefaults(true);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Could not save menu item configuration: " + exception.getMessage()
            );
        }
    }

    public void reload() {
        loadConfig();

        if (isEnabled()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                giveMenuItem(player);
            }
        }
    }

    private boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    private int getMenuSlot() {
        return Math.max(0, Math.min(8, config.getInt("slot", 8)));
    }

    private Material getMaterial() {
        Material material = Material.matchMaterial(
                config.getString("material", "COMPASS")
        );

        return material == null ? Material.COMPASS : material;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private ItemStack createItem() {
        ItemStack item = new ItemStack(getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(config.getString("name", "&6&lSSMP Menu")));

        java.util.List<String> lore = config.getStringList("lore");

        if (!lore.isEmpty()) {
            meta.setLore(lore.stream().map(this::color).toList());
        }
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(
                itemKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
        return item;
    }

    private boolean isMenuItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return meta != null
                && meta.getPersistentDataContainer().has(
                itemKey,
                PersistentDataType.BYTE
        );
    }

    private boolean hasMenuItem(PlayerInventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (isMenuItem(item)) {
                return true;
            }
        }

        return false;
    }

    private void giveMenuItem(Player player) {
        if (!isEnabled()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        int slot = getMenuSlot();

        ItemStack displaced = inventory.getItem(slot);

        if (isMenuItem(displaced)) {
            displaced = null;
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (isMenuItem(inventory.getItem(i))) {
                inventory.setItem(i, null);
            }
        }

        inventory.setItem(slot, createItem());

        if (displaced != null && !displaced.getType().isAir()) {
            Map<Integer, ItemStack> overflow = inventory.addItem(displaced);

            for (ItemStack remaining : overflow.values()) {
                player.getWorld().dropItemNaturally(
                        player.getLocation(),
                        remaining
                );
            }
        }

        player.updateInventory();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                giveMenuItem(event.getPlayer());
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                giveMenuItem(event.getPlayer());
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isEnabled()) {
            return;
        }

        event.getDrops().removeIf(this::isMenuItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isEnabled() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isMenuItem(current) || isMenuItem(cursor)) {
            event.setCancelled(true);
            return;
        }
        if (event.getHotbarButton() == getMenuSlot()) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getInventory())
                && event.getSlot() == getMenuSlot()) {
            event.setCancelled(true);
            return;
        }


    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (isMenuItem(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        int bottomSize = event.getView().getBottomInventory().getSize(); // typically 36
        int hotbarOffset = topSize + (bottomSize - 9); // hotbar starts after main storage
        int protectedSlot = getMenuSlot();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= hotbarOffset && rawSlot < hotbarOffset + 9) {
                int hotbarSlot = rawSlot - hotbarOffset;

                if (hotbarSlot == protectedSlot) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (isMenuItem(event.getMainHandItem())
                || isMenuItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (isMenuItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (isMenuItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (isMenuItem(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!isMenuItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);

        String command = config.getString("command", "ssmp");

        if (command == null || command.isBlank()) {
            return;
        }

        command = command.replace(
                "%player%",
                event.getPlayer().getName()
        );

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        event.getPlayer().performCommand(command);
    }

}
