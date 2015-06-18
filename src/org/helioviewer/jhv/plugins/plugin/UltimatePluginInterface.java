package org.helioviewer.jhv.plugins.plugin;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.plugins.hekplugin.HEKPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.sdocutoutplugin.SDOCutOutPlugin3D;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

import com.jogamp.opengl.GL2;

public class UltimatePluginInterface implements TimeLineListener, MouseListener, MouseMotionListener{
	
	private ArrayList<NewPlugin> plugins;
	
	public static final UltimatePluginInterface SIGLETON = new UltimatePluginInterface();
	
	private UltimatePluginInterface() {
		plugins = new ArrayList<NewPlugin>();
		TimeLine.SINGLETON.addListener(this);
		plugins.add(new SDOCutOutPlugin3D());
		plugins.add(new PfssPlugin());
		plugins.add(new HEKPlugin());
		MainFrame.MAIN_PANEL.addMouseListener(this);
		MainFrame.MAIN_PANEL.addMouseMotionListener(this);
	}
	
	public void addPlugin(NewPlugin plugin){
		plugins.add(plugin);
	}
	
	public void removePlugin(NewPlugin plugin){
		plugins.remove(plugin);
	}
	
	public static void addButtonToToolbar(AbstractButton button){
		MainFrame.TOP_TOOL_BAR.addButton(button);
	}
	
	public static void addPanelToLeftControllPanel(String title, JPanel panel, boolean startExpanded){
		MainFrame.LEFT_PANE.add(title, panel, startExpanded);
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		for (NewPlugin plugin : plugins){
			plugin.timeStampChanged(current, last);
		}
	}

	@Override
	public void dateTimesChanged(int framecount) {
		for (NewPlugin plugin : plugins){
			plugin.dateTimesChanged(framecount);
		}		
	}
	
	public void renderPlugin(GL2 gl){
		for (NewPlugin plugin : plugins){
			plugin.render(gl);
		}
	}
	
	public String[] getAboutLicenseTexts(){
		String[] licenseTexts = new String[plugins.size()];
		int index = 0;
		for (NewPlugin plugin : plugins){
			String licenseText = plugin.getAboutLicenseText();
			licenseTexts[index++] = licenseText != null ? licenseText : "";
		}
		return licenseTexts;
	}

	public LocalDateTime getCurrentDateTime() {
		return TimeLine.SINGLETON.getCurrentDateTime();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(e.getX(), e.getY(), MainFrame.MAIN_PANEL).getHitpoint();
		for (NewPlugin plugin : plugins){
			plugin.mouseDragged(e, hitpoint);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(e.getX(), e.getY(), MainFrame.MAIN_PANEL).getHitpoint();
		for (NewPlugin plugin : plugins){
			plugin.mouseMoved(e, hitpoint);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(e.getX(), e.getY(), MainFrame.MAIN_PANEL).getHitpoint();
		for (NewPlugin plugin : plugins){
			plugin.mouseClicked(e, hitpoint);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(e.getX(), e.getY(), MainFrame.MAIN_PANEL).getHitpoint();
		for (NewPlugin plugin : plugins){
			plugin.mousePressed(e, hitpoint);
		}	
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(e.getX(), e.getY(), MainFrame.MAIN_PANEL).getHitpoint();
		for (NewPlugin plugin : plugins){
			plugin.mouseReleased(e, hitpoint);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(e.getX(), e.getY(), MainFrame.MAIN_PANEL).getHitpoint();
		for (NewPlugin plugin : plugins){
			plugin.mouseEntered(e, hitpoint);
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(e.getX(), e.getY(), MainFrame.MAIN_PANEL).getHitpoint();
		for (NewPlugin plugin : plugins){
			plugin.mouseExited(e, hitpoint);
		}
	}
	
	public static void setCursor(Cursor cursor){
		MainFrame.MAIN_PANEL.setCursor(cursor);
	}

	public static Cursor getCursor(){
		return MainFrame.MAIN_PANEL.getCursor();
	}

	public static Point mainPanelGetLocationOnScreen(){
		return MainFrame.MAIN_PANEL.getLocationOnScreen();
	}
	
	public static Dimension mainPanelGetSize(){
		return MainFrame.MAIN_PANEL.getSize();
	}

	public static LocalDateTime getStartDateTime() {
		return TimeLine.SINGLETON.getFirstDateTime();
	}
	
	public static LocalDateTime getEndDateTime() {
		return TimeLine.SINGLETON.getLastDateTime();
	}

	public static Dimension getMainPanelSize(){
		return MainFrame.MAIN_PANEL.getSize();
	}
	
	public static double getViewPortSize(){
		return MainFrame.MAIN_PANEL.getTranslation().z * Math.tan(MainPanel.FOV / 2) * 2;
	}

	public static void repaintMainPanel() {
		MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
	}
	
	public static HTTPRequest generateAndStartHTPPRequest(String uri, PRIORITY priority){
		HTTPRequest httpRequest = new HTTPRequest(uri, priority);
		UltimateDownloadManager.addRequest(httpRequest);
		return httpRequest;
	}
	
	public static HTTPRequest generateAndStartHTPPRequest(String uri, int port, PRIORITY priority){
		HTTPRequest httpRequest = new HTTPRequest(uri, port, priority);
		UltimateDownloadManager.addRequest(httpRequest);
		return httpRequest;
	}
}
