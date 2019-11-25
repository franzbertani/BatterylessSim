package it.polimi.neslab.utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.Math;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.stream.Stream;

import se.sics.mspsim.config.MSP430f2132Config;
import se.sics.mspsim.config.MSP430fr6989Config;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.Tuple;

public class CapSimulator {
    private double capacitance;
    private double voltage;
    private double maxVoltage;
    public double getMaxVoltage() {
		return maxVoltage;
	}

	public void setMaxVoltage(double maxVoltage) {
		this.maxVoltage = maxVoltage;
	}

	public double getCapacitance() {
		return capacitance;
	}

	private double onThreshold;
    private SimpleFairy energyFairy;
	private int millisecFraction;
    private double vOff;
    private double vMax;
	private ResetManager resetManager;
	private int memoryLocation;
	private long timeSpan;
	private int frequency;
	private int lifecycles;
	private double currentTraceTime;
	private static double energyPerCC = 1e-9;
	private static double voltageComparatorResolution = 0.001;
	private ArrayList<Tuple> logValues;
    private static final double R_ESR = 30000;
    private EventLogger eventLogger;
    private static final int ACTIVE = 1;
    private static final int LPM = 2;
    private static final int OFF = 0;
    private int mode;
    
    public CapSimulator (double capacitance, double initVoltage){
        this.capacitance = capacitance;
        this.voltage = initVoltage;
        this.millisecFraction = 0;
        this.vOff = 1.88;
        this.vMax = 5;
        this.lifecycles = 0;
        this.timeSpan = 0;
        this.logValues = new ArrayList<Tuple>();
        System.err.println("Capacity = " + capacitance);
        this.mode = ACTIVE;
    }
    
    public double getvOff() {
		return vOff;
	}

	public void setMemoryLocation (int memoryLocation) {
    	this.memoryLocation = memoryLocation;
    }

    public void setEnergyFairy (SimpleFairy ef) {
        this.energyFairy = ef;
        this.currentTraceTime = ef.peekTime();
    }
    
    public SimpleFairy getEnergyFairy() {
		return energyFairy;
	}
    
    public void setEventLogger(EventLogger ev) {
    	this.eventLogger = ev;
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
    
    private double resistanceLPM() {
    	return (18232 * voltage) + 1017.9;
    }
    
    private double resistanceOff() {
    	// Resistance is only the comparator, so voltage / 1.5uA
    	return voltage / 0.0000015;
    }
    
    private void consumeEnergyAndUpdateV(double energy) {
    	double newEnergy = (0.5*capacitance*voltage*voltage) - energy;
    	updateVoltage(
    			Math.max(
    					Math.sqrt(2*newEnergy/capacitance), 
    					energyFairy.peekVoltage())
    			);
    }
    
    private double getOffEnergy() {
    	return 0.5*capacitance*vOff*vOff;
    }
    
    public double getVoltage () { return voltage; }
    
    private void updateVoltage( double voltage ) {
    	//System.out.println("Updating voltage at trace time " + currentTraceTime);
    	this.voltage = Math.min(this.vMax,voltage);
    	if(currentTraceTime != energyFairy.peekTime()) {
    		currentTraceTime = energyFairy.peekTime();
    		logValues.add(new Tuple(energyFairy.peekTime(), this.voltage));
    	}
    }
    
    public void setActiveMode() {
    	this.mode = ACTIVE;
    }
    
    public void setLPM() {
    	this.mode = LPM;
    }
    
    private int getMicrosecToVOn() {
    	double time = 0;
    	double timeFraction = 0.001 - millisecFraction/1000000;
    	double vSupply = energyFairy.peekVoltage();
    	double resistance;
    	int iteration = 0;
    	lifecycles++;
    	while (voltage < onThreshold) {
    		if(vSupply < voltage) {
    			
    			resistance = resistanceOff();
    	    	updateVoltage(Math.max( voltage*Math.exp(-(timeFraction)/(resistance*capacitance)), vSupply));
    		} else {
    			
    	    	updateVoltage(vSupply + (voltage - vSupply)*Math.exp(-(timeFraction)/(R_ESR*capacitance)));
    		}
    		//System.out.println( "vsupply: " + vSupply +  " vcurrent: " + voltage + " time fraction: " + timeFraction );
	    	millisecFraction = 0;
	    	time += (timeFraction * 1000000);
	    	timeFraction = 0.001 - millisecFraction/1000000;
	    	if(!energyFairy.hasEnded()) {
				energyFairy.stepTrace();
			} else {
				System.err.println("V trace ended");
				writeLogToCsv();
				resetManager.stopExecution("V trace ended", lifecycles);
			}
	    	vSupply = energyFairy.peekVoltage();
	    	iteration++;
    	}
    	return (int) (time);
    }
    
    private void writeLogToCsv() {
    	try {
			PrintWriter wr = new PrintWriter(new File("/Users/francesco/git/scheduler/datiScheduler/capacitorTrace.csv"));
			ListIterator<Tuple> iter = logValues.listIterator();
			System.out.println("SIZE: "+ logValues.size());
	    	while(iter.hasNext()) {
	    		wr.println(iter.next().toString());
	    		wr.flush();
	    	}
	    	wr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	try {
			PrintWriter wr = new PrintWriter(new File("/Users/francesco/git/scheduler/datiScheduler/eventsLog.csv"));
			ListIterator<CaptionedTuple> iter = eventLogger.getLog().listIterator();
	    	while(iter.hasNext()) {
	    		wr.println(iter.next().toString());
	    		wr.flush();
	    	}
	    	wr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    	
    }
    
    public int checkIfPowersOffDuringExecution (double microseconds, String taskName) {
    	double vSupply = energyFairy.peekVoltage();
		double timeLeft = microseconds;
		double resistance;
		int execCC;
		double spentEnergy;
		int iteration = 0;
		
		System.err.println("[CAP] Testin execution of " + taskName + " for "+microseconds+" microseconds, with trace at "+energyFairy.peekTime()+" milliseconds");
		
    	while (timeLeft >= 0) {
    		/*
    		if(Math.floorMod(iteration, 100)==0) {
    			System.out.println(iteration);
    		}
    		*/
    		iteration++;
    		
    		if(millisecFraction == 1000) {
    			millisecFraction = 0;
    			
    			if(!energyFairy.hasEnded()) {
    				energyFairy.stepTrace();
    			} else {
    				System.err.println("[CAP] V trace ended");
    				writeLogToCsv();
    				resetManager.stopExecution("[CAP] V trace ended", lifecycles); //THIS EXITS THE SIMULATION AND STOPS EVERTYTHING
    			}
    			
    			vSupply = energyFairy.peekVoltage();
    		}
    		
    		switch(this.mode) {
    		case ACTIVE:
    			resistance = resistanceActive();
    			break;
    		case LPM:
    			resistance = resistanceLPM();
    			break;
    		default:
    			resistance = resistanceActive();
    				
    		}

    		if(vSupply < voltage) { // MI STO SCARICANDO
    			execCC = (int) Math.floor(1e-6 * frequency);
    			spentEnergy = energyPerCC * execCC;
    			consumeEnergyAndUpdateV(spentEnergy);
    			
    			if(voltage <= vOff) { //MI SONO SPENTO
    				resetManager.performReset();
    				eventLogger.addLog(energyFairy.peekTime(), voltage, "PF");
    				int offTime = getMicrosecToVOn();
    				eventLogger.addLog(energyFairy.peekTime(), voltage, "On");
    				resetManager.persistOffTime(offTime);
    				System.err.println("[CAP] Power failure at trace time = " +energyFairy.peekTime() +   " milliseconds, reset after offTime = " + offTime + " microseconds");
    				resetManager.performReset();
    				return offTime;
    			}
    			
    		} else { // MI STO CARICANDO
    			updateVoltage(vSupply + (voltage - vSupply)*Math.exp(-(1e-6)/(resistance*capacitance))); //CARICA PER UN MICROSECONDO   			
    		} 
    		
    		millisecFraction += 1;
    		timeLeft -= 1;
    	}
    	
    	System.err.println("[CAP] No resets while running " + taskName);
    	return 0;
    }
    
    public double getOnThreshold() {
		return onThreshold;
	}

	public int checkIfPowersOffDuringExecution(int clockCycles, String name) {
    	return checkIfPowersOffDuringExecution(Math.ceil(((double)clockCycles / frequency) * 1000000), name);
    }
    
    
    public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public static void main(String[] args) {
    	MSP430 msp = new MSP430(0, new ComponentRegistry(),
                new MSP430fr6989Config());
    	ResetManager rm = new ResetManager(10, 70000000, msp);
    	CapSimulator c = new CapSimulator(4.7e-5, 0);
    	c.setResetManager(rm);
    	c.frequency = msp.getDCOFrequency();
    	c.setOnThreshold(3.3);
    	SimpleFairy sf = new SimpleFairy("/Users/francesco/git/BatterylessSim/traces/1.txt");
    	c.setEnergyFairy(sf);
    	System.out.println(c.getMicrosecToVOn());
    	c.checkIfPowersOffDuringExecution(8e6, "TEST");
    	
    	
    }
    

}
