package wa.was.FatedEarth;

import org.bukkit.ChatColor;
// Import Bukkit Utils
import org.bukkit.configuration.file.FileConfiguration;
// Event Handlers & Listener
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

// Event
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.connorlinfoot.actionbarapi.ActionBarAPI;

import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;

// Entities
import org.bukkit.entity.Zombie;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;

public class Events implements Listener {
	
	// Config global
	private JavaPlugin plugin;
	private FileConfiguration config;
	private ItemStack geigerItemStack;
	private Player player;
	private boolean useGeiger;
	public int maxRads;
	
	// Load config
	public Events() {
		plugin = FatedEarth.plugin;
		config = FatedEarth.config;
		useGeiger = FatedEarth.useGeiger;
		maxRads = Radiation.maxRads;
		if ( useGeiger ) {
			geigerItemStack = FatedEarth.geigerItemStack;
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent event) {
		
		player = event.getPlayer();
		double radiationLevel = Radiation.getRadiationLevel(player);
		if ( (int) radiationLevel > (int) maxRads ) {
	    @SuppressWarnings("unused")
		BukkitTask killPlayer = new BukkitRunnable() {
				@Override
				public void run() {
					player.damage(9999);
				}        
	        }.runTaskLater(plugin, 50);
			
		}
		Radiation.evaluateRadiation(player, 0, 0);
		
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerItemHeld(PlayerItemHeldEvent event) {
		if ( config.getBoolean("use-geiger-counter-item") ) {
			player = event.getPlayer();
			ItemStack item = player.getInventory().getItem(event.getNewSlot());
			if ( item != null && item.equals(geigerItemStack) ) {
				ActionBarAPI.sendActionBar(player, ChatColor.GREEN + "[Geiger Counter] " + ChatColor.YELLOW + "Radiation Level: " + ChatColor.WHITE + Radiation.getRadiationLevel(player), 40);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onEntityCombust(EntityCombustEvent event) {
		// Return if interactive event
	    if ( event instanceof EntityCombustByBlockEvent
	            || event instanceof EntityCombustByEntityEvent ) {
	        return;
	    }
		if ( config.getBoolean("sunproof-zombies") ) {
			if ( event.getEntity() instanceof Zombie ) {
				event.setCancelled(true);
			}
		}
		if ( config.getBoolean("sunproof-skeletons") ) {
			if ( event.getEntity() instanceof Skeleton ) {
				event.setCancelled(true);
			}
		}
		if ( config.getBoolean("sunproof-pigmen") ) {
			if ( event.getEntity() instanceof PigZombie ) {
				event.setCancelled(true);
			}
		}
	}

}
