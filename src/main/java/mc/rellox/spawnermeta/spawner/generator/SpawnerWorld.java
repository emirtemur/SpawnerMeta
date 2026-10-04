package mc.rellox.spawnermeta.spawner.generator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.github.rvskele.paperlib.PaperLib;
import mc.rellox.spawnermeta.SpawnerMeta;
import mc.rellox.spawnermeta.utility.Utility;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;

import mc.rellox.spawnermeta.api.spawner.IGenerator;
import mc.rellox.spawnermeta.api.spawner.ISpawner;
import mc.rellox.spawnermeta.api.spawner.location.Pos;
import mc.rellox.spawnermeta.configuration.Settings;
import mc.rellox.spawnermeta.spawner.ActiveGenerator;

public class SpawnerWorld {

	public final World world;
	protected final Map<Pos, IGenerator> spawners;

	// Chunk-based spawners lookups
	private final Map<Long, Set<Pos>> byChunk;

	public SpawnerWorld(World world) {
		this.world = world;
		this.spawners = Collections.synchronizedMap(new HashMap<>());
        this.byChunk = new ConcurrentHashMap<>();
	}

	public Stream<IGenerator> stream() {
		return spawners.values().stream();
	}

	public void load() {
		for (Chunk chunk : world.getLoadedChunks()) {
			if (SpawnerMeta.foliaLib().isFolia()) {
				Location location = new Location(world, chunk.getX() << 4, 0, chunk.getZ() << 4);
				SpawnerMeta.scheduler().runAtLocation(location, task -> {
					if (chunk.isLoaded()) load(chunk);
				});
			} else load(chunk);
		}
	}

	public void load(Chunk chunk) {
		BlockState[] tileEntities = PaperLib.getTileEntities(chunk, false).getTileEntities();
		for (BlockState state : tileEntities) {
			if (state instanceof CreatureSpawner) {
				Block block = state.getBlock();
				if (!Settings.settings.ignored(block)) {
					ISpawner spawner = ISpawner.of(block);
					Pos pos = Pos.of(block);
					if (!spawners.containsKey(pos)) put(new ActiveGenerator(spawner));
				}
			}
		}
	}

	public void unload(Chunk chunk) {
		long chunkKey = Utility.getChunkKey(chunk);

		Set<Pos> set = byChunk.get(chunkKey);
		if (set == null) return;

		for (Pos pos : set) {
			IGenerator generator = spawners.get(pos);
			if (generator != null) {
				generator.remove(false);
			}
		}
	}

	public void clear() {
		for (IGenerator generator : snapshot()) onGenerator(generator, generator::clear);
		spawners.clear();
		byChunk.clear();
	}

	public int active() {
		return spawners.size();
	}

	public void update() {
		for (IGenerator generator : snapshot()) onGenerator(generator, generator::update);
	}

	public void control() {
		for (IGenerator generator : snapshot()) onGenerator(generator, generator::control);
	}

	public void tick() {
		if (SpawnerMeta.foliaLib().isFolia()) {
			for (IGenerator generator : snapshot()) {
				generator.tickFolia();
			}
		} else {
			for (IGenerator generator : snapshot()) {
				generator.tick();
			}
		}
	}

	public void reduce() {
		for (Map.Entry<Pos, IGenerator> entry : snapshotEntries().entrySet()) {
			Pos pos = entry.getKey();
			IGenerator generator = entry.getValue();
			onGenerator(generator, () -> {
				if (generator.active() && generator.present()) return;
				if (!spawners.remove(pos, generator)) return;
				generator.clear();
				removeFromChunk(pos);
			});
		}
	}

	private Map<Pos, IGenerator> snapshotEntries() {
		synchronized (spawners) {
			return new HashMap<>(spawners);
		}
	}

	private List<IGenerator> snapshot() {
		return new ArrayList<>(snapshotEntries().values());
	}

	private void onGenerator(IGenerator generator, Runnable action) {
		if (!SpawnerMeta.foliaLib().isFolia()
				|| SpawnerMeta.scheduler().isOwnedByCurrentRegion(generator.block())) {
			action.run();
		} else SpawnerMeta.scheduler().runAtLocation(generator.block().getLocation(), task -> action.run());
	}

	public int remove(boolean fully, Predicate<IGenerator> filter) {
		List<Pos> toRemove = new ArrayList<>();

		Map<Pos, IGenerator> spawnersCopy;
		synchronized (spawners) {
			spawnersCopy = new HashMap<>(spawners);
		}

		for (Map.Entry<Pos, IGenerator> entry : spawnersCopy.entrySet()) {
			Pos pos = entry.getKey();
			IGenerator generator = entry.getValue();
			if (generator.active() && filter.test(generator)) {
				generator.remove(fully);
				toRemove.add(pos);
				removeFromChunk(pos);
			}
		}

		synchronized (spawners) {
			for (Pos pos : toRemove) {
				spawners.remove(pos);
			}
		}

		return toRemove.size();
	}

	public void put(Block block) {
		put(new ActiveGenerator(ISpawner.of(block)));
	}

	private void put(IGenerator generator) {
		Pos pos = generator.position();

		IGenerator last = spawners.put(pos, generator);
		byChunk.computeIfAbsent(Utility.getChunkKey(pos), k -> ConcurrentHashMap.newKeySet())
				.add(pos);
		if (last != null) {
			last.clear();
		}
	}

	public IGenerator get(Block block) {
		IGenerator generator = spawners.get(Pos.of(block));
		if (generator == null) {
			if (block.getType() == Material.SPAWNER) {
				put(block);
				generator = spawners.get(Pos.of(block));
			}
		} else if (!generator.active()) return null;
		return generator;
	}

	public IGenerator raw(Block block) {
		return spawners.get(Pos.of(block));
	}

	private void removeFromChunk(Pos pos) {
		long key = Utility.getChunkKey(pos);

		Set<Pos> set = byChunk.get(key);
		if (set == null) return;

		set.remove(pos);

		if (set.isEmpty()) {
			byChunk.remove(key, set);
		}
	}

}
