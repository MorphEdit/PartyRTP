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
        // new config path
        String cmd = plugin.getConfig().getString("rtp.command.execute", null);

        // fallback old paths (compat)
        if (cmd == null) cmd = plugin.getConfig().getString("rtp.command", null);
        if (cmd == null) cmd = plugin.getConfig().getString("rtp. command", null); // harmless typo-guard
        if (cmd == null) cmd = plugin.getConfig().getString("rtp.mode", null) != null
                ? plugin.getConfig().getString("rtp.command", null)
                : null;

        if (cmd == null || cmd.isBlank()) {
            return false;
        }

        cmd = PlaceholderUtil.apply(cmd, leader);

        // remove leading "/" if user put it
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    @Override
    public String getName() {
        return "COMMAND";
    }
}
