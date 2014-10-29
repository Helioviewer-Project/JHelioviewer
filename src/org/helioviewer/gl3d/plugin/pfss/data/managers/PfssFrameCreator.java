package org.helioviewer.gl3d.plugin.pfss.data.managers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.data.PfssFrame;

/**
 * This class is responsible for creating PfssFrames out of PfssData objects. This task can take some time, it is implemented asynchronously
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssFrameCreator {
	private final PfssFrameInitializer initializer;
	private final ExecutorService pool = Executors.newCachedThreadPool();
	
	public PfssFrameCreator(PfssFrameInitializer initializer){
		this.initializer = initializer;
	}
	
	/**
	 * Creates a PfssFrame object asynchronously
	 * @param data PfssData to create the frame from. PfssData does not have to be fully loaded.
	 * @return PfssFrame object which will be fully loaded and initialized in the future
	 */
	public PfssFrame createFrameAsync(PfssData data) {
		PfssFrame frame = new PfssFrame(data.getDateRange());
		PfssDataReader r = new PfssDataReader(data,frame,initializer);
		pool.execute(r);
		return frame;
	}
}
