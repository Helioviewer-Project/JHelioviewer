package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.MainFrame;

public class SetCameraZoomBoxInteractionAction  extends AbstractAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 953091770424739640L;


	public SetCameraZoomBoxInteractionAction() {
		super("Zoom Box");
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		MainFrame.MAIN_PANEL.setZoomBoxInteraction();
		MainFrame.OVERVIEW_PANEL.setZoomBoxInteraction();
	}
}
