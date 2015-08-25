package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.Layers;

public class SetCoronaVisibilityAction extends AbstractAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4120289247841074304L;

	public SetCoronaVisibilityAction() {
		super("Corona");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Layers.toggleCoronaVisibility();
		MainFrame.MAIN_PANEL.repaintMain(20);
	}
}
