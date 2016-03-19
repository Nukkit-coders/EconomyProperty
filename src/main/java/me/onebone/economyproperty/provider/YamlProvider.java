package me.onebone.economyproperty.provider;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.onebone.economyproperty.EconomyProperty;
import me.onebone.economyproperty.Property;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector2;
import cn.nukkit.utils.Utils;

public class YamlProvider implements Provider{
	private EconomyProperty plugin;
	private File file;
	private Map<Integer, Property> properties;
	
	private int propertyId;
	
	public YamlProvider(EconomyProperty plugin, File file){
		properties = new HashMap<>();
		
		this.plugin = plugin;
		this.file = file;
		
		try{
			File dataFile = new File(plugin.getDataFolder(), "PropertyData.json");
			if(dataFile.exists()){
				Map<String, Object> data = new GsonBuilder().create().fromJson(Utils.readFile(dataFile), new TypeToken<LinkedHashMap<String, Object>>(){}.getType());
				this.propertyId = (int) Double.parseDouble(data.getOrDefault("propertyId", 0).toString());
			}
		}catch(JsonSyntaxException | IOException e){
			plugin.getLogger().critical(e.getMessage());
		}
		
		Yaml yaml = new Yaml();
		
		HashMap<String, Level> tmp = new HashMap<String, Level>();
		try {
			@SuppressWarnings("unchecked")
			Map<Integer, LinkedHashMap<String, Object>> load = ((Map<Integer, LinkedHashMap<String, Object>>)yaml.load(Utils.readFile(file)));

			load.forEach((k, v) -> {
				if(!tmp.containsKey((String) v.get("level"))){
					Level level = plugin.getServer().getLevelByName((String) v.get("level"));
					tmp.put((String) v.get("level"), level);
				}
				
				properties.put(k, new Property(k, new Vector2((int) v.get("startX"), (int) v.get("startZ")), new Vector2((int) v.get("endX"), (int) v.get("endZ")), tmp.get((String) v.get("level")), (String) v.get("level"), Double.parseDouble(v.get("price").toString())));
			});
		}catch(FileNotFoundException e){
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@Override
	public int addProperty(Vector2 start, Vector2 end, Level level, double price){
		this.properties.put(this.propertyId, new Property(this.propertyId, start, end, level, level.getFolderName(), price));
		
		return this.propertyId++;
	}

	@Override
	public Property getProperty(int id){
		if(this.properties.containsKey(id)){
			return this.properties.get(id);
		}
		return null;
	}

	@Override
	public Property findProperty(Position pos){
		for(int id : this.properties.keySet()){
			Property property = this.properties.get(id);
			
			if(property.check(pos)) return property;
		}
		
		return null;
	}
	
	@Override
	public boolean removeProperty(int id){
		if(this.properties.containsKey(id)){
			this.properties.remove(id);
			return true;
		}
		return false;
	}
	
	@Override
	public Property checkOverlap(Position start, Position end){
		for(int id : this.properties.keySet()){
			Property property = this.properties.get(id);
			
			if(property.check(start) || property.check(end)) return property;
		}
		
		return null;
	}
	
	@Override
	public Property checkTransaction(Position pos){
		for(int id : this.properties.keySet()){
			Property property = this.properties.get(id);
			
			if(property.checkTransaction(pos)) return property;
		}
		
		return null;
	}
	
	@Override
	public Map<Integer, Property> getAll(){
		return new HashMap<Integer, Property>(this.properties);
	}
	
	@SuppressWarnings("serial")
	@Override
	public void save(){
		HashMap<Integer, LinkedHashMap<String, Object>> saves = new LinkedHashMap<Integer, LinkedHashMap<String, Object>>();
		properties.values().forEach((property) -> {
			saves.put(property.getId(), new LinkedHashMap<String, Object>(){
				{
					Vector2 start = property.getStart();
					put("startX", (int) start.x);
					put("startZ", (int) start.y);
					
					Vector2 end = property.getEnd();
					put("endX", (int) end.x);
					put("endZ", (int) end.y);
					
					put("level", property.getLevelName());
					
					put("price", property.getPrice());
					
					put("transaction", property.getTransactions());
				}
			});
		});
		
		try{
			DumperOptions option = new DumperOptions();
			option.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			
			Yaml yaml = new Yaml(option);
			Utils.writeFile(file, yaml.dump(saves));
			
			Map<String, Object> map = new HashMap<>();
			map.put("propertyId", this.propertyId);
			
			String content = new GsonBuilder().create().toJson(map);

			Utils.writeFile(new File(plugin.getDataFolder(), "PropertyData.json"), content);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void close(){
		this.save();
	}

}
