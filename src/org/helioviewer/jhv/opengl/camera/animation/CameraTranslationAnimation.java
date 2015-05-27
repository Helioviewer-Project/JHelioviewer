package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class CameraTranslationAnimation implements CameraAnimation {

	private boolean finished = false;
    private Vector3d translation;
	private long timeLeft;
	private Camera camera;
	private long startTime = -1;
	public CameraTranslationAnimation(Vector3d translation, Camera camera) {
        this(translation, camera, CameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public CameraTranslationAnimation(Vector3d translation, Camera camera, long duration) {
    	this.translation = translation;
        this.timeLeft = duration;
        this.camera = camera;
    }

	
	@Override
	public void animate(MainPanel compenentView) {
		if (startTime < 1){
			startTime = System.currentTimeMillis();
			camera.repaintViewAndSynchronizedViews();
		}
		else if (timeLeft < System.currentTimeMillis() - startTime){
			this.finished = true;
			camera.setTranslation(this.translation);
		}
		else {
			Vector3d direction = camera.getTranslation().negate().add(this.translation);			
			double time = (1-Math.cos((System.currentTimeMillis() - startTime)/(double)this.timeLeft*Math.PI)) / 2.0;
			camera.setTranslation(camera.getTranslation().add(direction.scale(time)));
		}
		
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void updateWithAnimation(CameraAnimation animation) {
		// TODO Auto-generated method stub
		
	}

}
