package net.morphedit.partyrtp.admin;

import net.morphedit.partyrtp.party.PartyService;
import net.morphedit.partyrtp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AdminCommandHandler {

    private final JavaPlugin plugin;
    private final PartyService partyService;
    private boolean debugMode = false;

    public AdminCommandHandler(JavaPlugin plugin, PartyService partyService) {
        this.plugin = plugin;
        this.partyService = partyService;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "reload" -> handleReload(sender);
            case "forcedisband" -> handleForceDisband(sender, args);
            case "info" -> handleInfo(sender, args);
            case "listall" -> handleListAll(sender);
            case "debug" -> handleDebug(sender, args);
            default -> false;
        };
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("partyrtp.admin.reload")) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.errors.noPermission", "&cYou don't have permission."));
            return true;
        }

        try {
            plugin.reloadConfig();
            partyService.saveParties(); // Save before reload
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.admin.reloadSuccess", "&aConfiguration reloaded successfully!"));
            plugin.getLogger().info(sender.getName() + " reloaded the configuration.");
            return true;
        } catch (Exception e) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.admin.reloadFailed", "&cFailed to reload config: " + e.getMessage()));
            plugin.getLogger().severe("Config reload failed: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private boolean handleForceDisband(CommandSender sender, String[] args) {
        if (!sender.hasPermission("partyrtp.admin.forcedisband")) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.errors.noPermission", "&cYou don't have permission."));
            return true;
        }

        if (args.length < 2) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.admin.forceDisbandUsage", "&cUsage: /prtp forcedisband <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetUUID;

        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // Try to find offline player
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
                Msg.sendPrefixed(plugin, sender,
                        Msg.get(plugin, "messages.errors.playerNotFound", "&cPlayer not found."));
                return true;
            }
            targetUUID = offlinePlayer.getUniqueId();
        }

        boolean disbanded = partyService.forceDisband(targetUUID);

        if (disbanded) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.fmt(
                            Msg.get(plugin, "messages.admin.forceDisbandSuccess", "&aForce disbanded party of %player%"),
                            Map.of("%player%", args[1])
                    ));
            plugin.getLogger().info(sender.getName() + " force disbanded party of " + args[1]);

            // Notify the player if online
            if (target != null) {
                Msg.sendPrefixed(plugin, target,
                        Msg.get(plugin, "messages.admin.partyDisbandedByAdmin", "&cYour party was disbanded by an administrator."));
            }
        } else {
            Msg.sendPrefixed(plugin, sender,
                    Msg.fmt(
                            Msg.get(plugin, "messages.admin.notPartyLeader", "&c%player% is not a party leader."),
                            Map.of("%player%", args[1])
                    ));
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("partyrtp.admin.info")) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.errors.noPermission", "&cYou don't have permission."));
            return true;
        }

        if (args.length < 2) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.admin.infoUsage", "&cUsage: /prtp info <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.errors.playerNotFound", "&cPlayer not found / offline."));
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        String playerName = target.getName();

        // Check if player is a leader
        if (partyService.isLeader(targetUUID)) {
            Set<UUID> members = partyService.getMembers(targetUUID);
            Msg.sendPrefixed(plugin, sender, "&7=== Party Info: &f" + playerName + " &7===");
            Msg.send(sender, "&fRole: &aLeader");
            Msg.send(sender, "&fMembers: &a" + members.size());

            if (!members.isEmpty()) {
                Msg.send(sender, "&fMember list:");
                for (UUID m : members) {
                    Player mp = Bukkit.getPlayer(m);
                    String status = mp != null ? "&a(online)" : "&7(offline)";
                    String name = mp != null ? mp.getName() : m.toString();
                    Msg.send(sender, "  &7- &f" + name + " " + status);
                }
            }

            // Cooldown info
            if (partyService.onCooldown(targetUUID)) {
                Msg.send(sender, "&fCooldown: &c" + partyService.cooldownLeftSeconds(targetUUID) + "s remaining");
            } else {
                Msg.send(sender, "&fCooldown: &aReady");
            }
        } else {
            // Check if member
            UUID leaderUUID = partyService.getLeaderOf(targetUUID);
            if (leaderUUID != null) {
                Player leader = Bukkit.getPlayer(leaderUUID);
                String leaderName = leader != null ? leader.getName() : leaderUUID.toString();
                Msg.sendPrefixed(plugin, sender, "&7=== Party Info: &f" + playerName + " &7===");
                Msg.send(sender, "&fRole: &eMember");
                Msg.send(sender, "&fLeader: &d" + leaderName);
            } else {
                Msg.sendPrefixed(plugin, sender, "&f" + playerName + " &7is not in any party.");
            }
        }

        return true;
    }

    private boolean handleListAll(CommandSender sender) {
        if (!sender.hasPermission("partyrtp.admin.listall")) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.errors.noPermission", "&cYou don't have permission."));
            return true;
        }

        Map<UUID, Set<UUID>> allParties = partyService.getAllParties();

        if (allParties.isEmpty()) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.admin.noParties", "&7No active parties."));
            return true;
        }

        Msg.sendPrefixed(plugin, sender, "&7=== All Parties (&f" + allParties.size() + "&7) ===");

        int index = 1;
        for (Map.Entry<UUID, Set<UUID>> entry : allParties.entrySet()) {
            Player leader = Bukkit.getPlayer(entry.getKey());
            String leaderName = leader != null ? leader.getName() : entry.getKey().toString();
            int memberCount = entry.getValue().size();

            Msg.send(sender, "&f" + index + ". &dLeader: &f" + leaderName + " &7(" + memberCount + " members)");
            index++;
        }

        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("partyrtp.admin.debug")) {
            Msg.sendPrefixed(plugin, sender,
                    Msg.get(plugin, "messages.errors.noPermission", "&cYou don't have permission."));
            return true;
        }

        if (args.length < 2) {
            String status = debugMode ? "&aON" : "&cOFF";
            Msg.sendPrefixed(plugin, sender, "&fDebug mode is currently: " + status);
            Msg.send(sender, "&7Usage: /prtp debug <on|off>");
            return true;
        }

        String mode = args[1].toLowerCase();
        switch (mode) {
            case "on", "true", "enable" -> {
                debugMode = true;
                Msg.sendPrefixed(plugin, sender,
                        Msg.get(plugin, "messages.admin.debugEnabled", "&aDebug mode enabled."));
                plugin.getLogger().info(sender.getName() + " enabled debug mode.");
            }
            case "off", "false", "disable" -> {
                debugMode = false;
                Msg.sendPrefixed(plugin, sender,
                        Msg.get(plugin, "messages.admin.debugDisabled", "&cDebug mode disabled."));
                plugin.getLogger().info(sender.getName() + " disabled debug mode.");
            }
            default -> {
                Msg.sendPrefixed(plugin, sender, "&cInvalid option. Use: on/off");
                return true;
            }
        }

        return true;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void debug(String message) {
        if (debugMode) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}