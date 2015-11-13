package org.helioviewer.jhv.base.math;

public class Matrix4d
{
	public static final Matrix4d IDENTITY = new Matrix4d(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f);

	/**
	 * 0 4  8 12
	 * 1 5  9 13
	 * 2 6 10 14
	 * 3 7 11 15
	 */
	public double[] m = new double[16];

	public Matrix4d(double M0, double M4, double M8, double M12, double M1, double M5, double M9, double M13, double M2,
			double M6, double M10, double M14, double M3, double M7, double M11, double M15)
	{
		m[0] = M0;
		m[1] = M1;
		m[2] = M2;
		m[3] = M3;
		m[4] = M4;
		m[5] = M5;
		m[6] = M6;
		m[7] = M7;
		m[8] = M8;
		m[9] = M9;
		m[10] = M10;
		m[11] = M11;
		m[12] = M12;
		m[13] = M13;
		m[14] = M14;
		m[15] = M15;
	}

	private Matrix4d()
	{
	}

	private Matrix4d(Matrix4d _m)
	{
		System.arraycopy(_m.m, 0, m, 0, m.length);
	}

	public double get(int _index)
	{
		if (_index < 0 || _index > 15)
			throw new IndexOutOfBoundsException("Mat4 has 16 fields");

		return m[_index];
	}

	public Matrix4d multiplied(Matrix4d _a)
	{
		return new Matrix4d(m[0] * _a.m[0] + m[4] * _a.m[1] + m[8] * _a.m[2] + m[12] * _a.m[3], // row 1
				m[0] * _a.m[4] + m[4] * _a.m[5] + m[8] * _a.m[6] + m[12] * _a.m[7],
				m[0] * _a.m[8] + m[4] * _a.m[9] + m[8] * _a.m[10] + m[12] * _a.m[11],
				m[0] * _a.m[12] + m[4] * _a.m[13] + m[8] * _a.m[14] + m[12] * _a.m[15],

				m[1] * _a.m[0] + m[5] * _a.m[1] + m[9] * _a.m[2] + m[13] * _a.m[3], // row 2
				m[1] * _a.m[4] + m[5] * _a.m[5] + m[9] * _a.m[6] + m[13] * _a.m[7],
				m[1] * _a.m[8] + m[5] * _a.m[9] + m[9] * _a.m[10] + m[13] * _a.m[11],
				m[1] * _a.m[12] + m[5] * _a.m[13] + m[9] * _a.m[14] + m[13] * _a.m[15],

				m[2] * _a.m[0] + m[6] * _a.m[1] + m[10] * _a.m[2] + m[14] * _a.m[3], // row 3
				m[2] * _a.m[4] + m[6] * _a.m[5] + m[10] * _a.m[6] + m[14] * _a.m[7],
				m[2] * _a.m[8] + m[6] * _a.m[9] + m[10] * _a.m[10] + m[14] * _a.m[11],
				m[2] * _a.m[12] + m[6] * _a.m[13] + m[10] * _a.m[14] + m[14] * _a.m[15],

				m[3] * _a.m[0] + m[7] * _a.m[1] + m[11] * _a.m[2] + m[15] * _a.m[3], // row 4
				m[3] * _a.m[4] + m[7] * _a.m[5] + m[11] * _a.m[6] + m[15] * _a.m[7],
				m[3] * _a.m[8] + m[7] * _a.m[9] + m[11] * _a.m[10] + m[15] * _a.m[11],
				m[3] * _a.m[12] + m[7] * _a.m[13] + m[11] * _a.m[14] + m[15] * _a.m[15]);
	}

	public Vector3d multiply(Vector3d _v)
	{
		double W = m[3] * _v.x + m[7] * _v.y + m[11] * _v.z + m[15];
		return new Vector3d((m[0] * _v.x + m[4] * _v.y + m[8] * _v.z + m[12]) / W,
				(m[1] * _v.x + m[5] * _v.y + m[9] * _v.z + m[13]) / W,
				(m[2] * _v.x + m[6] * _v.y + m[10] * _v.z + m[14]) / W);
	}

	public Vector4d multiply(Vector4d v)
	{
		return new Vector4d(m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12] * v.w,
				m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13] * v.w, m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14] * v.w,
				m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15] * v.w);
	}

	public Vector3d translationPart()
	{
		return new Vector3d(m[12], m[13], m[14]);
	}

	public Matrix4d translatedAbsolute(double x, double y, double z)
	{
		Matrix4d copy = new Matrix4d(this);
		copy.m[12] = x;
		copy.m[13] = y;
		copy.m[14] = z;
		return copy;
	}

	public Matrix4d translated(Vector3d _translation)
	{
		return translated(_translation.x, _translation.y, _translation.z);
	}

	public Matrix4d translated(double _x, double _y, double _z)
	{
		Matrix4d copy = new Matrix4d(this);
		copy.m[12] += _x;
		copy.m[13] += _y;
		copy.m[14] += _z;
		return copy;
	}

	public Matrix4d inverse()
	{
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

	public Matrix4d rotated(double angle, Vector3d axis)
	{
		return rotated(angle, axis.x, axis.y, axis.z);
	}

	public Matrix4d rotated(double angle, double axisx, double axisy, double axisz)
	{
		return multiplied(Matrix4d.createRotationMatrix(angle, axisx, axisy, axisz));
	}

	public Matrix4d scaled(Vector3d s)
	{
		return scaled(s.x, s.y, s.z);
	}

	public Matrix4d scaled(double sx, double sy, double sz)
	{
		return multiplied(Matrix4d.createScalingMatrix(sx, sy, sz));
	}

	public Matrix4d transposed()
	{
		Matrix4d copy = new Matrix4d();

		copy.m[0] = m[0];
		copy.m[1] = m[4];
		copy.m[2] = m[8];
		copy.m[3] = m[12];
		copy.m[4] = m[1];
		copy.m[5] = m[5];
		copy.m[6] = m[9];
		copy.m[7] = m[13];
		copy.m[8] = m[2];
		copy.m[9] = m[6];
		copy.m[10] = m[10];
		copy.m[11] = m[14];
		copy.m[12] = m[3];
		copy.m[13] = m[7];
		copy.m[14] = m[11];
		copy.m[15] = m[15];

		return copy;
	}

	public static Matrix4d createScalingMatrix(double _sx, double _sy, double _sz)
	{
		Matrix4d s = new Matrix4d();
		s.m[0] = _sx;
		s.m[5] = _sy;
		s.m[10] = _sz;
		s.m[15] = 1;
		return s;
	}

	public static Matrix4d createRotationMatrix(Quaternion _q)
	{
		return _q.toMatrix();
	}

	public static Matrix4d createRotationMatrix(double _angle, Vector3d _axis)
	{
		return Matrix4d.createRotationMatrix(_angle, _axis.x, _axis.y, _axis.z);
	}

	public static Matrix4d createRotationMatrix(double angle, double _x, double _y, double _z)
	{
		Matrix4d r = new Matrix4d(IDENTITY);
		double ca = Math.cos(angle);
		double sa = Math.sin(angle);

		if (_x == 1 && _y == 0 && _z == 0) // about x-axis
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
		}
		else if (_x == 0 && _y == 1 && _z == 0) // about y-axis
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
		}
		else if (_x == 0 && _y == 0 && _z == 1) // about z-axis
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
		}
		else // arbitrary axis
		{
			double len = _x * _x + _y * _y + _z * _z; // length squared
			double x = _x;
			double y = _y;
			double z = _z;
			if (len > 1.0001 || len < 0.9999 && len != 0)
			{
				len = 1 / Math.sqrt(len);
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

	public static Matrix4d createFrustumMatrix(double l, double r, double b, double t, double n, double f)
	{
		return new Matrix4d((2 * n) / (r - l), 0f, (r + l) / (r - l), 0f, 0f, (2 * n) / (t - b), (t + b) / (t - b), 0f,
				0f, 0f, -(f + n) / (f - n), (-2 * f * n) / (f - n), 0f, 0f, -1f, 0f);
	}

	public static Matrix4d createPerspectiveMatrix(double fov, double aspect, double n, double f)
	{
		double t = Math.tan(Math.toRadians(fov * 0.5)) * n;
		double b = -t;
		double r = t * aspect;
		double l = -r;
		return createFrustumMatrix(l, r, b, t, n, f);
	}

	public String toString()
	{
		String format = "%01.02f, %01.02f, %01.02f, %01.02f";
		return String.format(format, m[0], m[4], m[8], m[12]) + ", \n" +
				String.format(format, m[1], m[5], m[9], m[13]) + ", \n" +
				String.format(format, m[2], m[6], m[10], m[14]) + ", \n" +
				String.format(format, m[3], m[7], m[11], m[15]) + "\n";
	}

	public float[] toFloatArray()
	{
		float[] v = new float[16];
		for (int i = 0; i < m.length; i++)
			v[i] = (float) m[i];

		return v;
	}
}
