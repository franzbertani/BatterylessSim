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
    private double onThreshold;
    private SimpleFairy energyFairy;
	private int millisecFraction;
    private double vOff;
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

    public CapSimulator (double capacitance, double initVoltage){
        this.capacitance = capacitance;
        this.voltage = initVoltage;
        this.millisecFraction = 0;
        this.vOff = 1.8;
        this.lifecycles = 0;
        this.timeSpan = 0;
        this.logValues = new ArrayList<Tuple>();
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
    
    private double resistanceOff() {
    	// Resistance is only the comparator, so voltage / 1.5uA
    	return voltage / 0.0000015;
    }
    
    private void consumeEnergyAndUpdateV(double energy) {
    	double newEnergy = (0.5*capacitance*voltage*voltage) - energy;
    	updateVoltage(Math.max(Math.sqrt(2*newEnergy/capacitance), energyFairy.peekVoltage()));
    }
    
    private double getOffEnergy() {
    	return 0.5*capacitance*vOff*vOff;
    }
    
    public double getVoltage () { return voltage; }
    
    private void updateVoltage( double voltage ) {
    	//System.out.println("Updating voltage at trace time " + currentTraceTime);
    	this.voltage = voltage;
    	if(currentTraceTime != energyFairy.peekTime()) {
    		currentTraceTime = energyFairy.peekTime();
    		logValues.add(new Tuple(energyFairy.peekTime(), this.voltage));
    	}
    }
    
    private int getMicrosecToVOn() {
    	double time = 0;
    	double timeFraction = 0.001 - millisecFraction/1000000;
    	double vSupply = energyFairy.peekVoltage();
    	double resistance;
    	int iteration = 0;
    	lifecycles++;
    	while (voltage <= onThreshold) {
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
			PrintWriter wr = new PrintWriter(new File("/Users/francesco/git/scheduler/pydef/outputlog.csv"));
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
			PrintWriter wr = new PrintWriter(new File("/Users/francesco/git/scheduler/pydef/eventLogger.csv"));
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
    
    public void checkIfPowersOffDuringExecution (double microseconds, String taskName) {
    	double vSupply = energyFairy.peekVoltage();
		double timeLeft = microseconds;
		double resistance;
		int execCC;
		double spentEnergy;
		int iteration = 0;
		
		System.out.println("Testin execution of " + taskName + " for "+microseconds+" microseconds, with trace at "+energyFairy.peekTime()+" milliseconds");
		
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
    				System.err.println("V trace ended");
    				writeLogToCsv();
    				resetManager.stopExecution("V trace ended", lifecycles); //THIS EXITS THE SIMULATION AND STOPS EVERTYTHING
    			}
    			
    			vSupply = energyFairy.peekVoltage();
    		}

    		resistance = resistanceActive();

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
    				System.out.println("Power failure at trace time = " +energyFairy.peekTime() +   " milliseconds, reset after offTime = " + offTime + " microseconds");
    				return;
    			}
    			
    		} else { // MI STO CARICANDO
    			updateVoltage(vSupply + (voltage - vSupply)*Math.exp(-(1e-6)/(resistance*capacitance))); //CARICA PER UN MICROSECONDO   			
    		} 
    		
    		millisecFraction += 1;
    		timeLeft -= 1;
    	}
    	
    	System.out.println("No resets");
    }
    
    public void checkIfPowersOffDuringExecution(int clockCycles, String name) {
    	checkIfPowersOffDuringExecution(Math.ceil(((double)clockCycles / frequency) * 10000000), name);
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
