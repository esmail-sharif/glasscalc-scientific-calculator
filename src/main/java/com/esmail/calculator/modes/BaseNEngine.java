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
        if (expr == null || expr.isBlank()) return 0;
        BaseNParser parser = new BaseNParser(expr, base);
        return parser.parse();
    }

    private static final class BaseNParser {
        private final String input;
        private final Base defaultBase;
        private int pos = 0;

        BaseNParser(String input, Base defaultBase) {
            this.input = input;
            this.defaultBase = defaultBase;
        }

        long parse() {
            skipWhitespace();
            if (isAtEnd()) return 0;
            long result = parseOr();
            skipWhitespace();
            if (!isAtEnd()) {
                throw new IllegalArgumentException("Syntax ERROR: Unexpected token '" + input.substring(pos) + "'");
            }
            return result;
        }

        // Level 1: OR
        private long parseOr() {
            long left = parseXor();
            while (true) {
                skipWhitespace();
                if (matchWord("or")) {
                    long right = parseXor();
                    left = or(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        // Level 2: XOR, XNOR
        private long parseXor() {
            long left = parseAnd();
            while (true) {
                skipWhitespace();
                if (matchWord("xnor")) {
                    long right = parseAnd();
                    left = xnor(left, right);
                } else if (matchWord("xor")) {
                    long right = parseAnd();
                    left = xor(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        // Level 3: AND
        private long parseAnd() {
            long left = parseAddSub();
            while (true) {
                skipWhitespace();
                if (matchWord("and")) {
                    long right = parseAddSub();
                    left = and(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        // Level 4: Add, Subtract (+, -)
        private long parseAddSub() {
            long left = parseMulDiv();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    long right = parseMulDiv();
                    left = left + right;
                } else if (match('-') || match('−')) {
                    long right = parseMulDiv();
                    left = left - right;
                } else {
                    break;
                }
            }
            return left;
        }

        // Level 5: Multiply, Divide (*, /)
        private long parseMulDiv() {
            long left = parseUnary();
            while (true) {
                skipWhitespace();
                if (match('*') || match('×')) {
                    long right = parseUnary();
                    left = left * right;
                } else if (match('/') || match('÷')) {
                    long right = parseUnary();
                    if (right == 0) throw new ArithmeticException("Division by zero");
                    left = left / right;
                } else {
                    break;
                }
            }
            return left;
        }

        // Level 6: Unary (+, -, not, neg)
        private long parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-') || match('−')) {
                return -parseUnary();
            }
            if (matchWord("not")) {
                return not(parseUnary());
            }
            if (matchWord("neg")) {
                return neg(parseUnary());
            }
            return parsePrimary();
        }

        // Level 7: Parentheses, Literals
        private long parsePrimary() {
            skipWhitespace();
            if (isAtEnd()) {
                throw new IllegalArgumentException("Syntax ERROR");
            }
            if (match('(')) {
                long val = parseOr();
                skipWhitespace();
                if (!match(')')) {
                    throw new IllegalArgumentException("Syntax ERROR: Missing closing parenthesis");
                }
                return val;
            }

            return parseNumber();
        }

        private long parseNumber() {
            skipWhitespace();
            int start = pos;
            while (!isAtEnd() && isBaseChar(peek(), defaultBase)) {
                pos++;
            }
            if (pos == start) {
                throw new IllegalArgumentException("Syntax ERROR: Invalid character at position " + pos);
            }
            String numStr = input.substring(start, pos);
            try {
                if (defaultBase == Base.DEC) {
                    return Long.parseLong(numStr, 10);
                } else {
                    return Long.parseUnsignedLong(numStr, defaultBase.radix);
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Syntax ERROR: Invalid number for radix " + defaultBase.radix + ": " + numStr);
            }
        }

        private boolean isBaseChar(char c, Base base) {
            return switch (base) {
                case BIN -> c == '0' || c == '1';
                case OCT -> c >= '0' && c <= '7';
                case DEC -> c >= '0' && c <= '9';
                case HEX -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            };
        }

        private boolean matchWord(String word) {
            int len = word.length();
            if (pos + len <= input.length()) {
                String sub = input.substring(pos, pos + len);
                if (sub.equalsIgnoreCase(word)) {
                    if (pos + len == input.length() || !Character.isLetterOrDigit(input.charAt(pos + len))) {
                        pos += len;
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean match(char expected) {
            if (!isAtEnd() && input.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        private char peek() {
            return input.charAt(pos);
        }

        private boolean isAtEnd() {
            return pos >= input.length();
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }
}
