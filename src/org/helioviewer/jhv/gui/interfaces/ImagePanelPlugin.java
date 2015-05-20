package org.helioviewer.jhv.gui.interfaces;

import org.helioviewer.jhv.gui.components.BasicImagePanel;

/**
 * Interface representing a plugin for an image panel.
 * 
 * <p>
 * A plugin gets access to the view chain and can read or even modify the view
 * chain.
 */
public interface ImagePanelPlugin {

    /**
     * Sets the image panel to which the plugin is attached.
     * 
     * That way the plugin can access the image panel.
     * 
     * <p>
     * This function is called during
     * {@link org.helioviewer.jhv.gui.components.BasicImagePanel#addPlugin(ImagePanelPlugin)}
     * , so usually the user does not have to take care of this.
     * 
     * @param newImagePanel
     *            Image panel to which the plugin is attached
     * @see #getImagePanel()
     */
    public void setImagePanel(BasicImagePanel newImagePanel);

    /**
     * Returns the image panel to which the plugin is attached.
     * 
     * @return Image panel to which the plugin is attached
     * @see #setImagePanel(BasicImagePanel)
     */
    public BasicImagePanel getImagePanel();

    /**
     * Callback function to be called when the controller is detached. This
     * function provides space for clean up work to do.
     */
    public void detach();
}
