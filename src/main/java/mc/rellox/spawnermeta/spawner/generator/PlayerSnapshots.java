package mc.rellox.spawnermeta.spawner.generator;

import mc.rellox.spawnermeta.SpawnerMeta;
import mc.rellox.spawnermeta.api.region.IBox;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Immutable player positions sampled on each player's owning thread. */
public final class PlayerSnapshots implements Listener {

    private static final Map<UUID, Snapshot> PLAYERS = new ConcurrentHashMap<>();

    public static void initialize() {
        Bukkit.getPluginManager().registerEvents(new PlayerSnapshots(), SpawnerMeta.instance());
        SpawnerMeta.scheduler().runTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                SpawnerMeta.scheduler().runAtEntity(player, task -> {
                    UUID id = player.getUniqueId();
                    if (!player.isOnline()) {
                        PLAYERS.remove(id);
                        return;
                    }
                    Location location = player.getLocation();
                    PLAYERS.put(id, new Snapshot(player, location.getWorld().getUID(),
                            location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                            player.getGameMode() == GameMode.SPECTATOR));
                });
            }
        }, 1, 5);
    }

    public static boolean any(World world, IBox box) {
        UUID worldId = world.getUID();
        for (Snapshot snapshot : PLAYERS.values()) {
            if (snapshot.worldId.equals(worldId) && !snapshot.spectator
                    && box.in(snapshot.x, snapshot.y, snapshot.z)) return true;
        }
        return false;
    }

    public static List<Player> in(World world, IBox box) {
        UUID worldId = world.getUID();
        List<Player> result = new ArrayList<>();
        for (Snapshot snapshot : PLAYERS.values()) {
            if (snapshot.worldId.equals(worldId)
                    && box.in(snapshot.x, snapshot.y, snapshot.z)) result.add(snapshot.player);
        }
        return result;
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        PLAYERS.remove(event.getPlayer().getUniqueId());
    }

    private record Snapshot(Player player, UUID worldId, int x, int y, int z, boolean spectator) {}
}
