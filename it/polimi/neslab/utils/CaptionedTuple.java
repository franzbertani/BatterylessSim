package it.polimi.neslab.utils;

import se.sics.mspsim.util.Tuple;

public class CaptionedTuple extends Tuple {
	
	private String caption;
	
	public CaptionedTuple (double x, double y, String caption) {
		super(x,y);
		this.caption = caption;
	}
	
	public String getCaption() {
		return caption;
	}
	
	public String toString() {
		return super.toString() + ";" + caption;
	}

}
