package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.camera.animation.CameraAnimation;

public interface Camera {
	public Quaternion3d getRotation();
	public void setRotation(Quaternion3d rotation);
    public Vector3d getTranslation();
	public void setTranslation(Vector3d translation);
	public Matrix4d getTransformation();
	public void setZTranslation(double z);
	public void addCameraAnimation(CameraAnimation cameraAnimation);
	public double getAspect();
	public void setTransformation(Quaternion3d rotation, Vector3d translation);
	public void repaintMain(long millis);
}
