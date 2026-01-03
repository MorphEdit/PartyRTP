package net.morphedit.partyrtp.rtp.provider;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

public final class PlaceholderUtil {
    private PlaceholderUtil() {}

    public static String apply(String template, Player p) {
        if (template == null) return "";

        Location loc = p.getLocation();
        Map<String, String> vars = Map.of(
                "%player%", p.getName(),
                "%uuid%", p.getUniqueId().toString(),
                "%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world",
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
