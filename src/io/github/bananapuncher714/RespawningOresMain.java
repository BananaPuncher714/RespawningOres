package io.github.bananapuncher714;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public class RespawningOresMain extends JavaPlugin implements Listener {
	boolean WE = Bukkit.getPluginManager().getPlugin( "WorldEdit" ) != null;
	private RespawningOresUtility util = new RespawningOresUtility( this );
	HashMap< Location, Integer > ores = new HashMap< Location, Integer >();
	HashMap< Location, RespawningOre > locations = new HashMap< Location, RespawningOre >();
	HashMap< String, RespawningOre > oreTypes = new HashMap< String, RespawningOre >();
	int totalWeight, maxdelay, mindelay;
	Material brokenBlock, detectBlock, testMaterial;
	
	String stillWaiting = "You need to wait!";
	
	@Override
	public void onEnable() {
		if ( WE ) {
			getLogger().info( ChatColor.GREEN + "WorldEdit has been detected!" );
		} else {
			getLogger().info( ChatColor.RED + "WorldEdit has not been detected!" );
		}
		saveDefaultConfig();
		loadConfig();
		util.loadOres();
		util.loadLocations();
		if ( totalWeight <= 0 ) {
			System.out.println( "[RespawningOres] No ores have been registered!" );
			System.out.println( "[RespawningOres] Disabling plugin" );
			Bukkit.getPluginManager().disablePlugin( this );
		}
		Bukkit.getPluginManager().registerEvents( this, this );
		Bukkit.getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {
			@Override
			public void run() {
			    for( Iterator< HashMap.Entry< Location, Integer > > it = ores.entrySet().iterator(); it.hasNext(); ) {
			        HashMap.Entry< Location, Integer > entry = it.next();
			        int seconds = entry.getValue();
			        seconds--;
			        if( seconds <= 0 ) {
			          it.remove();
			          RespawningOre ore = util.selectRandomMaterial();
			          locations.put( entry.getKey(), ore );
			          entry.getKey().getBlock().setType( ore.block );
			        } else {
			        	ores.put( entry.getKey(), seconds );
					}
			    }
			}
		}, 0, 1 );
	}
	
	@Override
	public void onDisable() {
		util.saveLocations();
	}
	
	@Override
	public boolean onCommand( CommandSender s, Command c, String l, String[] a ) {
		if ( !( s instanceof Player ) ) return false;
		Player p = ( Player ) s;
		if ( c.getName().equalsIgnoreCase( "respawningores" ) ) {
			if ( p.hasPermission( "respawningores.admin" ) ) {
				if ( !WE ) {
					p.sendMessage( ChatColor.RED + "WorldEdit is not enabled! This command cannot be used!" );
					return false;
				}
				if ( a.length == 1 ) {
					if ( a[ 0 ].equalsIgnoreCase( "count" ) ) {
						changeBlocks( p, 0 );
						return true;
					} else if ( a[ 0 ].equalsIgnoreCase( "add" ) ) {
						changeBlocks( p, 1 );
						return true;
					} else if ( a[ 0 ].equalsIgnoreCase( "remove" ) ) {
						changeBlocks( p, 2 );
						return true;
					} else if ( a[ 0 ].equalsIgnoreCase( "regen" ) ) {
						changeBlocks( p, 3 );
						return true;
					} else {
						p.sendMessage( ChatColor.RED + "Invalid argument! /respawningores <count|add|remove|regen>" );
					}
				} else {
					p.sendMessage( ChatColor.RED + "You need to provide an argument! /respawningores <count|add|remove|regen" );
				}
			} else {
				p.sendMessage( ChatColor.RED + "You do not have permission to run this command!" );
			}
		}
		return false;
	}
	
	@EventHandler
	public void onBlockBreakEvent( BlockBreakEvent e ) {
		Location l = e.getBlock().getLocation();
		Player p = e.getPlayer();
		if ( locations.containsKey( l ) ) {
			e.setCancelled( true );
			if ( !ores.containsKey( l ) ) {
				RespawningOre ore = locations.get( l );
				util.dropItem( p, ore.item );
				e.getBlock().setType( brokenBlock );
				locations.put( l, null );
				ores.put( l, getDelay() * 20 );
			} else {
				if ( !stillWaiting.equalsIgnoreCase( "" ) ) p.sendMessage( stillWaiting.replaceAll( "%t", String.valueOf( ( int ) ores.get( l ) / 20 ) ) );
			}
		}
		return;
	}
	
	@EventHandler
	public void onPlayerInteractEvent( PlayerInteractEvent e ) {
		Action a = e.getAction();
		Player p = e.getPlayer();
		Block b = e.getClickedBlock();
		if ( b == null ) return;
		if ( !p.hasPermission( "respawningores.admin" ) ) return;
		if ( p.getInventory().getItemInMainHand().getType() != testMaterial ) return; 
		if ( a.equals( Action.RIGHT_CLICK_BLOCK ) ) {
			if ( !e.getHand().equals( EquipmentSlot.HAND ) ) return;
			e.setCancelled( true );
			Location bl = b.getLocation();
			if ( !locations.containsKey( bl ) ) {
				RespawningOre ore = util.selectRandomMaterial();
				locations.put( bl, ore );
				b.setType( ore.block );
				p.sendMessage( ChatColor.GREEN + "You have added a respawning ore!" );
			} else {
				bl.getBlock().setType( brokenBlock );
				locations.remove( bl );
				ores.remove( bl );
				p.sendMessage( ChatColor.RED + "You have removed a respawning ore!" );
			}
		}
	}
	
	public void loadConfig() {
		FileConfiguration config = getConfig();
		loadMessages( config );
		brokenBlock = Material.getMaterial( config.getInt( "broken-block" ) );
		detectBlock = Material.getMaterial( config.getInt( "detect-block" ) );
		testMaterial = Material.getMaterial( config.getInt( "edit-item" ) );
		maxdelay = config.getInt( "max-delay" );
		mindelay = config.getInt( "min-delay" );
	}
	
	public void loadMessages( FileConfiguration c ) {
		stillWaiting = c.getString( "wait-message" ).replaceAll( "&", "§" );
	}
	
	public int getDelay() {
		Random r = new Random();
		int delayrange = maxdelay - mindelay;
		int randomdelay = r.nextInt( delayrange + 1 );
		return mindelay + randomdelay;
	}
	
	public void changeBlocks( Player p, int mode ) {
		if ( !WE ) return;
		com.sk89q.worldedit.bukkit.WorldEditPlugin we = ( com.sk89q.worldedit.bukkit.WorldEditPlugin ) Bukkit.getPluginManager().getPlugin( "WorldEdit" );
		com.sk89q.worldedit.bukkit.selections.Selection sel = we.getSelection( p );
		if( sel == null ){
			p.sendMessage(ChatColor.RED + "You must make a WorldEdit Selection first" );
			return;
		}
		p.sendMessage( ChatColor.AQUA + "Calculating blocks..." );
		if ( mode == 1 ) p.sendMessage( ChatColor.AQUA + "Converting all " + detectBlock.toString() + " to respawning ores" );
		else if ( mode == 2 ) p.sendMessage( ChatColor.AQUA + "Removing all respawning ores" );
		else if ( mode == 3 ) p.sendMessage( ChatColor.AQUA + "Regenerating all respawning ores" );
		else p.sendMessage( ChatColor.AQUA + "Counting all respawning ores" );
		Location min = sel.getMinimumPoint();
		Location max = sel.getMaximumPoint();
		int amount = 0;
		int found = 0;
		for ( int bx = min.getBlockX(); bx <= max.getBlockX(); bx ++ ) {
			for ( int by = min.getBlockY(); by <= max.getBlockY(); by ++ ) {
				for ( int bz = min.getBlockZ(); bz <= max.getBlockZ(); bz ++ ) {
					Location blockl = new Location( min.getWorld(), bx, by, bz );
					if ( locations.containsKey( blockl ) ) found++;
					if ( mode == 1 ) {
						if ( blockl.getBlock().getType() == detectBlock ) {
							if ( !locations.containsKey( blockl ) ) {
								amount++;
								RespawningOre ore = util.selectRandomMaterial();
								locations.put( blockl, ore );
								blockl.getBlock().setType( ore.block );
							}
						}
					} else if ( mode == 2 ) {
						if ( locations.containsKey( blockl ) ) {
							amount++;
							blockl.getBlock().setType( brokenBlock );
							locations.remove( blockl );
							ores.remove( blockl );
						}
					} else if ( mode == 3 ) {
						if ( locations.containsKey( blockl ) ) {
							amount++;
							RespawningOre ore = util.selectRandomMaterial();
							blockl.getBlock().setType( ore.block );
							locations.put( blockl, ore );
							ores.remove( blockl );
						}
					}
				}
			}
		}
		if ( mode == 1 ) {
			p.sendMessage( ChatColor.AQUA + "Detected " + String.valueOf( amount ) + " new block(s) and converted them!" );
			p.sendMessage( ChatColor.AQUA + String.valueOf( found ) + " blocks were already respawning ore(s)!" );
		} else if ( mode == 2 ) p.sendMessage( ChatColor.AQUA + "Removed " + String.valueOf( amount ) + " respawning ore(s)" );
		else if ( mode == 3 ) p.sendMessage( ChatColor.AQUA + "Regenerated " + String.valueOf( amount ) + " respawning ore(s)" );
		else p.sendMessage( ChatColor.AQUA + "Found " + String.valueOf( found ) + " respawning ore(s)" );
	}
}
