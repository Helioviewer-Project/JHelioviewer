package org.helioviewer.jhv.base;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.statusLabels.FramerateStatusPanel;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import com.microsoft.applicationinsights.TelemetryClient;

public class Telemetry
{
	private static final TelemetryClient client;
	private static boolean openGLInitialized=false;
	private static final long START_TIME=System.currentTimeMillis();
	
	static
	{
		client = new TelemetryClient();
		
		client.getContext().getDevice().setScreenResolution((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()+"x"+(int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		client.getContext().getDevice().setOperatingSystem(System.getProperty("os.name"));
		client.getContext().getDevice().setOperatingSystemVersion(System.getProperty("os.version"));
		client.getContext().getComponent().setVersion(Globals.VERSION);
		client.getContext().getUser().setId(Settings.getString("UUID"));
		client.getContext().getSession().setId(UUID.randomUUID().toString());
		
		client.getContext().getProperties().put("Cores", Runtime.getRuntime().availableProcessors()+"");
		client.getContext().getProperties().put("JavaFX", Globals.JAVA_FX_AVAILABLE+"");
		
		client.getContext().getProperties().put("Screen size", Toolkit.getDefaultToolkit().getScreenSize().width+"x"+Toolkit.getDefaultToolkit().getScreenSize().height);
		client.getContext().getProperties().put("DPI", Toolkit.getDefaultToolkit().getScreenResolution()+"");
		
		
		ExitProgramAction.addShutdownHook(new Runnable()
		{
			@Override
			public void run()
			{
				Telemetry.trackMetric("Session duration", (System.currentTimeMillis()-START_TIME)/1000);
			}
		});
		
		
		
		Thread telemetryCollection = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				for(;;)
					try
					{
						Thread.sleep(70000);
						SwingUtilities.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								List<Layer> layers=Layers.getLayers();
								
								if(TimeLine.SINGLETON.isPlaying())
								Telemetry.trackMetric("FPS Current", FramerateStatusPanel.getFPS());
								Telemetry.trackMetric("FPS Target", 1000f/TimeLine.SINGLETON.getMillisecondsPerFrame());
								Telemetry.trackMetric("FPS Relative", FramerateStatusPanel.getFPS()/(1000f/TimeLine.SINGLETON.getMillisecondsPerFrame()));
								
								Telemetry.trackMetric("Layers", layers.size());
								for(Layer l:layers)
									if(l instanceof AbstractImageLayer)
									{
										AbstractImageLayer il=(AbstractImageLayer)l;
										Telemetry.trackMetric("Layer opacity", il.opacity);
										Telemetry.trackMetric("Layer contrast", il.contrast);
										Telemetry.trackMetric("Layer sharpness", il.sharpness);
										Telemetry.trackMetric("Layer red", il.redChannel?1:0);
										Telemetry.trackMetric("Layer green", il.greenChannel?1:0);
										Telemetry.trackMetric("Layer blue", il.blueChannel?1:0);
										Telemetry.trackMetric("Layer inverted", il.invertedLut?1:0);
										Telemetry.trackMetric("Layer visible", il.isVisible()?1:0);
										Telemetry.trackMetric("Layer corona", il.isCoronaVisible()?1:0);
									}
							}
						});
					}
					catch (InterruptedException e)
					{
						return;
					}
					catch (Exception e)
					{
						Telemetry.trackException(e);
					}
			}
		});
		
		
		
		telemetryCollection.setName("Telemetry collection");
		telemetryCollection.setDaemon(true);
		telemetryCollection.setPriority(Thread.MIN_PRIORITY);
		telemetryCollection.start();
	}
	
	public static void trackEvent(String _event,String ... params)
	{
		if(!Globals.isReleaseVersion())
			return;
		
		initializeOpenGL();
		
		Map<String,String> ps=new LinkedHashMap<String,String>();
		for(int i=0;i<params.length;i+=2)
			ps.put(params[i], params[i+1]);
		
		client.trackEvent(_event,ps,new HashMap<String, Double>());
	}
	
	public static void trackMetric(String _name,double _value)
	{
		client.trackMetric(_name, _value);
	}
	
	private synchronized static void initializeOpenGL()
	{
		try
		{
			if(openGLInitialized || GLContext.getCurrent()==null)
				return;
			
			client.getContext().getProperties().put("OpenGL renderer", GLContext.getCurrentGL().glGetString(GL.GL_RENDERER));
			openGLInitialized=true;
			
			/*client.getContext().getProperties().put("OpenGL version", GLContext.getCurrent().getGLVersion());
			client.getContext().getProperties().put("OpenGL GLSL version", GLContext.getCurrent().getGLSLVersionString());*/
		}
		catch(Exception _e)
		{
			_e.printStackTrace();
		}
	}

	public static void trackException(Throwable _e)
	{
		_e.printStackTrace();

		if(!Globals.isReleaseVersion())
			return;
		
		initializeOpenGL();
		
		if(_e instanceof Exception)
			client.trackException((Exception)_e);
		else
			client.trackException(new Exception(_e));
	}
}
