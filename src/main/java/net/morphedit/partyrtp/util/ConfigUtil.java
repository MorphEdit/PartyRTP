package net.morphedit.partyrtp.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ConfigUtil {
    private ConfigUtil() {}

    public record Tier(int maxSize, int cooldownSeconds, int nearRadius) {}

    public static Tier resolveTier(JavaPlugin plugin, Player p) {
        boolean enabled = plugin.getConfig().getBoolean("limits.enabled", false);
        if (!enabled) {
            return new Tier(
                    plugin.getConfig().getInt("party.maxSize", 6),
                    plugin.getConfig().getInt("go.cooldownSeconds", 300),
                    plugin.getConfig().getInt("party.nearRadius", 8)
            );
        }

        List<?> tiers = plugin.getConfig().getList("limits.tiers");
        if (tiers == null) return fallback(plugin);

        for (int i = 0; i < tiers.size(); i++) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("limits.tiers." + i);
            if (sec == null) continue;

            String perm = sec.getString("permission", "");
            if (!perm.isBlank() && p.hasPermission(perm)) {
                int maxSize = sec.getInt("maxSize", plugin.getConfig().getInt("party.maxSize", 6));
                int cd = sec.getInt("cooldownSeconds", plugin.getConfig().getInt("go.cooldownSeconds", 300));
                int radius = sec.getInt("nearRadius", plugin.getConfig().getInt("party.nearRadius", 8));
                return new Tier(maxSize, cd, radius);
            }
        }

        return fallback(plugin);
    }

    private static Tier fallback(JavaPlugin plugin) {
        return new Tier(
                plugin.getConfig().getInt("party.maxSize", 6),
                plugin.getConfig().getInt("go.cooldownSeconds", 300),
                plugin.getConfig().getInt("party.nearRadius", 8)
        );
    }

    public static boolean isWorldAllowed(JavaPlugin plugin, String worldName) {
        boolean enabled = plugin.getConfig().getBoolean("worlds.enabled", false);
        if (!enabled) return true;

        String mode = plugin.getConfig().getString("worlds.mode", "BLACKLIST");
        List<String> list = plugin.getConfig().getStringList("worlds.list");

        boolean inList = list.stream().anyMatch(w -> w.equalsIgnoreCase(worldName));
        if ("WHITELIST".equalsIgnoreCase(mode)) {
            return inList;
        }
        // BLACKLIST default
        return !inList;
    }
}
