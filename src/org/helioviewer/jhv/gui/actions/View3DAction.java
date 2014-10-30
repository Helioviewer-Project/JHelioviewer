package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.GuiState3DWCS;

public class View3DAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public View3DAction() {
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
      GL3DState.get().set3DState();
      GuiState3DWCS.mainComponentView.getAdapter(GL3DSceneGraphView.class).markLayersAsChanged();
      GL3DCameraSelectorModel.getInstance().set3DMode();
      GL3DCameraSelectorModel.getInstance().getSelectedItem().setCurrentInteraction(GL3DCameraSelectorModel.getInstance().getSelectedItem().getRotateInteraction());
      GuiState3DWCS.topToolBar.set3DMode();
    }
}
