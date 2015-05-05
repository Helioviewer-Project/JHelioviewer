package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.NewLayerListener;

public class NewLayerPanel extends JPanel implements NewLayerListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6800340702841902680L;
	
	private final AddLayerPanel addLayerPanel = new AddLayerPanel();
	private JTable table;
	private LayerTableModel tableModel;
	Object columnNames[] = { "Column One", "Column Two", "Column Three", "Column Four"};
	Object rowData[][] = { { "Row1-Column1", "Row1-Column2", "Row1-Column3"},
            { "Row2-Column1", "Row2-Column2", "Row2-Column3"} };
	
	public NewLayerPanel(){
		initGUI();
		updateData();
		this.setPreferredSize(new Dimension(200, 200));
		GuiState3DWCS.layers.addNewLayerListener(this);
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
        table.getColumnModel().getColumn(0).setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(1).setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(2).setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(3).setCellRenderer(new ImageIconCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(0).setWidth(35);
		table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(3).setResizable(false);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.getSelectedRow();
				int column = table.getSelectedColumn();
				if (column == 3){
					GuiState3DWCS.layers.removeLayer(row);
					updateData();
				}
				if (row != GuiState3DWCS.layers.getActiveLayerNumber()){
					GuiState3DWCS.layers.setActiveLayer(row);
				}
				if (column == 0){
					GuiState3DWCS.layers.getActiveLayer().setVisible((boolean) table.getModel().getValueAt(row, column));
				}

			}
		});
		scrollPane.setViewportView(table);

		/*for (int i = 0; i <  table.getColumnCount(); i++){
			table.getColumnModel().getColumn(i)
			.setCellRenderer(new LayerCellRenderer());
			}*/
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

		JButton btnNewButton_2 = new JButton(IconBank.getIcon(JHVIcon.INFO_NEW, 16, 16));
		btnNewButton_2.setToolTipText("Show the Metainformation of the currently selected Layer");
		GridBagConstraints gbc_btnNewButton_2 = new GridBagConstraints();
		gbc_btnNewButton_2.insets = new Insets(0, 0, 0, 5);
		gbc_btnNewButton_2.gridx = 9;
		gbc_btnNewButton_2.gridy = 0;
		panel.add(btnNewButton_2, gbc_btnNewButton_2);

		JButton btnNewButton_1 = new JButton(IconBank.getIcon(JHVIcon.DOWNLOAD_NEW, 16, 16));
		btnNewButton_1.setToolTipText("Download the currently selected Layer");
		GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
		gbc_btnNewButton_1.insets = new Insets(0, 0, 0, 5);
		gbc_btnNewButton_1.gridx = 10;
		gbc_btnNewButton_1.gridy = 0;
		panel.add(btnNewButton_1, gbc_btnNewButton_1);

		JButton btnNewButton = new JButton("Add Layer", IconBank.getIcon(JHVIcon.ADD_NEW, 16, 16));
		btnNewButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				addLayerPanel.setVisible(true);
			}
		});
		btnNewButton.setToolTipText("Add a new Layer");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.anchor = GridBagConstraints.EAST;
		gbc_btnNewButton.gridx = 11;
		gbc_btnNewButton.gridy = 0;
		panel.add(btnNewButton, gbc_btnNewButton);

	}

	
	private void updateData(){
		Object[][] data = new Object[GuiState3DWCS.layers.getLayerCount()][4];
		int count = 0;
		for (LayerInterface layer : GuiState3DWCS.layers.getLayers()){
			data[count][0] = layer.isVisible();
			data[count][1] = layer.getName();
			data[count][2] = layer.getTime() == null ? null : layer.getTime();
			data[count++][3] = IconBank.getIcon(JHVIcon.CANCEL_NEW, 16, 16);
		}
		tableModel.setDataVector(data, columnNames);
		table.setModel(tableModel);
		
        //table.getColumnModel().getColumn(0).setCellRenderer(new ImageIconCellRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new ImageIconCellRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new ImageIconCellRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new ImageIconCellRenderer());
		this.setFixedWidth(20, 0);
		this.setFixedWidth(16, 3);
        //table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        
		if (GuiState3DWCS.layers.getLayerCount() > 0){
			System.out.println(GuiState3DWCS.layers.getActiveLayerNumber());
			table.setRowSelectionInterval(GuiState3DWCS.layers.getActiveLayerNumber(), GuiState3DWCS.layers.getActiveLayerNumber());
		}


	}
	
	private void setFixedWidth(int width, int column){
		table.getColumnModel().getColumn(column).setMinWidth(width);
        table.getColumnModel().getColumn(column).setMaxWidth(width);
        table.getColumnModel().getColumn(column).setPreferredWidth(width);
	}
	
	private class LayerTableModel extends DefaultTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5224476911114851064L;

		public LayerTableModel(Object[][] data, String[] columnNames) {
			super(data, columnNames);
		}
		
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0){
				return Boolean.class;
			}
			
			return super.getColumnClass(columnIndex);
		}
		

		@Override
		public boolean isCellEditable(int row, int column) {
			if (column == 0){
				return true;
			}
			return false;
		}
	}
	
	
	private class ImageIconCellRenderer extends DefaultTableCellRenderer{

		/**
		 * 
		 */
		private static final long serialVersionUID = -2552431402411803683L;

		public ImageIconCellRenderer() {
			//setOpaque(true);
			setHorizontalAlignment(CENTER);
			//setBackground(Color.WHITE);			
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, final int row,
				int column) {
			switch (column) {
			case 0:				
				super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
				break;
			case 1:		
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				break;
			case 2:
				LocalDateTime localDateTime = (LocalDateTime) value;
				String date = localDateTime != null ? localDateTime.format(JHVGlobals.DATE_TIME_FORMATTER) : "";
				super.getTableCellRendererComponent(table, date, isSelected, hasFocus, row, column);
				break;
			case 3:				
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
				label.setIcon((ImageIcon)value);
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
	public void newtimestampChanged() {
		this.updateData();
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		// TODO Auto-generated method stub
		
	}
}
