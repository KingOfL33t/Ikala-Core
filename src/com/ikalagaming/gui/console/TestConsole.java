package com.ikalagaming.gui.console;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ikalagaming.event.EventManager;

/**
 * Tests the functionality of the console class.
 * 
 * @author Ches Burks
 *
 */
class TestConsole {

	/**
	 * Creates an event manager for the console test class.
	 * 
	 * @throws Exception if there is an exception setting up the console.
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestConsole.manager = new EventManager();
	}

	/**
	 * Shuts down the event manager for the console test class.
	 * 
	 * @throws Exception If there is an error shutting down the event manager
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		TestConsole.manager.shutdown();
	}

	private static EventManager manager;
	private Console console;

	@Before
	public void setUp() throws Exception {}

	@After
	public void tearDown() throws Exception {}

	@Test
	public void testAppendMessage() {
		Assert.fail("Not yet implemented"); // TODO
	}

	/**
	 * Tests the consoles constructor.
	 */
	@Test
	public void testConsole() {
		Console console = new Console(TestConsole.manager);
	}

	@Test
	public void testDisable() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testEnable() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetHeight() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetListeners() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetMaxLineCount() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetName() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetPackageState() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetVersion() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetWidth() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetWindowTitle() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testIsEnabled() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testLostOwnership() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testOnDisable() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testOnEnable() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testOnLoad() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testOnLogErrorEvent() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testOnLogEvent() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testOnReportUnknownCommand() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testOnUnload() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testReload() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testSetHeight() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testSetMaxLineCount() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testSetPackageState() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testSetWidth() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testSetWindowTitle() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowActivated() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowAdapter() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowClosed() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowClosing() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowDeactivated() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowDeiconified() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowGainedFocus() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowIconified() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowLostFocus() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowOpened() {
		Assert.fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWindowStateChanged() {
		Assert.fail("Not yet implemented"); // TODO
	}

}