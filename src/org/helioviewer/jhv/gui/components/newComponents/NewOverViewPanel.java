package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.Dimension;
import javax.media.opengl.GL2;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.NewLayer;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class NewOverViewPanel extends CompenentView{

	public NewOverViewPanel(JPanel jPanel) {
    	super();
		this.canvas.setPreferredSize(new Dimension(200, 200));
		this.canvas.setMinimumSize(new Dimension(200, 200));
    	jPanel.add(canvas);
    	
	}
	
	@Override
	public void displayLayer(GL2 gl, NewLayer layer) {
		System.out.println("repaint");
		this.rotation = ((CompenentView)GuiState3DWCS.mainComponentView).getRotation();
		super.displayLayer(gl, layer);
	}
}
