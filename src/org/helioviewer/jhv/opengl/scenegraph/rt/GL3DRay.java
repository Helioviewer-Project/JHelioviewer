package org.helioviewer.jhv.opengl.scenegraph.rt;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.scenegraph.GL3DShape;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;

/**
 * A {@link GL3DRay} is used for detecting hit points within the scene graph and
 * stores required attributes for a fast ray casting. {@link GL3DRay}s should be
 * created by the {@link GL3DRayTracer}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DRay {
    private Vector3d origin;
    private Vector3d direction;
    private Vector3d originOS;
    private Vector3d directionOS;
    private double length;

    private GL3DShape originShape;

    private Vector3d hitPoint;
    private Vector3d hitPointOS;
    private Vector3d hitNormal;

    private Vector3d invDirection;
    private Vector3d invDirectionOS;
    private int[] sign = new int[3];
    private int[] signOS = new int[3];
    private double tmin;
    private double tmax;

    public boolean isOutside;

    public boolean isOnSun = false;

    public static GL3DRay createPrimaryRay(Vector3d origin, Vector3d dir) {
        GL3DRay ray = new GL3DRay();
        ray.origin = origin;
        dir=dir.normalize();
        ray.setDir(dir);
        ray.length = Double.MAX_VALUE;
        ray.isOutside = true;

        // ray.fromVStoWS(state);

        // Log.debug("GL3DRay.createPrimaryRay: Origin: "+ray.origin+" Direction: "+ray.direction);
        return ray;
    }

    private GL3DRay() {
    }

    public void setDirOS(Vector3d dir) {
        this.directionOS = dir;
        invDirectionOS = new Vector3d(dir.x == 0 ? 0 : 1 / dir.x, dir.y == 0 ? 0 : 1 / dir.y, dir.z == 0 ? 0 : 1 / dir.z);
        signOS[0] = invDirectionOS.x < 0 ? 1 : 0;
        signOS[1] = invDirectionOS.y < 0 ? 1 : 0;
        signOS[2] = invDirectionOS.z < 0 ? 1 : 0;
    }

    public void setDir(Vector3d dir) {
        this.direction = dir;
        invDirection = new Vector3d(dir.x == 0 ? 0 : 1 / dir.x, dir.y == 0 ? 0 : 1 / dir.y, dir.z == 0 ? 0 : 1 / dir.z);
        invDirection = new Vector3d(1 / dir.x, 1 / dir.y, 1 / dir.z);
        sign[0] = invDirection.x < 0 ? 1 : 0;
        sign[1] = invDirection.y < 0 ? 1 : 0;
        sign[2] = invDirection.z < 0 ? 1 : 0;
    }

    /*
     * public void fromVStoWS(GL3DState state) { //
     * Log.debug("GL3DRay: VS: Origin: " + this.origin + " Direction: " +
     * this.direction+" InvDir: "+this.invDirection); this.origin =
     * (state.getMVInverse().multiply(this.origin));
     * setDir(state.getMVInverse().mat3().multiply(this.direction)); //
     * Log.debug("GL3DRay: WS: Origin: " + this.origin + " Direction: " +
     * this.direction+" InvDir: "+this.invDirection); }
     */

    public void draw(GL3DState state) {
        GL2 gl = state.gl;

        this.length = this.length < 1000 ? this.length : 100;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        state.pushMV();
        // state.loadIdentity();
        gl.glBegin(GL.GL_LINES);

        gl.glColor3d(1, 1, 0);
        gl.glVertex3d(origin.x, origin.y, origin.z);

        gl.glColor3d(1, 0, 0);
        gl.glVertex3d(origin.x + direction.x * length, origin.y + direction.y * length, origin.z + direction.z * length);

        gl.glEnd();
        state.popMV();

        // Log.debug("GL3DRay: DRAW IN WS: Origin: "+this.origin+" Destination: "+Vector3d.add(this.origin,
        // this.direction.copy().multiply(length)));
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    public Vector3d getOrigin() {
        return origin;
    }

    public Vector3d getDirection() {
        return direction;
    }

    public Vector3d getOriginOS() {
        return originOS;
    }

    public Vector3d getDirectionOS() {
        return directionOS;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public GL3DShape getOriginShape() {
        return originShape;
    }

    public void setOriginShape(GL3DShape originShape) {
        this.originShape = originShape;
    }

    public Vector3d getHitPoint() {
        return hitPoint;
    }

    public void setHitPoint(Vector3d hitPoint) {
        this.hitPoint = hitPoint;
    }

    public Vector3d getHitNormal() {
        return hitNormal;
    }

    public void setHitNormal(Vector3d hitNormal) {
        this.hitNormal = hitNormal;
    }

    public Vector3d getInvDirection() {
        return invDirection;
    }

    public Vector3d getInvDirectionOS() {
        return invDirectionOS;
    }

    public int[] getSign() {
        return sign;
    }

    public int[] getSignOS() {
        return signOS;
    }

    public double getTmin() {
        return tmin;
    }

    public void setTmin(double tmin) {
        this.tmin = tmin;
    }

    public double getTmax() {
        return tmax;
    }

    public void setTmax(double tmax) {
        this.tmax = tmax;
    }

    public boolean isOutside() {
        return isOutside;
    }

    public void setOutside(boolean isOutside) {
        this.isOutside = isOutside;
    }

    public void setOriginOS(Vector3d originOS) {
        this.originOS = originOS;
    }

    public void setDirectionOS(Vector3d directionOS) {
        this.directionOS = directionOS;
    }

    public Vector3d getHitPointOS() {
        return hitPointOS;
    }

    public void setHitPointOS(Vector3d hitPointOS) {
        this.hitPointOS = hitPointOS;
    }
}
