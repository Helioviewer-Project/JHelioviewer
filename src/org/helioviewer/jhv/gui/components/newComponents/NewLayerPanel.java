package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.DummyLayer;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;

public class NewLayerPanel extends JPanel {

	private JTable table;
	private LayerTableModel tableModel;
	private Layers layers;
	Object columnNames[] = { "Column One", "Column Two", "Column Three", "Column Four"};
	Object rowData[][] = { { "Row1-Column1", "Row1-Column2", "Row1-Column3"},
            { "Row2-Column1", "Row2-Column2", "Row2-Column3"} };
	
	public NewLayerPanel(Layers layers) {
		this.layers = layers;
		initGUI();
		updateData();
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
		table.getColumnModel().getColumn(3).setCellRenderer(new ImageIconCellRenderer());
		table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(3).setResizable(false);
        table.setShowGrid(false);
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
		btnNewButton.setToolTipText("Add a new Layer");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.anchor = GridBagConstraints.EAST;
		gbc_btnNewButton.gridx = 11;
		gbc_btnNewButton.gridy = 0;
		panel.add(btnNewButton, gbc_btnNewButton);

	}

	
	private void updateData(){
		Object[][] data = new Object[layers.getLayerCount()][4];
		int count = 0;
		for (LayerInterface layer : layers.getLayers()){
			data[count][2] = "a";
			data[count++][3] = IconBank.getIcon(JHVIcon.CANCEL_NEW, 16, 16);
		}

		
		tableModel.setDataVector(data, columnNames);
		/*for (int i = 0; i <  table.getColumnCount(); i++){
			table.getColumnModel().getColumn(i)
			.setCellRenderer(new LayerCellRenderer());
		}
		*/
		table.setModel(tableModel);
		table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(3).setResizable(false);
		table.getColumnModel().getColumn(3).setCellRenderer(new ImageIconCellRenderer());
	}
	
	public static void main(String[] args) {
		Layers layers = new Layers();
		layers.addLayer(new DummyLayer());
		layers.addLayer(new DummyLayer());
		NewLayerPanel newLayerPanel = new NewLayerPanel(layers);
		JFrame frame = new JFrame();
		frame.getContentPane().add(newLayerPanel);
		frame.pack();
		frame.setVisible(true);
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
	
	
	private class ImageIconCellRenderer extends JLabel implements TableCellRenderer{

		public ImageIconCellRenderer() {
			setOpaque(true);
			setBackground(Color.WHITE);			
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, final int row,
				int column) {
			this.setIcon((ImageIcon)value);
			return this;
		}
		
	}
}
