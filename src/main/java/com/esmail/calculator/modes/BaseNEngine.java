package com.esmail.calculator.modes;

import com.esmail.calculator.engine.NativeMath;

import java.util.Locale;

/**
 * CASIO BASE-N Mode Engine (Mode 4).
 * Handles DEC, HEX, BIN, OCT and Assembly-backed bitwise logic operations.
 */
public final class BaseNEngine {
    public enum Base {
        DEC(10, "Dec"),
        HEX(16, "Hex"),
        BIN(2, "Bin"),
        OCT(8, "Oct");

        public final int radix;
        public final String badge;

        Base(int radix, String badge) {
            this.radix = radix;
            this.badge = badge;
        }

        public String suffix() { return badge; }
    }

    private Base currentBase = Base.DEC;

    public Base getCurrentBase() { return currentBase; }
    public void setCurrentBase(Base base) { this.currentBase = base; }

    public static long and(long a, long b) {
        return NativeMath.isAvailable() ? NativeMath.baseAnd(a, b) : (a & b);
    }

    public static long or(long a, long b) {
        return NativeMath.isAvailable() ? NativeMath.baseOr(a, b) : (a | b);
    }

    public static long xor(long a, long b) {
        return NativeMath.isAvailable() ? NativeMath.baseXor(a, b) : (a ^ b);
    }

    public static long xnor(long a, long b) {
        return NativeMath.isAvailable() ? NativeMath.baseXnor(a, b) : ~(a ^ b);
    }

    public static long not(long a) {
        return NativeMath.isAvailable() ? NativeMath.baseNot(a) : ~a;
    }

    public static long neg(long a) {
        return NativeMath.isAvailable() ? NativeMath.baseNeg(a) : -a;
    }

    public static String format(long value, Base base) {
        return switch (base) {
            case DEC -> Long.toString(value);
            case HEX -> Long.toHexString(value).toUpperCase(Locale.ROOT);
            case BIN -> Long.toBinaryString(value);
            case OCT -> Long.toOctalString(value);
        };
    }

    public static long parse(String text, Base base) {
        String clean = text.trim();
        if (clean.isEmpty()) return 0;
        return Long.parseUnsignedLong(clean, base.radix);
    }

    public static long evaluate(String expr, Base base) {
        String clean = expr.trim();
        if (clean.isEmpty()) return 0;
        try {
            if (clean.contains("+")) {
                String[] parts = clean.split("\\+");
                return parse(parts[0], base) + parse(parts[1], base);
            }
            if (clean.contains("-") || clean.contains("−")) {
                String[] parts = clean.split("[-−]");
                return parse(parts[0], base) - parse(parts[1], base);
            }
            if (clean.contains("*") || clean.contains("×")) {
                String[] parts = clean.split("[*×]");
                return parse(parts[0], base) * parse(parts[1], base);
            }
            if (clean.contains("/") || clean.contains("÷")) {
                String[] parts = clean.split("[/÷]");
                long denom = parse(parts[1], base);
                if (denom == 0) throw new ArithmeticException("Division by zero");
                return parse(parts[0], base) / denom;
            }
            if (clean.toLowerCase(Locale.ROOT).contains("and")) {
                String[] parts = clean.toLowerCase(Locale.ROOT).split("and");
                return and(parse(parts[0], base), parse(parts[1], base));
            }
            if (clean.toLowerCase(Locale.ROOT).contains("xnor")) {
                String[] parts = clean.toLowerCase(Locale.ROOT).split("xnor");
                return xnor(parse(parts[0], base), parse(parts[1], base));
            }
            if (clean.toLowerCase(Locale.ROOT).contains("xor")) {
                String[] parts = clean.toLowerCase(Locale.ROOT).split("xor");
                return xor(parse(parts[0], base), parse(parts[1], base));
            }
            if (clean.toLowerCase(Locale.ROOT).contains("or")) {
                String[] parts = clean.toLowerCase(Locale.ROOT).split("or");
                return or(parse(parts[0], base), parse(parts[1], base));
            }
            if (clean.toLowerCase(Locale.ROOT).startsWith("not(") && clean.endsWith(")")) {
                String inner = clean.substring(4, clean.length() - 1);
                return not(parse(inner, base));
            }
            if (clean.toLowerCase(Locale.ROOT).startsWith("neg(") && clean.endsWith(")")) {
                String inner = clean.substring(4, clean.length() - 1);
                return neg(parse(inner, base));
            }
            return parse(clean, base);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Syntax ERROR");
        }
    }
}
