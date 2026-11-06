package com.esmail.calculator.modes;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.NativeMath;

/**
 * Immutable Complex Number implementation matching CASIO fx-991ES PLUS CMPLX mode.
 * Arithmetic is executed directly by the x86-64 Assembly engine.
 */
public record ComplexNumber(double real, double imag) {
    public static final ComplexNumber ZERO = new ComplexNumber(0, 0);
    public static final ComplexNumber ONE = new ComplexNumber(1, 0);
    public static final ComplexNumber I = new ComplexNumber(0, 1);

    public ComplexNumber add(ComplexNumber o) {
        if (NativeMath.isAvailable()) {
            return new ComplexNumber(NativeMath.add(real, o.real), NativeMath.add(imag, o.imag));
        }
        return new ComplexNumber(real + o.real, imag + o.imag);
    }

    public ComplexNumber subtract(ComplexNumber o) {
        if (NativeMath.isAvailable()) {
            return new ComplexNumber(NativeMath.subtract(real, o.real), NativeMath.subtract(imag, o.imag));
        }
        return new ComplexNumber(real - o.real, imag - o.imag);
    }

    public ComplexNumber multiply(ComplexNumber o) {
        if (NativeMath.isAvailable()) {
            double[] out = new double[2];
            NativeMath.complexMultiply(real, imag, o.real, o.imag, out);
            return new ComplexNumber(out[0], out[1]);
        }
        return new ComplexNumber(real * o.real - imag * o.imag, real * o.imag + imag * o.real);
    }

    public ComplexNumber divide(ComplexNumber o) {
        double denom = o.real * o.real + o.imag * o.imag;
        if (denom == 0.0) throw new ArithmeticException("Complex division by zero");
        if (NativeMath.isAvailable()) {
            double[] out = new double[2];
            NativeMath.complexDivide(real, imag, o.real, o.imag, out);
            return new ComplexNumber(out[0], out[1]);
        }
        return new ComplexNumber((real * o.real + imag * o.imag) / denom,
                                 (imag * o.real - real * o.imag) / denom);
    }

    public double abs() {
        if (NativeMath.isAvailable()) {
            return NativeMath.sqrt(NativeMath.add(NativeMath.multiply(real, real), NativeMath.multiply(imag, imag)));
        }
        return Math.hypot(real, imag);
    }

    public double arg(boolean degrees) {
        double rad = NativeMath.isAvailable() ? NativeMath.atan2(imag, real) : Math.atan2(imag, real);
        return degrees ? Math.toDegrees(rad) : rad;
    }

    public ComplexNumber conjugate() {
        return new ComplexNumber(real, -imag);
    }

    public ComplexNumber pow(double exponent) {
        double r = Math.pow(abs(), exponent);
        double theta = Math.atan2(imag, real) * exponent;
        return new ComplexNumber(r * Math.cos(theta), r * Math.sin(theta));
    }

    public String formatRectangular() {
        if (Math.abs(imag) < 1e-12) {
            return CasioFormatter.toExactOrFraction(real);
        }
        if (Math.abs(real) < 1e-12) {
            if (Math.abs(imag - 1.0) < 1e-12) return "i";
            if (Math.abs(imag + 1.0) < 1e-12) return "-i";
            return CasioFormatter.toExactOrFraction(imag) + "i";
        }
        String realStr = CasioFormatter.toExactOrFraction(real);
        String sign = imag >= 0 ? "+" : "-";
        double absImag = Math.abs(imag);
        String imagStr = Math.abs(absImag - 1.0) < 1e-12 ? "i" : CasioFormatter.toExactOrFraction(absImag) + "i";
        return realStr + sign + imagStr;
    }

    public String formatPolar(boolean degrees) {
        double r = abs();
        double theta = arg(degrees);
        return CasioFormatter.toExactOrFraction(r) + "∠" + CasioFormatter.toExactOrFraction(theta);
    }

    @Override
    public String toString() {
        return formatRectangular();
    }
}
