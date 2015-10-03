package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.camera.Camera;

/**
 * This animation zooms the camera by a given amount. Zooming only affects the
 * z-component of the {@link GL3DCamera}'s translation.
 */
public class CameraZoomAnimation extends CameraAnimation
{
    private double distanceDelta;

    public CameraZoomAnimation(Camera _cam, double _distanceDelta)
    {
        this(_cam, _distanceDelta, CameraAnimation.DEFAULT_ANIMATION_TIME);
    }

    public CameraZoomAnimation(Camera _cam, double _distanceDelta, long _duration)
    {
    	super(_duration);
    	distanceDelta = _distanceDelta;
        
        _cam.setTranslationEnd(_cam.getTranslationEnd().add(new Vector3d(0,0,_distanceDelta)));
    }

    @Override
    public void animate(Camera _cam)
    {
    	if(isFinished())
    		return;
    	
        _cam.setTranslationCurrent(_cam.getTranslationCurrent().add(new Vector3d(0,0,distanceDelta * getAndResetTimeDelta())));
    }
}
