package com.ikalagaming.event;

import com.ikalagaming.localization.Localization;
import com.ikalagaming.util.SafeResourceLoader;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Manages events and listeners. Based off lahwran's fevents.
 */
@Slf4j
public class EventManager {

	private static EventManager instance;

	/**
	 * The resource bundle for event manager.
	 *
	 * @return The current resource bundle, which may be null.
	 */
	@SuppressWarnings("javadoc")
	@Getter
	private static ResourceBundle resourceBundle = ResourceBundle
		.getBundle("com.ikalagaming.event.Events", Localization.getLocale());

	/**
	 * Shuts down the static instance if it exists, and then nullifies the
	 * reference to it. This exists in case you wish to use your own instances
	 * of the Event Manager and not use the single static instance provided. If
	 * the instance does not exist, nothing happens. Note that a new static
	 * instance may be created if the instance is requested later.
	 *
	 * @see EventManager#getInstance()
	 */
	public static void destoryInstance() {
		if (EventManager.instance == null) {
			return;
		}
		EventManager.instance.shutdown();
		EventManager.instance = null;
	}

	/**
	 * Returns the static instance of the event manager. Since there should only
	 * be one of these, having a static instance is fine and any class can get
	 * the instance which all other classes should share. If there is no
	 * instance yet, one will be created.
	 *
	 * @return the static instance of the Event Manager
	 * @see EventManager#destoryInstance()
	 */
	public static EventManager getInstance() {
		if (EventManager.instance == null) {
			EventManager.instance = new EventManager();
		}
		return EventManager.instance;
	}

	private EventDispatcher dispatcher;

	private HashMap<Class<? extends Event>, HandlerList> handlerMap;

	/**
	 * Sets up the event managers handlers and event dispatching and starts the
	 * dispatching thread
	 */
	public EventManager() {
		this.dispatcher = new EventDispatcher(this);
		this.handlerMap = new HashMap<>();
		this.dispatcher.start();
	}

	/**
	 * Creates {@link EventListener EventListeners} for a given {@link Listener
	 * listener}.
	 *
	 * @param listener The listener to create EventListenrs for
	 * @return A map of events to a set of EventListeners belonging to it
	 */
	private Map<Class<? extends Event>, Set<EventListener>>
		createRegisteredListeners(Listener listener) {

		Map<Class<? extends Event>, Set<EventListener>> toReturn =
			new HashMap<>();

		// search the methods for listeners
		for (final Method method : listener.getClass().getDeclaredMethods()) {
			final EventHandler handlerAnnotation =
				method.getAnnotation(EventHandler.class);
			if (handlerAnnotation == null) {
				continue;
			}
			final Class<?> checkClass = method.getParameterTypes()[0];
			if (method.getParameterTypes().length != 1
				|| !Event.class.isAssignableFrom(checkClass)) {
				continue;
			}
			final Class<? extends Event> eventClass =
				checkClass.asSubclass(Event.class);
			/*
			 * We need the method to be publicly visible so that it can be
			 * called and passed events. SonarLint java:S3011 complains about
			 * this but we don't have much better options.
			 */
			method.setAccessible(true);// NOSONAR

			Set<EventListener> eventSet = toReturn.computeIfAbsent(eventClass,
				ignored -> new HashSet<>());

			// creates a class to execute the listener for the event
			EventExecutor executor = (listener1, event) -> {
				try {
					if (!eventClass.isAssignableFrom(event.getClass())) {
						return;
					}
					method.invoke(listener1, event);
				}
				catch (Exception t) {
					throw new EventException(t);
				}
			};

			eventSet.add(new EventListener(listener, executor,
				handlerAnnotation.order()));

		}
		return toReturn;
	}

	/**
	 * Sends the {@link Event event} to all of its listeners.
	 *
	 * @param event The event to fire
	 * @throws IllegalStateException if the element cannot be added at this time
	 *             due to capacity restrictions
	 */
	public void fireEvent(Event event) throws IllegalStateException {
		try {
			this.dispatcher.dispatchEvent(event);
		}
		catch (IllegalStateException illegalState) {
			throw illegalState;
		}
		catch (Exception e) {
			String err = SafeResourceLoader.getString("EVT_QUEUE_FULL",
				"com.ikalagaming.event.strings")
				+ "in EventManager.fireEvent(Event)";
			EventManager.log.warn(err);
		}
	}

	/**
	 * Returns a {@link HandlerList} for a give event type. Creates one if none
	 * exist.
	 *
	 * @param type the type of event to find handlers for
	 * @return the map of handlers for the given type
	 */
	private HandlerList getEventListeners(Class<? extends Event> type) {
		synchronized (this.handlerMap) {
			this.handlerMap.computeIfAbsent(type, ignored -> new HandlerList());
			return this.handlerMap.get(type);
		}
	}

	/**
	 * Returns the handlerlist for the given event.
	 *
	 * @param event the class to find handlers for
	 * @return the handlerlist for that class
	 */
	public HandlerList getHandlers(Event event) {
		return this.getEventListeners(event.getClass());
	}

	/**
	 * Registers event listeners for the given event monitor.
	 *
	 * @param <T> The type of event we are recording a listener for.
	 *
	 * @param monitor The listener to register.
	 */
	<T extends Event> void registerEventListeners(EventMonitor<T> monitor) {

		@SuppressWarnings("unchecked")
		EventExecutor executor = (listener, event) -> {
			try {
				/*
				 * This executor only runs for the given monitor instance, so we
				 * can cast it to the type that it is.
				 */
				((EventMonitor<T>) listener).onEvent((T) event);
			}
			catch (Exception t) {
				throw new EventException(t);
			}
		};

		HandlerList handlers = this.getEventListeners(monitor.getEventType());
		EventListener listener =
			new EventListener(monitor, executor, Order.MONITOR);
		handlers.register(listener);
	}

	/**
	 * Registers event listeners in the supplied listener.
	 *
	 * @param listener The listener to register
	 */
	public void registerEventListeners(Listener listener) {
		Map<Class<? extends Event>, Set<EventListener>> listMap;
		listMap = this.createRegisteredListeners(listener);
		listMap.entrySet().forEach(
			e -> this.getEventListeners(e.getKey()).registerAll(e.getValue()));
	}

	/**
	 * Set the event dispatcher's class loader.
	 *
	 * @param loader The new loader to use.
	 */
	public void setThreadClassloader(ClassLoader loader) {
		this.dispatcher.setContextClassLoader(loader);
	}

	/**
	 * Clears up the handlers and stops the dispatching thread. Acts like an
	 * onUnload method.
	 */
	public void shutdown() {
		synchronized (this.handlerMap) {
			this.handlerMap.values().forEach(HandlerList::unregisterAll);
			this.handlerMap.clear();
		}

		this.dispatcher.terminate();
		try {
			this.dispatcher.join();
		}
		catch (InterruptedException e) {
			EventManager.log.warn("Interrupted while shutting down", e);
			// Re-interrupt as per SonarLint java:S2142
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Unregisters event listeners in the supplied listener.
	 *
	 * @param listener The listener to unregister
	 */
	public void unregisterEventListeners(Listener listener) {
		synchronized (this.handlerMap) {
			this.handlerMap.values().forEach(list -> list.unregister(listener));
		}
	}
}
