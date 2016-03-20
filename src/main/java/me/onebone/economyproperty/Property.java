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

import java.util.ArrayList;
import java.util.List;

import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector2;

public class Property{
	private int id;
	
	private Vector2 start, end;
	private Level level;
	private String levelName;
	private double price;
	
	private List<Position> transaction;
	
	public Property(int id, Vector2 start, Vector2 end, Level level, String levelName, double price){
		this(id, start, end, level, levelName, price, new ArrayList<Position>());
	}
	
	public Property(int id, Vector2 start, Vector2 end, Level level, String levelName, double price, List<Position> transaction){
		this.id = id;
		
		start = start.floor();
		end = end.floor();
		
		if(start.x > end.x){
			double tmp = start.x;
			start.x = end.x;
			end.x = tmp;
		}
		
		if(start.y > end.y){
			double tmp = start.y;
			start.y = end.y;
			end.y = tmp;
		}
		
		this.start = start;
		this.end = end;
		this.level = level;
		this.levelName = levelName;
		
		this.price = price;
		
		this.transaction = transaction;
	}
	
	public boolean check(Position pos){
		return pos.level == this.level
				&& (this.start.x <= pos.x && pos.x <= this.end.x)
				&& (this.start.y <= pos.z && pos.z <= this.end.y);
	}
	
	public boolean checkTransaction(Position pos){
		for(Position position : transaction){
			if(pos.level == position.level && pos.equals(position)) return true;
		}
		return false;
	}
	
	public int getId(){
		return this.id;
	}
	
	public Vector2 getStart(){
		return this.start;
	}
	
	public Vector2 getEnd(){
		return this.end;
	}
	
	public Level getLevel(){
		return this.level;
	}
	
	public double getPrice(){
		return this.price;
	}
	
	public int getWidth(){
		return (int) ((Math.abs(Math.floor(end.x) - Math.floor(start.x)) + 1) * (Math.abs(Math.floor(end.y) - Math.floor(start.y)) + 1));
	}
	
	public String getLevelName(){
		return this.levelName;
	}
	
	public List<Position> getTransactions(){
		return this.transaction;
	}
}
