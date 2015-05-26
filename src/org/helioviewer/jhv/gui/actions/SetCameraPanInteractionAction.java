package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.components.newComponents.MainFrame;

public class SetCameraPanInteractionAction extends AbstractAction{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1199107637545677948L;
	
	public SetCameraPanInteractionAction() {
		super("Pan");
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		MainFrame.MAIN_PANEL.setPanInteraction();
		MainFrame.OVERVIEW_PANEL.setPanInteraction();
	}

}
