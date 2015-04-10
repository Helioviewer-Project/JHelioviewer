package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;

/**
 * The GL3DCamera is resposible for the view space transformation. It sets up
 * the perspective and is generates the view space transformation. This
 * transformation is in turn influenced by the user interaction. Different
 * styles of user interaction are supported. These interactions are encapsuled
 * in {@link GL3DInteraction} objects that can be selected in the main toolbar.
 * The interactions then change the rotation and translation fields out of which
 * the resulting cameraTransformation is generated.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DCamera {
    public static final double MAX_DISTANCE = -Constants.SUN_MEAN_DISTANCE_TO_EARTH * 1.8;
    public static final double MIN_DISTANCE = -Constants.SUN_RADIUS * 1.2;

    private double clipNear = Constants.SUN_RADIUS / 10;
    private double clipFar = Constants.SUN_RADIUS * 1000;
    private double fov = 10;
    private double aspect = 0.0;
    private double width = 0.0;
    private double height = 0.0;

    private List<GL3DCameraListener> listeners = new ArrayList<GL3DCameraListener>();

    // This is the resulting cameraTransformation. All interactions should
    // modify this matrix
    private Matrix4d cameraTransformation;

    private Quaternion3d rotation;
    protected Vector3d translation;

    private Stack<GL3DCameraAnimation> cameraAnimations = new Stack<GL3DCameraAnimation>();

    public GL3DCamera(double clipNear, double clipFar) {
        this();
        this.clipNear = clipNear;
        this.clipFar = clipFar;
    }

    public GL3DCamera() {
        this.cameraTransformation = Matrix4d.identity();
        this.rotation = Quaternion3d.createRotation(0.0, new Vector3d(0, 1, 0));
        this.translation = new Vector3d();
    }

    public abstract void reset();

    /**
     * This method is called when the camera changes and should copy the
     * required settings of the preceding camera objects.
     * 
     * @param precedingCamera
     */
    public void activate(GL3DCamera precedingCamera) {
        if (precedingCamera != null) {
            this.rotation = precedingCamera.getRotation().copy();
            this.translation = precedingCamera.translation;
            this.width = precedingCamera.width;
            this.height = precedingCamera.height;
            this.updateCameraTransformation();

            // Also set the correct interaction
            if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getRotateInteraction())) {
                this.setCurrentInteraction(this.getRotateInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getPanInteraction())) {
                this.setCurrentInteraction(this.getPanInteraction());
            } else if (precedingCamera.getCurrentInteraction().equals(precedingCamera.getZoomBoxInteraction())) {
                this.setCurrentInteraction(this.getZoomBoxInteraction());
            }
        } else {
            System.out.println("GL3DCamera: No Preceding Camera, resetting Camera");
            //this.reset();
        }
    }

    protected void setZTranslation(double z) {
        this.translation = new Vector3d(
                this.translation.x,
                this.translation.y,
                Math.min(MIN_DISTANCE, Math.max(MAX_DISTANCE, z)));
    }

    protected void addPanning(double x, double y) {
        setPanning(this.translation.x + x, this.translation.y + y);
    }

    public void setPanning(double x, double y) {
        this.translation = new Vector3d(x,y,this.translation.z);
    }

    public Vector3d getTranslation() {
        return this.translation;
    }

    public Matrix4d getCameraTransformation() {
        return this.cameraTransformation;
    }

    public double getZTranslation() {
        return getTranslation().z;
    }

    public Quaternion3d getRotation() {
        return this.rotation;
    }

    public void deactivate() {
        this.cameraAnimations.clear();
    }

    public void applyPerspective(GL3DState state) {
        GL2 gl = state.gl.getGL2();
        int viewport[] = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        this.width = (double) viewport[2];
        this.height = (double) viewport[3];
        this.aspect = width / height;

        //gl.glMatrixMode(GL2.GL_PROJECTION);

        //gl.glPushMatrix();
        //gl.glLoadIdentity();
        //glu.gluPerspective(this.fov, this.aspect, this.clipNear, this.clipFar);

        //gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void resumePerspective(GL3DState state) {
        GL2 gl = state.gl.getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public void updateCameraTransformation() {
        this.updateCameraTransformation(true);
    }

    /**
     * Updates the camera transformation by applying the rotation and
     * translation information.
     */
    public void updateCameraTransformation(boolean fireEvent) {
        cameraTransformation = Matrix4d.identity();
        cameraTransformation.translate(this.translation);
        cameraTransformation.multiply(this.rotation.toMatrix());

        if (fireEvent) {
            fireCameraMoved();
        }
    }

    public void applyCamera(GL3DState state) {
        for (Iterator<GL3DCameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
            GL3DCameraAnimation animation = iter.next();
            if (!animation.isFinished()) {
                animation.animate(this);
            } else {
                iter.remove();
            }
        }
        state.multiplyMV(cameraTransformation);
    }

    public void addCameraAnimation(GL3DCameraAnimation animation) {
        for (Iterator<GL3DCameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
            GL3DCameraAnimation ani = iter.next();
            if (!ani.isFinished() && ani.getClass().isInstance(animation)) {
            	ani.updateWithAnimation(animation);
                return;
            }
        }

        this.cameraAnimations.add(animation);
    }

    public abstract Matrix4d getVM();

    public abstract double getDistanceToSunSurface();

    public abstract GL3DInteraction getPanInteraction();

    public abstract GL3DInteraction getRotateInteraction();

    public abstract GL3DInteraction getZoomBoxInteraction();

    public abstract String getName();

    public void drawCamera(GL3DState state) {
        getCurrentInteraction().drawInteractionFeedback(state, this);
    }

    public abstract GL3DInteraction getCurrentInteraction();

    public abstract void setCurrentInteraction(GL3DInteraction currentInteraction);

    public double getFOV() {
        return this.fov;
    }

    public double getClipNear() {
        return clipNear;
    }

    public double getClipFar() {
        return clipFar;
    }

    public double getAspect() {
        return aspect;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public String toString() {
        return getName();
    }

    public void addCameraListener(GL3DCameraListener listener) {
        this.listeners.add(listener);
    }

    public void removeCameraListener(GL3DCameraListener listener) {
        this.listeners.remove(listener);
    }

    protected void fireCameraMoved() {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoved(this);
        }
    }

    protected void fireCameraMoving() {
        for (GL3DCameraListener l : this.listeners) {
            l.cameraMoving(this);
        }
    }

    public abstract CoordinateSystem getViewSpaceCoordinateSystem();

	public abstract void setTrack(boolean selected);
	public abstract boolean isTrack();

	abstract public void mouseRay(MouseEvent e);

	public abstract GL3DRay getLastMouseRay();
	}
