package org.helioviewer.jhv.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.MainFrame;

public class ShutdownManager
{
	public enum ShutdownPhase
	{
		STOP_WORK_1,
		SAVE_SETTINGS_2,
		CLEANUP_3
	}
	
	private static final Map<ShutdownPhase,List<Runnable>> onShutdown=new HashMap<>();

	public static synchronized void addShutdownHook(ShutdownPhase _phase,Runnable _r)
	{
		if(!ShutdownManager.onShutdown.containsKey(_phase))
			ShutdownManager.onShutdown.put(_phase, new ArrayList<Runnable>());
		
		ShutdownManager.onShutdown.get(_phase).add(_r);
	}

	public static void shutdownWithoutConfirmation(boolean _synchronous)
	{
		UILatencyWatchdog.stopWatchdog();
		
		MainFrame.SINGLETON.startWaitCursor();
		MainFrame.SINGLETON.setVisible(false);
		
		if(_synchronous)
		{
			System.out.println("Running shutdown hooks");
			for(ShutdownPhase p:ShutdownPhase.values())
				if(ShutdownManager.onShutdown.get(p)!=null)
					for(Runnable r:ShutdownManager.onShutdown.get(p))
						r.run();
			
			System.out.println("Quitting application");
			System.exit(0);
		}
		else
			SwingUtilities.invokeLater(() ->
				{
					System.out.println("Running shutdown hooks");
					for(ShutdownPhase p:ShutdownPhase.values())
						if(ShutdownManager.onShutdown.get(p)!=null)
							for(Runnable r:ShutdownManager.onShutdown.get(p))
								r.run();
					
					System.out.println("Quitting application");
					System.exit(0);
				});
	}

	public static void removeShutdownHook(Runnable _r)
	{
		for(ShutdownPhase p:ShutdownPhase.values())
			if(ShutdownManager.onShutdown.get(p)!=null)
				ShutdownManager.onShutdown.get(p).remove(_r);
	}
}
