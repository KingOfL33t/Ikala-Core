
package com.ikalagaming.packages;

import com.ikalagaming.packages.Package;

/**
 * A command that has been registered with the system. This contains a string
 * representing the command and a reference to the owner of the command.
 * 
 * @author Ches Burks
 * 
 */
public class PackageCommand implements Comparable<PackageCommand> {
	private String command;
	private Package owner;

	/**
	 * Constructs a new registered command with the given command and package
	 * 
	 * @param cmd the command registered
	 * @param owner what package registered the command
	 */
	public PackageCommand(String cmd, Package owner) {
		this.command = cmd;
		this.owner = owner;
	}

	/**
	 * Returns the command
	 * 
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Returns the package that registered this command
	 * 
	 * @return the owner
	 */
	public Package getOwner() {
		return owner;
	}

	@Override
	public int compareTo(PackageCommand o) {
		return this.getCommand().toLowerCase()
				.compareTo(o.getCommand().toLowerCase());
	}

}
