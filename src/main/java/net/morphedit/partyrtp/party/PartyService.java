package net.morphedit.partyrtp.party;

import net.morphedit.partyrtp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PartyService {

    private final JavaPlugin plugin;

    // leader -> members (uuid)
    private final Map<UUID, Set<UUID>> parties = new HashMap<>();
    // invitee -> leader
    private final Map<UUID, UUID> invites = new HashMap<>();
    // leader cooldown unix millis
    private final Map<UUID, Long> goCooldownUntil = new HashMap<>();

    // "กำลังรอ teleport หัวหน้า" สำหรับ /go
    private final Set<UUID> pendingGoLeaders = new HashSet<>();
    private final Map<UUID, Long> pendingGoExpireAt = new HashMap<>();

    private final PartyStorage storage;
    private BukkitTask autosaveTask;

    private final Map<UUID, UUID> goToken = new HashMap<>(); // leader -> token
    private final Map<UUID, Long> goExpireAt = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> goFromLocation = new HashMap<>();

    public UUID getGoToken(UUID leader) {
        return goToken.get(leader);
    }

    public PartyService(JavaPlugin plugin) {
        this.plugin = plugin;

        this.storage = new PartyStorage(plugin);

        // Load parties from disk
        parties.clear();
        parties.putAll(storage.loadParties());

        // Auto-save every 2 minutes (ปรับได้)
        long autosaveTicks = 20L * 120; // 120s
        this.autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveParties, autosaveTicks, autosaveTicks);
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            Msg.sendPrefixed(plugin, sender, Msg.get(plugin, "messages.errors.playersOnly", "&cPlayers only."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            Msg.sendListPrefixed(plugin, p, plugin.getConfig().getStringList("messages.help"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> create(p);
            case "disband" -> disband(p);
            case "invite" -> {
                if (args.length < 2) {
                    Msg.send(p, "&cUsage: /prtp invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    Msg.send(p, "&cPlayer not found / offline.");
                    return true;
                }
                invite(p, target);
            }
            case "accept" -> {
                if (args.length < 2) {
                    Msg.send(p, "&cUsage: /prtp accept <leader>");
                    return true;
                }
                Player leader = Bukkit.getPlayerExact(args[1]);
                if (leader == null) {
                    Msg.send(p, "&cLeader not found / offline.");
                    return true;
                }
                accept(p, leader);
            }
            case "leave" -> leave(p);
            case "list" -> list(p);
            case "go" -> Msg.send(p, "&7Use &d/prtp go&7 again after we add RTP flow (next step)."); // RTPService จะ override flow ผ่าน listener
            default -> Msg.send(p, "&cUnknown subcommand. Use /prtp");
        }

        return true;
    }

    public void create(Player leader) {
        if (inAnyParty(leader.getUniqueId())) {
            Msg.send(leader, "&cYou are already in a party.");
            return;
        }
        parties.put(leader.getUniqueId(), new HashSet<>());
        Msg.sendPrefixed(plugin, leader,
                Msg.get(plugin, "messages.info.partyCreated",
                        "&aParty created. You are the leader."));
    }

    public void disband(Player leader) {
        UUID lid = leader.getUniqueId();
        if (!parties.containsKey(lid)) {
            Msg.send(leader, "&cYou are not a party leader.");
            return;
        }
        for (UUID m : parties.get(lid)) {
            Player mp = Bukkit.getPlayer(m);
            if (mp != null) Msg.send(mp, "&cParty was disbanded.");
        }
        parties.remove(lid);
        Msg.sendPrefixed(plugin, leader,
                Msg.get(plugin, "messages.info.partyDisbanded",
                        "&aParty disbanded."));
    }

    public void invite(Player leader, Player target) {
        UUID lid = leader.getUniqueId();
        UUID tid = target.getUniqueId();

        if (!parties.containsKey(lid)) {
            Msg.send(leader, "&cYou must /prtp create first.");
            return;
        }
        if (inAnyParty(tid)) {
            Msg.send(leader, "&cThat player is already in a party.");
            return;
        }

        int maxSize = net.morphedit.partyrtp.util.ConfigUtil.resolveTier(plugin, leader).maxSize();
        int size = 1 + parties.get(lid).size();
        if (size >= maxSize) {
            Msg.send(leader, "&cParty is full. Max: " + maxSize);
            return;
        }

        invites.put(tid, lid);
        Msg.sendPrefixed(plugin, leader,
                Msg.fmt(
                        Msg.get(plugin, "messages.info.invited", "&aInvited &f%player%"),
                        Map.of("%player%", target.getName())
                )
        );
        Msg.send(target, "&fYou were invited by &d" + leader.getName() + "&f. Type &a/prtp accept " + leader.getName());
    }

    public void accept(Player player, Player leader) {
        UUID tid = player.getUniqueId();
        UUID lid = leader.getUniqueId();

        if (inAnyParty(tid)) {
            Msg.send(player, "&cYou are already in a party.");
            return;
        }
        UUID invitedLeader = invites.get(tid);
        if (invitedLeader == null || !invitedLeader.equals(lid)) {
            Msg.send(player, "&cNo valid invite from that leader.");
            return;
        }
        if (!parties.containsKey(lid)) {
            Msg.send(player, "&cThat leader no longer has a party.");
            invites.remove(tid);
            return;
        }

        parties.get(lid).add(tid);
        invites.remove(tid);

        Msg.sendPrefixed(plugin, player,
                Msg.fmt(
                        Msg.get(plugin, "messages.info.joined",
                                "&aJoined party of &d%leader%"),
                        Map.of("%leader%", leader.getName())
                )
        );
        Msg.send(leader, "&a" + player.getName() + " joined your party.");
    }

    public void leave(Player player) {
        UUID pid = player.getUniqueId();

        // If leader
        if (parties.containsKey(pid)) {
            Msg.send(player, "&cYou are the leader. Use /prtp disband.");
            return;
        }

        UUID leader = getLeaderOf(pid);
        if (leader == null) {
            Msg.send(player, "&cYou are not in a party.");
            return;
        }

        parties.get(leader).remove(pid);

        Player lp = Bukkit.getPlayer(leader);
        if (lp != null) Msg.send(lp, "&c" + player.getName() + " left the party.");
        Msg.sendPrefixed(plugin, player,
                Msg.get(plugin, "messages.info.left",
                        "&aYou left the party."));
    }

    public void list(Player player) {
        UUID pid = player.getUniqueId();
        UUID leader = parties.containsKey(pid) ? pid : getLeaderOf(pid);

        if (leader == null) {
            Msg.send(player, "&cYou are not in a party.");
            return;
        }

        Player lp = Bukkit.getPlayer(leader);
        Msg.send(player, "&fLeader: &d" + (lp != null ? lp.getName() : leader));

        if (!parties.containsKey(leader)) return;

        Set<UUID> members = parties.get(leader);
        if (members.isEmpty()) {
            Msg.send(player, "&7(no members)");
            return;
        }
        for (UUID m : members) {
            Player mp = Bukkit.getPlayer(m);
            Msg.send(player, "&7- &f" + (mp != null ? mp.getName() : "(offline)"));
        }
    }

    public boolean inAnyParty(UUID player) {
        if (parties.containsKey(player)) return true; // leader
        return getLeaderOf(player) != null;
    }

    public UUID getLeaderOf(UUID member) {
        for (Map.Entry<UUID, Set<UUID>> e : parties.entrySet()) {
            if (e.getValue().contains(member)) return e.getKey();
        }
        return null;
    }

    public Set<UUID> getMembers(UUID leader) {
        return parties.getOrDefault(leader, Set.of());
    }

    // ----- GO Flow state -----
    public boolean isLeader(UUID leader) {
        return parties.containsKey(leader);
    }

    public boolean isPendingGo(UUID leader) {
        Long exp = pendingGoExpireAt.get(leader);
        if (exp == null) return false;
        if (System.currentTimeMillis() > exp) {
            pendingGoExpireAt.remove(leader);
            pendingGoLeaders.remove(leader);
            return false;
        }
        return pendingGoLeaders.contains(leader);
    }

    public void setPendingGo(UUID leader, long expireAtMillis) {
        pendingGoLeaders.add(leader);
        pendingGoExpireAt.put(leader, expireAtMillis);
    }

    public void clearPendingGo(UUID leader) {
        pendingGoLeaders.remove(leader);
        pendingGoExpireAt.remove(leader);
    }

    // ----- Cooldown -----
    public boolean onCooldown(UUID leader) {
        Long until = goCooldownUntil.get(leader);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            goCooldownUntil.remove(leader);
            return false;
        }
        return true;
    }

    public long cooldownLeftSeconds(UUID leader) {
        Long until = goCooldownUntil.get(leader);
        if (until == null) return 0;
        long left = (until - System.currentTimeMillis()) / 1000;
        return Math.max(left, 0);
    }

    public void setCooldown(UUID leader, long seconds) {
        goCooldownUntil.put(leader, System.currentTimeMillis() + (seconds * 1000));
    }

    public void saveParties() {
        storage.saveParties(parties);
    }

    public void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public UUID startGo(UUID leader, org.bukkit.Location from, long timeoutMillis) {
        UUID token = UUID.randomUUID();
        goToken.put(leader, token);
        goFromLocation.put(leader, from);
        goExpireAt.put(leader, System.currentTimeMillis() + timeoutMillis);
        return token;
    }

    public boolean isGoValid(UUID leader, UUID token) {
        UUID current = goToken.get(leader);
        Long exp = goExpireAt.get(leader);
        if (current == null || exp == null) return false;
        if (!current.equals(token)) return false;
        if (System.currentTimeMillis() > exp) {
            clearGo(leader);
            return false;
        }
        return true;
    }

    public org.bukkit.Location getGoFrom(UUID leader) {
        return goFromLocation.get(leader);
    }

    public void clearGo(UUID leader) {
        goToken.remove(leader);
        goExpireAt.remove(leader);
        goFromLocation.remove(leader);
    }
}
