package me.onebone.economyproperty.error;

public class PropertyOverlapException extends PropertyException{
	private static final long serialVersionUID = 8284271319527423073L;
	
	private int with;
	
	public PropertyOverlapException(int with){
		this.with = with;
	}
	
	public int overlappingWith(){
		return this.with;
	}
}
