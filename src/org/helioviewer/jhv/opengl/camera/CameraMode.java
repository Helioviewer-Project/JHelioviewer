package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.MainFrame;


public class CameraMode {

	public static MODE mode = MODE.MODE_3D;
	public enum MODE {
		MODE_2D, MODE_3D;
	}
	
	public static void set3DMode(){
        Settings.setProperty("startup.cameramode","3D");
        mode = MODE.MODE_3D;
        MainFrame.TOP_TOOL_BAR.set3DMode();
        MainFrame.MAIN_PANEL.activateRotationInteraction();
        //MainFrame.OVERVIEW_PANEL.setRotationInteraction();
        MainFrame.MAIN_PANEL.repaint();
	}

	public static void set2DMode(){
        mode = MODE.MODE_2D;
        Settings.setProperty("startup.cameramode", "2D");        
        MainFrame.TOP_TOOL_BAR.set2DMode();
        MainFrame.MAIN_PANEL.activatePanInteraction();
        //MainFrame.OVERVIEW_PANEL.setPanInteraction();
        MainFrame.MAIN_PANEL.repaint();
	}
	
	public static int getCameraMode(){
		return mode == MODE.MODE_3D? 1:0;
	}

}
