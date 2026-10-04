package mc.rellox.spawnermeta.holograms;

import mc.rellox.spawnermeta.api.hologram.IHologram;
import mc.rellox.spawnermeta.api.region.IBox;
import mc.rellox.spawnermeta.api.spawner.IGenerator;
import mc.rellox.spawnermeta.configuration.Settings;
import mc.rellox.spawnermeta.spawner.generator.PlayerSnapshots;
import mc.rellox.spawnermeta.text.content.Content;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class HologramImpl implements IHologram {
	
	protected final IGenerator generator;
	private final Object hologram;
	private final IBox box;
	private final Set<Player> players;
	
	public HologramImpl(IGenerator generator, boolean above, int radius) {
		this.generator = generator;
		String title = title().text();
		Block block = generator.block();
		this.hologram = modifier
				.create(block.getLocation()
						.add(0.5, 1 + (above ? 0.25 : 0)
								+ Settings.settings.holograms_height, 0.5), title);
		this.box = IBox.cube(block, radius);
		this.players = new HashSet<>();
	}
	
	@Override
	public IGenerator generator() {
		return generator;
	}
	
	@Override
	public Set<Player> viewers() {
		return players;
	}
	
	@Override
	public void update() {
		List<Player> list = PlayerSnapshots.in(generator.world(), box);
		for(Player player : list) {
			if(players.add(player)) show(player);
		}
		for(Player player : List.copyOf(players)) {
			if(!list.contains(player) && players.remove(player)) hide(player);
		}
	}
	
	@Override
	public void rewrite() {
		String title = title().text();
		modifier.update(players, hologram, title);
	}

	@Override
	public void show(Player player) {
		modifier.spawn(player, hologram);
	}
	
	@Override
	public void hide(Player player) {
		modifier.destroy(player, hologram);
	}

	@Override
	public void clear() {
		players.forEach(player -> modifier.destroy(player, hologram));
	}
	
	public abstract Content title();

}
