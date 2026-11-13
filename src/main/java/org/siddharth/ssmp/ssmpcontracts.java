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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ssmpcontracts implements Listener {

    private final JavaPlugin plugin;
    private final ssmpeconomy economy;
    private final ssmplevels levels;
    private final File folder;
    private final File configFile;
    private final File contractsFile;
    private final File dataFile;
    private final YamlConfiguration config;
    private final YamlConfiguration contracts;
    private final YamlConfiguration data;

    public ssmpcontracts(
            JavaPlugin plugin,
            ssmpeconomy economy,
            ssmplevels levels
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.levels = levels;

        folder = new File(plugin.getDataFolder(), "contracts");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException(
                    "Failed to create contracts directory."
            );
        }

        configFile = new File(folder, "config.yml");
        contractsFile = new File(folder, "contracts.yml");
        dataFile = new File(folder, "players.yml");

        migrateLegacyFile(
                new File(plugin.getDataFolder(), "contracts.yml"),
                contractsFile
        );

        migrateLegacyFile(
                new File(plugin.getDataFolder(), "contractdata.yml"),
                dataFile
        );

        createFile(configFile);
        createFile(contractsFile);
        createFile(dataFile);

        config = YamlConfiguration.loadConfiguration(configFile);
        contracts = YamlConfiguration.loadConfiguration(contractsFile);
        data = YamlConfiguration.loadConfiguration(dataFile);

        config.addDefault("settings.max-active-contracts", 0);
        config.addDefault("settings.save-on-change", true);
        config.options().copyDefaults(true);
        save(config, configFile);

        if (!contracts.contains("contracts")) {
            contracts.createSection("contracts");
            save(contracts, contractsFile);
        }
    }

    private void migrateLegacyFile(File source, File destination) {
        if (destination.exists() || !source.exists()) {
            return;
        }

        try {
            Files.copy(
                    source.toPath(),
                    destination.toPath(),
                    StandardCopyOption.COPY_ATTRIBUTES
            );

            plugin.getLogger().info(
                    "Migrated " + source.getName()
                            + " into the contracts directory."
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to migrate " + source.getName() + ".",
                    e
            );
        }
    }

    private void createFile(File file) {
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new IOException(
                            "Could not create " + file.getName()
                    );
                }
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to create " + file.getName(),
                        e
                );
            }
        }
    }

    private void save(
            YamlConfiguration configuration,
            File file
    ) {
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe(
                    "Failed to save " + file.getName() + ": "
                            + e.getMessage()
            );
        }
    }

    private void saveData() {
        if (config.getBoolean("settings.save-on-change", true)) {
            save(data, dataFile);
        }
    }

    public List<String> getContracts() {
        ConfigurationSection section =
                contracts.getConfigurationSection("contracts");

        return section == null
                ? new ArrayList<>()
                : new ArrayList<>(section.getKeys(false));
    }

    public String getContractName(String id) {
        return contracts.getString("contracts." + id + ".name", id);
    }

    public String getContractDescription(String id) {
        return contracts.getString(
                "contracts." + id + ".description",
                ""
        );
    }

    public String getDifficulty(String id) {
        return contracts.getString(
                "contracts." + id + ".difficulty",
                "NORMAL"
        );
    }

    public int getTaskCount(String id) {
        return contracts.getMapList(
                "contracts." + id + ".tasks"
        ).size();
    }

    public String getTaskName(String id, int index) {
        return contracts.getString(
                "contracts." + id + ".tasks." + index + ".name",
                "Task " + (index + 1)
        );
    }

    public int getTaskAmount(String id, int index) {
        return Math.max(
                1,
                contracts.getInt(
                        "contracts." + id + ".tasks." + index + ".amount",
                        1
                )
        );
    }

    public int getTaskProgress(Player player, String id, int index) {
        return data.getInt(
                "players." + player.getUniqueId()
                        + ".contracts." + id + ".tasks." + index,
                0
        );
    }

    public boolean isAccepted(Player player, String id) {
        return data.getBoolean(
                "players." + player.getUniqueId()
                        + ".contracts." + id + ".accepted",
                false
        );
    }

    public boolean isCompleted(Player player, String id) {
        return data.getBoolean(
                "players." + player.getUniqueId()
                        + ".contracts." + id + ".completed",
                false
        );
    }

    public boolean acceptContract(Player player, String id) {
        if (player == null
                || !contracts.contains("contracts." + id)
                || isAccepted(player, id)
                || isCompleted(player, id)) {
            return false;
        }

        int maximum = config.getInt(
                "settings.max-active-contracts",
                0
        );

        if (maximum > 0 && getAcceptedCount(player) >= maximum) {
            player.sendMessage(
                    ChatColor.RED
                            + "You have reached the active contract limit."
            );
            return false;
        }

        data.set(
                "players." + player.getUniqueId()
                        + ".contracts." + id + ".accepted",
                true
        );

        saveData();
        return true;
    }

    public int getAcceptedCount(Player player) {
        if (player == null) {
            return 0;
        }

        int count = 0;

        for (String id : getContracts()) {
            if (isAccepted(player, id) && !isCompleted(player, id)) {
                count++;
            }
        }

        return count;
    }

    public int getCompletedCount(Player player) {
        if (player == null) {
            return 0;
        }

        int count = 0;

        for (String id : getContracts()) {
            if (isCompleted(player, id)) {
                count++;
            }
        }

        return count;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.isCancelled()) {
            return;
        }

        progressItem(
                player,
                event.getItem().getItemStack(),
                false
        );
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || event.isCancelled()
                || event.getCurrentItem() == null) {
            return;
        }

        progressItem(player, event.getCurrentItem(), true);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        if (!(event.getCaught() instanceof org.bukkit.entity.Item item)) {
            return;
        }

        progressItem(
                event.getPlayer(),
                item.getItemStack(),
                true
        );
    }

    private void progressItem(
            Player player,
            ItemStack item,
            boolean craftedOrCaught
    ) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        for (String id : getContracts()) {
            if (!isAccepted(player, id) || isCompleted(player, id)) {
                continue;
            }

            List<Map<?, ?>> tasks =
                    contracts.getMapList("contracts." + id + ".tasks");

            for (int i = 0; i < tasks.size(); i++) {
                Map<?, ?> task = tasks.get(i);

                String type = getTaskValue(task, "type");
                String materialName = getTaskValue(task, "material");
                Material material = Material.matchMaterial(materialName);

                if (material == null || material != item.getType()) {
                    continue;
                }

                if (type.equalsIgnoreCase("collectresource")
                        && !craftedOrCaught) {
                    addProgress(player, id, i, item.getAmount());
                } else if (type.equalsIgnoreCase("obtainitem")) {
                    addProgress(player, id, i, item.getAmount());
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) {
            return;
        }

        Player killer = entity instanceof Player player ? player.getKiller() : null;

        if (killer == null) {
            return;
        }

        for (String id : getContracts()) {
            if (!isAccepted(killer, id) || isCompleted(killer, id)) {
                continue;
            }

            List<Map<?, ?>> tasks =
                    contracts.getMapList("contracts." + id + ".tasks");

            for (int i = 0; i < tasks.size(); i++) {
                Map<?, ?> task = tasks.get(i);

                String type = getTaskValue(task, "type");
                String target = getTaskValue(task, "entity");

                if (type.equalsIgnoreCase("killtarget")
                        && target.equalsIgnoreCase(entity.getType().name())) {
                    addProgress(killer, id, i, 1);
                }

                if (type.equalsIgnoreCase("defeatboss")) {
                    String configuredName = getTaskValue(task, "name");
                    String customName = entity.getCustomName();

                    boolean typeMatches =
                            target.equalsIgnoreCase(entity.getType().name());

                    boolean nameMatches =
                            !configuredName.isBlank()
                                    && customName != null
                                    && ChatColor.stripColor(customName)
                                    .equalsIgnoreCase(
                                            ChatColor.stripColor(configuredName)
                                    );

                    if (typeMatches
                            && (configuredName.isBlank() || nameMatches)) {
                        addProgress(killer, id, i, 1);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer == null || killer.equals(event.getEntity())) {
            return;
        }

        for (String id : getContracts()) {
            if (!isAccepted(killer, id) || isCompleted(killer, id)) {
                continue;
            }

            List<Map<?, ?>> tasks =
                    contracts.getMapList("contracts." + id + ".tasks");

            for (int i = 0; i < tasks.size(); i++) {
                Map<?, ?> task = tasks.get(i);

                if (getTaskValue(task, "type")
                        .equalsIgnoreCase("bountyhunter")) {
                    addProgress(killer, id, i, 1);
                }
            }
        }
    }

    private void addProgress(
            Player player,
            String id,
            int index,
            int amount
    ) {
        int current = getTaskProgress(player, id, index);
        int required = getTaskAmount(id, index);

        if (current >= required) {
            return;
        }

        int updated = (int) Math.min(
                required,
                (long) current + amount
        );

        data.set(
                "players." + player.getUniqueId()
                        + ".contracts." + id + ".tasks." + index,
                updated
        );

        saveData();
        checkCompletion(player, id);
    }

    private void checkCompletion(Player player, String id) {
        int count = getTaskCount(id);

        if (count == 0) {
            return;
        }

        for (int i = 0; i < count; i++) {
            if (getTaskProgress(player, id, i) < getTaskAmount(id, i)) {
                return;
            }
        }

        if (isCompleted(player, id)) {
            return;
        }

        data.set(
                "players." + player.getUniqueId()
                        + ".contracts." + id + ".completed",
                true
        );

        data.set(
                "players." + player.getUniqueId()
                        + ".contracts." + id + ".accepted",
                false
        );

        saveData();
        giveRewards(player, id);

        player.sendMessage(
                ChatColor.GREEN + "Contract completed: "
                        + ChatColor.translateAlternateColorCodes(
                        '&',
                        getContractName(id)
                )
        );
    }

    private void giveRewards(Player player, String id) {
        String path = "contracts." + id + ".rewards";

        double money = contracts.getDouble(path + ".money", 0);
        long xp = contracts.getLong(path + ".xp", 0);
        int levelUpgrade = contracts.getInt(
                path + ".level-upgrade",
                0
        );

        if (economy != null && money > 0 && Double.isFinite(money)) {
            economy.addPlayerBalance(player, money);
        }

        if (levels != null && xp > 0) {
            levels.addXP(player, xp);
        }

        if (levels != null && levelUpgrade > 0) {
            long target = (long) levels.getLevel(player) + levelUpgrade;

            levels.setLevel(
                    player,
                    (int) Math.min(Integer.MAX_VALUE, target)
            );
        }

        for (String command : contracts.getStringList(
                path + ".commands"
        )) {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command.replace("{player}", player.getName())
            );
        }
    }

    private String getTaskValue(Map<?, ?> task, String key) {
        Object value = task.get(key);
        return value == null ? "" : String.valueOf(value);
    }
    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void save() {
        save(contracts, contractsFile);
        save(data, dataFile);
        save(config, configFile);
    }

    public void reload() {
        save();
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public double getRewardMoney(String id) {
        return contracts.getDouble(
                "contracts." + id + ".rewards.money",
                0
        );
    }

    public long getRewardXP(String id) {
        return contracts.getLong(
                "contracts." + id + ".rewards.xp",
                0
        );
    }

    public int getRewardLevelUpgrade(String id) {
        return contracts.getInt(
                "contracts." + id + ".rewards.level-upgrade",
                0
        );
    }

}