package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.layers.Layers;

public class ToggleCoronaVisibilityAction extends AbstractAction
{
	public ToggleCoronaVisibilityAction()
	{
		super("Corona");
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Layers.toggleCoronaVisibility();
	}
}
