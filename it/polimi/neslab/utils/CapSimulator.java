package it.polimi.neslab.utils;
import java.lang.Math;

import se.sics.mspsim.config.MSP430f2132Config;
import se.sics.mspsim.config.MSP430fr6989Config;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.util.ComponentRegistry;

public class CapSimulator {
    private double capacitance;
    private double voltage;
    private double maxVoltage;
    private double onThreshold;
    private SimpleFairy energyFairy;
    private int millisecFraction;
    private double vOff;
	private ResetManager resetManager;
	private int memoryLocation;
	private int frequency;
	private static double energyPerCC = 1e-9;
	private static double voltageComparatorResolution = 0.001;

    private static final double R_ESR = 30000;

    public CapSimulator (double capacitance, double initVoltage){
        this.capacitance = capacitance;
        this.voltage = initVoltage;
        this.millisecFraction = 0;
        this.vOff = 1.8;
    }
    
    public void setMemoryLocation (int memoryLocation) {
    	this.memoryLocation = memoryLocation;
    }

    public void setEnergyFairy (SimpleFairy ef) {
        this.energyFairy = ef;
    }
    
    public void setResetManager (ResetManager resetManager) {
    	this.resetManager = resetManager;
    }
    
    public void setOnThreshold (double threshold) {
    	this.onThreshold = threshold;
    }
    
    private double resistanceActive () {
        return (4010.6 * voltage) + 803.53;
    }
    
    private double resistanceOff() {
    	// Resistance is only the comparator, so voltage / 1.5uA
    	return voltage / 0.0000015;
    }
    
    private void consumeEnergyAndUpdateV(double energy) {
    	double newEnergy = (0.5*capacitance*voltage*voltage) - energy;
    	voltage = Math.max(Math.sqrt(2*newEnergy/capacitance), energyFairy.peekVoltage());
    }
    
    private double getOffEnergy() {
    	return 0.5*capacitance*vOff*vOff;
    }
    
    public double getVoltage () { return voltage; }
    
    private int getMicrosecToVOn() {
    	double time = 0;
    	double timeFraction = 0.001 - millisecFraction/1000000;
    	double vSupply = energyFairy.peekVoltage();
    	double resistance;
    	
    	
    	while (voltage <= onThreshold) {
    		if(vSupply < voltage) {
    			resistance = resistanceOff();
    	    	voltage = Math.max( voltage*Math.exp(-(timeFraction)/(resistance*capacitance)), vSupply);
    		} else {
    	    	voltage = vSupply + (voltage - vSupply)*Math.exp(-(timeFraction)/(R_ESR*capacitance));
    		}
    		//System.out.println( "vsupply: " + vSupply +  " vcurrent: " + voltage + " time fraction: " + timeFraction );
	    	millisecFraction = 0;
	    	time += (timeFraction * 1000000);
	    	timeFraction = 0.001 - millisecFraction/1000000;
	    	energyFairy.stepTrace();
	    	vSupply = energyFairy.peekVoltage();
    	}
    	return (int) (time);
    }
    
    public void checkIfPowersOffDuringExecution (int microseconds) {
    	double vSupply = energyFairy.peekVoltage();
		int timeLeft = microseconds;
		double resistance;
		int execCC;
		double spentEnergy;
		
    	while (timeLeft > 0) {
    		
    		if(millisecFraction == 1000) {
    			millisecFraction = 0;
    			energyFairy.stepTrace();
    			vSupply = energyFairy.peekVoltage();
    		}

    		resistance = resistanceActive();

    		if(vSupply < voltage) { // MI STO SCARICANDO
    			execCC = (int) Math.floor(1e-6 * frequency);
    			spentEnergy = energyPerCC * execCC;
    			consumeEnergyAndUpdateV(spentEnergy);
    			
    			if(voltage <= vOff) { //MI SONO SPENTO
    				resetManager.performReset();
    				int offTime = getMicrosecToVOn();
    				//resetManager.persistOffTime(memoryLocation, offTime);
    				System.out.println("offTime = " + offTime);	
    				return;
    			}
    			
    		} else { // MI STO CARICANDO
    			voltage = vSupply + (voltage - vSupply)*Math.exp(-(1e-6)/(resistance*capacitance)); //CARICA PER UN MICROSECONDO   			
    		} 
    		
    		millisecFraction += 1;
    		timeLeft -= 1;
    	}	    	
    }
    
    
    public static void main(String[] args) {
    	MSP430 msp = new MSP430(0, new ComponentRegistry(),
                new MSP430fr6989Config());
    	ResetManager rm = new ResetManager(10, 70000000, msp);
    	CapSimulator c = new CapSimulator(4.7e-5, 0);
    	c.setResetManager(rm);
    	c.frequency = msp.getDCOFrequency();
    	c.setOnThreshold(3.3);
    	SimpleFairy sf = new SimpleFairy("/Users/francesco/git/BatterylessSim/traces/7.txt");
    	c.setEnergyFairy(sf);
    	System.out.println(c.getMicrosecToVOn());
    	c.checkIfPowersOffDuringExecution(8000000);
    	
    	
    }
    

}
