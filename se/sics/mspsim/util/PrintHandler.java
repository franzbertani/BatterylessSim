package se.sics.mspsim.util;

import edu.clemson.eval.EvalLogger;
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
	private static final int RESET_MEMORY_SIZE = 4;
	private static final int MAX_OFF_MILLISEC = 10000;

	
	private EvalLogger evallogger;
	private ComponentRegistry registry;
	private FramController fram; 
	private MSP430 cpu; 
	private ResetManager resetManager;
	private double time;
	
	public PrintHandler() {}
	
	
	public PrintHandler(ComponentRegistry registry, FramController fram) {
		this.registry = registry;
		this.fram = fram;
		this.cpu = this.registry.getComponent(MSP430.class);
		this.resetManager = new ResetManager(RESET_MEMORY_SIZE, MAX_OFF_MILLISEC, cpu);
		this.resetManager.setFramController(fram);
		this.time = 0.0;
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
		this.fram = fram;
		this.registry = registry;
		this.cpu = this.registry.getComponent(MSP430.class);
		this.resetManager = new ResetManager(RESET_MEMORY_SIZE, MAX_OFF_MILLISEC, cpu);
		this.resetManager.setFramController(fram);
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
			case START_TIME:
				this.time = cpu.getTimeMillis();
				break;
			case GET_TIME:
				double end_time = cpu.getTimeMillis();
				int delta_time = (int) ((end_time - time) * 1000);
				fram.framWrite(Integer.parseInt(command[1].split(" ")[1]), delta_time, AccessMode.WORD20);
				break;
			default:
				System.err.println("Command not recognized!");
				System.out.println(fullcommand);
		}
	}
}
