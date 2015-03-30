package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.Color;
import java.awt.Component;
import java.time.LocalDateTime;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class HeaderRenderer extends DefaultTableCellRenderer{
	
	public HeaderRenderer() {
        setHorizontalAlignment(CENTER);
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
				row, column);
		super.setBackground(Color.WHITE);
		return this;
	}
	
	
	
}
