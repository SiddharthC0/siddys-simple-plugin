/*
* Wassup, welcome to Siddy's Simple Plugin's home file. This file is responsible for all the tasks the plugin performs, as it
* is the one that controls which module can be enabled and all that, also the config.yml
*
* I've added various comments in this file to help you set up your own plugin if you forked this repository. This will
* get you fluent with the functions and all of the plugin. To begin with, this file contains the getters for other files
* that want to access a specfic file.
* */
package org.siddharth.ssmp;

// General imports
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;

// The import of the main files
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
    String version = getDescription().getVersion(); // If you want to customize version of plugin, consider looking resources/plugin.yml
    String code_name = "Sugar"; // Use this to customize the short name (code name) of the release.
    String full_name = "COMPLETED Wonders of RPG"; // The full name of the RELEASE, not the plugin...
    String plugin_name = "Siddy's Simple Plugin"; // Name of the PLUGIN. This is used widely, be careful!
    String plugin_prefix = "[Siddy's Simple Plugin]"; // The prefix used to print useful messages. Not currently applicable to all msgs.
    Boolean debug_mode = true; // Toggle debug mode which prints important information...

    // Defination of all the modules that have to be loaded from here
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
    private economy economy;
    private contractSystem contracts;
    private contractGUI contractGui;
    private bountySystem bounties;
    private bountyGUI bountyGui;
    private playerInformationSystem playerInfo;
    private playerInformationGUI playerInfoGui;
    private controlPanel control;
    private moderationSystem moderation;
    public coreUtilMethods getCore() {return core;}

    // Stuff that happens when the server starts with the plugin installed
    // Check for updates, Update manifest (if any changes), Create/Edit configuration file and load modules
    @Override
    public void onEnable() {
        new updateChecker(this).check();
        new information(this).create();
        createConfig();

        // Very very very critical stuff
        try {
            ranks = new rankSystem(this);
        } catch (Exception e) {getLogger().warning("The ranks system failed to load: "+e.getMessage());}

        try {
            permissions = new permissionSystem(this, ranks);
        } catch (Exception e) {getLogger().warning("The permissions system failed to load: "+e.getMessage());}

        try {
            sounds = new soundSystem(this);
        } catch (Exception e) {getLogger().warning("The sounds system failed to load: "+e.getMessage());}

        try {
            nicknames = new nicknameSystem(this);
        } catch (Exception e) {getLogger().warning("The nicknames system failed to load: "+e.getMessage());}

        try {
            menu = new menuSystem(this, sounds);
            getServer().getPluginManager().registerEvents(menu, this);
        } catch (Exception e) {getLogger().warning("The menu system failed to load: "+e.getMessage());}

        // Moderation System
        try {
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
        } catch (Exception e) {
            getLogger().warning("Moderation failed to load: "+e.getMessage());
        }

        // Rank system
        try {
            rankCommands rankCommands = new rankCommands(ranks, permissions);
            if (getCommand("rank") != null) {
                getCommand("rank").setExecutor(rankCommands);
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to load Ranks: "+e.getMessage());
            }
        }

        // Control Panel
        try {
            control = new controlPanel(this);
            control.setUpdateCheckAction(() -> new updateChecker(this).check());
            control.start();
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start control panel: " + e.getMessage());
            }
        }

        // Optimizer
        try {
            optimization = new optimizationSystem(this);
            optimization.initialize();
            core = new coreUtilMethods(this);
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start optimization system: " + e.getMessage());
            }
        }

        // Main menu system
        try {
            if (getCommand("ssmp") != null) {
                getCommand("ssmp").setExecutor(new commandSystem(this, sounds, menu));
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start menu system: " + e.getMessage());
            }
        }

        // Levels system
        try {
            if (isEnabled("levels")) {
                levels = new levelSystem(this);
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start level system: " + e.getMessage());
            }
        }

        // Tablist System
        try {
            if (isEnabled("tablist")) {
                tablist = new tablistSystem(this, ranks);
                getServer().getPluginManager().registerEvents(tablist, this);

                optimization.registerShutdownAction(tablist::stop);
                tablist.start();
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start tablist system: " + e.getMessage());
            }
        }

        // Random teleport system
        try {
            if (isEnabled("rtp")) {
                rtp = new RTPSystem(this);
                getServer().getPluginManager().registerEvents(rtp, this);
                if (getCommand("rtp") != null) {
                    getCommand("rtp").setExecutor(rtp);
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start rtp system: " + e.getMessage());
            }
        }

        // Kits system
        try {
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
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start kits system: " + e.getMessage());
            }
        }

        // Nickname system
        try {
            if (getCommand("nick") != null && isEnabled("nickname")) {
                getCommand("nick").setExecutor(new nicknameCommands(nicknames));
            }
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start nickname system: " + e.getMessage());
            }
        }

        // Economy system
        try {
            economy = null;
            if (isEnabled("economy")) {
                economy = new economy(this, sounds);
                getServer().getPluginManager().registerEvents(economy, this);
                economyCommands = new economyCommands(economy);
                if (getCommand("economy") != null) {
                    getCommand("economy").setExecutor(economyCommands);
                    getCommand("economy").setTabCompleter(economyCommands);
                }

                if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                    Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new ValutSupport(this, economy), this, org.bukkit.plugin.ServicePriority.Normal);
                    getLogger().info("Vault economy support enabled.");
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start economy system: " + e.getMessage());
            }
        }

        // Shop system
        try {
            if (isEnabled("shop") && economy != null) {
                shopSystem shop = new shopSystem(economy, this, sounds);
                getServer().getPluginManager().registerEvents(shop, this);
                if (getCommand("shop") != null) {
                    getCommand("shop").setExecutor(shop);
                }
            }
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start shop system: " + e.getMessage());
            }
        }

        // Contracts system
        try {
            if (isEnabled("contracts")) {
                contracts = new contractSystem(this, economy, levels);
                contractGui = new contractGUI(contracts);
                getServer().getPluginManager().registerEvents(contracts, this);
                getServer().getPluginManager().registerEvents(contractGui, this);
                if (getCommand("contracts") != null) {
                    getCommand("contracts").setExecutor(new contractCommands(contractGui));
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start contracts system: " + e.getMessage());
            }
        }

        // Bounty system
        try {
            if (isEnabled("bounties") && economy != null) {
                bounties = new bountySystem(this, economy);
                bountyGui = new bountyGUI(bounties, economy);

                getServer().getPluginManager().registerEvents(bounties, this);
                getServer().getPluginManager().registerEvents(bountyGui, this);
                bountyCommands bountyCommands = new bountyCommands(bountyGui);
                if (getCommand("bounties") != null) {
                    getCommand("bounties").setExecutor(bountyCommands);
                }

                if (getCommand("bounty") != null) {
                    getCommand("bounty").setExecutor(bountyCommands);
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start bounty system: " + e.getMessage());
            }
        }

        // Player information system
        try {
            if (isEnabled("economy") && isEnabled("levels") && isEnabled("contracts") && isEnabled("bounties")) {
                playerInfo = new playerInformationSystem(this, economy, levels, contracts, bounties);
                playerInfoGui = new playerInformationGUI(playerInfo);
                getServer().getPluginManager().registerEvents(playerInfoGui, this);
                if (playerInfoGui != null && getCommand("playerinfo") != null) {
                    getCommand("playerinfo").setExecutor(
                            new playerInformationCommands(playerInfoGui)
                    );
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start playerinfo system: " + e.getMessage());
            }
        }


        // Scoreboard system
        try {
            if (isEnabled("scoreboard") && economy != null) {
                scoreboard = new scoreboardSystem(economy, this);
                getServer().getPluginManager().registerEvents(scoreboard, this);
                optimization.registerShutdownAction(scoreboard::stop);
                scoreboard.start();
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start scoreboard system: " + e.getMessage());
            }
        }

        // Menu ITEM system
        try {
            if (isEnabled("menu")) {
                menuItemSystem menuItem = new menuItemSystem(this);
                getServer().getPluginManager().registerEvents(menuItem, this);
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start menu system: " + e.getMessage());
            }
        }

        // Auction house system
        try {
            if (isEnabled("auction") && economy != null) {
                auction = new auctionHouse(this, economy, permissions, sounds);
                getServer().getPluginManager().registerEvents(auction, this);
                if (getCommand("ah") != null) {
                    getCommand("ah").setExecutor(auction);
                    getCommand("ah").setTabCompleter(auction);
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start auction system: " + e.getMessage());
            }
        }

        // Ender chest system
        try {
            if (isEnabled("enderchest")) {
                enderChest = new enderChestSystem(this);
                getServer().getPluginManager().registerEvents(enderChest, this);
                if (getCommand("enderchest") != null) {
                    enderChestSystem finalEnderChest = enderChest;
                    getCommand("enderchest").setExecutor((sender, command, label, args) -> {
                                if (!(sender instanceof Player player)) {
                                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                                    return true;
                                }
                                if (!player.hasPermission("ssmp.enderchest.use")) {
                                    player.sendMessage(ChatColor.RED + "You don't have permission to use the Ender Chest.");
                                    return true;
                                }
                                finalEnderChest.open(player);
                                return true;
                            }
                    );
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start enderchest system: " + e.getMessage());
            }
        }

        // Homes system
        try {
            if (isEnabled("homes")) {
                homes = new homeSystem(this);
                getServer().getPluginManager().registerEvents(homes, this);
                homeCommands homeCommands = new homeCommands(homes);
                if (getCommand("sethome") != null) {
                    getCommand("sethome").setExecutor(homeCommands);
                }
                if (getCommand("home") != null) {
                    getCommand("home").setExecutor(homeCommands);
                }
                if (getCommand("homes") != null) {
                    getCommand("homes").setExecutor(homeCommands);
                }
                if (getCommand("delhome") != null) {
                    getCommand("delhome").setExecutor(homeCommands);
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start homes system: " + e.getMessage());
            }
        }

        // Warps system
        try {
            if (isEnabled("warps")) {
                warps = new warpSystem(this);
                getServer().getPluginManager().registerEvents(warps, this);
                warpCommands warpCommands = new warpCommands(warps);
                if (getCommand("setwarp") != null) {
                    getCommand("setwarp").setExecutor(warpCommands);
                }
                if (getCommand("warp") != null) {
                    getCommand("warp").setExecutor(warpCommands);
                }
                if (getCommand("warps") != null) {
                    getCommand("warps").setExecutor(warpCommands);
                }
                if (getCommand("delwarp") != null) {
                    getCommand("delwarp").setExecutor(warpCommands);
                }
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start warpsSystem: " + e.getMessage());
            }
        }

        // Placeholder System
        try {
            if (isEnabled("placeholders")) {
                placeholders = new placeholderSystem(this, ranks);
                if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    papiExpansion = new placholderAPISupport(this, placeholders, ranks, contracts, bounties,
                            levels, economy);
                    if (papiExpansion.register()) {
                        getLogger().info("PlaceholderAPI expansion registered.");
                    } else {
                        getLogger().warning("Failed to register PlaceholderAPI expansion.");
                    }
                }
            } else {
                placeholders = null;
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start placeholdersSystem: " + e.getMessage());
            }
        }

        // Chat system
        try {
            if (isEnabled("chat")) {
                chatFormatting chat = new chatFormatting(this, ranks, sounds, placeholders);
                chat.setNameProvider(player -> nicknames.getFormattedNickname(player));
                getServer().getPluginManager().registerEvents(chat, this);
            }
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start chat system: " + e.getMessage());
            }
        }

        // Finally, the plugin is enabled
        getLogger().info(plugin_prefix+" Enabled plugin version "+version);
        getLogger().info(plugin_prefix+" By Siddharth C");
        getLogger().info(plugin_prefix+" Code "+code_name+" Release "+full_name);
    }
    // Tasks that are performed when the server is stopped with the plugin installed
    @Override
    public void onDisable() {
        // Safely shutdown critical modules
        shutdownSafely("Control Panel", () -> {
            if (control != null) control.stop();
        });

        shutdownSafely("Optimization", () -> {
            if (optimization != null) optimization.shutdown();
        });

        shutdownSafely("Placeholders", () -> {
            if (placeholders != null) placeholders.stop();
        });

        shutdownSafely("Moderation", () -> {
            if (moderation != null) moderation.close();
        });

        // Save and reload
        if (levels != null) {
            levels.save();
        }
        if (contracts != null) {
            contracts.reload();
        }
        if (bounties != null) {
            bounties.save();
        }
        if (papiExpansion != null) {
            papiExpansion.unregister();
        }

        // Finally print the message notifying user that plugin disabled.
        getLogger().info(plugin_name+" has been disabled");
    }


    // Reload the plugin (a command btw)
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

            getLogger().info(plugin_name+"'s configuration has been reloaded succesfully'");
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to reload"+plugin_name+"configuration because of exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Method that creates the configuration (config.yml) file
    private void createConfig() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new RuntimeException("Failed to create "+plugin_name+" data folder.");
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) {
                    throw new IOException("Could not create config.yml.");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create config.yml because exception: ", e);
            }
        }

        reloadConfig();
        FileConfiguration config = getConfig();

        // All the values that will be added to the module config (config.yml). This is in order btw. Pls do not write memes here,
        // becuz memes belong in classes, not in final product...

        // These are modules with standalone configs.
        config.addDefault("modules.ranks.enabled", true);
        config.addDefault("modules.permissions.enabled", true);
        config.addDefault("modules.economy.enabled", true);
        config.addDefault("modules.contracts.enabled", true);
        config.addDefault("modules.bounties.enabled", true);
        config.addDefault("modules.shop.enabled", true);
        config.addDefault("modules.warps.enabled", true);
        config.addDefault("modules.kits.enabled", true);
        config.addDefault("modules.rtp.enabled", true);
        config.addDefault("cpanel.console.enabled", false);
        config.addDefault("modules.levels.enabled", true);
        config.addDefault("modules.scoreboard.enabled", true);
        config.addDefault("modules.chat.enabled", true);
        config.addDefault("modules.placeholders.enabled", true);
        config.addDefault("modules.menu.enabled", true);

        // These are modules with core config
        // Auction house
        config.addDefault("modules.auction.enabled", true);
        config.addDefault("modules.auction.max-auction-price", 1000000);
        config.addDefault("modules.auction.negate-buy-price", 0);
        config.addDefault("modules.auction.max-pages", 10);
        config.addDefault("modules.auction.non-sellable-items", List.of("COMPASS"));

        // Tablist
        config.addDefault("modules.tablist.enabled", true);
        config.addDefault("modules.tablist.header.enabled", true);
        config.addDefault("modules.tablist.header.lines", List.of(
                        "",
                        "<#55FFFF>&cSiddy's Tablist",
                        "&7Welcome, %player_nickname%",
                        ""));
        config.addDefault("modules.tablist.footer.enabled", true);
        config.addDefault("modules.tablist.footer.lines", List.of(
                        "",
                        "&7Online: &f%online%&7/&f%max_players%",
                        "&7This is siddy's default tablist. Change in SiddysSimplePlugin/config.yml",
                        "&7Or completely disable this module from there.",
                        ""));
        config.addDefault("modules.tablist.player-list.enabled", true);
        config.addDefault("modules.tablist.player-list.format", "%player_prefix%%player_nickname%%player_suffix% &8• &7%ping%ms");

        // Ender chest
        config.addDefault("modules.enderchest.enabled", true);
        config.addDefault("modules.enderchest.doubled", false);

        // Homes
        config.addDefault("modules.homes.enabled", true);
        config.addDefault("modules.homes.max-homes", 3);

        config.options().copyDefaults(true);
        saveConfig();
    }

    // This method provides a failsafe for shutting down (disabling) modules...
    private void shutdownSafely(String module, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            getLogger().severe("Failed to shut down " + module + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Meth that returns weather module enabled or no
    private boolean isEnabled(String module) {
        return getConfig().getBoolean("modules." + module + ".enabled", true);
    }

    // Getters : These meths allow other files to access module classes directly
    public homeSystem getHomes() {return homes;}
    public enderChestSystem getEnderChest() {return enderChest;}
    public warpSystem getWarps() {return warps;}
    public economy getEconomy() {return economy;}
    public contractSystem getContracts() {return contracts;}
    public contractGUI getContractGui() {return contractGui;}
    public bountySystem getBounties() {return bounties;}
    public bountyGUI getBountyGui() {return bountyGui;}
    public playerInformationSystem getPlayerInfo() {return playerInfo;}
    public playerInformationGUI getPlayerInfoGui() {return playerInfoGui;}
    public levelSystem getLevels() {return levels;}
    public kitGUI getKitGui() {return kitGui;}
    public RTPSystem getRTP() {return rtp;}
    public rankSystem getRanks() {return ranks;}
    public placeholderSystem getPlaceholders() {return placeholders;}
}

// Thank you for choosing Siddy's Simple Plugin :D
// If you are a developer, consider emailing me directly at siddy0060@gmail.com if you find any MAJOR BUGS
// Or just put them in the Issue Tracker in github. It would be very grateful if you could point out some
// Thanks, again?