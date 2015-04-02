package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.Color;
import java.awt.Component;
import java.time.LocalDateTime;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class CellRenderer extends DefaultTableCellRenderer{
	
	public CellRenderer() {
        setHorizontalAlignment(CENTER);
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);
		super.setBackground(Color.WHITE);
		if ((int) value > 15 && row < 2){
			super.setForeground(new Color(195, 195, 195));
		}
		else if ((int) value < 15 && row > 3){
			super.setForeground(new Color(195, 195, 195));
		}
		else {
			super.setForeground(Color.BLACK);
		}
		return this;
	}
	
	
	
}
