package org.helioviewer.jhv.opengl.camera.animation;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.opengl.camera.Camera;

/**
 * GL3DCameraAnimations are used to continuously change the camera over a
 * certain amount of time. Register animations to the currently active
 * {@link GL3DCamera}. Make sure that isFinished returns true as soon as the
 * animation should stop.
 */
public abstract class CameraAnimation
{
    public static final long DEFAULT_ANIMATION_TIME = 400;
    
    protected long timeLeft=0;
    private long lastTime;
    protected final long duration;
    
    CameraAnimation(long _timeLeft)
    {
    	duration=timeLeft=_timeLeft;
    }
    
    /**
     * Animate pass, called once per render loop by the {@link GL3DCamera}.
     * 
     * @param camera
     *            Active camera that this animation can be applied to.
     */
    public abstract void animate(Camera _cam);

    /**
     * Gets delta time between two calls in milliseconds
     * @return
     */
    public final double getAndResetTimeDelta()
    {
        if (lastTime == 0)
            lastTime = System.currentTimeMillis();
        
        long newTime = System.currentTimeMillis();
        long timeDelta = newTime - lastTime;
        lastTime = newTime;
        
        if(timeDelta>timeLeft)
        	timeDelta=timeLeft;
        
        double oldTimeRelative = 1 - timeLeft/(double)duration;
        timeLeft -= timeDelta;
        double newTimeRelative = 1 - timeLeft/(double)duration;
        
        return MathUtils.cosinize(MathUtils.cosinize(1-newTimeRelative))
             - MathUtils.cosinize(MathUtils.cosinize(1-oldTimeRelative));
        
        //return timeDelta/(double)duration;
    }
    
    /**
     * Return true if the animation has finished. The animation will then be
     * removed from the camera.
     * 
     * @return true if the animation has already finished
     */
    public final boolean isFinished()
    {
    	return timeLeft<=0;
    }
}
