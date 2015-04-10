package org.helioviewer.jhv.gui.actions.gl3d;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JToggleButton;

import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;

/**
 * Action that enables the Solar Rotation Tracking, which ultimately changes the
 * current {@link GL3DCamera} to the
 * {@link GL3DSolarRotationTrackingTrackballCamera}
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DToggleSolarRotationAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DToggleSolarRotationAction() {
        super("Track");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
    	GL3DCameraSelectorModel.getInstance().getCurrentCamera().setTrack(((JToggleButton)e.getSource()).isSelected());
    }

}
