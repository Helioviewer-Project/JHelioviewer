package org.helioviewer.jhv.base.math;

public class CartesianCoord {

    public double x = 0.0;
    public double y = 0.0;
    public double z = 0.0;

    public CartesianCoord() {

    }

    public CartesianCoord(Vector3d vector) {
        x = vector.x;
        y = vector.y;
        z = vector.z;
    }

    public String toString() {
        return x + " " + y + " " + z;
    }

}
