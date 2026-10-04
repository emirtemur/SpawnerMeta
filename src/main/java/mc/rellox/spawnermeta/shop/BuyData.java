package mc.rellox.spawnermeta.shop;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mc.rellox.spawnermeta.configuration.Language;
import mc.rellox.spawnermeta.items.ItemMatcher;
import mc.rellox.spawnermeta.prices.Group;
import mc.rellox.spawnermeta.prices.Price;
import mc.rellox.spawnermeta.spawner.type.SpawnerType;
import mc.rellox.spawnermeta.utility.DataManager;
import mc.rellox.spawnermeta.utility.Utility;

public record BuyData(SpawnerType type, int value) {
	
	public void buy(Player player, int a) {
		a = Math.min(a, 36 * 64);
		long total = (long) value * a;
		if(a <= 0 || total > Integer.MAX_VALUE) return;
		List<ItemStack> items = DataManager.getSpawners(type, a, false, true);
		if(items.isEmpty()) return;
		Price price = Price.of(Group.shop, (int) total);
		if(price.has(player) == false) {
			player.sendMessage(Language.get("Prices.insufficient",
					"insufficient", price.insufficient(), "price", price.requires(player)).text());
			player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 2f, 1f);
			return;
		}
		price.remove(player);
		items.forEach(item -> ItemMatcher.add(player, item));
		player.sendMessage(Language.get("Shop-buy.purchase.success",
				"amount", a, "type", type).text());
		player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 2f, 2f);
	}
	
	public ItemStack info(boolean[] bs) {
		ItemStack item = new ItemStack(Material.SPAWNER);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Language.get("Shop-buy.items.spawner.name",
				"type", type).text());
		List<String> lore = new ArrayList<>();
		lore.add("");
		lore.add(Language.get("Shop-buy.items.spawner.price",
				"price", Price.of(Group.shop, value)).text());
		lore.add("");
		if(bs[0] == true) lore.add(Language.get("Shop-buy.items.spawner.purchase.first",
				"amount", ShopRegistry.first).text());
		if(bs[1] == true) lore.add(Language.get("Shop-buy.items.spawner.purchase.second",
				"amount", ShopRegistry.second).text());
		if(bs[2] == true) lore.add(Language.get("Shop-buy.items.spawner.purchase.third",
				"amount", ShopRegistry.third).text());
		if(bs[3] == true) lore.add(Language.get("Shop-buy.items.spawner.purchase.all").text());
		meta.setLore(lore);
		meta.addItemFlags(Utility.ITEM_FLAGS);
		item.setItemMeta(meta);
		return item;
	}

}
