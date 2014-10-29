package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraPanAnimation;

/**
 * Action to center the active layer.
 * 
 * @author Markus Langenberg
 */
public class GL3DCenterImageAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public GL3DCenterImageAction() {
        super("Center Image");
        putValue(SHORT_DESCRIPTION, "Center the image");
        putValue(MNEMONIC_KEY, KeyEvent.VK_C);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
    	GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();
        if (camera != null) {
            camera.addCameraAnimation(new GL3DCameraPanAnimation(camera.getTranslation().copy().negate()));
        }

    }

}
