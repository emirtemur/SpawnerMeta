package mc.rellox.spawnermeta.holograms;

import mc.rellox.spawnermeta.SpawnerMeta;
import mc.rellox.spawnermeta.version.IVersion;
import mc.rellox.spawnermeta.version.Version;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;

public record HologramModifier(IVersion version) {

	public HologramModifier() {
		this(Version.v);
	}
	
	public Object create(Location location, String name) {
		return version.hologram(location, name);
	}
	
	public void spawn(Player player, Object entity) {
		Object spawn = version.spawn(entity), meta = version.meta(entity);
		SpawnerMeta.scheduler().runAtEntity(player, task -> version.send(player, spawn, meta));
	}

	public void destroy(Player player, Object entity) {
		Object destroy = version.destroy(entity);
		SpawnerMeta.scheduler().runAtEntity(player, task -> version.send(player, destroy));
	}

	public void update(Set<Player> players, Object entity, String name) {
		version.name(entity, name);
		Object meta = version.meta(entity);
		for (Player player : players)
			SpawnerMeta.scheduler().runAtEntity(player, task -> version.send(player, meta));
	}

}
