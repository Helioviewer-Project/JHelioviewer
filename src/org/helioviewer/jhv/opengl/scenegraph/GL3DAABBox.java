package org.helioviewer.jhv.opengl.scenegraph;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh.GL3DMeshPrimitive;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DMat4d;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DVec4d;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;

/**
 * The axis aligned bounding box is used as an acceleration structure for
 * intersection tests with Graph nodes. The Box is built by using the maximal
 * coordinates in each dimensions
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DAABBox {
    GL3DVec3d minWS = new GL3DVec3d();
    GL3DVec3d maxWS = new GL3DVec3d();
    

    GL3DVec3d minOS = new GL3DVec3d();
    GL3DVec3d maxOS = new GL3DVec3d();
    

    private GL3DBuffer vertexBuffer;
    private GL3DBuffer colorBuffer;
    private GL3DBuffer indexBuffer;

    public void fromOStoWS(GL3DVec3d minOS, GL3DVec3d maxOS, GL3DMat4d wm) {
        this.minWS.set(new GL3DVec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
        this.maxWS.set(new GL3DVec3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        this.minOS.set(minOS);
        this.maxOS.set(maxOS);

        GL3DVec3d[] corners = new GL3DVec3d[8];
        corners[0] = new GL3DVec3d(minOS);
        corners[1] = new GL3DVec3d(maxOS.x, minOS.y, minOS.z);
        corners[2] = new GL3DVec3d(maxOS.x, minOS.y, maxOS.z);
        corners[3] = new GL3DVec3d(minOS.x, minOS.y, maxOS.z);
        corners[4] = new GL3DVec3d(maxOS.x, maxOS.y, minOS.z);
        corners[5] = new GL3DVec3d(minOS.x, maxOS.y, minOS.z);
        corners[6] = new GL3DVec3d(minOS.x, maxOS.y, maxOS.z);
        corners[7] = new GL3DVec3d(maxOS);

        for (GL3DVec3d corner : corners) {
            corner.set(wm.multiply(corner));
        }

        for (GL3DVec3d corner : corners) {
            this.minWS.setMin(corner);
            this.maxWS.setMax(corner);
        }
    }

    public void fromWStoOS(GL3DVec3d minWS, GL3DVec3d maxWS, GL3DMat4d wmI) {
        this.minOS.set(new GL3DVec3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
        this.maxOS.set(new GL3DVec3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        this.minWS.set(minWS);
        this.maxWS.set(maxWS);

        GL3DVec3d[] corners = new GL3DVec3d[8];
        corners[0] = new GL3DVec3d(minWS);
        corners[1] = new GL3DVec3d(maxWS.x, minWS.y, minWS.z);
        corners[2] = new GL3DVec3d(maxWS.x, minWS.y, maxWS.z);
        corners[3] = new GL3DVec3d(minWS.x, minWS.y, maxWS.z);
        corners[4] = new GL3DVec3d(maxWS.x, maxWS.y, minWS.z);
        corners[5] = new GL3DVec3d(minWS.x, maxWS.y, minWS.z);
        corners[6] = new GL3DVec3d(minWS.x, maxWS.y, maxWS.z);
        corners[7] = new GL3DVec3d(maxWS);

        for (GL3DVec3d corner : corners) {
            corner.set(wmI.multiply(corner));
        }

        for (GL3DVec3d corner : corners) {
            this.minOS.setMin(corner);
            this.maxOS.setMax(corner);
        }
    }

    public void drawOS(GL3DState state, GL3DVec4d color) {
        this.draw(state, minOS, maxOS, color);
    }

    private void draw(GL3DState state, GL3DVec3d minV, GL3DVec3d maxV, GL3DVec4d color) {
        if (this.indexBuffer == null) {

            GL3DVec3d[] corners = new GL3DVec3d[8];
            corners[0] = new GL3DVec3d(minV);
            corners[1] = new GL3DVec3d(maxV.x, minV.y, minV.z);
            corners[2] = new GL3DVec3d(maxV.x, minV.y, maxV.z);
            corners[3] = new GL3DVec3d(minV.x, minV.y, maxV.z);

            corners[4] = new GL3DVec3d(minV.x, maxV.y, minV.z);
            corners[5] = new GL3DVec3d(maxV.x, maxV.y, minV.z);
            corners[6] = new GL3DVec3d(maxV.x, maxV.y, maxV.z);
            corners[7] = new GL3DVec3d(minV.x, maxV.y, maxV.z);

            int[] lines = new int[24];
            lines[0] = 0;
            lines[1] = 1;
            lines[2] = 1;
            lines[3] = 2;
            lines[4] = 2;
            lines[5] = 3;
            lines[6] = 3;
            lines[7] = 0;
            lines[8] = 4;
            lines[9] = 5;
            lines[10] = 5;
            lines[11] = 6;
            lines[12] = 6;
            lines[13] = 7;
            lines[14] = 7;
            lines[15] = 4;
            lines[16] = 0;
            lines[17] = 4;
            lines[18] = 1;
            lines[19] = 5;
            lines[20] = 2;
            lines[21] = 6;
            lines[22] = 3;
            lines[23] = 7;
            List<GL3DVec4d> colors = new ArrayList<GL3DVec4d>();
            for (int i = 0; i < 8; i++) {
                colors.add(color);
            }

            vertexBuffer = GL3DBuffer.createPositionBuffer(state, corners);
            indexBuffer = GL3DBuffer.createIndexBuffer(state, lines);
            colorBuffer = GL3DBuffer.createColorBuffer(state, colors);
        }

        this.vertexBuffer.enable(state);
        this.indexBuffer.enable(state);
        this.colorBuffer.enable(state);

        // state.gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE,
        // this.diffuseMaterial, 0);

        // state.gl.glColor4d(color.x, color.y, color.z, color.w);
        GL3DMeshPrimitive primitive = GL3DMeshPrimitive.LINES;
        state.gl.glDisable(GL2.GL_LIGHTING);
        state.gl.glDrawElements(primitive.id, this.indexBuffer.numberOfElements, this.indexBuffer.dataType.id, 0);
        state.gl.glEnable(GL2.GL_LIGHTING);

        this.vertexBuffer.disable(state);
        this.colorBuffer.disable(state);
        this.indexBuffer.disable(state);
    }

    public boolean isHitInWS(GL3DRay ray) {
        GL3DVec3d[] params = { this.minWS, this.maxWS };
        double tymin, tymax, tzmin, tzmax;
        ray.setTmin((params[ray.getSign()[0]].x - ray.getOrigin().x) * ray.getInvDirection().x);
        ray.setTmax((params[1 - ray.getSign()[0]].x - ray.getOrigin().x) * ray.getInvDirection().x);
        tymin = (params[ray.getSign()[1]].y - ray.getOrigin().y) * ray.getInvDirection().y;
        tymax = (params[1 - ray.getSign()[1]].y - ray.getOrigin().y) * ray.getInvDirection().y;

        if ((ray.getTmin() > tymax) || (tymin > ray.getTmax()))
            return false;
        if (tymin > ray.getTmin())
            ray.setTmin(tymin);
        if (tymax < ray.getTmax())
            ray.setTmax(tymax);

        tzmin = (params[ray.getSign()[2]].z - ray.getOrigin().z) * ray.getInvDirection().z;
        tzmax = (params[1 - ray.getSign()[2]].z - ray.getOrigin().z) * ray.getInvDirection().z;

        if ((ray.getTmin() > tzmax) || (tzmin > ray.getTmax()))
            return false;
        if (tzmin > ray.getTmin())
            ray.setTmin(tzmin);
        if (tzmax < ray.getTmax())
            ray.setTmax(tzmax);

        return ((ray.getTmin() < ray.getLength()) && (ray.getTmax() > 0));
    }

    public void merge(GL3DAABBox bb) {
        minWS.setMin(bb.minWS);
        maxWS.setMax(bb.maxWS);
    }

}
