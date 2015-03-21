
package com.ikalagaming.logging;

import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.ikalagaming.event.EventHandler;
import com.ikalagaming.event.Listener;
import com.ikalagaming.localization.Localization;
import com.ikalagaming.logging.events.Log;
import com.ikalagaming.logging.events.LogError;
import com.ikalagaming.packages.Package;
import com.ikalagaming.packages.PackageState;
import com.ikalagaming.util.SafeResourceLoader;

/**
 * Handles reporting and logging errors.
 * 
 * @author Ches Burks
 * 
 */
public class LoggingPackage implements Package, Listener {

	private ResourceBundle resourceBundle;
	private PackageState state = PackageState.DISABLED;
	private final double version = 0.2;
	private String packageName = "logging";
	private LogDispatcher dispatcher;
	private String newLog = "";
	private Set<Listener> listeners;
	/**
	 * Only logs events that are of this level or higher
	 */
	public static LoggingLevel threshold = LoggingLevel.ALL;

	/**
	 * Logs the provided error. Attempts to use localized names for the error
	 * code and logging level. This only logs errors that are above or equal to
	 * the threshold. The package name is listed before the info. <br>
	 * If the package is not enabled, simply logs straight to System.err
	 * 
	 * @param origin The package that is logging the error
	 * @param error The error that occurred
	 * @param level what level is the requested log
	 * @param details additional information about the error
	 */
	private void logError(String origin, String error, LoggingLevel level,
			String details) {
		newLog = "";
		if (!isEnabled()) {
			System.err.println(level.getName() + " " + error + " " + details);
			return;
		}
		if (level.intValue() < threshold.intValue()) {
			return;
		}

		newLog =
				SafeResourceLoader.getString("level_prefix", resourceBundle,
						"[")
						+ level.getLocalizedName()
						+ SafeResourceLoader.getString("level_postfix",
								resourceBundle, "]")
						+ " "
						+ SafeResourceLoader.getString("name_prefix",
								resourceBundle, "<")
						+ origin
						+ SafeResourceLoader.getString("name_postfix",
								resourceBundle, ">")
						+ " "
						+ error
						+ " "
						+ details;

		dispatcher.log(newLog);

	}

	/**
	 * Logs the provided information. Attempts to use localized names for the
	 * logging level. This only logs information that is above or equal to the
	 * logging threshold. <br>
	 * If the package is not enabled, simply logs straight to System.out
	 * 
	 * @param origin The package that is logging the info
	 * @param level what level is the requested log
	 * @param details what to log
	 */
	private void log(String origin, LoggingLevel level, String details) {
		newLog = "";
		if (!isEnabled()) {
			System.out.println(level.getName() + " " + details);
			return;
		}
		if (level.intValue() < threshold.intValue()) {
			return;
		}
		try {
			newLog =
					SafeResourceLoader.getString("level_prefix",
							resourceBundle, "[")
							+ level.getLocalizedName()
							+ SafeResourceLoader.getString("level_postfix",
									resourceBundle, "]")
							+ " "
							+ SafeResourceLoader.getString("name_prefix",
									resourceBundle, "<")
							+ origin
							+ SafeResourceLoader.getString("name_postfix",
									resourceBundle, ">") + " " + details;
		}
		catch (Exception e) {
			System.err.println(level.getName());
			System.err.println(details);
			e.printStackTrace(System.err);// we need to know what broke the log
		}
		dispatcher.log(newLog);
	}

	@Override
	public String getName() {
		return packageName;
	}

	@Override
	public double getVersion() {
		return version;
	}

	@Override
	public boolean enable() {
		setPackageState(PackageState.ENABLING);
		try {
			this.onEnable();
		}
		catch (Exception e) {
			logError(packageName, SafeResourceLoader.getString(
					"package_enable_fail",
					"com.ikalagaming.packages.resources.PackageManager",
					"Package failed to enable"), LoggingLevel.SEVERE,
					"LoggingPackage.enable()");
			// better safe than sorry (probably did not initialize correctly)
			setPackageState(PackageState.CORRUPTED);
			return false;
		}
		return true;
	}

	@Override
	public boolean disable() {
		setPackageState(PackageState.DISABLING);
		try {
			this.onDisable();
		}
		catch (Exception e) {
			logError(packageName, SafeResourceLoader.getString(
					"package_disable_fail",
					"com.ikalagaming.packages.resources.PackageManager",
					"Package failed to disable"), LoggingLevel.SEVERE,
					"LoggingPackage.enable()");
			setPackageState(PackageState.CORRUPTED);
			return false;
		}
		return true;
	}

	@Override
	public boolean reload() {
		setPackageState(PackageState.UNLOADING);
		if (getPackageState() == PackageState.ENABLED) {
			disable();
		}
		this.resourceBundle = null;
		setPackageState(PackageState.LOADING);
		onLoad();
		enable();// it will start up enabled
		return true;
	}

	@Override
	public boolean isEnabled() {
		if (getPackageState() == PackageState.ENABLED) {
			return true;
		}
		return false;
	}

	@Override
	public void onEnable() {
		setPackageState(PackageState.ENABLED);
	}

	@Override
	public void onDisable() {
		setPackageState(PackageState.DISABLED);
	}

	@Override
	public void onLoad() {
		setPackageState(PackageState.LOADING);
		try {
			resourceBundle =
					ResourceBundle.getBundle(
							"com.ikalagaming.logging.resources.LoggingPackage",
							Localization.getLocale());
		}
		catch (MissingResourceException missingResource) {
			// TODO Attempt to localize this somehow
			logError(packageName, "locale not found", LoggingLevel.SEVERE,
					"LoggingPackage.onLoad()");
		}
		dispatcher = new LogDispatcher();
		dispatcher.start();
		setPackageState(PackageState.DISABLED);
	}

	@Override
	public void onUnload() {
		setPackageState(PackageState.UNLOADING);
		this.resourceBundle = null;
		setPackageState(PackageState.PENDING_REMOVAL);
	}

	@Override
	public Set<Listener> getListeners() {
		if (listeners == null) {
			listeners = new HashSet<Listener>();
			listeners.add(this);
		}
		return listeners;
	}

	@Override
	public PackageState getPackageState() {
		synchronized (state) {
			return state;
		}
	}

	@Override
	public void setPackageState(PackageState newState) {
		synchronized (state) {
			state = newState;
		}
	}

	/**
	 * Logs the provided information. Attempts to use localized names for the
	 * logging level. This only logs information that is above or equal to the
	 * logging threshold. <br>
	 * If the package is not enabled, simply logs straight to System.out
	 * 
	 * @param event the Event to record
	 */
	@EventHandler
	public void onLogEvent(Log event) {
		log(event.getSender(), event.getLevel(), event.getDetails());
	}

	/**
	 * Logs the provided error. Attempts to use localized names for the error
	 * code and logging level. This only logs errors that are above or equal to
	 * the threshold. The package name is listed before the info. <br>
	 * If the package is not enabled, simply logs straight to System.err
	 * 
	 * @param event the Event to record
	 */
	@EventHandler
	public void onLogErrorEvent(LogError event) {
		logError(event.getSender(), event.getError(), event.getLevel(),
				event.getDetails());
	}

}
