package org.helioviewer.gl3d.plugin.pfss.data.managers;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL;

import org.helioviewer.gl3d.plugin.pfss.data.PfssFrame;

/**
 * Responsible for initializing a frame which was loaded into memory asynchronously.
 * this class is threadsafe
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssFrameInitializer {
	private final ConcurrentLinkedQueue<PfssFrame> queue = new ConcurrentLinkedQueue<>();
	
	
	public void addLoadedFrame(PfssFrame frame) {
		queue.add(frame);
	}
	
	public void init(GL gl) {
		
		PfssFrame f = queue.poll();
		if(f != null)
			f.init(gl);
	}
}
