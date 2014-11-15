package org.helioviewer.jhv.opengl.scenegraph;

import org.helioviewer.jhv.opengl.scenegraph.GL3DDrawBits.Bit;

/**
 * A {@link GL3DModel} is a node within the Scene graph that can be turned on
 * and off by the user.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DModel extends GL3DGroup {
    public GL3DModel(String name) {
        super(name);
    }

    public boolean isActive() {
        return !this.isDrawBitOn(Bit.Hidden);
    }

    public void setActive(boolean value) {
        this.drawBits.set(Bit.Hidden, !value);
    }
}
