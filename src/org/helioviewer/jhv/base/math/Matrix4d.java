package org.helioviewer.jhv.base.math;

public class Matrix4d {
    /**
     * 0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15
     */
    public double[] m = new double[16];

    public Matrix4d(double M0, double M4, double M8, double M12, double M1, double M5, double M9, double M13, double M2, double M6, double M10, double M14, double M3, double M7, double M11, double M15) {
        m[0] = M0;
        m[4] = M4;
        m[8] = M8;
        m[12] = M12;
        m[1] = M1;
        m[5] = M5;
        m[9] = M9;
        m[13] = M13;
        m[2] = M2;
        m[6] = M6;
        m[10] = M10;
        m[14] = M14;
        m[3] = M3;
        m[7] = M7;
        m[11] = M11;
        m[15] = M15;
    }

    public Matrix4d() {
    }

    public Matrix4d(Matrix4d mat) {
        set(mat);
    }

    public void setIdentity() {
        this.set(Matrix4d.identity());
    }

    public static Matrix4d identity() {
        return new Matrix4d(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f);
    }

    public Matrix4d set(Matrix4d A) {
        m[0] = A.m[0];
        m[4] = A.m[4];
        m[8] = A.m[8];
        m[12] = A.m[12];
        m[1] = A.m[1];
        m[5] = A.m[5];
        m[9] = A.m[9];
        m[13] = A.m[13];
        m[2] = A.m[2];
        m[6] = A.m[6];
        m[10] = A.m[10];
        m[14] = A.m[14];
        m[3] = A.m[3];
        m[7] = A.m[7];
        m[11] = A.m[11];
        m[15] = A.m[15];

        return this;
    }

    public Matrix4d set(double M0, double M4, double M8, double M12, double M1, double M5, double M9, double M13, double M2, double M6, double M10, double M14, double M3, double M7, double M11, double M15) {
        m[0] = M0;
        m[4] = M4;
        m[8] = M8;
        m[12] = M12;
        m[1] = M1;
        m[5] = M5;
        m[9] = M9;
        m[13] = M13;
        m[2] = M2;
        m[6] = M6;
        m[10] = M10;
        m[14] = M14;
        m[3] = M3;
        m[7] = M7;
        m[11] = M11;
        m[15] = M15;

        return this;
    }

    public Matrix4d set(int index, double f) {
        if (index < 0 || index > 15)
            throw new IndexOutOfBoundsException("Mat4 has 16 fields");

        m[index] = f;
        return this;
    }

    public double get(int index) {
        if (index < 0 || index > 15)
            throw new IndexOutOfBoundsException("Mat4 has 16 fields");

        return m[index];
    }

    public Matrix4d multiply(Matrix4d A) {
        set(m[0] * A.m[0] + m[4] * A.m[1] + m[8] * A.m[2] + m[12] * A.m[3], // row
                                                                            // 1
                m[0] * A.m[4] + m[4] * A.m[5] + m[8] * A.m[6] + m[12] * A.m[7], m[0] * A.m[8] + m[4] * A.m[9] + m[8] * A.m[10] + m[12] * A.m[11], m[0] * A.m[12] + m[4] * A.m[13] + m[8] * A.m[14] + m[12] * A.m[15],

                m[1] * A.m[0] + m[5] * A.m[1] + m[9] * A.m[2] + m[13] * A.m[3], // row
                                                                                // 2
                m[1] * A.m[4] + m[5] * A.m[5] + m[9] * A.m[6] + m[13] * A.m[7], m[1] * A.m[8] + m[5] * A.m[9] + m[9] * A.m[10] + m[13] * A.m[11], m[1] * A.m[12] + m[5] * A.m[13] + m[9] * A.m[14] + m[13] * A.m[15],

                m[2] * A.m[0] + m[6] * A.m[1] + m[10] * A.m[2] + m[14] * A.m[3], // row
                                                                                 // 3
                m[2] * A.m[4] + m[6] * A.m[5] + m[10] * A.m[6] + m[14] * A.m[7], m[2] * A.m[8] + m[6] * A.m[9] + m[10] * A.m[10] + m[14] * A.m[11], m[2] * A.m[12] + m[6] * A.m[13] + m[10] * A.m[14] + m[14] * A.m[15],

                m[3] * A.m[0] + m[7] * A.m[1] + m[11] * A.m[2] + m[15] * A.m[3], // row
                                                                                 // 4
                m[3] * A.m[4] + m[7] * A.m[5] + m[11] * A.m[6] + m[15] * A.m[7], m[3] * A.m[8] + m[7] * A.m[9] + m[11] * A.m[10] + m[15] * A.m[11], m[3] * A.m[12] + m[7] * A.m[13] + m[11] * A.m[14] + m[15] * A.m[15]);
        return this;
    }

    public Vector3d multiply(Vector3d v)
    {
        double W = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15];
        return new Vector3d((m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12]) / W, (m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13]) / W, (m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]) / W);
    }

    public Vector4d multiply(Vector4d v)
    {
        return new Vector4d(m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12] * v.w, m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13] * v.w, m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * v.w, m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * v.w);
    }
    
    public Matrix3d rotation(){
    	return new Matrix3d(m[0], m[1], m[2], m[4], m[5], m[6], m[8], m[9], m[10]);
    }
    
    public void setRotation(Matrix3d rotation){
    	m[0] = rotation.m[0];
    	m[1] = rotation.m[1];
    	m[2] = rotation.m[3];
    	m[4] = rotation.m[4];
    	m[5] = rotation.m[5];
    	m[6] = rotation.m[6];
    	m[8] = rotation.m[7];
    	m[9] = rotation.m[8];
    	m[10] = rotation.m[9];
    }

    public Vector3d translation() {
        return new Vector3d(m[12], m[13], m[14]);
    }

    public void setTranslation(double x, double y, double z) {
        m[12] = x;
        m[13] = y;
        m[14] = z;
    }
    
    public void addTranslation(Vector3d translation){
    	m[12] += translation.x;
    	m[13] += translation.y;
    	m[14] += translation.z;
    }

    public Matrix4d inverse() {
        Matrix4d inverse = new Matrix4d();
        // Cache the matrix values (makes for huge speed increases!)
        double a00 = this.m[0], a01 = this.m[1], a02 = this.m[2], a03 = this.m[3];
        double a10 = this.m[4], a11 = this.m[5], a12 = this.m[6], a13 = this.m[7];
        double a20 = this.m[8], a21 = this.m[9], a22 = this.m[10], a23 = this.m[11];
        double a30 = this.m[12], a31 = this.m[13], a32 = this.m[14], a33 = this.m[15];

        double b00 = a00 * a11 - a01 * a10;
        double b01 = a00 * a12 - a02 * a10;
        double b02 = a00 * a13 - a03 * a10;
        double b03 = a01 * a12 - a02 * a11;
        double b04 = a01 * a13 - a03 * a11;
        double b05 = a02 * a13 - a03 * a12;
        double b06 = a20 * a31 - a21 * a30;
        double b07 = a20 * a32 - a22 * a30;
        double b08 = a20 * a33 - a23 * a30;
        double b09 = a21 * a32 - a22 * a31;
        double b10 = a21 * a33 - a23 * a31;
        double b11 = a22 * a33 - a23 * a32;

        // Calculate the determinant (inlined to avoid double-caching)
        double invDet = 1 / (b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06);

        inverse.m[0] = (a11 * b11 - a12 * b10 + a13 * b09) * invDet;
        inverse.m[1] = (-a01 * b11 + a02 * b10 - a03 * b09) * invDet;
        inverse.m[2] = (a31 * b05 - a32 * b04 + a33 * b03) * invDet;
        inverse.m[3] = (-a21 * b05 + a22 * b04 - a23 * b03) * invDet;
        inverse.m[4] = (-a10 * b11 + a12 * b08 - a13 * b07) * invDet;
        inverse.m[5] = (a00 * b11 - a02 * b08 + a03 * b07) * invDet;
        inverse.m[6] = (-a30 * b05 + a32 * b02 - a33 * b01) * invDet;
        inverse.m[7] = (a20 * b05 - a22 * b02 + a23 * b01) * invDet;
        inverse.m[8] = (a10 * b10 - a11 * b08 + a13 * b06) * invDet;
        inverse.m[9] = (-a00 * b10 + a01 * b08 - a03 * b06) * invDet;
        inverse.m[10] = (a30 * b04 - a31 * b02 + a33 * b00) * invDet;
        inverse.m[11] = (-a20 * b04 + a21 * b02 - a23 * b00) * invDet;
        inverse.m[12] = (-a10 * b09 + a11 * b07 - a12 * b06) * invDet;
        inverse.m[13] = (a00 * b09 - a01 * b07 + a02 * b06) * invDet;
        inverse.m[14] = (-a30 * b03 + a31 * b01 - a32 * b00) * invDet;
        inverse.m[15] = (a20 * b03 - a21 * b01 + a22 * b00) * invDet;

        return inverse;
    }

    //
    // public GL3DMat4d inverse() {
    // GL3DMat4d I = new GL3DMat4d();
    //
    // // Code from Mesa-2.2\src\glu\project.c
    // double det, d12, d13, d23, d24, d34, d41;
    //
    // // Inverse = adjoint / det. (See linear algebra texts.)
    // // pre-compute 2x2 dets for last two rows when computing
    // // cofactors of first two rows.
    // d12 = (m[2] * m[7] - m[3] * m[6]);
    // d13 = (m[2] * m[11] - m[3] * m[10]);
    // d23 = (m[6] * m[11] - m[7] * m[10]);
    // d24 = (m[6] * m[15] - m[7] * m[14]);
    // d34 = (m[10] * m[15] - m[11] * m[14]);
    // d41 = (m[14] * m[3] - m[15] * m[2]);
    //
    // I.m[0] = (m[5] * d34 - m[9] * d24 + m[13] * d23);
    // I.m[1] = -(m[1] * d34 + m[9] * d41 + m[13] * d13);
    // I.m[2] = (m[1] * d24 + m[5] * d41 + m[13] * d12);
    // I.m[3] = -(m[1] * d23 - m[5] * d13 + m[9] * d12);
    //
    // // Compute determinant as early as possible using these cof_actors.
    // det = m[0] * I.m[0] + m[4] * I.m[1] + m[8] * I.m[2] + m[12] * I.m[3];
    //
    // // Run singularity test.
    // if (Math.abs(det) <= 0.0000005) {
    // // throw new IllegalArgumentException(
    // // "Matrix is singular. Inversion impossible.");
    // Log.error("Matrix is singular. Inversion impossible. Matrix:\n"+this.toString());
    // } else {
    // double invDet = 1 / det;
    // // Compute rest of inverse.
    // I.m[0] *= invDet;
    // I.m[1] *= invDet;
    // I.m[2] *= invDet;
    // I.m[3] *= invDet;
    //
    // I.m[4] = -(m[4] * d34 - m[8] * d24 + m[12] * d23) * invDet;
    // I.m[5] = (m[0] * d34 + m[8] * d41 + m[12] * d13) * invDet;
    // I.m[6] = -(m[0] * d24 + m[4] * d41 + m[12] * d12) * invDet;
    // I.m[7] = (m[0] * d23 - m[4] * d13 + m[8] * d12) * invDet;
    //
    // // Pre-compute 2x2 dets for first two rows when computing
    // // cofactors of last two rows.
    // d12 = m[0] * m[5] - m[1] * m[4];
    // d13 = m[0] * m[9] - m[1] * m[8];
    // d23 = m[4] * m[9] - m[5] * m[8];
    // d24 = m[4] * m[13] - m[5] * m[12];
    // d34 = m[8] * m[13] - m[9] * m[12];
    // d41 = m[12] * m[1] - m[13] * m[0];
    //
    // I.m[8] = (m[7] * d34 - m[11] * d24 + m[15] * d23) * invDet;
    // I.m[9] = -(m[3] * d34 + m[11] * d41 + m[15] * d13) * invDet;
    // I.m[10] = (m[3] * d24 + m[7] * d41 + m[15] * d12) * invDet;
    // I.m[11] = -(m[3] * d23 - m[7] * d13 + m[11] * d12) * invDet;
    // I.m[12] = -(m[6] * d34 - m[10] * d24 + m[14] * d23) * invDet;
    // I.m[13] = (m[2] * d34 + m[10] * d41 + m[14] * d13) * invDet;
    // I.m[14] = -(m[2] * d24 + m[6] * d41 + m[14] * d12) * invDet;
    // I.m[15] = (m[2] * d23 - m[6] * d13 + m[10] * d12) * invDet;
    // }
    // return I;
    // }

    public Matrix4d translate(Vector3d t) {
        return this.multiply(Matrix4d.translation(t));
    }

    public Matrix4d translate(double x, double y, double z) {
        return this.multiply(Matrix4d.translation(new Vector3d(x, y, z)));
    }

    public Matrix4d rotate(double angle, Vector3d axis) {
        return this.rotate(angle, axis.x, axis.y, axis.z);
    }

    public Matrix4d rotate(double angle, double axisx, double axisy, double axisz) {
        return this.multiply(Matrix4d.rotation(angle, axisx, axisy, axisz));
    }

    public Matrix4d scale(Vector3d s) {
        return this.scale(s.x, s.y, s.z);
    }

    public Matrix4d scale(double sx, double sy, double sz) {
        return this.multiply(Matrix4d.scaling(sx, sy, sz));
    }

    public Matrix4d invert() {
        return this.set(this.inverse());
    }

    public Matrix4d transpose() {
        return swap(1, 4).swap(2, 8).swap(6, 9).swap(3, 12).swap(7, 13).swap(11, 14);
    }

    public Matrix4d swap(int i1, int i2) {
        double temp = get(i1);
        set(i1, get(i2));
        set(i2, temp);
        return this;
    }

    public void posAtUp(Vector3d pos) {
        this.posAtUp(pos, new Vector3d(), new Vector3d());
    }

    public void posAtUp(Vector3d pos, Vector3d dirAt, Vector3d dirUp) {
        lightAt(pos, dirAt, dirUp);
    }

    public void lightAt(Vector3d pos, Vector3d dirAt, Vector3d dirUp) {
        Vector3d VX;
        Vector3d VY;
        Vector3d VZ;

        Matrix3d xz = new Matrix3d(0f, 0f, 1f, 0f, 0f, 0f, -1f, 0f, 0f);

        VZ = pos.subtract(dirAt);
        if (dirUp.isApproxEqual(Vector3d.ZERO, 0f)) {
            VX = xz.multiply(VZ).normalize();
            VY = VZ.cross(VX).normalize();
        } else {
            VX = dirUp.cross(VZ).normalize();
            VY = VZ.cross(VX).normalize();
        }

        set(VX.x, VY.x, VZ.x, pos.x, VX.y, VY.y, VZ.y, pos.y, VX.z, VY.z, VZ.z, pos.z, 0f, 0f, 0f, 1f);
    }

    public static Matrix4d translation(Vector3d t) {
        Matrix4d tr = Matrix4d.identity();
        tr.set(12, t.x);
        tr.set(13, t.y);
        tr.set(14, t.z);
        return tr;
    }

    public static Matrix4d scaling(double sx, double sy, double sz) {
        Matrix4d s = Matrix4d.identity();
        s.set(0, sx);
        s.set(5, sy);
        s.set(10, sy);
        return s;
    }

    public static Matrix4d rotation(Quaternion3d q) {
        return Matrix4d.rotation(q.getAngle(), q.getRotationAxis());
    }

    public static Matrix4d rotation(double angle, Vector3d axis) {
        return Matrix4d.rotation(angle, axis.x, axis.y, axis.z);
    }

    public static Matrix4d rotation(double angle, double axisx, double axisy, double axisz) {
        // Quaterniond quat = new Quaterniond(degAng, axisx, axisy, axisz);
        // return buildRotationMatrix(quat);
        Matrix4d r = Matrix4d.identity();
        double RadAng = (double) angle;
        double ca = (double) Math.cos(RadAng);
        double sa = (double) Math.sin(RadAng);

        if (axisx == 1 && axisy == 0 && axisz == 0) // about x-axis
        {
            r.m[0] = 1;
            r.m[4] = 0;
            r.m[8] = 0;
            r.m[1] = 0;
            r.m[5] = ca;
            r.m[9] = -sa;
            r.m[2] = 0;
            r.m[6] = sa;
            r.m[10] = ca;
        } else if (axisx == 0 && axisy == 1 && axisz == 0) // about y-axis
        {
            r.m[0] = ca;
            r.m[4] = 0;
            r.m[8] = sa;
            r.m[1] = 0;
            r.m[5] = 1;
            r.m[9] = 0;
            r.m[2] = -sa;
            r.m[6] = 0;
            r.m[10] = ca;
        } else if (axisx == 0 && axisy == 0 && axisz == 1) // about z-axis
        {
            r.m[0] = ca;
            r.m[4] = -sa;
            r.m[8] = 0;
            r.m[1] = sa;
            r.m[5] = ca;
            r.m[9] = 0;
            r.m[2] = 0;
            r.m[6] = 0;
            r.m[10] = 1;
        } else // arbitrary axis
        {
            double len = axisx * axisx + axisy * axisy + axisz * axisz; // length
                                                                        // squared
            double x, y, z;
            x = axisx;
            y = axisy;
            z = axisz;
            if (len > 1.0001 || len < 0.9999 && len != 0) {
                len = 1 / (double) Math.sqrt(len);
                x *= len;
                y *= len;
                z *= len;
            }
            double xy = x * y, yz = y * z, xz = x * z, xx = x * x, yy = y * y, zz = z * z;
            r.m[0] = xx + ca * (1 - xx);
            r.m[4] = xy - xy * ca - z * sa;
            r.m[8] = xz - xz * ca + y * sa;
            r.m[1] = xy - xy * ca + z * sa;
            r.m[5] = yy + ca * (1 - yy);
            r.m[9] = yz - yz * ca - x * sa;
            r.m[2] = xz - xz * ca - y * sa;
            r.m[6] = yz - yz * ca + x * sa;
            r.m[10] = zz + ca * (1 - zz);
        }
        r.m[3] = r.m[7] = r.m[11] = 0;
        r.m[15] = 1;

        return r;
    }

    public static Matrix4d frustum(double l, double r, double b, double t, double n, double f) {
        return new Matrix4d((2 * n) / (r - l), 0f, (r + l) / (r - l), 0f, 0f, (2 * n) / (t - b), (t + b) / (t - b), 0f, 0f, 0f, -(f + n) / (f - n), (-2 * f * n) / (f - n), 0f, 0f, -1f, 0f);
    }

    public static Matrix4d perspective(double fov, double aspect, double n, double f) {
        double t = (double) (Math.tan(Math.toRadians(fov * 0.5)) * n);
        double b = -t;
        double r = t * aspect;
        double l = -r;
        return frustum(l, r, b, t, n, f);
    }

    public static Matrix4d viewport(double x, double y, double ww, double wh, double n, double f) {
        double ww2 = ww * 0.5f;
        double wh2 = wh * 0.5f;
        // negate the first wh because windows has topdown window coords
        return new Matrix4d(ww2, 0f, 0f, ww2 + x, 0f, -wh2, 0f, wh2 + y, 0f, 0f, (f - n) * 0.5f, (f + n) * 0.5f, 0f, 0f, 0f, 1f);
    }

    public Matrix3d mat3() {
        Matrix3d mat3 = new Matrix3d(m[0], m[4], m[8], m[1], m[5], m[9], m[2], m[6], m[10]);
        return mat3;
    }

    public Matrix4d copy() {
        return new Matrix4d(this);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        String format = "%01.02f";
        sb.append(String.format(format, m[0]) + ", ");
        sb.append(String.format(format, m[4]) + ", ");
        sb.append(String.format(format, m[8]) + ", ");
        sb.append(String.format(format, m[12]) + ", \n");
        sb.append(String.format(format, m[1]) + ", ");
        sb.append(String.format(format, m[5]) + ", ");
        sb.append(String.format(format, m[9]) + ", ");
        sb.append(String.format(format, m[13]) + ", \n");
        sb.append(String.format(format, m[2]) + ", ");
        sb.append(String.format(format, m[6]) + ", ");
        sb.append(String.format(format, m[10]) + ", ");
        sb.append(String.format(format, m[14]) + ", \n");
        sb.append(String.format(format, m[3]) + ", ");
        sb.append(String.format(format, m[7]) + ", ");
        sb.append(String.format(format, m[11]) + ", ");
        sb.append(String.format(format, m[15]) + ", \n");

        return sb.toString();
    }

    public float[] toFloatArray(){
    	float[] v = new float[16];
    	for (int i = 0; i < m.length; i++){
    		v[i] = (float)m[i];
    	}
    	return v;
    }
}
