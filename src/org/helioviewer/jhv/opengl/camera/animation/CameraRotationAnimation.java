package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.opengl.camera.Camera;

public class CameraRotationAnimation extends CameraAnimation
{
	private Quaternion rotationDelta;
	
	public CameraRotationAnimation(Camera _cam,Quaternion _rotationDelta)
	{
        this(_cam,_rotationDelta, CameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public CameraRotationAnimation(Camera _cam,Quaternion _rotationDelta, long _duration)
    {
    	super(_duration);
    	rotationDelta = _rotationDelta.normalized();
    	_cam.setRotationEnd(_cam.getRotationEnd().rotated(_rotationDelta));
    }
	
	@Override
	public void animate(Camera _cam)
	{
		if(isFinished())
			return;
		
		_cam.setRotationCurrent(_cam.getRotationCurrent().rotated(rotationDelta.powered(getAndResetTimeDelta())));
	}
}
