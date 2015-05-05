package org.helioviewer.jhv.gui.controller;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;

public interface Camera {
	public Quaternion3d getRotation();
	public void setRotation(Quaternion3d rotation);
    public Vector3d getTranslation();
	public void setTranslation(Vector3d translation);
	public Matrix4d getTransformation();
	public Matrix4d getTransformation(Quaternion3d rotation);
	public void setZTranslation(double z);
}
