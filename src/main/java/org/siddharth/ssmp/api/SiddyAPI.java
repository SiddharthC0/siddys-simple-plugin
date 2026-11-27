package org.siddharth.ssmp.api;
import org.siddharth.ssmp.SiddysSimplePlugin;
import org.siddharth.ssmp.commands.KitCommands;
import org.siddharth.ssmp.mechanics.*;
import org.siddharth.ssmp.economy.*;
import org.siddharth.ssmp.gui.*;
import org.siddharth.ssmp.controlpanel.ControlPanel;
import org.siddharth.ssmp.moderation.*;
import org.siddharth.ssmp.information.SupportMain;
import org.siddharth.ssmp.util.CoreUtilMethods;

public final class SiddyAPI {
    private final SiddysSimplePlugin plugin;
    public SiddyAPI(SiddysSimplePlugin plugin) {
        if (plugin == null) {throw new IllegalArgumentException("SSMP plugin cannot be null.");}
        this.plugin = plugin;
    }

    public SiddysSimplePlugin getSSMP() {return plugin;}
    public boolean isAvailable() {return plugin.isEnabled();}
    public String getVersion() {return plugin.getDescription().getVersion();}

    public CoreUtilMethods getCore() {return plugin.getCore();}
    public PermissionSystem getPermissions() {return plugin.getPermissions();}
    public SoundSystem getSounds() {return plugin.getSounds();}
    public RankSystem getRanks() {return plugin.getRanks();}
    public PlaceholderSystem getPlaceholders() {return plugin.getPlaceholders();}
    public TabListSystem getTablist() {return plugin.getTablist();}
    public ScoreboardSystem getScoreboard() {return plugin.getScoreboard();}
    public Economy getEconomy() {return plugin.getEconomy();}
    public EconomyCommands getEconomyCommands() {return plugin.getEconomyCommands();}
    public AuctionHouse getAuctionHouse() {return plugin.getAuction();}
    public ShopSystem getShop() {return plugin.getShop();}
    public EnderChestSystem getEnderChest() {return plugin.getEnderChest();}
    public HomeSystem getHomes() {return plugin.getHomes();}
    public WarpSystem getWarps() {return plugin.getWarps();}
    public MenuSystem getMenu() {return plugin.getMenu();}
    public NicknameSystem getNicknames() {return plugin.getNicknames();}
    public KitSystem getKits() {return plugin.getKits();}
    public KitGUI getKitGui() {return plugin.getKitGui();}
    public KitCommands getKitCommands() {return plugin.getKitCommands();}
    public RTPSystem getRTP() {return plugin.getRTP();}
    public OptimizationSystem getOptimization() {return plugin.getOptimization();}
    public LevelSystem getLevels() {return plugin.getLevels();}
    public ContractSystem getContracts() {return plugin.getContracts();}
    public ContractGUI getContractGui() {return plugin.getContractGui();}
    public BountySystem getBounties() {return plugin.getBounties();}
    public BountyGUI getBountyGui() {return plugin.getBountyGui();}
    public PlayerInformationSystem getPlayerInfo() {return plugin.getPlayerInfo();}
    public PlayerInformationGUI getPlayerInfoGui() {return plugin.getPlayerInfoGui();}
    public ControlPanel getControl() {return plugin.getControl();}
    public ModerationSystem getModeration() {return plugin.getModeration();}
    public SupportMain getSupport() {return plugin.getSupport();}
    public InfoBarSystem getInfoBar() {return plugin.getInfoBar();}
    public InfoBarGUI getInfoBarMenu() {return plugin.getInfoBarMenu();}
    public ConfigSystem getConfigMenu() {return plugin.getConfigMenu();}
}