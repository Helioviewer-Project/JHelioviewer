package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.helioviewer.jhv.base.downloadmanager.AbstractDownloadRequest;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class AbstractLayer
{
	private boolean visible;
	protected String name;
	
	//FIXME: synchronization not done properly
	protected ArrayList<AbstractDownloadRequest> failedRequests;
	
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

	public void setVisible(boolean visible)
	{
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
		

	}

	public String getURL() {
		
		return null;
	}

	public String getFullName()
	{
		return null;
	}

	public LocalDateTime getTime()
	{
		return null;
	}

	abstract void remove();

	public int getBadRequestCount()
	{
		return failedRequests.size();
	}

	public void clearBadRequests()
	{
		failedRequests.clear();
		MainFrame.LAYER_PANEL.repaintPanel();
	}

	public void retryFailedRequests()
	{
		//FIXME
		throw new RuntimeException("Not yet implemented");
		/*Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				AbstractDownloadRequest[] requests;
				synchronized(failedRequests)
				{
					requests = new AbstractDownloadRequest[failedRequests.size()];
					failedRequests.toArray(requests);
					failedRequests.clear();
				}
				MainFrame.LAYER_PANEL.repaintPanel();
				for (AbstractDownloadRequest request : requests)
				{
					request.setRetries(3);
					UltimateDownloadManager.addRequest(request);
				}
			}
		}, "RETRY-REQUESTS");
		thread.setDaemon(true);
		thread.start();*/
	}
}
