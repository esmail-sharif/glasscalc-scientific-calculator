package com.esmail.calculator.engine;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Casio Natural Textbook Display formatter.
 * Handles automatic and on-demand conversion between exact mathematical forms
 * (fractions p/q, multiples of pi, square roots) and decimal representation.
 */
public final class CasioFormatter {
    private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.US);

    private CasioFormatter() { }

    /**
     * Converts a numeric value to its natural exact form (fraction, pi-multiple, root)
     * if applicable; otherwise falls back to clean decimal representation.
     */
    public static String toExactOrFraction(double value) {
        if (!Double.isFinite(value)) return formatDecimal(value);
        if (value == 0.0) return "0";

        // 1. Check for integer
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 1e-9) {
            return Long.toString((long) rounded);
        }

        // 2. Check for multiple of PI (e.g. pi, pi/2, pi/3, 2pi/3, etc. with small denominator <= 48)
        double piRatio = value / Math.PI;
        long[] piFrac = findFraction(piRatio, 48, 1e-7);
        if (piFrac != null && piFrac[1] >= 1) {
            long p = piFrac[0];
            long q = piFrac[1];
            if (q == 1) {
                return (p == 1 ? "" : (p == -1 ? "-" : p)) + "π";
            } else {
                String num = (p == 1 ? "π" : (p == -1 ? "-π" : p + "π"));
                return num + "/" + q;
            }
        }

        // 3. Check for clean square root: value^2 is integer (e.g. sqrt(2), sqrt(3), sqrt(12))
        double sign = Math.signum(value);
        double sq = value * value;
        if (sq > 1.0 && sq < 10000.0 && Math.abs(sq - Math.rint(sq)) < 1e-7) {
            long n = (long) Math.rint(sq);
            long outside = 1;
            long inside = n;
            for (long i = (long) Math.sqrt(n); i >= 2; i--) {
                if (inside % (i * i) == 0) {
                    outside *= i;
                    inside /= (i * i);
                }
            }
            if (inside != 1) {
                String res = (sign < 0 ? "-" : "") + (outside == 1 ? "" : outside) + "√" + inside;
                return res;
            }
        }

        // 4. Check for simple rational fraction p / q (small denominator <= 2000)
        long[] frac = findFraction(value, 2000, 1e-7);
        if (frac != null) {
            long p = frac[0];
            long q = frac[1];
            if (q > 1) {
                return p + "/" + q;
            }
        }

        return formatDecimal(value);
    }

    /** Formats a numeric value into a standard clean decimal string. */
    public static String formatDecimal(double value) {
        if (Double.isNaN(value)) return "Math ERROR";
        if (value == Double.POSITIVE_INFINITY) return "Math ERROR: Overflow";
        if (value == Double.NEGATIVE_INFINITY) return "Math ERROR: Underflow";

        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 1e-9) {
            return Long.toString((long) rounded);
        }

        DecimalFormat df = new DecimalFormat("0.##########", SYMBOLS);
        return df.format(value);
    }

    /** Continued-fractions algorithm for finding nearest rational approximation p/q. */
    private static long[] findFraction(double x, long maxDenominator, double tolerance) {
        long m00 = 1, m01 = 0, m10 = 0, m11 = 1;
        double sign = 1.0;
        if (x < 0) {
            sign = -1.0;
            x = -x;
        }

        double val = x;
        for (int iter = 0; iter < 40; iter++) {
            long a = (long) val;
            long t1 = m00 * a + m01;
            long t2 = m10 * a + m11;
            if (t2 > maxDenominator) break;

            m01 = m00;
            m00 = t1;
            m11 = m10;
            m10 = t2;

            double approx = (double) m00 / (double) m10;
            if (Math.abs(approx - x) < tolerance) {
                return new long[] { (long) (sign * m00), m10 };
            }

            if (Math.abs(val - a) < 1e-14) break;
            val = 1.0 / (val - a);
            if (Double.isInfinite(val)) break;
        }

        if (m10 >= 1 && Math.abs(((double) m00 / (double) m10) - x) < tolerance) {
            return new long[] { (long) (sign * m00), m10 };
        }
        return null;
    }
}
