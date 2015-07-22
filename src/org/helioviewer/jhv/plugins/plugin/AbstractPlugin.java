package org.helioviewer.jhv.plugins.plugin;

import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.math.Vector3d;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class AbstractPlugin {
	protected final String pluginName;
	protected boolean loadOnStartup = true;
	protected ArrayList<AbstractRequest> badRequests = new ArrayList<AbstractRequest>();
	
	public enum RENDER_MODE {
		MAIN_PANEL, OVERVIEW_PANEL, ALL_PANEL;
	};
	protected RENDER_MODE renderMode = RENDER_MODE.ALL_PANEL;
	
	public AbstractPlugin(String name) {
		pluginName = name;
	}
	
	public void timeStampChanged(LocalDateTime current, LocalDateTime last){
		
	}
	
	
	public void dateTimesChanged(int framecount) {
		// TODO Auto-generated method stub
		
	}
	
	public void render(GL2 gl){
		
	}
	
	public abstract String getAboutLicenseText();	
	
	
	public void mouseDragged(MouseEvent e, Vector3d point) {
	}

	public void mouseMoved(MouseEvent e, Vector3d point) {
	}

	public void mouseClicked(MouseEvent e, Vector3d point) {
	}

	public void mousePressed(MouseEvent e, Vector3d point) {
	}

	public void mouseReleased(MouseEvent e, Vector3d point) {
	}

	public void mouseEntered(MouseEvent e, Vector3d point) {
	}

	public void mouseExited(MouseEvent e, Vector3d point) {
	}
	
	public RENDER_MODE getRenderMode(){
		return this.renderMode;
	}
	
	abstract public void loadStateFile(JSONObject jsonObject);
	abstract public void writeStateFile(JSONObject jsonObject);

	public abstract void setVisible(boolean visible);
	public abstract boolean isVisible();
	
	public String getName(){
		return pluginName;
	}

	abstract public void load();
	abstract public void remove();

	@Override
	public String toString() {
		return pluginName;
	}
	
	abstract public boolean checkBadRequests(LocalDateTime firstDate, LocalDateTime lastDate);
	abstract public int getBadRequestCount();
	abstract public void retryBadReqeuest();
	
	public void addBadRequest(AbstractRequest request) {
		badRequests.add(request);
	}

}
