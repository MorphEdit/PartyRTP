package net.morphedit.partyrtp.rtp;

import net.morphedit.partyrtp.party.PartyService;
import net.morphedit.partyrtp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.morphedit.partyrtp.rtp.provider.RTPProvider;
import net.morphedit.partyrtp.rtp.provider.RTPProviderFactory;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class RTPService {

    private final JavaPlugin plugin;
    private final PartyService party;
    private final RTPProvider provider;

    public RTPService(JavaPlugin plugin, PartyService party) {
        this.plugin = plugin;
        this.party = party;
        this.provider = RTPProviderFactory.create(plugin);
    }

    public void handleGo(Player leader) {
        UUID lid = leader.getUniqueId();

        String worldName = leader.getWorld().getName();
        if (!net.morphedit.partyrtp.util.ConfigUtil.isWorldAllowed(plugin, worldName)) {
            Msg.sendPrefixed(plugin, leader, "&cPartyRTP is disabled in this world: &f" + worldName);
            return;
        }

        if (!party.isLeader(lid)) {
            Msg.send(leader, "&cYou must be a party leader.");
            return;
        }

        if (party.onCooldown(lid)) {
            Msg.send(leader, "&cCooldown: &f" + party.cooldownLeftSeconds(lid) + "s");
            return;
        }

        var tier = net.morphedit.partyrtp.util.ConfigUtil.resolveTier(plugin, leader);
        // near check (ของเดิม)
        boolean requireNear = plugin.getConfig().getBoolean("party.requireNear", true);
        int radius = tier.nearRadius();

        boolean membersMustBeOnline = plugin.getConfig().getBoolean("party.membersMustBeOnline", true);
        boolean requireSameWorld = plugin.getConfig().getBoolean("worlds.requireSameWorldAsLeader", true);

        for (UUID m : party.getMembers(lid)) {
            Player mp = Bukkit.getPlayer(m);

            if (mp == null) {
                if (membersMustBeOnline) {
                    Msg.sendPrefixed(plugin, leader, "&cAll members must be online.");
                    return;
                }
                continue; // allow offline => skip
            }

            if (requireSameWorld && !mp.getWorld().equals(leader.getWorld())) {
                Msg.sendPrefixed(plugin, leader, "&cAll members must be in same world.");
                return;
            }

            if (requireNear && mp.getLocation().distance(leader.getLocation()) > radius) {
                Msg.sendPrefixed(plugin, leader, "&cMember too far: &f" + mp.getName());
                return;
            }
        }

        int cd = tier.cooldownSeconds();
        int timeoutSec = plugin.getConfig().getInt("go.leaderTeleportTimeoutSeconds", 15);

        // start hardened GO
        UUID token = party.startGo(
                lid,
                leader.getLocation().clone(),
                timeoutSec * 1000L
        );

        party.setCooldown(lid, cd);

        Msg.sendPrefixed(plugin, leader,
                Msg.fmt(Msg.get(plugin, "messages.info.goIssued", "&aRTP leader... waiting for teleport. (&7provider: %provider%&a)"),
                        java.util.Map.of("%provider%", provider.getName()))
        );

        boolean issued = provider.requestRtp(leader);
        if (!issued) {
            party.clearGo(lid);
            Msg.sendPrefixed(plugin, leader, Msg.get(plugin, "messages.errors.rtpConfigBad", "&cRTP request failed. Check config.yml"));
            return;
        }

        // timeout fail message
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (party.isGoValid(lid, token)) {
                party.clearGo(lid);
                Msg.sendPrefixed(plugin, leader, Msg.get(plugin, "messages.errors.rtpTimeout", "&cRTP failed or timed out."));
            }
        }, timeoutSec * 20L);
    }

    public void pullMembersToLeader(Player leader) {
        UUID lid = leader.getUniqueId();
        Set<UUID> members = party.getMembers(lid);
        int delayTicks = plugin.getConfig().getInt("go.pullMemberDelayTicks", 2);

        int i = 0;
        for (UUID m : members) {
            Player mp = Bukkit.getPlayer(m);
            if (mp == null) continue;

            int wait = i * delayTicks;
            i++;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!leader.isOnline() || !mp.isOnline()) return;
                // ปลอดภัย: ย้ายโลกตามหัวหน้า
                mp.teleportAsync(leader.getLocation());
                Msg.send(mp, "&aParty RTP: teleported to leader.");
            }, wait);
        }

        Msg.send(leader, "&aMembers pulled to your location.");
    }
}
