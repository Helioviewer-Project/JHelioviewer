package org.helioviewer.jhv.viewmodel.view;

/**
 * Animation mode.
 * 
 * @see MovieView#setAnimationMode(AnimationMode)
 */
public enum AnimationMode
{
    LOOP
    {
        public String toString()
        {
            return "Loop";
        }
    },
    STOP
    {
        public String toString()
        {
            return "Stop";
        }
    },
    SWING
    {
        public String toString()
        {
            return "Swing";
        }
    }
}