package it.polimi.neslab.utils;
import java.lang.Math;

public class CapSimulator {
    private double capacitance;
    private double voltage;
    private double maxVoltage;
    private double onThreshold;
    private SimpleFairy energyFairy;
    private double millisecFraction;
    private double vOff;
	private ResetManager resetManager;
	private int memoryLocation;

    private static final double R_ESR = 1000000;

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
    
    private double resistanceActive () {
        // Linear regression calculated from spreadsheet; R^2 = 0.9958
        return (4010.6 * voltage) + 803.53;
    }
    
    private double resistanceOff() {
    	// Resistance is only the comparator, so voltage / 1.5uA
    	return voltage / 0.0000015;
    }
    
    public double getVoltage () { return voltage; }
    
    private double getTimeToFullCharge() {
    	double time = 0;
    	double fraction = 1000 - millisecFraction;
    	double vSupply = energyFairy.peekVoltage();
    	
    	while (voltage < onThreshold) {
	    	double t = - resistanceActive()* capacitance * Math.log((onThreshold - vSupply)/( voltage - vSupply));
	    	if( t < fraction) {
	    		millisecFraction += t;
	    		voltage = onThreshold;
	    		time += t;
	    		return time;
	    	}
	    	millisecFraction += fraction;
	    	time += fraction;
	    	voltage = vSupply + (voltage - vSupply)*Math.exp(-fraction/resistanceOff()*capacitance);
	    	if(millisecFraction == 1000) {
	    		millisecFraction = 0;
	    		energyFairy.stepTrace();
	        	vSupply = energyFairy.peekVoltage();
	    	}
    	}
    	return time;
    }
    
    public void chargeForMicroseconds (double microseconds) {
    	double vSupply = energyFairy.peekVoltage();
		double timeLeft = microseconds;
		
    	while (timeLeft > 0) {
	    	double fraction = Math.min(1000 - millisecFraction, timeLeft);
	    	double t = - resistanceActive()* capacitance * Math.log((vOff - vSupply)/( voltage - vSupply));
	    	if ( t < fraction) {
	    		millisecFraction += t;
	    		resetManager.performReset();
	    		double offTime = getTimeToFullCharge();
	    		resetManager.persistOffTime(memoryLocation, (int) offTime);
	    		return;
	    	}
	    		
	    	millisecFraction += fraction;
	    	voltage = vSupply + (voltage - vSupply)*Math.exp(-fraction/resistanceActive()*capacitance);
	    	timeLeft -= fraction;
	    	if(millisecFraction == 1000) {
	    		millisecFraction = 0;
	    		energyFairy.stepTrace();
	        	vSupply = energyFairy.peekVoltage();
	    	}
    	}	    	
    }
    
    
    
    
    
    
    
    
    

    

    
    

}
