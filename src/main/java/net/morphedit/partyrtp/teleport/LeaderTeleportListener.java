package net.morphedit.partyrtp.teleport;

import net.morphedit.partyrtp.party.PartyService;
import net.morphedit.partyrtp.rtp.RTPService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class LeaderTeleportListener implements Listener {

    private final JavaPlugin plugin;
    private final PartyService party;
    private final RTPService rtp;

    public LeaderTeleportListener(JavaPlugin plugin, PartyService party, RTPService rtp) {
        this.plugin = plugin;
        this.party = party;
        this.rtp = rtp;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeaderTeleport(PlayerTeleportEvent e) {
        Player leader = e.getPlayer();
        UUID lid = leader.getUniqueId();

        UUID token = party.getGoToken(lid);
        if (token == null) return;

        if (!party.isGoValid(lid, token)) return;

        Location from = party.getGoFrom(lid);
        Location to = e.getTo();

        if (from == null || to == null) return;

        double minDistance = plugin.getConfig().getDouble("go.successMinDistance", 64.0);

        // Success criteria: different world OR distance >= minDistance
        boolean success =
                !from.getWorld().equals(to.getWorld()) ||
                        from.distanceSquared(to) >= minDistance * minDistance;

        if (!success) return;

        // SUCCESS - Clear state and pull members
        party.clearGo(lid);

        // Store leader's landing location for validation
        final Location landingLocation = to.clone();

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    // Verify leader is still online and hasn't moved too far
                    if (!leader.isOnline()) return;

                    Location current = leader.getLocation();
                    if (current.getWorld().equals(landingLocation.getWorld()) &&
                            current.distanceSquared(landingLocation) < 100) { // Within 10 blocks
                        rtp.pullMembersToLeader(leader);
                    } else {
                        plugin.getLogger().warning("Leader " + leader.getName() +
                                " moved too far from landing location, skipping member pull");
                    }
                },
                5L
        );
    }
}