package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.camera.Camera;

public class CameraTranslationAnimation extends CameraAnimation
{
    private Vector3d translationDelta;
	
	public CameraTranslationAnimation(Camera _cam,Vector3d _translationDelta)
	{
        this(_cam, _translationDelta, CameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public CameraTranslationAnimation(Camera _cam, Vector3d _translationDelta, long _duration)
    {
    	super(_duration);
    	translationDelta = _translationDelta;
    	_cam.setTranslationEnd(_cam.getTranslationEnd().add(_translationDelta));
    }
	
	@Override
	public void animate(Camera _cam)
	{
		_cam.setTranslationCurrent(_cam.getTranslationCurrent().add(translationDelta.scaled(getAndResetTimeDelta())));
	}
}
