package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.MainFrame;

public class SetCameraZoomBoxInteractionAction  extends AbstractAction
{
	public SetCameraZoomBoxInteractionAction()
	{
		super("Zoom Box");
	}
	
	
	@Override
	public void actionPerformed(@Nullable ActionEvent e)
	{
		MainFrame.MAIN_PANEL.activateZoomBoxInteraction();
		MainFrame.OVERVIEW_PANEL.activateZoomBoxInteraction();
	}
}
