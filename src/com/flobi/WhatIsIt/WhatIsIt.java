package com.flobi.WhatIsIt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.util.Vector;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.config.skills.alchemy.PotionConfig;
import com.gmail.nossr50.datatypes.skills.alchemy.AlchemyPotion;

/**
 * 
 * WhatIsIt
 * Friendly names plugin for Bukkit
 * 
 * @author Joshua "Flobi" Hatfield
 */
 
public class WhatIsIt extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");

	private static File configFile = null;
	private static InputStream defConfigStream;
	private static FileConfiguration config = null;
	private static boolean showDataValues = false;
	private static boolean useDisplayName = false;
	private static File namesConfigFile = null;
	private static InputStream defNamesConfigStream;
	private static FileConfiguration namesConfig = null;
	private static YamlConfiguration defConfig = null;
	private static YamlConfiguration defNamesConfig = null;
	private static String version;

	private static File dataFolder;
	private static Permission perms = null;
	private static ConsoleCommandSender console;
	

	/**
	 * Event executed when enabled.
	 */
	public void onEnable() {
		version = this.getDescription().getVersion();
		console = getServer().getConsoleSender();
		dataFolder = getDataFolder();
		defConfigStream = getResource("config.yml");
		defNamesConfigStream = getResource("names.yml");
		loadConfig();

		if (getServer().getPluginManager().getPlugin("Vault") != null) {
	        setupPermissions();
		}

        // Load up the Plugin metrics
        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }
		console.sendMessage(chatPrep(config.getString("messages.has-been-enabled")));
	}
	/**
	 * Event executed when disabled.
	 */
	public void onDisable() { 
		console.sendMessage(chatPrep(config.getString("messages.has-been-disabled")));
	}
	
	/**
	 * Event executed when commands are entered.
	 * 
	 * @param CommandSender sender
	 * @param Command command being sent
	 * @param String command label
	 * @param String[] command arguments
	 * 
	 * @return boolean success of command
	 */
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	Player player = null;
    	String iATR = "XXXITEMXAMPERSANDXTEMPRARYXREPLACEMENTXXX";
    	if (sender instanceof Player) {
    		player = (Player) sender;
    	} else {
    		console.sendMessage(chatPrep(config.getString("messages.console-error")));
			return true;
    	}
    	String newName = null;
     
    	if (
    		cmd.getName().equalsIgnoreCase("wis") ||
        	cmd.getName().equalsIgnoreCase("wit")
    	) {
    		if(hasPermission(player, "whatisit.admin")) {
	    		if (args.length > 0) {
	    			if (args[0].equalsIgnoreCase("itis")) {
	    				String[] tmpArgs = (String[]) Array.newInstance(String.class, args.length - 1);
	    				System.arraycopy(args, 1, tmpArgs, 0, args.length - 1);
	    				newName = StringUtils.join(tmpArgs, " ");
	    				if (newName.length() == 0) {
	    					newName = null;
	    				}
	    			} else if (args[0].equalsIgnoreCase("reload")) {
		    			namesConfig = null;
		    			loadConfig();
	    			}
	    		}
    		}
    	}
    	if (cmd.getName().equalsIgnoreCase("wis")) {
    		if(!hasPermission(player, "whatisit.use")) {
    			player.sendMessage(chatPrep(config.getString("messages.no-permission")));
    			return false;
    		}
    		ItemStack heldItem = player.getItemInHand();
    		player.sendMessage(chatPrep(config.getString("messages.this-is") + WhatIsIt.itemName(heldItem, showDataValues, newName).replaceAll("&", iATR)).replaceAll(iATR, "&"));
    		Map<Enchantment, Integer> enchantments = heldItem.getEnchantments();
    		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
   				player.sendMessage(chatPrep(config.getString("messages.enchanted") + enchantmentName(enchantment, showDataValues, newName).replaceAll("&", iATR)).replaceAll(iATR, "&"));
    		}				
    		return true;
    		
    	} else if (cmd.getName().equalsIgnoreCase("wit")) {
    		if(!hasPermission(player, "whatisit.use")) {
    			player.sendMessage(chatPrep(config.getString("messages.no-permission")));
    			return false;
    		}
    		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
    			namesConfig = null;
    			loadConfig();
    		}
    		Object targetedObject = getTarget(player);
    		player.sendMessage(chatPrep(config.getString("messages.that-is") + WhatIsIt.name(targetedObject, showDataValues, newName).replaceAll("&", iATR)).replaceAll(iATR, "&"));
    		return true;
    	}
    	return false;
    }
    
	/**
	 * Permissions wrapper, uses Vault if available, otherwise superPerms.
	 * 
	 * @param Player player to check
	 * @param String permission level
	 * @return boolean has permission
	 */
    private boolean hasPermission(Player player, String permission) {
    	if (player == null) {
    		return false;
    	}
    	if (perms == null) {
    		return player.hasPermission(permission);
    	} else {
    		return perms.has(player, permission);
    	}
    }
    
	/**
	 * Prepares chat, prepending prefix and processing colors.
	 * 
	 * @param String message to prepare
	 * @return String prepared message
	 */
    private static String chatPrep(String message) {
    	message = config.getString("messages.prefix") + message;
    	message = ChatColor.translateAlternateColorCodes('&', message);//message.replaceAll("&([0-9a-fA-F])", "�$1");
    	return message;
    }

	/**
	 * Attempts to retrieve targeted entity or object.
	 * 
	 * @param Player player whose sight to check for target
	 * @return Object target entity or block
	 */
    private static Object getTarget(Player entity) {
    	int range = 100;
    	Iterable<Entity> entities = entity.getNearbyEntities(range, range, range);
    	Entity targetEntity = null;
    	double threshold = 1.2;
    	for (Entity other:entities) {
	    	Vector n = other.getLocation().toVector().subtract(entity.getLocation().toVector());
	    	if (entity.getLocation().getDirection().normalize().crossProduct(n).lengthSquared() < threshold && n.normalize().dot(entity.getLocation().getDirection().normalize()) >= 0) {
		    	if (targetEntity == null || targetEntity.getLocation().distanceSquared(entity.getLocation()) > other.getLocation().distanceSquared(entity.getLocation()))
		    		targetEntity = other;
    		}
    	}
    	Block targetBlock = entity.getTargetBlock(null, range);
    	if (targetBlock == null) {
    		return targetEntity;
    	} else if (targetEntity == null) {
    		return targetBlock;
    	} else {
        	if (targetBlock.getLocation().distanceSquared(entity.getLocation()) > targetEntity.getLocation().distanceSquared(entity.getLocation())) {
    	    	return targetEntity;
        	} else {
    	    	return targetBlock;
        	}
    	}
    }

    /**
	 * Loads config.yml and names.yml configuration files.
	 */
    private static void loadConfig() {
	    if (defConfigStream != null) {
	        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        defConfigStream = null;
	    }
		if (configFile == null) {
	    	configFile = new File(dataFolder, "config.yml");
	    }
	    if (!configFile.exists() && defConfig != null) {
	    	try {
	    		defConfig.save(configFile);
			} catch(IOException ex) {
				log.severe(chatPrep(defConfig.getString("messages.cannot-save-default-config")));
			}
	    }
	    config = YamlConfiguration.loadConfiguration(configFile);
    	config.setDefaults(defConfig);

    	// Make sure any new or missing entries are added to the config.yml file so people can see them.
		Map<String, Object> configValues = config.getDefaults().getValues(true);
		for (Map.Entry<String, Object> configEntry : configValues.entrySet()) {
			config.set(configEntry.getKey(), config.get(configEntry.getKey()));
		}
    	
    	try {
    		config.save(configFile);
		} catch(IOException ex) {
			log.severe(chatPrep(config.getString("messages.cannot-save-default-config")));
		}

	    if (defNamesConfigStream != null) {
	        defNamesConfig = YamlConfiguration.loadConfiguration(defNamesConfigStream);
	        defNamesConfigStream = null;
	    }
    	if (namesConfigFile == null) {
	    	namesConfigFile = new File(dataFolder, "names.yml");
	    }
	    if (!namesConfigFile.exists() && defNamesConfig != null) {
	    	try {
	    		defNamesConfig.save(namesConfigFile);
			} catch(IOException ex) {
				log.severe(chatPrep(config.getString("messages.cannot-save-default-names")));
			}
	    }
	    namesConfig = YamlConfiguration.loadConfiguration(namesConfigFile);
        namesConfig.setDefaults(defNamesConfig);

		showDataValues = config.getBoolean("config.display-data-values");
		useDisplayName = config.getBoolean("config.use-display-name");

    	// This is the WhatIsIt version, not the Minecraft version.
		if (namesConfig.getString("version").compareTo(version) < 0) {
	    	if (namesConfig.getString("version").compareTo("1.2.1") < 0) {
	    		// These were reused/renamed by Minecraft:
	    		namesConfig.set("items.373;6", defNamesConfig.getString("items.373;6"));
	    		namesConfig.set("items.373;14", defNamesConfig.getString("items.373;14"));
	    		namesConfig.set("items.373;54", defNamesConfig.getString("items.373;54"));
	    		namesConfig.set("items.373;62", defNamesConfig.getString("items.373;62"));
	    	}
	    	if (namesConfig.getString("version").compareTo("1.3.0") < 0) {
	    		// This was reassigned to allow %r and %a.
	    		namesConfig.set("enchantmentlevels.UNKNOWN", defNamesConfig.getString("enchantmentlevels.UNKNOWN"));
	    	}
	    	if (namesConfig.getString("version").compareTo("1.3.5") < 0) {
	    		// I screwed these up, sorry.
	    		namesConfig.set("items.158;0", defNamesConfig.getString("items.158;0"));
	    		namesConfig.set("items.159;0", defNamesConfig.getString("items.159;0"));
	    	}
	    	if (namesConfig.getString("version").compareTo("1.3.6") < 0) {
	    		// These were wrongly set.
	    		namesConfig.set("entities.19:0", null);
	    		namesConfig.set("entities.65:0", null);
	    		namesConfig.set("entities.66:0", null);
	    		
	    		// I decided to remove my joke of "Fried Chicken" instead of "Cooked Chicken"...dont' know if anyone even saw it.
	    		namesConfig.set("items.366;0", defNamesConfig.getString("items.366;0"));
	    		
	    		// Changed silver sheep to be in line with the other items that color (light gray).
	    		namesConfig.set("entities.91;8", defNamesConfig.getString("items.91;8"));
	    		
	    		// Renamed Discs
	    		namesConfig.set("items.2256;0", defNamesConfig.getString("items.2256;0"));
	    		namesConfig.set("items.2257;0", defNamesConfig.getString("items.2257;0"));
	    		namesConfig.set("items.2258;0", defNamesConfig.getString("items.2258;0"));
	    		namesConfig.set("items.2259;0", defNamesConfig.getString("items.2259;0"));
	    		namesConfig.set("items.2260;0", defNamesConfig.getString("items.2260;0"));
	    		namesConfig.set("items.2261;0", defNamesConfig.getString("items.2261;0"));
	    		namesConfig.set("items.2262;0", defNamesConfig.getString("items.2262;0"));
	    		namesConfig.set("items.2263;0", defNamesConfig.getString("items.2263;0"));
	    		namesConfig.set("items.2264;0", defNamesConfig.getString("items.2264;0"));
	    		namesConfig.set("items.2265;0", defNamesConfig.getString("items.2265;0"));
	    		namesConfig.set("items.2266;0", defNamesConfig.getString("items.2266;0"));
	    		namesConfig.set("items.2267;0", defNamesConfig.getString("items.2267;0"));
	    	}
	    	if (namesConfig.getString("version").compareTo("1.3.7") < 0) {
	    		// Rose renamed to poppy, locked chest reused as stained glass in Minecraft 1.7.
	    		namesConfig.set("items.38;0", defNamesConfig.getString("items.38;0"));
	    		namesConfig.set("items.95;0", defNamesConfig.getString("items.95;0"));
	    		// Potion of Water Breathing takes two slots and replaces other unused ones.
	    		namesConfig.set("items.373;13", defNamesConfig.getString("items.373;13"));
	    		namesConfig.set("items.373;45", defNamesConfig.getString("items.373;45"));
	    	}
	    	if (namesConfig.getString("version").compareTo("1.3.8") < 0) {
	    		// mcMMO Alchemy Potions
	    		namesConfig.set("items.373;768", defNamesConfig.getString("items.373;768"));
	    		namesConfig.set("items.373;800", defNamesConfig.getString("items.373;800"));
	    		namesConfig.set("items.373;1024", defNamesConfig.getString("items.373;1024"));
	    		namesConfig.set("items.373;1056", defNamesConfig.getString("items.373;1056"));
	    		namesConfig.set("items.373;2048", defNamesConfig.getString("items.373;2048"));
	    		namesConfig.set("items.373;2080", defNamesConfig.getString("items.373;2080"));
	    		namesConfig.set("items.373;2304", defNamesConfig.getString("items.373;2304"));
	    		namesConfig.set("items.373;2816", defNamesConfig.getString("items.373;2816"));
	    		namesConfig.set("items.373;2848", defNamesConfig.getString("items.373;2848"));
	    		namesConfig.set("items.373;3840", defNamesConfig.getString("items.373;3840"));
	    		namesConfig.set("items.373;4352", defNamesConfig.getString("items.373;4352"));
	    		namesConfig.set("items.373;4384", defNamesConfig.getString("items.373;4384"));
	    		namesConfig.set("items.373;5120", defNamesConfig.getString("items.373;5120"));
	    		namesConfig.set("items.373;5152", defNamesConfig.getString("items.373;5152"));
	    		namesConfig.set("items.373;5376", defNamesConfig.getString("items.373;5376"));
	    		namesConfig.set("items.373;5408", defNamesConfig.getString("items.373;5408"));
	    		namesConfig.set("items.373;5632", defNamesConfig.getString("items.373;5632"));
	    		namesConfig.set("items.373;5664", defNamesConfig.getString("items.373;5664"));
	    		namesConfig.set("items.373;5888", defNamesConfig.getString("items.373;5888"));
	    		namesConfig.set("items.373;5920", defNamesConfig.getString("items.373;5920"));
	    	}
	    	
	    	// Make sure any new entries are added to the names.yml file so people can see them.
    		Map<String, Object> nameConfigValues = namesConfig.getDefaults().getValues(true);
    		for (Map.Entry<String, Object> textConfigEntry : nameConfigValues.entrySet()) {
    			namesConfig.set(textConfigEntry.getKey(), namesConfig.get(textConfigEntry.getKey()));
    		}

    		namesConfig.set("version", version);
	    	saveNamesConfig();
		}
    }

	/**
	 * Saves names.yml configuration file
	 */
    private static void saveNamesConfig() {
    	
    	try {
    		namesConfig.save(namesConfigFile);
		} catch(IOException ex) {
			log.severe(chatPrep(config.getString("messages.cannot-save-names")));
		}
    }

	/**
	 * Retrieves the name of Object.
	 * 
	 * @param Object what to identify
	 * @param Boolean show data values
	 * @param String new name
	 * @return String name of object
	 */
    private static String name(Object whatToIdentify, Boolean showData, String newName) {
		if (whatToIdentify instanceof Entity) {
			return entityName((Entity) whatToIdentify, showData, newName);
		}
		if (whatToIdentify instanceof ItemStack) {
			return itemName((ItemStack) whatToIdentify, showData, newName);
		}
		if (whatToIdentify instanceof Block) {
			return blockName((Block) whatToIdentify, showData, newName);
		}
		if (whatToIdentify instanceof ItemStack) {
			return itemName((ItemStack) whatToIdentify, showData, newName);
		}
		return config.getString("messages.unknown-object");
	}
	
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Entry&lt;Enchantment,&nbsp;Integer> enchantment to identify
	 * @return String name of enchantment
	 */
	public static String enchantmentName(Entry<Enchantment, Integer> enchantment) {
		return enchantmentName(enchantment, false);
	}
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Entry&lt;Enchantment,&nbsp;Integer> enchantment to identify
	 * @param Boolean show data values
	 * @return String name of enchantment
	 */
	public static String enchantmentName(Entry<Enchantment, Integer> enchantment, Boolean showData) {
		return enchantmentName(enchantment, showData, null);
	}
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Entry&lt;Enchantment,&nbsp;Integer> enchantment to identify
	 * @param Boolean show data values
	 * @param String new name
	 * @return String name of enchantment
	 */
	private static String enchantmentName(Entry<Enchantment, Integer> enchantment, Boolean showData, String newName) {
		if (enchantment == null) {
			return namesConfig.getString("enchantments.UNKNOWN");
		}
		return enchantmentName(enchantment.getKey(), enchantment.getValue());
	}
	/**
	 * Retrieves the name of Enchantment.
	 *
	 * @param Enchantment enchantment to identify
	 * @param Integer level of enchantment
	 * @return String name of enchantment
	 */
	public static String enchantmentName(Enchantment enchantment, Integer level) {
		return enchantmentName(enchantment, level, false);
	}
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Enchantment enchantment to identify
	 * @param Integer level of enchantment
	 * @param Boolean show data values
	 * @return String name of enchantment
	 */
	public static String enchantmentName(Enchantment enchantment, Integer level, Boolean showData) {
		return enchantmentName(enchantment, level, showData, null);
	}
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Enchantment enchantment to identify
	 * @param Integer level of enchantment
	 * @param Boolean show data values
	 * @param String new name
	 * @return String name of enchantment
	 */
	private static String enchantmentName(Enchantment enchantment, Integer level, Boolean showData, String newName) {
		if (enchantment == null) {
			return namesConfig.getString("enchantments.UNKNOWN");
		}
		return enchantmentName(enchantment) + 
			config.getString("messages.enchantment-level") + 
			enchantmentLevelName(level);
	}
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Enchantment enchantment to identify
	 * @return String name of enchantment
	 */
	public static String enchantmentName(Enchantment enchantment) {
		return enchantmentName(enchantment, false);
	}
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Enchantment enchantment to identify
	 * @param Boolean show data values
	 * @return String name of enchantment
	 */
	public static String enchantmentName(Enchantment enchantment, Boolean showData) {
		return enchantmentName(enchantment, showData, null);
	}
	/**
	 * Retrieves the name of Enchantment.
	 * 
	 * @param Enchantment enchantment to identify
	 * @param Boolean show data values
	 * @param String new name
	 * @return String name of enchantment
	 */
	private static String enchantmentName(Enchantment enchantment, Boolean showData, String newName) {
		if (enchantment == null) {
			return namesConfig.getString("enchantments.UNKNOWN");
		}
		
		String data = Integer.toString(enchantment.getId());

		if (newName != null) {
			namesConfig.set("entities." + data, newName);
			saveNamesConfig();
		}
		
		String name = namesConfig.getString("enchantments." + data);
		if (name == null) {
			name = namesConfig.getString("enchantments.UNKNOWN");
		}
		if (showData) {
			name = "(" + Integer.toString(enchantment.getId()) + ") " + name;
		}
		return name;
	}
	/**
	 * Retrieves the name of Enchantment level.
	 * 
	 * @param Integer enchantment level to identify
	 * @return String name of enchantment level
	 */
	public static String enchantmentLevelName(Integer level) {
		String name = namesConfig.getString("enchantmentlevels." + Integer.toString(level));
		if (name == null) {
			name = namesConfig.getString("enchantmentlevels.UNKNOWN");
		}
		name = name.replace("%r", romanNumerals(level)).replace("%a", level.toString());
		return name;
	}
	
	/**
	 * Converts arabic to roman.
	 * 
	 * @param Integer arabic numeral
	 * @return String roman numeral
	 */
	public static String romanNumerals(Integer level) {
		if (level == null) return "0";
		if (level == 0) return "0";
		String res = "";
		if (level < 0) {
			res += "-";
			level = 0 - level;
		}
	    String[] roman = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
	    int[] arabic  = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        for (int i = 0; i < roman.length; i++) {
            while (level >= arabic[i]) {
            	level -= arabic[i];
            	res  += roman[i];
            }
        }
	    
		return res;
	}
	
	/**
	 * Retrieves the name of Block.
	 * 
	 * @param Block block to identify
	 * @return String name of block
	 */
	public static String blockName(Block block) {
		return blockName(block, false);
	}
	/**
	 * Retrieves the name of Block.
	 * 
	 * @param Block block to identify
	 * @param Boolean show data values
	 * @return String name of block
	 */
	public static String blockName(Block block, Boolean showData) {
		return blockName(block, showData, null);
	}
	/**
	 * Retrieves the name of Block.
	 * 
	 * @param Block block to identify
	 * @param Boolean show data values
	 * @param String new name
	 * @return String name of block
	 */
	private static String blockName(Block block, Boolean showData, String newName) {
		if (block == null) {
			return namesConfig.getString("items.UNKNOWN");
		}
		
		byte blockData = block.getData();
		if (block.getTypeId() == 52) {
			// Monster Spawner, get type for data to pass to itemName:
			CreatureSpawner spawner = (CreatureSpawner) block.getState();
			blockData = (byte) spawner.getSpawnedType().getTypeId();
		}
		@SuppressWarnings("deprecation")
		ItemStack item = new ItemStack(block.getType(), 1, (short) 0, blockData);
		
		return itemName(item, showData, newName);
	}
	
	/**
	 * Retrieves the name of Player.
	 * 
	 * @param Player player to identify
	 * @return String name of player
	 */
	private static String playerName(Player player) {
		if (player == null) {
			return namesConfig.getString("entities.UNKNOWN");
		}
		if (useDisplayName) {
			return player.getName();
		} else {
			return player.getDisplayName();
		}
	}
	
	/**
	 * Retrieves the name of Entity.
	 * 
	 * @param Entity entity to identify
	 * @return String name of entity
	 */
	public static String entityName(Entity entity) {
		return entityName(entity, false);
	}
	/**
	 * Retrieves the name of Entity.
	 * @param Entity entity to identify
	 * @param Boolean show data values
	 * @return String name of entity
	 */
	public static String entityName(Entity entity, Boolean showData) {
		return entityName(entity, showData, null);
	}
	/**
	 * Retrieves the name of Entity.
	 * 
	 * @param Entity entity to identify
	 * @param Boolean show data values
	 * @param String new name
	 * @return String name of entity
	 */
	private static String entityName(Entity entity, Boolean showData, String newName) {
		if (entity == null) {
			return namesConfig.getString("entities.UNKNOWN");
		} else if (entity.getType() == EntityType.SPLASH_POTION) {
			Item item = (Item) entity;
			return itemName(item.getItemStack());
		} else if (entity.getType() == EntityType.DROPPED_ITEM) {
			Item item = (Item) entity;
			return itemName(item.getItemStack());
		} else if (entity instanceof Player) {
			return playerName((Player) entity);
		}
		
		String typeId = "";
		String data = "";
		String name = "";
		String owner_prefix = "";
		typeId = Short.toString(entity.getType().getTypeId());
		
		if (entity.getType() == EntityType.HORSE) {
			Horse horsey = (Horse) entity;
			data = Integer.toString(horsey.getVariant().ordinal());
		} else if (entity.getType() == EntityType.SKELETON) {
			Skeleton skelly = (Skeleton) entity;
			data = Integer.toString(skelly.getSkeletonType().ordinal());
		} else if (entity.getType() == EntityType.SHEEP) {
			Sheep sheep = (Sheep) entity;
			data = Byte.toString(sheep.getColor().getDyeData());
		} else if (entity.getType() == EntityType.OCELOT) {
			Ocelot ocelot = (Ocelot) entity;
			data = Integer.toString(ocelot.getCatType().getId());
			Player owner = (Player) ocelot.getOwner();
			if (owner != null) {
				owner_prefix = owner.getDisplayName() + "'s ";
			}
		} else if (entity.getType() == EntityType.WOLF) {
			Wolf wolf = (Wolf) entity;
			Player owner = (Player) wolf.getOwner();
			if (owner != null) {
				owner_prefix = owner.getDisplayName() + "'s ";
			}
		} else if (entity.getType() == EntityType.VILLAGER) {
			Villager villager = (Villager) entity;
			data = Integer.toString(villager.getProfession().getId());
		} else {
			data = "0";
		}
		
		if (newName != null) {
			namesConfig.set("entities." + typeId + ";" + data, newName);
			saveNamesConfig();
		}
		
		name = namesConfig.getString("entities." + typeId + ";" + data);
		if (name == null) {
			name = namesConfig.getString("entities." + typeId + ";0");
		}
		if (showData) {
			name = "(" + typeId + ":" + data + ") " + name;
		}
		if (name == null) {
			name = namesConfig.getString("entities.UNKNOWN");
		}
		return owner_prefix + name;
	}
	
	/**
	 * Retrieves the name of Item.
	 * 
	 * @param ItemStack item to identify
	 * @return String name of item
	 */
	public static String itemName(ItemStack item) {
		return itemName(item, false);
	}
	/**
	 * Retrieves the name of Item.
	 * 
	 * @param ItemStack item to identify
	 * @param Boolean show data values
	 * @return String name of item
	 */
	public static String itemName(ItemStack item, Boolean showData) {
		return itemName(item, showData, null);
	}
	/**
	 * Retrieves the name of Item.
	 * 
	 * @param ItemStack item to identify
	 * @param Boolean show data values
	 * @param String new name
	 * @return String name of item
	 */
	private static String itemName(ItemStack item, Boolean showData, String newName) {
		if (item == null) {
			return namesConfig.getString("items.UNKNOWN");
		}
		String typeId = "";
		String data = "";
		String name = null;
		String prefix = "";
		String suffix = "";
		typeId = Integer.toString(item.getTypeId());
		if (item.getType().getMaxDurability() > 0) {
			data = "0";
		} else if (item.getDurability() > 0) {
			data = Short.toString(item.getDurability());
		} else if (item.getData().getData() > 0) {
			data = Byte.toString(item.getData().getData());
		} else {
			data = "0";
		}
		
		if (typeId.equals("373")) {
			// Data on potions is a bit more complicated...
			short potionData = Short.parseShort(data);
			if (potionData == 0) {
				name = namesConfig.getString("potion-parts.absolute-zero");
			} else {
				boolean isAlchemyPotion = false;
				boolean alchemyPotionIsExtended = false;
				if (Bukkit.getPluginManager().isPluginEnabled("mcMMO") && ExperienceAPI.isValidSkillType("alchemy")) {
					AlchemyPotion alchemyPotion = PotionConfig.getInstance().getPotion(item.getDurability());
					if (alchemyPotion != null && !alchemyPotion.getEffects().isEmpty()) {
						Potion potion = Potion.fromItemStack(item);
						alchemyPotionIsExtended = potion.hasExtendedDuration();
						isAlchemyPotion = true;
					}
				}
				for (short bitPos = 14; bitPos > 5; bitPos--) {
					if (isAlchemyPotion && ((bitPos == 6 && !alchemyPotionIsExtended) || (bitPos > 6 && bitPos < 13) )) {
						continue;
					}
					short bitPow = (short) Math.pow(2, bitPos);
					if (potionData >= bitPow) {
						potionData -= bitPow;
						String tmpPrefix = namesConfig.getString("potion-parts.prefix-bit-" + bitPos);
						if (tmpPrefix != null) prefix += tmpPrefix; 
						String tmpSuffix = namesConfig.getString("potion-parts.suffix-bit-" + bitPos);
						if (tmpSuffix != null) suffix += tmpSuffix; 
					}
				}
				data = Short.toString(potionData);
			}
		}
		if (typeId.equals("397") && data.equals("3")) {
			String headOwner = getHeadOwner((ItemStack) item);
			if (headOwner != null) {
				name = namesConfig.getString("special.players-head").replace("%p", headOwner);
			}
		}
		
		if (newName != null) {
			namesConfig.set("items." + typeId + ";" + data, newName);
			saveNamesConfig();
		}
		
		if (name == null) {
			name = namesConfig.getString("items." + typeId + ";" + data);
		}
		if (name == null) {
			name = namesConfig.getString("items." + typeId + ";0");
		}
		if (name == null) {
			name = namesConfig.getString("items.UNKNOWN");
		}
		name = prefix + name + suffix;
		if (showData) {
			name = "(" + typeId + ":" + data + ") " + name;
		}
		name = name.replace("%d", data);
		return name;
	}
	/**
	 * Registers Vault permissions to local object.
	 * 
	 * @return boolean is Vault perms enabled
	 */
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

	/**
	 * Retrieves the owner of a head.
	 * 
	 * @param CraftItemStack head to identify
	 * @return String name of head owner
	 */
	public static String getHeadOwner(ItemStack item) {
		if (item == null) return null;
		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta == null) return null;
		if (itemMeta instanceof SkullMeta) {
			return ((SkullMeta)itemMeta).getOwner();
		}
		return null;
	}
}
