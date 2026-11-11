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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ssmpcontractsgui implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int CONTRACT_SLOTS = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int PAGE_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final ssmpcontracts contracts;
    private final File folder;
    private final File configFile;
    private final YamlConfiguration config;

    public ssmpcontractsgui(ssmpcontracts contracts) {
        this.contracts = contracts;

        JavaPlugin plugin = contracts.getPlugin();

        folder = new File(
                plugin.getDataFolder(),
                "contractsgui"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create contractsgui directory."
            );
        }

        configFile = new File(folder, "config.yml");

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException(
                            "Could not create contractsgui config.yml."
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to create contractsgui config.yml.",
                        e
                );
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        config.addDefault("gui.title", "&8SSMP Contracts");
        config.addDefault("gui.size", INVENTORY_SIZE);
        config.addDefault("gui.contract-slots", CONTRACT_SLOTS);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to save contractsgui config.yml.",
                    e
            );
        }
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        if (player == null) {
            return;
        }

        List<String> contractList = contracts.getContracts();

        int size = config.getInt("gui.size", INVENTORY_SIZE);

        if (size != INVENTORY_SIZE) {
            size = INVENTORY_SIZE;
        }

        int contractSlots = Math.max(
                1,
                Math.min(
                        config.getInt("gui.contract-slots", CONTRACT_SLOTS),
                        CONTRACT_SLOTS
                )
        );

        int totalPages = Math.max(
                1,
                (int) Math.ceil(
                        contractList.size() / (double) contractSlots
                )
        );

        page = Math.max(0, Math.min(page, totalPages - 1));

        Holder holder = new Holder(page);

        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                ChatColor.translateAlternateColorCodes(
                        '&',
                        config.getString(
                                "gui.title",
                                "&8SSMP Contracts"
                        )
                )
        );

        holder.setInventory(inventory);

        int start = page * contractSlots;
        int end = Math.min(
                start + contractSlots,
                contractList.size()
        );

        for (int i = start; i < end; i++) {
            String id = contractList.get(i);
            int slot = i - start;

            inventory.setItem(
                    slot,
                    createContractItem(player, id)
            );

            holder.getSlots().put(slot, id);
        }

        if (page > 0) {
            inventory.setItem(
                    PREVIOUS_SLOT,
                    createItem(
                            Material.ARROW,
                            ChatColor.YELLOW + "Previous Page",
                            List.of(
                                    ChatColor.GRAY + "Go to page " + page
                            )
                    )
            );
        }

        inventory.setItem(
                PAGE_SLOT,
                createItem(
                        Material.PAPER,
                        ChatColor.GOLD + "Page " + (page + 1),
                        List.of(
                                ChatColor.GRAY + "Page " + (page + 1)
                                        + " of " + totalPages,
                                ChatColor.GRAY + "Contracts: "
                                        + contractList.size()
                        )
                )
        );

        if (page < totalPages - 1) {
            inventory.setItem(
                    NEXT_SLOT,
                    createItem(
                            Material.ARROW,
                            ChatColor.YELLOW + "Next Page",
                            List.of(
                                    ChatColor.GRAY + "Go to page "
                                            + (page + 2)
                            )
                    )
            );
        }

        player.openInventory(inventory);
    }

    private ItemStack createContractItem(Player player, String id) {
        Material material;

        if (contracts.isCompleted(player, id)) {
            material = Material.LIME_STAINED_GLASS_PANE;
        } else if (contracts.isAccepted(player, id)) {
            material = Material.YELLOW_STAINED_GLASS_PANE;
        } else {
            material = Material.WRITABLE_BOOK;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                ChatColor.translateAlternateColorCodes(
                        '&',
                        contracts.getContractName(id)
                )
        );

        List<String> lore = new ArrayList<>();

        lore.add(
                ChatColor.GRAY + contracts.getContractDescription(id)
        );

        lore.add("");
        lore.add(
                ChatColor.GRAY + "Difficulty: "
                        + contracts.getDifficulty(id)
        );
        lore.add("");

        for (int i = 0; i < contracts.getTaskCount(id); i++) {
            int progress = contracts.getTaskProgress(player, id, i);
            int required = contracts.getTaskAmount(id, i);

            ChatColor color = progress >= required
                    ? ChatColor.GREEN
                    : ChatColor.YELLOW;

            lore.add(
                    ChatColor.GRAY + contracts.getTaskName(id, i)
                            + " " + color + progress + "/" + required
            );
        }

        lore.add("");

        if (contracts.isCompleted(player, id)) {
            lore.add(ChatColor.GREEN + "Completed");
        } else if (contracts.isAccepted(player, id)) {
            lore.add(ChatColor.YELLOW + "Already accepted");
        } else {
            lore.add(ChatColor.GREEN + "Click to accept");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createItem(
            Material material,
            String name,
            List<String> lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof Holder holder)) {
            return;
        }

        int rawSlot = event.getRawSlot();

        if (rawSlot < 0
                || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        event.setCancelled(true);

        if (rawSlot == PREVIOUS_SLOT) {
            if (holder.getPage() > 0) {
                open(player, holder.getPage() - 1);
            }
            return;
        }

        if (rawSlot == NEXT_SLOT) {
            open(player, holder.getPage() + 1);
            return;
        }

        String id = holder.getSlots().get(rawSlot);

        if (id == null) {
            return;
        }

        if (contracts.acceptContract(player, id)) {
            player.sendMessage(
                    ChatColor.GREEN + "You accepted "
                            + ChatColor.translateAlternateColorCodes(
                            '&',
                            contracts.getContractName(id)
                    ) + "."
            );

            open(player, holder.getPage());
        } else if (contracts.isAccepted(player, id)) {
            player.sendMessage(
                    ChatColor.YELLOW
                            + "You have already accepted this contract."
            );
        } else if (contracts.isCompleted(player, id)) {
            player.sendMessage(
                    ChatColor.GREEN
                            + "You have already completed this contract."
            );
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    private static class Holder implements InventoryHolder {

        private final int page;
        private final Map<Integer, String> slots = new HashMap<>();
        private Inventory inventory;

        private Holder(int page) {
            this.page = page;
        }

        public int getPage() {
            return page;
        }

        public Map<Integer, String> getSlots() {
            return slots;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}