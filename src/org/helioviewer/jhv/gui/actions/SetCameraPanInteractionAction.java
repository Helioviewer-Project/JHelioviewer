package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.MainFrame;

public class SetCameraPanInteractionAction extends AbstractAction
{
	public SetCameraPanInteractionAction()
	{
		super("Pan");
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		MainFrame.MAIN_PANEL.activatePanInteraction();
		MainFrame.OVERVIEW_PANEL.activatePanInteraction();
	}
}
