package org.helioviewer.jhv.plugins.pfssplugin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.layers.PluginLayer;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.pfssplugin.data.FrameManager;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PfssPlugin extends Plugin
{
	private static int threadNumber = 0;
	
	public static final ExecutorService pool = Executors.newFixedThreadPool(2, new ThreadFactory()
	{
		@Override
		public Thread newThread(@Nullable Runnable _r)
		{
			Thread t = Executors.defaultThreadFactory().newThread(_r);
			t.setName("PFSS-" + (threadNumber++));
			t.setDaemon(true);
			return t;
		}
	});
	
	private FrameManager manager;
	public ArrayList<HTTPRequest> failedDownloads = new ArrayList<>();
	
	public PfssPlugin()
	{
		super("PFSS", "PFSS", RenderMode.MAIN_PANEL);
		manager = new FrameManager(this);
	}
	
	@Override
	public boolean supportsFilterOpacity()
	{
		return true;
	}
	
	@Override
	public void render(GL2 gl, PluginLayer _imageParams)
	{
		LocalDateTime localDateTime = Plugins.SINGLETON.getCurrentDateTime();
		PfssDecompressed frame = manager.getFrame(gl, localDateTime);
		
		if (frame != null)
			frame.display(gl, localDateTime, _imageParams.opacity);
	}

	@Override
	public void restoreConfiguration(JSONObject jsonObject)
	{
	}

	@Override
	public void storeConfiguration(JSONObject jsonObject) throws JSONException
	{
	}
	
	@Override
	public void timeRangeChanged(@Nullable LocalDateTime _start, @Nullable LocalDateTime _end)
	{
		manager.setDateRange(_start, _end);
	}
	
	@Override
	public boolean retryNeeded()
	{
		return !failedDownloads.isEmpty();
	}

	@Override
	public void retry()
	{
		if (manager.getStartDate() == null || manager.getEndDate() == null)
		{
			LocalDateTime startLocalDateTime = Plugins.getStartDateTime();
			LocalDateTime endLocalDateTime = Plugins.getEndDateTime();
			if (startLocalDateTime != null && endLocalDateTime != null)
				manager.setDateRange(startLocalDateTime, endLocalDateTime);
		}
		else
			manager.retry();
	}
}