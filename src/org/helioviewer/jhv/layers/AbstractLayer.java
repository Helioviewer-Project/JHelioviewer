package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.gui.MainFrame;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class AbstractLayer {
	protected boolean visible;
	protected String name;
	private ArrayList<AbstractRequest> badRequests;
	protected boolean isImageLayer = false;
	protected boolean isDownloadable = false; 
	
	public String getName(){
		return name;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	public boolean checkBadRequest() {
		return badRequests != null && !badRequests.isEmpty();
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
	}
	
	public boolean isImageLayer(){
		return isImageLayer;
	}
	
	abstract void renderLayer(GL2 gl);

	public void cancelDownload() {
	}

	public boolean isDownloadable() {
		return isDownloadable;
	}

	public void writeStateFile(JSONObject jsonLayer) {
		// TODO Auto-generated method stub
		
	}

	public String getURL() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFullName() {
		// TODO Auto-generated method stub
		return null;
	}

	public LocalDateTime getTime() {
		// TODO Auto-generated method stub
		return null;
	}
}
