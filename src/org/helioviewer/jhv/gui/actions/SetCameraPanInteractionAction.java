package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.MainFrame;

public class SetCameraPanInteractionAction extends AbstractAction
{
	public SetCameraPanInteractionAction()
	{
		super("Pan");
	}
	
	@Override
	public void actionPerformed(@Nullable ActionEvent e)
	{
		MainFrame.SINGLETON.MAIN_PANEL.activatePanInteraction();
		MainFrame.SINGLETON.OVERVIEW_PANEL.activatePanInteraction();
	}
}
