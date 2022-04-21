package com.ikalagaming.util;

import java.io.File;

/**
 * Contains methods for fetching various system information such as the Java
 * version or the directory in which applications store data.
 *
 * @author Ches Burks
 *
 */
public class SystemProperties {

	/**
	 * The type of operating system the engine is running on
	 */
	private static String operatingSystem;

	/**
	 * Equivalent to the current systems appdata folder
	 */
	private static String homeDirectory;

	/**
	 * Returns the home directory for the program. This is the application data
	 * folder and <b>NOT</b> the games root folder. The games folder is
	 * contained in this directory. For example, the game 'TextGame's folder
	 * would look like <code>getHomeDir() + "/TextGame/"</code> on a Windows
	 * computer.
	 * <table summary="The associated directory">
	 * <tr>
	 * <th>Operating System</th>
	 * <th>Directory</th>
	 * </tr>
	 * <tr>
	 * <td>WINDOWS</td>
	 * <td><code>System.getenv("APPDATA")</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>MAC</code></td>
	 * <td>Users home directory +
	 * <code>"/Library/Application Support"</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>LINUX</code></td>
	 * <td>Users home directory</td>
	 * </tr>
	 * <tr>
	 * <td><code>UNKNOWN(anything else)</code></td>
	 * <td>The user's current working directory</td>
	 * </tr>
	 * </table>
	 * <p>
	 * The users home directory is defined as
	 * <code>System.getProperty("user.home")</code>
	 *
	 * @return the programs home directory
	 */
	public static String getHomeDir() {
		if (SystemProperties.homeDirectory == null) {
			SystemProperties.obtainHomeDir();
		}
		return SystemProperties.homeDirectory;
	}

	/**
	 * Returns the OS name. Note that this is not equivalent to
	 * <code>System.getProperty("os.name")</code> Possible OS names are:
	 * <ul>
	 * <li>WINDOWS</li>
	 * <li>MAC</li>
	 * <li>LINUX</li>
	 * <li>UNKNOWN</li>
	 * </ul>
	 *
	 * @see #getOSActualName()
	 *
	 * @return the name of the OS
	 */
	public static String getOS() {
		if (SystemProperties.operatingSystem == null) {
			SystemProperties.obtainOS();
		}
		return SystemProperties.operatingSystem;
	}

	/**
	 * Returns the OS name returned by
	 * <code>System.getProperty("os.name")</code>. Please note that this is not
	 * the same thing as {@link #getOS()}.
	 *
	 * @see #getOS()
	 * @return operating system name
	 */
	public static String getOSActualName() {
		return System.getProperty("os.name");
	}

	/**
	 * Returns the current Operating system architecture. This is the same thing
	 * as calling <code>System.getProperty("os.arch")</code>
	 *
	 * @return operating system architecture
	 */
	public static String getOSArch() {
		return System.getProperty("os.arch");
	}

	/**
	 * Returns the current Operating system version. This is the same thing as
	 * calling <code>System.getProperty("os.version")</code>
	 *
	 * @return operating system version
	 */
	public static String getOSVersion() {
		return System.getProperty("os.version");
	}

	/**
	 * Returns the Default temp file path. This is the same thing as calling
	 * <code>System.getProperty("java.io.tmpdir")</code>
	 *
	 * @return default temp file path
	 */
	public static String getTmpDir() {
		return System.getProperty("java.io.tmpdir");
	}

	/**
	 * Determines the application data folder based on the OS type. Sets the
	 * home dir string to the folder path.
	 */
	private static void obtainHomeDir() {
		if (SystemProperties.operatingSystem == null) {
			SystemProperties.obtainOS();
		}
		String theDir = "";
		if ("WINDOWS".equals(SystemProperties.operatingSystem)) {
			theDir = System.getenv("APPDATA");
		}
		else if ("MAC".equals(SystemProperties.operatingSystem)) {
			theDir = System.getProperty("user.home") + File.separator
				+ "Library" + File.separator + "Application Support";
		}
		else if ("LINUX".equals(SystemProperties.operatingSystem)) {
			theDir = System.getProperty("user.home");
		}
		else {
			theDir = System.getProperty("user.dir");
		}
		SystemProperties.homeDirectory = theDir;
	}

	/**
	 * Determines what operating system is running and sets the OS string.
	 */
	private static void obtainOS() {
		String osName = System.getProperty("os.name").toLowerCase();
		String toSet;
		if (osName.contains("win")) {
			toSet = "WINDOWS";
		}
		else if (osName.contains("mac")) {
			toSet = "MAC";
		}
		else if (osName.contains("linux") || osName.contains("unix")) {
			toSet = "LINUX";
		}
		else {
			toSet = "UNKNOWN";
		}
		SystemProperties.operatingSystem = toSet;
	}

	/**
	 * Private constructor so that this class is not instantiated.
	 */
	private SystemProperties() {}

}
