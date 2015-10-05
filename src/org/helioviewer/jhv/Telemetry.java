package org.helioviewer.jhv;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import com.microsoft.applicationinsights.TelemetryClient;

public class Telemetry
{
	private static final TelemetryClient client;
	private static boolean openGLInitialized=false;
	
	static
	{
		client = new TelemetryClient();
		
		client.getContext().getDevice().setScreenResolution((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()+"x"+(int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		client.getContext().getDevice().setOperatingSystem(System.getProperty("os.name"));
		client.getContext().getDevice().setOperatingSystemVersion(System.getProperty("os.version"));
		client.getContext().getComponent().setVersion(Globals.VERSION);
		client.getContext().getUser().setId(Settings.getProperty("UUID"));
		client.getContext().getSession().setId(UUID.randomUUID().toString());
		
		client.getContext().getProperties().put("Cores", Runtime.getRuntime().availableProcessors()+"");
		client.getContext().getProperties().put("JavaFX", Globals.JAVA_FX_AVAILABLE+"");
		
		client.getContext().getProperties().put("Screen size", Toolkit.getDefaultToolkit().getScreenSize().width+"x"+Toolkit.getDefaultToolkit().getScreenSize().height);
		client.getContext().getProperties().put("DPI", Toolkit.getDefaultToolkit().getScreenResolution()+"");
	}
	
	public static void trackEvent(String _event,String ... params)
	{
		if(!Globals.isReleaseVersion())
			return;
		
		initializeOpenGL();
		
		Map<String,String> ps=new LinkedHashMap<String,String>();
		for(int i=0;i<params.length;i+=2)
			ps.put(params[i], params[i+1]);
		client.trackEvent("Startup",ps,new HashMap<String, Double>());
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
