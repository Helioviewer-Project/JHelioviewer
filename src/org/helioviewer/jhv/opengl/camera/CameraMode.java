package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.gui.MainFrame;

public class CameraMode
{

	public static MODE mode = MODE.MODE_3D;

	public enum MODE
	{
		MODE_2D, MODE_3D;
	}

	public static void set3DMode()
	{
		Settings.setString("startup.cameramode", "3D");
		mode = MODE.MODE_3D;
		MainFrame.SINGLETON.TOP_TOOL_BAR.set3DMode();
		MainFrame.SINGLETON.MAIN_PANEL.activateRotationInteraction();
		// MainFrame.SINGLETON.OVERVIEW_PANEL.setRotationInteraction();
		MainFrame.SINGLETON.MAIN_PANEL.repaint();
	}

	public static void set2DMode()
	{
		mode = MODE.MODE_2D;
		Settings.setString("startup.cameramode", "2D");
		MainFrame.SINGLETON.TOP_TOOL_BAR.set2DMode();
		MainFrame.SINGLETON.MAIN_PANEL.activatePanInteraction();
		// MainFrame.SINGLETON.OVERVIEW_PANEL.setPanInteraction();
		MainFrame.SINGLETON.MAIN_PANEL.repaint();
	}

	public static int getCameraMode()
	{
		return mode == MODE.MODE_3D ? 1 : 0;
	}

}
