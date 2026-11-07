/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ssmpenderchest implements Listener {

    private final ssmp plugin;

    public ssmpenderchest(ssmp plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (!plugin.getConfig().getBoolean(
                "modules.enderchest.enabled",
                true
        )) {
            player.sendMessage(
                    ChatColor.RED + "The Ender Chest system is disabled."
            );
            return;
        }

        player.openInventory(player.getEnderChest());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null
                || event.getClickedBlock().getType().name().equals("ENDER_CHEST") == false) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "modules.enderchest.enabled",
                true
        )) {
            return;
        }

        event.setCancelled(true);
        open(event.getPlayer());
    }
}