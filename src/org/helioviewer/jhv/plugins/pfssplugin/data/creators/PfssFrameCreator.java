package org.helioviewer.jhv.plugins.pfssplugin.data.creators;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssFrame;

/**
 * This class is responsible for creating PfssFrames out of PfssData objects. This task can take some time, it is implemented asynchronously
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssFrameCreator {
	private final ExecutorService pool = Executors.newCachedThreadPool();
	
	public PfssFrameCreator(){
		
	}
	
	/**
	 * Creates a PfssFrame object asynchronously
	 * @param data PfssData to create the frame from. PfssData does not have to be fully loaded.
	 * @return PfssFrame object which will be fully loaded in the future
	 */
	public PfssFrame getFrameAsync(PfssData data) {
		PfssFrame frame = new PfssFrame(data.getDescriptor());
		PfssDecompressor r = new PfssDecompressor(data,frame);
		pool.execute(r);
		return frame;
	}
}
