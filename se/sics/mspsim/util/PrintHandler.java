package se.sics.mspsim.util;

import edu.clemson.eval.EvalLogger;
import se.sics.mspsim.core.MSP430;

public class PrintHandler {

	private static final String GRAPH_EVENT = "GRAPH-EVENT";
	private static final String PRINT = "PRINTF";
	private static final String RESET = "RESET";
	
	private EvalLogger evallogger;
	private ComponentRegistry registry;
	private MSP430 cpu; 
	
	public PrintHandler() {}
	
	public PrintHandler(ComponentRegistry registry) {
		this.registry = registry;
		this.cpu = this.registry.getComponent(MSP430.class);
	}
	
	public PrintHandler(String name) {
		evallogger = EvalLogger.getInstance(name);
	}
	
	public PrintHandler(String name, ComponentRegistry registry) {
		evallogger = EvalLogger.getInstance(name);
		this.registry = registry;
		this.cpu = this.registry.getComponent(MSP430.class);
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
				cpu.reset();
				break;
			default:
				System.err.println("Command not recognized!");
				System.out.println(fullcommand);
		}
	}
}
