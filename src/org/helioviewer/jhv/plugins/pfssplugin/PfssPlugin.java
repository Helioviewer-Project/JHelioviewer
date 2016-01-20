package org.helioviewer.jhv.plugins.pfssplugin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.pfssplugin.data.FrameManager;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;
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
		super("PFSS", RenderMode.MAIN_PANEL);
		manager = new FrameManager(this);
	}
	
	@Override
	public void render(GL2 gl)
	{
		LocalDateTime localDateTime = Plugins.SINGLETON.getCurrentDateTime();
		PfssDecompressed frame = manager.getFrame(gl, localDateTime);
		
		if (frame != null)
			frame.display(gl, localDateTime);
	}

	@Override
	public void restoreConfiguration(JSONObject jsonObject)
	{
		/*if (jsonObject.has(JSON_NAME))
		{
			try
			{
				JSONObject jsonPfss = jsonObject.getJSONObject(JSON_NAME);
			}
			catch (JSONException e)
			{
				Telemetry.trackException(e);
			}
		}*/
	}

	@Override
	public void storeConfiguration(JSONObject jsonObject)
	{
		/*try
		{
			JSONObject jsonPfss = new JSONObject();
			jsonObject.put(JSON_NAME, jsonPfss);
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}*/
	}
	
	@Override
	public void timeRangeChanged(LocalDateTime _start, LocalDateTime _end)
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