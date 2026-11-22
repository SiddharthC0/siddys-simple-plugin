package org.siddharth.ssmp.gui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.siddharth.ssmp.economy.Economy;
import org.siddharth.ssmp.mechanics.BountySystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class BountyGUI implements Listener {
    private final BountySystem bounties;
    private final Economy economy;
    private final File folder;
    private final File configFile;
    private final YamlConfiguration config;
    public BountyGUI(BountySystem bounties, Economy economy) {
        this.bounties = bounties;
        this.economy = economy;
        folder = new File(bounties.getPlugin().getDataFolder(), "bountygui");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Bounty folder could not be made!");
        }

        configFile = new File(folder, "config.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException("Bounty in config.yml could not be created...");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Bounty config couldn't be created due to exception: ", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("gui.title", "&4SSMP Bounties");
        config.addDefault("gui.size", 54);
        config.addDefault("gui.max-entries", 45);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("Bounty config couldn't be created because of exception: ", e);
        }
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        int size = config.getInt("gui.size", 54);
        if (size < 18 || size > 54 || size % 9 != 0) {
            size = 54;
        }

        int maxEntries = Math.max(1, Math.min(config.getInt("gui.max-entries", 45), size - 9));

        Holder gui = new Holder();
        Inventory inventory = Bukkit.createInventory(gui, size, ChatColor.translateAlternateColorCodes('&', config.getString("gui.title", "&4SSMP Bounties")));

        gui.setInventory(inventory);
        int slot = 0;
        for (UUID uuid : bounties.getBountyTargets()) {
            if (slot >= maxEntries) {
                break;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);

            // AI gave me this tenary so it could be wrong
            String name = target.getName() == null ? uuid.toString() : target.getName();
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }

            meta.setDisplayName(ChatColor.RED + name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current bounty:");

            // and i made this tenary myself :D
            lore.add(ChatColor.GOLD + (economy == null ? String.valueOf(bounties.getBounty(uuid)) : economy.formatMoney(bounties.getBounty(uuid))));
            lore.add("");
            lore.add(ChatColor.GRAY + "Target UID:");
            lore.add(ChatColor.DARK_GRAY + uuid.toString());

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot++, item);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder()
                instanceof Holder)) {
            return;
        }

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        event.setCancelled(true);
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    private static class Holder implements InventoryHolder {
        private Inventory inventory;
        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}