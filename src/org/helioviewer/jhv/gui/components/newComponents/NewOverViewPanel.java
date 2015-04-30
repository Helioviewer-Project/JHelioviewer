package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class NewOverViewPanel extends CompenentView{

	private JPanel contentPanel;
	
	public NewOverViewPanel() {
    	super();
    	this.contentPanel = new JPanel();
		this.canvas.setPreferredSize(new Dimension(200, 200));
		this.canvas.setMinimumSize(new Dimension(200, 200));
    	this.contentPanel.add(canvas);
    	this.contentPanel.setPreferredSize(new Dimension(200, 200));
    	this.contentPanel.setMinimumSize(new Dimension(200, 200));
	}
	
	public Component getContentPane(){
		return this.contentPanel;
	}
}
