package net.morphedit.partyrtp.util;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class Msg {
    private Msg() {}

    public static void send(CommandSender to, String text) {
        if (to == null || text == null) return;
        to.sendMessage(color(text));
    }

    public static void sendPrefixed(JavaPlugin plugin, CommandSender to, String text) {
        if (to == null || text == null) return;
        String prefix = plugin.getConfig().getString("messages.prefix", "&d[PartyRTP]&f ");
        send(to, prefix + text);
    }

    public static void sendListPrefixed(JavaPlugin plugin, CommandSender to, List<String> lines) {
        if (to == null || lines == null) return;
        String prefix = plugin.getConfig().getString("messages.prefix", "&d[PartyRTP]&f ");
        for (String line : lines) {
            send(to, prefix + line);
        }
    }

    public static String get(JavaPlugin plugin, String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    public static String fmt(String text, Map<String, String> vars) {
        if (text == null) return "";
        String out = text;
        if (vars != null) {
            for (var e : vars.entrySet()) {
                out = out.replace(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    public static String color(String text) {
        return text.replace('&', '§');
    }
}
