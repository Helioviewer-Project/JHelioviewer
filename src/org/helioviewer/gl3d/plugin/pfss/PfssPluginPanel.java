package org.helioviewer.gl3d.plugin.pfss;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.plugin.pfss.data.PfssCache;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.opengl.GLView;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

/**
 * Panel of Pfss-Plugin
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPluginPanel extends OverlayPanel implements ActionListener,
		LayersListener, ViewListener {

	private static final long serialVersionUID = 1L;
	private PfssPlugin3dRenderer renderer;
	private PfssCache pfssCache = null;
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
	public PfssPluginPanel(PfssCache pfssCache,PfssPlugin3dRenderer renderer) {
		this.pfssCache = pfssCache;
		// set up visual components
		initVisualComponents();
		// register as layers listener
		LayersModel.getSingletonInstance().addLayersListener(this);
		this.renderer = renderer;
	}

	/**
	 * Force a redraw of the main window
	 */
	private void fireRedraw() {
		LayersModel.getSingletonInstance().viewChanged(null,
				new ChangeEvent(new SubImageDataChangedReason(null)));
	}

	/**
	 * Sets up the visual sub components and the visual part of the component
	 * itself.
	 * */
	private void initVisualComponents() {

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
		}

		if (act.getSource().equals(reloadButton)) {
			layerAdded(0);
		}

	}

	private void showData() {
		if (showAgain) {
			for(String message: messages){
				
				Object[] options = { "Retry", "OK" };
				Log.error(message);
				messages.remove(message);
				JCheckBox checkBox = new JCheckBox(
						"Don't show this message again.");
				checkBox.setEnabled(showAgain);
				Object[] params = { message, checkBox };
				int n = JOptionPane.showOptionDialog(this, params,
							"Pfss-Data", JOptionPane.YES_NO_CANCEL_OPTION,
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

	public void layerAdded(int idx) {
		this.reload();
	}
	
	public void reload(){
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

		if (start != null && end != null) {
			Calendar startCal = GregorianCalendar.getInstance();
			startCal.setTime(start);

			Calendar endCal = GregorianCalendar.getInstance();
			endCal.setTime(end);
			int startYear = startCal.get(Calendar.YEAR);
			int startMonth = startCal.get(Calendar.MONTH);
			int endYear = endCal.get(Calendar.YEAR);
			int endMonth = endCal.get(Calendar.MONTH);
			boolean run = true;

			while (run) {
				retry = false;
				URL data;
				try {
					String m = (startMonth) < 9 ? "0" + (startMonth + 1)
							: (startMonth + 1) + "";
					data = new URL("http://soleil.i4ds.ch/sol-win/" + startYear
							+ "/" + m + "/list.txt");
					BufferedReader in = new BufferedReader(
							new InputStreamReader(data.openStream()));

					String inputLine;
					String[] splitted = null;
					String url;
					String[] date;
					String[] time;
					while ((inputLine = in.readLine()) != null) {
						splitted = inputLine.split(" ");
						url = splitted[1];
						splitted = splitted[0].split("T");
						date = splitted[0].split("-");
						time = splitted[1].split(":");
						pfssCache.addData(
								startYear,
								startMonth,
								Integer.parseInt(date[2]) * 1000000
										+ Integer.parseInt(time[0]) * 10000
										+ Integer.parseInt(time[1]) * 100
										+ Integer.parseInt(time[2]), url);
					}
					in.close();

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					if (showAgain) {

						Object[] options = { "Retry", "OK" };
						String message = "PFSS data for " + startYear + "-"
								+ (startMonth + 1) + " is not available";
						Log.error(message);
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
							if (n == 0) {
								retry = true;
							}
						}
						else{
							messages.add(message);
						}
					}
				}

				if (!retry) {
					pfssCache.preloadData(startYear, startMonth,
							startCal.get(Calendar.DAY_OF_MONTH) * 1000000
									+ startCal.get(Calendar.HOUR_OF_DAY)
									* 10000 + startCal.get(Calendar.MINUTE)
									* 100 + startCal.get(Calendar.SECOND));
					// pfssCache.addData(startYear, startMonth, dayAndTime,
					// url);
					if (startYear == endYear && startMonth == endMonth)
						run = false;
					else if (startYear == endYear && startMonth < endMonth) {
						startMonth++;
					} else if (startYear < endYear) {
						if (startMonth == 11) {
							startMonth = 1;
							startYear++;
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
		TimedMovieView masterView = LinkedMovieManager.getActiveInstance()
				.getMasterMovie();
		if (masterView != null) {
			Date date = masterView.getCurrentFrameDateTime().getTime();
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(date);

			pfssCache.updateData(
					cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH),
					cal.get(Calendar.DAY_OF_MONTH) * 1000000
							+ cal.get(Calendar.HOUR_OF_DAY) * 10000
							+ cal.get(Calendar.MINUTE) * 100
							+ cal.get(Calendar.SECOND));
		}
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

	public static void main(String[] args) {
		new PfssPluginPanel(null,null);
	}
}
