package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.GuiState3DWCS;

/**
 * This animations changes the camera's panning (x- and y-translation) by the
 * specified amount.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraPanAnimation implements GL3DCameraAnimation {
    private boolean isFinished = false;

    private long startTime = -1;
    private long lastAnimationTime = -1;
    private long timeLeft = 0;

    private Vector3d distanceToMove;
    private Vector3d distanceDelta;
    private Vector3d targetTranslation;

    public GL3DCameraPanAnimation(Vector3d distanceToMove) {
        this(distanceToMove, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public GL3DCameraPanAnimation(Vector3d distanceToMove, long duration) {
        this.distanceToMove = distanceToMove;
        this.timeLeft = duration;
        this.distanceDelta = distanceToMove.scale(1/(double)this.timeLeft);
        GuiState3DWCS.mainComponentView.regristryAnimation(duration);
    }    
    
    public void animate(GL3DCamera camera) {
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis();
            this.lastAnimationTime = System.currentTimeMillis();
            this.targetTranslation = camera.getTranslation().add(distanceToMove);
        }

        long timeDelta = System.currentTimeMillis() - lastAnimationTime;

        this.timeLeft -= timeDelta;

        if (timeLeft <= 0) {
            camera.setPanning(this.targetTranslation.x, this.targetTranslation.y);
            this.isFinished = true;
            camera.updateCameraTransformation(true);
        } else {
            Vector3d translation = this.distanceDelta.scale((double)timeDelta);
            camera.addPanning(translation.x, translation.y);
            camera.updateCameraTransformation(false);
        }

        this.lastAnimationTime = System.currentTimeMillis();
    }

    public void updateWithAnimation(GL3DCameraAnimation animation) {
        if (animation instanceof GL3DCameraPanAnimation) {
            GL3DCameraPanAnimation ani = (GL3DCameraPanAnimation) animation;
            this.timeLeft = Math.min(2000, this.timeLeft / 5 + ani.timeLeft);
            this.distanceToMove = this.distanceToMove.add(ani.distanceToMove);
            this.distanceDelta = distanceToMove.scale(1/(double)this.timeLeft);
        }
    }

    public boolean isFinished() {
        return isFinished;
    }
}
