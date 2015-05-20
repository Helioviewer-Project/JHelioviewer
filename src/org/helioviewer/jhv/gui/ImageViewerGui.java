package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.net.URL;
import java.util.AbstractList;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.SplashScreen;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.components.MainContentPanel;
import org.helioviewer.jhv.gui.components.MainImagePanel;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.components.newComponents.FilterTabPanel;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.gui.components.newComponents.NewLayerPanel;
import org.helioviewer.jhv.gui.components.newComponents.NewPlayPanel;
import org.helioviewer.jhv.gui.components.statusplugins.CurrentTimeLabel;
import org.helioviewer.jhv.gui.components.statusplugins.FramerateStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.PositionStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.ZoomStatusPanel;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.io.JHVRequest;

/**
 * A class that sets up the graphical user interface.
 * 
 * @author caplins
 * @author Benjamin Wamsler
 * @author Alen Agheksanterian
 * @author Stephan Pagel
 * @author Markus Langenberg
 * @author Andre Dau
 * 
 */
public class ImageViewerGui {

	/** The sole instance of this class. */
	private static final ImageViewerGui SINGLETON = new ImageViewerGui();

	private static JFrame mainFrame;
	private JPanel contentPanel;
	private JSplitPane midSplitPane;
	private JScrollPane leftScrollPane;

	private MainContentPanel mainContentPanel;
	protected MainImagePanel mainImagePanel;

	private SideContentPane leftPane;
	//private NewImageSelectorPanel newImageSelectorPanel;
	private NewLayerPanel newLayerPanel;
	private Component moviePanel;
	private JMenuBar menuBar;

	private JPanel overViewPane;

	public FilterTabPanel filterTabPanel;

	public static final int SIDE_PANEL_WIDTH = 320;

	/**
	 * The private constructor that creates and positions all the gui
	 * components.
	 */
	private ImageViewerGui() {
		mainFrame = createMainFrame();

		menuBar = new MenuBar();
		menuBar.setFocusable(false);

		mainFrame.setJMenuBar(menuBar);
		mainFrame.setFocusable(true);

		prepareGui();
	}

	public void prepareGui() {
			contentPanel = new JPanel(new BorderLayout());
			mainFrame.setContentPane(contentPanel);

			//midSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
			midSplitPane = new JSplitPane();
			midSplitPane.setOneTouchExpandable(false);

			contentPanel.add(midSplitPane, BorderLayout.CENTER);
			mainContentPanel = new MainContentPanel();
			mainContentPanel.setMainComponent(getMainImagePanel());

			// STATE - GET TOP TOOLBAR
			contentPanel.add(MainFrame.TOP_TOOL_BAR, BorderLayout.PAGE_START);

			// // FEATURES / EVENTS
			// solarEventCatalogsPanel = new SolarEventCatalogsPanel();
			// leftPane.add("Features/Events", solarEventCatalogsPanel, false);

			//Overview-panel
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout(0, 0));

			overViewPane = new JPanel();
			overViewPane.setPreferredSize(new Dimension(240, 200));
			overViewPane.setMinimumSize(new Dimension(240, 200));
			overViewPane.setBackground(Color.BLACK);
			panel.add(overViewPane, BorderLayout.NORTH);

			// STATE - GET LEFT PANE
			leftScrollPane = new JScrollPane(getLeftContentPane(),
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			leftScrollPane.setFocusable(false);
			leftScrollPane.getVerticalScrollBar().setUnitIncrement(10);
			panel.add(leftScrollPane, BorderLayout.CENTER);
			midSplitPane.setLeftComponent(panel);

			midSplitPane.setRightComponent(mainContentPanel);

			// ///////////////////////////////////////////////////////////////////////////////
			// STATUS PANEL
			// ///////////////////////////////////////////////////////////////////////////////

			ZoomStatusPanel zoomStatusPanel = new ZoomStatusPanel();
			FramerateStatusPanel framerateStatus = new FramerateStatusPanel();

			PositionStatusPanel positionStatusPanel = null;

			positionStatusPanel = new PositionStatusPanel(getMainImagePanel());


			StatusPanel statusPanel = new StatusPanel(5, 5);
			statusPanel.addLabel(new CurrentTimeLabel() , StatusPanel.Alignment.LEFT);
			statusPanel.addPlugin(zoomStatusPanel, StatusPanel.Alignment.LEFT);
			statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
			statusPanel.addPlugin(positionStatusPanel,
					StatusPanel.Alignment.RIGHT);
			
			contentPanel.add(statusPanel, BorderLayout.PAGE_END);
			
			contentPanel.validate();
            leftScrollPane.setMinimumSize(new Dimension(leftScrollPane.getWidth(),0));
	}

	/**
	 * Packs, positions and shows the GUI
	 * 
	 * @param _show
	 *            If GUI should be displayed.
	 */
	public void packAndShow(final boolean _show) {

		final SplashScreen splash = SplashScreen.getSingletonInstance();

		// load images which should be displayed first in a separated thread
		// that splash screen will be updated
		splash.setProgressText("Loading Images...");

		Thread thread = new Thread(new Runnable() {

			public void run() {
				loadImagesAtStartup();
				// show GUI
				splash.setProgressText("Starting JHelioviewer...");
				splash.nextStep();
				mainFrame.pack();
				mainFrame.setLocationRelativeTo(null);
				mainFrame.setVisible(_show);
				splash.setProgressValue(100);

				// remove splash screen
				splash.dispose();

			}
		}, "LoadImagesOnStartUp");
		thread.setDaemon(true);
		thread.start();
	}
	
	private void loadImagesAtStartup() {

        // get values for different command line options
        AbstractList<JHVRequest> jhvRequests = CommandLineProcessor.getJHVOptionValues();
        AbstractList<URI> jpipUris = CommandLineProcessor.getJPIPOptionValues();
        AbstractList<URI> downloadAddresses = CommandLineProcessor.getDownloadOptionValues();
        AbstractList<URL> jpxUrls = CommandLineProcessor.getJPXOptionValues();

        // Do nothing if no resource is specified
        if (jhvRequests.isEmpty() && jpipUris.isEmpty() && downloadAddresses.isEmpty() && jpxUrls.isEmpty()) {
            return;
        }

        // //////////////////////
        // -jhv
        // //////////////////////

        // go through all jhv values

        // //////////////////////
        // -jpx
        // //////////////////////
    }



	/**
	 * Initializes the main and overview view chain.
	 */
	public void createViewchains() {
		
		// prepare gui again

		mainFrame.validate();

		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				updateComponentPanels();
				
			}
		});
		
		packAndShow(true);
		mainFrame.validate();
		mainFrame.repaint();
	}

	/**
	 * Method that creates and initializes the main JFrame.
	 * 
	 * @return the created and initialized main frame.
	 */
	private JFrame createMainFrame() {
		JFrame frame = new JFrame("ESA JHelioviewer");

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent arg0) {
				ExitProgramAction exitAction = new ExitProgramAction();
				exitAction.actionPerformed(new ActionEvent(this, 0, ""));
			}
		});

		Dimension maxSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension minSize = new Dimension(800, 600);

		maxSize.width -= 200;
		// if the display is not very high, we want to take most of the height,
		// as the rest is not useful anyway
		if (maxSize.height < 1000) {
			maxSize.height -= 100;
		} else {
			maxSize.height -= 150;
		}

		minSize.width = Math.min(minSize.width, maxSize.width);
		minSize.height = Math.min(minSize.height, maxSize.height);
		//frame.setMaximumSize(maxSize);
		frame.setMinimumSize(minSize);
		frame.setPreferredSize(maxSize);
		frame.setFont(new Font("SansSerif", Font.BOLD, 12));
		frame.setIconImage(IconBank.getIcon(JHVIcon.HVLOGO_SMALL).getImage());
		return frame;
	}

	/**
	 * Returns the scrollpane containing the left content pane.
	 * 
	 * @return instance of the scrollpane containing the left content pane.
	 * */
	public SideContentPane getLeftContentPane() {
		// ////////////////////////////////////////////////////////////////////////////////
		// LEFT CONTROL PANEL
		// ////////////////////////////////////////////////////////////////////////////////

		if (this.leftPane != null) {
			return this.leftPane;
		} else {
			leftPane = new SideContentPane();

			// Movie control
			this.moviePanel = new NewPlayPanel();
			
			
			leftPane.add("Movie Controls", moviePanel, true);

			// Layer control
			newLayerPanel = new NewLayerPanel();

			leftPane.add("new Layers", newLayerPanel, true);

			filterTabPanel = new FilterTabPanel();
			leftPane.add("NEWAdjustments", filterTabPanel , true);
			
			return leftPane;
		}
	}

	/**
	 * @return the menu bar of jhv
	 */
	public JMenuBar getMenuBar() {
		return menuBar;
	}

	public MainImagePanel getMainImagePanel() {
		// ////////////////////////////////////////////////////////////////////////////////
		// MAIN IMAGE PANEL
		// ////////////////////////////////////////////////////////////////////////////////
		if (mainImagePanel == null) {

			// set up main image panel
			mainImagePanel = new MainImagePanel();
			mainImagePanel.setAutoscrolls(true);
			mainImagePanel.setFocusable(false);

		}

		return mainImagePanel;
	}

	public void addTopToolBarPlugin(
			PropertyChangeListener propertyChangeListener, JToggleButton button) {
		MainFrame.TOP_TOOL_BAR.addToolbarPlugin(button);

		MainFrame.TOP_TOOL_BAR
				.addPropertyChangeListener(propertyChangeListener);

		MainFrame.TOP_TOOL_BAR.validate();
		MainFrame.TOP_TOOL_BAR.repaint();
	}

	private void updateComponentPanels() {

		mainFrame.add(MainFrame.MAIN_PANEL);
		mainFrame.validate();
	}

	/**
	 * Returns the only instance of this class.
	 * 
	 * @return the only instance of this class.
	 * */
	public static ImageViewerGui getSingletonInstance() {
		return SINGLETON;
	}

	/**
	 * Returns the main frame.
	 * 
	 * @return the main frame.
	 * */
	public static JFrame getMainFrame() {
		return mainFrame;
	}

	/**
	 * Returns the scrollpane containing the left content pane.
	 * 
	 * @return instance of the scrollpane containing the left content pane.
	 * */
	public JScrollPane getLeftScrollPane() {
		return leftScrollPane;
	}

	/**
	 * Calls the update methods of sub components of the main frame so they can
	 * e.g. reload data.
	 */
	public void updateComponents() {
	}

	/**
	 * Returns the content panel of JHV
	 * 
	 * @return The content panel of JHV
	 */
	public JPanel getContentPane() {
		return contentPanel;
	}

	public final MainContentPanel getMainContentPanel() {
		return mainContentPanel;
	}

	public JPanel getOverviewPane() {
		return overViewPane;
	}
}
