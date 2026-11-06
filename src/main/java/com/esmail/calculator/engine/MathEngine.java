package com.esmail.calculator.engine;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Mathematical facade. Basic primitives and selected transcendental routines
 * use NASM/JNI when safe; Java Math supplies IEEE special cases and stable
 * algorithms where the x87 instruction set has known range limitations.
 */
public final class MathEngine {
    private static final double TWO_TO_63 = 0x1.0p63;
    private static final double TRIG_X87_LIMIT = 0x1.0p63;
    private static final int MAX_COMBINATORIC_STEPS = 100_000;

    private MathEngine() { }

    public static boolean isAssemblyActive() { return NativeMath.isAvailable(); }

    public static double add(double a, double b) {
        return NativeMath.isAvailable() ? NativeMath.add(a, b) : a + b;
    }

    public static double subtract(double a, double b) {
        return NativeMath.isAvailable() ? NativeMath.subtract(a, b) : a - b;
    }

    public static double multiply(double a, double b) {
        return NativeMath.isAvailable() ? NativeMath.multiply(a, b) : a * b;
    }

    public static double divide(double a, double b) {
        if (b == 0.0) throw new ArithmeticException("Division by zero");
        return NativeMath.isAvailable() ? NativeMath.divide(a, b) : a / b;
    }

    public static double sin(double x) {
        if (!Double.isFinite(x) || Math.abs(x) >= TRIG_X87_LIMIT) return Math.sin(x);
        return NativeMath.isAvailable() ? NativeMath.sin(x) : Math.sin(x);
    }

    public static double cos(double x) {
        if (!Double.isFinite(x) || Math.abs(x) >= TRIG_X87_LIMIT) return Math.cos(x);
        return NativeMath.isAvailable() ? NativeMath.cos(x) : Math.cos(x);
    }

    public static double tan(double x) {
        if (!Double.isFinite(x) || Math.abs(x) >= TRIG_X87_LIMIT) return Math.tan(x);
        return NativeMath.isAvailable() ? NativeMath.tan(x) : Math.tan(x);
    }

    public static double sqrt(double x) {
        if (x < 0.0) throw new ArithmeticException("Square root requires a non-negative value");
        return NativeMath.isAvailable() ? NativeMath.sqrt(x) : Math.sqrt(x);
    }

    public static double cbrt(double x) { return Math.cbrt(x); }

    /** nth root with an integer, non-zero degree. */
    public static double nthRoot(double value, double degreeValue) {
        long degree = integerArgument(degreeValue, "Root degree");
        if (degree == 0) throw new ArithmeticException("Root degree cannot be zero");
        if (value < 0.0 && (degree & 1L) == 0L) {
            throw new ArithmeticException("An even root of a negative value is not real");
        }
        double magnitude = Math.pow(Math.abs(value), 1.0 / Math.abs((double) degree));
        double root = value < 0.0 ? -magnitude : magnitude;
        return degree < 0 ? 1.0 / root : root;
    }

    public static double ln(double x) {
        if (x <= 0.0) throw new ArithmeticException("ln requires a positive value");
        return NativeMath.isAvailable() ? NativeMath.ln(x) : Math.log(x);
    }

    public static double log10(double x) {
        if (x <= 0.0) throw new ArithmeticException("log10 requires a positive value");
        return NativeMath.isAvailable() ? NativeMath.log10(x) : Math.log10(x);
    }

    public static double log2(double x) {
        if (x <= 0.0) throw new ArithmeticException("log2 requires a positive value");
        return NativeMath.isAvailable() ? NativeMath.log2(x) : (Math.log(x) / Math.log(2.0));
    }

    public static double logBase(double value, double base) {
        if (value <= 0.0 || base <= 0.0 || base == 1.0) {
            throw new ArithmeticException("Logarithm requires value > 0 and base > 0, base != 1");
        }
        return ln(value) / ln(base);
    }

    public static double exp(double x) {
        if (NativeMath.isAvailable() && Double.isFinite(x) && x > -700.0 && x < 700.0) {
            return NativeMath.exp(x);
        }
        return Math.exp(x);
    }

    public static double exp2(double x) {
        if (NativeMath.isAvailable() && Double.isFinite(x) && x > -1070.0 && x < 1020.0) {
            return NativeMath.exp2(x);
        }
        return Math.pow(2.0, x);
    }

    public static double pow(double base, double exponent) {
        if (!Double.isFinite(base) || !Double.isFinite(exponent)
                || base <= 0.0 || exponent == 0.0) {
            return Math.pow(base, exponent);
        }

        // Keep x87 FYL2X/F2XM1 away from the binary64 overflow and
        // subnormal boundaries, where libm has more complete rounding rules.
        double binaryExponent = (Math.log(base) / Math.log(2.0)) * exponent;
        if (!Double.isFinite(binaryExponent)
                || binaryExponent <= -1020.0 || binaryExponent >= 1020.0) {
            return Math.pow(base, exponent);
        }
        return NativeMath.isAvailable() ? NativeMath.pow(base, exponent) : Math.pow(base, exponent);
    }

    public static double atan(double x) {
        return NativeMath.isAvailable() && Double.isFinite(x)
                ? NativeMath.atan(x) : Math.atan(x);
    }

    public static double atan2(double y, double x) {
        if (!Double.isFinite(y) || !Double.isFinite(x) || y == 0.0 || x == 0.0) {
            return Math.atan2(y, x);
        }
        return NativeMath.isAvailable() ? NativeMath.atan2(y, x) : Math.atan2(y, x);
    }

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

    public static double sinh(double x) {
        if (NativeMath.isAvailable() && Math.abs(x) < 700.0) {
            double ex = NativeMath.exp(x);
            double emx = NativeMath.exp(-x);
            return NativeMath.divide(NativeMath.subtract(ex, emx), 2.0);
        }
        return Math.sinh(x);
    }

    public static double cosh(double x) {
        if (NativeMath.isAvailable() && Math.abs(x) < 700.0) {
            double ex = NativeMath.exp(x);
            double emx = NativeMath.exp(-x);
            return NativeMath.divide(NativeMath.add(ex, emx), 2.0);
        }
        return Math.cosh(x);
    }

    public static double tanh(double x) {
        if (NativeMath.isAvailable() && Math.abs(x) < 700.0) {
            double ex = NativeMath.exp(x);
            double emx = NativeMath.exp(-x);
            return NativeMath.divide(NativeMath.subtract(ex, emx), NativeMath.add(ex, emx));
        }
        return Math.tanh(x);
    }

    public static double asinh(double x) {
        if (!Double.isFinite(x)) return x;
        double ax = Math.abs(x);
        double r = ln(add(ax, sqrt(add(multiply(ax, ax), 1.0))));
        return Math.copySign(r, x);
    }

    public static double acosh(double x) {
        if (x < 1.0) throw new ArithmeticException("acosh requires a value >= 1");
        if (x == 1.0) return 0.0;
        return ln(add(x, sqrt(subtract(multiply(x, x), 1.0))));
    }

    public static double atanh(double x) {
        if (Math.abs(x) >= 1.0) throw new ArithmeticException("atanh requires -1 < x < 1");
        return multiply(0.5, ln(divide(add(1.0, x), subtract(1.0, x))));
    }

    public static double sec(double x) { return reciprocalTrig(cos(x), "sec"); }
    public static double csc(double x) { return reciprocalTrig(sin(x), "csc"); }
    public static double cot(double x) { return reciprocalTrig(tan(x), "cot"); }

    private static double reciprocalTrig(double denominator, String name) {
        if (Math.abs(denominator) < 1.0e-14) {
            throw new ArithmeticException(name + " is undefined at this angle");
        }
        return divide(1.0, denominator);
    }

    public static double abs(double x) { return Math.abs(x); }
    public static double floor(double x) { return Math.floor(x); }
    public static double ceil(double x) { return Math.ceil(x); }
    public static double trunc(double x) { return x < 0.0 ? Math.ceil(x) : Math.floor(x); }
    public static double round(double x) {
        return Math.copySign(Math.floor(Math.abs(x) + 0.5), x);
    }
    public static double sign(double x) { return Math.signum(x); }
    public static double hypot(double x, double y) {
        return sqrt(add(multiply(x, x), multiply(y, y)));
    }

    /** Mathematical modulo: result has the sign of the divisor. */
    public static double modulo(double a, double b) {
        requireFinite(a);
        requireFinite(b);
        if (b == 0.0) throw new ArithmeticException("Modulo by zero");
        double result = a % b;
        if (result == 0.0) return Math.copySign(0.0, b);
        if (Math.signum(result) != Math.signum(b)) result += b;
        return result;
    }

    /** IEEE remainder, rounded quotient with ties to even. */
    public static double remainder(double a, double b) {
        if (b == 0.0) throw new ArithmeticException("Remainder by zero");
        return Math.IEEEremainder(a, b);
    }

    /** Truncating integer division, returned as a double for expression composition. */
    public static double integerDivide(double a, double b) {
        requireFinite(a);
        requireFinite(b);
        if (b == 0.0) throw new ArithmeticException("Integer division by zero");
        return trunc(a / b);
    }

    public static double factorial(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value != Math.rint(value)) {
            throw new ArithmeticException("Factorial requires a non-negative integer");
        }
        if (value > 170.0) return Double.POSITIVE_INFINITY;
        long n = (long) value;
        if (NativeMath.isAvailable()) return NativeMath.factorial(n);
        double result = 1.0;
        for (long i = 2; i <= n; i++) result *= i;
        return result;
    }

    public static double mean(double... values) {
        requireValues(values, 1, "mean");
        if (NativeMath.isAvailable()) return NativeMath.statMean(values);
        double scale = 0.0;
        for (double value : values) {
            requireFinite(value);
            scale = Math.max(scale, Math.abs(value));
        }
        if (scale == 0.0) return 0.0;

        double normalizedSum = 0.0;
        double compensation = 0.0;
        for (double value : values) {
            double adjusted = value / scale - compensation;
            double next = normalizedSum + adjusted;
            compensation = (next - normalizedSum) - adjusted;
            normalizedSum = next;
        }
        return (normalizedSum / values.length) * scale;
    }

    public static double median(double... values) {
        requireValues(values, 1, "median");
        double[] copy = values.clone();
        for (double value : copy) requireFinite(value);
        Arrays.sort(copy);
        int middle = copy.length / 2;
        return copy.length % 2 == 1 ? copy[middle] : copy[middle - 1] / 2.0 + copy[middle] / 2.0;
    }

    public static double variance(double... values) { return varianceOf(values, false); }
    public static double sampleVariance(double... values) { return varianceOf(values, true); }
    public static double standardDeviation(double... values) {
        if (NativeMath.isAvailable() && values != null && values.length >= 1) return NativeMath.statStdDev(values, false);
        return Math.sqrt(variance(values));
    }
    public static double sampleStandardDeviation(double... values) {
        if (NativeMath.isAvailable() && values != null && values.length >= 2) return NativeMath.statStdDev(values, true);
        return Math.sqrt(sampleVariance(values));
    }

    public static double[] linearRegression(double[] x, double[] y) {
        if (NativeMath.isAvailable()) {
            double[] res = NativeMath.statLinearReg(x, y);
            if (res != null) return res;
        }
        int n = Math.min(x.length, y.length);
        if (n < 2) return new double[]{0, 0, 0};
        double sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            sx += x[i]; sy += y[i]; sxx += x[i]*x[i]; syy += y[i]*y[i]; sxy += x[i]*y[i];
        }
        double deltaX = n * sxx - sx * sx;
        double deltaY = n * syy - sy * sy;
        double cov = n * sxy - sx * sy;
        double b = Math.abs(deltaX) > 1e-15 ? cov / deltaX : 0;
        double a = (sy - b * sx) / n;
        double denomR = Math.sqrt(Math.abs(deltaX * deltaY));
        double r = denomR > 1e-15 ? cov / denomR : 0;
        return new double[]{a, b, r};
    }

    private static double varianceOf(double[] values, boolean sample) {
        requireValues(values, sample ? 2 : 1, sample ? "sample variance" : "variance");
        double scale = 0.0;
        for (double value : values) {
            requireFinite(value);
            scale = Math.max(scale, Math.abs(value));
        }
        if (scale == 0.0) return 0.0;

        double mean = 0.0;
        double sumSquares = 0.0;
        long count = 0;
        for (double value : values) {
            double normalized = value / scale;
            count++;
            double delta = normalized - mean;
            mean += delta / count;
            sumSquares += delta * (normalized - mean);
        }
        double denominator = sample ? values.length - 1.0 : values.length;
        return scale * (scale * (sumSquares / denominator));
    }

    public static double sum(double... values) {
        requireValues(values, 1, "sum");
        if (NativeMath.isAvailable()) return NativeMath.statSum(values);
        double scale = 0.0;
        for (double value : values) {
            requireFinite(value);
            scale = Math.max(scale, Math.abs(value));
        }
        if (scale == 0.0) return 0.0;

        double normalizedSum = 0.0;
        double compensation = 0.0;
        for (double value : values) {
            double adjusted = value / scale - compensation;
            double next = normalizedSum + adjusted;
            compensation = (next - normalizedSum) - adjusted;
            normalizedSum = next;
        }
        return normalizedSum * scale;
    }

    public static double product(double... values) {
        requireValues(values, 1, "product");
        double result = 1.0;
        for (double value : values) {
            requireFinite(value);
            result *= value;
        }
        return result;
    }

    public static double min(double... values) {
        requireValues(values, 2, "min");
        double result = Double.POSITIVE_INFINITY;
        for (double value : values) { requireFinite(value); result = Math.min(result, value); }
        return result;
    }

    public static double max(double... values) {
        requireValues(values, 2, "max");
        double result = Double.NEGATIVE_INFINITY;
        for (double value : values) { requireFinite(value); result = Math.max(result, value); }
        return result;
    }

    public static double gcd(double a, double b) {
        BigInteger left = BigInteger.valueOf(integerArgument(a, "gcd argument"));
        BigInteger right = BigInteger.valueOf(integerArgument(b, "gcd argument"));
        return left.gcd(right).doubleValue();
    }

    public static double lcm(double a, double b) {
        BigInteger left = BigInteger.valueOf(integerArgument(a, "lcm argument"));
        BigInteger right = BigInteger.valueOf(integerArgument(b, "lcm argument"));
        if (left.signum() == 0 || right.signum() == 0) return 0.0;
        return left.divide(left.gcd(right)).multiply(right).abs().doubleValue();
    }

    public static double permutation(double nValue, double rValue) {
        long n = integerArgument(nValue, "n");
        long r = integerArgument(rValue, "r");
        validateCombinatorics(n, r);
        if (r > MAX_COMBINATORIC_STEPS) throw new ArithmeticException("Permutation is too large to calculate interactively");
        double result = 1.0;
        for (long i = 0; i < r; i++) result *= (double) (n - i);
        return result;
    }

    public static double combination(double nValue, double rValue) {
        long n = integerArgument(nValue, "n");
        long r = integerArgument(rValue, "r");
        validateCombinatorics(n, r);
        r = Math.min(r, n - r);
        if (r > MAX_COMBINATORIC_STEPS) throw new ArithmeticException("Combination is too large to calculate interactively");
        double result = 1.0;
        for (long i = 1; i <= r; i++) {
            result *= (double) (n - r + i);
            result /= (double) i;
        }
        return result;
    }

    private static void validateCombinatorics(long n, long r) {
        if (n < 0 || r < 0 || r > n) {
            throw new ArithmeticException("Combinatorics requires integers with n >= r >= 0");
        }
    }

    private static long integerArgument(double value, String label) {
        if (!Double.isFinite(value) || value != Math.rint(value)
                || value < -TWO_TO_63 || value >= TWO_TO_63) {
            throw new ArithmeticException(label + " must be a 64-bit integer");
        }
        return (long) value;
    }

    private static void requireValues(double[] values, int minimum, String name) {
        if (values == null || values.length < minimum) {
            throw new ArithmeticException(name + " requires at least " + minimum + " value(s)");
        }
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) throw new ArithmeticException("Statistical functions require finite values");
    }
}
