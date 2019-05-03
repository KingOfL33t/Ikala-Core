package com.ikalagaming.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.ikalagaming.plugins.Plugin;
import com.ikalagaming.plugins.PluginManager;
import com.ikalagaming.plugins.PluginState;

/**
 * Displays various information about the program.
 *
 * @author Ches Burks
 *
 */
public class TaskManager extends JFrame {

	private static class Updater extends Thread {
		TaskManager owner;

		public Updater(TaskManager manager) {
			this.setName("TaskMgrUpdateThread");
			this.owner = manager;
		}

		@Override
		public void run() {
			while (true) {
				this.owner.tick();
				try {
					Thread.sleep(this.owner.getDelay());
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static final long serialVersionUID = -4427516209866980363L;

	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					this.showMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					this.showMenu(e);
				}
			}

			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());

				if (e.getSource() instanceof JTable) {
					JTable source = (JTable) e.getSource();
					int row = source.rowAtPoint(e.getPoint());
					int column = source.columnAtPoint(e.getPoint());

					if (!source.isRowSelected(row)) {
						source.changeSelection(row, column, false, false);
					}
				}
			}
		});
	}

	private JPanel contentPane;
	private JTable table;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	/**
	 * low long to wait between plugin status updates
	 */
	long delay = 1000;
	private DefaultTableModel model;
	private Map<String, PluginState> plugins = new HashMap<>();
	private final int maxTick = 10;
	private int tickCount = 0;// counts down to refreshing plugin list
	private JLabel threads;
	private JLabel memUsage;
	private long memUsed = 0;

	private long percentUsed = 0;

	private PluginManager pluginManager;

	/**
	 * Create the frame.
	 *
	 * @param pluginMgr the plugin manager to list plugins for
	 *
	 */
	public TaskManager(PluginManager pluginMgr) {
		this.pluginManager = pluginMgr;
		this.setTitle("Task Manager");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBounds(100, 100, 450, 465);

		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmNewPlugin = new JMenuItem("New Plugin");
		mnFile.add(mntmNewPlugin);

		JMenuItem mntmExitTaskManager = new JMenuItem("Exit Task Manager");
		mnFile.add(mntmExitTaskManager);

		JMenu mnView = new JMenu("View");
		menuBar.add(mnView);

		JMenuItem mntmUpdateNow = new JMenuItem("Update now");
		mnView.add(mntmUpdateNow);

		JMenu mnUpdateSpeed = new JMenu("Update Speed");
		mnView.add(mnUpdateSpeed);

		JRadioButtonMenuItem rdbtnmntmHigh = new JRadioButtonMenuItem("High");
		rdbtnmntmHigh.addItemListener(arg0 -> {
			if (arg0.getStateChange() == ItemEvent.SELECTED) {
				TaskManager.this.delay = 500;
			}
		});
		this.buttonGroup.add(rdbtnmntmHigh);
		mnUpdateSpeed.add(rdbtnmntmHigh);

		JRadioButtonMenuItem rdbtnmntmNormal =
			new JRadioButtonMenuItem("Normal");
		rdbtnmntmNormal.setSelected(true);
		rdbtnmntmNormal.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				TaskManager.this.delay = 1000;
			}
		});
		this.buttonGroup.add(rdbtnmntmNormal);
		mnUpdateSpeed.add(rdbtnmntmNormal);

		JRadioButtonMenuItem rdbtnmntmLow = new JRadioButtonMenuItem("Low");
		rdbtnmntmLow.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				TaskManager.this.delay = 2000;
			}
		});
		this.buttonGroup.add(rdbtnmntmLow);
		mnUpdateSpeed.add(rdbtnmntmLow);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setContentPane(this.contentPane);
		this.contentPane.setLayout(new BorderLayout(0, 0));

		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
		this.contentPane.add(tabbedPane, BorderLayout.CENTER);

		JScrollPane scrollPane = new JScrollPane();
		tabbedPane.addTab("Plugins", null, scrollPane, null);

		this.table = new JTable();
		this.table.setModel(new DefaultTableModel(new Object[][] {},
			new String[] {"Plugin Name", "Status"}));
		this.table.getColumnModel().getColumn(0).setPreferredWidth(102);

		JPopupMenu popupMenu_1 = new JPopupMenu();

		JMenuItem mntmEnable = new JMenuItem("Enable");
		mntmEnable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				TaskManager.this.changeState("Enable");
			}
		});
		popupMenu_1.add(mntmEnable);

		JMenuItem mntmDisable = new JMenuItem("Disable");
		mntmDisable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				TaskManager.this.changeState("Disable");
			}
		});
		popupMenu_1.add(mntmDisable);

		JMenuItem mntmUnload = new JMenuItem("Unload");
		mntmUnload.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				TaskManager.this.changeState("Unload");
			}
		});
		popupMenu_1.add(mntmUnload);

		JMenuItem mntmReload = new JMenuItem("Reload");
		mntmReload.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				TaskManager.this.changeState("Reload");
			}
		});
		popupMenu_1.add(mntmReload);

		TaskManager.addPopup(this.table, popupMenu_1);

		scrollPane.setViewportView(this.table);

		JPanel panel_1 = new JPanel();
		this.contentPane.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new GridLayout(1, 0, 0, 0));

		JLabel lblMemoryUsage = new JLabel("Memory Usage:");
		panel_1.add(lblMemoryUsage);

		this.memUsage = new JLabel("0");
		panel_1.add(this.memUsage);

		JLabel lblThreads = new JLabel("Threads:");
		panel_1.add(lblThreads);

		this.threads = new JLabel("0");
		panel_1.add(this.threads);
		this.model = (DefaultTableModel) this.table.getModel();

		Updater update = new Updater(this);
		update.start();
	}

	/**
	 * Attempts to change the status of the currently selected plugin. This may
	 * be any of the following:
	 * <ul>
	 * <li>Enable</li>
	 * <li>Disable</li>
	 * <li>Load</li>
	 * <li>Unload</li>
	 * </ul>
	 *
	 * @param change the change to make
	 */
	public void changeState(String change) {
		int row = this.table.getSelectedRow();
		int column = -1;
		for (int i = 0; i < this.table.getColumnCount(); ++i) {
			if (this.table.getColumnName(i).equals("Plugin Name")) {
				column = i;
			}
		}
		if (row == -1 || column == -1) {
			return;
		}
		Optional<Plugin> p = this.pluginManager
			.getPlugin(this.table.getValueAt(row, column).toString());
		if (!p.isPresent()) {
			return;
		}
		Plugin pack = p.get();
		if (change == "Enable") {
			if (!this.pluginManager.isEnabled(pack)) {
				this.pluginManager.enable(pack);
			}
		}
		else if (change == "Disable") {
			if (this.pluginManager.isEnabled(pack)) {
				this.pluginManager.disable(pack);
			}
		}
		else if (change == "Unload") {
			this.pluginManager.unloadPlugin(pack);
		}
		else if (change == "Reload") {
			this.pluginManager.reload(pack);
		}

	}

	/**
	 * Returns the length of time to wait between refreshes.
	 *
	 * @return the delay time in ms
	 */
	public long getDelay() {
		return this.delay;
	}

	/**
	 * Updates and displays information about the program
	 */
	public void tick() {
		if (this.tickCount == 0) {
			this.updatePluginNames();
			this.tickCount = this.maxTick;
		}
		--this.tickCount;

		PluginState currentState;
		String name = "";
		for (int i = 0; i < this.model.getRowCount(); ++i) {
			name = (String) this.model.getValueAt(i, 0);
			currentState = this.pluginManager
				.getPluginState(this.pluginManager.getPlugin(name).get());
			if (!this.model.getValueAt(i, 1).equals(currentState)) {
				this.model.setValueAt(currentState, i, 1);
			}
		}
		this.threads.setText(java.lang.Thread.activeCount() + "");

		this.memUsed = (Runtime.getRuntime().totalMemory()
			- Runtime.getRuntime().freeMemory()) / 1024;
		this.percentUsed = (Runtime.getRuntime().totalMemory()
			- Runtime.getRuntime().freeMemory()) * 100
			/ Runtime.getRuntime().totalMemory();

		this.memUsage.setText(this.memUsed + " kb (" + this.percentUsed + "%)");
	}

	private void updatePluginNames() {
		Set<String> pluginNames =
			this.pluginManager.getLoadedPlugins().keySet();
		for (String s : pluginNames) {
			if (!this.plugins.containsKey(s)) {
				this.plugins.put(s, this.pluginManager
					.getPluginState(this.pluginManager.getPlugin(s).get()));
				boolean exists = false;
				for (int i = 0; i < this.model.getRowCount(); ++i) {
					if (this.model.getValueAt(i, 0).equals(s)) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					this.model.addRow(new Object[] {s, this.plugins.get(s)});
				}
			}
		}
		for (String s : this.plugins.keySet()) {
			if (!pluginNames.contains(s)) {
				this.plugins.remove(s);
				for (int i = 0; i < this.model.getRowCount(); ++i) {
					if (this.model.getValueAt(i, 0).equals(s)) {
						this.model.removeRow(i);
						break;
					}
				}
			}
		}
		pluginNames = null;

	}
}
