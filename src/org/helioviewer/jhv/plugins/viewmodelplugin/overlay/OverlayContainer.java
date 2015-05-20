package org.helioviewer.jhv.plugins.viewmodelplugin.overlay;

import java.net.URI;

import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Container;

/**
 * The basic class which manages the interface between JHV and the contained
 * overlay.
 * <p>
 * It handles the installation process of a contained overlay and manages its
 * current status.
 * 
 * @author Stephan Pagel
 */
public abstract class OverlayContainer implements Container {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private boolean active;
    private int position;
    private URI pluginLocation;

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * Method will be called from JHV to add a overlay to a {@link OverlayView}
     * of the view chain and add the visual part of the overlay to the GUI.
     * 
     * @param overlayView
     *            OverlayView where to add the contained overlay.
     * @param controlList
     *            List which manages the locations for the visual GUI elements
     *            of the overlays.
     */
    /*public final void installOverlay(OverlayView overlayView, OverlayControlComponentManager controlList) {
        installOverlayImpl(overlayView, controlList);
        saveOverlaySettings();
    }*/

    /**
     * This method installs the corresponding overlay and adds the visual
     * overlay control to the GUI.
     * 
     * @param overlayView
     *            OverlayView where to add the contained overlay.
     * @param controlList
     *            List which manages the locations for the visual GUI elements
     *            of the overlays.
     */
    //protected abstract void installOverlayImpl(OverlayView overlayView, OverlayControlComponentManager controlList);

    /**
     * {@inheritDoc}
     */
    public boolean isActive() {
        return active;
    }

    /**
     * {@inheritDoc}
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * {@inheritDoc}
     */
    public void changeSettings() {
        saveOverlaySettings();
    }

    /**
     * {@inheritDoc}
     */

    public String toString() {
        return getName();
    }

    /**
     * Returns the current order position of the overlay. The position is
     * related to the position of the {@link OverlayView}s among each other in
     * the viewchain. The position 0 is the closest position to the
     * {@link org.helioviewer.jhv.viewmodel.view.LayeredView}, the position n is the
     * closest one to the {@link org.helioviewer.jhv.viewmodel.view.LayeredView}.
     * 
     * @return Position of the filter among each other.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the position of the filter among each other.
     * 
     * @param position
     *            New position of the filter among all other filters.
     * @see #getPosition()
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Sets the location of the corresponding plug-in.
     * 
     * @param pluginLocation
     *            Location of corresponding plug-in.
     */
    public final void setPluginLocation(URI pluginLocation) {
        this.pluginLocation = pluginLocation;
    }

    /**
     * Adds or updates the current status to the settings.
     * <p>
     * By calling this method the settings file will not be rewritten! This will
     * be done by the
     * {@link org.helioviewer.jhv.plugins.viewmodelplugin.controller.PluginManager}.
     */
    private void saveOverlaySettings() {
        //PluginSettings.getSingeltonInstance().overlaySettingsToXML(pluginLocation, this);
    }
}
