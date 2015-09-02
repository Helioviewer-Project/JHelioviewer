package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class AbstractLayer
{
	protected boolean visible;
	protected String name;
	
	//FIXME: synchronization not done properly
	protected ArrayList<AbstractRequest> failedRequests;
	
	protected boolean isImageLayer = false;
	protected boolean isDownloadable = false;

	public String getName()
	{
		return name;
	}

	public boolean isVisible()
	{
		return visible;
	}

	public boolean checkBadRequest() {
		return failedRequests != null && !failedRequests.isEmpty();
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		MainFrame.MAIN_PANEL.repaint();
	}

	public boolean isImageLayer() {
		return isImageLayer;
	}
	
	public enum RenderResult
	{
		RETRY_LATER,
		ERROR,
		OK
	}

	abstract public RenderResult renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel, ByteBuffer _imageData);

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

	public int getBadRequestCount()
	{
		return failedRequests.size();
	}

	public void addBadRequests(ArrayList<AbstractRequest> badRequests)
	{
		synchronized(failedRequests)
		{
			this.failedRequests = badRequests;
		}
		MainFrame.LAYER_PANEL.repaintPanel();
	}

	public void clearBadRequests() {
		failedRequests.clear();
		MainFrame.LAYER_PANEL.repaintPanel();
	}

	public void retryFailedRequests()
	{
		Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				AbstractRequest[] requests;
				synchronized(failedRequests)
				{
					requests = new AbstractRequest[failedRequests.size()];
					failedRequests.toArray(requests);
					failedRequests.clear();
				}
				MainFrame.LAYER_PANEL.repaintPanel();
				for (AbstractRequest request : requests){
					request.setRetries(3);
					UltimateDownloadManager.addRequest(request);
				}
			}
		}, "RETRY-REQUESTS");
		thread.setDaemon(true);
		thread.start();
	}
}
