package net.morphedit.partyrtp.rtp.provider;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

public final class PlaceholderUtil {
    private PlaceholderUtil() {}

    /**
     * Apply placeholders to a template string
     * Supports: %player%, %uuid%, %world%, %x%, %y%, %z%
     */
    public static String apply(String template, Player p) {
        if (template == null || template.isBlank()) return "";
        if (p == null) return template;

        Location loc = p.getLocation();
        String worldName = "world";
        if (loc.getWorld() != null) {
            worldName = loc.getWorld().getName();
        }

        Map<String, String> vars = Map.of(
                "%player%", p.getName(),
                "%uuid%", p.getUniqueId().toString(),
                "%world%", worldName,
                "%x%", String.valueOf(loc.getBlockX()),
                "%y%", String.valueOf(loc.getBlockY()),
                "%z%", String.valueOf(loc.getBlockZ())
        );

        String out = template;
        for (var e : vars.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }
}