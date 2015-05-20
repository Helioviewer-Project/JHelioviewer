package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;

import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class MainFrame extends JFrame{

	public static final MainPanel MAIN_PANEL = new MainPanel();
	public static final OverViewPanel OVERVIEW_PANEL = new OverViewPanel();
	public static final TopToolBar TOP_TOOL_BAR = new TopToolBar();


	public MainFrame() {
		super("ESA JHelioviewer");
		initMenuBar();
		initGui();
	}
	
	
	private void initGui(){
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 755, 651);
		setMinimumSize(new Dimension(400, 200));
		
		JPanel contentPane = new JPanel(new BorderLayout());
		setContentPane(contentPane);
		
		// add top toolbar
		contentPane.add(MainFrame.TOP_TOOL_BAR, BorderLayout.NORTH);
		
		JPanel middleFrame = new JPanel(new BorderLayout());
		//middleFrame.setMinimumSize(new Dimension(400, 200));
		// Add splitpane
		JSplitPane splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, initLeftPane(), MAIN_PANEL);
		
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(false);
		middleFrame.add(splitPane, BorderLayout.CENTER);
		contentPane.add(middleFrame, BorderLayout.CENTER);
		
		
		JPanel right = new JPanel(new BorderLayout());
		right.setMinimumSize(new Dimension());
		splitPane.setRightComponent(right);
		right.add(MAIN_PANEL);
		MAIN_PANEL.setMinimumSize(new Dimension());
		//right.add(MainFrame.MAIN_PANEL, BorderLayout.CENTER);
		//splitPane.setLeftComponent(initLeftPane());
		//splitPane.setLeftComponent(OVERVIEW_PANEL);
		//OVERVIEW_PANEL.setMinimumSize(new Dimension());
		splitPane.setResizeWeight(0);

		JPanel bottom = new JPanel(new BorderLayout());
		contentPane.add(bottom, BorderLayout.SOUTH);
	}
	
	private JPanel initLeftPane(){
		final JPanel left = new JPanel(new BorderLayout());
		left.setMinimumSize(new Dimension(200, 200));
		JPanel overViewContentPane = new JPanel(new BorderLayout());
		overViewContentPane.setMinimumSize(new Dimension());
		overViewContentPane.add(OVERVIEW_PANEL, BorderLayout.CENTER);
		left.add(OVERVIEW_PANEL, BorderLayout.NORTH);
		OVERVIEW_PANEL.setMinimumSize(new Dimension(200, 200));
		OVERVIEW_PANEL.setPreferredSize(new Dimension(200, 200));
		//left.add(OVERVIEW_PANEL, BorderLayout.CENTER);
		
		OVERVIEW_PANEL.addMainView(MAIN_PANEL);
		//overViewPanel.setMinimumSize(new Dimension(200, 210));
		//overViewPanel.setPreferredSize(new Dimension(200, 210));
		//left.add(overViewPanel, BorderLayout.CENTER);
		
		addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				MainFrame.this.invalidate();
				MainFrame.this.validate();
			}
			
			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		JSeparator separator = new JSeparator();
		separator.setMinimumSize(new Dimension());
		//left.add(separator, BorderLayout.NORTH);
		JPanel leftContainer = new JPanel(new BorderLayout());
		leftContainer.setMinimumSize(new Dimension());
		//left.add(leftContainer, BorderLayout.CENTER);
		leftContainer.add(separator, BorderLayout.NORTH);
		
		
		
		
		SideContentPane leftContentPane = new SideContentPane();
		leftContentPane.setMinimumSize(new Dimension());
		
		JPanel test = new JPanel();
		test.setMinimumSize(new Dimension());
		test.setBackground(Color.BLUE);
		test.setPreferredSize(new Dimension(200, 500));
		
		
		JScrollPane leftScrollPane = new JScrollPane(test, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//leftScrollPane.add(leftContentPane);

		
		leftScrollPane.setFocusable(false);
		leftScrollPane.getVerticalScrollBar().setUnitIncrement(10);
		
		
		leftContainer.add(leftScrollPane, BorderLayout.CENTER);
		leftContainer.setMinimumSize(new Dimension());
		
		leftContentPane.add("Movie Controls", new NewPlayPanel(), true);
		return left;
	}

	private void initMenuBar(){
		JMenuBar menuBar = new MenuBar();
		menuBar.setFocusable(false);
		this.setJMenuBar(menuBar);
		
	}
	
	public static void main(String[] args) {
		MainFrame mainFrame = new MainFrame();
		mainFrame.setVisible(true);
	}
		
}
