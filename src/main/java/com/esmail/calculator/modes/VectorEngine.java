package com.esmail.calculator.modes;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.NativeMath;

import java.util.HashMap;
import java.util.Map;

/**
 * CASIO VECTOR Mode Engine (Mode 8).
 * Dot product, cross product, norm, and linear combination executed via x86-64 NASM Assembly.
 */
public final class VectorEngine {
    public static final class Vector3D {
        public final double[] v;

        public Vector3D(double x, double y) {
            this(x, y, 0.0);
        }

        public Vector3D(double x, double y, double z) {
            this.v = new double[] { x, y, z };
        }

        public Vector3D(double[] components) {
            this.v = new double[3];
            for (int i = 0; i < Math.min(3, components.length); i++) {
                this.v[i] = components[i];
            }
        }

        public double x() { return v[0]; }
        public double y() { return v[1]; }
        public double z() { return v[2]; }

        public double dot(Vector3D o) {
            if (NativeMath.isAvailable()) {
                return NativeMath.vector3dDot(this.v, o.v);
            }
            return v[0] * o.v[0] + v[1] * o.v[1] + v[2] * o.v[2];
        }

        public Vector3D cross(Vector3D o) {
            if (NativeMath.isAvailable()) {
                double[] out = new double[3];
                NativeMath.vector3dCross(this.v, o.v, out);
                return new Vector3D(out);
            }
            return new Vector3D(
                v[1] * o.v[2] - v[2] * o.v[1],
                v[2] * o.v[0] - v[0] * o.v[2],
                v[0] * o.v[1] - v[1] * o.v[0]
            );
        }

        public double norm() {
            if (NativeMath.isAvailable()) {
                return NativeMath.vector3dNorm(this.v);
            }
            return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        }

        public Vector3D add(Vector3D o) {
            return new Vector3D(
                NativeMath.isAvailable() ? NativeMath.add(v[0], o.v[0]) : v[0] + o.v[0],
                NativeMath.isAvailable() ? NativeMath.add(v[1], o.v[1]) : v[1] + o.v[1],
                NativeMath.isAvailable() ? NativeMath.add(v[2], o.v[2]) : v[2] + o.v[2]
            );
        }

        public Vector3D subtract(Vector3D o) {
            return new Vector3D(
                NativeMath.isAvailable() ? NativeMath.subtract(v[0], o.v[0]) : v[0] - o.v[0],
                NativeMath.isAvailable() ? NativeMath.subtract(v[1], o.v[1]) : v[1] - o.v[1],
                NativeMath.isAvailable() ? NativeMath.subtract(v[2], o.v[2]) : v[2] - o.v[2]
            );
        }

        public Vector3D scale(double scalar) {
            return new Vector3D(
                NativeMath.isAvailable() ? NativeMath.multiply(v[0], scalar) : v[0] * scalar,
                NativeMath.isAvailable() ? NativeMath.multiply(v[1], scalar) : v[1] * scalar,
                NativeMath.isAvailable() ? NativeMath.multiply(v[2], scalar) : v[2] * scalar
            );
        }

        public double angle(Vector3D o, boolean degrees) {
            double d = this.dot(o);
            double n = this.norm() * o.norm();
            if (n == 0.0) throw new ArithmeticException("Math ERROR: Zero vector in angle calculation");
            double cosVal = Math.max(-1.0, Math.min(1.0, d / n));
            double rad = Math.acos(cosVal);
            return degrees ? Math.toDegrees(rad) : rad;
        }

        public String format() {
            return "[" + CasioFormatter.toExactOrFraction(v[0]) + ", "
                       + CasioFormatter.toExactOrFraction(v[1]) + ", "
                       + CasioFormatter.toExactOrFraction(v[2]) + "]";
        }
    }

    private static final Map<String, Vector3D> STORAGE = new HashMap<>();

    static {
        STORAGE.put("VCTA", new Vector3D(1, 2, 3));
        STORAGE.put("VCTB", new Vector3D(4, 5, 6));
        STORAGE.put("VCTC", new Vector3D(7, 8, 9));
        STORAGE.put("VCTANS", new Vector3D(0, 0, 0));
    }

    public static void setVector(String name, Vector3D v) {
        STORAGE.put(name.toUpperCase(), v);
    }

    public static Vector3D getVector(String name) {
        return STORAGE.get(name.toUpperCase());
    }
}
