package org.helioviewer.jhv.opengl.scenegraph;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh.GL3DMeshPrimitive;
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
    Vector3d minWS = new Vector3d();
    Vector3d maxWS = new Vector3d();
    

    Vector3d minOS = new Vector3d();
    Vector3d maxOS = new Vector3d();
    

    private GL3DBuffer vertexBuffer;
    private GL3DBuffer colorBuffer;
    private GL3DBuffer indexBuffer;

    public void fromOStoWS(Vector3d minOS, Vector3d maxOS, Matrix4d wm) {
        this.minWS=new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        this.maxWS=new Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
        this.minOS=minOS;
        this.maxOS=maxOS;

        Vector3d[] corners = new Vector3d[8];
        corners[0] = new Vector3d(minOS);
        corners[1] = new Vector3d(maxOS.x, minOS.y, minOS.z);
        corners[2] = new Vector3d(maxOS.x, minOS.y, maxOS.z);
        corners[3] = new Vector3d(minOS.x, minOS.y, maxOS.z);
        corners[4] = new Vector3d(maxOS.x, maxOS.y, minOS.z);
        corners[5] = new Vector3d(minOS.x, maxOS.y, minOS.z);
        corners[6] = new Vector3d(minOS.x, maxOS.y, maxOS.z);
        corners[7] = new Vector3d(maxOS);

        for(int i=0;i<corners.length;i++)
            corners[i] = wm.multiply(corners[i]);

        for (Vector3d corner : corners) {
            this.minWS = this.minWS.setMin(corner);
            this.maxWS = this.maxWS.setMax(corner);
        }
    }

    public void fromWStoOS(Vector3d minWS, Vector3d maxWS, Matrix4d wmI) {
        this.minOS=new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.maxOS=new Vector3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        this.minWS=minWS;
        this.maxWS=maxWS;

        Vector3d[] corners = new Vector3d[8];
        corners[0] = new Vector3d(minWS);
        corners[1] = new Vector3d(maxWS.x, minWS.y, minWS.z);
        corners[2] = new Vector3d(maxWS.x, minWS.y, maxWS.z);
        corners[3] = new Vector3d(minWS.x, minWS.y, maxWS.z);
        corners[4] = new Vector3d(maxWS.x, maxWS.y, minWS.z);
        corners[5] = new Vector3d(minWS.x, maxWS.y, minWS.z);
        corners[6] = new Vector3d(minWS.x, maxWS.y, maxWS.z);
        corners[7] = new Vector3d(maxWS);

        for(int i=0;i<corners.length;i++)
            corners[i]=wmI.multiply(corners[i]);

        for (Vector3d corner : corners) {
            this.minOS = this.minOS.setMin(corner);
            this.maxOS = this.maxOS.setMax(corner);
        }
    }

    public void drawOS(GL3DState state, Vector4d color) {
        this.draw(state, minOS, maxOS, color);
    }

    private void draw(GL3DState state, Vector3d minV, Vector3d maxV, Vector4d color) {
        if (this.indexBuffer == null) {

            Vector3d[] corners = new Vector3d[8];
            corners[0] = new Vector3d(minV);
            corners[1] = new Vector3d(maxV.x, minV.y, minV.z);
            corners[2] = new Vector3d(maxV.x, minV.y, maxV.z);
            corners[3] = new Vector3d(minV.x, minV.y, maxV.z);

            corners[4] = new Vector3d(minV.x, maxV.y, minV.z);
            corners[5] = new Vector3d(maxV.x, maxV.y, minV.z);
            corners[6] = new Vector3d(maxV.x, maxV.y, maxV.z);
            corners[7] = new Vector3d(minV.x, maxV.y, maxV.z);

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
            List<Vector4d> colors = new ArrayList<Vector4d>();
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
        Vector3d[] params = { this.minWS, this.maxWS };
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

    public void merge(GL3DAABBox bb)
    {
        minWS = minWS.setMin(bb.minWS);
        maxWS = maxWS.setMax(bb.maxWS);
    }

}
