package com.flobi.WhatIsIt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.util.Vector;

public class WhatIsIt extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");
	private static FileConfiguration namesConfig = null;
	private static File namesConfigFile = null;
	private static boolean showDataValues = false;
	private static String pluginName;
	private static File dataFolder;
	private static InputStream defConfigStream;
	

	public void onEnable() {
		pluginName = getDescription().getName();
		dataFolder = getDataFolder();
		defConfigStream = getResource("names.yml");

		loadNamesConfig();
		showDataValues = namesConfig.getBoolean("config.display-data-values");

		log.info("[" + getDescription().getName() + "]" + namesConfig.getString("messages.HAS_BEEN_ENABLED"));
	}
	public void onDisable() { 
		log.info("[" + getDescription().getName() + "]" + namesConfig.getString("messages.HAS_BEEN_DISABLED"));
	}
	
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	Player player = null;
    	if (sender instanceof Player) {
    		player = (Player) sender;
    	} else {
			sender.sendMessage(namesConfig.getString("messages.CONSOLE_ERROR"));
			return true;
    	}
    	String newName = null;
     
    	if (
    		cmd.getName().equalsIgnoreCase("wis") ||
        	cmd.getName().equalsIgnoreCase("wit")
    	) {
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
	    			loadNamesConfig();
    			}
    		}
    	}
    	if (cmd.getName().equalsIgnoreCase("wis")) {
    		ItemStack heldItem = player.getItemInHand();
    		player.sendMessage(namesConfig.getString("messages.THIS_IS") + WhatIsIt.itemName(heldItem, showDataValues, newName));
    		Map<Enchantment, Integer> enchantments = heldItem.getEnchantments();
    		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
    			String message = 
    				namesConfig.getString("messages.ENCHANTED") + enchantmentName(enchantment, showDataValues, newName);
    			
   				player.sendMessage(message);
    		}				
    		return true;
    		
    	} else if (cmd.getName().equalsIgnoreCase("wit")) {
    		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
    			namesConfig = null;
    			loadNamesConfig();
    		}
    		Object targetedObject = getTarget(player);
    		player.sendMessage(namesConfig.getString("messages.THAT_IS") + WhatIsIt.name(targetedObject, showDataValues, newName));
    		return true;
    	}
    	return false;
    }
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
    private static void loadNamesConfig() {
		YamlConfiguration defConfig = null;
	    if (namesConfigFile == null) {
	    	namesConfigFile = new File(dataFolder, "names.yml");
	    }
	    namesConfig = YamlConfiguration.loadConfiguration(namesConfigFile);
	 
	    // Look for defaults in the jar
	    if (defConfigStream != null) {
	        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        namesConfig.setDefaults(defConfig);
	    }
	    if (!namesConfigFile.exists() && defConfig != null) {
	    	try {
	    		defConfig.save(namesConfigFile);
			} catch(IOException ex) {
				log.severe("[" + dataFolder.getName() + "]" + namesConfig.getString("messages.CANNOT_SAVE_NAMES_DEFAULT"));
			}
	    }
    }
    private static void saveNamesConfig() {
    	try {
    		namesConfig.save(namesConfigFile);
		} catch(IOException ex) {
			log.severe("[" + pluginName + "]" + namesConfig.getString("messages.CANNOT_SAVE_NAMES"));
		}
    }

    private static String name(Object whatToIdentify, Boolean showData) {
    	return name(whatToIdentify, false, null);
    }
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
		return namesConfig.getString("messages.UNKNOWN_OBJECT");
	}
	
	// ----- ENCHANTMENT NAMES -----
	public static String enchantmentName(Entry<Enchantment, Integer> enchantment) {
		return enchantmentName(enchantment, false);
	}
	public static String enchantmentName(Entry<Enchantment, Integer> enchantment, Boolean showData) {
		return enchantmentName(enchantment, showData, null);
	}
	private static String enchantmentName(Entry<Enchantment, Integer> enchantment, Boolean showData, String newName) {
		if (enchantment == null) {
			return namesConfig.getString("enchantments.UNKNOWN");
		}
		return enchantmentName(enchantment.getKey(), enchantment.getValue());
	}
	public static String enchantmentName(Enchantment enchantment, Integer level) {
		return enchantmentName(enchantment, level, false);
	}
	public static String enchantmentName(Enchantment enchantment, Integer level, Boolean showData) {
		return enchantmentName(enchantment, level, showData, null);
	}
	private static String enchantmentName(Enchantment enchantment, Integer level, Boolean showData, String newName) {
		if (enchantment == null) {
			return namesConfig.getString("enchantments.UNKNOWN");
		}
		return enchantmentName(enchantment) + 
			namesConfig.getString("messages.ENCHANTMENT_LEVEL") + 
			enchantmentLevelName(level);
	}
	public static String enchantmentName(Enchantment enchantment) {
		return enchantmentName(enchantment, false);
	}
	public static String enchantmentName(Enchantment enchantment, Boolean showData) {
		return enchantmentName(enchantment, showData, null);
	}
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
	public static String enchantmentLevelName(Integer level) {
		String name = namesConfig.getString("enchantmentlevels." + Integer.toString(level));
		if (name == null) {
			name = namesConfig.getString("enchantmentlevels.UNKNOWN");
		}
		return name;
	}
	
	// ----- BLOCK NAMES -----
	public static String blockName(Block block) {
		return blockName(block, false);
	}
	public static String blockName(Block block, Boolean showData) {
		return blockName(block, showData, null);
	}
	private static String blockName(Block block, Boolean showData, String newName) {
		if (block == null) {
			return namesConfig.getString("items.UNKNOWN");
		}
		ItemStack item = new ItemStack(block.getType(), 1, (short) 0, block.getData());
		return itemName(item, showData, newName);
	}
	
	// ----- PLAYER NAMES -----
	private static String playerName(Player player) {
		if (player == null) {
			return namesConfig.getString("entities.UNKNOWN");
		}
		return player.getName();
	}
	
	// ----- ENTITY NAMES -----
	public static String entityName(Entity entity) {
		return entityName(entity, false);
	}
	public static String entityName(Entity entity, Boolean showData) {
		return entityName(entity, showData, null);
	}
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
		
		if (entity.getType() == EntityType.SHEEP) {
			Sheep sheep = (Sheep) entity;
			data = Byte.toString(sheep.getColor().getData());
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
		
		log.info("Entity: " + typeId + ";" + data);
		
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
	
	// ----- ITEM NAMES -----
	public static String itemName(ItemStack item) {
		return itemName(item, false);
	}
	public static String itemName(ItemStack item, Boolean showData) {
		return itemName(item, showData, null);
	}
	private static String itemName(ItemStack item, Boolean showData, String newName) {
		if (item == null) {
			return namesConfig.getString("items.UNKNOWN");
		}
		String typeId = "";
		String data = "";
		String name = "";
		typeId = Integer.toString(item.getTypeId());
		if (item.getDurability() > 0) {
			data = Short.toString(item.getDurability());
			log.info("Item damage: " + data);
		} else if (item.getData().getData() > 0) {
			data = Byte.toString(item.getData().getData());
		} else {
			data = "0";
		}
		
//		log.info("Item: " + typeId + ";" + data);
		
		if (newName != null) {
			namesConfig.set("items." + typeId + ";" + data, newName);
			saveNamesConfig();
		}
		
		name = namesConfig.getString("items." + typeId + ";" + data);
		if (name == null) {
			name = namesConfig.getString("items." + typeId + ";0");
		}
		if (showData) {
			name = "(" + typeId + ":" + data + ") " + name;
		}
		if (name == null) {
			name = namesConfig.getString("items.UNKNOWN");
		}
		return name;
	}

}
