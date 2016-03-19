package me.onebone.economyproperty;

/*
 * EconomyProperty: A plugin which allows your server to manage properties
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.onebone.economyapi.EconomyAPI;
import me.onebone.economyland.EconomyLand;
import me.onebone.economyland.error.LandCountMaximumException;
import me.onebone.economyland.error.LandOverlapException;
import me.onebone.economyproperty.provider.*;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.TranslationContainer;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector2;
import cn.nukkit.network.protocol.UpdateBlockPacket;
import cn.nukkit.network.protocol.UpdateBlockPacket.Entry;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;

public class EconomyProperty extends PluginBase implements Listener{
	private Map<Player, Position[]> positions;
	private Map<Player, Position[]> showing;
	
	private Map<Player, Object[]> creationMode;
	private List<Player> placeQueue;
	
	private Provider provider;
	private EconomyAPI api;
	private EconomyLand land;
	
	private PlayerManager manager;
	
	private Map<String, String> lang;
	
	public String getMessage(String key){
		return this.getMessage(key, new String[]{});
	}
	
	public String getMessage(String key, Object[] params){
		if(this.lang.containsKey(key)){
			return replaceMessage(this.lang.get(key), params);
		}
		return "Could not find message with " + key;
	}
	
	private String replaceMessage(String lang, Object[] params){
		StringBuilder builder = new StringBuilder();
		
		for(int i = 0; i < lang.length(); i++){
			char c = lang.charAt(i);
			if(c == '{'){
				int index;
				if((index = lang.indexOf('}', i)) != -1){
					try{
						String p = lang.substring(i + 1, index);
						if(p.equals("M")){
							i = index;
							
							builder.append(api.getMonetaryUnit());
							continue;
						}
						int param = Integer.parseInt(p);
						
						if(params.length > param){
							i = index;
							
							builder.append(params[param]);
							continue;
						}
					}catch(NumberFormatException e){}
				}
			}else if(c == '&'){
				char color = lang.charAt(++i);
				if((color >= '0' && color <= 'f') || color == 'r' || color == 'l' || color == 'o'){
					builder.append(TextFormat.ESCAPE);
					builder.append(color);
					continue;
				}
			}
			
			builder.append(c);
		}
		
		return builder.toString();
	}
	
	@Override
	public void onEnable(){
		positions = new HashMap<>();
		showing = new HashMap<>();
		
		creationMode = new HashMap<>();
		placeQueue = new LinkedList<>();
		
		this.saveDefaultConfig();
		
		this.api = EconomyAPI.getInstance();
		this.land = (EconomyLand) this.getServer().getPluginManager().getPlugin("EconomyLand");
		
		InputStream is = this.getResource("lang_" + this.getConfig().get("langauge", "eng") + ".json");
		if(is == null){
			this.getLogger().critical("Could not load language file. Changing to default.");
			
			is = this.getResource("lang_eng.json");
		}
		
		try{
			lang = new GsonBuilder().create().fromJson(Utils.readFile(is), new TypeToken<LinkedHashMap<String, String>>(){}.getType());
		}catch(JsonSyntaxException | IOException e){
			this.getLogger().critical(e.getMessage());
		}
		
		this.provider = new YamlProvider(this, new File(this.getDataFolder(), "Property.yml"));
		
		this.manager = new PlayerManager();
		
		this.getServer().getScheduler().scheduleDelayedRepeatingTask(new ShowBlockTask(this), 20, 20);
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable(){
		if(this.provider != null){
			this.provider.close();
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(command.getName().equals("property")){
			if(args.length < 1){
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
				return true;
			}
			
			args[0] = args[0].toLowerCase();
			
			if(args[0].equals("pos1")){
				if(!(sender instanceof Player)){
					sender.sendMessage(new TranslationContainer("commands.generic.ingame"));
					return true;
				}
				
				Player player = (Player) sender;
				if(!player.hasPermission("economyproperty.command.property.pos1")){
					player.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
					return true;
				}
				
				Position pos = player.floor();
				
				if(positions.containsKey(player.getName().toLowerCase())){
					positions.get(player)[0] = pos;
				}else{
					positions.put(player, new Position[]{
						pos, null
					});
				}
				
				sender.sendMessage(this.getMessage("pos1-set", new Object[]{
						(int) pos.x + ":" + (int) pos.y + ":" + (int) pos.z
				}));
			}else if(args[0].equals("pos2")){
				if(!(sender instanceof Player)){
					sender.sendMessage(new TranslationContainer("commands.generic.ingame"));
					return true;
				}
				
				Player player = (Player) sender;
				if(!player.hasPermission("economyproperty.command.property.pos2")){
					player.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
					return true;
				}
				
				Position pos = player.floor();
				
				if(!positions.containsKey(player)){
					sender.sendMessage(this.getMessage("pos1-not-set"));
					return true;
				}
				positions.get(player)[1] = pos;
				
				sender.sendMessage(this.getMessage("pos2-set", new Object[]{
						(int) pos.x + ":" + (int) pos.y + ":" + (int) pos.z
				}));
			}else if(args[0].equals("make")){
				if(!(sender instanceof Player)){
					sender.sendMessage(new TranslationContainer("commands.generic.ingame"));
					return true;
				}
				
				if(!sender.hasPermission("economyproperty.command.property.make")){
					sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
					return true;
				}
				
				if(args.length < 2){
					sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
					return true;
				}
				
				double price;
				try{
					price = Double.parseDouble(args[1]);
				}catch(NumberFormatException e){
					sender.sendMessage(this.getMessage("provide-number"));
					return true;
				}
				
				Player player = (Player) sender;
				
				if(!positions.containsKey(player)){
					sender.sendMessage(this.getMessage("pos-not-set"));
					return true;
				}
				
				Position[] pos = positions.get(player);
				if(pos[0] == null || pos[1] == null){
					sender.sendMessage(this.getMessage("pos-not-set"));
					return true;
				}
				
				Property property;
				if((property = this.provider.checkOverlap(pos[0], pos[1])) != null){
					sender.sendMessage(this.getMessage("property-overlap", new Object[]{property.getId()}));
					return true;
				}
				removeBlocks(player, true);
				
				int id = this.provider.addProperty(new Vector2(pos[0].x, pos[0].z), new Vector2(pos[1].x, pos[1].z), pos[0].level, price);
				
				sender.sendMessage(this.getMessage("property-created", new Object[]{id}));
			}else if(args[0].equals("create")){
				if(!(sender instanceof Player)){
					sender.sendMessage(new TranslationContainer("commands.generic.ingame"));
					return true;
				}
				
				Player player = (Player) sender;
				if(!player.hasPermission("economyproperty.command.property.create")){
					player.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
					return true;
				}
				
				if(this.creationMode.containsKey(player)){
					this.creationMode.remove(player);
					
					sender.sendMessage(this.getMessage("exit-creation-mode"));
				}else{
					if(args.length < 4){
						sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
						return true;
					}
					
					int x, z;
					double price;
					try{
						x = Integer.parseInt(args[1]);
						z = Integer.parseInt(args[2]);
						
						price = Double.parseDouble(args[3]);
					}catch(NumberFormatException e){
						sender.sendMessage(this.getMessage("provide-number-range"));
						return true;
					}
					
					this.creationMode.put(player, new Object[]{x, z, price, null});
					
					sender.sendMessage(this.getMessage("enter-creation-mode"));
				}
			}else if(args[0].equals("buy")){
				if(!(sender instanceof Player)){
					sender.sendMessage(new TranslationContainer("commands.generic.ingame"));
					return true;
				}
				
				Player player = (Player) sender;
				if(!player.hasPermission("economyproperty.command.property.buy")){
					player.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
					return true;
				}
				
				Property property;
				if((property = this.provider.findProperty(player)) != null){
					Position start = new Position(property.getStart().x, 0, property.getStart().y, property.getLevel());
					Position end = new Position(property.getEnd().x, 0, property.getEnd().y, property.getLevel());
					
					try{
						if(this.api.myMoney(player) < property.getPrice()){
							player.sendMessage(this.getMessage("no-money", new Object[]{property.getId(), property.getPrice()}));
							return true;
						}
						
						this.api.reduceMoney(player, property.getPrice(), true);
						this.land.addLand(start, end, player.level, player.getName());
						
						this.provider.removeProperty(property.getId());
						
						player.sendMessage(this.getMessage("bought-property", new Object[]{property.getId()}));
					}catch(LandOverlapException e){
						player.sendMessage(this.getMessage("land-overlap", new Object[]{e.overlappingWith().getId()}));
					}catch(LandCountMaximumException e){
						player.sendMessage(this.getMessage("land-maximum"));
					}
				}
			}else{
				sender.sendMessage(TextFormat.RED + "Usage: " + command.getUsage());
			}
			return true;
		}
		return false;
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event){
		Block block = event.getBlock();
		Item item = event.getItem();
		Player player = event.getPlayer();
		
		if(this.creationMode.containsKey(player)){
			block = block.getSide(event.getFace());
			
			Object[] data = this.creationMode.get(player);
			if(data[3] != null){
				Position[] pos = this.positions.get(player);
				
				if(pos == null || pos[0] == null || pos[1] == null){
					this.creationMode.get(player)[3] = null;
				}else{
					Vector2 vec = (Vector2) data[3];
					
					if(vec.x == block.x && vec.y == block.z){
						Property property = this.provider.checkOverlap(pos[0], pos[1]);
						if(property == null){
							int id = this.provider.addProperty(new Vector2(pos[0].x, pos[0].z), new Vector2(pos[1].x, pos[1].z), pos[0].level, (double) data[2]);
							
							player.sendMessage(this.getMessage("property-created", new Object[]{id}));
							
							this.creationMode.get(player)[3] = null;
						}else{
							player.sendMessage(this.getMessage("property-overlap", new Object[]{property.getId()}));
						}
						
						removeBlocks(player, true);
					}else{
						player.sendMessage(this.getMessage("creation-cancelled"));
						removeBlocks(player, true);
					}
					return;
				}
			}
			
			removeBlocks(player, true);
			
			int[] range = new int[]{(Integer) data[0], (Integer) data[1]};
			
			Position end;
			if(player.yaw >= 0 && player.yaw < 90){ // 0 <= yaw < 90
				end = new Position(block.x - range[0], block.y, block.z + range[1]);	
			}else if(player.yaw >= 90 && player.yaw < 180){ // 90 <= yaw < 180
				end = new Position(block.x - range[0], block.y, block.z - range[1]);	
			}else if(player.yaw >= 180 && player.yaw < 270){ // 180 <= yaw < 270
				end = new Position(block.x + range[0], block.y, block.z - range[1]);	
			}else{ // 270 < yaw
				end = new Position(block.x + range[0], block.y, block.z + range[1]);
			}
			
			end.level = block.level;
			positions.put(player, new Position[]{
				new Position(block.x, block.y, block.z, block.level),
				end
			});
			
			this.creationMode.get(player)[3] = new Vector2(block.x, block.z);
			
			return;
		}
		
		if(item.canBePlaced() && !block.canBeActivated() && event.getAction() == PlayerInteractEvent.RIGHT_CLICK_BLOCK){ // placing
			block = block.getSide(event.getFace());
		}
		
		Map<Integer, Property> properties = this.provider.getAll();
		
		for(Property property : properties.values()){
			if(property.checkTransaction(event.getBlock())){
				if(this.api.myMoney(player) < property.getPrice()){
					player.sendMessage(this.getMessage("no-money", new Object[]{property.getId(), property.getPrice()}));
				}else{
					this.api.reduceMoney(player, property.getPrice(), true);
					
					Position start = new Position(property.getStart().x, 0, property.getStart().y, property.getLevel());
					Position end = new Position(property.getEnd().x, 0, property.getEnd().y, property.getLevel());
					
					try{
						this.land.addLand(start, end, property.getLevel(), player.getName());
						
						this.provider.removeProperty(property.getId());
					}catch(LandOverlapException e){
						player.sendMessage(this.getMessage("land-overlap", new Object[]{e.overlappingWith().getId()}));
					}catch(LandCountMaximumException e){
						player.sendMessage(this.getMessage("land-maximum"));
					}
				}
				if(event.getAction() != PlayerInteractEvent.RIGHT_CLICK_BLOCK){
					event.setCancelled();
				}
			}
			
			if(property.check(event.getBlock())){
				if(!player.hasPermission("economyproperty.admin.modify")){
					player.sendMessage(this.getMessage("no-permission-modify", new Object[]{property.getId()}));
					
					event.setCancelled();
					
					// RIGHT_CLICK_BLOCK
					if(event.getAction() == PlayerInteractEvent.RIGHT_CLICK_BLOCK && !block.canBeActivated() && event.getItem().canBePlaced()){
						this.placeQueue.add(player);
					}
				}
				
				return;
			}
		}
	}
	
	@EventHandler (ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event){
		Block block = event.getBlock();
		
		Property property = this.provider.findProperty(block);
		if(property != null){
			Player player = event.getPlayer();
			
			if(!player.hasPermission("economyproperty.admin.modify")){
				player.sendMessage(this.getMessage("no-permission-modify", new Object[]{property.getId()}));
				
				event.setCancelled();
			}
		}
	}
	
	@EventHandler
	public void onPlace(BlockPlaceEvent event){
		if(placeQueue.contains(event.getPlayer())){
			event.setCancelled();
			
			placeQueue.remove(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event){
		Player player = event.getPlayer();
		
		if(this.manager.isMoved(player)){
			Property property;
			if((property = this.provider.findProperty(player)) != null){
				if(this.manager.isMovedProperty(player, property.getId())){
					player.sendMessage(this.getMessage("property-available", new Object[]{property.getId(), property.getPrice()}));
					
					this.manager.setLastProperty(player, property.getId());
				}
			}else{
				this.manager.setLastProperty(player, -1);
			}
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event){
		Player player = event.getPlayer();
		
		if(placeQueue.contains(player)){
			placeQueue.remove(player);
		}
		
		if(positions.containsKey(player)){
			positions.remove(player);
		}
		
		if(showing.containsKey(player)){
			showing.remove(player);
		}
	}
	
	public void removeBlocks(Player player){
		if(!showing.containsKey(player)){
			return;
		}
		Position[] pos = showing.get(player);
		
		Entry[] entries = new Entry[pos.length];
		
		UpdateBlockPacket pk = new UpdateBlockPacket();
		for(int i = 0; i < pos.length; i++){
			Position p = pos[i];
			
			entries[i] = new Entry((int) p.x, (int) p.z, (int) p.y, 
					player.level.getBlockIdAt((int) p.x, (int) p.y, (int) p.z),
					player.level.getBlockDataAt((int) p.x, (int) p.y, (int) p.z),
					UpdateBlockPacket.FLAG_ALL);
		}
		
		pk.records = entries;
		player.dataPacket(pk);
	}
	
	public void removeBlocks(Player player, boolean removePosition){
		if(removePosition){
			positions.remove(player);
		}
		
		removeBlocks(player);
	}
	
	public void showBlocks(boolean show){
		UpdateBlockPacket pk = new UpdateBlockPacket();
		
		for(Player player : positions.keySet()){
			Position[] pos = positions.get(player);
			
			if(player == null) return; // If player is not in server
			
			Position pos1 = pos[0];
			Position pos2 = pos[1];
			
			Entry[] entries = new Entry[1];
			if(pos2 != null){
				entries = new UpdateBlockPacket.Entry[4];
			}
			
			if(pos1 != null){
				if(pos1.level == player.level){
					entries[0] = new Entry((int) pos1.x, (int) pos1.z, (int) pos1.y, 
							show ? Block.GLASS : player.level.getBlockIdAt((int) pos1.x, (int) pos1.y, (int) pos1.z),
							player.level.getBlockDataAt((int) pos1.x, (int) pos1.y, (int) pos1.z), UpdateBlockPacket.FLAG_ALL);
					
					if(pos2 != null){
						entries[1] = new Entry((int) pos2.x, (int) pos2.z, (int) pos2.y, 
								show ? Block.GLASS : player.level.getBlockIdAt((int) pos2.x, (int) pos2.y, (int) pos2.z),
										player.level.getBlockDataAt((int) pos2.x, (int) pos2.y, (int) pos2.z), UpdateBlockPacket.FLAG_ALL);
						entries[2] = new Entry((int) pos1.x, (int) pos2.z, (int) pos1.y, 
								show ? Block.GLASS : player.level.getBlockIdAt((int) pos1.x, (int) pos1.y, (int) pos2.z),
										player.level.getBlockDataAt((int) pos1.x, (int) pos1.y, (int) pos2.z), UpdateBlockPacket.FLAG_ALL);
						entries[3] = new Entry((int) pos2.x, (int) pos1.z, (int) pos1.y, 
								show ? Block.GLASS : player.level.getBlockIdAt((int) pos2.x, (int) pos2.y, (int) pos1.z),
										player.level.getBlockDataAt((int) pos2.x, (int) pos2.y, (int) pos1.z), UpdateBlockPacket.FLAG_ALL);
					}
					pk.records = entries;
				}
				
				player.dataPacket(pk);
				
				if(show){
					Position[] shown = new Position[entries.length];
					
					for(int i = 0; i < entries.length; i++){
						Entry entry = entries[i];
						shown[i] = new Position(entry.x, entry.y, entry.z, player.level);
					}
					
					showing.put(player, shown);
				}else{
					showing.remove(player);
				}
			}
		}
	}
}
