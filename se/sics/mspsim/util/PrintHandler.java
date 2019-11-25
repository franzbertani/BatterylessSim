package se.sics.mspsim.util;

import java.sql.Time;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.clemson.eval.EvalLogger;
import it.polimi.neslab.utils.CapSimulator;
import it.polimi.neslab.utils.EventLogger;
import it.polimi.neslab.utils.ResetManager;
import it.polimi.neslab.utils.SimpleFairy;
import se.sics.mspsim.core.FramController;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.Memory.AccessMode;

public class PrintHandler {

	private static final String GRAPH_EVENT = "GRAPH-EVENT";
	private static final String PRINT = "PRINTF";
	private static final String PRINTFLONG = "PRINTFLONG";
	private static final String RESET = "RESET";
	private static final String TEST_RESET = "TEST_RESET";
	private static final String CHVAR = "CHVAR"; // set FRAM variable value.
	private static final String START_TIME = "START_TIME";
	private static final String START_CCOUNT = "START_CCOUNT";
	private static final String GET_TIME = "GET_TIME";
	private static final String GET_CCOUNT = "GET_CCOUNT";
	private static final String TEST_EXECUTION_TIME = "TEST_EXECUTION_TIME";
	private static final String TEST_EXECUTION_CCOUNT = "TEST_EXECUTION_CCOUNT";
	private static final String SET_TARDIS_VARIABLE = "SET_TARDIS_VARIABLE";
	private static final String SET_VON = "SET_VON";
	private static final String LOG_EVENT = "LOG_EVENT";
	private static final String GET_FREQ = "GET_FREQ";
	private static final String GET_CURRENT_CLOCK = "GET_CURRENT_CLOCK";
	private static final String GET_CAP_VOLTAGE = "GET_CAP_VOLTAGE";
	private static final String SLEEP_FOR_TIME = "SLEEP_FOR_TIME";
	private static final String INCREASE_THRESHOLD = "INCREASE_THRESHOLD";
	
	private EvalLogger evallogger;
	private EventLogger eventLogger;
	private ComponentRegistry registry;
	private FramController fram; 
	private MSP430 cpu; 
	private ResetManager resetManager;
	private CapSimulator capacitor;
	private Map<String, Integer> timersMap;
	private Map<String, Long> cCountersMap;
	private boolean isSenseEnabled;
	private static int senseMillis = 20;
	
	public PrintHandler() {}
	
	
	public PrintHandler(ComponentRegistry registry, FramController fram) {
		this.registry = registry;
		this.fram = fram;
		this.cpu = registry.getComponent(MSP430.class);
		this.capacitor = cpu.getCapSimulator();
		this.resetManager = cpu.getResetManager();
		this.eventLogger = new EventLogger();
		capacitor.setEventLogger(eventLogger);
		this.timersMap = new HashMap<>();
		this.cCountersMap = new HashMap<>();
		this.isSenseEnabled = true;
		
	}
	
	/*
	public PrintHandler(String name, FramController fram) {
		evallogger = EvalLogger.getInstance(name);
		this.fram = fram;
		this.resetManager = new ResetManager(RESET_MEMORY_SIZE, MAX_OFF_MILLISEC, cpu);
		this.resetManager.setFramController(fram);
	}
	*/
	
	public PrintHandler(String name, ComponentRegistry registry, FramController fram) {
		evallogger = EvalLogger.getInstance(name);	
		this.eventLogger = new EventLogger();
		this.fram = fram;
		this.registry = registry;
		this.cpu = registry.getComponent(MSP430.class);
		this.capacitor = cpu.getCapSimulator();
		capacitor.setEventLogger(eventLogger);
		this.resetManager = cpu.getResetManager();
		this.timersMap = new HashMap<>();
		this.cCountersMap = new HashMap<>(); 
		this.isSenseEnabled = true;
		
	}

	public void handleCommand (String fullcommand) {
		String [] command = fullcommand.split(":", 2);
		String[] input;
		String taskName;

		switch (command[0]) { // Switch on the command (what comes before the semi-colon) 
			case GRAPH_EVENT:
				try {
					if (command[1].contains("GRAPH")) {
						// Not sure why this happens right now, but for the time being we'll ignore it...
						System.out.println("command = " + fullcommand);
					} else {
						evallogger.addSensorEvent(command[1] + "\n");
					}
				} catch (NullPointerException e) {
					System.err.println("Graphing events only works with an ekhotracedir specified!");
				}
				break;
			case PRINT:
				System.out.println(cpu.getTime() + " printf: "+ command[1]);
				break;
			case PRINTFLONG:
				String[] parts = command[1].split("-");
				String[] values = parts[1].split(";");
				System.out.println(values[0] + " " + values[1]);
				double value = Integer.parseInt(values[0].trim()) + Integer.parseInt(values[1].trim())*(Math.pow(2, 16));
				System.out.println("printf: " + parts[0] + " " + value);
			case RESET:
				System.err.println("[RESET] reset: "+ command[1]);
				resetManager.performReset();
				break;
			case CHVAR:
				System.err.println("[CHVAR] Set new value for variable");
				fram.framWrite(Integer.parseInt(command[1].split(" ")[1]),  800, AccessMode.WORD20); //TODO implement 
				break;
			case TEST_RESET:
				System.err.println("[TEST_RESET] Test reset for current task");
				int offTime = resetManager.computeOffTime();
				if(offTime == 0) {
					System.err.println("[TEST_RESET] No resets for current task");
				} else {
					resetManager.performReset();
					resetManager.persistOffTime(Integer.parseInt(command[1].split(" ")[1]));
				}
				break;
			case SET_TARDIS_VARIABLE:
				resetManager.setMemoryLocation(Integer.parseInt(command[1].split(" ")[1]));
				break;
			case START_TIME:
				timersMap.put(command[1].trim(), (int) cpu.getTimeMillis());
				if(command[1].trim().equals("misd(SENSE)"))
					this.isSenseEnabled=false;
				break;
			case GET_TIME:
				double end_time = cpu.getTimeMillis();
				String timerId = command[1].split("-")[0].trim();
				int address = Integer.parseInt(command[1].split("-")[1].trim());
				int deltaTime;
				if(timersMap.containsKey(timerId)) {
					deltaTime = (int) ((end_time - timersMap.get(timerId)) * 1000);
					if(timerId.equals("misd(SENSE)")) {
						if(!this.isSenseEnabled)
							this.isSenseEnabled = (end_time - timersMap.get(timerId)) > this.senseMillis; //20Hz
					} else
						System.err.println("[GET_TIME] Requested timer " + timerId + " --- started at time " + timersMap.get(timerId) * 1000 + " delta " + deltaTime);
				} else {
					System.err.println("[GET_TIME] ERROR: requested unexistent timer '" + timerId + "' persisted 0");
					deltaTime = 0;
				}
				if(timerId.equals("misd(SENSE)")) {
					fram.framWrite(address, isSenseEnabled ? 1 : 0, AccessMode.WORD20);
				} else
					fram.framWrite(address, deltaTime, AccessMode.WORD20);
				break;
			case TEST_EXECUTION_TIME:
				input = command[1].split(",");
				int microseconds = Integer.parseInt(input[0].split(" ")[1]);
				taskName = input.length==2 ? input[1] : "no name";
				int time = capacitor.checkIfPowersOffDuringExecution(microseconds, taskName);
				if(time>50000) {
					this.isSenseEnabled = true;
				}
				break;
			case START_CCOUNT:
				cCountersMap.put(command[1].trim(), cpu.cpuCycles);
				break;
			case GET_CCOUNT:
				String counterId = command[1].split("-")[0].trim();
				int deltaCC;
				long initialCC;
				if(cCountersMap.containsKey(counterId)) {
					initialCC = cCountersMap.get(counterId);		
					deltaCC = (int) (cpu.cpuCycles - initialCC);
					System.err.println("[GET_CCOUNT] Requested timer " + counterId + " --- start cc=" + initialCC + " final cc=" + cpu.cpuCycles + " delta cc=" + deltaCC);
				} else {
					System.err.println("[GET_CCOUNT] ERROR: requested unexistent timer '" + counterId + "' persisted 0");
					deltaCC = 0;
				}
				fram.framWrite(Integer.parseInt(command[1].split("-")[1].trim()), deltaCC, AccessMode.WORD20);
				break;
			case TEST_EXECUTION_CCOUNT:
				input = command[1].split(",");
				int cc = Integer.parseInt(input[0].split(" ")[1]);
				taskName = input.length==2 ? input[1] : "no name";
				int offT = capacitor.checkIfPowersOffDuringExecution(cc, taskName);
				if(offT>50000) {
					this.isSenseEnabled = true;
				}
				break;
			case SET_VON:
				double energy = Integer.parseInt(command[1].split(" ")[1]) * 1e-6;
				double deltaV = Math.sqrt(2*energy/capacitor.getCapacitance());
				double von = Math.round((capacitor.getvOff() + deltaV)*100.0)/100.0;
				System.err.println("[SET_VON] Setting V On to " + von);
				capacitor.setOnThreshold(Math.min(von,5));
				eventLogger.addLog(capacitor.getEnergyFairy().peekTime(), capacitor.getVoltage(), "VOn= " + von);
				break;
			case INCREASE_THRESHOLD:
				capacitor.setOnThreshold(Math.min(capacitor.getOnThreshold()+0.2, 5));
				System.err.println("[INCREASE_THRESHOLD] Setting V On to " + (capacitor.getOnThreshold()+0.2));
				eventLogger.addLog(capacitor.getEnergyFairy().peekTime(), capacitor.getVoltage(), "VOn= " + capacitor.getOnThreshold());
				break;
			case LOG_EVENT:
				String logValue = command[1];
				eventLogger.addLog(capacitor.getEnergyFairy().peekTime(), capacitor.getVoltage(), logValue);
				break;
			case GET_FREQ:
				fram.framWrite(Integer.parseInt(command[1].split(" ")[1]),  cpu.dcoFrq, AccessMode.WORD20);
				break;
			case GET_CAP_VOLTAGE:
				float voltage = (int) ((Math.round(capacitor.getVoltage()*100.0)/100.0));
				System.err.println("[GET_CAP_VOLTAGE] Current cap voltage " + voltage + "V");
				int availableVoltage = (int) ((voltage - capacitor.getvOff()) * 100); 
				fram.framWrite(Integer.parseInt(command[1].split(" ")[1]),  availableVoltage, AccessMode.WORD); 
				break;
			case SLEEP_FOR_TIME:
				input = command[1].split(",");
				int sleepTime = Integer.parseInt(input[0].split(" ")[1]);
				capacitor.setLPM();
				capacitor.checkIfPowersOffDuringExecution((double)sleepTime, "sleep");
				capacitor.setActiveMode();
				break;
			default:
				System.err.println("[!!!] Command not recognized!");
				System.out.println(fullcommand);
		}
	}
}
