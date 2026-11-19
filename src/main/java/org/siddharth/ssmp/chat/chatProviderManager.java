package org.siddharth.ssmp.chat;
import org.bukkit.entity.Player;
import org.siddharth.ssmp.mechanics.rankSystem;
import org.siddharth.ssmp.ssmp;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
public class chatProviderManager {
    private final ssmp plugin;
    private final rankSystem ranks;
    private final List<Function<Player, String>> prefixProviders = new CopyOnWriteArrayList<>();
    private final List<Function<Player, String>> suffixProviders = new CopyOnWriteArrayList<>();
    private volatile Function<Player, String> nameProvider;
    public chatProviderManager(ssmp plugin, rankSystem ranks) {
        this.plugin = plugin;
        this.ranks = ranks;
    }

    public void registerPrefixProvider(Function<Player, String> provider) {
        if (provider != null && !prefixProviders.contains(provider)) {
            prefixProviders.add(provider);
        }
    }

    public void registerSuffixProvider(Function<Player, String> provider) {
        if (provider != null && !suffixProviders.contains(provider)) {
            suffixProviders.add(provider);
        }
    }

    public void setNameProvider(Function<Player, String> provider) {
        nameProvider = provider;
    }

    public String getPrefix(Player player) {
        StringBuilder result = new StringBuilder();
        String rankPrefix = ranks.getPlayerPrefix(player);
        if (rankPrefix != null) result.append(rankPrefix);
        for (Function<Player, String> provider : prefixProviders) {
            String value = apply(provider, player);
            if (value != null) result.append(value);
        }

        return result.toString();
    }

    public String getSuffix(Player player) {
        StringBuilder result = new StringBuilder();
        String rankSuffix = ranks.getPlayerSuffix(player);
        if (rankSuffix != null) result.append(rankSuffix);
        for (Function<Player, String> provider : suffixProviders) {
            String value = apply(provider, player);
            if (value != null) result.append(value);
        }
        return result.toString();
    }

    public String getName(Player player) {
        Function<Player, String> provider = nameProvider;
        if (provider != null) {
            String value = apply(provider, player);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return player.getName();
    }

    private String apply(Function<Player, String> provider, Player player) {
        try {
            return provider.apply(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Chat provider failed: " + e.getMessage());
            return null;
        }
    }
}