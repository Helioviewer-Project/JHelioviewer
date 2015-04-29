package org.helioviewer.jhv.gui.actions.gl3d;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DInteraction;

/**
 * Sets the current {@link GL3DInteraction} of the current {@link GL3DCamera} to
 * Panning (Camera Translation).
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSetPanInteractionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public GL3DSetPanInteractionAction() {
        super("Pan");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
    	if (JHVGlobals.OLD_RENDER_MODE) GL3DCameraSelectorModel.getInstance().getSelectedItem().setCurrentInteraction(GL3DCameraSelectorModel.getInstance().getSelectedItem().getPanInteraction());
    }

}
