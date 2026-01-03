package net.morphedit.partyrtp.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ConfigUtil {
    private ConfigUtil() {}

    public record Tier(int maxSize, int cooldownSeconds, int nearRadius) {}

    public static Tier resolveTier(JavaPlugin plugin, Player p) {
        boolean enabled = plugin.getConfig().getBoolean("limits.enabled", Constants.DEFAULT_LIMITS_ENABLED);
        if (!enabled) {
            return new Tier(
                    plugin.getConfig().getInt("party.maxSize", Constants.DEFAULT_MAX_SIZE),
                    plugin.getConfig().getInt("go.cooldownSeconds", Constants.DEFAULT_COOLDOWN_SECONDS),
                    plugin.getConfig().getInt("party.nearRadius", Constants.DEFAULT_NEAR_RADIUS)
            );
        }

        List<?> tiers = plugin.getConfig().getList("limits.tiers");
        if (tiers == null || tiers.isEmpty()) {
            plugin.getLogger().warning("limits.enabled is true but no tiers defined. Using fallback values.");
            return fallback(plugin);
        }

        for (int i = 0; i < tiers.size(); i++) {
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("limits.tiers." + i);
            if (sec == null) continue;

            String perm = sec.getString("permission", "");
            if (!perm.isBlank() && p.hasPermission(perm)) {
                int maxSize = sec.getInt("maxSize", plugin.getConfig().getInt("party.maxSize", Constants.DEFAULT_MAX_SIZE));
                int cd = sec.getInt("cooldownSeconds", plugin.getConfig().getInt("go.cooldownSeconds", Constants.DEFAULT_COOLDOWN_SECONDS));
                int radius = sec.getInt("nearRadius", plugin.getConfig().getInt("party.nearRadius", Constants.DEFAULT_NEAR_RADIUS));
                return new Tier(maxSize, cd, radius);
            }
        }

        return fallback(plugin);
    }

    private static Tier fallback(JavaPlugin plugin) {
        return new Tier(
                plugin.getConfig().getInt("party.maxSize", Constants.DEFAULT_MAX_SIZE),
                plugin.getConfig().getInt("go.cooldownSeconds", Constants.DEFAULT_COOLDOWN_SECONDS),
                plugin.getConfig().getInt("party.nearRadius", Constants.DEFAULT_NEAR_RADIUS)
        );
    }

    public static boolean isWorldAllowed(JavaPlugin plugin, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            plugin.getLogger().warning("Attempted to check world permission for null/blank world name");
            return false;
        }

        boolean enabled = plugin.getConfig().getBoolean("worlds.enabled", Constants.DEFAULT_WORLDS_ENABLED);
        if (!enabled) return true;

        String mode = plugin.getConfig().getString("worlds.mode", Constants.DEFAULT_WORLD_MODE);
        List<String> list = plugin.getConfig().getStringList("worlds.list");

        if (list == null || list.isEmpty()) {
            plugin.getLogger().warning("worlds.enabled is true but worlds.list is empty. Allowing all worlds.");
            return true;
        }

        boolean inList = list.stream().anyMatch(w -> w.equalsIgnoreCase(worldName));

        if ("WHITELIST".equalsIgnoreCase(mode)) {
            return inList;
        }
        // BLACKLIST default
        return !inList;
    }
}