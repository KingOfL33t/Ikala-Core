
package com.ikalagaming.gui.console.events;

import com.ikalagaming.event.Event;

/**
 * A message needs to be sent to the console.
 * 
 * @author Ches Burks
 * 
 */
public class ConsoleCommandEntered extends Event {

	/**
	 * The command and parameters.
	 */
	private final String message;

	/**
	 * Prints a help message to the console stating the given command was not
	 * recognized.
	 * 
	 * @param cmd the command that was not known
	 */
	public ConsoleCommandEntered(String cmd) {
		this.message = cmd;
	}

	/**
	 * Returns the command transmitted.
	 * 
	 * @return the command
	 */
	public String getCommand() {
		return this.message;
	}

}