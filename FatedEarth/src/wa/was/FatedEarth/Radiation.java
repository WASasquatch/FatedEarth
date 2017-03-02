package wa.was.FatedEarth;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
// Import Bukkit Utils
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
// Player
import org.bukkit.entity.Player;
// Event Handlers & Listener
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
// Import ActionBarAPI
import com.connorlinfoot.actionbarapi.ActionBarAPI;

public class Radiation implements Listener {
	
	// Globals
	private static FileConfiguration config;
	public static Radiation radiation;
	public static JavaPlugin plugin;
	private static Server server;
	public static Player player;
	public static Object actWorld;
	public static String world;
	public static ItemStack geigerItemStack;
	public static List<ItemStack> hazmatSuits;
	public static boolean exposure;
	public static boolean isRaining;
	public static boolean isUVRisk;
	public static boolean useGeiger;
	public static byte lightLevel;
	public static byte lightExposure;
	public static int maxRads;
	public static int threshold;
	public static int radDam;
	public static int irradiatedBlockRadius;
	
	// Start Event
	public Radiation() {

		radiation = this;
		server = Bukkit.getServer();
		config = FatedEarth.config;
		plugin = FatedEarth.plugin;
		useGeiger = FatedEarth.useGeiger;
		geigerItemStack = FatedEarth.geigerItemStack;
		hazmatSuits = FatedEarth.hazmatSuits;
        maxRads = config.getInt("max-radiation-level");
        threshold = config.getInt("radiation-threshold");
        radDam = config.getInt("radiation-damage");
        irradiatedBlockRadius = config.getInt("irradiated-block-radius");
		// Start Fated Earth Cycles
		startCycles();

	}
	
	private void startCycles() {
			
			@SuppressWarnings("unused")
	        BukkitTask radEffects = new BukkitRunnable() {
				@Override
				public void run() {
					irradiateBlocks();
				}        
	        }.runTaskTimer(plugin, 0, 200);
	        
			@SuppressWarnings("unused")
	        BukkitTask toxicWater = new BukkitRunnable() {
				@Override
				public void run() {
					toxicWater();
				}        
	        }.runTaskTimer(plugin, 0, 200);

			//@SuppressWarnings("unused")
	        //BukkitTask irradiateBlocks = new BukkitRunnable() {
			//	@Override
			//	public void run() {
			//		irradiateBlocks();
			//	}        
	        //}.runTaskTimer(plugin, 0, 100);
			
	}
	
	public void radiationEffects() {
		
		// Online Players
		Collection<? extends Player> list = (Bukkit.getOnlinePlayers());
		
		if ( list != null ) {
		
			// Iterate online players
			for ( Player player : list ) {
				
				if ( player != null ) {
					
					// Sync this player
					syncPlayer(player);
					
					// World
					world = player.getWorld().getName();
					Location pl = player.getLocation();
					World pw = player.getWorld();
					Biome biome = pw.getBiome(pl.getBlockX(), pl.getBlockZ());
					 
					// Storming and rainy biome?
					if ( pw.hasStorm() && biome != Biome.DESERT && biome != Biome.DESERT_HILLS ) {
					    isRaining = true;
					} else {
						isRaining = false;
					}
					
					// Get Blocks Above
			        int blocksAbovePlayer = getSolidBlocksAbove(player);	
			        
			        // Get valid worlds
			        boolean inWorld = false;
			        List<?> validWorlds = config.getList("solar-radiation-risk");
			        if ( validWorlds.contains(player.getWorld().getName()) ) {
			        	inWorld = true;
			        } else {
			        	System.out.print("[FatedEarth] Invalid wolrd: "+player.getWorld().getName());
			        }
			        player.sendMessage("In world: "+inWorld);
					
			        // Is player exposed to light and under right conditions?
			        lightExposure = player.getLocation().getBlock().getLightFromSky();
			        lightLevel = player.getLocation().getBlock().getLightLevel();
			        if ( lightExposure > 10 && lightLevel > 10 && blocksAbovePlayer == 0 && inWorld ) {
			        	isUVRisk = true;
			        } else {
			        	isUVRisk = false;
			        }				

			        // Do poison on rain
			        if ( config.getBoolean("toxic-rain") ) {
				        // Is it storming?
				        if ( isRaining && blocksAbovePlayer == 0 && isExposed(player) ) {
				        	if ( hasGeigerCounter(player) ) {
				        		ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN+"[Geiger Counter] "+ChatColor.YELLOW + "Toxic Rain Exposure Detected", 40);
				        	}
			        		if ( FatedEarth.isEffected(player) && !FatedEarth.isImmune(player) ) {
				        		if ( config.getBoolean("no-toxic-rain-death") ) { 
							        if ( (float) ( ( player.getHealth() * 100) / player.getMaxHealth() ) > 20.0 ) {
							        	evaluateRadiation(player, 1, 2);
							        }
				        		} else {
				        			evaluateRadiation(player, 1, 2);
				        		}
				        		playRadFatigue(player);
			        		}
				        }
			        }
			       
			        
					// Is it day and UV Risk?
					if ( this.day(player.getWorld().getName()) && isUVRisk ) {
						
					    // Do damage on sunlight
					    if ( config.getBoolean("solar-radiation") ) {
					    	
					        if ( hasGeigerCounter(player) ) {
					        	ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN+"[Geiger Counter] "+ChatColor.YELLOW + "Ultraviolet Radiation Exposure Detected", 40);
					        }
					        
						    if ( config.getBoolean("no-radiation-death") ) { 
						        // Damage Player if above 20% health & Isn't Wearing Armor (Protection)
				        		if ( FatedEarth.isEffected(player) && !FatedEarth.isImmune(player) ) {
								    if ( (float) ( ( player.getHealth() * 100) / player.getMaxHealth() ) > 20.0 ) {
								        player.damage(1);
								    }
				        		}
						    } else {
				        		if ( FatedEarth.isEffected(player) && !FatedEarth.isImmune(player) ) {
				        			player.damage(1);	        
				        		}
						    }
						    
					    }
						
					}
					
				}
				
			}
			
		}
		
	}
	
	public void toxicWater() {
		
		if ( config.getBoolean("toxic-water") ) {
		
			// Online Players
			Collection<? extends Player> list = (Bukkit.getOnlinePlayers());
			
			if ( list != null ) {
				
				// Iterate online players
				for ( Player player : list ) {
					
					if ( player != null ) {
						
						// Sync this player
						syncPlayer(player);
						
						if ( isInWater(player) ) {
							
						    if ( hasGeigerCounter(player) ) {
						    	ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN+"[Geiger Counter] "+ChatColor.YELLOW + "Toxic Water Exposure Detected", 40);
						    }
						    
			        		if ( FatedEarth.isEffected(player) && !FatedEarth.isImmune(player) ) {
				        		if ( config.getBoolean("no-toxic-rain-death") ) { 
							        if ( (float) ( ( player.getHealth() * 100) / player.getMaxHealth() ) > 20.0 ) {
							        	evaluateRadiation(player, 3, 2);
							        }
				        		} else {
				        			evaluateRadiation(player, 3, 2);
				        		}
				        		playRadFatigue(player);
			        		}
			        		
			        		playRadFatigue(player);
							
						}
						
					}
					
				}
	
			}
		
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void irradiateBlocks() {
		
		// Online Players
		Collection<? extends Player> list = (Bukkit.getOnlinePlayers());
		
		if ( list != null ) {
		
			// Iterate online players
			for ( Player player : list ) {
								
				if ( player != null ) {
					
					// Sync this player
					syncPlayer(player);
		
			        // Player near irradiated blocks?
			        List<String> blocks = (List<String>) config.getList("irradiated-blocks");

			        // Are there irradiated blocks?
			        if ( blocks != null ) {
			        	
			        	for ( int radius = irradiatedBlockRadius; radius >= 1; radius-- ) {
			        		
			        		if ( isPlayerNearBlocks(player, blocks, radius) ) {
			        				        		
			        			double radiationDamage = radDam;
				        		if ( radius > 1 ) {
				        			radiationDamage = radDam * 2;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 2 ) {
				        			radiationDamage = radDam * 1.5;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 3 ) {
				        			radiationDamage = radDam * 1.25;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 4 ) {
				        			radiationDamage = radDam;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 5 ) {
				        			radiationDamage = (radDam > (radDam - 0.25)) ? (radDam - 0.25) : 0;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 6 ) {
					        		radiationDamage = (radDam > (radDam - 0.50)) ? (radDam - 0.50) : 0;
						        	if ( hasGeigerCounter(player) ) {
						        		ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
						        	}
					        	} else if ( radius > 7 ) {
				        			radiationDamage = (radDam > (radDam - 0.75)) ? (radDam - 0.75) : radDam;;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 8 ) {
				        			radiationDamage = (radDam > (radDam - 1)) ? (radDam - 1) : 0;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 9 ) {
				        			radiationDamage = (radDam > (radDam - 1.25)) ? (radDam - 1.25) : 0;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 10 ) {
				        			radiationDamage = (radDam > (radDam - 1.50)) ? (radDam - 1.50) : 0;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 11 ) {
				        			radiationDamage = (radDam > (radDam - 1.75)) ? (radDam - 1.75) : 0;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		} else if ( radius > 12 ) {
				        			radiationDamage = (radDam > (radDam - 2)) ? (radDam - 2) : 0;
					        		if ( hasGeigerCounter(player) ) {
					        			ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN + "[Geiger Counter] " + ChatColor.GREEN + "Radioactive Source Detected: " + ChatColor.YELLOW + "~"+radiationDamage+"rad/s", 40);
					        		}
				        		}
				        		
				        		if ( FatedEarth.isEffected(player) && !FatedEarth.isImmune(player) ) {
					        		
					        		if ( config.getBoolean("no-radiation-death") ) { 
								        if ( (float) ( ( player.getHealth() * 100) / player.getMaxHealth() ) > 20.0 ) {
								        	evaluateRadiation(player, radiationDamage, 2);
								        }
					        		} else {
					        			evaluateRadiation(player, radiationDamage, 2);
					        		}
				        		
				        		}
			        		
			        		}
			        		
			        	}
			        	
			        }
		        
				}
		        
			}
			
		}
        
	}
	
	
	// START FUNCTIONS
	
	// Play the fatigue sounds
	public static void playRadFatigue(Player player) {
		
    	player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_BREATH, 1, 1);
        new BukkitRunnable() {
			@Override
			public void run() {
				player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_BREATH, 1, 1);
			}        
        }.runTaskLater(plugin, 10);
        
	}
	
	// has a geiger counter
	public boolean hasGeigerCounter(Player player) {
		if ( useGeiger == false ) {
			return true;
		}
	    for(int i = 0; i <= 8; i++) {
	    	if ( player.getInventory().getItem(i) != null ) {
		        ItemStack item = player.getInventory().getItem(i);
		        if ( item.equals(geigerItemStack) ) {
		        	return true;
		    	}
	    	}
		}
		return false;
		
	}
	
	public void syncPlayer(Player player) {
		
		evaluateRadiation(player, 0, 0);
		
	}
	
	// Testing radiation evaluation
	public static void evaluateRadiation(Player player, double addRads, double setDam) {
		
        File playerFolder;
        try {
            playerFolder = new File(plugin.getDataFolder() + File.separator + "users" + File.separator);
            if( !playerFolder.exists() ){
                playerFolder.mkdirs();
            }
            UUID uuid = player.getUniqueId();
            File file = new File(playerFolder, uuid+".yml");
            if ( !file.exists() ) {
            	
            	FileConfiguration playerFile = YamlConfiguration.loadConfiguration(file);
            	
	            try {
	            	
	            	if ( !playerFile.isSet("radiation-level") ) {
	            		
	            		playerFile.set("radiation-level", 0);
	            		playerFile.save(file);
	            		
		            }
	            	
	            } catch(IOException e) {
	            	e.printStackTrace();
	            }
	            
            }
        	FileConfiguration playerFile = YamlConfiguration.loadConfiguration(file);
        	
            double radsLevel = getRadiationLevel(player);
            
            if ( (double) radsLevel > (int) maxRads ) {
            	
            	// Ultra-Kill?
            	player.damage(99999);
            	playRadFatigue(player);
            	playerFile.set("radiation-level", 0);
            	
	            try {
	            	playerFile.save(file);
	            } catch(IOException e) {
	            	e.printStackTrace();
	            }
	            
	            return;
	            
            }
            
            double newRads = ((double) radsLevel + (double) addRads);
            
            if ( newRads > (double) maxRads ) {
            	
            	// Ultra-Kill?
            	player.damage(99999);
            	playRadFatigue(player);
            	playerFile.set("radiation-level", 0);
            	
	            try {
	            	playerFile.save(file);
	            } catch(IOException e) {
	            	e.printStackTrace();
	            }
	            
            } else {
            	
            	playerFile.set("radiation-level", newRads);
            	
	            try {
	            	playerFile.save(file);
	            } catch(IOException e) {
	            	e.printStackTrace();
	            }
	            
            }
            
            radiationDamage(player, setDam);
            
        } catch(SecurityException e) {
            e.printStackTrace();
        }

	}
	
	public static void radiationDamage(Player player, double setDam) {
		
        double radsLevel = getRadiationLevel(player);

        if ( radsLevel >= threshold ) {
        	
        	ActionBarAPI.sendActionBar(player, ChatColor.DARK_GREEN+"[Geiger Counter] " + ChatColor.RED + "You have radiation sickness!", 40);
        	
        	if ( config.getBoolean("no-radiation-death") ) {
        		
		        if ( (float) ( ( player.getHealth() * 100) / player.getMaxHealth() ) > 20.0 ) {
		        	if ( (double) setDam > 1 ) {
		        		player.damage(setDam);
		        	} else {
		        		player.damage(radDam);
		        	}
		        }
		        
        	} else {
	        	if ( (double) setDam > 1 ) {
	        		player.damage(setDam);
	        	} else {
	        		player.damage(radDam);
	        	}
        	}
        	
        	// Play radiation fatigue sound
        	playRadFatigue(player);

        }
        
	}
	
	// Testing get radiation
	public static double getRadiationLevel(Player player) {
		
        File playerFolder;
        
        try {
        	
            playerFolder = new File(plugin.getDataFolder() + File.separator + "users" + File.separator);

            if( !playerFolder.exists() ){
            	evaluateRadiation(player, 0, 0);
            }
            
            UUID uuid = player.getUniqueId();
            File file = new File(playerFolder, uuid+".yml");
            FileConfiguration playerFile = YamlConfiguration.loadConfiguration(file);
            
            return (double) playerFile.getDouble("radiation-level");
            
        } catch(SecurityException e) {
            e.printStackTrace();
        }
        
		return 0;
        
	}
	
	// Detect tick time
	public boolean day(String world) {
		
	    long time = server.getWorld(world).getTime();
	    return time < 12300 || time > 23850;
	    
	}
	
	// check is wearing radiation suit
	public static boolean hasRadiationSuit(Player player, List<ItemStack> hazmatSuits) {
		
		ItemStack[] pa = player.getInventory().getArmorContents();
		
		int partCount = 0;
		for ( ItemStack item : pa ) {
			for ( int i = 0; i <= 3; i++ ) {
				if ( item == null ) {
					return false;
				}
				for ( ItemStack hazmatItem : hazmatSuits ) {
					if ( hazmatItem.equals(item) ) {
						partCount++;
					}
				}
			}
		}
		if ( (int) partCount == 4 ) {
			return true;
		}
		return false;
		
	}
	
	// check if in water
	public boolean isInWater(Player player) {
    Material m = player.getLocation().getBlock().getType();
    if (m == Material.STATIONARY_WATER || m == Material.WATER) {
        return true;
    }
    return false;
}
	
	public boolean isExposed(Player player) {

		if ( hasRadiationSuit(player, hazmatSuits) ) {
			return false;
		}
		ItemStack[] pa = player.getInventory().getArmorContents();

		int armorCount = 0;
		for ( ItemStack item : pa ) {
			if ( item != null ) {
				armorCount++;
			}
		}
		if ( (int) armorCount == 4 ) {
			return false;
		}
		
		return true;
	}
	
	// Iterate Blocks Above Player
    public static int getSolidBlocksAbove(Player player) {
 
        World pw = player.getWorld();
        int buildHeight = pw.getMaxHeight();
        Location location = player.getLocation();
        int distance = buildHeight - location.getBlockY();
        int SolidAboveCount = 0;
 
        BlockIterator bi = new BlockIterator( pw, location.toVector(), new Vector( 0, 1, 0 ), 0, distance );

        while ( bi.hasNext() ) {
            Block toInspect = bi.next();
            if( toInspect.getType() != Material.AIR )
                SolidAboveCount++;
        }
        return SolidAboveCount;
        
    }
    
    // Thanks to BillyGalbreath for help with ellipsis search
    public boolean isPlayerNearBlocks(Player player, List<String> blocks, int radius) {
        World world = player.getWorld();
        Location center = player.getLocation();
        int cX = center.getBlockX();
        int cY = center.getBlockY();
        int cZ = center.getBlockZ();
        int radiusSquared = radius * radius;
        for ( int x = cX - radius; x <= cX + radius; x++ ) { // cycle X
            for ( int y = cY - radius; y <= cY + radius; y++ ) { // cycle Y
                for ( int z = cZ - radius; z <= cZ + radius; z++ ) { // cycle Z
                    if ( (cX - x) * (cX - x) + (cY - y) * (cY - y) + (cZ - z) * (cZ - z) <= radiusSquared ) { // Ignore all coords outside a circle (leave this line out to be square)
                        for ( int i = 0; i < blocks.size(); i++ ) {
                        	if ( Material.valueOf(blocks.get(i)).equals(world.getBlockAt(x, y, z).getType()) ) {
                        		return true;
                        	}
                        }
                    }
                }
            }
        }
        return false;
    }
    
	
}
