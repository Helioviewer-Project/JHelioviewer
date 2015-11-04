package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.camera.animation.CameraAnimation;

public interface Camera
{
	public Quaternion getRotationCurrent();
	public Quaternion getRotationEnd();
	public void setRotationCurrent(Quaternion rotation);
	public void setRotationEnd(Quaternion rotation);
    public Vector3d getTranslationCurrent();
    public Vector3d getTranslationEnd();
	public void setTranslationCurrent(Vector3d translation);
	public void setTranslationEnd(Vector3d _translationEnd);
	public Matrix4d getTransformation();
	public void addCameraAnimation(CameraAnimation cameraAnimation);
	public double getAspect();
	public void repaint();
	public void stopAllAnimations();
}
