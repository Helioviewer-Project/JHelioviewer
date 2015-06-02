package org.helioviewer.jhv.plugins.plugin;

import javax.swing.JPanel;

public abstract class Plugin {
	protected JPanel pluginPanel;
	protected PluginRenderer pluginRenderer;
	
	abstract public String getAboutLicenseText();
	abstract public String getName();
 
	public JPanel getPanel(){
		return pluginPanel;
	}
	public PluginRenderer getRenderer(){
		return pluginRenderer;
	}
}
