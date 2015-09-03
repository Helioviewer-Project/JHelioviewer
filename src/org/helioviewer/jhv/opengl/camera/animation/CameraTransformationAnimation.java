package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.opengl.camera.Camera;

public class CameraTransformationAnimation implements CameraAnimation{

	private boolean finished = false;
	private long timeLeft;
	private Camera camera;
	private long startTime = -1;
	
	private Quaternion3d rotation;
	private Vector3d translation;
	
	public CameraTransformationAnimation(Quaternion3d rotation, Vector3d translation, Camera camera) {
        this(rotation, translation, camera, CameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public CameraTransformationAnimation(Quaternion3d rotation, Vector3d translation, Camera camera, long duration) {
    	this.rotation = rotation;
    	this.translation = translation;
        this.timeLeft = duration;
        this.camera = camera;
    }

	
	@Override
	public void animate(MainPanel compenentView) {
		if (startTime < 1){
			startTime = System.currentTimeMillis();
			camera.repaint();
		}
		else if (timeLeft < System.currentTimeMillis() - startTime){
			this.finished = true;
			camera.setRotation(this.rotation);
		}
		else {
			Vector3d direction = camera.getTranslation().negate().add(this.translation);			
			double time = (1-Math.cos((System.currentTimeMillis() - startTime)/(double)this.timeLeft*Math.PI)) / 2.0;
			Quaternion3d rotation = camera.getRotation().slerp(this.rotation, time);
			Vector3d translation = camera.getTranslation().add(direction.scale(time));
			camera.setTransformation(rotation, translation);
		}
		
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void updateWithAnimation(CameraAnimation animation) {
		
		
	}


}
