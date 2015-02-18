package org.helioviewer.jhv.plugins.pfssplugin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayPanel;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;

/**
 * Panel of Pfss-Plugin
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPluginPanel extends OverlayPanel implements ActionListener,
		LayersListener, ViewListener {

	private static final long serialVersionUID = 1L;
	private PfssPlugin3dRenderer renderer;
	private boolean showAgain = true;
	private boolean retry = false;
	private CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<String>();
	// UI Components
	private JButton visibleButton = new JButton(new ImageIcon(
			PfssPlugin.getResourceUrl("/images/invisible_dm.png")));
	private JButton reloadButton = new JButton(new ImageIcon(
			PfssPlugin.getResourceUrl("/images/reload.png")));

	/**
	 * Default constructor
	 * 
	 * */
	public PfssPluginPanel(PfssPlugin3dRenderer renderer)
	{
		// set up visual components
		initVisualComponents();
		// register as layers listener
		LayersModel.getSingletonInstance().addLayersListener(this);
		this.renderer = renderer;
	}

	/**
	 * Force a redraw of the main window
	 */
	private void fireRedraw()
	{
	    GuiState3DWCS.mainComponentView.getComponent().repaint();
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

	/**
	 * Updates components.
	 * */
	public void updateComponents() {
	}

	public void actionPerformed(ActionEvent act) {
		if (act.getSource().equals(visibleButton)) {
			if (renderer.isVisible()) {
				renderer.setVisible(false);
				visibleButton.setIcon(new ImageIcon(PfssPlugin
						.getResourceUrl("/images/invisible_dm.png")));
			} else {
				this.showData();
				renderer.setVisible(true);
				visibleButton.setIcon(new ImageIcon(PfssPlugin
						.getResourceUrl("/images/visible_dm.png")));
			}
			
			fireRedraw();
		}

		if (act.getSource().equals(reloadButton)) {
		    messages.clear();
			reload();
		}

	}

	private void showData() {
		if (showAgain)
		{
			for(String message: messages)
			{
				Object[] options = { "Retry", "OK" };
				messages.remove(message);
				JCheckBox checkBox = new JCheckBox(
						"Don't show this message again.");
				checkBox.setEnabled(showAgain);
				Object[] params = { message, checkBox };
				int n = JOptionPane.showOptionDialog(this, params,
							"PFSS data", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE, null, options,
							options[1]);
					showAgain = !checkBox.isSelected();
					if (n == 0) {
						retry = true;
					}
				}
			}
	}

	public void setEnabled(boolean b) {
	}

	public void activeLayerChanged(int idx) {
	}

	public void layerAdded(int idx)
	{
		reload();
	}
	
	public void reload()
	{
		int master = -1000;
		for (int i = 0; i < LayersModel.getSingletonInstance().getNumLayers(); i++){
			if (LayersModel.getSingletonInstance().isMaster(i))
				master = i;
		}
		Date start;
		Date end;
		if (master >=0){
			start = LayersModel.getSingletonInstance().getStartDate(master).getTime();
			end = LayersModel.getSingletonInstance().getStartDate(master).getTime();
		}
		else {
			start = LayersModel.getSingletonInstance().getFirstDate();
			end = LayersModel.getSingletonInstance().getLastDate();
		}
		
		messages.clear();
		if (start != null && end != null) {
				retry = true;
				while(retry) {
				try {
					renderer.setDisplayRange(start, end);
					retry = false;
				} catch (IOException e) {
					if (showAgain) {
						retry = false;
						Object[] options = { "Retry", "OK" };
						String message = e.getMessage();
						JCheckBox checkBox = new JCheckBox(
								"Don't show this message again.");
						if (this.renderer.isVisible()){
							checkBox.setEnabled(showAgain);
							Object[] params = { message, checkBox };
							int n = JOptionPane.showOptionDialog(this, params,
									"Pfss-Data", JOptionPane.YES_NO_CANCEL_OPTION,
									JOptionPane.WARNING_MESSAGE, null, options,
									options[1]);
							showAgain = !checkBox.isSelected();
							retry = n == 0;
							
						}
						else{
							messages.add(message);
						}
					}
				}
			}
		}
	}

	public void layerChanged(int idx) {
	}

	public void layerRemoved(View oldView, int oldIdx) {
		this.reload();
	}

	public void subImageDataChanged() {
	}

	public void timestampChanged(int idx) {
		// Not used anymore
		//handled in renderer
		
	}

	public void viewportGeometryChanged() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void regionChanged() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void layerDownloaded(int idx) {
	}

	@Override
	public void viewChanged(View sender, ChangeEvent aEvent) {
	}
}
