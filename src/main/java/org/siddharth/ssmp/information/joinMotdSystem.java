package org.siddharth.ssmp.information;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
public class joinMotdSystem implements Listener {

    private final JavaPlugin plugin;
    private final File motdFile;

    public joinMotdSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.motdFile = new File(plugin.getDataFolder(), "motd.txt");
        createMotd();
    }

    private void createMotd() {
        if (motdFile.exists()) return;
        try {
            plugin.getDataFolder().mkdirs();

            List<String> defaults = List.of(
                    "&b&lWelcome <player>!",
                    "&7Thank a lot for using &b&lSiddy's Simple Plugin&7!",
                    "&7Use the &f/support&7 command to open the Quick Start Guide...",
                    "&7Use the &f/support &c<section> <subsection>&7 command to open a specific resource page of support...",
                    "&6Also remember that the &7MAIN MENU&6 is available as a &7Compass&6 in your 9th slot!",
                    "&cNote that this is the default MOTD. Customize this from SiddysSimplePlugin/motd.txt"
            );
            Files.write(motdFile.toPath(), defaults, StandardCharsets.UTF_8);
        } catch (IOException e) {plugin.getLogger().severe("Failed to create motd.txt: " + e.getMessage());}
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            for (String line : Files.readAllLines(motdFile.toPath(), StandardCharsets.UTF_8)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', line.replace("<player>", player.getName())));
            }
        } catch (IOException e) {plugin.getLogger().warning("Failed to read motd.txt: " + e.getMessage());}
    }
}
