package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

public class View2DAction extends AbstractAction
{

    private static final long serialVersionUID=1L;

    /**
     * Default constructor.
     */
    public View2DAction()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e)
    {
        Settings.setProperty("startup.cameramode", "2D");
        
        GL3DState.get().set2DState();
        GuiState3DWCS.mainComponentView.getAdapter(GL3DSceneGraphView.class).markLayersAsChanged();
        GL3DCameraSelectorModel.getInstance().set2DMode();
        GL3DCameraSelectorModel.getInstance().getSelectedItem().setCurrentInteraction(GL3DCameraSelectorModel.getInstance().getSelectedItem().getPanInteraction());
        GuiState3DWCS.topToolBar.set2DMode();
    }
}
