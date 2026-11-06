package com.esmail.calculator.modes;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.NativeMath;

import java.util.ArrayList;
import java.util.List;

/**
 * CASIO EQN Mode Engine (Mode 5).
 * Solves 2-variable and 3-variable linear systems, quadratic equations, and cubic equations.
 * Discriminant and root evaluations are accelerated by x86-64 NASM Assembly.
 */
public final class EquationSolver {
    public record Root(String label, ComplexNumber value) {
        public String format() {
            return label + " = " + value.formatRectangular();
        }
    }

    public record EquationResult(List<Root> roots, List<String> vertexInfo) {}

    /**
     * Type 1: Simultaneous linear with 2 unknowns:
     * a1*X + b1*Y = c1
     * a2*X + b2*Y = c2
     */
    public static EquationResult solveLinear2(double a1, double b1, double c1,
                                             double a2, double b2, double c2) {
        double det = NativeMath.isAvailable() ? NativeMath.matrix2x2Det(a1, b1, a2, b2) : a1 * b2 - b1 * a2;
        if (Math.abs(det) < 1e-12) {
            double detX = NativeMath.isAvailable() ? NativeMath.matrix2x2Det(c1, b1, c2, b2) : c1 * b2 - b1 * c2;
            if (Math.abs(detX) < 1e-12) {
                throw new ArithmeticException("Infinite Solutions");
            } else {
                throw new ArithmeticException("No Solution");
            }
        }
        double detX = NativeMath.isAvailable() ? NativeMath.matrix2x2Det(c1, b1, c2, b2) : c1 * b2 - b1 * c2;
        double detY = NativeMath.isAvailable() ? NativeMath.matrix2x2Det(a1, c1, a2, c2) : a1 * c2 - c1 * a2;
        double x = detX / det;
        double y = detY / det;

        List<Root> roots = new ArrayList<>();
        roots.add(new Root("X", new ComplexNumber(x, 0)));
        roots.add(new Root("Y", new ComplexNumber(y, 0)));
        return new EquationResult(roots, List.of());
    }

    /**
     * Type 2: Simultaneous linear with 3 unknowns:
     * a_i*X + b_i*Y + c_i*Z = d_i (i=1..3)
     */
    public static EquationResult solveLinear3(double[] row1, double[] row2, double[] row3) {
        double[] mA = new double[] {
            row1[0], row1[1], row1[2],
            row2[0], row2[1], row2[2],
            row3[0], row3[1], row3[2]
        };
        double det = NativeMath.isAvailable() ? NativeMath.matrix3x3Det(mA) : new MatrixEngine.Matrix(3, 3, mA).determinant();
        if (Math.abs(det) < 1e-12) throw new ArithmeticException("Math ERROR: No unique solution");

        double[] mX = new double[] {
            row1[3], row1[1], row1[2],
            row2[3], row2[1], row2[2],
            row3[3], row3[1], row3[2]
        };
        double[] mY = new double[] {
            row1[0], row1[3], row1[2],
            row2[0], row2[3], row2[2],
            row3[0], row3[3], row3[2]
        };
        double[] mZ = new double[] {
            row1[0], row1[1], row1[3],
            row2[0], row2[1], row2[3],
            row3[0], row3[1], row3[3]
        };

        double detX = NativeMath.isAvailable() ? NativeMath.matrix3x3Det(mX) : new MatrixEngine.Matrix(3, 3, mX).determinant();
        double detY = NativeMath.isAvailable() ? NativeMath.matrix3x3Det(mY) : new MatrixEngine.Matrix(3, 3, mY).determinant();
        double detZ = NativeMath.isAvailable() ? NativeMath.matrix3x3Det(mZ) : new MatrixEngine.Matrix(3, 3, mZ).determinant();

        List<Root> roots = new ArrayList<>();
        roots.add(new Root("X", new ComplexNumber(detX / det, 0)));
        roots.add(new Root("Y", new ComplexNumber(detY / det, 0)));
        roots.add(new Root("Z", new ComplexNumber(detZ / det, 0)));
        return new EquationResult(roots, List.of());
    }

    /**
     * Type 3: Quadratic equation:
     * a*X² + b*X + c = 0
     * Also computes vertex extrema (X-value of Min/Max, Y-value of Min/Max) matching Casio manual.
     */
    public static EquationResult solveQuadratic(double a, double b, double c) {
        if (Math.abs(a) < 1e-14) throw new ArithmeticException("Math ERROR: 'a' cannot be zero");

        double delta = NativeMath.isAvailable() ? NativeMath.quadraticDiscriminant(a, b, c) : b * b - 4.0 * a * c;
        List<Root> roots = new ArrayList<>();

        if (delta >= 0.0) {
            double sqrtD = NativeMath.isAvailable() ? NativeMath.sqrt(delta) : Math.sqrt(delta);
            double x1 = (-b + sqrtD) / (2.0 * a);
            double x2 = (-b - sqrtD) / (2.0 * a);
            roots.add(new Root("X1", new ComplexNumber(x1, 0)));
            roots.add(new Root("X2", new ComplexNumber(x2, 0)));
        } else {
            double sqrtD = NativeMath.isAvailable() ? NativeMath.sqrt(-delta) : Math.sqrt(-delta);
            double realPart = -b / (2.0 * a);
            double imagPart = sqrtD / (2.0 * a);
            roots.add(new Root("X1", new ComplexNumber(realPart, imagPart)));
            roots.add(new Root("X2", new ComplexNumber(realPart, -imagPart)));
        }

        // Vertex information (Min / Max)
        List<String> vertex = new ArrayList<>();
        String extremaType = a > 0 ? "Min" : "Max";
        double xVertex = -b / (2.0 * a);
        double yVertex = c - (b * b) / (4.0 * a);
        vertex.add("X-value of " + extremaType + " = " + CasioFormatter.toExactOrFraction(xVertex));
        vertex.add("Y-value of " + extremaType + " = " + CasioFormatter.toExactOrFraction(yVertex));

        return new EquationResult(roots, vertex);
    }

    /**
     * Type 4: Cubic equation:
     * a*X³ + b*X² + c*X + d = 0
     * Solved via Cardano's analytical method.
     */
    public static EquationResult solveCubic(double a, double b, double c, double d) {
        if (Math.abs(a) < 1e-14) throw new ArithmeticException("Math ERROR: 'a' cannot be zero");

        // Normalize: x^3 + B*x^2 + C*x + D = 0
        double B = b / a;
        double C = c / a;
        double D = d / a;

        // Substitute x = t - B/3 -> t^3 + p*t + q = 0
        double p = C - (B * B) / 3.0;
        double q = (2.0 * B * B * B) / 27.0 - (B * C) / 3.0 + D;

        double delta = (q * q) / 4.0 + (p * p * p) / 27.0;
        List<Root> roots = new ArrayList<>();
        double shift = B / 3.0;

        if (Math.abs(delta) < 1e-12) {
            double u = Math.cbrt(-q / 2.0);
            roots.add(new Root("X1", new ComplexNumber(2.0 * u - shift, 0)));
            roots.add(new Root("X2", new ComplexNumber(-u - shift, 0)));
            roots.add(new Root("X3", new ComplexNumber(-u - shift, 0)));
        } else if (delta > 0.0) {
            double sqrtD = Math.sqrt(delta);
            double u = Math.cbrt(-q / 2.0 + sqrtD);
            double v = Math.cbrt(-q / 2.0 - sqrtD);
            double x1 = u + v - shift;
            double realPart = -(u + v) / 2.0 - shift;
            double imagPart = (u - v) * Math.sqrt(3.0) / 2.0;
            roots.add(new Root("X1", new ComplexNumber(x1, 0)));
            roots.add(new Root("X2", new ComplexNumber(realPart, imagPart)));
            roots.add(new Root("X3", new ComplexNumber(realPart, -imagPart)));
        } else {
            // 3 real roots via trigonometric approach
            double m = 2.0 * Math.sqrt(-p / 3.0);
            double theta = Math.acos((3.0 * q) / (p * m)) / 3.0;
            double x1 = m * Math.cos(theta) - shift;
            double x2 = m * Math.cos(theta + 2.0 * Math.PI / 3.0) - shift;
            double x3 = m * Math.cos(theta + 4.0 * Math.PI / 3.0) - shift;
            roots.add(new Root("X1", new ComplexNumber(x1, 0)));
            roots.add(new Root("X2", new ComplexNumber(x2, 0)));
            roots.add(new Root("X3", new ComplexNumber(x3, 0)));
        }

        return new EquationResult(roots, List.of());
    }
}
