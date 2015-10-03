package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.opengl.camera.Camera;

public class CameraRotationAnimation extends CameraAnimation
{
	private Quaternion3d rotationDelta;
	
	public CameraRotationAnimation(Camera _cam,Quaternion3d _rotationDelta)
	{
        this(_cam,_rotationDelta, CameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public CameraRotationAnimation(Camera _cam,Quaternion3d _rotationDelta, long _duration)
    {
    	super(_duration);
    	rotationDelta = _rotationDelta.normalized();
    	_cam.setRotationEnd(_cam.getRotationEnd().rotate(_rotationDelta));
    }
	
	@Override
	public void animate(Camera _cam)
	{
		if(isFinished())
			return;
		
		_cam.setRotationCurrent(_cam.getRotationCurrent().slerp(_cam.getRotationCurrent().rotate(rotationDelta), getAndResetTimeDelta()));
	}
}
