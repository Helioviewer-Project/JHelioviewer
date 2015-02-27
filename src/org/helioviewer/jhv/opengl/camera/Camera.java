package org.helioviewer.jhv.opengl.camera;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.opengl.camera.newCamera.CameraAnimation;
import org.helioviewer.jhv.opengl.camera.newCamera.CameraListener;

public class Camera {
	public static final double MAX_DISTANCE = Constants.SUN_MEAN_DISTANCE_TO_EARTH * 1.8;
    public static final double MIN_DISTANCE = Constants.SUN_RADIUS * 1.2;
	public static final double DEFAULT_CAMERA_DISTANCE = 12 * Constants.SUN_RADIUS;

    private double clipNear = Constants.SUN_RADIUS / 10;
    private double fov = 10;
    private double aspect = 0.0;


    private Quaternion3d rotation;
    private Vector3d translation;
    
    private CameraInteraction zoomCameraInteraction;
    private CameraInteraction rotationInteraction;
    
    private Stack<CameraAnimation> cameraAnimations = new Stack<CameraAnimation>();
    private CopyOnWriteArrayList<CameraListener> cameraListeners = new CopyOnWriteArrayList<CameraListener>();
    
	private Timer animationTimer = new Timer(20, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			animate();
		}
	});

    
    public Camera() {
    	this.rotation = Quaternion3d.createRotation(0.0, new Vector3d(0, 1, 0));
    	this.translation = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);
    	zoomCameraInteraction = new CameraZoomInteraction(this);
    	rotationInteraction = new CameraRotationInteraction(this);
    	this.addCamera();
	}
    
    public void init(Component component){
    	component.addMouseWheelListener(zoomCameraInteraction);
    	//component.addMouseListener(panInteraction);
    	//component.addMouseMotionListener(panInteraction);
    	component.addMouseListener(rotationInteraction);
    	component.addMouseMotionListener(rotationInteraction);
    }
    
    private void addCamera(){
    }
    
    public Quaternion3d getRotation(){
    	return rotation;
    }
    
    public void setRotation(Quaternion3d rotation){
		this.rotation = rotation;
	}
	
    public Vector3d getTranslation() {
		return translation;
	}
	
	public void setTranslation(Vector3d translation){
		this.translation = translation;
	}
	
	public Matrix4d getTransformation(){
		Matrix4d transformation = this.rotation.toMatrix();
		System.out.println("scale : " + (1/Constants.SUN_RADIUS));
		transformation.addTranslation(translation);
		return transformation;
	}
	
	public Matrix4d getTransformation(Quaternion3d rotation){
		Quaternion3d newRotation = this.rotation.copy();
		newRotation.rotate(rotation);
		Matrix4d transformation = newRotation.toMatrix();
		System.out.println("scale : " + (1/Constants.SUN_RADIUS));
		transformation.addTranslation(translation);
		return transformation;
	}

	public double getClipNear() {
		return this.clipNear;
	}

	public double getFOV() {
		return this.fov;
	}

	public double getAspect() {
		return this.aspect;
	}
    
    public void addCameraAnimation(CameraAnimation animation) {
        for (Iterator<CameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
            CameraAnimation ani = iter.next();
            if (!ani.isFinished() && ani.getClass().isInstance(animation)) {
            	ani.updateWithAnimation(animation);
                return;
            }
        }
        this.cameraAnimations.add(animation);
        this.animationTimer.start();
    }
    
    private void animate(){
        for (Iterator<CameraAnimation> iter = this.cameraAnimations.iterator(); iter.hasNext();) {
        	System.out.println("animate Camera!!!");
            CameraAnimation animation = iter.next();
            if (!animation.isFinished()) {
                animation.animate(this);
            } else {
                iter.remove();
                if (cameraAnimations.isEmpty()){
                	animationTimer.stop();
                }
            }
        }
    }

    public void setZTranslation(double z) {
        this.translation = new Vector3d(
                this.translation.x,
                this.translation.y,
                Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, z)));
    }

    public void addCameraListener(CameraListener cameraListener){
    	this.cameraListeners.add(cameraListener);
    }
    
    public void fireCameraMoving(){
    	for(CameraListener cameraListener : cameraListeners){
    		cameraListener.cameraMoving();
    	}
    }
    
    public void fireCameraMoved(){
    	for(CameraListener cameraListener : cameraListeners){
    		cameraListener.cameraMoving();
    	}
    }
}
