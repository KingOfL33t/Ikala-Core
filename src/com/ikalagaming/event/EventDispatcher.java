package com.ikalagaming.event;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

import com.ikalagaming.logging.LoggingLevel;
import com.ikalagaming.logging.events.LogError;
import com.ikalagaming.util.SafeResourceLoader;

/**
 * Holds an EventQueue and dispatches the events in order when possible.
 *
 * @author Ches Burks
 *
 */
class EventDispatcher extends Thread {

	/**
	 * The number of milliseconds to wait before timing out and checking if
	 * there are more items again.
	 */
	private static final long WAIT_TIMEOUT = 10000;

	/**
	 * A running average of recent dispatch times
	 */
	private long averageTime;

	/**
	 * The difference between the start and end times
	 */
	private long estimatedTime;

	/**
	 * Keep a rolling average event dispatch time over the this many of the last
	 * measured values
	 */
	private static final long AVG_COUNT = 10;

	/**
	 * Used in calculating a rolling average.
	 */
	private static final float AVG_ALPHA = 2.0f / (AVG_COUNT + 1);

	/**
	 * Used to time dispatch calls. Specifically, the time just before
	 * execution.
	 */
	private long startTime;

	private ArrayDeque<Event> queue;

	private EventManager eventManager;

	private boolean running;
	private boolean hasEvents;

	/**
	 * Used to handle synchronization and waiting for events
	 */
	private Object syncObject;

	/**
	 * Creates and starts the thread. It will begin attempting to dispatch
	 * events immediately if there are any available.
	 *
	 * @param manager the event manager that this dispatcher belongs to
	 */
	public EventDispatcher(EventManager manager) {
		this.setName("EventDispatcher");
		this.queue = new ArrayDeque<>();
		this.eventManager = manager;
		this.hasEvents = false;
		this.running = true;
		this.syncObject = new Object();
		averageTime = 1;// Base value
	}

	private void dispatch(Event event) {
		HandlerList handlers = this.eventManager.getHandlers(event);
		if (handlers == null) {
			return;
		}
		EventListener[] listeners = handlers.getRegisteredListeners();
		for (EventListener registration : listeners) {
			try {
				startTime = System.nanoTime();
				registration.callEvent(event);
				estimatedTime = System.nanoTime() - startTime;

				/*
				 * Equivalent to acc=alpha*est + (1-alpha)*acc
				 */
				averageTime += AVG_ALPHA * (estimatedTime - averageTime);
			}
			catch (EventException e) {
				String error =
						SafeResourceLoader.getString("DISPATCH_ERROR",
								"com.ikalagaming.event.resources.strings",
								"There was a problem sending an event");
				this.eventManager.fireEvent(new LogError(error,
						LoggingLevel.WARNING, "event-manager"));
				System.err.println(e.toString());
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Adds the {@link Event event} to the queue pending dispatch.
	 *
	 * @param event The event to send out
	 * @throws IllegalStateException if the element cannot be added at this time
	 *             due to capacity restrictions
	 */
	public void dispatchEvent(Event event) throws IllegalStateException {
		try {
			synchronized (this.queue) {
				this.queue.add(event);
			}
			this.hasEvents = true;
		}
		catch (IllegalStateException illegalState) {
			throw illegalState;
		}
		catch (NullPointerException nullPointer) {
			;// do nothing since its a null event
			return;// don't wake up thread
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			return;// don't wake up thread
		}
		wakeUp();
	}

	private void handleEvent() {
		synchronized (this.queue) {
			if (this.queue.isEmpty()) {
				this.hasEvents = false;
				return;
			}
		}
		Event event;
		try {
			event = this.queue.remove();
		}
		catch (NoSuchElementException noElement) {
			// the queue is empty
			this.hasEvents = false;
			System.err.println(noElement.toString());
			return;
		}
		this.dispatch(event);
	}

	/**
	 * Wakes this thread up when it is sleeping
	 */
	private void wakeUp() {
		synchronized (this.syncObject) {
			// Wake the thread up as there is now an event
			this.syncObject.notify();
		}
	}

	/**
	 * Returns a running estimate of the past several times taken to execute
	 * event handler methods. If called before several events have been run, it
	 * will likely be useless due to statistical error.
	 * 
	 * @return the average time spent dispatching recent events
	 */
	public long getAverageTime() {
		return averageTime;
	}

	/**
	 * Checks for events in the queue, and dispatches them if possible. Does not
	 * do anything if {@link #terminate()} has been called.
	 */
	@Override
	public void run() {
		while (this.running) {
			while (!this.hasEvents) {
				synchronized (this.syncObject) {
					try {
						// block this thread until an item is added
						this.wait(EventDispatcher.WAIT_TIMEOUT);
					}
					catch (InterruptedException e) {
						// TODO log this
						e.printStackTrace(System.err);
					}
				}
				// in case it was terminated while waiting
				if (!this.running) {
					break;
				}
			}
			if (this.hasEvents) {
				this.handleEvent();
			}
		}
		// Done running
		this.queue.clear();
	}

	/**
	 * Stops the thread from executing its run method in preparation for
	 * shutting down the thread.
	 */
	public void terminate() {
		this.hasEvents = false;
		this.running = false;
		this.eventManager = null;
		wakeUp();
	}
}
