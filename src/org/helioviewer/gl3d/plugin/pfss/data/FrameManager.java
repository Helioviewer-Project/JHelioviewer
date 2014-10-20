package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.Date;

import javax.media.opengl.GL2;

/**
 * This class is responsible for managing frames. It Tries to have all frames pre-loaded and pre-initialized
 * 
 * @author Jonas Schwammberger
 */
public class FrameManager {
	private final PfssDataCreator loader;
	private final PfssFrameCreator frameCreator;
	private final PfssFrameInitializer initializer;
	
	private PfssFrame[] preloadedFrames;
	
	public FrameManager() {
		loader = new PfssDataCreator();
		initializer = new PfssFrameInitializer();
		frameCreator = new PfssFrameCreator(initializer);
	}
	
	public PfssFrame getFrame(Date date) {
		return null;
	}
	
	public void preInitFrames(GL2 gl) {
		initializer.init(gl);
	}
	
}
