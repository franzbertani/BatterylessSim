package it.polimi.neslab.utils;

import java.util.Random;

import edu.clemson.eval.EvalLogger;
import it.polimi.neslab.utils.ResetsMemory;
import se.sics.mspsim.core.FramController;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.core.StopExecutionException;
import se.sics.mspsim.core.Memory.AccessMode;
import se.sics.mspsim.util.Utils;

/*
 * Handles cpu resets and randomly compute off time.
 */
public class ResetManager implements MSP430Constants{
    private ResetsMemory memory;
    private int maxOffTime;
    private double prob;
    private Random rand;
	private FramController fram; 
	private MSP430 cpu; 

    
    public ResetManager(int size, int maxOffTime, MSP430 cpu) {
    	this.prob = 0.6;
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
		fram.framWrite(memoryLocation,  memory.getNewest()*1000, AccessMode.WORD20);    	
    }
    
    public void persistOffTime(int memoryLocation, int value) {
    	if(fram == null) {
    		throw new UnsupportedOperationException("No fram controller, set it first.");
    	}
		fram.framWrite(memoryLocation,  value, AccessMode.WORD20);    	
    }
    
    @Override
    public String toString() {
    	return memory.toString();
    }

	public void stopExecution(String reason, int lifecycles) {
		throw new StopExecutionException(cpu.readRegister(15),
	              reason + " after " +
	              lifecycles + " lifecycles; " +
	              "R15=" + Utils.hex16(cpu.readRegister(15)) +
	              "; PC=" + Utils.hex16(cpu.readRegister(PC)) +
	              "; prev PC=" + Utils.hex16(cpu.getPreviousPC()) +
	              "; SP=" + Utils.hex16(cpu.readRegister(SP))
	              );
		
	}
    
}
