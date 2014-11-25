package org.helioviewer.jhv.opengl.scenegraph.rt;

import java.awt.Dimension;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.scenegraph.GL3DNode;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;

/**
 * The {@link GL3DRayTracer} can be used to cast {@link GL3DRay}s through the
 * scene graph. To be able to create a Ray a reference to a camera is required.
 * Also, the rayTracer does necessarily need to traverse the whole scene graph,
 * also subnodes can be used as root nodes for the Ray Tracer.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DRayTracer {

	private GL3DNode sceneRoot;
	private GL3DCamera camera;

	double hh;
	double hw;
	double pixelSize;

	public GL3DRayTracer(GL3DNode sceneRoot, GL3DCamera camera) {
		this.sceneRoot = sceneRoot;
		this.camera = camera;
		hh = Math.tan(Math.toRadians(camera.getFOV() / 2))
				* camera.getClipNear();
		hw = hh * camera.getAspect();
		pixelSize = hw / camera.getWidth() * 2;
	}

	public synchronized GL3DRay cast(int pixelX, int pixelY) {
		GL3DRay ray = createPrimaryRay(this.camera, pixelX, pixelY);

		// isOutside flag set to true if the ray hit no object in the scene
		ray.isOutside = !this.sceneRoot.hit(ray);
		return ray;
	}

	private GL3DRay createPrimaryRay(GL3DCamera camera, int x, int y) {
		GL3DRay ray;
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D) {
			Matrix4d VM = camera.getVM();
			Matrix4d invRot = new Matrix4d(VM);
            invRot.setTranslation(0, 0, 0); // remove the translation
            invRot.transpose(); // transpose it to get inverse rot.
            Vector3d EYE=invRot.multiply(VM.translation().negate()); // setMatrix eye
            Vector3d LR=new Vector3d(VM.m[0], VM.m[4], VM.m[8]); // normalized look right vector
            Vector3d LU=new Vector3d(VM.m[1], VM.m[5], VM.m[9]); // normalized look up vector
            Vector3d LA=new Vector3d(-VM.m[2], -VM.m[6], -VM.m[10]); // normalized look at vector
			LA=LA.normalize();
			LU=LU.normalize();
			LR=LR.normalize();
			Vector3d C = LA.scale(camera.getClipNear());
			Vector3d TL = C.subtract(LR.scale(hw)).add(
					LU.scale(hh));
			Vector3d dir = TL.add(
					LR.scale((double)x).subtract(LU.scale((double)y)).scale(pixelSize));
			ray = GL3DRay.createPrimaryRay(EYE, dir);
		} else {
		    
		    Matrix4d cameraRotation = camera.getRotation().toMatrix();
		    
			Vector3d dir = cameraRotation.multiply(new Vector3d(0, 0, -1.0));
			Vector3d up = cameraRotation.multiply(new Vector3d(0, -1.0, 0));
			Vector3d right = cameraRotation.multiply(new Vector3d(1.0, 0, 0));
			Vector3d eye = cameraRotation.multiply(new Vector3d(camera.getTranslation()
					.negate()));
			
			double height = camera.getZTranslation()
					* Math.tanh(Math.toRadians(camera.getFOV()));
			Dimension dimension = GuiState3DWCS.mainComponentView
					.getCanavasSize();
			double unitPerPixel = height / dimension.getHeight();
			
			ray = GL3DRay.createPrimaryRay(
			        eye.add(
			                right.scale(unitPerPixel * (x - dimension.getWidth() / 2.0))
			        )
			        .add(up.scale(unitPerPixel * (y - dimension.getHeight() / 2.0)))
			      , dir);
		}
		return ray;
	}

}
