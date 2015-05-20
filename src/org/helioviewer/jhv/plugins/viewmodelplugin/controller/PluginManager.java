package org.helioviewer.jhv.plugins.viewmodelplugin.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayContainer;

/**
 * This class is responsible to manage all plug-ins for JHV. It loads available
 * plug-ins and provides methods to access the loaded plug-ins.
 * 
 * @author Stephan Pagel
 */
public class PluginManager {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private static PluginManager singeltonInstance = new PluginManager();

    private PluginSettings pluginSettings = PluginSettings.getSingeltonInstance();
    private Map<Plugin, PluginContainer> plugins = new HashMap<Plugin, PluginContainer>();
    private AbstractList<OverlayContainer> pluginOverlays = new LinkedList<OverlayContainer>();

    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    /**
     * The private constructor to support the singleton pattern.
     * */
    private PluginManager() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static PluginManager getSingeltonInstance() {
        return singeltonInstance;
    }

    /**
     * Loads the saved settings from the corresponding file.
     * 
     * @param settingsFilePath
     *            Path of the directory where the plug-in settings file is
     *            saved.
     */
    public void loadSettings(String settingsFilePath) {
        pluginSettings.loadPluginSettings(settingsFilePath);
    }

    /**
     * Returns a list with all loaded plug-ins.
     * 
     * @return a list with all loaded plug-ins.
     */
    public PluginContainer[] getAllPlugins() {
        return this.plugins.values().toArray(new PluginContainer[0]);
    }


    /**
     * Adds a container with a overlay to the list of all overlays.
     * 
     * @param container
     *            Overlay container to add to the list.
     */
    public void addOverlayContainer(OverlayContainer container) {
    	
        pluginOverlays.add(container);
    }

    /**
     * Removes a container with a overlay from the list of all overlays.
     * 
     * @param container
     *            Overlay container to remove from the list.
     */
    public void removeOverlayContainer(OverlayContainer container) {

        container.setActive(false);
        container.changeSettings();

        pluginOverlays.remove(container);
    }

    /**
     * Returns the number of all available overlays.
     * 
     * @return Number of available overlays.
     * */
    public int getNumberOfOverlays() {
        return pluginOverlays.size();
    }

    /**
     * Returns a list with all overlays which have the passed active status. If
     * the active status is true all activated overlays will be returned
     * otherwise all available and not activated overlays will be returned.
     * 
     * @param activated
     *            Indicates if all available (false) or all activated (true)
     *            overlays have to be returned.
     * @return list with all overlays which have the passed active status.
     */
    public AbstractList<OverlayContainer> getOverlayContainers(boolean activated) {

        AbstractList<OverlayContainer> result = new LinkedList<OverlayContainer>();

        for (OverlayContainer oc : pluginOverlays) {
            if (oc.isActive() == activated)
                result.add(oc);
        }

        return result;
    }

    /**
     * Adds an internal plug-in to the list of all loaded plug-ins. Internal
     * plug-ins are installed and activated by default.
     * 
     * @param classLoader
     *            The class loader used to load the plugin classes
     * @param plugin
     *            internal plug-in to add to the list of all loaded plug-ins.
     */
    public void addInternalPlugin(ClassLoader classLoader, Plugin plugin) {
        try {
            PluginContainer pluginContainer = new PluginContainer(classLoader, plugin, new URI("internal"), true);
            plugins.put(plugin, pluginContainer);
            PluginSettings.getSingeltonInstance().pluginSettingsToXML(pluginContainer);
            plugin.installPlugin();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a plug-in to the list of all loaded plug-ins. By default a plug-in
     * is not activated. If there is a plug-in entry in the plug-in settings
     * file the status of the plug-in will be set to this value.
     * 
     * @param classLoader
     *            The class loader used to load the plugin classes
     * @param plugin
     *            Plug-in to add to the list.
     * @param pluginLocation
     *            Location of the corresponding file of the plug-in.
     */
    public void addPlugin(ClassLoader classLoader, Plugin plugin, URI pluginLocation) {
        PluginContainer pluginContainer = new PluginContainer(classLoader, plugin, pluginLocation, pluginSettings.isPluginActivated(pluginLocation));
        plugins.put(plugin, pluginContainer);
        if (pluginContainer.isActive()) {
            plugin.installPlugin();
        }
    }

    /**
     * Removes a container with a plug-in from the list of all plug-ins.
     * 
     * @param container
     *            Plug-in container to remove from the list.
     */
    public void removePluginContainer(PluginContainer container) {
        plugins.remove(container.getPlugin());
        pluginSettings.removePluginFromXML(container);
    }
}
