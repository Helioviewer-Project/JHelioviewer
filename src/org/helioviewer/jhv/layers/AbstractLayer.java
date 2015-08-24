package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class AbstractLayer {
	protected boolean visible;
	protected String name;
	protected ArrayList<AbstractRequest> badRequests;
	protected boolean isImageLayer = false;
	protected boolean isDownloadable = false;

	public String getName() {
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
		MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews(20);
	}

	public boolean isImageLayer() {
		return isImageLayer;
	}

	abstract public boolean renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel);

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

	abstract void remove();

	public int getBadRequestCount() {
		return badRequests.size();
	}

	public void addBadRequests(ArrayList<AbstractRequest> badRequests) {
		this.badRequests = badRequests;
		MainFrame.LAYER_PANEL.repaintPanel();
	}

	public void clearBadRequests() {
		badRequests.clear();
		MainFrame.LAYER_PANEL.repaintPanel();
	}

	public void retryBadRequest() {
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				AbstractRequest[] requests = new AbstractRequest[badRequests.size()];
				badRequests.toArray(requests);
				badRequests.clear();
				MainFrame.LAYER_PANEL.repaintPanel();
				for (AbstractRequest request : requests){
					request.setRetries(3);
					UltimateDownloadManager.addRequest(request);
				}
			}
		}, "RETRY-REQUESTS");
		thread.start();
	}
}
