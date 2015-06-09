package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statuslabels.CurrentTimeLabel;
import org.helioviewer.jhv.gui.components.statuslabels.FramerateStatusPanel;
import org.helioviewer.jhv.gui.components.statuslabels.PositionStatusPanel;
import org.helioviewer.jhv.gui.components.statuslabels.ZoomStatusPanel;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class MainFrame extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5593418566466319335L;
	
	public static final MainPanel MAIN_PANEL = new MainPanel();
	public static final OverViewPanel OVERVIEW_PANEL = new OverViewPanel();
	public static final TopToolBar TOP_TOOL_BAR = new TopToolBar();
	public static final MainFrame SINGLETON = new MainFrame();
	public static final int SIDE_PANEL_WIDTH = 320;
	public static SideContentPane LEFT_PANE;

	private JSplitPane splitPane;

	public FilterTabPanel filterTabPanel;



	public MainFrame() {
		super("ESA JHelioviewer");
		initMainFrame();
		initMenuBar();
		initGui();
		
	}
	
	private void initMainFrame(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
		setMinimumSize(minSize);
		setPreferredSize(maxSize);
		setFont(new Font("SansSerif", Font.BOLD, 12));
		setIconImage(IconBank.getIcon(JHVIcon.HVLOGO_SMALL).getImage());
		this.pack();
		setLocationRelativeTo(null);
	}
	
	private void initGui(){			
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
		OVERVIEW_PANEL.setMinimumSize(new Dimension(200, 200));
		OVERVIEW_PANEL.setPreferredSize(new Dimension(240, 200));

		splitPane.setRightComponent(MAIN_PANEL);		
		splitPane.setLeftComponent(getLeftPane());
		
		contentPane.add(this.getStatusPane(), BorderLayout.SOUTH);		
	}
	
	private JPanel getLeftPane(){
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
		OVERVIEW_PANEL.addMainView(MAIN_PANEL);
		MAIN_PANEL.addSynchronizedView(OVERVIEW_PANEL);
		
		JPanel scrollContentPane = new JPanel(new BorderLayout());
		scrollContentPane.setMinimumSize(new Dimension());
		JScrollPane scrollPane = new JScrollPane(scrollContentPane);
		scrollPane.setMinimumSize(new Dimension());
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		left.add(scrollPane, gbc_scrollPane);
		
		scrollContentPane.add(getSideBar());
		
		
		this.addComponentListener(new ComponentAdapter() {
						
			@Override
			public void componentResized(ComponentEvent e) {
				// this is a hack to support GLCanvas as AWT in a splitpane
				splitPane.setDividerLocation(splitPane.getDividerLocation()+1);
				SwingUtilities.invokeLater(new Runnable() {	
					@Override
					public void run() {
						splitPane.setDividerLocation(splitPane.getDividerLocation()-1);
					}
				});
			}
		});
		return left;
	}
	
	private JPanel getStatusPane(){
		JPanel statusPane = new JPanel();
		
		statusPane.setLayout(new FormLayout(new ColumnSpec[] {
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
				FormFactory.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC,}));
		

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
	
	private SideContentPane getSideBar(){
		LEFT_PANE = new SideContentPane();
		// Movie control
		NewPlayPanel moviePanel = new NewPlayPanel();
		LEFT_PANE.add("Movie Controls", moviePanel, true);
		
		// Layer control
		NewLayerPanel newLayerPanel = new NewLayerPanel();
		LEFT_PANE.add("Layers", newLayerPanel, true);

		// Filter control
		filterTabPanel = new FilterTabPanel();
		LEFT_PANE.add("Adjustments", filterTabPanel , true);
		
		return LEFT_PANE;
	}

	private void initMenuBar(){
		JMenuBar menuBar = new MenuBar();
		menuBar.setMinimumSize(new Dimension());
		this.setJMenuBar(menuBar);		
	}

	public void addTopToolBarPlugin(
			PropertyChangeListener propertyChangeListener,
			JToggleButton button) {
		MainFrame.TOP_TOOL_BAR.addToolbarPlugin(button);

		MainFrame.TOP_TOOL_BAR
				.addPropertyChangeListener(propertyChangeListener);

		MainFrame.TOP_TOOL_BAR.validate();
		MainFrame.TOP_TOOL_BAR.repaint();
	}
}
