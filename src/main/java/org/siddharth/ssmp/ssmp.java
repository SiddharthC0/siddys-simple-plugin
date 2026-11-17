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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.siddharth.ssmp.updateChecker.update;
import org.siddharth.ssmp.info.info;
import org.siddharth.ssmp.cpanel.ssmpCPanel;

public final class ssmp extends JavaPlugin {

    String version = "r3.0.0";
    String code_name = "Sugar";
    String full_name = "COMPLETED Wonders of RPG";
    String plugin_prefix = "[Siddy's Simple Plugin]";

    private ssmpRanks ranks;
    private ssmpPermissions permissions;
    private ssmpSoundSystem sounds;
    private ssmpPlaceholderAPI papiExpansion;
    private ssmpPlaceholders placeholders;
    private ssmpTablist tablist;
    private ssmpScoreboard scoreboard;
    private ssmpAuction auction;
    private ssmpEconomyCommands economyCommands;
    private ssmpEnderChest enderChest;
    private ssmpHomes homes;
    private ssmpWarps warps;
    private ssmpMenu menu;
    private ssmpNicknames nicknames;
    private ssmpKits kits;
    private ssmpKitGUI kitGui;
    private ssmpKitCommands kitCommands;
    private ssmpCoreMethods core;
    private ssmpRTP rtp;
    private ssmpOptimization optimization;
    private ssmpLevels levels;

    // Wonders of rpg start from here
    private ssmpEconomy economy;
    private ssmpContracts contracts;
    private ssmpContractsGUI contractGui;
    private ssmpBounties bounties;
    private ssmpBountyGUI bountyGui;
    private ssmpPlayerInfo playerInfo;
    private ssmpPlayerInfoGUI playerInfoGui;

    private ssmpCPanel control;

    public ssmpCoreMethods getCore() {
        return core;
    }
    @Override
    public void onEnable() {
        new update(this).check();
        new info(this).create();
        createConfig();
        control = new ssmpCPanel(this);
        control.setUpdateCheckAction(() -> new update(this).check());
        control.start();

        optimization = new ssmpOptimization(this);
        optimization.initialize();
        core = new ssmpCoreMethods(this);

        ranks = new ssmpRanks(this);
        permissions = new ssmpPermissions(this, ranks);
        if (optimization.isModuleEnabled("levels")) {
            levels = new ssmpLevels(this);
        } else {
            levels = null;
        }
        if (isEnabled("placeholders")) {
            placeholders = new ssmpPlaceholders(this, ranks);
        } else {
            placeholders = null;
        }

        sounds = new ssmpSoundSystem(this);
        nicknames = new ssmpNicknames(this);

        menu = new ssmpMenu(this, sounds);

        getServer().getPluginManager().registerEvents(
                menu,
                this
        );

        ssmpRankCommands rankCommands =
                new ssmpRankCommands(
                        ranks,
                        permissions
                );

        if (getCommand("rank") != null) {
            getCommand("rank").setExecutor(rankCommands);
        }



        if (isEnabled("tablist")) {
            tablist = new ssmpTablist(
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
            rtp = new ssmpRTP(this);

            getServer().getPluginManager().registerEvents(
                    rtp,
                    this
            );

            if (getCommand("rtp") != null) {
                getCommand("rtp").setExecutor(rtp);
            }
        }

        if (isEnabled("kits")) {
            kits = new ssmpKits(this);
            kitGui = new ssmpKitGUI(this, kits);
            kitCommands = new ssmpKitCommands(this, kits, kitGui);

            getServer().getPluginManager().registerEvents(kitGui, this);
            if (getCommand("kit") != null) {
                getCommand("kit").setExecutor(kitCommands);
                getCommand("kit").setTabCompleter(kitCommands);
            }
        }

        if (isEnabled("chat")) {
            ssmpChatFormatting chatFormatting =
                    new ssmpChatFormatting(
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
                    new ssmpNicknameCommands(nicknames)
            );
        }

        if (getCommand("ssmp") != null) {
            getCommand("ssmp").setExecutor(
                    new ssmpCommands(
                            this,
                            sounds,
                            menu
                    )
            );
        }

        economy = null;

        if (isEnabled("economy")) {
            economy = new ssmpEconomy(
                    this,
                    sounds
            );

            getServer().getPluginManager().registerEvents(
                    economy,
                    this
            );

            economyCommands =
                    new ssmpEconomyCommands(economy);

            if (getCommand("economy") != null) {
                getCommand("economy").setExecutor(
                        economyCommands
                );

                getCommand("economy").setTabCompleter(
                        economyCommands
                );
            }

            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                Bukkit.getServicesManager().register(
                        net.milkbowl.vault.economy.Economy.class,
                        new ssmpVaultEconomy(this, economy),
                        this,
                        org.bukkit.plugin.ServicePriority.Normal
                );

                getLogger().info("Vault economy support enabled.");
            }
        }

        if (isEnabled("shop") && economy != null) {
            ssmpShop shop =
                    new ssmpShop(
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

        if (isEnabled("contracts")) {
            contracts = new ssmpContracts(this, economy, levels);
            contractGui = new ssmpContractsGUI(contracts);

            getServer().getPluginManager().registerEvents(contracts, this);
            getServer().getPluginManager().registerEvents(contractGui, this);
        }

        if (isEnabled("bounties") && economy != null) {
            bounties = new ssmpBounties(this, economy);
            bountyGui = new ssmpBountyGUI(bounties, economy);

            getServer().getPluginManager().registerEvents(bounties, this);
            getServer().getPluginManager().registerEvents(bountyGui, this);
        }

        playerInfo = new ssmpPlayerInfo(
                this,
                economy,
                levels,
                contracts,
                bounties
        );

        playerInfoGui = new ssmpPlayerInfoGUI(playerInfo);

        getServer().getPluginManager().registerEvents(playerInfoGui, this);

        if (isEnabled("scoreboard") && economy != null) {
            scoreboard =
                    new ssmpScoreboard(
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
            ssmpMenuItem menuItem =
                    new ssmpMenuItem(this);

            getServer().getPluginManager().registerEvents(
                    menuItem,
                    this
            );
        }

        if (isEnabled("auction") && economy != null) {
            auction =
                    new ssmpAuction(
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
                    new ssmpEnderChest(this);

            getServer().getPluginManager().registerEvents(
                    enderChest,
                    this
            );

            if (getCommand("enderchest") != null) {
                ssmpEnderChest finalEnderChest = enderChest;

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
                    new ssmpHomes(this);

            getServer().getPluginManager().registerEvents(
                    homes,
                    this
            );

            ssmpHomeCommands homeCommands =
                    new ssmpHomeCommands(homes);

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
                    new ssmpWarps(this);

            getServer().getPluginManager().registerEvents(
                    warps,
                    this
            );

            ssmpWarpCommands warpCommands =
                    new ssmpWarpCommands(warps);

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
        if (contracts != null && contractGui != null && getCommand("contracts") != null) {
            getCommand("contracts").setExecutor(
                    new ssmpContractCommands(contractGui)
            );
        }

        if (bounties != null && bountyGui != null) {
            ssmpBountyCommands bountyCommands = new ssmpBountyCommands(bountyGui);

            if (getCommand("bounties") != null) {
                getCommand("bounties").setExecutor(bountyCommands);
            }

            if (getCommand("bounty") != null) {
                getCommand("bounty").setExecutor(bountyCommands);
            }
        }

        if (playerInfoGui != null && getCommand("playerinfo") != null) {
            getCommand("playerinfo").setExecutor(
                    new ssmpPlayerInfoCommands(playerInfoGui)
            );
        }

        if (placeholders != null
                && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {

            papiExpansion = new ssmpPlaceholderAPI(
                    this,
                    placeholders,
                    ranks,
                    contracts,
                    bounties,
                    levels,
                    economy
            );

            if (papiExpansion.register()) {
                getLogger().info("PlaceholderAPI expansion registered.");
            } else {
                getLogger().warning("Failed to register PlaceholderAPI expansion.");
            }
        }


        getLogger().info(plugin_prefix+" Enabled plugin version "+version);
        getLogger().info(plugin_prefix+" By Siddharth C");
        getLogger().info(plugin_prefix+" Code "+code_name+" Release "+full_name);

    }

    @Override
    public void onDisable() {
        if (control != null) {
            control.stop();
        }
        if (optimization != null) {
            optimization.shutdown();
        }
        if (levels != null) levels.save();
        if (contracts != null) contracts.reload();
        if (bounties != null) bounties.save();
        if (papiExpansion != null) {
            papiExpansion.unregister();
        }

        if (placeholders != null) {
            placeholders.stop();
        }
        getLogger().info("SSMP disabled.");
    }
    public ssmpLevels getLevels() {
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

        config.addDefault("modules.contracts.enabled", true);
        config.addDefault("modules.bounties.enabled", true);

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
        config.addDefault(
                "modules.placeholders.enabled",
                true
        );

        config.addDefault(
                "cpanel.console.enabled",
                true
        );
        config.addDefault("modules.levels.enabled", true);

        config.options().copyDefaults(true);

        saveConfig();
    }

    private boolean isEnabled(String module) {
        return optimization.isModuleEnabled(module);
    }
    public ssmpHomes getHomes() {
        return homes;
    }
    public ssmpEnderChest getEnderChest() {
        return enderChest;
    }
    public ssmpWarps getWarps() {
        return warps;
    }
    public ssmpEconomy getEconomy() {
        return economy;
    }
    public ssmpContracts getContracts() {
        return contracts;
    }
    public ssmpContractsGUI getContractGui() {
        return contractGui;
    }
    public ssmpBounties getBounties() {
        return bounties;
    }
    public ssmpBountyGUI getBountyGui() {
        return bountyGui;
    }
    public ssmpPlayerInfo getPlayerInfo() {
        return playerInfo;
    }
    public ssmpPlayerInfoGUI getPlayerInfoGui() {
        return playerInfoGui;
    }
    public ssmpKitGUI getKitGui() {return kitGui;}
    public ssmpRTP getRTP() {return rtp;}
    public ssmpRanks getRanks() {return ranks;}
}