package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.BooleanKey;
import org.helioviewer.jhv.gui.MainFrame;

public class CameraMode
{
	public static MODE mode = MODE.MODE_3D;

	public enum MODE
	{
		MODE_2D, MODE_3D
	}

	public static void set3DMode()
	{
		//TODO: restore this setting on startup
		Settings.setBoolean(BooleanKey.STARTUP_3DCAMERA, true);
		mode = MODE.MODE_3D;
		MainFrame.SINGLETON.TOP_TOOL_BAR.set3DMode();
		MainFrame.SINGLETON.MAIN_PANEL.activateRotationInteraction();
		MainFrame.SINGLETON.MAIN_PANEL.repaint();
	}

	public static void set2DMode()
	{
		mode = MODE.MODE_2D;
		Settings.setBoolean(BooleanKey.STARTUP_3DCAMERA, false);
		MainFrame.SINGLETON.TOP_TOOL_BAR.set2DMode();
		MainFrame.SINGLETON.MAIN_PANEL.activatePanInteraction();
		MainFrame.SINGLETON.MAIN_PANEL.repaint();
	}
}
