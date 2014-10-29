package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.gui.GuiState3DWCS;

public class View2DAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public View2DAction() {
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
      GL3DState.get().set2DState();
      GL3DCameraSelectorModel.getInstance().set2DMode();
        GL3DCameraSelectorModel.getInstance().getSelectedItem().setCurrentInteraction(GL3DCameraSelectorModel.getInstance().getSelectedItem().getPanInteraction());
        GuiState3DWCS.topToolBar.set2DMode();
      //StateController.getInstance().set2DState();
    }
}
