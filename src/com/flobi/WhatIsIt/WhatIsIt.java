package com.flobi.WhatIsIt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

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
import org.bukkit.util.Vector;

public class WhatIsIt extends JavaPlugin {
	private static final Logger log = Logger.getLogger("Minecraft");
	private static FileConfiguration namesConfig = null;
	

	public void onEnable() {
		loadNamesConfig();
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
     
    	if (cmd.getName().equalsIgnoreCase("wis")) {
    		ItemStack heldItem = player.getItemInHand();
    		player.sendMessage(namesConfig.getString("messages.THIS_IS") + WhatIsIt.itemName(heldItem));
    		Map<Enchantment, Integer> enchantments = heldItem.getEnchantments();
    		for (Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
    			String message = 
    				namesConfig.getString("messages.ENCHANTED") + enchantmentName(enchantment);
    			
   				player.sendMessage(message);
    		}				
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("wit")) {
    		Object targetedObject = getTarget(player);
    		player.sendMessage(namesConfig.getString("messages.THAT_IS") + WhatIsIt.name(targetedObject));
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
    private void loadNamesConfig() {
		File namesConfigFile = null;
		YamlConfiguration defConfig = null;
	    if (namesConfigFile == null) {
	    	namesConfigFile = new File(getDataFolder(), "names.yml");
	    }
	    namesConfig = YamlConfiguration.loadConfiguration(namesConfigFile);
	 
	    // Look for defaults in the jar
	    InputStream defConfigStream = getResource("names.yml");
	    if (defConfigStream != null) {
	        defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        namesConfig.setDefaults(defConfig);
	    }
	    if (!namesConfigFile.exists() && defConfig != null) {
	    	try {
	    		defConfig.save(namesConfigFile);
			} catch(IOException ex) {
				log.severe("[" + getDescription().getName() + "]" + namesConfig.getString("messages.CANNOT_SAVE_NAMES_DEFAULT"));
			}
	    	
	    }
    }

	private static String name(Object whatToIdentify) {
		if (whatToIdentify == null) {
			return "nothing";
		}
		if (whatToIdentify instanceof Entity) {
			return entityName((Entity) whatToIdentify);
		}
		if (whatToIdentify instanceof ItemStack) {
			return itemName((ItemStack) whatToIdentify);
		}
		if (whatToIdentify instanceof Block) {
			return blockName((Block) whatToIdentify);
		}
		if (whatToIdentify instanceof Server) {
			return "console";
		}
		if (whatToIdentify instanceof ItemStack) {
			return itemName((ItemStack) whatToIdentify);
		}
		return namesConfig.getString("messages.UNKNOWN_OBJECT");
	}
	public static String enchantmentName(Entry<Enchantment, Integer> enchantment) {
		return enchantmentName(enchantment.getKey(), enchantment.getValue());
	}
	public static String enchantmentName(Enchantment enchantment, Integer level) {
		return enchantmentName(enchantment) + 
			namesConfig.getString("messages.ENCHANTMENT_LEVEL") + 
			enchantmentLevelName(level);
	}
	public static String enchantmentName(Enchantment enchantment) {
		String name = namesConfig.getString("enchantments." + Integer.toString(enchantment.getId()));
		if (name == null) {
			name = namesConfig.getString("enchantments.UNKNOWN");
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
	public static String blockName(Block block) {
		ItemStack item = new ItemStack(block.getType(), 1, (short) 0, block.getData());
		return itemName(item);
	}
	public static String playerName(Player player) {
		return player.getName();
	}
	public static String entityName(Entity entity) {
		if (entity.getType() == EntityType.SPLASH_POTION) {
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
		
		name = namesConfig.getString("entities." + typeId + ";" + data);
		if (name == null) {
			name = namesConfig.getString("entities." + typeId + ";0");
		}
		if (name == null) {
			return "unknown entity";
		} else {
			return owner_prefix + name;
		}
	}
	public static String itemName(ItemStack item) {
		String typeId = "";
		String data = "";
		String name = "";
		typeId = Integer.toString(item.getTypeId());
		if (item.getData().getData() > 0) {
			data = Byte.toString(item.getData().getData());
		} else if (item.getDurability() > 0) {
			data = Short.toString(item.getDurability());
		} else {
			data = "0";
		}
		
		log.info("Item: " + typeId + ";" + data);
		
		name = namesConfig.getString("items." + typeId + ";" + data);
		if (name == null) {
			name = namesConfig.getString("items." + typeId + ";0");
		}
		if (name == null) {
			return "unknown item";
		} else {
			return name;
		}
	}

}
