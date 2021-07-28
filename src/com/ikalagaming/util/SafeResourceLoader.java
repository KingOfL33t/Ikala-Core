package com.ikalagaming.util;

import com.ikalagaming.localization.Localization;
import com.ikalagaming.plugins.PluginManager;

import lombok.CustomLog;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *
 * Adds methods for accessing resources.
 *
 * @author Ches Burks
 *
 */
@CustomLog(topic = PluginManager.PLUGIN_NAME)
public class SafeResourceLoader {

	/**
	 * Returns a string from the supplied bundle. Any errors are printed to
	 * console. If no string is loaded, it attempts to load from the root
	 * resource bundle. If it fails again, the name is returned.
	 *
	 * @param name what to get from the bundle
	 * @param from the bundle to use
	 * @return the string from the bundle or name
	 */
	public static String getString(String name, ResourceBundle from) {
		try {
			return from.getString(name);
		}
		catch (MissingResourceException missingResource) {
			logMissingResource(name, from.getBaseBundleName());
		}
		catch (ClassCastException classCast) {
			logClassCastException(name, from.getBaseBundleName());
		}

		ResourceBundle rootOnly = ResourceBundle
			.getBundle(from.getBaseBundleName(), new ResourceBundle.Control() {
				@Override
				public List<Locale> getCandidateLocales(String n,
					Locale locale) {
					return Collections.singletonList(Locale.ROOT);
				}
			});
		try {
			return rootOnly.getString(name);
		}
		catch (MissingResourceException missingResource) {
			logMissingResource(name, from.getBaseBundleName());
		}
		catch (ClassCastException classCast) {
			logClassCastException(name, from.getBaseBundleName());
		}
		return name;
	}

	/**
	 * Returns a string from the supplied bundle. Any errors are printed to
	 * console. If no string is loaded, returns the fallback.
	 *
	 * @param name what to get from the bundle
	 * @param from the bundle to use
	 * @param fallback the string to use in the event of failure
	 * @return the string from the bundle or the fallback
	 */
	public static String getString(String name, ResourceBundle from,
		String fallback) {
		try {
			return from.getString(name);
		}
		catch (MissingResourceException missingResource) {
			logMissingResource(name, from.getBaseBundleName());
		}
		catch (ClassCastException classCast) {
			logClassCastException(name, from.getBaseBundleName());
		}
		return fallback;
	}

	/**
	 * Returns a string from the supplied bundle. Any errors are printed to
	 * console. If no string is loaded, it attempts to load from the root
	 * resource bundle. If it fails again, the name is returned.
	 *
	 * @param name what to get from the bundle
	 * @param from the bundle to use
	 * @return the string from the bundle or the fallback
	 */
	public static String getString(String name, String from) {
		ResourceBundle bundle;

		try {
			bundle = ResourceBundle.getBundle(from, Localization.getLocale());
			return bundle.getString(name);
		}
		catch (MissingResourceException missingResource) {
			logMissingResource(name, from);
		}
		catch (ClassCastException classCast) {
			logClassCastException(name, from);
		}

		ResourceBundle rootOnly =
			ResourceBundle.getBundle(from, new ResourceBundle.Control() {
				@Override
				public List<Locale> getCandidateLocales(String n,
					Locale locale) {
					return Collections.singletonList(Locale.ROOT);
				}
			});

		try {
			return rootOnly.getString(name);
		}
		catch (MissingResourceException missingResource) {
			logMissingResource(name, from);
		}
		catch (ClassCastException classCast) {
			logClassCastException(name, from);
		}
		return name;
	}

	/**
	 * Returns a string from the supplied bundle. Any errors are printed to
	 * console. If no string is loaded, returns the fallback.
	 *
	 * @param name what to get from the bundle
	 * @param from the bundle to use
	 * @param fallback the string to use in the event of failure
	 * @return the string from the bundle or the fallback
	 */
	public static String getString(String name, String from, String fallback) {
		ResourceBundle bundle;
		try {
			bundle = ResourceBundle.getBundle(from, Localization.getLocale());
			return bundle.getString(name);
		}
		catch (MissingResourceException missingResource) {
			logMissingResource(name, from);
		}
		catch (ClassCastException classCast) {
			logClassCastException(name, from);
		}
		return fallback;
	}

	private static void logMissingResource(String name, String bundle) {
		log.warning(
			"Missing the " + name + " key from the " + bundle + " bundle");
	}

	private static void logClassCastException(String name, String bundle) {
		log.warning("The " + name + " key from the " + bundle
			+ " bundle is not a string ");
	}

	/**
	 * Private constructor so that this class is not instantiated.
	 */
	private SafeResourceLoader() {}
}
