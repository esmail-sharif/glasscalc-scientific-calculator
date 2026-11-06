package com.esmail.calculator.engine;

/** Arithmetic facade: delegates to NASM through JNI whenever the native DLL is available. */
public final class MathEngine {
    private MathEngine() { }

    public static boolean isAssemblyActive() { return NativeMath.isAvailable(); }
    public static double add(double a, double b) { return NativeMath.isAvailable() ? NativeMath.add(a, b) : a + b; }
    public static double subtract(double a, double b) { return NativeMath.isAvailable() ? NativeMath.subtract(a, b) : a - b; }
    public static double multiply(double a, double b) { return NativeMath.isAvailable() ? NativeMath.multiply(a, b) : a * b; }

    public static double divide(double a, double b) {
        if (b == 0.0) throw new ArithmeticException("Division by zero");
        return NativeMath.isAvailable() ? NativeMath.divide(a, b) : a / b;
    }

    public static double sin(double x) { return NativeMath.isAvailable() ? NativeMath.sin(x) : Math.sin(x); }
    public static double cos(double x) { return NativeMath.isAvailable() ? NativeMath.cos(x) : Math.cos(x); }
    public static double tan(double x) { return NativeMath.isAvailable() ? NativeMath.tan(x) : Math.tan(x); }

    public static double sqrt(double x) {
        if (x < 0.0) throw new ArithmeticException("Square root of a negative number");
        return NativeMath.isAvailable() ? NativeMath.sqrt(x) : Math.sqrt(x);
    }

    public static double ln(double x) {
        if (x <= 0.0) throw new ArithmeticException("ln is defined only for positive numbers");
        return NativeMath.isAvailable() ? NativeMath.ln(x) : Math.log(x);
    }

    public static double log10(double x) {
        if (x <= 0.0) throw new ArithmeticException("log is defined only for positive numbers");
        return NativeMath.isAvailable() ? NativeMath.log10(x) : Math.log10(x);
    }

    public static double pow(double base, double exponent) {
        if (base == 0.0 && exponent < 0.0) throw new ArithmeticException("Zero cannot be raised to a negative power");
        return NativeMath.isAvailable() ? NativeMath.pow(base, exponent) : Math.pow(base, exponent);
    }

    public static double atan(double x) { return NativeMath.isAvailable() ? NativeMath.atan(x) : Math.atan(x); }

    public static double asin(double x) {
        if (x < -1.0 || x > 1.0) throw new ArithmeticException("asin input must be between -1 and 1");
        if (x == 1.0) return Math.PI / 2.0;
        if (x == -1.0) return -Math.PI / 2.0;
        return atan(x / sqrt(1.0 - x * x));
    }

    public static double acos(double x) {
        if (x < -1.0 || x > 1.0) throw new ArithmeticException("acos input must be between -1 and 1");
        return Math.PI / 2.0 - asin(x);
    }

    public static double factorial(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 170 || value != Math.rint(value)) {
            throw new ArithmeticException("Factorial requires an integer from 0 to 170");
        }
        long n = (long) value;
        if (NativeMath.isAvailable()) return NativeMath.factorial(n);
        double result = 1.0;
        for (long i = 2; i <= n; i++) result *= i;
        return result;
    }
}
