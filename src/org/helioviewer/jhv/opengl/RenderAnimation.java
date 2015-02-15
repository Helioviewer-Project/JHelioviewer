package org.helioviewer.jhv.opengl;

import javax.media.opengl.GL2;

public interface RenderAnimation {

	public void render(GL2 gl,double canvasWidth, double canvasHeight);
	public void isFinish();
}
