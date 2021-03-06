package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.Plugins.PluginIcon;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheListener;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheLoadingModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCacheSelectionModel;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKPath;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKStupidDownloader;
import org.helioviewer.jhv.plugins.hekplugin.cache.gui.HEKCacheTreeView;
import org.helioviewer.jhv.plugins.hekplugin.cache.gui.HEKCacheTreeViewContainer;

/**
 * Represents the UI components which manage the HEK event catalog.
 * */
class HEKPluginPanel extends JPanel implements ActionListener,
		HEKCacheListener {

	// UI Components
	private JPanel buttonPanel = new JPanel(new BorderLayout());
	private JProgressBar progressBar = new JProgressBar();
	private HEKCacheTreeView tree = new HEKCacheTreeView(
			HEKCache.getSingletonInstance());
	private JScrollPane treeView = new JScrollPane(tree);
	private JButton cancelButton = new JButton(Plugins.getIcon(
			PluginIcon.CANCEL, 16, 16));
	private JButton reloadButton = new JButton(Plugins.getIcon(
			PluginIcon.REFRESH, 16, 16));
	private HEKCacheTreeViewContainer container = new HEKCacheTreeViewContainer();

	private HEKCacheModel cacheModel;
	private HEKCache cache;
	private HEKCacheSelectionModel selectionModel;
	private HEKCacheLoadingModel loadingModel;

	private LocalDateTime start = null;
	private LocalDateTime end = null;

	/**
	 * Default constructor
	 * 
	 * @param hekCache
	 * */
	public HEKPluginPanel(HEKCache hekCache) {

		this.cache = hekCache;
		this.cacheModel = hekCache.getModel();
		this.selectionModel = hekCache.getSelectionModel();
		this.loadingModel = hekCache.getLoadingModel();

		// set up visual components
		initVisualComponents();

		// register as layers listener
		HEKCache.getSingletonInstance().getModel().addCacheListener(this);

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				reload();
			}
		});
	}

	/**
	 * Force a redraw of the main window
	 */
	private void fireRedraw() {
		Plugins.repaintMainPanel();
	}

	/**
	 * Update the plugin's currently displayed interval.
	 * 
	 * The plugin is currently stafeFUL, so keep in mind that just calling this
	 * method without triggering any other update method might not be a good
	 * decision.
	 * 
	 * @param newInterval
	 *            - the interval that should be displayed
	 * 
	 * @see org.helioviewer.jhv.plugins.overlay.hek.cache.HEKCacheModel#setCurInterval
	 * 
	 */
	public void setCurInterval(Interval<Date> newPosition) {
		if (!HEKCache.getSingletonInstance().getModel().getCurInterval()
				.equals(newPosition)) {
			HEKCache.getSingletonInstance().getController()
					.setCurInterval(newPosition);
		}
	}

	/**
	 * Request the plugin to download and display the Events available in the
	 * catalogue
	 * 
	 * The interval to be requested depends on the current state of the plug-in.
	 * 
	 * @see org.helioviewer.jhv.plugins.overlay.hek.cache.HEKCacheController#requestStructure
	 * 
	 */
	public void getStructure() {
		Interval<Date> selected = HEKCache.getSingletonInstance().getModel()
				.getCurInterval();
		HEKCache.getSingletonInstance().getController()
				.requestStructure(selected);
	}

	/**
	 * Sets up the visual sub components and the visual part of the component
	 * itself.
	 * */
	private void initVisualComponents() {

		// set general appearance
		setLayout(new GridBagLayout());

		this.setPreferredSize(new Dimension(150, 200));

		progressBar.setIndeterminate(true);

		cancelButton.addActionListener(this);
		reloadButton.addActionListener(this);

		tree.setModel(HEKCache.getSingletonInstance().getTreeModel());
		tree.setController(HEKCache.getSingletonInstance().getController());
		tree.setRootVisible(false);

		container.setMain(treeView);
		container.update();

		HEKCache.getSingletonInstance()
				.getController()
				.fireEventsChanged(
						(HEKCache.getSingletonInstance().getController()
								.getRootPath()));

		setEnabled(true);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;

		this.add(container, c);

		GridBagConstraints c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 1.0;
		c2.weighty = 0.0;
		c2.gridx = 0;
		c2.gridy = 1;

		this.add(progressBar, c2);
		this.setLoading(false);

		buttonPanel.add(reloadButton, BorderLayout.EAST);
		buttonPanel.add(cancelButton, BorderLayout.EAST);

		GridBagConstraints c3 = new GridBagConstraints();
		c3.fill = GridBagConstraints.NONE;
		c3.anchor = GridBagConstraints.EAST;
		c3.weightx = 0.0;
		c3.weighty = 0.0;
		c3.gridx = 1;
		c3.gridy = 1;

		this.add(reloadButton, c3);
		this.add(cancelButton, c3);
	}

	public void actionPerformed(ActionEvent act) {

		if (act.getSource().equals(cancelButton)) {
			HEKStupidDownloader.getSingletonInstance().cancelDownloads();
		}

		if (act.getSource().equals(reloadButton)) {
			this.reload();
		}

		if (act.getActionCommand().equals("request")) {
			// move into controller
			// td move into loading watcher
			HashMap<HEKPath, List<Interval<Date>>> selected = selectionModel
					.getSelection(cacheModel.getCurInterval());
			HashMap<HEKPath, List<Interval<Date>>> needed = cache
					.needed(selected);
			HashMap<HEKPath, List<Interval<Date>>> nonQueued = HEKCache
					.getSingletonInstance().getLoadingModel()
					.filterState(needed, HEKCacheLoadingModel.PATH_NOTHING);
			HEKCache.getSingletonInstance().getController()
					.requestEvents(nonQueued);
		}

	}

	public void reload() {

		LocalDateTime startDateTime;
		startDateTime = Plugins.getStartDateTime();
		LocalDateTime endDateTime = Plugins.getEndDateTime();
		if (startDateTime != null
				&& endDateTime != null
				&& (start == null || (startDateTime.isBefore(start) || endDateTime
						.isAfter(end)))) {

			Thread threadUpdate = new Thread(new Runnable() {
				public void run() {
					LocalDateTime startDateTime;
					startDateTime = Plugins.getStartDateTime();
					LocalDateTime endDateTime = Plugins
							.getEndDateTime();
					if (startDateTime != null && endDateTime != null) {
						Date start = Date.from(startDateTime.atZone(
								ZoneId.systemDefault()).toInstant());
						Date end = Date.from(endDateTime.atZone(
								ZoneId.systemDefault()).toInstant());
						if (start != null && end != null) {
							Interval<Date> range = new Interval<>(start,
									end);
							HEKCache.getSingletonInstance().getController()
									.setCurInterval(range);
							getStructure();
						}
						HEKPluginPanel.this.start = startDateTime;
						HEKPluginPanel.this.end = endDateTime;
					}
				}
			});
			threadUpdate.setDaemon(true);
			threadUpdate.start();
		}

	}

	public void setEnabled(boolean b) {
		// super.setEnabled(b);
		if (!b) {
			HEKCache.getSingletonInstance().getExpansionModel()
					.expandToLevel(0, true, true);
			HEKPath rootPath = HEKCache.getSingletonInstance().getController()
					.getRootPath();
			HEKCache.getSingletonInstance().getController()
					.fireEventsChanged(rootPath);
		}
		tree.setEnabled(b);
	}

	public void cacheStateChanged() {
		// anything loading?
		boolean loading = loadingModel.getState(cacheModel.getRoot(), true) != HEKCacheLoadingModel.PATH_NOTHING;
		this.setLoading(loading);
	}

	private void setLoading(boolean loading)
	{
		//TODO: don't do this on non EDT threads
		/*try
		{
			if(SwingUtilities.isEventDispatchThread())
			{*/
				progressBar.setVisible(loading);
				cancelButton.setVisible(loading);
				reloadButton.setVisible(!loading);
			/*}
			else
				SwingUtilities.invokeAndWait(() ->
				{
					progressBar.setVisible(loading);
					cancelButton.setVisible(loading);
					reloadButton.setVisible(!loading);
				});
		}
		catch (Throwable _t)
		{
			Telemetry.trackException(_t);
		}*/
	}

	public void eventsChanged(HEKPath path) {
		fireRedraw();
	}

	public void structureChanged(HEKPath path) {
		fireRedraw();
	}

	/**
	 * {@inheritDoc}
	 */
	public void layerDownloaded(int idx) {
	}
}
