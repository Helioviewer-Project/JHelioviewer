package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.gui.states.GuiState3DWCS;
import org.helioviewer.jhv.opengl.GLInfo;

public class View3DAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public View3DAction() {
        this.setEnabled(GLInfo.glIsEnabled() && GLInfo.glIsUsable());
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
      GL3DState.get().set3DState();
      GL3DCameraSelectorModel.getInstance().set3DMode();
      GL3DCameraSelectorModel.getInstance().getSelectedItem().setCurrentInteraction(GL3DCameraSelectorModel.getInstance().getSelectedItem().getRotateInteraction());
      new GuiState3DWCS().getTopToolBar().set3DMode();
    }
}
