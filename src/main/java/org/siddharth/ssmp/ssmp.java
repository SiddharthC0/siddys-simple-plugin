/*
 * Siddy's Simple Plugin©
 * Copyright © 2026 Siddharth C
 * Licensed under the MIT License.
 *
 * This class is part of SSMP.
 */
package org.siddharth.ssmp;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class ssmp extends JavaPlugin {

    private ssmpranks ranks;
    private ssmppermissions permissions;
    private ssmpsoundsystem sounds;
    private ssmpplaceholders placeholders;
    private ssmptablist tablist;
    private ssmpscoreboard scoreboard;
    private ssmpauction auction;
    private ssmpeconomycommands economyCommands;
    private ssmpenderchest enderChest;
    private ssmphomes homes;
    private ssmpwarps warps;
    private ssmpmenu menu;
    private ssmpnicknames nicknames;
    private ssmpkits kits;
    private ssmpkitgui kitGui;
    private ssmpkitcommands kitCommands;
    private ssmpcore core;
    private ssmprtp rtp;
    private ssmpoptimization optimization;
    private ssmplevels levels;

    public ssmpcore getCore() {
        return core;
    }
    @Override
    public void onEnable() {
        createConfig();
        createConfig();

        optimization = new ssmpoptimization(this);
        optimization.initialize();
        core = new ssmpcore(this);

        ranks = new ssmpranks(this);
        permissions = new ssmppermissions(this, ranks);
        placeholders = new ssmpplaceholders(this, ranks);
        sounds = new ssmpsoundsystem(this);
        nicknames = new ssmpnicknames(this);

        menu = new ssmpmenu(this, sounds);

        getServer().getPluginManager().registerEvents(
                menu,
                this
        );

        ssmprankcommands rankCommands =
                new ssmprankcommands(
                        ranks,
                        permissions
                );

        if (getCommand("rank") != null) {
            getCommand("rank").setExecutor(rankCommands);
        }
        if (optimization.isModuleEnabled("levels")) {
            levels = new ssmplevels(this);
        } else {
            levels = null;
        }

        if (isEnabled("tablist")) {
            tablist = new ssmptablist(
                    this,
                    ranks
            );

            getServer().getPluginManager().registerEvents(
                    tablist,
                    this
            );

            optimization.registerShutdownAction(tablist::stop);
            tablist.start();
        }
        if (isEnabled("rtp")) {
            rtp = new ssmprtp(this);

            getServer().getPluginManager().registerEvents(
                    rtp,
                    this
            );

            if (getCommand("rtp") != null) {
                getCommand("rtp").setExecutor(rtp);
            }
        }

        if (isEnabled("kits")) {
            kits = new ssmpkits(this);
            kitGui = new ssmpkitgui(this, kits);
            kitCommands = new ssmpkitcommands(this, kits, kitGui);

            getServer().getPluginManager().registerEvents(kitGui, this);
            if (getCommand("kit") != null) {
                getCommand("kit").setExecutor(kitCommands);
                getCommand("kit").setTabCompleter(kitCommands);
            }
        }

        if (isEnabled("chat")) {
            chatformatting chatFormatting =
                    new chatformatting(
                            this,
                            ranks,
                            sounds
                    );

            chatFormatting.setNameProvider(
                    player -> nicknames.getFormattedNickname(player)
            );

            getServer().getPluginManager().registerEvents(
                    chatFormatting,
                    this
            );
        }

        if (getCommand("nick") != null) {
            getCommand("nick").setExecutor(
                    new ssmpnicknamecommands(nicknames)
            );
        }

        if (getCommand("ssmp") != null) {
            getCommand("ssmp").setExecutor(
                    new ssmpcommand(
                            this,
                            sounds,
                            menu
                    )
            );
        }

        ssmpeconomy economy = null;

        if (isEnabled("economy")) {
            economy = new ssmpeconomy(
                    this,
                    sounds
            );

            getServer().getPluginManager().registerEvents(
                    economy,
                    this
            );

            economyCommands =
                    new ssmpeconomycommands(economy);

            if (getCommand("economy") != null) {
                getCommand("economy").setExecutor(
                        economyCommands
                );

                getCommand("economy").setTabCompleter(
                        economyCommands
                );
            }
        }

        if (isEnabled("shop") && economy != null) {
            ssmpshop shop =
                    new ssmpshop(
                            economy,
                            this,
                            sounds
                    );

            getServer().getPluginManager().registerEvents(
                    shop,
                    this
            );

            if (getCommand("shop") != null) {
                getCommand("shop").setExecutor(shop);
            }
        }

        if (isEnabled("scoreboard") && economy != null) {
            scoreboard =
                    new ssmpscoreboard(
                            economy,
                            this
                    );

            getServer().getPluginManager().registerEvents(
                    scoreboard,
                    this
            );

            optimization.registerShutdownAction(scoreboard::stop);
            scoreboard.start();
        }

        if (isEnabled("menu")) {
            ssmpmenuitem menuItem =
                    new ssmpmenuitem(this);

            getServer().getPluginManager().registerEvents(
                    menuItem,
                    this
            );
        }

        if (isEnabled("auction") && economy != null) {
            auction =
                    new ssmpauction(
                            this,
                            economy,
                            permissions,
                            sounds
                    );

            getServer().getPluginManager().registerEvents(
                    auction,
                    this
            );

            if (getCommand("ah") != null) {
                getCommand("ah").setExecutor(auction);
                getCommand("ah").setTabCompleter(auction);
            }
        }

        if (isEnabled("enderchest")) {
            enderChest =
                    new ssmpenderchest(this);

            getServer().getPluginManager().registerEvents(
                    enderChest,
                    this
            );

            if (getCommand("enderchest") != null) {
                ssmpenderchest finalEnderChest = enderChest;

                getCommand("enderchest").setExecutor(
                        (sender, command, label, args) -> {
                            if (!(sender instanceof Player player)) {
                                sender.sendMessage(
                                        ChatColor.RED
                                                + "Only players can use this command."
                                );
                                return true;
                            }

                            if (!player.hasPermission(
                                    "ssmp.enderchest.use"
                            )) {
                                player.sendMessage(
                                        ChatColor.RED
                                                + "You don't have permission to use the Ender Chest."
                                );
                                return true;
                            }

                            finalEnderChest.open(player);
                            return true;
                        }
                );
            }
        }

        if (isEnabled("homes")) {
            homes =
                    new ssmphomes(this);

            getServer().getPluginManager().registerEvents(
                    homes,
                    this
            );

            ssmphomecommands homeCommands =
                    new ssmphomecommands(homes);

            if (getCommand("sethome") != null) {
                getCommand("sethome").setExecutor(
                        homeCommands
                );
            }

            if (getCommand("home") != null) {
                getCommand("home").setExecutor(
                        homeCommands
                );
            }

            if (getCommand("homes") != null) {
                getCommand("homes").setExecutor(
                        homeCommands
                );
            }

            if (getCommand("delhome") != null) {
                getCommand("delhome").setExecutor(
                        homeCommands
                );
            }
        }

        if (isEnabled("warps")) {
            warps =
                    new ssmpwarps(this);

            getServer().getPluginManager().registerEvents(
                    warps,
                    this
            );

            ssmpwarpcommands warpCommands =
                    new ssmpwarpcommands(warps);

            if (getCommand("setwarp") != null) {
                getCommand("setwarp").setExecutor(
                        warpCommands
                );
            }

            if (getCommand("warp") != null) {
                getCommand("warp").setExecutor(
                        warpCommands
                );
            }

            if (getCommand("warps") != null) {
                getCommand("warps").setExecutor(
                        warpCommands
                );
            }

            if (getCommand("delwarp") != null) {
                getCommand("delwarp").setExecutor(
                        warpCommands
                );
            }
        }

        getLogger().info(
                "Siddy's Simple Server Plugin has been enabled."
        );
        getLogger().info("""
                  Siddy's    | Paper
                  Simple     | Spigot
                  Plugin     | Bukkit
                  V. 2.1.0   | Purpur
                """
        );
    }

    @Override
    public void onDisable() {
        if (optimization != null) {
            optimization.shutdown();
        }

        getLogger().info("SSMP disabled.");
    }
    public ssmplevels getLevels() {
        return levels;
    }

    public boolean reloadSSMP() {
        try {
            reloadConfig();
            if (optimization != null) {
                optimization.refresh();
            }

            if (sounds != null) {
                sounds.reload();
            }

            if (rtp != null) {
                rtp.reload();
            }

            getLogger().info(
                    "SSMP configuration reloaded."
            );

            return true;
        } catch (Exception e) {
            getLogger().severe(
                    "Failed to reload SSMP configuration: "
                            + e.getMessage()
            );

            e.printStackTrace();

            return false;
        }
    }

    private void createConfig() {
        if (!getDataFolder().exists()
                && !getDataFolder().mkdirs()) {
            throw new RuntimeException(
                    "Failed to create SSMP data folder."
            );
        }

        File configFile =
                new File(
                        getDataFolder(),
                        "config.yml"
                );

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException(
                            "Could not create config.yml."
                    );
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to create config.yml.",
                        e
                );
            }
        }

        reloadConfig();

        FileConfiguration config =
                getConfig();

        config.addDefault(
                "modules.ranks.enabled",
                true
        );

        config.addDefault(
                "modules.permissions.enabled",
                true
        );

        config.addDefault(
                "modules.economy.enabled",
                true
        );

        config.addDefault(
                "modules.shop.enabled",
                true
        );

        config.addDefault(
                "modules.auction.enabled",
                true
        );

        config.addDefault(
                "modules.auction.max-auction-price",
                1000000
        );

        config.addDefault(
                "modules.auction.negate-buy-price",
                0
        );

        config.addDefault(
                "modules.auction.max-pages",
                10
        );

        config.addDefault(
                "modules.auction.non-sellable-items",
                List.of("COMPASS")
        );

        config.addDefault(
                "modules.tablist.enabled",
                true
        );

        config.addDefault(
                "modules.tablist.header.enabled",
                true
        );

        config.addDefault(
                "modules.tablist.header.lines",
                List.of(
                        "",
                        "<#55FFFF>&cSiddy's Tablist",
                        "&7Welcome, %player_nickname%",
                        ""
                )
        );

        config.addDefault(
                "modules.tablist.footer.enabled",
                true
        );

        config.addDefault(
                "modules.tablist.footer.lines",
                List.of(
                        "",
                        "&7Online: &f%online%&7/&f%max_players%",
                        "&7This is siddy's default tablist. Change in SiddysSimplePlugin/config.yml",
                        "&7Or completely disable this module from there.",
                        ""
                )
        );

        config.addDefault(
                "modules.tablist.player-list.enabled",
                true
        );

        config.addDefault(
                "modules.tablist.player-list.format",
                "%player_prefix%%player_nickname%%player_suffix% &8• &7%ping%ms"
        );

        config.addDefault(
                "modules.scoreboard.enabled",
                true
        );

        config.addDefault(
                "modules.chat.enabled",
                true
        );

        config.addDefault(
                "modules.placeholders.enabled",
                true
        );

        config.addDefault(
                "modules.menu.enabled",
                true
        );

        config.addDefault(
                "modules.enderchest.enabled",
                true
        );

        config.addDefault(
                "modules.enderchest.doubled",
                false
        );

        config.addDefault(
                "modules.homes.enabled",
                true
        );

        config.addDefault(
                "modules.homes.max-homes",
                3
        );

        config.addDefault(
                "modules.warps.enabled",
                true
        );

        config.addDefault(
                "modules.kits.enabled",
                true
        );

        config.addDefault(
                "modules.rtp.enabled",
                true
        );
        config.addDefault("modules.levels.enabled", true);

        config.options().copyDefaults(true);

        saveConfig();
    }

    private boolean isEnabled(String module) {
        return optimization.isModuleEnabled(module);
    }

    public ssmphomes getHomes() {
        return homes;
    }

    public ssmpenderchest getEnderChest() {
        return enderChest;
    }

    public ssmpwarps getWarps() {
        return warps;
    }
}