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

import java.util.HashMap;

import cn.nukkit.Player;
import cn.nukkit.level.Position;

public class PlayerManager{
	private HashMap<Player, Position> lastPosition;
	private HashMap<Player, Long> lastShown;
	private HashMap<Player, Integer> lastProperty;
	
	public PlayerManager(){
		lastPosition = new HashMap<>();
		lastShown = new HashMap<>();
		lastProperty = new HashMap<>();
	}
	
	public boolean isMoved(Player player){
		if(!lastPosition.containsKey(player)){
			lastPosition.put(player, new Position(player.x, player.y, player.z, player.level));
			return true;
		}
		
		Position now = player.floor();
		Position last = lastPosition.get(player).floor();
		return !(last.x == now.x && last.z == now.z);
	}
	
	public void setPosition(Player player){
		lastPosition.put(player, new Position(player.x, player.y, player.z, player.level));
	}
	
	public Position getLastPosition(Player player){
		if(!lastPosition.containsKey(player)){
			lastPosition.put(player, new Position(player.x, player.y, player.z, player.level));
		}
		
		return lastPosition.get(player);
	}
	
	public void setShown(Player player){
		this.lastShown.put(player, System.currentTimeMillis());
	}
	
	public boolean canShow(Player player){
		if(!lastShown.containsKey(player)){
			return true;
		}
		
		return System.currentTimeMillis() - lastShown.get(player) > 2000;
	}
	
	public void setLastProperty(Player player, int id){
		this.lastProperty.put(player, id);
	}
	
	public boolean isMovedProperty(Player player, int id){
		if(!this.lastProperty.containsKey(player)){
			return true;
		}
		
		return this.lastProperty.get(player) != id;
	}
}
