package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.leftPanel.FilterPanel;
import org.helioviewer.jhv.gui.leftPanel.LayerPanel;
import org.helioviewer.jhv.gui.leftPanel.MoviePanel;
import org.helioviewer.jhv.gui.statusLabels.CurrentTimeLabel;
import org.helioviewer.jhv.gui.statusLabels.FramerateStatusPanel;
import org.helioviewer.jhv.gui.statusLabels.PositionStatusPanel;
import org.helioviewer.jhv.gui.statusLabels.ZoomStatusPanel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jogamp.opengl.GLContext;

public class MainFrame extends JFrame
{
	public static MainPanel MAIN_PANEL;
	public static OverviewPanel OVERVIEW_PANEL;
	public static final TopToolBar TOP_TOOL_BAR = new TopToolBar();
	public static final MainFrame SINGLETON = new MainFrame();
	public static final int SIDE_PANEL_WIDTH = 320;
	public static SideContentPane LEFT_PANE;
	public static MoviePanel MOVIE_PANEL;
	public static LayerPanel LAYER_PANEL;
	private JSplitPane splitPane;

	public static FilterPanel FILTER_PANEL;
	
	private MainFrame()
	{
		super("ESA JHelioviewer");
	}
	
	public void initContext()
	{
		GLContext context = GLContext.getCurrent();
		
		MAIN_PANEL = new MainPanel(context);
		OVERVIEW_PANEL = new OverviewPanel(context);
		initMainFrame();
		initMenuBar();
		initGui();
	}
	
	private void initMainFrame()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
		this.pack();
		setLocationRelativeTo(null);
	}
	
	private void initGui()
	{
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		// Add toolbar
		contentPane.add(TOP_TOOL_BAR, BorderLayout.NORTH);
		
		splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.0);
		splitPane.setContinuousLayout(true);
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		MAIN_PANEL.setMinimumSize(new Dimension());
		OVERVIEW_PANEL.setMinimumSize(new Dimension(240, 200));
		OVERVIEW_PANEL.setPreferredSize(new Dimension(240, 200));

		splitPane.setRightComponent(MAIN_PANEL);		
		splitPane.setLeftComponent(createLeftPanel());
		
		contentPane.add(this.getStatusPane(), BorderLayout.SOUTH);		
	}
	
	private JPanel createLeftPanel()
	{
		JPanel left = new JPanel();
		GridBagLayout gbl_left = new GridBagLayout();
		gbl_left.columnWidths = new int[]{0, 0};
		gbl_left.rowHeights = new int[]{0, 0, 0};
		gbl_left.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_left.rowWeights = new double[]{1.0, 100.0, Double.MIN_VALUE};
		left.setLayout(gbl_left);
		
		GridBagConstraints gbc_overViewPane = new GridBagConstraints();
		gbc_overViewPane.insets = new Insets(0, 0, 5, 0);
		gbc_overViewPane.fill = GridBagConstraints.BOTH;
		gbc_overViewPane.gridx = 0;
		gbc_overViewPane.gridy = 0;
		
		left.add(OVERVIEW_PANEL, gbc_overViewPane);
		OVERVIEW_PANEL.setMinimumSize(new Dimension(450, 200));
		OVERVIEW_PANEL.addMainView(MAIN_PANEL);
		MAIN_PANEL.addSynchronizedView(OVERVIEW_PANEL);
		
		JPanel scrollContentPane = new JPanel(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(scrollContentPane);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		left.add(scrollPane, gbc_scrollPane);
		
		scrollContentPane.add(getSideBar());
		
		//F-IXME: still needed?
		/*this.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				// this is a hack to support GLCanvas as AWT in a splitpane
				splitPane.setDividerLocation(splitPane.getDividerLocation()+1);
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						splitPane.setDividerLocation(splitPane.getDividerLocation()-1);
					}
				});
			}
		});*/
		return left;
	}
	
	private JPanel getStatusPane()
	{
		JPanel statusPane = new JPanel();
		
		statusPane.setLayout(new FormLayout(
			new ColumnSpec[]
			{
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC
			},
			new RowSpec[]
			{
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC
			}));
		

		FramerateStatusPanel framerateStatusPanel = new FramerateStatusPanel();
		statusPane.add(framerateStatusPanel, "2, 2, fill, fill");
		
		ZoomStatusPanel zoomStatusPanel = new ZoomStatusPanel();
		statusPane.add(zoomStatusPanel, "4, 2, fill, fill");
		
		CurrentTimeLabel currentTimeLabel = new CurrentTimeLabel();
		statusPane.add(currentTimeLabel, "6, 2, fill, fill");
		
		PositionStatusPanel positionStatusPanel = new PositionStatusPanel(this);
		statusPane.add(positionStatusPanel, "10, 2, fill, fill");

		return statusPane;
	}
	
	private SideContentPane getSideBar()
	{
		LEFT_PANE = new SideContentPane();
		LEFT_PANE.setMinimumSize(new Dimension(300, 200));
		// Movie control
		MOVIE_PANEL = new MoviePanel();
		LEFT_PANE.add("Movie Controls", MOVIE_PANEL, true);
		
		// Layer control
		LAYER_PANEL = new LayerPanel();
		LEFT_PANE.add("Layers", LAYER_PANEL, true);

		// Filter control
		FILTER_PANEL = new FilterPanel();
		LEFT_PANE.add("Adjustments", FILTER_PANEL , true);
		
		return LEFT_PANE;
	}

	private void initMenuBar()
	{
		JMenuBar menuBar = new MenuBar();
		menuBar.setMinimumSize(new Dimension());
		this.setJMenuBar(menuBar);		
	}
}
