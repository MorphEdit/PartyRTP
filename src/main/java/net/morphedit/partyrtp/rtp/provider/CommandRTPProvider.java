package net.morphedit.partyrtp.rtp.provider;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandRTPProvider implements RTPProvider {

    private final JavaPlugin plugin;

    public CommandRTPProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean requestRtp(Player leader) {
        // New config path
        String cmd = plugin.getConfig().getString("rtp.command.execute", null);

        // Fallback old paths (compatibility)
        if (cmd == null) cmd = plugin.getConfig().getString("rtp.command", null);
        if (cmd == null) cmd = plugin.getConfig().getString("rtp. command", null);
        if (cmd == null) cmd = plugin.getConfig().getString("rtp.mode", null) != null
                ? plugin.getConfig().getString("rtp.command", null)
                : null;

        if (cmd == null || cmd.isBlank()) {
            plugin.getLogger().warning("RTP command not configured! Check config.yml -> rtp.command.execute");
            return false;
        }

        cmd = PlaceholderUtil.apply(cmd, leader);

        // Remove leading "/" if user added it
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        try {
            boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            if (!result) {
                plugin.getLogger().warning("RTP command dispatch failed for player " + leader.getName() +
                        ". Command: " + cmd);
            }
            return result;
        } catch (Exception e) {
            plugin.getLogger().severe("Exception while executing RTP command: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getName() {
        return "COMMAND";
    }
}