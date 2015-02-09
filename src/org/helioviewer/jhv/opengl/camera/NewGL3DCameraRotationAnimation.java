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
public class NewGL3DCameraRotationAnimation implements GL3DCameraAnimation {
	private boolean isFinished = false;

	private long lastAnimationTime = -1;
	private long timeLeft = 0;
	private long duration = 0;

	private Quaternion3d endRotation;

	public NewGL3DCameraRotationAnimation(Quaternion3d endRotation) {
		this(endRotation, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
	}

	public NewGL3DCameraRotationAnimation(Quaternion3d endRotation, long duration) {
		this.endRotation = endRotation.copy();
		this.duration = duration;
		this.timeLeft = duration;
		this.lastAnimationTime = System.currentTimeMillis();
		GuiState3DWCS.mainComponentView.regristryAnimation(duration);
	}

	public void animate(GL3DCamera camera) {

		long timeDelta = System.currentTimeMillis() - lastAnimationTime;

		this.timeLeft -= timeDelta;
		if (timeLeft <= 0) {
			timeLeft = 0;
			camera.getRotation().set(this.endRotation);
			this.isFinished = true;
		}
		if (!this.isFinished) {
			double t = 1 - ((double) this.timeLeft) / this.duration;
			camera.getRotation().set(
					camera.getRotation().slerp(this.endRotation,
							0.5 - Math.cos(t * Math.PI) * 0.5));
		}
		camera.updateCameraTransformation();

		this.lastAnimationTime = System.currentTimeMillis();
	}

	public void updateWithAnimation(GL3DCameraAnimation animation) {
		if (animation instanceof NewGL3DCameraRotationAnimation) {
			NewGL3DCameraRotationAnimation ani = (NewGL3DCameraRotationAnimation) animation;
			this.duration = this.timeLeft + ani.duration;
			this.timeLeft = ani.timeLeft;
			// this.duration += ani.duration;
			this.endRotation = ani.endRotation.copy();
		}
	}

	public boolean isFinished() {
		return isFinished;
	}
}
