package org.helioviewer.jhv.plugins.hekplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.helioviewer.jhv.base.math.Interval;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;
import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayPlugin;

/**
 * @author Malte Nuhn
 * */
public class HEKPlugin3D extends OverlayPlugin implements Plugin {
    /**
     * Reference to the eventPlugin
     */
    private HEKPluginContainer eventPlugin;

    /**
     * Default constructor.
     */
    public HEKPlugin3D() {
        try {
            this.pluginLocation = new URI(HEKSettings.PLUGIN_LOCATION);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        eventPlugin = new HEKPluginContainer();
        addOverlayContainer(eventPlugin);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overrides the default method because the internal provided filters are
     * activated by default.
     */
    public void installPlugin() {
        for (OverlayContainer overlay : overlayContainerList) {
            //overlay.setActive(PluginSettings.getSingeltonInstance().isOverlayInPluginActivated(pluginLocation, overlay.getOverlayClass(), true));
            //overlay.setPosition(PluginSettings.getSingeltonInstance().getOverlayPosition(pluginLocation, overlay.getOverlayClass()));
            //PluginManager.getSingeltonInstance().addOverlayContainer(overlay);
            //ImageViewerGui.getSingletonInstance().getMainImagePanel().addPlugin(new ImagePanelEventPopupController());
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "HEK Overlay Plugin";
    }

    /**
     * Wrapper around HEKEventPlugins functions.
     * 
     * @see org.helioviewer.jhv.plugins.overlay.hek.plugin.HEKEventPlugin
     * @see org.helioviewer.jhv.plugins.overlay.hek.plugin.HEKEventPlugin#setCurInterval
     * @param newInterval
     */
    public void setCurInterval(Interval<Date> newInterval) {
        eventPlugin.setCurInterval(newInterval);
    }

    public void setEnabled(boolean b) {
        eventPlugin.setEnabled(b);
    }

    /**
     * {@inheritDoc}
     * 
     * null because this is an internal plugin
     */
    public String getAboutLicenseText() {
        String description = "";
        description += "<p>" + "This software uses the <a href=\"http://www.json.org/java/\">JSON in Java</a> Library, licensed under a <a href=\"http://www.json.org/license.html\">custom License</a>.";

        return description;
    }

    public static URL getResourceUrl(String name) {
        return HEKPlugin3D.class.getResource(name);
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public void setState(String state) {
        // TODO Implement setState for HEKPlugin
    }

    /**
     * {@inheritDoc} In this case, does nothing.
     */
    public String getState() {
        // TODO Implement getState for HEKPlugin
        return "";
    }
}