package net.morphedit.partyrtp;

import net.morphedit.partyrtp.party.PartyService;
import net.morphedit.partyrtp.rtp.RTPService;
import net.morphedit.partyrtp.teleport.LeaderTeleportListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class PartyRTPPlugin extends JavaPlugin {

    private PartyService partyService;
    private RTPService rtpService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.partyService = new PartyService(this);
        this.rtpService = new RTPService(this, partyService);

        getServer().getPluginManager().registerEvents(
                new LeaderTeleportListener(this, partyService, rtpService),
                this
        );

        getLogger().info("PartyRTP enabled.");
    }

    @Override
    public void onDisable() {
        if (partyService != null) {
            partyService.saveParties();
            partyService.stopAutosave();
        }
        getLogger().info("PartyRTP disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("prtp")) return false;

        if (sender instanceof org.bukkit.entity.Player p) {
            if (args.length > 0 && args[0].equalsIgnoreCase("go")) {
                rtpService.handleGo(p);
                return true;
            }
        }

        return partyService.handleCommand(sender, args);
    }
}
