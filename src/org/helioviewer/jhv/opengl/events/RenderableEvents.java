package org.helioviewer.jhv.opengl.events;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.OpenGLHelper;

import com.jogamp.opengl.GL2;

public class RenderableEvents {
	private int textureID = -1;
	private final String name;
	private OpenGLHelper openGLHelper;
	private Vector3d[] bounds = null;
	private Vector3d[] area = null;
	private Vector3d[] position;
	
	private ConcurrentSkipListSet<RenderableEvent> events;
	
	enum VISIBLE_MDOE{
		VISIBLE, INVISIBLE, PREVIEW;
	}
	
	enum RENDER_MODE{
		ALL_PANEL, OVERVIEW_PANEL, MAIN_PANEL;
	}	
	
	
	private Comparator<RenderableEvent> comparator = new Comparator<RenderableEvent>() {

		@Override
		public int compare(RenderableEvent o1, RenderableEvent o2) {
			return 0;
		}
	};

	
	private boolean visible = false;
	
	private RENDER_MODE renderMode = RENDER_MODE.ALL_PANEL;
	
	public RenderableEvents(String name) {
		this.name = name;
		events = new ConcurrentSkipListSet<RenderableEvent>(comparator);
	}
	
	public RenderableEvents(String name, BufferedImage image){
		this.name = name;
		
		events = new ConcurrentSkipListSet<RenderableEvent>(comparator);
		events = new ConcurrentSkipListSet<RenderableEvent>();
		
		this.openGLHelper = new OpenGLHelper();
		textureID = openGLHelper.createTextureID();
		openGLHelper.bindBufferedImageToGLTexture(image);
	}
	
	public void setBounds(Vector3d[] bounds){
		this.bounds = bounds;
	}
	
	public void setArea(Vector3d[] area){
		this.area = area;
	}

	public void render(GL2 gl){
		if (textureID >= 0){
			renderIcon(gl);
		}
		if (bounds != null){
			renderBounds(gl);
		}
		if (area != null){
			renderPolygonArea(gl);
		}
	}
	
	
	private void renderIcon(GL2 gl){
		
	}
	
	private void renderBounds(GL2 gl){
		
	}
	
	private void renderPolygonArea(GL2 gl){
		
	}
}
