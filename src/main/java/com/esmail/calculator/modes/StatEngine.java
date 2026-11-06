package com.esmail.calculator.modes;

import com.esmail.calculator.engine.MathEngine;
import com.esmail.calculator.engine.NativeMath;

import java.util.ArrayList;
import java.util.List;

/**
 * Official CASIO fx-991ES PLUS / fx-570ES PLUS Statistical Engine (STAT Mode 3).
 * Supports all 8 official Casio calculation models:
 *   1. 1-VAR         (Single variable statistics & normal distribution)
 *   2. A+BX          (Linear regression)
 *   3. _+CX²         (Quadratic regression: y = A + Bx + Cx²)
 *   4. ln X          (Logarithmic regression: y = A + B·ln(x))
 *   5. e^X           (Exponential e regression: y = A·e^(Bx))
 *   6. A•B^X         (Exponential ab regression: y = A·B^x)
 *   7. A•X^B         (Power regression: y = A·x^B)
 *   8. 1/X           (Inverse regression: y = A + B/x)
 * All core operations (mean, sums, stddev, linear regression) use x86-64 NASM Assembly.
 */
public final class StatEngine {

    public enum StatType {
        ONE_VAR(1, "1-VAR"),
        LINEAR(2, "A+BX"),
        QUADRATIC(3, "_+CX²"),
        LOGARITHMIC(4, "ln X"),
        EXPONENTIAL_E(5, "e^X"),
        EXPONENTIAL_AB(6, "A•B^X"),
        POWER(7, "A•X^B"),
        INVERSE(8, "1/X");

        private final int id;
        private final String label;

        StatType(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int id() { return id; }
        public String label() { return label; }
    }

    private final StatType type;
    private final List<Double> xData = new ArrayList<>();
    private final List<Double> yData = new ArrayList<>();
    private final List<Double> freqData = new ArrayList<>();

    public StatEngine(StatType type) {
        this.type = type;
    }

    public StatType getType() { return type; }

    public void addPoint(double x) {
        addPoint(x, 0.0, 1.0);
    }

    public void addPoint(double x, double y) {
        addPoint(x, y, 1.0);
    }

    /**
     * Maximum expanded observation count used by the native statistical routines.
     * Keeping this bounded prevents an untrusted FREQ value from overflowing an
     * int or requesting an unbounded Java array in getExpandedX/getExpandedY.
     */
    static final int MAX_EXPANDED_OBSERVATIONS = 1_000_000;

    public void addPoint(double x, double y, double freq) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new ArithmeticException("STAT ERROR: X and Y must be finite numbers");
        }
        if (!Double.isFinite(freq) || freq <= 0.0 || freq != Math.rint(freq)) {
            throw new ArithmeticException("STAT ERROR: Frequency must be a positive integer");
        }

        double nextTotal = freq;
        for (double existing : freqData) nextTotal += existing;
        if (nextTotal > MAX_EXPANDED_OBSERVATIONS) {
            throw new ArithmeticException("STAT ERROR: Total frequency exceeds " + MAX_EXPANDED_OBSERVATIONS);
        }

        xData.add(x);
        yData.add(y);
        freqData.add(freq);
    }

    public void clear() {
        xData.clear();
        yData.clear();
        freqData.clear();
    }

    public int count() {
        return xData.size();
    }

    // --- 1-VAR & Common Statistics (Backed by Pure NASM Assembly) ---

    public double n() {
        if (freqData.isEmpty()) return 0.0;
        double total = 0.0;
        for (double f : freqData) total += f;
        return total;
    }

    public double sumX() {
        if (NativeMath.isAvailable() && !xData.isEmpty()) {
            return NativeMath.statSum(getExpandedX());
        }
        double s = 0.0;
        for (int i = 0; i < xData.size(); i++) {
            s += xData.get(i) * freqData.get(i);
        }
        return s;
    }

    public double sumX2() {
        if (NativeMath.isAvailable() && !xData.isEmpty()) {
            return NativeMath.statSumSq(getExpandedX());
        }
        double s = 0.0;
        for (int i = 0; i < xData.size(); i++) {
            double x = xData.get(i);
            s += x * x * freqData.get(i);
        }
        return s;
    }

    public double sumX3() {
        double s = 0.0;
        for (int i = 0; i < xData.size(); i++) {
            double x = xData.get(i);
            s += x * x * x * freqData.get(i);
        }
        return s;
    }

    public double sumX4() {
        double s = 0.0;
        for (int i = 0; i < xData.size(); i++) {
            double x = xData.get(i);
            s += x * x * x * x * freqData.get(i);
        }
        return s;
    }

    public double meanX() {
        double totalN = n();
        if (totalN <= 0.0) throw new ArithmeticException("STAT ERROR: Empty dataset");
        if (NativeMath.isAvailable() && !xData.isEmpty()) {
            return NativeMath.statMean(getExpandedX());
        }
        return sumX() / totalN;
    }

    public double sigmaX() {
        double totalN = n();
        if (totalN <= 0.0) throw new ArithmeticException("STAT ERROR: Empty dataset");
        if (NativeMath.isAvailable() && !xData.isEmpty()) {
            return NativeMath.statStdDev(getExpandedX(), false);
        }
        double variance = (sumX2() - (sumX() * sumX()) / totalN) / totalN;
        return Math.sqrt(Math.max(0.0, variance));
    }

    public double sX() {
        double totalN = n();
        if (totalN <= 1.0) throw new ArithmeticException("STAT ERROR: Sample stddev requires n >= 2");
        if (NativeMath.isAvailable() && totalN >= 2.0) {
            return NativeMath.statStdDev(getExpandedX(), true);
        }
        double variance = (sumX2() - (sumX() * sumX()) / totalN) / (totalN - 1.0);
        return Math.sqrt(Math.max(0.0, variance));
    }

    public double minX() {
        if (xData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return xData.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    public double maxX() {
        if (xData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return xData.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    // --- 2-VAR Statistics ---

    public double sumY() {
        double s = 0.0;
        for (int i = 0; i < yData.size(); i++) {
            s += yData.get(i) * freqData.get(i);
        }
        return s;
    }

    public double sumY2() {
        double s = 0.0;
        for (int i = 0; i < yData.size(); i++) {
            double y = yData.get(i);
            s += y * y * freqData.get(i);
        }
        return s;
    }

    public double sumXY() {
        double s = 0.0;
        for (int i = 0; i < Math.min(xData.size(), yData.size()); i++) {
            s += xData.get(i) * yData.get(i) * freqData.get(i);
        }
        return s;
    }

    public double sumX2Y() {
        double s = 0.0;
        for (int i = 0; i < Math.min(xData.size(), yData.size()); i++) {
            double x = xData.get(i);
            s += x * x * yData.get(i) * freqData.get(i);
        }
        return s;
    }

    public double meanY() {
        double totalN = n();
        if (totalN <= 0.0) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return sumY() / totalN;
    }

    public double sigmaY() {
        double totalN = n();
        if (totalN <= 0.0) throw new ArithmeticException("STAT ERROR: Empty dataset");
        double variance = (sumY2() - (sumY() * sumY()) / totalN) / totalN;
        return Math.sqrt(Math.max(0.0, variance));
    }

    public double sY() {
        double totalN = n();
        if (totalN <= 1.0) throw new ArithmeticException("STAT ERROR: Sample stddev requires n >= 2");
        double variance = (sumY2() - (sumY() * sumY()) / totalN) / (totalN - 1.0);
        return Math.sqrt(Math.max(0.0, variance));
    }

    public double minY() {
        if (yData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return yData.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    public double maxY() {
        if (yData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return yData.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    // --- Normal Distribution Calculations (P, Q, R, t) ---

    public double tStandard(double x) {
        double s = sigmaX();
        if (s == 0.0) throw new ArithmeticException("STAT ERROR: Standard deviation is zero");
        return (x - meanX()) / s;
    }

    /** Standard normal distribution CDF P(t) = P(Z <= t). */
    public double probP(double t) {
        return 0.5 * (1.0 + erf(t / Math.sqrt(2.0)));
    }

    /** Casio Q(t) = Probability between 0 and t: Q(t) = P(t) - 0.5. */
    public double probQ(double t) {
        return probP(t) - 0.5;
    }

    /** Casio R(t) = Probability to the right of t: R(t) = 1 - P(t). */
    public double probR(double t) {
        return 1.0 - probP(t);
    }

    /** Error function erf(z) implementation for standard normal distribution. */
    private static double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));
        double tau = t * Math.exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418
                     + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587
                     + t * (-0.82215223 + t * 0.17087277)))))))));
        return z >= 0.0 ? 1.0 - tau : tau - 1.0;
    }

    // --- Regression Solvers for All 8 CASIO Models ---

    /**
     * Solves the active regression model.
     * Returns:
     * - LINEAR, LOG, EXP_E, EXP_AB, POWER, INVERSE: double[3] { A, B, r }
     * - QUADRATIC: double[3] { A, B, C }
     */
    public double[] regressionParams() {
        if (xData.size() < 2) throw new ArithmeticException("STAT ERROR: Regression requires n >= 2");

        switch (type) {
            case LINEAR -> {
                double[] x = getExpandedX();
                double[] y = getExpandedY();
                return MathEngine.linearRegression(x, y);
            }
            case LOGARITHMIC -> {
                // y = A + B·ln(x)
                double[] ex = getExpandedX();
                double[] ey = getExpandedY();
                double[] lx = new double[ex.length];
                for (int i = 0; i < ex.length; i++) {
                    if (ex[i] <= 0.0) throw new ArithmeticException("Math ERROR: ln requires x > 0");
                    lx[i] = Math.log(ex[i]);
                }
                return MathEngine.linearRegression(lx, ey);
            }
            case EXPONENTIAL_E -> {
                // y = A·e^(Bx) => ln(y) = ln(A) + Bx
                double[] ex = getExpandedX();
                double[] ey = getExpandedY();
                double[] ly = new double[ey.length];
                for (int i = 0; i < ey.length; i++) {
                    if (ey[i] <= 0.0) throw new ArithmeticException("Math ERROR: e^X requires y > 0");
                    ly[i] = Math.log(ey[i]);
                }
                double[] lin = MathEngine.linearRegression(ex, ly);
                return new double[] { Math.exp(lin[0]), lin[1], lin[2] };
            }
            case EXPONENTIAL_AB -> {
                // y = A·B^x => ln(y) = ln(A) + x·ln(B)
                double[] ex = getExpandedX();
                double[] ey = getExpandedY();
                double[] ly = new double[ey.length];
                for (int i = 0; i < ey.length; i++) {
                    if (ey[i] <= 0.0) throw new ArithmeticException("Math ERROR: A·B^X requires y > 0");
                    ly[i] = Math.log(ey[i]);
                }
                double[] lin = MathEngine.linearRegression(ex, ly);
                return new double[] { Math.exp(lin[0]), Math.exp(lin[1]), lin[2] };
            }
            case POWER -> {
                // y = A·x^B => ln(y) = ln(A) + B·ln(x)
                double[] ex = getExpandedX();
                double[] ey = getExpandedY();
                double[] lx = new double[ex.length];
                double[] ly = new double[ey.length];
                for (int i = 0; i < ex.length; i++) {
                    if (ex[i] <= 0.0 || ey[i] <= 0.0) throw new ArithmeticException("Math ERROR: Power requires x>0, y>0");
                    lx[i] = Math.log(ex[i]);
                    ly[i] = Math.log(ey[i]);
                }
                double[] lin = MathEngine.linearRegression(lx, ly);
                return new double[] { Math.exp(lin[0]), lin[1], lin[2] };
            }
            case INVERSE -> {
                // y = A + B/x
                double[] ex = getExpandedX();
                double[] ey = getExpandedY();
                double[] invX = new double[ex.length];
                for (int i = 0; i < ex.length; i++) {
                    if (ex[i] == 0.0) throw new ArithmeticException("Math ERROR: 1/X divide by zero");
                    invX[i] = 1.0 / ex[i];
                }
                return MathEngine.linearRegression(invX, ey);
            }
            case QUADRATIC -> {
                // y = A + Bx + Cx²
                // Solve 3x3 linear system for [A, B, C]
                double dn = n();
                double sx = sumX(), sx2 = sumX2(), sx3 = sumX3(), sx4 = sumX4();
                double sy = sumY(), sxy = sumXY(), sx2y = sumX2Y();

                double det = dn * (sx2 * sx4 - sx3 * sx3)
                           - sx * (sx * sx4 - sx3 * sx2)
                           + sx2 * (sx * sx3 - sx2 * sx2);

                if (Math.abs(det) < 1e-15) throw new ArithmeticException("Math ERROR: No unique quadratic solution");

                double detA = sy * (sx2 * sx4 - sx3 * sx3)
                            - sx * (sxy * sx4 - sx3 * sx2y)
                            + sx2 * (sxy * sx3 - sx2 * sx2y);

                double detB = dn * (sxy * sx4 - sx3 * sx2y)
                            - sy * (sx * sx4 - sx3 * sx2)
                            + sx2 * (sx * sx2y - sxy * sx2);

                double detC = dn * (sx2 * sx2y - sxy * sx3)
                            - sx * (sx * sx2y - sxy * sx2)
                            + sy * (sx * sx3 - sx2 * sx2);

                return new double[] { detA / det, detB / det, detC / det };
            }
            default -> throw new ArithmeticException("STAT ERROR: Not a regression mode");
        }
    }

    public double regA() { return regressionParams()[0]; }
    public double regB() { return regressionParams()[1]; }
    public double regC() {
        double[] p = regressionParams();
        return p.length >= 3 ? p[2] : 0.0;
    }
    public double regR() {
        double[] p = regressionParams();
        return p.length >= 3 && type != StatType.QUADRATIC ? p[2] : 0.0;
    }

    public double estimateY(double x) {
        double[] p = regressionParams();
        return switch (type) {
            case LINEAR -> p[0] + p[1] * x;
            case QUADRATIC -> p[0] + p[1] * x + p[2] * x * x;
            case LOGARITHMIC -> p[0] + p[1] * Math.log(x);
            case EXPONENTIAL_E -> p[0] * Math.exp(p[1] * x);
            case EXPONENTIAL_AB -> p[0] * Math.pow(p[1], x);
            case POWER -> p[0] * Math.pow(x, p[1]);
            case INVERSE -> p[0] + p[1] / x;
            default -> p[0] + p[1] * x;
        };
    }

    public double estimateX(double y) {
        double[] p = regressionParams();
        return switch (type) {
            case LINEAR -> (y - p[0]) / p[1];
            case LOGARITHMIC -> Math.exp((y - p[0]) / p[1]);
            case EXPONENTIAL_E -> Math.log(y / p[0]) / p[1];
            case EXPONENTIAL_AB -> Math.log(y / p[0]) / Math.log(p[1]);
            case POWER -> Math.pow(y / p[0], 1.0 / p[1]);
            case INVERSE -> p[1] / (y - p[0]);
            case QUADRATIC -> {
                // Solves C x^2 + B x + (A - y) = 0
                double disc = p[1] * p[1] - 4.0 * p[2] * (p[0] - y);
                if (disc < 0) throw new ArithmeticException("Math ERROR: No real estimate for X");
                yield (-p[1] + Math.sqrt(disc)) / (2.0 * p[2]);
            }
            default -> (y - p[0]) / p[1];
        };
    }

    private double[] getExpandedX() {
        int total = 0;
        for (double f : freqData) total += (int) f;
        double[] arr = new double[total];
        int idx = 0;
        for (int i = 0; i < xData.size(); i++) {
            int count = (int) freqData.get(i).doubleValue();
            for (int c = 0; c < count; c++) arr[idx++] = xData.get(i);
        }
        return arr;
    }

    private double[] getExpandedY() {
        int total = 0;
        for (double f : freqData) total += (int) f;
        double[] arr = new double[total];
        int idx = 0;
        for (int i = 0; i < yData.size(); i++) {
            int count = (int) freqData.get(i).doubleValue();
            for (int c = 0; c < count; c++) arr[idx++] = yData.get(i);
        }
        return arr;
    }
}
