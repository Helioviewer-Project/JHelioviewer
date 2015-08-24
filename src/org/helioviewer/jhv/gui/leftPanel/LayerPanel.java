package org.helioviewer.jhv.gui.leftPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDateTime;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.dialogs.AddLayerPanel;
import org.helioviewer.jhv.gui.dialogs.DownloadMovieDialog;
import org.helioviewer.jhv.gui.dialogs.InstrumentModel;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

/**
 * The new LayerPanel, include a JTable for the current added layers
 * 
 * @author stefanmeier
 *
 */
public class LayerPanel extends JPanel implements LayerListener,
		TimeLineListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6800340702841902680L;
	private static int size;
	static{
		JLabel label = new JLabel("size");
		size = label.getPreferredSize().height;
	}
	private final AddLayerPanel addLayerPanel = new AddLayerPanel();
	private final MetaDataDialog metaDataDialog = new MetaDataDialog();
	private final DownloadMovieDialog downloadMovieDialog = new DownloadMovieDialog();
	private JTable table;
	private Object columnNames[] = { "Column One", "Column Two",
			"Column Three", "Column Four", "Column Five" };
	private LayerTableModel tableModel;
	private Object rowData[][] = {
			{ "Row1-Column1", "Row1-Column2", "Row1-Column3" },
			{ "Row2-Column1", "Row2-Column2", "Row2-Column3" } };

	private JButton btnDownloadLayer;
	private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
	private static final ImageIcon WARNING_BAD_REQUEST = IconBank.getIcon(
			JHVIcon.WARNING, size, size);
	private int activePopupLayer = 0;

	private JPopupMenu popupMenu;

	private JMenuItem showLayer;
	private JMenuItem hideLayer;
	private JMenuItem showMetaView;
	private JMenuItem downloadLayer;
	private JMenuItem removeLayer;
	
	private JButton btnShowInfo;
	private static final int lblHeight;
	private static final int btnHeight;

	static{
		JLabel lblTest = new JLabel("test");
		JButton btnTest = new JButton();
		lblHeight = lblTest.getPreferredSize().height;
		btnHeight = btnTest.getPreferredSize().height;
	}
	
	public LayerPanel() {
		JLabel size = new JLabel("size");
		double x = size.getPreferredSize().getHeight();
		initPopup();
		initGUI();
		updateData();
		InstrumentModel.addAddLayerPanel(addLayerPanel);
		this.setMinimumSize(new Dimension(240, 200));
		this.setPreferredSize(new Dimension(240, 200));
		Layers.addNewLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
	}

	private void initPopup() {
		popupMenu = new JPopupMenu();
		showMetaView = new JMenuItem("Show metainfo...",
				IconBank.getIcon(JHVIcon.INFO_NEW, size, size));
		showMetaView.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				metaDataDialog.showDialog();
			}
		});
		downloadLayer = new JMenuItem("Download movie",
				IconBank.getIcon(JHVIcon.DOWNLOAD_NEW, size, size));
		downloadLayer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				downloadMovieDialog.startDownload(Layers.getLayer(
						activePopupLayer).getURL(), Layers.getLayer(activePopupLayer));
			}
		});
		hideLayer = new JMenuItem("Hide layer", IconBank.getIcon(
				JHVIcon.HIDDEN, size, size));
		hideLayer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Layers.getLayer(activePopupLayer).setVisible(false);
				updateData();
			}
		});
		showLayer = new JMenuItem("Show layer", IconBank.getIcon(
				JHVIcon.VISIBLE, size, size));
		showLayer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Layers.getLayer(activePopupLayer).setVisible(true);
				updateData();
			}
		});
				
		removeLayer = new JMenuItem("Close layer", IconBank.getIcon(
				JHVIcon.REMOVE_NEW, size, size));
		removeLayer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Layers.removeLayer(activePopupLayer);
				updateData();
			}
		});
		showLayer.setVisible(false);
		hideLayer.setVisible(false);

		popupMenu.add(showMetaView);
		popupMenu.add(downloadLayer);
		popupMenu.addSeparator();
		popupMenu.add(hideLayer);
		popupMenu.add(showLayer);
		popupMenu.add(removeLayer);

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				if (Layers.getLayer(activePopupLayer).isImageLayer()){
					showMetaView.setEnabled(true);
					downloadLayer.setEnabled(Layers.getLayer(activePopupLayer).isDownloadable());
					removeLayer.setEnabled(false);
				}
				else {
					showMetaView.setEnabled(false);
					downloadLayer.setEnabled(false);
					removeLayer.setEnabled(false);
				}
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	
	/**
	 * Create the panel.
	 */
	private void initGUI() {
		setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);

		tableModel = new LayerTableModel(null, null);
		table = new JTable(tableModel);
		table = new JTable(rowData, columnNames);
		table.setTableHeader(null);
		table.getColumnModel().getColumn(0)
				.setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(1)
				.setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(2)
				.setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(3)
				.setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(4)
				.setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(0).setPreferredWidth(35);
		table.getColumnModel().getColumn(0).setWidth(35);
		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(3).setResizable(false);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								int row = table.getSelectedRow();
								if (row != Layers.getActiveLayerNumber()) {
									Layers.setActiveLayer(row);
								}
							}
						});
					}
				});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JTable jTable = (JTable) e.getSource();
				int row = jTable.rowAtPoint(e.getPoint());
				int column = table.getSelectedColumn();
				if (column == 4) {
					Layers.removeLayer(row);
					updateData();
				} else if (column == 0) {
					AbstractLayer layer = Layers.getLayer(row);
					if (layer != null) {
						layer.setVisible(!layer.isVisible());
					}
				} else if (column == 1) {
					boolean value = (boolean) table.getValueAt(row, column);
					if (value) {
						Object[] options = {"Retry failed downloads", "Remove layer", "Ignore"};

						int n = JOptionPane.showOptionDialog(MainFrame.SINGLETON, "Images could not be downloaded. Server didn't replied. This happened with "+ Layers.getLayer(row).getBadRequestCount() +" other requests as well.", "Images could not be downloaded", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
						switch (n) {
						case 0:
							Layers.getLayer(row).retryBadRequest();
							break;
						case 1:
							Layers.removeLayer(row);
							break;
						case 2:
							Layers.getLayer(row).clearBadRequests();
							break;
						default:
							break;
						}
					}
				}

			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					JTable jTable = (JTable) e.getSource();
					int row = jTable.rowAtPoint(e.getPoint());
					if (row >= 0 && row < Layers.getLayerCount()) {
						activePopupLayer = row;
						hideLayer.setVisible(Layers.getLayer(activePopupLayer)
								.isVisible());
						showLayer.setVisible(!Layers.getLayer(activePopupLayer)
								.isVisible());
						popupMenu.show(jTable, e.getX(), e.getY());
					}
				}
			}
		});

		table.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				JTable jTable = (JTable) e.getSource();
				int row = jTable.rowAtPoint(e.getPoint());
				int column = table.columnAtPoint(e.getPoint());
				if (column == 0 || column == 4)
					table.setCursor(HAND_CURSOR);
				else if (column == 1){
					boolean value = (boolean) table.getValueAt(row, column);
					if (value){
						table.setCursor(HAND_CURSOR);						
					}
					else 
						table.setCursor(Cursor.getDefaultCursor());
				}
				else
					table.setCursor(Cursor.getDefaultCursor());
			}
		});

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE
						|| e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
					int row = table.getSelectedRow();
					Layers.removeLayer(row);
					updateData();
				}
			}
		});
		scrollPane.setViewportView(table);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0 };
		gbl_panel.rowHeights = new int[] { 0, 0 };
		gbl_panel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);

		btnShowInfo = new JButton(IconBank.getIcon(JHVIcon.INFO_NEW,
				size, size));
		btnShowInfo
				.setToolTipText("Show the Metainformation of the currently selected Layer");
		btnShowInfo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				metaDataDialog.showDialog();
			}
		});
		GridBagConstraints gbcBtnShowInfo = new GridBagConstraints();
		gbcBtnShowInfo.insets = new Insets(0, 0, 0, 5);
		gbcBtnShowInfo.gridx = 9;
		gbcBtnShowInfo.gridy = 0;
		panel.add(btnShowInfo, gbcBtnShowInfo);

		btnDownloadLayer = new JButton(IconBank.getIcon(JHVIcon.DOWNLOAD_NEW,
				size, size));
		btnDownloadLayer
				.setToolTipText("Download the currently selected Layer");
		btnDownloadLayer.setEnabled(false);
		btnDownloadLayer.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (Layers.getActiveImageLayer() != null) {
					downloadMovieDialog.startDownload(Layers.getActiveLayer()
							.getURL(), Layers.getActiveLayer());
				}
			}
		});
		GridBagConstraints gbcBtnDownloadLayer = new GridBagConstraints();
		gbcBtnDownloadLayer.insets = new Insets(0, 0, 0, 5);
		gbcBtnDownloadLayer.gridx = 10;
		gbcBtnDownloadLayer.gridy = 0;
		panel.add(btnDownloadLayer, gbcBtnDownloadLayer);

		JButton btnAddLayer = new JButton("Add Layer", IconBank.getIcon(
				JHVIcon.ADD_NEW, size, size));
		btnAddLayer.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addLayerPanel.setVisible(true);
			}
		});
		btnAddLayer.setToolTipText("Add a new Layer");
		GridBagConstraints gbcBtnAddLayer = new GridBagConstraints();
		gbcBtnAddLayer.anchor = GridBagConstraints.EAST;
		gbcBtnAddLayer.gridx = 11;
		gbcBtnAddLayer.gridy = 0;
		panel.add(btnAddLayer, gbcBtnAddLayer);

	}

	private synchronized void updateData() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				Object[][] data = new Object[Layers.getLayerCount()][5];
				int count = 0;
				for (AbstractLayer layer : Layers.getLayers()) {
					data[count][0] = layer.isVisible();

					data[count][1] = layer.checkBadRequest();

					data[count][2] = layer.getName();
					data[count][3] = layer.getTime() == null ? null : layer
							.getTime();
					data[count][4] = IconBank.getIcon(JHVIcon.REMOVE_NEW, size,
							size);
					count++;
				}
				tableModel.setDataVector(data, columnNames);
				table.setModel(tableModel);

				// table.getColumnModel().getColumn(0).setCellRenderer(new
				// ImageIconCellRenderer());
				table.getColumnModel().getColumn(1)
						.setCellRenderer(new ImageIconCellRenderer());
				table.getColumnModel().getColumn(2)
						.setCellRenderer(new ImageIconCellRenderer());
				table.getColumnModel().getColumn(3)
						.setCellRenderer(new ImageIconCellRenderer());
				table.getColumnModel().getColumn(4)
						.setCellRenderer(new ImageIconCellRenderer());
				setFixedWidth(size+4, 0);
				setFixedWidth(size, 1);
				setFixedWidth(size, 4);
				// table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				table.setShowGrid(false);
				table.setIntercellSpacing(new Dimension(0, 0));

				if (Layers.getLayerCount() > 0 && Layers.getLayerCount() > Layers.getActiveLayerNumber()) {
					table.setRowSelectionInterval(
							Layers.getActiveLayerNumber(),
							Layers.getActiveLayerNumber());
				}
			}
		});
	}

	private void setFixedWidth(int width, int column) {
		table.getColumnModel().getColumn(column).setMinWidth(width);
		table.getColumnModel().getColumn(column).setMaxWidth(width);
		table.getColumnModel().getColumn(column).setPreferredWidth(width);
	}

	private static class LayerTableModel extends DefaultTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5224476911114851064L;

		public LayerTableModel(Object[][] data, String[] columnNames) {
			super(data, columnNames);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return Boolean.class;
			}

			return super.getColumnClass(columnIndex);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if (column == 0) {
				return true;
			}
			return false;
		}
	}

	private static class ImageIconCellRenderer extends DefaultTableCellRenderer {

		/**
		 * 
		 */
		private static final long serialVersionUID = -2552431402411803683L;

		public ImageIconCellRenderer() {
			// setOpaque(true);
			setHorizontalAlignment(CENTER);
			// setBackground(Color.WHITE);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus,
				final int row, int column) {
			switch (column) {
			case 0:
				super.getTableCellRendererComponent(table, null, isSelected,
						hasFocus, row, column);
				break;
			case 1:
				JLabel label = (JLabel) super
				.getTableCellRendererComponent(table, null,
						isSelected, hasFocus, row, column);
				if ((Boolean) value == true) {
					label.setIcon(WARNING_BAD_REQUEST);
					label.setPreferredSize(new Dimension(20, 20));
					
				}
				else label.setIcon(null);;
				break;
			case 2:
				super.getTableCellRendererComponent(table, value, isSelected,
						hasFocus, row, column);
				break;
			case 3:
				LocalDateTime localDateTime = (LocalDateTime) value;
				String date = localDateTime != null ? localDateTime
						.format(JHVGlobals.DATE_TIME_FORMATTER) : "";
				super.getTableCellRendererComponent(table, date, isSelected,
						hasFocus, row, column);
				break;
			case 4:
				JLabel label4 = (JLabel) super.getTableCellRendererComponent(
						table, null, isSelected, hasFocus, row, column);
				if (Layers.getLayer(row).isImageLayer()){
				label4.setIcon((ImageIcon) value);
				label4.setPreferredSize(new Dimension(20, 20));
				}
				else{
					label4.setIcon(null);
				}
				return label4;
			default:
				break;
			}
			return this;
		}

	}

	@Override
	public void newlayerAdded() {
			updateData();			
	}

	@Override
	public void newlayerRemoved(int idx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activeLayerChanged(AbstractLayer layer) {
		if (layer != null){
			btnDownloadLayer.setEnabled(layer.isDownloadable());
			btnShowInfo.setEnabled(layer.isImageLayer());
		}
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
			updateData();			
	}

	@Override
	public void dateTimesChanged(int framecount) {
	}

	public void repaintPanel() {
			updateData();			
	}
}
