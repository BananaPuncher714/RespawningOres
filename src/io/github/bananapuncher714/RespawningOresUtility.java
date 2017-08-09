package io.github.bananapuncher714;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RespawningOresUtility {
	RespawningOresMain plugin;
	
	public RespawningOresUtility( RespawningOresMain p ) {
		plugin = p;
	}
	
	public void loadLocations() {
		File locationFile = new File( plugin.getDataFolder(), "locations.yml" );
		if ( !locationFile.exists() ) return;
		FileConfiguration lfile = YamlConfiguration.loadConfiguration( locationFile );
		for ( String s : lfile.getKeys( false ) ) {
			Location l = toLocation( s );
			if ( !plugin.oreTypes.containsKey( lfile.get( s ) ) ) {
				plugin.locations.put( l, selectRandomMaterial() );
			} else {
				plugin.locations.put( l, plugin.oreTypes.get( lfile.get( s ) ) );
			}
			l.getBlock().setType( plugin.locations.get( l ).block );
		}
	}
	
	public void saveLocations() {
		File locationFile = new File( plugin.getDataFolder(), "locations.yml" );
		locationFile.delete();
		try {
			locationFile.createNewFile();
		} catch ( IOException e ) {
			plugin.getLogger().warning( "Was unable to create locations.yml in RespawningOres" );
		}
		FileConfiguration lfile = YamlConfiguration.loadConfiguration( locationFile );
		for ( Location l : plugin.locations.keySet() ) {
			RespawningOre ore = plugin.locations.get( l );
			if ( ore == null ) ore = selectRandomMaterial();
			lfile.set( toString( l ), ore.name );
		}
		try {
			lfile.save( locationFile );
		} catch ( IOException e ) {
			plugin.getLogger().warning( "Was unable to save locations.yml in RespawningOres" );
		}
	}
	
	public void loadOres() {
		plugin.saveResource( "ores.yml", false );
		File oreFile = new File( plugin.getDataFolder(), "ores.yml" );
		FileConfiguration ores = YamlConfiguration.loadConfiguration( oreFile );
		int weight = 0;
		for ( String name : ores.getKeys( false ) ) {
			Material b = Material.getMaterial( ores.getInt( name + ".material" ) );
			Material m = Material.getMaterial( ores.getInt( name + ".drop" ) );
			Short s = ( short ) ores.getInt( name + ".drop-data" );
			int a = ores.getInt( name + ".drop-amount" );
			int w = ores.getInt( name + ".spawn-chance-percent" );
			weight = weight + w;
			plugin.oreTypes.put( name, new RespawningOre( name, b, m, s, a, w ) );
		}
		plugin.totalWeight = weight;
	}
	
	// It would be better to use ChoiceFormat...
	// OR would it?...
	public RespawningOre selectRandomMaterial() {
		if ( plugin.totalWeight <= 0 ) {
			return null;
		}
		Random rand = new Random();
        int index = Math.abs( rand.nextInt( plugin.totalWeight + 1 ) );
        int sum = 0;
        int i = 0;
        ArrayList< RespawningOre > ores = new ArrayList< RespawningOre >( plugin.oreTypes.values() );
        while( sum < index ) {
             sum = sum + ores.get( i++ ).weight;
        }
        return ores.get( Math.max( 0, i-1 ) );
	}
	
	public void dropItem( Player p, ItemStack item ) {
		for ( ItemStack i : p.getInventory().addItem( item ).values() ) {
			p.getWorld().dropItem( p.getEyeLocation(), i );
		}
	}
	
	public Location toLocation( String s ) {
		String[] ll = s.replace( ',', '.' ).split( "_" );
		Location l = new Location( plugin.getServer().getWorld( ll[ 0 ] ), Double.parseDouble( ll[ 1 ] ), Double.parseDouble( ll[ 2 ] ), Double.parseDouble( ll[ 3 ] ), Float.parseFloat( ll[ 4 ] ), Float.parseFloat( ll[ 5 ] ) );
		return l;
	}
	
	private String toString( Location l ) {
		String newLoc = l.getWorld().getName() + "_" + String.valueOf( l.getX() ) + "_" + String.valueOf( l.getY() ) + "_" + String.valueOf( l.getZ() ) + "_" + String.valueOf( l.getYaw() ) + "_" + String.valueOf( l.getPitch() );
		return newLoc.replace( '.', ',' );
	}
}