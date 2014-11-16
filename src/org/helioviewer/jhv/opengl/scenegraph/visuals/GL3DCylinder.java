package org.helioviewer.jhv.opengl.scenegraph.visuals;

import java.util.List;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;

public class GL3DCylinder extends GL3DMesh {

    private int detail;
    private double radius;
    private double height;

    private Vector4d color;

    public GL3DCylinder(double radius, double height, int detail, Vector4d color) {
        this("Cylinder", radius, height, detail, color);
    }

    public GL3DCylinder(String name, double radius, double height, int detail, Vector4d color) {
        super(name, new Vector4d(1, 1, 1, 1));
        this.color = new Vector4d(color.x, color.y, color.z, color.w);
        this.radius = radius;
        this.detail = detail;
        this.height = height;
        if (detail < 3) {
            throw new IllegalArgumentException("Cylinder must have at least detail=3!");
        }
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<Vector3d> positions, List<Vector3d> normals, List<Vector2d> textCoords, List<Integer> indices, List<Vector4d> colors) {
        double dPhi = 2 * Math.PI / this.detail;
        for (int i = 0; i < this.detail; i++) {
            double phi = i * dPhi;
            double nx = Math.cos(phi);
            double ny = Math.sin(phi);
            double nz = 0;

            double x = nx * radius;
            double y = ny * radius;
            double z = this.height / 2;

            positions.add(new Vector3d(x, y, z));
            normals.add(new Vector3d(nx, ny, nz));
            colors.add(this.color);

            positions.add(new Vector3d(x, y, -z));
            normals.add(new Vector3d(nx, ny, nz));
            colors.add(this.color);

            indices.add(i * 2);
            indices.add(i * 2 + 1);
        }
        indices.add(0);
        indices.add(1);

        return GL3DMeshPrimitive.TRIANGLE_STRIP;
    }
}
