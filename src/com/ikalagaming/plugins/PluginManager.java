package com.ikalagaming.plugins;

import com.ikalagaming.event.Event;
import com.ikalagaming.event.EventManager;
import com.ikalagaming.event.Listener;
import com.ikalagaming.localization.Localization;
import com.ikalagaming.logging.Logging;
import com.ikalagaming.plugins.events.PluginDisabled;
import com.ikalagaming.plugins.events.PluginEnabled;
import com.ikalagaming.plugins.events.PluginLoaded;
import com.ikalagaming.plugins.events.PluginUnloaded;
import com.ikalagaming.util.SafeResourceLoader;

import com.github.zafarkhaja.semver.Version;
import lombok.CustomLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Handles loading, unloading and storage of plugins. This is considered a
 * plugin, but is always enabled and never loaded.
 *
 * @author Ches Burks
 *
 */
@CustomLog(topic = PluginManager.PLUGIN_NAME)
public class PluginManager {

	/**
	 * The name of the core system, since it is not technically a plugin.
	 */
	public static final String PLUGIN_NAME = "Ikala-Core";

	private static PluginManager instance;

	/**
	 * Compare the order of version strings, as if using
	 * first.compareTo(second). This should be string semantic versions and can
	 * handle build info and the like but build metadata is ignored.
	 *
	 * For example, compareVersions("1.0.0-rc.1+build.1",
	 * "1.3.7+build.2.b8f12d7") would return a number less than 0.
	 * compareVersions("2.1.3", "1.0.1") would return a number greater than 0.
	 * compareVersions("1.0.0+build.1", "1.0.0+build.1") would return 0 because
	 * they are equal builds.
	 *
	 * @param first The first version string
	 * @param second The second version string
	 * @return the value of comparing the two versions.
	 */
	public final static int compareVersions(final String first,
		final String second) {
		return Version.valueOf(first).compareTo(Version.valueOf(second));
	}

	/**
	 * Shuts down the static instance (unregisters commands and unloads plugins)
	 * if it exists, and then nullifies the reference to it. This exists in case
	 * you wish to use your own instances of the Plugin Manager and not use the
	 * single static instance provided. If the instance does not exist, nothing
	 * happens. Note that a new static instance may be created if the instance
	 * is requested later.
	 *
	 * @see #getInstance()
	 * @see #getInstance(EventManager)
	 */
	public static void destoryInstance() {
		if (PluginManager.instance == null) {
			return;
		}

		PluginManager.instance.shutdown();
		PluginManager.instance = null;
	}

	/**
	 * Returns the static instance of the plugin manager. Since there should
	 * only be one of these, having a static instance is fine and any class can
	 * get the instance which all other classes should share. If there is no
	 * instance yet, one will be created.
	 *
	 * @return the static instance of the Plugin Manager
	 * @see #getInstance(EventManager)
	 * @see PluginManager#destoryInstance()
	 */
	public static PluginManager getInstance() {
		if (PluginManager.instance == null) {
			PluginManager.instance =
				new PluginManager(EventManager.getInstance());
		}
		return PluginManager.instance;
	}

	/**
	 * Returns the static instance of the plugin manager. Since there should
	 * only be one of these, having a static instance is fine and any class can
	 * get the instance which all other classes should share. If there is no
	 * instance yet, one will be created.
	 *
	 * @param manager the event manager to use for the static instance
	 *
	 * @return the static instance of the Plugin Manager
	 * @see #getInstance()
	 * @see #destoryInstance()
	 */
	public static PluginManager getInstance(EventManager manager) {
		if (PluginManager.instance == null) {
			PluginManager.instance = new PluginManager(manager);
		}
		return PluginManager.instance;
	}

	/**
	 * Returns true if the first string is a strictly newer version than the
	 * second, where both are semantic version strings.
	 *
	 * For example, isNewerVersion("1.2.3", "0.0.1") returns true.
	 * isNewerVersion("1.0.0+build.1", "1.0.2") returns false.
	 * isNewerVersion("1.0.0+build.1", "1.0.0") returns false because it is the
	 * same.
	 *
	 * @param toCheck The string that is unknown relative to the known version.
	 * @param existing The existing version that is known but could be outdated.
	 * @return true if the first version is newer than the second, otherwise
	 *         false.
	 */
	public final static boolean isNewerVersion(final String toCheck,
		final String existing) {
		return Version.valueOf(toCheck).greaterThan(Version.valueOf(existing));
	}

	private EventManager eventManager;

	/**
	 * Maps strings to info about plugins loaded in memory
	 */
	private Map<String, PluginDetails> loadedPlugins;

	/**
	 * Stores a list of names for convenience.
	 */
	private Map<Plugin, String> pluginNames;

	/**
	 * Lock for plugin related maps
	 */
	private ReentrantLock pluginLock;

	/**
	 * Stores all the classes loaded by plugins. Keys are the unique class
	 * names.
	 */
	private final Map<String, Class<?>> pluginClasses;

	private ResourceBundle resourceBundle;

	/**
	 * If plugins should be enabled by the plugin manager when they are loaded.
	 * If this is false then plugins must be enabled manually after they are
	 * loaded.
	 */
	private boolean enableOnLoad;

	/**
	 * Lock for enableOnLoad
	 */
	private ReentrantLock enableLock;

	/**
	 * A list of all of the commands registered. This list is sorted.
	 */
	private ArrayList<PluginCommand> commands;

	private ReentrantLock commandLock;

	private MiscLoggingListener logListener;

	private final String PLUGIN_CONFIG_FILENAME = "plugin.yml";

	/**
	 * Constructs a new {@link PluginManager} and initializes variables.
	 *
	 * @param evtManager The event manager to use for the plugin system
	 */
	public PluginManager(EventManager evtManager) {
		this.pluginLock = new ReentrantLock();
		this.commandLock = new ReentrantLock();
		this.enableLock = new ReentrantLock();
		this.enableOnLoad = false;
		this.eventManager = evtManager;
		this.loadedPlugins = Collections.synchronizedMap(new HashMap<>());
		this.pluginClasses = Collections.synchronizedMap(new HashMap<>());
		this.pluginNames = Collections.synchronizedMap(new HashMap<>());
		this.resourceBundle = ResourceBundle.getBundle(
			"com.ikalagaming.plugins.resources.PluginManager",
			Localization.getLocale());
		this.logListener = new MiscLoggingListener();

		this.commands = new ArrayList<>();

		Logging.create();

		this.registerCommands();
		this.eventManager.registerEventListeners(logListener);
	}

	private void alertMissingArgs() {
		String tmp = SafeResourceLoader.getString("COMMAND_ARG_MISSING",
			this.getResourceBundle());
		log.warning(tmp);
	}

	private void cbDisable(String[] args) {
		if (args.length < 1) {
			alertMissingArgs();
			return;
		}
		disable(args[0]);
	}

	private void cbEnable(String[] args) {
		if (args.length < 1) {
			alertMissingArgs();
			return;
		}
		enable(args[0]);
	}

	private void cbLoad(String[] args) {
		if (args.length < 1) {
			alertMissingArgs();
			return;
		}

		String tmp =
			SafeResourceLoader.getString("WIP_TEXT", this.getResourceBundle());
		log.warning(tmp);
		// TODO loading
		// loadPlugin(System.getProperty("user.dir"), args[0]);
	}

	private void cbReload(String[] args) {
		if (args.length < 1) {
			alertMissingArgs();
			return;
		}
		Optional<Plugin> plug = getPlugin(args[0]);
		if (!plug.isPresent()) {
			return;
		}
		reload(plug.get());
	}

	private void cbUnload(String[] args) {
		if (args.length < 1) {
			alertMissingArgs();
			return;
		}
		unloadPlugin(args[0]);
	}

	/**
	 * Unregisters all commands
	 */
	public void clearCommands() {
		this.commandLock.lock();
		try {
			ArrayList<String> cmds = new ArrayList<>();
			for (PluginCommand s : this.commands) {
				cmds.add(s.getCommand());
			}
			for (String s : cmds) {
				this.unregisterCommand(s);
			}
			this.commands.clear();
		}
		finally {
			this.commandLock.unlock();
		}
	}

	/**
	 * Returns true if the array contains the given string.
	 *
	 * @param s the string to look for
	 * @return true if the string exists
	 */
	public boolean containsCommand(final String s) {
		int i;
		boolean res = false;
		this.commandLock.lock();
		try {
			for (i = 0; i < this.commands.size(); ++i) {
				if (this.commands.get(i).getCommand().equalsIgnoreCase(s)) {
					res = true;
					break;
				}
			}
		}
		finally {
			this.commandLock.unlock();
		}
		return res;
	}

	/**
	 * Deactivates the plugin and halts all of its operations. The plugin is
	 * still loaded in memory but not active. Calls {@link Plugin#onDisable()}.
	 * This changes the plugin state to {@link PluginState#DISABLING DISABLING}.
	 * The plugin state is changed to {@link PluginState#DISABLED DISABLED}
	 * after completion. If {@link Plugin#onDisable()} returns false (failed),
	 * the plugin state is set to {@link PluginState#CORRUPTED CORRUPTED}.
	 *
	 * @param target the plugin to disable
	 *
	 * @return true if the plugin has been successfully disabled, false if there
	 *         was a problem
	 */
	public boolean disable(Plugin target) {
		this.setPluginState(target, PluginState.DISABLING);

		logAlert("ALERT_DISABLING", this.pluginNames.get(target));

		boolean success = target.onDisable();
		if (success) {
			this.setPluginState(target, PluginState.DISABLED);
			PluginDisabled packDisabled =
				new PluginDisabled(this.pluginNames.get(target));
			this.fireEvent(packDisabled);
		}
		else {
			this.setPluginState(target, PluginState.CORRUPTED);
			logStateCorrupted(target);
		}

		return success;
	}

	/**
	 * Deactivates the plugin and halts all of its operations. The plugin is
	 * still loaded in memory but not active. Calls {@link Plugin#onDisable()}.
	 * This changes the plugins state to {@link PluginState#DISABLING
	 * DISABLING}. The plugin state is changed to {@link PluginState#DISABLED
	 * DISABLED} after completion. If {@link Plugin#onDisable()} returns false
	 * (failed), the plugin state is set to {@link PluginState#CORRUPTED
	 * CORRUPTED}.
	 *
	 * @param target the name of the plugin to disable
	 *
	 * @return true if the plugin has been successfully disabled, false if there
	 *         was a problem
	 */
	public boolean disable(final String target) {
		Optional<Plugin> p;
		this.pluginLock.lock();
		try {
			p = this.getPlugin(target);
		}
		finally {
			this.pluginLock.unlock();
		}

		if (!p.isPresent()) {
			return false;
		}
		Plugin plugin = p.get();

		boolean disabled = this.disable(plugin);

		if (!disabled) {
			this.setPluginState(plugin, PluginState.CORRUPTED);
			logStateCorrupted(plugin);
		}
		return disabled;
	}

	/**
	 * Activates the plugin and enables it to perform its normal functions.
	 * Calls {@link Plugin#onEnable()}. This changes the plugin state to
	 * {@link PluginState#ENABLING ENABLING}. The plugin state is changed to
	 * {@link PluginState#ENABLED ENABLED} after completion. If
	 * {@link Plugin#onEnable()} returns false (failed), the plugin state is set
	 * to {@link PluginState#CORRUPTED CORRUPTED}.
	 *
	 * @param target The plugin to enable
	 *
	 * @return true if the plugin was successfully enabled, false if there was a
	 *         problem
	 */
	public boolean enable(Plugin target) {
		this.setPluginState(target, PluginState.ENABLING);

		logAlert("ALERT_ENABLING", this.pluginNames.get(target));

		boolean success = target.onEnable();
		if (success) {
			this.setPluginState(target, PluginState.ENABLED);
			PluginEnabled plugEnabled =
				new PluginEnabled(this.pluginNames.get(target));
			this.fireEvent(plugEnabled);
		}
		else {
			this.setPluginState(target, PluginState.CORRUPTED);
			logStateCorrupted(target);
		}

		return success;
	}

	/**
	 * Activates the plugin and enables it to perform its normal functions.
	 * Calls {@link Plugin#onEnable()}. This changes the plugin state to
	 * {@link PluginState#ENABLING ENABLING}. The plugin state is changed to
	 * {@link PluginState#ENABLED ENABLED} after completion. If
	 * {@link Plugin#onEnable()} returns false (failed), the plugin state is set
	 * to {@link PluginState#CORRUPTED CORRUPTED}.
	 *
	 * @param target The name of the plugin to enable
	 *
	 * @return true if the plugin was successfully enabled, false if there was a
	 *         problem
	 */
	public boolean enable(final String target) {
		Optional<Plugin> p;
		this.pluginLock.lock();
		try {
			p = this.getPlugin(target);
		}
		finally {
			this.pluginLock.unlock();
		}

		if (!p.isPresent()) {
			return false;
		}

		Plugin plugin = p.get();

		boolean enabled = this.enable(plugin);

		if (!enabled) {
			this.setPluginState(plugin, PluginState.CORRUPTED);
		}

		return enabled;
	}

	/**
	 * Returns true if this plugin manager automatically enables plugins when
	 * they are loaded. If they are not enabled on load, they must be enabled
	 * manually after being loaded.
	 *
	 * @return true if the plugins enable after loading, false if they must be
	 *         manually enabled
	 * @see #setEnableOnLoad(boolean)
	 */
	public boolean enableOnLoad() {
		this.enableLock.lock();
		try {
			return this.enableOnLoad;
		}
		finally {
			this.enableLock.unlock();
		}
	}

	/**
	 * Load up the plugin info from the given jar file and return it. If there
	 * is some error like the file not actually being a jar or plugin info
	 * missing, then the returned optional is empty.
	 *
	 * @param jar the jar to load info from.
	 * @return an optional containing the plugin info, or an empty optional on
	 *         failure.
	 */
	public Optional<PluginInfo> extractPluginInfo(final File jar) {
		JarFile jfile;
		ZipEntry config;

		final String fileName = jar.getName();

		/*
		 * Check for being a jar file check for plugin info file load and check
		 * for valid info load the file if necessary
		 */
		try {
			jfile = new JarFile(jar);
			config = jfile.getEntry(PLUGIN_CONFIG_FILENAME);
			if (config == null) {
				String msg = SafeResourceLoader
					.getString("PLUGIN_CONFIG_MISSING", this.resourceBundle)
					.replaceFirst("\\$FILE", PLUGIN_CONFIG_FILENAME);
				log.warning(msg);
				jfile.close();
				return Optional.empty();
			}
		}
		catch (Exception e) {
			String msg = SafeResourceLoader
				.getString("PLUGIN_JAR_ERROR", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", fileName);
			log.warning(msg);
			return Optional.empty();
		}

		// grab an input stream for the configuration file
		InputStream configIStream;
		try {
			configIStream = jfile.getInputStream(config);
		}
		catch (IOException e1) {
			String msg = SafeResourceLoader
				.getString("PLUGIN_CONFIG_READ_ERROR", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", fileName);
			log.warning(msg);
			try {
				jfile.close();
			}
			catch (IOException e) {
				String errorMsg = SafeResourceLoader
					.getString("PLUGIN_JAR_CLOSE_ERROR", this.resourceBundle)
					.replaceFirst("\\$PLUGIN", fileName);
				log.warning(errorMsg);
				// wow we really must have messed up
			}
			return Optional.empty();
		}

		// Load in the plugin info from the config file
		PluginInfo info = null;
		try {
			info = new PluginInfo(configIStream);
		}
		catch (InvalidDescriptionException e1) {
			String msg = SafeResourceLoader
				.getString("PLUGIN_INVALID_DESCRIPTION", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", fileName);
			log.warning(msg);
			log.warning(e1.getMessage());
			try {
				jfile.close();
			}
			catch (IOException e) {
				String errorMsg = SafeResourceLoader
					.getString("PLUGIN_JAR_CLOSE_ERROR", this.resourceBundle)
					.replaceFirst("\\$PLUGIN", fileName);
				log.warning(errorMsg);
			}
			return Optional.empty();
		}

		try {
			jfile.close();// We already loaded in the config info
		}
		catch (IOException e1) {
			String errorMsg = SafeResourceLoader
				.getString("PLUGIN_JAR_CLOSE_ERROR", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", fileName);
			log.warning(errorMsg);
		}
		return Optional.ofNullable(info);
	}

	/**
	 * Fires an event with a message to a plugin type from the plugin manager.
	 * If an error occurs, this will return false. The event should not have
	 * been sent if false was returned.
	 *
	 * @param event the event to fire
	 *
	 * @return true if the event was fired correctly
	 */
	public boolean fireEvent(Event event) {
		if (this.eventManager == null) {
			String err = SafeResourceLoader
				.getString("PLUGIN_NOT_LOADED", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", PluginManager.PLUGIN_NAME);
			System.err.println(err);
			return false;
		}

		if (event != null) {// just in case the assignment failed
			this.eventManager.fireEvent(event);
		}

		return true;
	}

	/**
	 * Returns a clone of the commands list.
	 *
	 * @return a copy of the stored list
	 */
	public ArrayList<PluginCommand> getCommands() {
		ArrayList<PluginCommand> cmds = new ArrayList<>();
		this.commandLock.lock();
		try {
			for (PluginCommand pc : this.commands) {
				cmds.add(pc);
			}
		}
		finally {
			this.commandLock.unlock();
		}
		return cmds;
	}

	/**
	 * Returns the index of the given string if it exists. If it is not in the
	 * array, returns -1.
	 *
	 * @param s the string to look for
	 * @return the index of the string
	 */
	private int getIndexOfCommand(String s) {
		int i;
		boolean found = false;
		this.commandLock.lock();
		try {
			for (i = 0; i < this.commands.size(); ++i) {
				if (this.commands.get(i).getCommand().equalsIgnoreCase(s)) {
					found = true;
					break;
				}
			}
		}
		finally {
			this.commandLock.unlock();
		}
		if (!found) {
			i = -1;
		}
		return i;
	}

	/**
	 * Get the plugin info for a given plugin. If no info exists for the
	 * specified plugin, an empty optinal is returned.
	 *
	 * @param target The plugin to get info from.
	 * @return The info for the specified plugin.
	 */
	public Optional<PluginInfo> getInfo(Plugin target) {
		this.pluginLock.lock();
		try {
			if (!pluginNames.containsKey(target)) {
				return Optional.empty();
			}
			return getInfo(pluginNames.get(target));
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * Get the plugin info for a given plugin. If no info exists for the
	 * specified plugin, an empty optinal is returned.
	 *
	 * @param target The name of the plugin to get info from.
	 * @return The info for the specified plugin.
	 */
	public Optional<PluginInfo> getInfo(String target) {
		this.pluginLock.lock();
		try {
			PluginDetails details = this.loadedPlugins.get(target);
			if (details == null) {
				return Optional.empty();
			}
			return Optional.ofNullable(details.getInfo());
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * Returns the map of plugin name to Plugin of the currently loaded plugins.
	 *
	 * @return the plugin map
	 */
	public HashMap<String, Plugin> getLoadedPlugins() {
		HashMap<String, Plugin> ret = new HashMap<>();
		this.pluginLock.lock();
		try {
			loadedPlugins.forEach((key, value) -> {
				ret.put(key, value.getPlugin());
			});
		}
		finally {
			this.pluginLock.unlock();
		}
		return ret;
	}

	/**
	 * If a plugin of the given type exists ({@link #isLoaded(String)}), then
	 * the plugin that is of that type is returned. Otherwise, an empty optional
	 * is returned.
	 *
	 * @param type The plugin type
	 * @return The Plugin with the given type
	 */
	public Optional<Plugin> getPlugin(final String type) {
		this.pluginLock.lock();
		try {
			PluginDetails details = this.loadedPlugins.get(type);
			if (details == null) {
				return Optional.empty();
			}
			return Optional.ofNullable(details.getPlugin());
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * Returns the {@link PluginState current state} of the plugin.
	 *
	 * @param target The plugin to fetch the state of
	 *
	 * @return a PluginState representing the status of this plugin
	 */
	public PluginState getPluginState(Plugin target) {
		if (target == null) {
			return PluginState.NOT_LOADED;
		}
		this.pluginLock.lock();
		try {
			return getPluginState(pluginNames.get(target));
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * Returns the {@link PluginState current state} of the plugin.
	 *
	 * @param target The name of the plugin to fetch the state of
	 *
	 * @return a PluginState representing the status of this plugin
	 */
	public PluginState getPluginState(String target) {
		if (target == null) {
			return PluginState.NOT_LOADED;
		}
		this.pluginLock.lock();
		try {
			if (!this.loadedPlugins.containsKey(target)) {
				return PluginState.NOT_LOADED;
			}
			return this.loadedPlugins.get(target).getState();
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * Returns the resource bundle for the plugin manager. This is not safe and
	 * could be null.
	 *
	 * @return the current resource bundle
	 */
	public ResourceBundle getResourceBundle() {
		return this.resourceBundle;
	}

	/**
	 * Returns true if the plugin is enabled, and false otherwise. A state of
	 * {@link PluginState#ENABLED ENABLED} returns true, any other states will
	 * return false. Plugins that are not loaded will return false.
	 *
	 * @param target The plugin to check for enabled status
	 *
	 * @return true if the plugin is fully ready to operate
	 */
	public boolean isEnabled(Plugin target) {
		if (target == null) {
			return false;
		}
		this.pluginLock.lock();
		try {
			return isEnabled(this.pluginNames.get(target));
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * Returns true if the plugin is enabled, and false otherwise. A state of
	 * {@link PluginState#ENABLED ENABLED} returns true, any other states will
	 * return false. Plugins that are not loaded will return false.
	 *
	 * @param target The name of plugin to check for enabled status
	 *
	 * @return true if the plugin is fully ready to operate
	 */
	public boolean isEnabled(final String target) {
		if (target == null) {
			return false;
		}
		this.pluginLock.lock();
		try {
			if (!this.isLoaded(target)) {
				return false;
			}
			PluginState state = this.getPluginState(target);
			if (PluginState.ENABLED.equals(state)) {
				return true;
			}
		}
		finally {
			this.pluginLock.unlock();
		}
		return false;
	}

	/**
	 * Returns true if a plugin exists that has the same type as the provided
	 * plugin (for example: "Graphics").
	 *
	 * @param type the plugin type
	 * @return true if the plugin is loaded in memory, false if it does not
	 *         exist
	 */
	public boolean isLoaded(Plugin type) {
		if (type == null) {
			return false;
		}
		this.pluginLock.lock();
		try {
			if (!this.pluginNames.containsKey(type)) {
				return false;
			}
			return isLoaded(this.pluginNames.get(type));
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * Returns true if a plugin exists with the given type (for example:
	 * "Graphics")'. Most existing states count as loaded, but the following do
	 * not:
	 * <ul>
	 * <li>{@link PluginState#UNLOADING}</li>
	 * <li>{@link PluginState#PENDING_REMOVAL}</li>
	 * <li>{@link PluginState#NOT_LOADED}</li>
	 * </ul>
	 *
	 * @param type The plugin name
	 * @return true If the plugin is loaded in memory, and not actively being
	 *         unloaded/removed
	 */
	public boolean isLoaded(final String type) {
		if (type == null) {
			return false;
		}
		this.pluginLock.lock();
		try {
			PluginDetails details = this.loadedPlugins.get(type);
			if (details == null) {
				return false;
			}
			switch (details.getState()) {
				case CORRUPTED:
				case DEPS_CHECKING:
				case DEPS_MISSING:
				case DEPS_SATISFIED:
				case DISABLED:
				case DISABLING:
				case DISCOVERED:
				case ENABLED:
				case ENABLING:
				case LOADING:
					return true;
				default:
				case UNLOADING:
				case PENDING_REMOVAL:
				case NOT_LOADED:
					return false;
			}
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	/**
	 * <p>
	 * Loads the given plugin into memory, stores it by type, and enables it if
	 * plugins are enabled on load by default.
	 * </p>
	 * <p>
	 * If the type of plugin already exists in the manager, and the new plugin
	 * has a higher version number, then the old plugin is unloaded and the new
	 * one is loaded in its place. If the versions are equal, or the new plugin
	 * is older, then it does not load the new version and returns false.
	 * </p>
	 *
	 * @see PluginManager#enableOnLoad()
	 * 
	 * @deprecated This is getting phased out, all plugins will be loaded from
	 *             file
	 *
	 * @param toLoad the plugin to load
	 * @param info The plugin info to use while loading a plugin
	 * @return true if the plugin was loaded properly, false otherwise
	 */
	@Deprecated
	public boolean loadPlugin(Plugin toLoad, PluginInfo info) {

		final String pluginName = info.getName();
		final String pluginVersion = info.getVersion();

		logAlert("ALERT_LOADING", pluginName);

		// if the plugin exists and is older than toLoad, unload
		if (this.isLoaded(toLoad)) {

			logAlert("ALERT_PLUGIN_ALREADY_LOADED", pluginName);

			boolean lowerVersion = false;
			this.pluginLock.lock();
			try {
				// TODO real version checking
				lowerVersion = isNewerVersion(pluginVersion, "0.0.0");

			}
			finally {
				this.pluginLock.unlock();
			}
			if (lowerVersion) {
				this.unloadPlugin(pluginName);
				// unload the old plugin and continue loading the new one
			}
			else {
				logAlert("ALERT_PLUGIN_OUTDATED", pluginName);
				return false;
			}
		}
		this.setPluginState(toLoad, PluginState.LOADING);

		this.pluginLock.lock();
		try {
			// store the new plugin
			this.pluginNames.put(toLoad, pluginName);
			PluginDetails details =
				new PluginDetails(null, null, toLoad, PluginState.CORRUPTED);
			this.loadedPlugins.put(pluginName, details);
		}
		finally {
			this.pluginLock.unlock();
		}

		for (Listener l : toLoad.getListeners()) {
			this.eventManager.registerEventListeners(l);
		}
		String msg = SafeResourceLoader
			.getString("ALERT_REG_EVENT_LISTENERS", this.resourceBundle)
			.replaceFirst("\\$PLUGIN", pluginName);
		log.finer(msg);

		// load it
		toLoad.onLoad();

		this.setPluginState(toLoad, PluginState.DISABLED);
		PluginLoaded packLoaded = new PluginLoaded(pluginName);
		this.fireEvent(packLoaded);

		return true;
	}

	/**
	 * Find the class by class name. Checks for cached versions, but will search
	 * through known plugin class loaders to find it. If it cannot be found,
	 * returns null.
	 * 
	 * @param name The name of the class to look for.
	 * @return The class by the given name, or null if not found.
	 */
	Class<?> getClassByName(final String name) {
		Class<?> cachedClass = pluginClasses.get(name);

		if (cachedClass != null) {
			return cachedClass;
		}
		for (String pluginName : this.loadedPlugins.keySet()) {
			PluginClassLoader loader =
				this.loadedPlugins.get(pluginName).getClassLoader();
			try {
				cachedClass = loader.findClass(name, false);
			}
			catch (ClassNotFoundException e) {
			}
			if (cachedClass != null) {
				return cachedClass;
			}
		}
		return null;
	}

	/**
	 * Stores the class by name in the cached class list.
	 * 
	 * @param name The name of the class.
	 * @param clazz The corresponding class object.
	 */
	void setClass(final String name, final Class<?> clazz) {
		if (!pluginClasses.containsKey(name)) {
			pluginClasses.put(name, clazz);
		}
	}

	/**
	 * Removes the class by name in the cached class list.
	 * 
	 * @param name The name of the class.
	 */
	void removeClass(final String name) {
		pluginClasses.remove(name);
	}

	/**
	 * Load all the plugins from .jar files that are located in the given
	 * folder.
	 * 
	 * @param folder The folder that contains all the jar files we want to load.
	 */
	public void loadAllPlugins(String folder) {

		File pluginFolder = null;
		{// keeps scope name space clean
			Optional<File> folderMaybe = this.plGetFolder(folder);
			if (!folderMaybe.isPresent()) {
				// TODO log this
				return;
			}
			pluginFolder = folderMaybe.get();
		}

		// find all the jars
		ArrayList<File> jars = this.plGetAllJars(pluginFolder);

		Map<File, PluginInfo> jarInfoMap = new HashMap<>();

		// grab all the plugin info from them, discard invalid plugins
		for (File jarFile : jars) {
			Optional<PluginInfo> info = extractPluginInfo(jarFile);
			if (!info.isPresent()) {
				// We don't have a valid plugin
				// TODO log this
				continue;
			}
			jarInfoMap.put(jarFile, info.get());
		}
		// Unload/dereference any files that we don't care about anymore
		jars.removeIf((file) -> {
			return !jarInfoMap.containsKey(file);
		});

		// load the plugins into memory with plugin info, DISCOVERED status
		for (File jarFile : jarInfoMap.keySet()) {
			PluginInfo info = jarInfoMap.get(jarFile);

			PluginClassLoader loader = null;
			try {
				loader = new PluginClassLoader(this,
					getClass().getClassLoader(), info, jarFile);
			}
			catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InvalidPluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (loader == null) {
				jarInfoMap.remove(jarFile);
				jars.remove(jarFile);
				// TODO log this problem
				continue;
			}

			PluginDetails details = new PluginDetails(loader, info,
				loader.plugin, PluginState.DISCOVERED);

			this.loadedPlugins.put(info.getName(), details);
			this.pluginNames.put(loader.plugin, info.getName());

			// setPluginState(loader.plugin, PluginState.DISCOVERED);
			// TODO log discovered plugin
		}
		/*
		 * Resolve easy dependencies. Loop through all plugins once, if all
		 * dependencies are satisfied (DISCOVERED, loaded/enabled,
		 * DEPS_SATISFIED, generally existing in the system), or it has no
		 * dependencies, mark it as DEPS_SATISFIED as well. If a plugin has
		 * unsatisfied dependencies, such as something NOT_LOADED, mark the
		 * plugin as DEPS_MISSING. If a plugin does not have missing
		 * dependencies, but it has dependencies that are also DEPS_CHECKING,
		 * mark it as DEPS_CHECKING.
		 */

		/*
		 * Handle loops/clusters with BFS. Go through all nodes still
		 * DEPS_CHECKING and: if all dependencies are DEPS_SATISFIED, the node
		 * is DEPS_SATISFIED. If there are dependencies not found, or
		 * DEPS_MISSING, the node and all parent nodes are set to DEPS_MISSING
		 * and we move to a new node. If a child node is DEPS_CHECKING, and not
		 * already in the tree, we tack it on and keep navigating down. Once we
		 * completely exhaust reachable nodes, and everything is DEPS_SATISFIED
		 * or DEPS_CHECKING, we can mark the whole tree as DEPS_SATISFIED.
		 */
		// Report and unload all DEPS_MISSING plugins
		/*
		 * All plugins should now be DEPS_SATISFIED, so load them all. During
		 * the onLoad() method, plugins should deal with connecting to plugins
		 * that may be in a dependency loop.
		 */
		/*
		 * After all plugins have been loaded, now they can be enabled (if that
		 * configuration is set). At this point problems with loops should have
		 * been resolved enough that the plugins can start in any order.
		 */

	}

	/**
	 * Loads a plugin by name from a folder.
	 *
	 * @param path the path to the folder containing the file
	 * @param name the filename to load from, without a file extension
	 * @return true on success, false if it failed
	 */
	@Deprecated
	public boolean loadPlugin(String path, String name) {

		File pluginFolder = null;
		{// keeps scope name space clean
			Optional<File> folderMaybe = this.plGetFolder(path);
			if (!folderMaybe.isPresent()) {
				return false;
			}
			pluginFolder = folderMaybe.get();
		}

		ArrayList<File> jars = this.plGetAllJars(pluginFolder);

		File jarFile = null;

		{// keeps scope name space clean
			Optional<File> jarMaybe = this.plPickJarByName(jars, name);
			if (!jarMaybe.isPresent()) {
				return false;
			}
			jarFile = jarMaybe.get();
		}

		PluginInfo info = null;

		{// keeps scope name space clean
			Optional<PluginInfo> infoMaybe = this.extractPluginInfo(jarFile);
			if (!infoMaybe.isPresent()) {
				return false;
			}
			info = infoMaybe.get();
		}

		ClassLoader loader;
		try {
			loader =
				URLClassLoader.newInstance(new URL[] {jarFile.toURI().toURL()},
					this.getClass().getClassLoader());
		}
		catch (MalformedURLException e) {
			String err = SafeResourceLoader
				.getString("PLUGIN_URL_INVALID", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", name);
			log.warning(err);
			return false;
		}

		Plugin p = null;
		// Optional<Plugin> pluginMaybe = loadPluginMainClass(loader, info);
		// if (pluginMaybe.isPresent()) {
		// p = pluginMaybe.get();
		// }
		// else {
		// return false;
		// }

		final String pluginName = info.getName();

		logAlert("ALERT_LOADING", pluginName);

		// if the plugin exists and is older than toLoad, unload
		if (this.isLoaded(pluginName)) {
			logAlert("ALERT_PLUGIN_ALREADY_LOADED", pluginName);

			boolean existingPluginOld = false;
			this.pluginLock.lock();
			try {
				Plugin oldPlugin = getPlugin(pluginName).get();
				Optional<PluginInfo> pInfo = getInfo(oldPlugin);
				if (!pInfo.isPresent()) {
					String err = SafeResourceLoader
						.getString("PLUGIN_INFO_MISSING", this.resourceBundle)
						.replaceFirst("\\$PLUGIN", pluginName);
					log.warning(err);
					return false;
				}
				existingPluginOld = PluginManager.isNewerVersion(
					info.getVersion(), pInfo.get().getVersion());
			}
			finally {
				this.pluginLock.unlock();
			}
			if (!existingPluginOld) {
				logAlert("ALERT_PLUGIN_OUTDATED", pluginName);
				return false;
			}

			this.unloadPlugin(pluginName);
		}

		this.setPluginState(p, PluginState.LOADING);

		this.pluginLock.lock();
		try {
			// store the new plugin
			this.pluginNames.put(p, pluginName);
			PluginDetails details =
				new PluginDetails(null, null, p, PluginState.CORRUPTED);
			this.loadedPlugins.put(pluginName, details);
		}
		finally {
			this.pluginLock.unlock();
		}

		// for (Listener l : p.getListeners()) {
		// this.eventManager.registerEventListeners(l);
		// }
		String msg = SafeResourceLoader
			.getString("ALERT_REG_EVENT_LISTENERS", this.resourceBundle)
			.replaceFirst("\\$PLUGIN", pluginName);
		log.finer(msg);

		// load it
		// p.onLoad();

		this.setPluginState(p, PluginState.DISABLED);
		PluginLoaded packLoaded = new PluginLoaded(pluginName);
		this.fireEvent(packLoaded);

		return true;

		// TODO finish this?
	}

	/**
	 * Log an alert about a plugin lifecycle, where plugin name and version are
	 * automatically replaced.
	 * 
	 * @param whichAlert The string to read from the resource bundle
	 * @param pluginName The plugin that the alert is about
	 */
	void logAlert(String whichAlert, String pluginName) {
		PluginDetails details = this.loadedPlugins.get(pluginName);
		String version;
		if (details == null || details.getInfo() == null) {
			version = "?";
		}
		else {
			version = details.getInfo().getVersion();
		}
		String message =
			SafeResourceLoader.getString(whichAlert, getResourceBundle())
				.replaceFirst("\\$PLUGIN", pluginName)
				.replaceFirst("\\$VERSION", version);
		log.fine(message);
	}

	/**
	 * Log the fact that a plugin had its state corrupted.
	 * 
	 * @param plugin The plugin that was corrupted.
	 */
	void logStateCorrupted(Plugin plugin) {
		PluginInfo pInfo = getInfo(plugin).get();
		String msgCorrupted = SafeResourceLoader
			.getString("PLUGIN_STATE_CORRUPTED", this.resourceBundle)
			.replaceFirst("\\$PLUIGN", pInfo.getName());
		log.warning(msgCorrupted);
	}

	/**
	 * Return all jar files in the specified folder. If none are found, an empty
	 * list is returned.
	 *
	 * @param folder a valid folder
	 * @return a list of all readable jars in the folder
	 */
	private ArrayList<File> plGetAllJars(final File folder) {
		File[] files;
		files = folder.listFiles();

		ArrayList<File> jars = new ArrayList<>();
		if (files == null) {
			String msg = SafeResourceLoader
				.getString("PLUGIN_FILES_NULL", this.resourceBundle)
				.replaceFirst("\\$FOLDER", folder.getAbsolutePath());
			log.warning(msg);
			return jars;
		}
		if (files.length == 0) {
			String msg = SafeResourceLoader
				.getString("PLUGIN_FOLDER_EMPTY", this.resourceBundle)
				.replaceFirst("\\$FOLDER", folder.getAbsolutePath());
			log.warning(msg);
			return jars;
		}

		for (File file : files) {
			try {
				if (!file.exists()) {
					continue;
				}
				if (!file.canRead()) {
					continue;
				}
				if (file.isDirectory()) {
					continue;
				}
				if (!file.getName().endsWith(".jar")) {
					continue;
				}
			}
			catch (SecurityException secExcep) {
				String msg = SafeResourceLoader
					.getString("PLUGIN_FILE_SECURITY_ERR", this.resourceBundle)
					.replaceFirst("\\$FILE", file.getName());
				log.fine(msg);
				// Maybe one or more files can't be read
				continue;
			}
			jars.add(file);
		}

		return jars;
	}

	/**
	 * For plugin loading. The return value is an empty optional if there was a
	 * problem accessing the folder from path. If there is a file in the
	 * optional it should be an existing folder that can be read.
	 *
	 * @param path the path of the folder to return as a File
	 * @return an optional containing the folder or empty if there was an error
	 */
	private Optional<File> plGetFolder(final String path) {
		File pluginFolder;
		try {
			pluginFolder = new File(path);
		}
		catch (NullPointerException nullExcept) {
			String msg = SafeResourceLoader.getString("PLUGIN_PATH_NULL",
				this.resourceBundle);
			log.warning(msg);
			return Optional.empty();
		}
		try {
			if (!pluginFolder.exists()) {
				String msg = SafeResourceLoader
					.getString("PLUGIN_FOLDER_NOT_FOUND", this.resourceBundle)
					.replaceFirst("\\$FOLDER", pluginFolder.getAbsolutePath());
				log.warning(msg);
				return Optional.empty();
			}
			if (!pluginFolder.isDirectory()) {
				String msg = SafeResourceLoader
					.getString("PLUGIN_FOLDER_NOT_FOLDER", this.resourceBundle)
					.replaceFirst("\\$FOLDER", pluginFolder.getAbsolutePath());
				log.warning(msg);
				return Optional.empty();
			}
			if (!pluginFolder.canRead()) {
				String msg = SafeResourceLoader
					.getString("PLUGIN_FOLDER_UNREADABLE", this.resourceBundle)
					.replaceFirst("\\$FOLDER", pluginFolder.getAbsolutePath());
				log.warning(msg);
				return Optional.empty();
			}
		}
		catch (SecurityException securExcep) {
			String msg = SafeResourceLoader
				.getString("PLUGIN_FOLDER_ACCESS_DENIED", this.resourceBundle)
				.replaceFirst("\\$FOLDER", path);
			log.warning(msg);
			return Optional.empty();
		}
		return Optional.of(pluginFolder);
	}

	/**
	 * Returns an optional that contains the jar file with the specified name,
	 * or an empty optional if none was found in the list of files. The file
	 * name is without an extension so "example.jar" should be passed as
	 * "example", and "test-1.0.2.jar" should be "test-1.0.2".
	 *
	 * @param files The list of files to look through, must all be valid
	 *            readable files.
	 * @param name the name of the jar, without a jar extension.
	 * @return an optional containing the matching jar, or an empty optional if
	 *         it was not found
	 */
	private Optional<File> plPickJarByName(final List<File> files,
		final String name) {
		Pattern fileNamePattern = Pattern.compile(name + "\\.jar\\z");
		Matcher fileNameMatcher = fileNamePattern.matcher("");

		for (File file : files) {
			try {
				if (fileNameMatcher.reset(file.getName()).matches()) {
					return Optional.of(file);
				}
			}
			catch (SecurityException secExcep) {
				String msg = SafeResourceLoader
					.getString("PLUGIN_FILE_SECURITY_ERR", this.resourceBundle)
					.replaceFirst("\\$FILE", file.getName());
				log.fine(msg);
				// Maybe one or more files can't be read
				continue;
			}
		}

		// It hasn't returned by now which means it couldn't find a match
		String msg = SafeResourceLoader
			.getString("PLUGIN_NOT_FOUND", this.resourceBundle)
			.replaceFirst("\\$PLUGIN", name);
		log.warning(msg);
		return Optional.empty();
	}

	/**
	 * Attempts to register the command for the given class. If the command
	 * already exists, an error is logged and the method returns false.
	 * 
	 * @param command the command to register
	 * @param callback The function to call when executing a command. Argument
	 *            list is passed in.
	 * @return true if the command registered successfully
	 * 
	 * @see #registerCommand(String, Consumer, Plugin)
	 */
	public boolean registerCommand(final String command,
		Consumer<String[]> callback) {
		return registerCommand(command, callback, null);
	}

	/**
	 * Attempts to register the command for the given class. If the command
	 * already exists, an error is logged and the method returns false.
	 * 
	 * @param command the command to register
	 * @param callback The function to call when executing a command. Argument
	 *            list is passed in.
	 * @param owner The owner of the plugin
	 * @return true if the command registered successfully
	 * 
	 * @see #registerCommand(String, Consumer)
	 */
	public boolean registerCommand(final String command,
		Consumer<String[]> callback, Plugin owner) {
		if (this.containsCommand(command)) {
			String msg = SafeResourceLoader
				.getString("COMMAND_ALREADY_REGISTERED",
					this.getResourceBundle())
				.replaceFirst("\\$COMMAND", command);
			log.warning(msg);
			return false;
		}

		PluginCommand cmd = new PluginCommand(command, callback, owner);
		this.commandLock.lock();
		try {
			this.commands.add(cmd);
		}
		finally {
			this.commandLock.unlock();
		}

		String msg = SafeResourceLoader
			.getString("REGISTERED_COMMAND", this.getResourceBundle())
			.replaceFirst("\\$COMMAND", command);
		log.finest(msg);

		this.commandLock.lock();
		try {
			java.util.Collections.sort(this.commands);
		}
		finally {
			this.commandLock.unlock();
		}

		return true;
	}

	/**
	 * Registers commands with the registry that the plugin manager uses
	 */
	private void registerCommands() {
		this.registerCommand(
			SafeResourceLoader.getString("COMMAND_ENABLE", this.resourceBundle),
			this::cbEnable);
		this.registerCommand(SafeResourceLoader.getString("COMMAND_DISABLE",
			this.resourceBundle), this::cbDisable);
		this.registerCommand(
			SafeResourceLoader.getString("COMMAND_LOAD", this.resourceBundle),
			this::cbLoad);
		this.registerCommand(
			SafeResourceLoader.getString("COMMAND_UNLOAD", this.resourceBundle),
			this::cbUnload);
		this.registerCommand(
			SafeResourceLoader.getString("COMMAND_RELOAD", this.resourceBundle),
			this::cbReload);
	}

	/**
	 * This is essentially restarting the plugins. The plugin is disabled if it
	 * is enabled, unloaded, then loaded.
	 *
	 * @param target The plugin to reload
	 *
	 * @return true if the plugin reloaded successfully, false otherwise
	 */
	public boolean reload(Plugin target) {
		Optional<PluginInfo> p = getInfo(target);

		if (!p.isPresent()) {
			// TODO log this
			return false;
		}
		PluginInfo pInfo = p.get();
		if (!this.isLoaded(target)) {
			String tmp = SafeResourceLoader
				.getString("PLUGIN_NOT_LOADED", this.getResourceBundle())
				.replaceFirst("\\$PLUGIN", pInfo.getName());
			log.warning(tmp);
			return false;
		}
		if (this.isEnabled(target)) {
			if (!this.disable(target)) {
				// disable failed
				this.setPluginState(target, PluginState.CORRUPTED);

				return false;
			}
		}
		this.setPluginState(target, PluginState.UNLOADING);
		this.unloadPlugin(target);
		this.setPluginState(target, PluginState.LOADING);
		if (!this.loadPlugin(target, pInfo)) {
			this.setPluginState(target, PluginState.CORRUPTED);
			return false;
		}
		return true;
	}

	/**
	 * Sets the flag for automatically enabling plugins when they are loaded. If
	 * they are not enabled on load, they must be enabled manually after being
	 * loaded.
	 * <p>
	 * This allows some customization of how the plugin system works.
	 *
	 * @param newValue true if the plugins should be automatically enabled after
	 *            loading, false if they should be manually enabled
	 */
	public void setEnableOnLoad(final boolean newValue) {
		this.enableLock.lock();
		try {
			this.enableOnLoad = newValue;
		}
		finally {
			this.enableLock.unlock();
		}
	}

	private boolean setPluginState(Plugin target, PluginState newState) {
		this.pluginLock.lock();
		try {
			if (!this.pluginNames.containsKey(target)) {
				return false;
			}
			return setPluginState(this.pluginNames.get(target), newState);
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	private boolean setPluginState(String target, PluginState newState) {
		this.pluginLock.lock();
		try {
			if (!this.loadedPlugins.containsKey(target)) {
				return false;
			}
		}
		finally {
			this.pluginLock.unlock();
		}

		this.pluginLock.lock();
		try {
			// replaces the old state
			PluginState oldState = this.loadedPlugins.get(target).getState();
			if (PluginState.PENDING_REMOVAL.equals(oldState)) {
				// can't change plugins pending removal
				return false;
			}
			this.loadedPlugins.get(target).setState(newState);
			return true;
		}
		finally {
			this.pluginLock.unlock();
		}
	}

	private void shutdown() {
		this.eventManager.unregisterEventListeners(logListener);
		pluginLock.lock();
		try {
			for (String s : loadedPlugins.keySet()) {
				// TODO would this break things?
				unloadPlugin(s);
			}
		}
		finally {
			pluginLock.unlock();
		}

		PluginManager.instance.clearCommands();
		PluginManager.instance.eventManager = null;
		PluginManager.instance.commands = null;
	}

	/**
	 * Attempts to unload the plugin from memory. Does nothing if the plugin is
	 * not loaded. Plugins are disabled before unloading. This calls
	 * {@link #unloadPlugin(String)} using the plugin type.
	 *
	 * @param toUnload The type of plugin to unload
	 */
	public void unloadPlugin(Plugin toUnload) {
		/*
		 * Using a string the plugins type to ensure the plugin that is stored
		 * in this class is modified and not just the plugin passed to the
		 * method.
		 */
		String type = getInfo(toUnload).get().getName();
		if (!this.isLoaded(type)) {
			String notLoaded = SafeResourceLoader
				.getString("PLUGIN_NOT_LOADED", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", type);
			log.fine(notLoaded);
			return;
		}
		this.unloadPlugin(type);
	}

	/**
	 * Attempts to unload the plugin from memory. If no plugin exists with the
	 * given name ({@link #isLoaded(String)}), returns false and does nothing.
	 *
	 * @param toUnload The type of plugin to unload
	 * @return true if the plugin was unloaded properly
	 */
	public boolean unloadPlugin(final String toUnload) {
		String unloading =
			SafeResourceLoader.getString("ALERT_UNLOADING", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", toUnload);
		log.fine(unloading);

		if (!this.isLoaded(toUnload)) {
			String notLoaded = SafeResourceLoader
				.getString("PLUGIN_NOT_LOADED", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", toUnload);
			log.fine(notLoaded);
			return false;
		}

		Plugin pack;
		this.pluginLock.lock();
		try {
			pack = this.loadedPlugins.get(toUnload).getPlugin();
		}
		finally {
			this.pluginLock.unlock();
		}
		if (pack == null) {
			String notLoaded = SafeResourceLoader
				.getString("PLUGIN_LOADED_BUT_NULL", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", toUnload);
			log.warning(notLoaded);
			return false;
		}

		// It has to be disabled before unloading.
		if (this.isEnabled(pack)) {
			this.disable(pack);
		}
		this.setPluginState(pack, PluginState.UNLOADING);

		pack.onUnload();
		// TODO will this not become an invalid reference during event sending?
		PluginUnloaded packUnloaded = new PluginUnloaded(toUnload);
		this.fireEvent(packUnloaded);

		for (Listener l : pack.getListeners()) {
			this.eventManager.unregisterEventListeners(l);
		}
		String unreg = SafeResourceLoader
			.getString("ALERT_UNREG_EVENT_LISTENERS", this.resourceBundle)
			.replaceFirst("\\$PLUGIN", PluginManager.PLUGIN_NAME);
		log.finer(unreg);
		this.pluginLock.lock();
		try {
			PluginDetails removedDetails = this.loadedPlugins.remove(toUnload);
			removedDetails.dispose();
		}
		finally {
			this.pluginLock.unlock();
		}

		String unloaded =
			SafeResourceLoader.getString("ALERT_UNLOADED", this.resourceBundle)
				.replaceFirst("\\$PLUGIN", toUnload);
		log.fine(unloaded);
		return true;
	}

	/**
	 * Unregisters the given command.
	 *
	 * @param command the command to remove
	 * @return true if the command was removed
	 */
	public boolean unregisterCommand(final String command) {
		boolean found = false;

		this.commandLock.lock();
		try {
			if (this.containsCommand(command)) {

				while (this.containsCommand(command)) {
					// just in case there are multiple
					int index = this.getIndexOfCommand(command);
					this.commands.remove(index);
				}
				String msg = SafeResourceLoader.getString(
					"UNREGISTERED_COMMAND", this.getResourceBundle());
				msg = msg.replaceFirst("\\$COMMAND", command);
				log.finest(msg);
				found = true;
			}
		}
		finally {
			this.commandLock.unlock();
		}
		return found;
	}

	/**
	 * Removes all commands that the given plugin registered.
	 *
	 * @param owner the plugin which is having commands removed
	 */
	public void unregisterPluginCommands(Plugin owner) {
		ArrayList<String> pluginCommands = new ArrayList<>();
		final String ownerName = getInfo(owner).get().getName();

		this.commandLock.lock();
		try {
			// find all the commands registered to the plugin
			for (PluginCommand c : this.commands) {
				if (!c.hasOwner()) {
					continue;
				}
				/*
				 * Gets the name from PluginInfo about the command owner,
				 * compares it with the known owner name.
				 */
				if (getInfo(c.getOwner().get()).get().getName()
					.equalsIgnoreCase(ownerName)) {
					pluginCommands.add(c.getCommand());
				}
			}
			// unload the commands
			for (String s : pluginCommands) {
				this.unregisterCommand(s);
			}
		}
		finally {
			this.commandLock.unlock();
		}

	}

}
