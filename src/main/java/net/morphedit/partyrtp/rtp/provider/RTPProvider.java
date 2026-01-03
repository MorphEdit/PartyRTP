package net.morphedit.partyrtp.rtp.provider;

import org.bukkit.entity.Player;

public interface RTPProvider {
    /**
     * Request RTP teleport for leader. Should return true if command/request was issued.
     * Success is still determined later by our teleport listener.
     */
    boolean requestRtp(Player leader);

    String getName();
}
