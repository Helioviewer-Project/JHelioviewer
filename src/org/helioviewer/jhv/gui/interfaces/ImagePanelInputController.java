package org.helioviewer.jhv.gui.interfaces;

import java.awt.event.MouseWheelListener;

import javax.swing.event.MouseInputListener;

import org.helioviewer.jhv.base.math.Vector2i;

/**
 * Interface representing an input controller for an image panel.
 * 
 * <p>
 * There can only be one input controller attached to an image panel at a time.
 * It receives all mouse event from the panel.
 * 
 * <p>
 * For further informations, see {@link ImagePanelPlugin}
 * {@link org.helioviewer.jhv.gui.components.BasicImagePanel}.
 * 
 */
public interface ImagePanelInputController extends MouseInputListener, MouseWheelListener, ImagePanelPlugin {
    /**
     * Get the last mouse position
     * 
     * @return mouse position or null if the mouse is not within the panel
     */
    public Vector2i getMousePosition();

}
