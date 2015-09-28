package org.helioviewer.jhv;

import java.awt.Toolkit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.microsoft.applicationinsights.TelemetryClient;

public class Telemetry
{
	private static final TelemetryClient client;
	
	static
	{
		/*for(Map.Entry p:System.getProperties().entrySet())
			System.out.println(p.getKey()+"="+p.getValue());
		System.exit(0);*/
		
		client = new TelemetryClient();
		
		client.getContext().getDevice().setScreenResolution((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()+"x"+(int)Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		client.getContext().getDevice().setOperatingSystem(System.getProperty("os.name"));
		client.getContext().getDevice().setOperatingSystemVersion(System.getProperty("os.version"));
		client.getContext().getComponent().setVersion(Globals.VERSION);
		client.getContext().getUser().setId(Settings.getProperty("UUID"));
		client.getContext().getSession().setId(UUID.randomUUID().toString());
		
		client.getContext().getProperties().put("Cores", Runtime.getRuntime().availableProcessors()+"");
		client.getContext().getProperties().put("JavaFX", Globals.USE_JAVA_FX+"");
		
		
		//TODO: track opengl info
		//OpenGLHelper.glContext.
		
		//client.getContext().getDevice().setOperatingSystemVersion(operatingSystemVersion);
	}
	
	public static void trackEvent(String _event,String ... params)
	{
		if(!Globals.isReleaseVersion())
			return;
		
		Map<String,String> ps=new LinkedHashMap<String,String>();
		for(int i=0;i<params.length;i+=2)
			ps.put(params[i], params[i+1]);
		client.trackEvent("Startup",ps,new HashMap<String, Double>());
	}
	
	public static void trackException(Throwable _e)
	{
		_e.printStackTrace();
		
		if(_e instanceof Exception)
			client.trackException((Exception)_e);
		else
			client.trackException(new Exception(_e));
	}
}
