package net.morphedit.partyrtp;

import net.morphedit.partyrtp.admin.AdminCommandHandler;
import net.morphedit.partyrtp.party.PartyService;
import net.morphedit.partyrtp.rtp.RTPService;
import net.morphedit.partyrtp.teleport.LeaderTeleportListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PartyRTPPlugin extends JavaPlugin implements Listener {

    private PartyService partyService;
    private RTPService rtpService;
    private AdminCommandHandler adminHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.partyService = new PartyService(this);
        this.rtpService = new RTPService(this, partyService);
        this.adminHandler = new AdminCommandHandler(this, partyService);

        getServer().getPluginManager().registerEvents(
                new LeaderTeleportListener(this, partyService, rtpService),
                this
        );

        // Register self for PlayerQuitEvent
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("PartyRTP v0.9.0 enabled.");
    }

    /**
     * Reload all components after config reload
     */
    public void reloadAll() {
        if (rtpService != null) {
            rtpService.reloadProvider();
        }
        getLogger().info("All components reloaded.");
    }

    @Override
    public void onDisable() {
        if (partyService != null) {
            partyService.saveParties();
            partyService.stopAutosave();
        }
        getLogger().info("PartyRTP disabled.");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (partyService != null) {
            java.util.UUID uuid = event.getPlayer().getUniqueId();
            partyService.clearGo(uuid);
            partyService.clearPendingGo(uuid);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("prtp")) return false;

        // Admin commands (work for console and players with permission)
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reload") || sub.equals("forcedisband") ||
                    sub.equals("info") || sub.equals("listall") || sub.equals("debug")) {
                return adminHandler.handleCommand(sender, args);
            }
        }

        // Player commands
        if (sender instanceof org.bukkit.entity.Player p) {
            if (args.length > 0 && args[0].equalsIgnoreCase("go")) {
                rtpService.handleGo(p);
                return true;
            }
        }

        return partyService.handleCommand(sender, args);
    }

    public AdminCommandHandler getAdminHandler() {
        return adminHandler;
    }
}