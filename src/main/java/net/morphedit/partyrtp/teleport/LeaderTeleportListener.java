package net.morphedit.partyrtp.teleport;

import net.morphedit.partyrtp.party.PartyService;
import net.morphedit.partyrtp.rtp.RTPService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void onLeaderTeleport(PlayerTeleportEvent e) {
        Player leader = e.getPlayer();
        UUID lid = leader.getUniqueId();

        UUID token = party.getGoToken(lid);
        if (token == null) return;

        if (!party.isGoValid(lid, token)) return;

        var from = party.getGoFrom(lid);
        if (from == null) return;

        double minDistance = plugin.getConfig().getDouble("go.successMinDistance", 64.0);

        boolean success =
                !from.getWorld().equals(e.getTo().getWorld()) ||
                        from.distance(e.getTo()) >= minDistance;

        if (!success) return;

        // SUCCESS
        party.clearGo(lid);

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> rtp.pullMembersToLeader(leader),
                5L
        );
    }
}
