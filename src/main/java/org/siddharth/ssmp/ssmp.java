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

import org.siddharth.ssmp.chat.chatFormatting;
import org.siddharth.ssmp.commands.*;
import org.siddharth.ssmp.economy.auctionHouse;
import org.siddharth.ssmp.support.ValutSupport;
import org.siddharth.ssmp.gui.bountyGUI;
import org.siddharth.ssmp.gui.contractGUI;
import org.siddharth.ssmp.gui.kitGUI;
import org.siddharth.ssmp.gui.playerInformationGUI;
import org.siddharth.ssmp.economy.economy;
import org.siddharth.ssmp.economy.economyCommands;
import org.siddharth.ssmp.economy.shopSystem;
import org.siddharth.ssmp.mechanics.*;
import org.siddharth.ssmp.updateChecker.updateChecker;
import org.siddharth.ssmp.information.information;
import org.siddharth.ssmp.controlpanel.controlPanel;
import org.siddharth.ssmp.support.placholderAPISupport;
import org.siddharth.ssmp.util.coreUtilMethods;
import org.siddharth.ssmp.moderation.moderationSystem;
import org.siddharth.ssmp.moderation.moderationCommands;

public final class ssmp extends JavaPlugin {

    String version = "r3.0.0";
    String code_name = "Sugar";
    String full_name = "COMPLETED Wonders of RPG";
    String plugin_prefix = "[Siddy's Simple Plugin]";

    private rankSystem ranks;
    private permissionSystem permissions;
    private soundSystem sounds;
    private placholderAPISupport papiExpansion;
    private placeholderSystem placeholders;
    private tablistSystem tablist;
    private scoreboardSystem scoreboard;
    private auctionHouse auction;
    private economyCommands economyCommands;
    private enderChestSystem enderChest;
    private homeSystem homes;
    private warpSystem warps;
    private menuSystem menu;
    private nicknameSystem nicknames;
    private kitSystem kits;
    private kitGUI kitGui;
    private kitCommands kitCommands;
    private coreUtilMethods core;
    private RTPSystem rtp;
    private optimizationSystem optimization;
    private levelSystem levels;

    // Wonders of rpg start from here
    private economy economy;
    private contractSystem contracts;
    private contractGUI contractGui;
    private bountySystem bounties;
    private bountyGUI bountyGui;
    private playerInformationSystem playerInfo;
    private playerInformationGUI playerInfoGui;

    private controlPanel control;
    private moderationSystem moderation;

    public coreUtilMethods getCore() {
        return core;
    }
    @Override
    public void onEnable() {
        new updateChecker(this).check();
        new information(this).create();
        createConfig();
        moderation = new moderationSystem(this);
        moderationCommands moderationCommandHandler = new moderationCommands(moderation);
        String[] moderationCommandNames = {"mute", "unmute", "warn", "ban", "unban", "filter", "history", "resetviolations"};
        for (String commandName : moderationCommandNames) {
            if (getCommand(commandName) != null) {
                getCommand(commandName).setExecutor(moderationCommandHandler);
                getCommand(commandName).setTabCompleter(moderationCommandHandler);
            } else {
                getLogger().warning("Missing moderation command in plugin.yml: " + commandName);
            }
        }
        control = new controlPanel(this);
        control.setUpdateCheckAction(() -> new updateChecker(this).check());
        control.start();

        optimization = new optimizationSystem(this);
        optimization.initialize();
        core = new coreUtilMethods(this);

        ranks = new rankSystem(this);
        permissions = new permissionSystem(this, ranks);
        if (optimization.isModuleEnabled("levels")) {
            levels = new levelSystem(this);
        } else {
            levels = null;
        }
        if (isEnabled("placeholders")) {
            placeholders = new placeholderSystem(this, ranks);
        } else {
            placeholders = null;
        }

        sounds = new soundSystem(this);
        nicknames = new nicknameSystem(this);

        menu = new menuSystem(this, sounds);

        getServer().getPluginManager().registerEvents(
                menu,
                this
        );

        rankCommands rankCommands =
                new rankCommands(
                        ranks,
                        permissions
                );

        if (getCommand("rank") != null) {
            getCommand("rank").setExecutor(rankCommands);
        }



        if (isEnabled("tablist")) {
            tablist = new tablistSystem(
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
            rtp = new RTPSystem(this);

            getServer().getPluginManager().registerEvents(
                    rtp,
                    this
            );

            if (getCommand("rtp") != null) {
                getCommand("rtp").setExecutor(rtp);
            }
        }

        if (isEnabled("kits")) {
            kits = new kitSystem(this);
            kitGui = new kitGUI(this, kits);
            kitCommands = new kitCommands(this, kits, kitGui);

            getServer().getPluginManager().registerEvents(kitGui, this);
            if (getCommand("kit") != null) {
                getCommand("kit").setExecutor(kitCommands);
                getCommand("kit").setTabCompleter(kitCommands);
            }
        }

        if (isEnabled("chat")) {
            chatFormatting chat = new chatFormatting(
                    this,
                    ranks,
                    sounds,
                    placeholders
            );

            chat.setNameProvider(
                    player -> nicknames.getFormattedNickname(player)
            );

            getServer().getPluginManager().registerEvents(chat, this);
        }

        if (getCommand("nick") != null) {
            getCommand("nick").setExecutor(
                    new nicknameCommands(nicknames)
            );
        }

        if (getCommand("ssmp") != null) {
            getCommand("ssmp").setExecutor(
                    new commandSystem(
                            this,
                            sounds,
                            menu
                    )
            );
        }

        economy = null;

        if (isEnabled("economy")) {
            economy = new economy(
                    this,
                    sounds
            );

            getServer().getPluginManager().registerEvents(
                    economy,
                    this
            );

            economyCommands =
                    new economyCommands(economy);

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
                        new ValutSupport(this, economy),
                        this,
                        org.bukkit.plugin.ServicePriority.Normal
                );

                getLogger().info("Vault economy support enabled.");
            }
        }

        if (isEnabled("shop") && economy != null) {
            shopSystem shop =
                    new shopSystem(
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
            contracts = new contractSystem(this, economy, levels);
            contractGui = new contractGUI(contracts);

            getServer().getPluginManager().registerEvents(contracts, this);
            getServer().getPluginManager().registerEvents(contractGui, this);
        }

        if (isEnabled("bounties") && economy != null) {
            bounties = new bountySystem(this, economy);
            bountyGui = new bountyGUI(bounties, economy);

            getServer().getPluginManager().registerEvents(bounties, this);
            getServer().getPluginManager().registerEvents(bountyGui, this);
        }

        playerInfo = new playerInformationSystem(
                this,
                economy,
                levels,
                contracts,
                bounties
        );

        playerInfoGui = new playerInformationGUI(playerInfo);

        getServer().getPluginManager().registerEvents(playerInfoGui, this);

        if (isEnabled("scoreboard") && economy != null) {
            scoreboard =
                    new scoreboardSystem(
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
            menuItemSystem menuItem =
                    new menuItemSystem(this);

            getServer().getPluginManager().registerEvents(
                    menuItem,
                    this
            );
        }

        if (isEnabled("auction") && economy != null) {
            auction =
                    new auctionHouse(
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
                    new enderChestSystem(this);

            getServer().getPluginManager().registerEvents(
                    enderChest,
                    this
            );

            if (getCommand("enderchest") != null) {
                enderChestSystem finalEnderChest = enderChest;

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
                    new homeSystem(this);

            getServer().getPluginManager().registerEvents(
                    homes,
                    this
            );

            homeCommands homeCommands =
                    new homeCommands(homes);

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
                    new warpSystem(this);

            getServer().getPluginManager().registerEvents(
                    warps,
                    this
            );

            warpCommands warpCommands =
                    new warpCommands(warps);

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
                    new contractCommands(contractGui)
            );
        }

        if (bounties != null && bountyGui != null) {
            bountyCommands bountyCommands = new bountyCommands(bountyGui);

            if (getCommand("bounties") != null) {
                getCommand("bounties").setExecutor(bountyCommands);
            }

            if (getCommand("bounty") != null) {
                getCommand("bounty").setExecutor(bountyCommands);
            }
        }

        if (playerInfoGui != null && getCommand("playerinfo") != null) {
            getCommand("playerinfo").setExecutor(
                    new playerInformationCommands(playerInfoGui)
            );
        }

        if (placeholders != null
                && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {

            papiExpansion = new placholderAPISupport(
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
        if (moderation != null) {
            moderation.close();
        }
        getLogger().info("SSMP disabled.");
    }
    public levelSystem getLevels() {
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
            if (moderation != null) {
                moderation.reloadFromConfig();
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
    public homeSystem getHomes() {
        return homes;
    }
    public enderChestSystem getEnderChest() {
        return enderChest;
    }
    public warpSystem getWarps() {
        return warps;
    }
    public economy getEconomy() {
        return economy;
    }
    public contractSystem getContracts() {
        return contracts;
    }
    public contractGUI getContractGui() {
        return contractGui;
    }
    public bountySystem getBounties() {
        return bounties;
    }
    public bountyGUI getBountyGui() {
        return bountyGui;
    }
    public playerInformationSystem getPlayerInfo() {
        return playerInfo;
    }
    public playerInformationGUI getPlayerInfoGui() {
        return playerInfoGui;
    }
    public kitGUI getKitGui() {return kitGui;}
    public RTPSystem getRTP() {return rtp;}
    public rankSystem getRanks() {return ranks;}
    public placeholderSystem getPlaceholders() {
        return placeholders;
    }
}
