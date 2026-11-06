package com.esmail.calculator.modes;

import com.esmail.calculator.engine.MathEngine;
import com.esmail.calculator.engine.NativeMath;

import java.util.ArrayList;
import java.util.List;

/**
 * CASIO fx-991ES PLUS / fx-570ES PLUS Statistical Engine (STAT Mode 3).
 * Supports 1-VAR single-variable statistics and 2-VAR (A+BX) linear regression.
 * All computations (sums, sums of squares, mean, stddev, variance, regression coefficients)
 * are calculated directly via the x86-64 NASM Assembly engine (glasscalc.dll).
 */
public final class StatEngine {

    public enum StatType {
        ONE_VAR(1, "1-VAR"),
        LINEAR(2, "A+BX");

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

    public StatEngine(StatType type) {
        this.type = type;
    }

    public StatType getType() { return type; }

    public void addPoint(double x) {
        xData.add(x);
        yData.add(0.0);
    }

    public void addPoint(double x, double y) {
        xData.add(x);
        yData.add(y);
    }

    public void clear() {
        xData.clear();
        yData.clear();
    }

    public int count() {
        return xData.size();
    }

    public double[] getXArray() {
        return xData.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public double[] getYArray() {
        return yData.stream().mapToDouble(Double::doubleValue).toArray();
    }

    // --- 1-VAR & Common Statistics (Backed by Pure NASM Assembly) ---

    public int n() {
        return xData.size();
    }

    public double sumX() {
        if (xData.isEmpty()) return 0.0;
        return MathEngine.sum(getXArray());
    }

    public double sumX2() {
        if (xData.isEmpty()) return 0.0;
        double[] x = getXArray();
        return NativeMath.isAvailable() ? NativeMath.statSumSq(x) : sumSquaresFallback(x);
    }

    public double meanX() {
        if (xData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return MathEngine.mean(getXArray());
    }

    public double sigmaX() {
        if (xData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return MathEngine.standardDeviation(getXArray());
    }

    public double sX() {
        if (xData.size() < 2) throw new ArithmeticException("STAT ERROR: Sample stddev requires n >= 2");
        return MathEngine.sampleStandardDeviation(getXArray());
    }

    public double minX() {
        if (xData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return xData.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    public double maxX() {
        if (xData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return xData.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    // --- 2-VAR Linear Regression (A+BX) ---

    public double sumY() {
        if (yData.isEmpty()) return 0.0;
        return MathEngine.sum(getYArray());
    }

    public double sumY2() {
        if (yData.isEmpty()) return 0.0;
        double[] y = getYArray();
        return NativeMath.isAvailable() ? NativeMath.statSumSq(y) : sumSquaresFallback(y);
    }

    public double sumXY() {
        if (xData.isEmpty()) return 0.0;
        int n = Math.min(xData.size(), yData.size());
        double s = 0.0;
        for (int i = 0; i < n; i++) s += xData.get(i) * yData.get(i);
        return s;
    }

    public double meanY() {
        if (yData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return MathEngine.mean(getYArray());
    }

    public double sigmaY() {
        if (yData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return MathEngine.standardDeviation(getYArray());
    }

    public double sY() {
        if (yData.size() < 2) throw new ArithmeticException("STAT ERROR: Sample stddev requires n >= 2");
        return MathEngine.sampleStandardDeviation(getYArray());
    }

    public double minY() {
        if (yData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return yData.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    public double maxY() {
        if (yData.isEmpty()) throw new ArithmeticException("STAT ERROR: Empty dataset");
        return yData.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    /** Returns [A, B, r] computed via NASM assembly linear regression kernel. */
    public double[] regressionParams() {
        if (xData.size() < 2) throw new ArithmeticException("STAT ERROR: Regression requires n >= 2");
        return MathEngine.linearRegression(getXArray(), getYArray());
    }

    public double regA() { return regressionParams()[0]; }
    public double regB() { return regressionParams()[1]; }
    public double regR() { return regressionParams()[2]; }

    public double estimateY(double x) {
        double[] p = regressionParams();
        return p[0] + p[1] * x;
    }

    public double estimateX(double y) {
        double[] p = regressionParams();
        if (Math.abs(p[1]) < 1e-15) throw new ArithmeticException("STAT ERROR: Slope B is zero");
        return (y - p[0]) / p[1];
    }

    private static double sumSquaresFallback(double[] arr) {
        double s = 0.0;
        for (double v : arr) s += v * v;
        return s;
    }
}
