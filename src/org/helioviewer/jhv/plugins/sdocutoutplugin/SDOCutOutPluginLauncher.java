package org.helioviewer.jhv.plugins.sdocutoutplugin;

import org.helioviewer.base.JavaCompatibility;
import org.helioviewer.jhv.JavaHelioViewer;
import org.helioviewer.viewmodelplugin.controller.PluginManager;

/**
 * Class for testing the external plugin
 * 
 * @author Andre Dau
 * 
 */
public class SDOCutOutPluginLauncher {

    /**
     * Used for testing the plugin
     * 
     * @see org.helioviewer.jhv.plugins.sdocutoutplugin.SDOCutOutPlugin3D#main(String[])
     * @param args
     */
    public static void main(String[] args) {
        String args2[] = JavaCompatibility.copyArray(args, args.length+2);

        args2[args2.length-2] = "--deactivate-plugin";
        args2[args2.length-1] = "SDOCutOutPlugin.jar";
        
        JavaHelioViewer.main(args2);
        SDOCutOutPlugin3D sdoCutOutPlugin = new SDOCutOutPlugin3D();
        PluginManager.getSingeltonInstance().addPlugin(SDOCutOutPluginLauncher.class.getClassLoader(), sdoCutOutPlugin, sdoCutOutPlugin.getLocation());
    }
}
