package org.helioviewer.jhv.opengl.camera;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.camera.animation.CameraAnimation;

public interface Camera
{
	Quaternion getRotationCurrent();
	Quaternion getRotationEnd();
	void setRotationCurrent(Quaternion rotation);
	void setRotationEnd(Quaternion rotation);
    Vector3d getTranslationCurrent();
    Vector3d getTranslationEnd();
	void setTranslationCurrent(Vector3d translation);
	void setTranslationEnd(Vector3d _translationEnd);
	void addCameraAnimation(CameraAnimation cameraAnimation);
	double getAspect();
	void repaint();
	void abortAllAnimations();
}
