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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ssmpkitgui implements Listener {

    private final ssmp plugin;
    private final ssmpkits kits;
    private final ssmpcore core;

    private static final int[] KIT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ssmpkitgui(ssmp plugin, ssmpkits kits) {
        this.plugin = plugin;
        this.kits = kits;
        this.core = plugin.getCore();
    }

    public void open(Player player, int page) {
        if (!kits.isEnabled()) {
            core.sendMessage(player, "&cThe kits module is currently disabled.");
            return;
        }

        List<String> names = kits.getKitNames();
        int maxPages = Math.max(1, (int) Math.ceil(names.size() / (double) KIT_SLOTS.length));
        page = Math.max(0, Math.min(page, maxPages - 1));

        KitMenuHolder holder = new KitMenuHolder(page);
        String title = core.getConfigString("kits", "settings.gui-title", "&8SSMP | Kits");

        Inventory inventory = core.createGui(holder, 54, title);
        holder.setInventory(inventory);

        ItemStack border = core.createGuiItem(
                Material.BLACK_STAINED_GLASS_PANE,
                1,
                " ",
                List.of()
        );

        for (int slot = 0; slot < 9; slot++) {
            core.setGuiItem(inventory, slot, border);
        }

        for (int slot = 45; slot < 54; slot++) {
            core.setGuiItem(inventory, slot, border);
        }

        int start = page * KIT_SLOTS.length;
        int end = Math.min(start + KIT_SLOTS.length, names.size());

        for (int index = start; index < end; index++) {
            String name = names.get(index);
            int slot = KIT_SLOTS[index - start];

            Material material = Material.matchMaterial(kits.getIcon(name));

            if (material == null || material.isAir()) {
                material = Material.CHEST;
            }

            boolean unlocked = kits.canUseKit(player, name);
            long cooldown = kits.getRemainingCooldown(player, name);

            List<String> lore = new ArrayList<>(kits.getDescription(name));
            lore.add("");

            if (!unlocked) {
                lore.add("&cYou don't have permission to use this kit.");
                material = Material.BARRIER;
            } else if (cooldown > 0) {
                lore.add("&cCooldown: &f" + formatTime(cooldown));
            } else {
                lore.add("&aAvailable to claim!");
            }

            lore.add("&7Permission: &f" + kits.getKitPermission(name));
            lore.add("");
            lore.add(unlocked ? "&eClick to claim!" : "&cLocked");

            ItemStack item = core.createGuiItem(
                    material,
                    1,
                    "&b" + kits.getDisplayName(name),
                    lore
            );

            core.setGuiItem(inventory, slot, item);
        }

        core.setGuiItem(
                inventory,
                45,
                core.createGuiItem(
                        Material.ARROW,
                        "&ePrevious Page",
                        "&7Go to the previous page."
                )
        );

        core.setGuiItem(
                inventory,
                49,
                core.createGuiItem(
                        Material.PAPER,
                        "&bPage " + (page + 1) + "/" + maxPages,
                        "&7Available kits: &f" + names.size()
                )
        );

        core.setGuiItem(
                inventory,
                53,
                core.createGuiItem(
                        Material.ARROW,
                        "&eNext Page",
                        "&7Go to the next page."
                )
        );

        core.setGuiItem(
                inventory,
                48,
                core.createGuiItem(
                        Material.BARRIER,
                        "&cClose",
                        "&7Close the kits menu."
                )
        );

        core.openGui(player, inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof KitMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != top) {
            return;
        }

        int slot = event.getRawSlot();
        int page = holder.getPage();

        if (slot == 45) {
            open(player, page - 1);
            return;
        }

        if (slot == 53) {
            open(player, page + 1);
            return;
        }

        if (slot == 48) {
            core.closeGui(player);
            return;
        }

        int kitIndex = -1;

        for (int index = 0; index < KIT_SLOTS.length; index++) {
            if (KIT_SLOTS[index] == slot) {
                kitIndex = index;
                break;
            }
        }

        if (kitIndex == -1) {
            return;
        }

        List<String> names = kits.getKitNames();
        int index = page * KIT_SLOTS.length + kitIndex;

        if (index < 0 || index >= names.size()) {
            return;
        }

        String name = names.get(index);
        ssmpkits.KitResult result = kits.giveKit(player, name);

        switch (result) {
            case SUCCESS -> {
                core.sendMessage(player, "&aYou claimed the &f" + kits.getDisplayName(name) + " &akit!");
                core.closeGui(player);
            }

            case NOT_FOUND ->
                    core.sendMessage(player, "&cThat kit no longer exists.");

            case NO_PERMISSION ->
                    core.sendMessage(player, "&cYou don't have permission to use this kit.");

            case COOLDOWN ->
                    core.sendMessage(player, "&cThis kit is on cooldown for another &f"
                            + formatTime(kits.getRemainingCooldown(player, name)) + "&c.");

            case DISABLED ->
                    core.sendMessage(player, "&cThe kits module is currently disabled.");
        }
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + remainingSeconds + "s";
        }

        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        }

        return remainingSeconds + "s";
    }

    private static class KitMenuHolder implements InventoryHolder {

        private final int page;
        private Inventory inventory;

        private KitMenuHolder(int page) {
            this.page = page;
        }

        public int getPage() {
            return page;
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