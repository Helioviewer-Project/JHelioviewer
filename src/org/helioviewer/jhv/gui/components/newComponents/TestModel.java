package org.helioviewer.jhv.gui.components.newComponents;

import javax.swing.table.DefaultTableModel;

public class TestModel extends DefaultTableModel {

	public TestModel(Object[][] data, String[] columnNames){
		super(data, columnNames);
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
	
	
}
