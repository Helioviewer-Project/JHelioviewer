package org.helioviewer.jhv.opengl.scenegraph;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.opengl.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;

/**
 * A {@link GL3DShape} is a {@link GL3DNode} that does have a position and a
 * bounding box within the scene graph. In practice, almost every
 * {@link GL3DNode} is also a {@link GL3DShape}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DShape extends GL3DNode {
    // Model Matrix
    protected Matrix4d mv;

    // World Matrix
    protected Matrix4d wm;
    protected Matrix4d wmI;

    protected GL3DAABBox aabb;

    public GL3DShape(String name) {
        this(name, null);
    }

    public GL3DShape(String name, CoordinateSystem coordinateSystem) {
        super(name);

        this.mv = Matrix4d.identity();
        this.wm = Matrix4d.identity();
        this.aabb = new GL3DAABBox();
    }

    public void init(GL3DState state) {
        state.pushMV();
        this.wm = state.multiplyMV(this.mv);
        state.buildInverseAndNormalMatrix();
        this.wmI = new Matrix4d(state.getMVInverse());
        
        this.shapeInit(state);
        this.buildAABB();
        this.isInitialised = true;
        state.popMV();
    }

    public void update(GL3DState state) {
        if (!this.isInitialised) {
            this.init(state);
        }
		if (this.hasChanged()) {
            state.pushMV();
            //this.updateMatrix(state);
            this.wm = state.multiplyMV(this.mv);
            state.buildInverseAndNormalMatrix();
            this.wmI = new Matrix4d(state.getMVInverse());
            this.shapeUpdate(state);
            this.setUnchanged();
            this.buildAABB();
            state.popMV();
        }
    }

    public void updateMatrix(GL3DState state) {
    	
    }

    public void draw(GL3DState state) {
    	if (!isDrawBitOn(Bit.Hidden)) {
            // Log.debug("GL3DShape: Drawing '"+getName()+"'");
            state.pushMV();
            state.multiplyMV(this.mv);
            state.buildInverseAndNormalMatrix();
            // this.wmI = new GL3DMat4d(state.getMVInverse());
            // this.wmN = new GL3DMat3d(state.normalMatrix);
            this.shapeDraw(state);

            if (isDrawBitOn(Bit.BoundingBox)) {
                if (GL3DGroup.class.isAssignableFrom(this.getClass())) {
                    // Is it the root?
                    if (this.parent == null) {
                        state.gl.glLineWidth(2.0f);
                        this.aabb.drawOS(state, new Vector4d(0, 1, 0, 1));
                        state.gl.glLineWidth(1.0f);
                    } else {
                        this.aabb.drawOS(state, new Vector4d(0, 1, 1, 1));
                    }
                } else {
                    this.aabb.drawOS(state, new Vector4d(1, 0, 0, 1));
                }
            } else if (isDrawBitOn(Bit.Selected)) {
                state.gl.glLineWidth(2.0f);
                this.aabb.drawOS(state, new Vector4d(0, 0.0, 1, 1));
                state.gl.glLineWidth(1.0f);
            }

            state.popMV();
        }
    }

    public boolean hit(GL3DRay ray) {
        // if its hidden, it can't be hit
        if (isDrawBitOn(Bit.Hidden)) {
            return false;
        }

        // First check if bounding Box is hit
        if (!this.aabb.isHitInWS(ray)) {
            return false;
        }
        // Log.debug("GL3DShape.hit: AABB is Hit!");

        // Transform ray to object space for non-groups
        if (!getClass().isAssignableFrom(GL3DGroup.class)) {
            ray.setOriginOS(this.wmI.multiply(ray.getOrigin()));
            ray.setDirOS(this.wmI.mat3().multiply(ray.getDirection()));
        }
        return this.shapeHit(ray);
    }

    public void delete(GL3DState state) {
        if (parent != null)
        {
            ((GL3DGroup) parent).removeNode(this);
        }
        parent = null;
        shapeDelete(state);
    }

    public abstract void shapeDelete(GL3DState state);

    public abstract boolean shapeHit(GL3DRay ray);

    public abstract void shapeInit(GL3DState state);

    public abstract void shapeDraw(GL3DState state);

    public abstract void shapeUpdate(GL3DState state);

    public Matrix4d modelView() {
        return this.mv;
    }

    public GL3DAABBox getAABBox() {
        return this.aabb;
    }

}
