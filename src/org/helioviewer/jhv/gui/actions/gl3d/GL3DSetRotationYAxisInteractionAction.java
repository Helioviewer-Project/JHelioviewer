package org.helioviewer.jhv.gui.actions.gl3d;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JToggleButton;

import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DInteraction;

/**
 * Sets the current {@link GL3DInteraction} of the current {@link GL3DCamera} to
 * Rotation.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSetRotationYAxisInteractionAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DSetRotationYAxisInteractionAction() {
        super("Lock Y-axis");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
    	GL3DCameraSelectorModel.getInstance().getSelectedItem().getRotateInteraction().setYAxisBlocked(((JToggleButton)e.getSource()).isSelected());
    }

}
