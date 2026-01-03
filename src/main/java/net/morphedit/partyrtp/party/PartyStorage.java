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
        if (!file.exists()) {
            plugin.getLogger().info("No parties.yml found, starting fresh.");
            return out;
        }

        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            var root = yml.getConfigurationSection("parties");
            if (root == null) {
                plugin.getLogger().info("Empty parties.yml, no parties to load.");
                return out;
            }

            for (String leaderKey : root.getKeys(false)) {
                UUID leader;
                try {
                    leader = UUID.fromString(leaderKey);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in parties.yml: " + leaderKey);
                    continue;
                }

                List<String> memberStrings = yml.getStringList("parties." + leaderKey + ".members");
                Set<UUID> members = new HashSet<>();
                for (String ms : memberStrings) {
                    try {
                        members.add(UUID.fromString(ms));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid member UUID for party " + leaderKey + ": " + ms);
                    }
                }

                out.put(leader, members);
            }

            plugin.getLogger().info("Loaded " + out.size() + " parties from disk.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load parties.yml: " + e.getMessage());
            e.printStackTrace();
        }

        return out;
    }

    public void saveParties(Map<UUID, Set<UUID>> parties) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
            return;
        }

        try {
            YamlConfiguration yml = new YamlConfiguration();

            for (Map.Entry<UUID, Set<UUID>> e : parties.entrySet()) {
                UUID leader = e.getKey();
                List<String> members = e.getValue().stream().map(UUID::toString).toList();
                yml.set("parties." + leader + ".members", members);
            }

            yml.save(file);
            plugin.getLogger().fine("Saved " + parties.size() + " parties to disk.");
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save parties.yml: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}