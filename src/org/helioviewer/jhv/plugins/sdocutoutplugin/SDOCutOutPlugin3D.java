package org.helioviewer.jhv.plugins.sdocutoutplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;

public class SDOCutOutPlugin3D implements Plugin {

	/**
     * Sets up the visual sub components and the visual part of the component
     * itself.
     **/
    public SDOCutOutToggleButton sdoCutOutToggleButton;
    
    private URI pluginLocation;
    
    
    /**
     * Default constructor
     */
    public SDOCutOutPlugin3D() {
    	try {
    		pluginLocation = new URI(SDOCutOutSettings.PLUGIN_LOCATION);
    	} catch (URISyntaxException e) {
    		e.printStackTrace();
    	}
    }
    
    public URI getLocation() {
    	return this.pluginLocation;
    }
	
	public void installPlugin() {
		if (sdoCutOutToggleButton == null)
			sdoCutOutToggleButton = new SDOCutOutToggleButton();
		
    }

    public void uninstallPlugin() {
    	if (sdoCutOutToggleButton != null)
    		sdoCutOutToggleButton.removeButton();
    }
    
    public String getName() {
        return "SDOCutOut Plugin";
    }

    public String getDescription() {
        return "Plugin for the SDO cut-out-service";
    }

    public String getAboutLicenseText() {
    	return null;
    }

    /**
     * Used for testing the plugin
     * 
     * @see org.helioviewer.jhv.plugins.sdocutoutplugin.SDOCutOutPluginLauncher#main(String[])
     * @param args
     */
    public static void main(String[] args) {
        //JavaHelioViewerLauncher.start(SDOCutOutPluginLauncher.class, args);
    }

	@Override
	public String getState() {
		return null;
	}

	@Override
	public void setState(String state) {
	}
	
	public static URL getResourceUrl(String name) {
		return SDOCutOutPlugin3D.class.getResource(name);
	}
	
}
