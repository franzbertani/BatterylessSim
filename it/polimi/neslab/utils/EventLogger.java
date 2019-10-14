package it.polimi.neslab.utils;

import java.util.ArrayList;


public class EventLogger {
	private ArrayList<CaptionedTuple> logValues;
	
	public EventLogger() {
		this.logValues = new ArrayList<CaptionedTuple>();
	}
	
	public ArrayList<CaptionedTuple> getLog(){
		return logValues;
	}
	
	public void addLog(double x, double y, String caption) {
		logValues.add(new CaptionedTuple(x, y, caption));
	}
	
}
