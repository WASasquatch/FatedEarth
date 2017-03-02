package wa.was.FatedEarth;

// Import Java Utils
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Import Bukkit API & Utils
import org.bukkit.plugin.java.JavaPlugin;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.*;

public class FatedEarth extends JavaPlugin {
	
	public static Server server;
	public static FileConfiguration config;
	public static JavaPlugin plugin;
	public static boolean useGeiger;
	public static ItemStack geigerItemStack;
	public static List<ItemStack> hazmatSuits = new ArrayList<ItemStack>();
	
	// Setup globals
	public FatedEarth() {
    	plugin = this;
    	server = Bukkit.getServer();
    	config = plugin.getConfig();
    	useGeiger = config.getBoolean("use-geiger-counter-item");
    	createConfig();
	}
	
    @Override
    public void onEnable() {
    	
        // Inject Recipes
    	if ( useGeiger ) {
    		System.out.print("[FatedEarth] Using Geiger Counter: true");
    		server.addRecipe(geigerCounter());
    	} else {
    		System.out.print("[FatedEarth] Using Geiger Counter: false");
    	}
    	
    	// Inject Radiation Suits
    	radiationSuit();
    	
		List<String> validWorlds = config.getStringList("solar-radiation-risk");
		String worldActive = "[FatedEarth] Solar Radiation Risk in:";
		int worldCnt = 1;
    	for ( String world : validWorlds ) {
    		if ( server.getWorld(world) != null ) {
    			worldActive += (" "+world);
    			if ( worldCnt < validWorlds.size() ) {
    				worldActive += ",";
    				worldCnt++;
    			}
    		} else {
    			worldActive += (" "+world+" (Invalid!),");
    			if ( worldCnt < validWorlds.size() ) {
    				worldActive += ",";
    				worldCnt++;
    			}
    		}
    	}
    	System.out.print(worldActive);
    	
    	// Register Events
    	server.getPluginManager().registerEvents(new Events(), plugin);
    	server.getPluginManager().registerEvents(new Radiation(), plugin);
        
    }
    
    @Override
    public void onDisable() {
    	
    }
    
    // Radiation Suits
	public void radiationSuit() {
    	
    	@SuppressWarnings("unchecked")
		List<ItemStack> setArmor = (List<ItemStack>)config.getList("radiation-suit");
    	
    	
    	for (Object piece : setArmor) {
    		
    		if ( piece != null ) {
    			
	    		Material pieceMaterial = Material.valueOf(piece.toString());
	    		String itemType[] = piece.toString().split("_");
	    		String itemTitle = WordUtils.capitalize(itemType[0].toLowerCase())
	        			+ " Hazmat " + WordUtils.capitalize(itemType[1].toLowerCase());
 	    		String compatTypes = "HELMET CHESTPLATE LEGGINGS BOOTS";
	    		
	    		if ( pieceMaterial != null ) {
	    			
	    			if ( compatTypes.matches("(?i).*"+itemType[1]+".*") ) {

	    		        ItemStack hazmatItemStack = new ItemStack(pieceMaterial);
	    		        System.out.print("[FatedEarth] Radiation Suit Piece Added: "+itemTitle);
	    		        ItemMeta itemMeta = hazmatItemStack.getItemMeta();
	    		        itemMeta.setDisplayName(ChatColor.GREEN + "Hazmat " + itemTitle + " Piece");
	    		        hazmatItemStack.setItemMeta(itemMeta);
	    		        hazmatSuits.add(hazmatItemStack);
	    		        ShapelessRecipe hazmatRecipe = new ShapelessRecipe(hazmatItemStack);
	    		        hazmatRecipe.addIngredient(pieceMaterial);
	    		        hazmatRecipe.addIngredient(Material.EMERALD);
	    		        hazmatRecipe.addIngredient(Material.EMERALD);
	    		        server.addRecipe(hazmatRecipe);
	    				
	    			}
	    			
	    		}
	    		
    		}
    		
    	}

    }
    
    // Geiger Counter Recipe
    public ShapedRecipe geigerCounter() {
    	
    	if ( config.getString("geiger-item").isEmpty() == false ) {
    		
	    	Material geigerItem = Material.valueOf(config.getString("geiger-item").toUpperCase());
	    	ItemStack geigerCounter = new ItemStack(geigerItem);
	    	System.out.print("[FatedEarth] Geiger Item: " + geigerCounter.getType().name());
	    	ItemMeta geigerMeta = geigerCounter.getItemMeta();
	    	geigerMeta.setDisplayName(ChatColor.GREEN + "Geiger Counter");
	    	geigerCounter.setItemMeta(geigerMeta);
	    	geigerItemStack = geigerCounter;
	        ShapedRecipe geiger = new ShapedRecipe(geigerCounter);
	        geiger.shape(" E ", "ICI", " E ");
	        geiger.setIngredient('E', Material.EMERALD);
	        geiger.setIngredient('I', Material.IRON_INGOT);
	        geiger.setIngredient('C', geigerItem);
	        return geiger;
	        
    	}
    	
    	return null;

    }
    
    
    // Create config
    private void createConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().info("Config.yml not found, creating!");
                saveDefaultConfig();
            } else {
                getLogger().info("Config.yml found, loading!");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

    }
    
	public static boolean isEffected(Player player) {
		if ( player.hasPermission("fatedearth.radiation") ) {
			return true;
		}
		return false;
	}
	
	public static boolean isImmune(Player player) {
		if ( player.hasPermission("fatedearth.radiation.deny") || Radiation.hasRadiationSuit(player, (List<ItemStack>) hazmatSuits) ) {
			return true;
		}
		return false;
	}
    
}