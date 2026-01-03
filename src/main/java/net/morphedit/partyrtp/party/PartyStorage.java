package net.morphedit.partyrtp.party;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PartyStorage {

    private final JavaPlugin plugin;
    private final File file;

    public PartyStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "parties.yml");
    }

    public Map<UUID, Set<UUID>> loadParties() {
        Map<UUID, Set<UUID>> out = new HashMap<>();
        if (!file.exists()) return out;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        var root = yml.getConfigurationSection("parties");
        if (root == null) return out;

        for (String leaderKey : root.getKeys(false)) {
            UUID leader;
            try {
                leader = UUID.fromString(leaderKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            List<String> memberStrings = yml.getStringList("parties." + leaderKey + ".members");
            Set<UUID> members = new HashSet<>();
            for (String ms : memberStrings) {
                try {
                    members.add(UUID.fromString(ms));
                } catch (IllegalArgumentException ignored) {}
            }

            out.put(leader, members);
        }

        return out;
    }

    public void saveParties(Map<UUID, Set<UUID>> parties) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
            return;
        }

        YamlConfiguration yml = new YamlConfiguration();

        for (Map.Entry<UUID, Set<UUID>> e : parties.entrySet()) {
            UUID leader = e.getKey();
            List<String> members = e.getValue().stream().map(UUID::toString).toList();
            yml.set("parties." + leader + ".members", members);
        }

        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save parties.yml: " + ex.getMessage());
        }
    }
}
