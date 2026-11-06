package com.esmail.calculator.modes;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.NativeMath;

import java.util.HashMap;
import java.util.Map;

/**
 * CASIO MATRIX Mode Engine (Mode 6).
 * Backed by x86-64 Assembly routines in glasscalc.dll for determinants and multiplication.
 */
public final class MatrixEngine {
    public static final class Matrix {
        public final int rows;
        public final int cols;
        public final double[] data;

        public Matrix(int rows, int cols) {
            this(rows, cols, new double[rows * cols]);
        }

        public Matrix(int rows, int cols, double[] data) {
            this.rows = rows;
            this.cols = cols;
            this.data = data.clone();
        }

        public double get(int r, int c) {
            return data[r * cols + c];
        }

        public void set(int r, int c, double val) {
            data[r * cols + c] = val;
        }

        public double determinant() {
            if (rows != cols) throw new ArithmeticException("Dimension ERROR: Determinant requires square matrix");
            if (rows == 1) return data[0];
            if (rows == 2) {
                if (NativeMath.isAvailable()) {
                    return NativeMath.matrix2x2Det(data[0], data[1], data[2], data[3]);
                }
                return data[0] * data[3] - data[1] * data[2];
            }
            if (rows == 3) {
                if (NativeMath.isAvailable()) {
                    return NativeMath.matrix3x3Det(data);
                }
                return data[0] * (data[4] * data[8] - data[5] * data[7])
                     - data[1] * (data[3] * data[8] - data[5] * data[6])
                     + data[2] * (data[3] * data[7] - data[4] * data[6]);
            }
            throw new ArithmeticException("Dimension ERROR: Max 3x3 supported");
        }

        public Matrix multiply(Matrix o) {
            if (this.cols != o.rows) throw new ArithmeticException("Dimension ERROR: Matrix dimensions do not match");
            if (this.rows == 3 && this.cols == 3 && o.rows == 3 && o.cols == 3) {
                if (NativeMath.isAvailable()) {
                    double[] out = new double[9];
                    NativeMath.matrix3x3Multiply(this.data, o.data, out);
                    return new Matrix(3, 3, out);
                }
            }
            Matrix res = new Matrix(this.rows, o.cols);
            for (int i = 0; i < this.rows; i++) {
                for (int j = 0; j < o.cols; j++) {
                    double sum = 0.0;
                    for (int k = 0; k < this.cols; k++) {
                        sum += this.get(i, k) * o.get(k, j);
                    }
                    res.set(i, j, sum);
                }
            }
            return res;
        }

        public Matrix add(Matrix o) {
            if (this.rows != o.rows || this.cols != o.cols) {
                throw new ArithmeticException("Dimension ERROR: Incompatible matrix addition sizes");
            }
            double[] res = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                res[i] = NativeMath.isAvailable() ? NativeMath.add(data[i], o.data[i]) : data[i] + o.data[i];
            }
            return new Matrix(rows, cols, res);
        }

        public Matrix subtract(Matrix o) {
            if (this.rows != o.rows || this.cols != o.cols) {
                throw new ArithmeticException("Dimension ERROR: Incompatible matrix subtraction sizes");
            }
            double[] res = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                res[i] = NativeMath.isAvailable() ? NativeMath.subtract(data[i], o.data[i]) : data[i] - o.data[i];
            }
            return new Matrix(rows, cols, res);
        }

        public Matrix scale(double scalar) {
            double[] res = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                res[i] = NativeMath.isAvailable() ? NativeMath.multiply(data[i], scalar) : data[i] * scalar;
            }
            return new Matrix(rows, cols, res);
        }

        public Matrix transpose() {
            Matrix res = new Matrix(cols, rows);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    res.set(c, r, get(r, c));
                }
            }
            return res;
        }

        public Matrix inverse() {
            double det = determinant();
            if (Math.abs(det) < 1e-12) throw new ArithmeticException("Math ERROR: Matrix is singular (det=0)");
            if (rows == 1) return new Matrix(1, 1, new double[] { 1.0 / data[0] });
            if (rows == 2) {
                double invDet = 1.0 / det;
                return new Matrix(2, 2, new double[] {
                    data[3] * invDet, -data[1] * invDet,
                    -data[2] * invDet, data[0] * invDet
                });
            }
            if (rows == 3) {
                double invDet = 1.0 / det;
                double[] inv = new double[9];
                inv[0] = (data[4] * data[8] - data[5] * data[7]) * invDet;
                inv[1] = (data[2] * data[7] - data[1] * data[8]) * invDet;
                inv[2] = (data[1] * data[5] - data[2] * data[4]) * invDet;
                inv[3] = (data[5] * data[6] - data[3] * data[8]) * invDet;
                inv[4] = (data[0] * data[8] - data[2] * data[6]) * invDet;
                inv[5] = (data[2] * data[3] - data[0] * data[5]) * invDet;
                inv[6] = (data[3] * data[7] - data[4] * data[6]) * invDet;
                inv[7] = (data[1] * data[6] - data[0] * data[7]) * invDet;
                inv[8] = (data[0] * data[4] - data[1] * data[3]) * invDet;
                return new Matrix(3, 3, inv);
            }
            throw new ArithmeticException("Dimension ERROR");
        }

        public String format() {
            StringBuilder sb = new StringBuilder("[");
            for (int r = 0; r < rows; r++) {
                if (r > 0) sb.append("; ");
                for (int c = 0; c < cols; c++) {
                    if (c > 0) sb.append(", ");
                    sb.append(CasioFormatter.toExactOrFraction(get(r, c)));
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private static final Map<String, Matrix> STORAGE = new HashMap<>();

    static {
        // Initialize default Casio matrices
        STORAGE.put("MATA", new Matrix(3, 3, new double[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 }));
        STORAGE.put("MATB", new Matrix(3, 3, new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
        STORAGE.put("MATC", new Matrix(2, 2, new double[] { 1, 2, 3, 4 }));
        STORAGE.put("MATANS", new Matrix(3, 3));
    }

    public static void setMatrix(String name, Matrix m) {
        STORAGE.put(name.toUpperCase(), m);
    }

    public static Matrix getMatrix(String name) {
        return STORAGE.get(name.toUpperCase());
    }
}
