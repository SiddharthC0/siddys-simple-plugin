package org.siddharth.ssmp.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.siddharth.ssmp.mechanics.InfoBarSystem;

import java.util.List;

public final class InfoBarGUI implements Listener {

    private final JavaPlugin plugin;
    private final InfoBarSystem system;

    public InfoBarGUI(JavaPlugin plugin, InfoBarSystem system) {
        this.plugin = plugin;
        this.system = system;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        boolean enabled = system.isPlayerEnabled(player);
        guiHolder holder = new guiHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, ChatColor.DARK_PURPLE + "InfoBar Settings");
        holder.setInventory(inventory);
        for (int i = 0; i < 27; i++) { inventory.setItem(i, filler()); }
        inventory.setItem(11, toggleItem(enabled));
        inventory.setItem(13, statusItem(enabled));
        inventory.setItem(15, closeItem());
        player.openInventory(inventory);
    }

    private ItemStack toggleItem(boolean enabled) {
        return buildItem(enabled ? Material.LIME_DYE : Material.RED_DYE, enabled ? "&aClick to Disable" : "&cClick to Enable", List.of("&7Toggles your personal InfoBar.", "", "&8Currently: " + (enabled ? "&aEnabled" : "&cDisabled")));
    }

    private ItemStack statusItem(boolean enabled) {
        return buildItem(enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, enabled ? "&aInfoBar is Active" : "&cInfoBar is Inactive", List.of("&7Your HUD display status."));
    }

    private ItemStack closeItem() {
        return buildItem(Material.BARRIER, "&cClose", List.of("&7Close this menu."));
    }

    private ItemStack filler() {
        return buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { return item; }
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(this::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof guiHolder)) { return; }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) { return; }
        if (event.getClickedInventory() != event.getView().getTopInventory()) { return; }
        if (event.getRawSlot() == 11) { system.togglePlayer(player); open(player); }
        if (event.getRawSlot() == 15) { player.closeInventory(); }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof guiHolder) { event.setCancelled(true); }
    }

    private static final class guiHolder implements InventoryHolder {
        private Inventory inventory;
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
