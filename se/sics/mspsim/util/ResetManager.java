package se.sics.mspsim.util;

import java.util.Random;

import se.sics.mspsim.util.ResetsMemory;
import se.sics.mspsim.core.FramController;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.Memory.AccessMode;

public class ResetManager{
    private ResetsMemory memory;
    private int maxOffTime;
    private double prob;
    private Random rand;
	private FramController fram; 
	private MSP430 cpu; 

    
    public ResetManager(int size, int maxOffTime, MSP430 cpu) {
    	this.prob = 0.2;
    	this.memory = new ResetsMemory(size);
    	this.maxOffTime = maxOffTime;
    	this.rand = new Random();
    	this.cpu = cpu;

    }
    
    public void setFramController(FramController fram) {
    	this.fram = fram;
    }
    
    public int computeOffTime() {
    	double seed = rand.nextDouble();
    	int geo = (int) Math.abs(Math.floor( Math.log( (seed/prob)*(1.0-prob) ) / Math.log(1.0-prob) ));
    	int offTime = Math.min(geo, maxOffTime);
    	memory.put(offTime);
    	return offTime;
    }
    
    public void performReset() {
    	cpu.reset();
    }
    
    public void persistOffTime(int memoryLocation) {
    	if(fram == null) {
    		throw new UnsupportedOperationException("No fram controller, set it first.");
    	}
		fram.framWrite(memoryLocation,  memory.getNewest(), AccessMode.WORD20);    	
    }
    
    @Override
    public String toString() {
    	return memory.toString();
    }
    
}
