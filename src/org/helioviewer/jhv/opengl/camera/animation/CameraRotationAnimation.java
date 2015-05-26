package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.controller.Camera;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class CameraRotationAnimation implements CameraAnimation{

	private boolean finished = false;
	private long timeLeft;
	private Camera camera;
	private long startTime = -1;
	
	private Quaternion3d rotation;
	
	public CameraRotationAnimation(Quaternion3d rotation, Camera camera) {
        this(rotation, camera, CameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public CameraRotationAnimation(Quaternion3d rotation, Camera camera, long duration) {
    	this.rotation = rotation;
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
			camera.setRotation(this.rotation);
		}
		else {
			double time = (1-Math.cos((System.currentTimeMillis() - startTime)/(double)this.timeLeft*Math.PI)) / 2.0;
			Quaternion3d rotation = camera.getRotation().slerp(this.rotation, time);
			System.out.println("rotation : " + rotation);
			System.out.println("carot    : " + camera.getRotation());
			System.out.println("rot      : " + this.rotation);
			camera.setRotation(rotation);
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
