package se.sics.mspsim.util;

import java.sql.Time;
import java.util.HashMap;
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
	private static final String RESET = "RESET";
	private static final String TEST_RESET = "TEST_RESET";
	private static final String CHVAR = "CHVAR"; // set FRAM variable value.
	private static final String START_TIME = "START_TIME";
	private static final String GET_TIME = "GET_TIME";
	private static final String TEST_EXECUTION = "TEST_EXECUTION";
	private static final String SET_TARDIS_VARIABLE = "SET_TARDIS_VARIABLE";
	private static final String SET_VON = "SET_VON";
	private static final String LOG_EVENT = "LOG_EVENT";
	
	private EvalLogger evallogger;
	private EventLogger eventLogger;
	private ComponentRegistry registry;
	private FramController fram; 
	private MSP430 cpu; 
	private ResetManager resetManager;
	private CapSimulator capacitor;
	private Map<String, Integer> timersMap;
	
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
		
	}

	public void handleCommand (String fullcommand) {
		String [] command = fullcommand.split(":", 2);

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
				System.out.println("printf: "+ command[1]);
				break;
			case RESET:
				System.out.println("reset: "+ command[1]);
				resetManager.performReset();
				break;
			case CHVAR:
				System.out.println("Set new value for variable");
				fram.framWrite(Integer.parseInt(command[1].split(" ")[1]),  800, AccessMode.WORD20); //TODO implement 
				break;
			case TEST_RESET:
				System.out.println("Test reset for current task");
				int offTime = resetManager.computeOffTime();
				if(offTime == 0) {
					System.out.println("No resets for current task");
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
				break;
			case GET_TIME:
				double end_time = cpu.getTimeMillis();
				String timerId = command[1].split("-")[0].trim();
				System.err.println("Requested timer " + timerId);
				System.err.println("Started at time " + timersMap.get(timerId));
				int address = Integer.parseInt(command[1].split("-")[1].trim());
				int deltaTime;
				if(timersMap.containsKey(timerId)) {
					deltaTime = (int) ((end_time - timersMap.get(timerId)) * 1000);
				} else {
					System.err.println("ERROR: requested unexistent timer '" + timerId + "' persisted 0");
					deltaTime = 0;
				}
				fram.framWrite(address, deltaTime, AccessMode.WORD20);
				break;
			case TEST_EXECUTION:
				String[] input = command[1].split(",");
				int microseconds = Integer.parseInt(input[0].split(" ")[1]);
				String taskName = input.length==2 ? input[1] : "no name";
				capacitor.checkIfPowersOffDuringExecution(microseconds, taskName);
				break;
			case SET_VON:
				double von = Integer.parseInt(command[1].split(" ")[1]) / 10.0;
				System.err.println("Setting V On to " + von);
				capacitor.setOnThreshold(von);
				eventLogger.addLog(capacitor.getEnergyFairy().peekTime(), capacitor.getVoltage(), "Setting V On to " + von);
				break;
			case LOG_EVENT:
				String logValue = command[1];
				eventLogger.addLog(capacitor.getEnergyFairy().peekTime(), capacitor.getVoltage(), logValue);
				break;
			default:
				System.err.println("Command not recognized!");
				System.out.println(fullcommand);
		}
	}
}
