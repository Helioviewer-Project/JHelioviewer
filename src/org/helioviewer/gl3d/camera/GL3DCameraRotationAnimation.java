package org.helioviewer.gl3d.camera;

import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.jhv.gui.GuiState3DWCS;

/**
 * This animation rotates the camera from a startpoint to an endpoint by using
 * the {@link GL3DQuatd}'s slerp interpolation.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraRotationAnimation implements GL3DCameraAnimation {
    private boolean isFinished = false;

    private long lastAnimationTime = -1;
    private long timeLeft = 0;
    private long duration = 0;

    private GL3DQuatd endRotation;
    
    
    public GL3DCameraRotationAnimation(GL3DQuatd endRotation){
    	this(endRotation, GL3DCameraAnimation.DEFAULT_ANIMATION_TIME);
    }
    
    public GL3DCameraRotationAnimation(GL3DQuatd endRotation, long duration){
    	this.endRotation = endRotation.copy();
    	this.duration = duration;
        this.timeLeft = duration;
        System.currentTimeMillis();
        this.lastAnimationTime = System.currentTimeMillis();
        GuiState3DWCS.mainComponentView.regristryAnimation(duration);        
    }

    
    
    public void animate(GL3DCamera camera) {
    	
    	long timeDelta = System.currentTimeMillis() - lastAnimationTime;
    	
    	this.timeLeft -= timeDelta;
        if (timeLeft <= 0) {
        	timeLeft = 0;
            this.isFinished = true;
        }
        double t = 1 - ((double) this.timeLeft) / this.duration;
        camera.getRotation().set(camera.getRotation().slerp(this.endRotation, 0.5-Math.cos(t*Math.PI)*0.5));
    
        camera.updateCameraTransformation();

        this.lastAnimationTime = System.currentTimeMillis();
    }

    public void updateWithAnimation(GL3DCameraAnimation animation) {
    	if (animation instanceof GL3DCameraRotationAnimation){
    		GL3DCameraRotationAnimation ani = (GL3DCameraRotationAnimation)animation;
    		this.duration = this.timeLeft + ani.duration;
    		this.timeLeft = ani.timeLeft;
    		//this.duration += ani.duration;
    		this.endRotation = ani.endRotation.copy();
    	}
    }

    public boolean isFinished() {
        return isFinished;
    }
}
