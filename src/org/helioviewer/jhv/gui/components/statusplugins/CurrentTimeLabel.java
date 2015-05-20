package org.helioviewer.jhv.gui.components.statusplugins;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;


public class CurrentTimeLabel extends JLabel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9216168376764918306L;
	
	private String empty = " - ";
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
	
	public CurrentTimeLabel() {
        this.setBorder(BorderFactory.createEtchedBorder());

		this.setText(empty);
	}
}
