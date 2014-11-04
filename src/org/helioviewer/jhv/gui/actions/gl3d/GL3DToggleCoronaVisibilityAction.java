package org.helioviewer.jhv.gui.actions.gl3d;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * Action that enables the Solar Rotation Tracking, which ultimately changes the
 * current {@link GL3DCamera} to the
 * {@link GL3DSolarRotationTrackingTrackballCamera}
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DToggleCoronaVisibilityAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DToggleCoronaVisibilityAction() {
        super("Corona");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
        GL3DSceneGraphView sceneGraph = GuiState3DWCS.mainComponentView.getAdapter(GL3DSceneGraphView.class);
        if (sceneGraph != null) {
            sceneGraph.toggleCoronaVisibility();
        }
        GuiState3DWCS.mainComponentView.getComponent().repaint();
    }

}
