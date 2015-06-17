package org.helioviewer.jhv.plugins.pfssplugin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayPanel;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

/**
 * Panel of Pfss-Plugin
 * */
public class PfssPluginPanel extends OverlayPanel implements ActionListener{

	private static final long serialVersionUID = 1L;

	// UI Components
	private JButton visibleButton = new JButton(new ImageIcon(PfssPlugin.getResourceUrl("/images/invisible_dm.png")));
	private JButton reloadButton = new JButton(new ImageIcon(PfssPlugin.getResourceUrl("/images/reload.png")));
	private PfssPlugin renderer;
	
	/**
	 * Default constructor
	 * 
	 * */
	public PfssPluginPanel(PfssPlugin renderer)
	{
		// set up visual components
		initVisualComponents();
		// register as layers listener
		this.renderer = renderer;
	}

	/**
	 * Force a redraw of the main window
	 */
	private void fireRedraw()
	{
		MainFrame.MAIN_PANEL.repaint();
	}

	/**
	 * Sets up the visual sub components and the visual part of the component
	 * itself.
	 * */
	private void initVisualComponents()
	{
		// set general appearance
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0,
				Double.MIN_VALUE };
		setLayout(gridBagLayout);

		visibleButton.addActionListener(this);
		reloadButton.addActionListener(this);

		setEnabled(true);

		GridBagConstraints c3 = new GridBagConstraints();
		c3.insets = new Insets(0, 0, 5, 0);
		c3.gridx = 1;
		c3.gridy = 0;

		this.add(visibleButton, c3);
		visibleButton.setToolTipText("disable/enable PFSS");
		GridBagConstraints c6 = new GridBagConstraints();
		c6.insets = new Insets(0, 0, 5, 0);
		c6.gridx = 2;
		c6.gridy = 0;

		this.add(reloadButton, c6);
		reloadButton.setToolTipText("reload PFSS data");
	}

	public void actionPerformed(ActionEvent act) {
		if (act.getSource().equals(visibleButton)) {
			if (renderer.isVisible()) {
				renderer.setVisible(false);
				visibleButton.setIcon(new ImageIcon(PfssPlugin
						.getResourceUrl("/images/invisible_dm.png")));
			}
			else
			{
				renderer.setVisible(true);
				visibleButton.setIcon(new ImageIcon(PfssPlugin
						.getResourceUrl("/images/visible_dm.png")));
			}
			
			fireRedraw();
		}

		if (act.getSource().equals(reloadButton))
			reload();
	}

	public void setEnabled(boolean b)
	{
	}

	public void activeLayerChanged(int idx)
	{
	}

	public void layerAdded(int idx)
	{
		reload();
	}
	
	public void reload()
	{
		LocalDateTime startLocalDateTime = TimeLine.SINGLETON.getFirstDateTime();
		LocalDateTime endLocalDateTime = TimeLine.SINGLETON.getLastDateTime();
		if (startLocalDateTime != null && endLocalDateTime != null)
			renderer.setDisplayRange(startLocalDateTime, endLocalDateTime);
		
	}
}
