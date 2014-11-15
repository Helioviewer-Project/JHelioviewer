package org.helioviewer.jhv.opengl.scenegraph;

import java.util.BitSet;

/**
 * The draw bits store attributes within the scene graph. Every node has its
 * draw bits object. An attribute applies to the node and all of its child
 * nodes.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DDrawBits {
    BitSet drawBits;

    public GL3DDrawBits() {
        this.drawBits = new BitSet();
        for (Bit b : Bit.values()) {
            if(b.value)
                on(b);
        }
    }

    public void toggle(Bit bit) {
        set(bit, !get(bit));
    }

    public void on(Bit bit) {
        set(bit, true);
    }

    public void off(Bit bit) {
        set(bit, false);
    }

    public void set(Bit bit, boolean value) {
        if(value)
            drawBits.set(bit.pos);
        else
            drawBits.clear(bit.pos);
    }

    public boolean get(Bit bit) {
        return drawBits.get(bit.pos);
    }

    public enum Bit {
        Hidden(0, false), Normals(1, false), BoundingBox(2, false), Wireframe(3, false), Selected(4, false);

        public boolean value;
        public int pos;

        private Bit(int pos, boolean defaultValue) {
            this.value = defaultValue;
            this.pos = pos;
        }
    }

}
