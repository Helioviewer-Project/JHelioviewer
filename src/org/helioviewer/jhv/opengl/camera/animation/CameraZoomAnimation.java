package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainPanel;
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
    	
    	Vector3d translationEnd = _cam.getTranslationEnd();
    	
    	distanceDelta = MathUtils.clip(_distanceDelta + translationEnd.z, MainPanel.MIN_DISTANCE, MainPanel.MAX_DISTANCE) - translationEnd.z;
        _cam.setTranslationEnd(translationEnd.add(new Vector3d(0,0,distanceDelta)));
    }

    @Override
    public void animate(Camera _cam)
    {
    	if(isFinished())
    		return;
    	
        _cam.setTranslationCurrent(_cam.getTranslationCurrent().add(new Vector3d(0,0,distanceDelta * getAndResetTimeDelta())));
    }
}
