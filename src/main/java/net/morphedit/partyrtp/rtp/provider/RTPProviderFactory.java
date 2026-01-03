package net.morphedit.partyrtp.rtp.provider;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class RTPProviderFactory {
    private RTPProviderFactory() {}

    public static RTPProvider create(JavaPlugin plugin) {
        String p = plugin.getConfig().getString("rtp.provider", "COMMAND");
        if (p == null) p = "COMMAND";
        p = p.toUpperCase(Locale.ROOT);

        // MVP: only COMMAND
        return new CommandRTPProvider(plugin);
    }
}
