package org.helioviewer.jhv.gui.leftPanel;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.time.LocalDateTime;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.JHVException.LayerException;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.dialogs.AddLayerPanel;
import org.helioviewer.jhv.gui.dialogs.DownloadMovieDialog;
import org.helioviewer.jhv.gui.dialogs.InstrumentModel;
import org.helioviewer.jhv.gui.dialogs.MetaDataDialog;
import org.helioviewer.jhv.layers.LayerInterface;
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

	private final AddLayerPanel addLayerPanel = new AddLayerPanel();
	private final MetaDataDialog metaDataDialog = new MetaDataDialog();
	private final DownloadMovieDialog downloadMovieDialog = new DownloadMovieDialog();
	private JTable table;
	private Object columnNames[] = { "Column One", "Column Two",
			"Column Three", "Column Four" };
	private LayerTableModel tableModel;
	private Object rowData[][] = {
			{ "Row1-Column1", "Row1-Column2", "Row1-Column3" },
			{ "Row2-Column1", "Row2-Column2", "Row2-Column3" } };

	private JButton btnDownloadLayer;
	
	public LayerPanel() {
		initGUI();
		updateData();
		InstrumentModel.addAddLayerPanel(addLayerPanel);
		this.setMinimumSize(new Dimension(200, 200));
		this.setPreferredSize(new Dimension(200, 200));
		Layers.addNewLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
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
				int row = table.getSelectedRow();
				int column = table.getSelectedColumn();
				if (column == 3) {
					Layers.removeLayer(row);
					updateData();
				} else if (column == 0) {
					// TODO: potential null pointer exception? other callers of
					// getActiveLayer() check for != null...
					try {
						Layers.getActiveLayer().setVisible(
								(boolean) table.getModel().getValueAt(row,
										column));
					} catch (LayerException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

			}
		});
		
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
					int row = table.getSelectedRow();
					Layers.removeLayer(row);
					updateData();
				}
			}
		});
	scrollPane.setViewportView(table);

		/*
		 * for (int i = 0; i < table.getColumnCount(); i++){
		 * table.getColumnModel().getColumn(i) .setCellRenderer(new
		 * LayerCellRenderer()); }
		 */
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

		JButton btnShowInfo = new JButton(IconBank.getIcon(JHVIcon.INFO_NEW,
				16, 16));
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

		btnDownloadLayer = new JButton(IconBank.getIcon(
				JHVIcon.DOWNLOAD_NEW, 16, 16));
		btnDownloadLayer
				.setToolTipText("Download the currently selected Layer");
		btnDownloadLayer.setEnabled(false);
		btnDownloadLayer.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					downloadMovieDialog.startDownload(Layers.getActiveLayer()
							.getURL());
				} catch (LayerException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		GridBagConstraints gbcBtnDownloadLayer = new GridBagConstraints();
		gbcBtnDownloadLayer.insets = new Insets(0, 0, 0, 5);
		gbcBtnDownloadLayer.gridx = 10;
		gbcBtnDownloadLayer.gridy = 0;
		panel.add(btnDownloadLayer, gbcBtnDownloadLayer);

		JButton btnAddLayer = new JButton("Add Layer", IconBank.getIcon(
				JHVIcon.ADD_NEW, 16, 16));
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
				Object[][] data = new Object[Layers.getLayerCount()][4];
				int count = 0;
				for (LayerInterface layer : Layers.getLayers()) {
					data[count][0] = layer.isVisible();
					try {
						data[count][1] = layer.getName();
						data[count][2] = layer.getTime() == null ? null : layer
								.getTime();
					} catch (JHVException.MetaDataException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					data[count][3] = IconBank.getIcon(JHVIcon.CANCEL_NEW, 16,
							16);
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
				setFixedWidth(20, 0);
				setFixedWidth(16, 3);
				// table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				table.setShowGrid(false);
				table.setIntercellSpacing(new Dimension(0, 0));

				if (Layers.getLayerCount() > 0) {
					System.out.println(Layers.getActiveLayerNumber());
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
				super.getTableCellRendererComponent(table, value, isSelected,
						hasFocus, row, column);
				break;
			case 2:
				LocalDateTime localDateTime = (LocalDateTime) value;
				String date = localDateTime != null ? localDateTime
						.format(JHVGlobals.DATE_TIME_FORMATTER) : "";
				super.getTableCellRendererComponent(table, date, isSelected,
						hasFocus, row, column);
				break;
			case 3:
				JLabel label = (JLabel) super.getTableCellRendererComponent(
						table, null, isSelected, hasFocus, row, column);
				label.setIcon((ImageIcon) value);
				label.setPreferredSize(new Dimension(20, 20));
				return label;
			default:
				break;
			}
			return this;
		}

	}

	@Override
	public void newlayerAdded() {
		this.updateData();
	}

	@Override
	public void newlayerRemoved(int idx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		btnDownloadLayer.setEnabled(layer.isDownloadable());
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		updateData();
	}

	@Override
	public void dateTimesChanged(int framecount) {
		updateData();
	}
}
