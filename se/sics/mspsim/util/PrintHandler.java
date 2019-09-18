package se.sics.mspsim.util;

import edu.clemson.eval.EvalLogger;
import se.sics.mspsim.core.FramController;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.Memory.AccessMode;

public class PrintHandler {

	private static final String GRAPH_EVENT = "GRAPH-EVENT";
	private static final String PRINT = "PRINTF";
	private static final String RESET = "RESET";
	private static final String CHVAR = "CHVAR";
	
	private EvalLogger evallogger;
	private ComponentRegistry registry;
	private FramController fram; 
	private MSP430 cpu; 
	
	public PrintHandler() {}
	
	
	public PrintHandler(ComponentRegistry registry, FramController fram) {
		this.registry = registry;
		this.fram = fram;
		this.cpu = this.registry.getComponent(MSP430.class);
	}
	
	public PrintHandler(String name, FramController fram) {
		evallogger = EvalLogger.getInstance(name);
		this.fram = fram;
	}
	
	public PrintHandler(String name, ComponentRegistry registry, FramController fram) {
		evallogger = EvalLogger.getInstance(name);
		this.fram = fram;
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
			case CHVAR:
				System.out.println("cambio valore variabile");
				fram.framWrite(Integer.parseInt(command[1].split(" ")[1]),  800, AccessMode.WORD20);
				break;
			default:
				System.err.println("Command not recognized!");
				System.out.println(fullcommand);
		}
	}
}
