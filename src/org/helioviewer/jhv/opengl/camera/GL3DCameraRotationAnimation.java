package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.gui.GuiState3DWCS;

/**
 * This animation rotates the camera from a startpoint to an endpoint by using
 * the {@link Quaternion3d}'s slerp interpolation.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraRotationAnimation implements GL3DCameraAnimation {
	private boolean isFinished = false;

	private long duration = 0;
	private long startAnimation = -1;
	
	private Quaternion3d endRotation;

	public GL3DCameraRotationAnimation(Quaternion3d endRotation) {
		this(endRotation, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
	}

	public GL3DCameraRotationAnimation(Quaternion3d endRotation, long duration) {
		this.endRotation = endRotation.copy();
		this.duration = duration;
		GuiState3DWCS.mainComponentView.regristryAnimation(duration);
	}

	public void animate(GL3DCamera camera) {
		if (startAnimation == -1) startAnimation = System.currentTimeMillis();
		
		long timeDelta = System.currentTimeMillis() - startAnimation;
		if (timeDelta >= duration) {
			camera.getRotation().set(this.endRotation);
			this.isFinished = true;
		}
		if (!this.isFinished) {
			double t = ((double) timeDelta) / this.duration;
			camera.getRotation().set(
					camera.getRotation().slerp(this.endRotation,
							0.5 - Math.cos(t * Math.PI) * 0.5));
		}
		camera.updateCameraTransformation();

	}

	public void updateWithAnimation(GL3DCameraAnimation animation) {
		if (animation instanceof GL3DCameraRotationAnimation) {
			GL3DCameraRotationAnimation ani = (GL3DCameraRotationAnimation) animation;
			this.duration = startAnimation - System.currentTimeMillis() + ani.duration;
			startAnimation = System.currentTimeMillis();
			this.endRotation = ani.endRotation.copy();
		}
	}

	public boolean isFinished() {
		return isFinished;
	}
}
