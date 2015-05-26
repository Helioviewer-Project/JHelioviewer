package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;


public class CameraMode {

	public static MODE mode = MODE.MODE_3D;
	public enum MODE {
		MODE_2D, MODE_3D;
	}
	
	public static void set3DMode(){
        Settings.setProperty("startup.cameramode","3D");
        mode = MODE.MODE_3D;
        MainFrame.TOP_TOOL_BAR.set3DMode();
        MainFrame.MAIN_PANEL.setRotationInteraction();
        MainFrame.OVERVIEW_PANEL.setRotationInteraction();
        MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
	}

	public static void set2DMode(){
        mode = MODE.MODE_2D;
        Settings.setProperty("startup.cameramode", "2D");        
        MainFrame.TOP_TOOL_BAR.set2DMode();
        MainFrame.MAIN_PANEL.setPanInteraction();
        MainFrame.OVERVIEW_PANEL.setPanInteraction();
        MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
	}
	
	public static int getCameraMode(){
		System.out.println("mode : " + (mode == MODE.MODE_3D? 1:0));
		return mode == MODE.MODE_3D? 1:0;
	}

}
