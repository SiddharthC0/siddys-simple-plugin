// Wassup, welcome to Siddy's Simple Plugin's home file. This file is responsible for all the tasks the plugin performs, as it
// is the one that controls which module can be enabled and all that, also the config.yml..
// I've added various comments in this file to help you set up your own plugin if you forked this repository. This will
// get you fluent with the functions and all of the plugin. To begin with, this file contains the getters for other files
// that want to access a specfic file.
package org.siddharth.ssmp;

// General imports
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// The import of the main files
import org.siddharth.ssmp.chat.ChatFormatting;
import org.siddharth.ssmp.commands.*;
import org.siddharth.ssmp.economy.AuctionHouse;
import org.siddharth.ssmp.gui.*;
import org.siddharth.ssmp.information.JoinMotdSystem;
import org.siddharth.ssmp.support.VaultSupport;
import org.siddharth.ssmp.economy.Economy;
import org.siddharth.ssmp.economy.EconomyCommands;
import org.siddharth.ssmp.economy.ShopSystem;
import org.siddharth.ssmp.mechanics.*;
import org.siddharth.ssmp.updateChecker.UpdateChecker;
import org.siddharth.ssmp.information.Information;
import org.siddharth.ssmp.controlpanel.ControlPanel;
import org.siddharth.ssmp.support.PlacholderAPISupport;
import org.siddharth.ssmp.moderation.ModerationSystem;
import org.siddharth.ssmp.moderation.ModerationCommands;
import org.siddharth.ssmp.information.SupportMain;

// Utility files
import org.siddharth.ssmp.util.CoreUtilMethods;

public final class SiddysSimplePlugin extends JavaPlugin {
    String version = getDescription().getVersion(); // If you want to customize version of plugin, consider looking resources/plugin.yml
    String code_name = "Spice"; // Use this to customize the short name (code name) of the release.
    String full_name = "Improved"; // The full name of the RELEASE, not the plugin...
    String plugin_name = "Siddy's Simple Plugin"; // Name of the PLUGIN. This is used widely, be careful!
    String plugin_prefix = "[Siddy's Simple Plugin]"; // The prefix used to print useful messages. Not currently applicable to all msgs.
    String creator_name = "Siddy"; // Config the name of the creator (ur name :D)
    Boolean debug_mode = true; // Toggle debug mode which prints important information...

    // Defination of all the modules that have to be loaded from here
    private RankSystem ranks;
    private PermissionSystem permissions;
    private SoundSystem sounds;
    private PlacholderAPISupport papiExpansion;
    private PlaceholderSystem placeholders;
    private TabListSystem tablist;
    private ScoreboardSystem scoreboard;
    private AuctionHouse auction;
    private EconomyCommands economyCommands;
    private EnderChestSystem enderChest;
    private HomeSystem homes;
    private WarpSystem warps;
    private MenuSystem menu;
    private NicknameSystem nicknames;
    private KitSystem kits;
    private KitGUI kitGui;
    private KitCommands kitCommands;
    private CoreUtilMethods core;
    private RTPSystem rtp;
    private OptimizationSystem optimization;
    private LevelSystem levels;
    private Economy economy;
    private ContractSystem contracts;
    private ContractGUI contractGui;
    private BountySystem bounties;
    private BountyGUI bountyGui;
    private PlayerInformationSystem playerInfo;
    private PlayerInformationGUI playerInfoGui;
    private ControlPanel control;
    private ModerationSystem moderation;
    private ShopSystem shop;
    private AuctionHouse auctionHouse;
    private SupportMain support;
    private InfoBarSystem infoBar;
    private InfoBarGUI infoBarMenu;
    private ConfigSystem configMenu;
    public CoreUtilMethods getCore() {return core;}

    // Stuff that happens when the server starts with the plugin installed
    // Check for updates, Update manifest (if any changes), Create/Edit configuration file and load modules
    @Override
    public void onEnable() {
        new UpdateChecker(this).check();
        new Information(this).create();
        core = new CoreUtilMethods(this);
        createConfig();
        initializeModules();

        // configure this message if you want
        getLogger().info(plugin_prefix+" Enabled plugin version "+version);
        getLogger().info(plugin_prefix+" By "+creator_name);
        getLogger().info(plugin_prefix+" Code "+code_name+" Release "+full_name);
    }

    // uhh this method was added for that hot reloading thing... Because we wanna hot reload some time in the near future, ts, it REQUIRED.
    // if you want to configure multiple stuff or smth, use this.
    public void initializeModules() {
        // Very very very critical stuff
        configMenu = new ConfigSystem(this);
        try {
            ranks = new RankSystem(this);
        } catch (Exception e) {getLogger().warning("The ranks system failed to load: " + e.getMessage());}
        try {
            if (ranks != null) {
                permissions = new PermissionSystem(this, ranks);
            } else {
                getLogger().warning("The permissions system was disabled because ranks failed to load.");
            }
        } catch (Exception e) {getLogger().warning("The permissions system failed to load: "+e.getMessage());}

        try {
            sounds = new SoundSystem(this);
        } catch (Exception e) {getLogger().warning("The sounds system failed to load: "+e.getMessage());}

        try {
            nicknames = new NicknameSystem(this);
        } catch (Exception e) {getLogger().warning("The nicknames system failed to load: "+e.getMessage());}

        try {
            if (sounds != null) {
                menu = new MenuSystem(this, sounds);
                getServer().getPluginManager().registerEvents(menu, this);
            } else {
                getLogger().warning("The menu system was disabled because sounds failed to load");
            }
        } catch (Exception e) {getLogger().warning("The menu system failed to load: "+e.getMessage());}

        // Support system
        try {
            support = new SupportMain(this);

            if (getCommand("support") != null) {
                getCommand("support").setExecutor(support);
                getCommand("support").setTabCompleter(support);
            }
        } catch (Exception e) {getLogger().warning("The support system failed to load: "+e.getMessage());}

        // Join MOTD
        try {
            getServer().getPluginManager().registerEvents(new JoinMotdSystem(this), this);
        } catch (Exception e) {
            getLogger().warning("The Join MOTD system failed to load: " + e.getMessage());
        }

        // Info bar
        try {
            if (isEnabled("infobar")) {
                infoBar = new InfoBarSystem(this);
                if (infoBar.isAvailable()) {
                    infoBarMenu = new InfoBarGUI(this, infoBar);
                    InfoBarCommands commands = new InfoBarCommands(this, infoBar, infoBarMenu);
                    if (getCommand("infobar") != null) {
                        getCommand("infobar").setExecutor(commands);
                        getCommand("infobar").setTabCompleter(commands);
                    } else {
                        getLogger().warning("InfoBar command is missing from plugin.yml.");
                    }
                }
            }
        } catch (Exception e) {
            getLogger().severe("InfoBar subsystem failed: " + e.getMessage());
        }

        // Moderation System
        try {
            moderation = new ModerationSystem(this);
            ModerationCommands moderationCommandHandler = new ModerationCommands(moderation);

            String[] moderationCommandNames = {
                    "mute", "unmute", "warn", "ban", "unban",
                    "filter", "history", "resetviolations"
            };

            for (String commandName : moderationCommandNames) {
                if (getCommand(commandName) != null) {
                    getCommand(commandName).setExecutor(moderationCommandHandler);
                    getCommand(commandName).setTabCompleter(moderationCommandHandler);
                } else {
                    getLogger().warning("Missing moderation command in plugin.yml: " + commandName);
                }
            }
        } catch (Exception e) {
            getLogger().severe("Moderation failed to load: " + e.getMessage());
            e.printStackTrace();
        }

        // Control Panel
        try {
            if (isEnabled("cpanel")) {
                control = new ControlPanel(this);
                control.setUpdateCheckAction(() -> new UpdateChecker(this).check());

                if (getCommand("cpanel") != null) {
                    getCommand("cpanel").setExecutor((sender, command, label, args) -> {
                        if (!sender.hasPermission("ssmp.cpanel")) {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                            return true;
                        }

                        if (args.length > 1) {
                            sender.sendMessage(ChatColor.RED + "Usage: /cpanel [start|stop|status]");
                            return true;
                        }

                        String action = args.length == 0 ? "start" : args[0].toLowerCase();

                        switch (action) { // uhh so if u reading this, just replace the entire comamnd section with your own command java file if you want..
                            case "start" -> {
                                if (control.isRunning()) {
                                    sender.sendMessage(ChatColor.YELLOW + "The Control Panel is already running.");
                                    return true;
                                }

                                control.start();

                                if (control.isRunning()) {
                                    sender.sendMessage(ChatColor.GREEN + "The Control Panel server has started.");
                                    sender.sendMessage(ChatColor.GRAY + "Check the server console for its address.");
                                    sender.sendMessage(ChatColor.GREEN + "And if you want any support or is confused, just do /support... only /support without any args will open the quick start guide");
                                } else {
                                    sender.sendMessage(ChatColor.RED + "The Control Panel could not be started. Check the console.");
                                }
                            }

                            case "stop" -> {if (!control.isRunning()) {
                                sender.sendMessage(ChatColor.YELLOW + "The Control Panel is already stopped.");
                                return true;}
                                control.stop();
                                sender.sendMessage(ChatColor.GREEN + "The Control Panel server has stopped.");}
                            case "status" -> sender.sendMessage(ChatColor.GRAY + "Control Panel status: " + (control.isRunning() ? ChatColor.GREEN + "Running" : ChatColor.RED + "Stopped"));
                            default -> sender.sendMessage(ChatColor.RED + "Usage: /cpanel [start|stop|status]");
                        }

                        return true;
                    });
                } else {
                    getLogger().warning("Missing cpanel command in plugin.yml.");
                }
            } else if (getCommand("cpanel") != null) { // js in case your smartypants removed it from plugin.yml
                getCommand("cpanel").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The control panel is currently "+ChatColor.BLACK+"DISABLED");
                    sender.sendMessage(ChatColor.GREEN+"Enable the control panel from SiddysSimplePlugin/config.yml");
                    sender.sendMessage(ChatColor.AQUA+"Or use the /spconfig or /configp to open the SiddysSimplePlugin module enable/disable config menu.");
                    return true;
                });
            }
        } catch (Exception e) {
            getLogger().warning("Failed to initialize Control Panel: " + e.getMessage());
        }

        // Optimizer
        try {
            optimization = new OptimizationSystem(this);
            optimization.initialize();
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start optimization system: " + e.getMessage());
            }
        }

        // Main menu system
        try {
            if (getCommand("ssmp") != null && sounds != null && menu != null && support != null) {
                getCommand("ssmp").setExecutor(new CommandSystem(this, sounds, menu, support));
            } else {
                getLogger().warning("Fatal fallback: menu system couldn't load because some important modules were disabled. SiddysSimplePlugin will FAIL to load...");
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start menu system: " + e.getMessage());
            }
        }

        // Levels system
        try {
            if (isEnabled("levels")) {
                levels = new LevelSystem(this);
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start level system: " + e.getMessage());
            }
        }

        // Tablist System
        try {
            if (isEnabled("tablist")) {
                tablist = new TabListSystem(this, ranks);
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
                kits = new KitSystem(this);
                kitGui = new KitGUI(this, kits);
                kitCommands = new KitCommands(this, kits, kitGui);

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
                getCommand("nick").setExecutor(new NicknameCommands(nicknames));
                getCommand("realname").setExecutor((sender, command, label, args) -> {
                    if (args.length == 0) {
                        sender.sendMessage("Usage: /realname <nickname>");
                        return true;
                    }
                    String nickname = String.join(" ", args);
                    String realname = nicknames.revealName(nickname);
                    if (realname == null) {
                        sender.sendMessage("No player found with nickname " + nickname);
                        return true;
                    }
                    sender.sendMessage(realname);
                    return true;
                });
                getCommand("realname").setTabCompleter((sender, command, alias, args) -> {
                    if (args.length != 1) {return Collections.emptyList();}
                    String input = args[0].toLowerCase();
                    return nicknames.getRegisteredNicknames().stream().filter(nickname -> nickname.toLowerCase().startsWith(input)).collect(Collectors.toList());});
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
                economy = new Economy(this, sounds);
                getServer().getPluginManager().registerEvents(economy, this);
                economyCommands = new EconomyCommands(economy);
                if (getCommand("economy") != null) {
                    getCommand("economy").setExecutor(economyCommands);
                    getCommand("economy").setTabCompleter(economyCommands);
                }

                if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                    Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new VaultSupport(this, economy), this, org.bukkit.plugin.ServicePriority.Normal);
                    getLogger().info("Vault economy support enabled.");
                }
            } else {
                getConfig().set("modules.shop.enabled", false);
                getConfig().set("modules.contracts.enabled", false);
                getConfig().set("modules.bounties.enabled", false);
                getConfig().set("modules.playerinfo.enabled", false);
                getConfig().set("modules.auction.enabled", false);
                saveConfig();

                getCommand("economy").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start economy system: " + e.getMessage());
            }
        }

        // Shop system
        try {
            if (isEnabled("shop") && economy != null) {
                shop = new ShopSystem(economy, this, sounds);
                getServer().getPluginManager().registerEvents(shop, this);
                if (getCommand("shop") != null) {
                    getCommand("shop").setExecutor(shop);
                }
            } else {
                getCommand("shop").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("store").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
            }
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start shop system: " + e.getMessage());
            }
        }

        // Contracts system
        try {
            if (isEnabled("contracts") && economy != null && levels != null) {
                contracts = new ContractSystem(this, economy, levels);
                contractGui = new ContractGUI(contracts);
                getServer().getPluginManager().registerEvents(contracts, this);
                getServer().getPluginManager().registerEvents(contractGui, this);
                if (getCommand("contracts") != null) {
                    getCommand("contracts").setExecutor(new ContractCommands(contractGui));
                }
            } else {
                getCommand("contracts").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("contractsgui").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("contractgui").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("contract").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start contracts system: " + e.getMessage());
            }
        }

        // Bounty system
        try {
            if (isEnabled("bounties") && economy != null) {
                bounties = new BountySystem(this, economy);
                bountyGui = new BountyGUI(bounties, economy);

                getServer().getPluginManager().registerEvents(bounties, this);
                getServer().getPluginManager().registerEvents(bountyGui, this);
                BountyCommands bountyCommands = new BountyCommands(bountyGui);
                if (getCommand("bounties") != null) {
                    getCommand("bounties").setExecutor(bountyCommands);
                }

                if (getCommand("bounty") != null) {
                    getCommand("bounty").setExecutor(bountyCommands);
                }
            } else {
                getCommand("bounty").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("bounties").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("bountygui").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start bounty system: " + e.getMessage());
            }
        }

        // Player information system
        try {
            if (isEnabled("economy") && levels != null && contracts != null && bounties != null) {
                playerInfo = new PlayerInformationSystem(this, economy, levels, contracts, bounties);
                playerInfoGui = new PlayerInformationGUI(playerInfo);
                getServer().getPluginManager().registerEvents(playerInfoGui, this);
                if (playerInfoGui != null && getCommand("playerinfo") != null) {
                    getCommand("playerinfo").setExecutor(
                            new PlayerInformationCommands(playerInfoGui)
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
            if (isEnabled("scoreboard")) {
                scoreboard = new ScoreboardSystem(economy, this);
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
                MenuItemSystem menuItem = new MenuItemSystem(this);
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
                auction = new AuctionHouse(this, economy, permissions, sounds);
                getServer().getPluginManager().registerEvents(auction, this);
                if (getCommand("ah") != null) {
                    getCommand("ah").setExecutor(auction);
                    getCommand("ah").setTabCompleter(auction);
                }
            } else {
                getCommand("ah").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module AUCTIONHOUSE is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("auctionhouse").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module AUCTIONHOUSE is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
                getCommand("auction").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module AUCTIONHOUSE is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start auction system: " + e.getMessage());
            }
        }

        // Ender chest system
        try {
            if (isEnabled("enderchest")) {
                enderChest = new EnderChestSystem(this);
                getServer().getPluginManager().registerEvents(enderChest, this);

                if (getCommand("enderchest") != null) {
                    EnderChestSystem finalEnderChest = enderChest;
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
                    });
                }
            } else {
                if (getCommand("enderchest") != null) {
                    getCommand("enderchest").setExecutor((sender, command, label, args) -> {
                        sender.sendMessage(ChatColor.RED + "The module ENDERCHEST is currently disabled.");
                        sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                        return true;
                    });
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
                homes = new HomeSystem(this);
                getServer().getPluginManager().registerEvents(homes, this);
                HomeCommands homeCommands = new HomeCommands(homes);
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
            } else {
                getCommand("homes").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module HOMES is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start homes system: " + e.getMessage());
            }
        }

        // Warps system
        try {
            if (isEnabled("warps")) {
                warps = new WarpSystem(this);
                getServer().getPluginManager().registerEvents(warps, this);
                WarpCommands warpCommands = new WarpCommands(warps);
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
            } else {
                getCommand("warps").setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(ChatColor.RED + "The module WARPS is currently disabled.");
                    sender.sendMessage(ChatColor.RED + "Ask an op to do /spconfig and configure the enabling/disabling");
                    return true;
                });
            }
        } catch (Exception e) {
            if (debug_mode == true) {
                getLogger().warning("Failed to start warpsSystem: " + e.getMessage());
            }
        }

        // Placeholder System
        try {
            if (isEnabled("placeholders")) {
                placeholders = new PlaceholderSystem(this, ranks);
                if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    papiExpansion = new PlacholderAPISupport(this, placeholders, ranks, contracts, bounties,
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
                ChatFormatting chat = new ChatFormatting(this, ranks, sounds, placeholders);
                // look at this mom, i finally mastered ternarys :D
                chat.setNameProvider(player -> nicknames != null ? nicknames.getFormattedNickname(player) : player.getName());
                getServer().getPluginManager().registerEvents(chat, this);
            }
        } catch (Exception e) {
            if  (debug_mode == true) {
                getLogger().warning("Failed to start chat system: " + e.getMessage());
            }
        }
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
        shutdownSafely("InfoBar", () -> {
            if (infoBar != null) {infoBar.shutdown();}
        });

        // Save and reload
        try {
            if (levels != null) {
                levels.save();
            }
        } catch (Exception e) {getLogger().warning("Failed to save levels: " + e.getMessage());}
        try {
            if (bounties != null) {
                bounties.save();
            }
        } catch (Exception e) {getLogger().warning("Failed to save bounties: " + e.getMessage());}
        if (contracts != null) {
            contracts.reload();
        }
        if (papiExpansion != null) {
            papiExpansion.unregister();
        }

        // Finally print the message notifying user that plugin disabled.
        getLogger().info(plugin_name+" has been disabled");
    }

    // meth that unloads modules
    private void unloadModules() {
        shutdownSafely("Control Panel", () -> {if (control != null) control.stop();});
        shutdownSafely("Optimization", () -> {if (optimization != null) optimization.shutdown();});
        shutdownSafely("Placeholders", () -> {if (placeholders != null) placeholders.stop();});
        shutdownSafely("Moderation", () -> {if (moderation != null) moderation.close();});
        shutdownSafely("InfoBar", () -> {if (infoBar != null) infoBar.shutdown();});
        shutdownSafely("Levels", () -> {if (levels != null) levels.save();});
        shutdownSafely("Bounties", () -> {if (bounties != null) bounties.save();});
        shutdownSafely("Contracts", () -> {if (contracts != null) contracts.reload();});
        shutdownSafely("PlaceholderAPI", () -> {if (papiExpansion != null) papiExpansion.unregister();});
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        Bukkit.getServicesManager().unregisterAll(this);

        // all the commands in the plugin
        String[] commands = {
                "support", "infobar", "mute", "unmute", "warn", "ban", "unban", "filter", "history", "resetviolations",
                "rank", "cpanel", "ssmp", "rtp", "kit", "nick", "economy", "shop", "contracts", "bounties", "bounty",
                "playerinfo", "ah", "enderchest", "sethome", "home", "homes", "delhome", "setwarp", "warp", "warps",
                "delwarp", "configp"
        };

        for (String commandName : commands) {
            if (getCommand(commandName) != null) {
                getCommand(commandName).setExecutor(null);
                getCommand(commandName).setTabCompleter(null);
            }
        }

        ranks = null;
        permissions = null;
        sounds = null;
        papiExpansion = null;
        placeholders = null;
        tablist = null;
        scoreboard = null;
        auction = null;
        economyCommands = null;
        enderChest = null;
        homes = null;
        warps = null;
        menu = null;
        nicknames = null;
        kits = null;
        kitGui = null;
        kitCommands = null;
        rtp = null;
        optimization = null;
        levels = null;
        economy = null;
        contracts = null;
        contractGui = null;
        bounties = null;
        bountyGui = null;
        playerInfo = null;
        playerInfoGui = null;
        control = null;
        moderation = null;
        shop = null;
        auctionHouse = null;
        support = null;
        infoBar = null;
        infoBarMenu = null;
        configMenu = null;
    }

    // meth that reloads modules.. not the entire plugin.
    public boolean reloadModules() {
        try {
            unloadModules();
            reloadConfig();
            initializeModules();

            getLogger().info(plugin_name+" modules have been reloaded.");
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to reload " + plugin_name + " modules: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    // Reload the plugin (a command btw)
    public boolean reloadSSMP() {
        try {
            reloadConfig();
            if (optimization != null) {optimization.refresh();}
            if (sounds != null) {sounds.reload();}
            if (rtp != null) {rtp.reload();}
            if (scoreboard != null) {scoreboard.reload();}
            if (economy != null) {economy.reload();}
            if (tablist != null) {// Pls add tablist reload here
                }
            if (kits != null) {kits.reload();}
            if (shop != null) {// pls add shop reload here
                }
            if (moderation != null) {moderation.reloadFromConfig();}
            if (infoBar != null) { infoBar.reload(); }
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

        config.setComments("", List.of(
                "Welcome to Siddy's Simple Plugin config.yml",
                "This file contains all the core enabling and disabling of modules, along with the configuration of standalone modules",
                "If you do not understand this, or how to do stuff, I recommend that you use the QuickStartGuide.pdf attached in the SiddysSimplePlugin folder",
                "Quick Start Guide however will not introduce you to all features, but will be enough...",
                "Read documentation and please report bugs on the issue tracker...",
                "Thank you :)"
        ));

        // These are modules with standalone configs.
        config.setComments("", List.of("Use this modules section to enable and disable modules (both core and standalone)"));
        config.addDefault("modules.permissions.enabled", true);
        config.addDefault("modules.economy.enabled", true);
        config.addDefault("modules.contracts.enabled", true);
        config.addDefault("modules.bounties.enabled", true);
        config.addDefault("modules.shop.enabled", true);
        config.addDefault("modules.warps.enabled", true);
        config.addDefault("modules.kits.enabled", true);
        config.addDefault("modules.rtp.enabled", true);
        config.addDefault("modules.cpanel.enabled", false);
        config.addDefault("modules.levels.enabled", true);
        config.addDefault("modules.scoreboard.enabled", true);
        config.addDefault("modules.chat.enabled", true);
        config.addDefault("modules.placeholders.enabled", true);
        config.addDefault("modules.menu.enabled", true);
        config.addDefault("modules.auction.enabled", true);
        config.addDefault("modules.tablist.enabled", true);
        config.addDefault("modules.enderchest.enabled", true);
        config.addDefault("modules.homes.enabled", true);
        config.addDefault("modules.infobar.enabled", true);
        config.addDefault("modules.nickname.enabled", true);

        // These are modules with core config
        // Auction house
        config.setComments("", List.of("Use this modules section to configure core modules"));
        config.addDefault("modules.auction.max-auction-price", 1000000);
        config.addDefault("modules.auction.negate-buy-price", 0);
        config.addDefault("modules.auction.max-pages", 10);
        config.addDefault("modules.auction.non-sellable-items", List.of("COMPASS"));

        // Tablist
        config.addDefault("modules.tablist.header.enabled", true);
        config.addDefault("modules.tablist.header.lines", List.of(
                        "",
                        "&cSiddy's Default Tablist",
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
        config.addDefault("modules.enderchest.doubled", false);

        // Homes
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
    public HomeSystem getHomes() {return homes;}
    public EnderChestSystem getEnderChest() {return enderChest;}
    public WarpSystem getWarps() {return warps;}
    public Economy getEconomy() {return economy;}
    public ContractSystem getContracts() {return contracts;}
    public ContractGUI getContractGui() {return contractGui;}
    public BountySystem getBounties() {return bounties;}
    public BountyGUI getBountyGui() {return bountyGui;}
    public PlayerInformationSystem getPlayerInfo() {return playerInfo;}
    public PlayerInformationGUI getPlayerInfoGui() {return playerInfoGui;}
    public LevelSystem getLevels() {return levels;}
    public KitGUI getKitGui() {return kitGui;}
    public RTPSystem getRTP() {return rtp;}
    public RankSystem getRanks() {return ranks;}
    public PlaceholderSystem getPlaceholders() {return placeholders;}
    public ConfigSystem getConfigSystem() {return configMenu;}
    public SupportMain getSupport() {return support;}
}

// Thank you for choosing Siddy's Simple Plugin :D
// If you are a developer, consider emailing me directly at siddy0060@gmail.com if you find any MAJOR BUGS
// Or just put them in the Issue Tracker in github. It would be very grateful if you could point out some
// Thanks, again?