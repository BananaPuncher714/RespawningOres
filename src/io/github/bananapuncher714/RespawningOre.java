package io.github.bananapuncher714;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class RespawningOre {
	Material block;
	ItemStack item;
	int weight;
	String name;
	
	public RespawningOre( String n, Material b, Material m, Short d, Integer a, Integer w ) {
		name = n;
		block = b;
		item = new ItemStack( m );
		item.setDurability( d );
		item.setAmount( a );
		weight = w;
	}
}
