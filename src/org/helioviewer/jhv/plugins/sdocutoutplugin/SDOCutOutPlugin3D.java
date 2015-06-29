package org.helioviewer.jhv.plugins.sdocutoutplugin;

import java.net.URI;
import java.net.URL;

import org.helioviewer.jhv.plugins.plugin.AbstractPlugin;
import org.helioviewer.jhv.plugins.plugin.UltimatePluginInterface;
import org.json.JSONObject;

public class SDOCutOutPlugin3D extends AbstractPlugin {

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
    	UltimatePluginInterface.addButtonToToolbar(new SDOCutOutToggleButton());
    }
    
    public URI getLocation() {
    	return this.pluginLocation;
    }
    
    public String getName() {
        return "SDOCutOut Plugin";
    }

    public String getAboutLicenseText() {
    	return null;
    }
	
	public static URL getResourceUrl(String name) {
		return SDOCutOutPlugin3D.class.getResource(name);
	}

	@Override
	public void loadStateFile(JSONObject jsonObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeStateFile(JSONObject jsonObject) {
	}
	
}
