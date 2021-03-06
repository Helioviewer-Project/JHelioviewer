package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;

import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.statusLabels.CurrentTimeLabel;
import org.helioviewer.jhv.gui.statusLabels.FramerateStatusPanel;
import org.helioviewer.jhv.gui.statusLabels.PositionStatusPanel;
import org.helioviewer.jhv.gui.statusLabels.ZoomStatusPanel;

import com.jogamp.opengl.GLContext;

public class MainFrame extends JFrame
{
	public final MainPanel MAIN_PANEL;
	public final OverviewPanel OVERVIEW_PANEL;
	public final TopToolBar TOP_TOOL_BAR = new TopToolBar();
	public static MainFrame SINGLETON;
	public final int SIDE_PANEL_WIDTH = 320;
	public final SideContentPane LEFT_PANE;
	public final MoviePanel MOVIE_PANEL;
	public final LayerPanel LAYER_PANEL;
	public final MenuBar MENU_BAR;
	private final JSplitPane splitPane;
	private final JPanel leftPanel;

	public final FilterPanel FILTER_PANEL;
	
	public static void init(GLContext _context)
	{
		SINGLETON = new MainFrame(_context);
		
		if(Globals.IS_OS_X)
		{
			enableOSXFullscreen(SINGLETON);
		}
	}
	
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void enableOSXFullscreen(Window window) 
	{
		//code from https://stackoverflow.com/a/8693890
	    try 
	    {
	        Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
	        Class params[] = new Class[]{Window.class, Boolean.TYPE};
	        Method method = util.getMethod("setWindowCanFullScreen", params);
	        method.invoke(util, window, true);
	    } 
	    catch (ClassNotFoundException e1) 
	    {	   
	    	
	    } 
	    catch (Exception e) 
	    {
	        Telemetry.trackException(e);
	    }
	}
	
	private MainFrame(GLContext context)
	{
		super("ESA JHelioviewer");

		MAIN_PANEL = new MainPanel(context);
		OVERVIEW_PANEL = new OverviewPanel(context);
		initMainFrame();
		
		MENU_BAR = new MenuBar();
		setJMenuBar(MENU_BAR);
		
		getContentPane().add(TOP_TOOL_BAR, BorderLayout.NORTH);
		
		MAIN_PANEL.setMinimumSize(new Dimension());
		OVERVIEW_PANEL.setMinimumSize(new Dimension(0, 200));
		OVERVIEW_PANEL.setPreferredSize(new Dimension(0, 200));
		
		OVERVIEW_PANEL.addMainView(MAIN_PANEL);
		MAIN_PANEL.addSynchronizedView(OVERVIEW_PANEL);
		
		MOVIE_PANEL = new MoviePanel();
		LAYER_PANEL = new LayerPanel();
		FILTER_PANEL = new FilterPanel();
		
		//TODO: save & restore state upon startup
		LEFT_PANE = new SideContentPane();
		LEFT_PANE.add("Playback", MOVIE_PANEL, true);
		LEFT_PANE.add("Layers", LAYER_PANEL, true);
		LEFT_PANE.add("Adjustments", FILTER_PANEL, true);
		
		JScrollPane scrollPane = new JScrollPane(LEFT_PANE);
		
		leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout(0,2));
		leftPanel.add(OVERVIEW_PANEL, BorderLayout.NORTH);
		leftPanel.add(scrollPane, BorderLayout.CENTER);
		
		splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.0);
		splitPane.setContinuousLayout(true);
		splitPane.setRightComponent(MAIN_PANEL);		
		splitPane.setLeftComponent(leftPanel);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		
		scrollPane.setMinimumSize(new Dimension(scrollPane.getPreferredSize().width+scrollPane.getVerticalScrollBar().getVisibleAmount()*2,0));
		
		if(Globals.IS_OS_X)
		{
			splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,(e) ->
			{
				SwingUtilities.invokeLater(() -> workAroundOSXOpenGLBugs(true));
			});
			
			addComponentListener(new ComponentAdapter()
			{
				@Override
				public void componentResized(ComponentEvent e)
				{
					workAroundOSXOpenGLBugs(false);
				}
			});
		}
		
		getContentPane().add(this.getStatusPane(), BorderLayout.SOUTH);
		
		
		repaintTimer=new Timer(250, a ->
		{
			LAYER_PANEL.updateDataAsync();
			MAIN_PANEL.repaint();
			MOVIE_PANEL.repaint();
		});
		repaintTimer.setRepeats(false);
	}
	
	private void workAroundOSXOpenGLBugs(boolean _isHorizontalMove)
	{
		if(splitPane==null || leftPanel==null)
			return;
		
		int div = splitPane.getDividerLocation();
		
		if(_isHorizontalMove)
		{
			// When we enter Full Screen mode, the right Component is temporarily removed
			if(splitPane.getRightComponent() != null)
			{
				splitPane.setRightComponent(null);
				splitPane.setRightComponent(MAIN_PANEL);
			}
		}
		else
		{
			leftPanel.remove(OVERVIEW_PANEL);
			leftPanel.add(OVERVIEW_PANEL,BorderLayout.NORTH);
		}
		
		splitPane.setDividerLocation(div);
	}
	
	private Timer repaintTimer;
	
	public void repaintLazy()
	{
		repaintTimer.start();
	}
	
	public void startWaitCursor()
	{
	    RootPaneContainer root = (RootPaneContainer)getRootPane().getTopLevelAncestor();
	    root.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    root.getGlassPane().setVisible(true);
	}
	 
	public void stopWaitCursor()
	{
	    RootPaneContainer root = (RootPaneContainer)getRootPane().getTopLevelAncestor();
	    root.getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    root.getGlassPane().setVisible(false);
	}

	
	private void initMainFrame()
	{
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(@Nullable WindowEvent e)
			{
				super.windowClosing(e);
				new ExitProgramAction().actionPerformed(new ActionEvent(MainFrame.this, 0, "CLOSING"));
			}
		});
		
		
		Dimension maxSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension minSize = new Dimension(800, 600);

		maxSize.width -= 200;
		
		// if the display is not very high, we want to take most of the height,
		// as the rest is not useful anyway
		if (maxSize.height < 1000)
			maxSize.height -= 100;
		else
			maxSize.height -= 150;

		minSize.width = Math.min(minSize.width, maxSize.width);
		minSize.height = Math.min(minSize.height, maxSize.height);
		setMinimumSize(minSize);
		setPreferredSize(maxSize);
		setFont(new Font("SansSerif", Font.BOLD, 12));
		setIconImage(IconBank.getIcon(JHVIcon.HVLOGO_SMALL).getImage());
		pack();
		setLocationRelativeTo(null);
	}
	
	private JPanel getStatusPane()
	{
		JPanel statusPane = new JPanel();
		GridBagLayout gbl_statusPane = new GridBagLayout();
		gbl_statusPane.columnWidths = new int[]{70, 104, 20, 0, 104, 0};
		gbl_statusPane.rowHeights = new int[]{0};
		gbl_statusPane.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_statusPane.rowWeights = new double[]{0.0};
		statusPane.setLayout(gbl_statusPane);
		

		FramerateStatusPanel framerateStatusPanel = new FramerateStatusPanel();
		GridBagConstraints gbc_framerateStatusPanel = new GridBagConstraints();
		gbc_framerateStatusPanel.fill = GridBagConstraints.BOTH;
		gbc_framerateStatusPanel.insets = new Insets(0, 0, 0, 5);
		gbc_framerateStatusPanel.gridx = 0;
		gbc_framerateStatusPanel.gridy = 0;
		statusPane.add(framerateStatusPanel, gbc_framerateStatusPanel);
		
		ZoomStatusPanel zoomStatusPanel = new ZoomStatusPanel(MAIN_PANEL);
		GridBagConstraints gbc_zoomStatusPanel = new GridBagConstraints();
		gbc_zoomStatusPanel.fill = GridBagConstraints.BOTH;
		gbc_zoomStatusPanel.insets = new Insets(0, 0, 0, 5);
		gbc_zoomStatusPanel.gridx = 1;
		gbc_zoomStatusPanel.gridy = 0;
		statusPane.add(zoomStatusPanel, gbc_zoomStatusPanel);
		
		CurrentTimeLabel currentTimeLabel = new CurrentTimeLabel();
		GridBagConstraints gbc_currentTimeLabel = new GridBagConstraints();
		gbc_currentTimeLabel.fill = GridBagConstraints.BOTH;
		gbc_currentTimeLabel.insets = new Insets(0, 0, 0, 5);
		gbc_currentTimeLabel.gridx = 2;
		gbc_currentTimeLabel.gridy = 0;
		statusPane.add(currentTimeLabel, gbc_currentTimeLabel);
		
		PositionStatusPanel positionStatusPanel = new PositionStatusPanel(this);
		GridBagConstraints gbc_positionStatusPanel = new GridBagConstraints();
		gbc_positionStatusPanel.fill = GridBagConstraints.BOTH;
		gbc_positionStatusPanel.gridx = 4;
		gbc_positionStatusPanel.gridy = 0;
		statusPane.add(positionStatusPanel, gbc_positionStatusPanel);

		return statusPane;
	}
}
