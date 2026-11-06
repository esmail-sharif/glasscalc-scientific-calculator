package com.esmail.calculator.modes;

import com.esmail.calculator.engine.CasioFormatter;

import java.util.Locale;

/**
 * Robust Recursive-Descent Typed Evaluator for MATRIX and VECTOR modes.
 * Enforces operator precedence, parentheses, dimensions, and transactional Ans preservation.
 * Core vector and matrix primitives call MatrixEngine and VectorEngine (x86-64 NASM Assembly).
 */
public final class MatrixVectorEvaluator {

    private MatrixVectorEvaluator() {}

    // =========================================================================
    // MATRIX EVALUATOR
    // =========================================================================

    public sealed interface MatVal permits MatVal.Mat, MatVal.Num {
        record Mat(MatrixEngine.Matrix m) implements MatVal {}
        record Num(double v) implements MatVal {}
    }

    public static String evaluateMatrix(String expr) {
        if (expr == null || expr.isBlank()) return "[0]";
        MatrixParser parser = new MatrixParser(expr);
        MatVal res = parser.parse();
        if (res instanceof MatVal.Mat mat) {
            MatrixEngine.setMatrix("MATANS", mat.m());
            return mat.m().format();
        } else if (res instanceof MatVal.Num num) {
            return CasioFormatter.toExactOrFraction(num.v());
        }
        throw new IllegalArgumentException("Syntax ERROR");
    }

    private static final class MatrixParser {
        private final String input;
        private int pos = 0;

        MatrixParser(String input) {
            this.input = input.trim();
        }

        MatVal parse() {
            skipWhitespace();
            if (isAtEnd()) throw new IllegalArgumentException("Syntax ERROR: Empty expression");
            MatVal val = parseAddSub();
            skipWhitespace();
            if (!isAtEnd()) {
                throw new IllegalArgumentException("Syntax ERROR: Unexpected token '" + input.substring(pos) + "'");
            }
            return val;
        }

        private MatVal parseAddSub() {
            MatVal left = parseMul();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    MatVal right = parseMul();
                    left = add(left, right);
                } else if (match('-') || match('−')) {
                    MatVal right = parseMul();
                    left = sub(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        private MatVal parseMul() {
            MatVal left = parsePower();
            while (true) {
                skipWhitespace();
                if (match('*') || match('×')) {
                    MatVal right = parsePower();
                    left = mul(left, right);
                } else if (startsMatrixFactor()) {
                    MatVal right = parsePower();
                    left = mul(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        private MatVal parsePower() {
            MatVal left = parseUnary();
            skipWhitespace();
            while (true) {
                if (match('²') || matchWord("^2")) {
                    left = power(left, 2);
                } else if (match('³') || matchWord("^3")) {
                    left = power(left, 3);
                } else if (matchWord("^-1") || matchWord("⁻¹")) {
                    left = inverse(left);
                } else {
                    break;
                }
                skipWhitespace();
            }
            return left;
        }

        private MatVal parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-') || match('−')) {
                MatVal inner = parseUnary();
                if (inner instanceof MatVal.Num num) return new MatVal.Num(-num.v());
                if (inner instanceof MatVal.Mat mat) return new MatVal.Mat(mat.m().scale(-1.0));
            }
            if (matchWord("det") || matchWord("Det")) {
                return parseDet();
            }
            if (matchWord("abs") || matchWord("Abs")) {
                return parseAbs();
            }
            if (matchWord("trn") || matchWord("Trn")) {
                return parseTrn();
            }
            return parsePrimary();
        }

        private MatVal parseDet() {
            skipWhitespace();
            if (!match('(')) throw new IllegalArgumentException("Syntax ERROR: det requires (");
            MatVal arg = parseAddSub();
            skipWhitespace();
            if (!match(')')) throw new IllegalArgumentException("Syntax ERROR: det missing )");
            if (arg instanceof MatVal.Mat mat) {
                return new MatVal.Num(mat.m().determinant());
            }
            throw new IllegalArgumentException("Dimension ERROR: det requires a matrix argument");
        }

        private MatVal parseAbs() {
            skipWhitespace();
            if (!match('(')) throw new IllegalArgumentException("Syntax ERROR: Abs requires (");
            MatVal arg = parseAddSub();
            skipWhitespace();
            if (!match(')')) throw new IllegalArgumentException("Syntax ERROR: Abs missing )");
            if (arg instanceof MatVal.Mat mat) {
                return new MatVal.Num(mat.m().determinant());
            }
            if (arg instanceof MatVal.Num num) {
                return new MatVal.Num(Math.abs(num.v()));
            }
            throw new IllegalArgumentException("Syntax ERROR");
        }

        private MatVal parseTrn() {
            skipWhitespace();
            if (!match('(')) throw new IllegalArgumentException("Syntax ERROR: Trn requires (");
            MatVal arg = parseAddSub();
            skipWhitespace();
            if (!match(')')) throw new IllegalArgumentException("Syntax ERROR: Trn missing )");
            if (arg instanceof MatVal.Mat mat) {
                return new MatVal.Mat(mat.m().transpose());
            }
            throw new IllegalArgumentException("Dimension ERROR: Trn requires a matrix argument");
        }

        private MatVal parsePrimary() {
            skipWhitespace();
            if (isAtEnd()) throw new IllegalArgumentException("Syntax ERROR");
            if (match('(')) {
                MatVal val = parseAddSub();
                skipWhitespace();
                if (!match(')')) throw new IllegalArgumentException("Syntax ERROR: Missing )");
                return val;
            }

            if (Character.isDigit(peek()) || peek() == '.') {
                return new MatVal.Num(parseNumber());
            }

            String id = parseIdentifier();
            String upper = id.toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(upper);
            if (m == null && (upper.equals("ANS") || upper.equals("MATANS"))) {
                m = MatrixEngine.getMatrix("MATANS");
            }
            if (m != null) {
                return new MatVal.Mat(m);
            }
            throw new IllegalArgumentException("Syntax ERROR: Unknown matrix symbol '" + id + "'");
        }

        private boolean startsMatrixFactor() {
            if (isAtEnd()) return false;
            char c = peek();
            return c == '(' || Character.isDigit(c) || Character.isLetter(c);
        }

        private MatVal add(MatVal a, MatVal b) {
            if (a instanceof MatVal.Mat m1 && b instanceof MatVal.Mat m2) {
                return new MatVal.Mat(m1.m().add(m2.m()));
            }
            if (a instanceof MatVal.Num n1 && b instanceof MatVal.Num n2) {
                return new MatVal.Num(n1.v() + n2.v());
            }
            throw new IllegalArgumentException("Dimension ERROR: Cannot add matrix and scalar");
        }

        private MatVal sub(MatVal a, MatVal b) {
            if (a instanceof MatVal.Mat m1 && b instanceof MatVal.Mat m2) {
                return new MatVal.Mat(m1.m().subtract(m2.m()));
            }
            if (a instanceof MatVal.Num n1 && b instanceof MatVal.Num n2) {
                return new MatVal.Num(n1.v() - n2.v());
            }
            throw new IllegalArgumentException("Dimension ERROR: Cannot subtract matrix and scalar");
        }

        private MatVal mul(MatVal a, MatVal b) {
            if (a instanceof MatVal.Mat m1 && b instanceof MatVal.Mat m2) {
                return new MatVal.Mat(m1.m().multiply(m2.m()));
            }
            if (a instanceof MatVal.Num s && b instanceof MatVal.Mat m) {
                return new MatVal.Mat(m.m().scale(s.v()));
            }
            if (a instanceof MatVal.Mat m && b instanceof MatVal.Num s) {
                return new MatVal.Mat(m.m().scale(s.v()));
            }
            if (a instanceof MatVal.Num n1 && b instanceof MatVal.Num n2) {
                return new MatVal.Num(n1.v() * n2.v());
            }
            throw new IllegalArgumentException("Dimension ERROR");
        }

        private MatVal power(MatVal val, int exp) {
            if (val instanceof MatVal.Mat mat) {
                MatrixEngine.Matrix m = mat.m();
                if (m.rows != m.cols) throw new ArithmeticException("Dimension ERROR: Matrix power requires square matrix");
                MatrixEngine.Matrix res = m;
                for (int i = 1; i < exp; i++) res = res.multiply(m);
                return new MatVal.Mat(res);
            }
            if (val instanceof MatVal.Num num) {
                return new MatVal.Num(Math.pow(num.v(), exp));
            }
            throw new IllegalArgumentException("Syntax ERROR");
        }

        private MatVal inverse(MatVal val) {
            if (val instanceof MatVal.Mat mat) {
                return new MatVal.Mat(mat.m().inverse());
            }
            if (val instanceof MatVal.Num num) {
                if (num.v() == 0.0) throw new ArithmeticException("Math ERROR: Division by zero");
                return new MatVal.Num(1.0 / num.v());
            }
            throw new IllegalArgumentException("Syntax ERROR");
        }

        private double parseNumber() {
            int start = pos;
            while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) pos++;
            return Double.parseDouble(input.substring(start, pos));
        }

        private String parseIdentifier() {
            int start = pos;
            while (!isAtEnd() && Character.isLetterOrDigit(peek())) pos++;
            return input.substring(start, pos);
        }

        private boolean matchWord(String word) {
            int len = word.length();
            if (pos + len <= input.length() && input.substring(pos, pos + len).equalsIgnoreCase(word)) {
                pos += len;
                return true;
            }
            return false;
        }

        private boolean match(char c) {
            if (!isAtEnd() && input.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        private char peek() { return input.charAt(pos); }
        private boolean isAtEnd() { return pos >= input.length(); }
        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(input.charAt(pos))) pos++;
        }
    }

    // =========================================================================
    // VECTOR EVALUATOR
    // =========================================================================

    public sealed interface VctVal permits VctVal.Vct, VctVal.Num {
        record Vct(VectorEngine.Vector3D v) implements VctVal {}
        record Num(double v) implements VctVal {}
    }

    public static String evaluateVector(String expr) {
        return evaluateVector(expr, true);
    }

    public static String evaluateVector(String expr, boolean isDegrees) {
        if (expr == null || expr.isBlank()) return "[0, 0, 0]";
        VectorParser parser = new VectorParser(expr, isDegrees);
        VctVal res = parser.parse();
        if (res instanceof VctVal.Vct vct) {
            VectorEngine.setVector("VCTANS", vct.v());
            return vct.v().format();
        } else if (res instanceof VctVal.Num num) {
            return CasioFormatter.toExactOrFraction(num.v());
        }
        throw new IllegalArgumentException("Syntax ERROR");
    }

    private static final class VectorParser {
        private final String input;
        private final boolean isDegrees;
        private int pos = 0;

        VectorParser(String input, boolean isDegrees) {
            this.input = input.trim();
            this.isDegrees = isDegrees;
        }

        VctVal parse() {
            skipWhitespace();
            if (isAtEnd()) throw new IllegalArgumentException("Syntax ERROR: Empty expression");
            VctVal val = parseAddSub();
            skipWhitespace();
            if (!isAtEnd()) {
                throw new IllegalArgumentException("Syntax ERROR: Unexpected token '" + input.substring(pos) + "'");
            }
            return val;
        }

        private VctVal parseAddSub() {
            VctVal left = parseDotCross();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    VctVal right = parseDotCross();
                    left = add(left, right);
                } else if (match('-') || match('−')) {
                    VctVal right = parseDotCross();
                    left = sub(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        private VctVal parseDotCross() {
            VctVal left = parseMul();
            while (true) {
                skipWhitespace();
                if (matchWord("dot") || matchWord("Dot")) {
                    VctVal right = parseMul();
                    left = dot(left, right);
                } else if (matchWord("cross") || matchWord("Cross")) {
                    VctVal right = parseMul();
                    left = cross(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        private VctVal parseMul() {
            VctVal left = parseUnary();
            while (true) {
                skipWhitespace();
                if (match('*') || match('×')) {
                    VctVal right = parseUnary();
                    left = mul(left, right);
                } else if (startsVectorFactor()) {
                    VctVal right = parseUnary();
                    left = mul(left, right);
                } else {
                    break;
                }
            }
            return left;
        }

        private VctVal parseUnary() {
            skipWhitespace();
            if (match('+')) {
                return parseUnary();
            }
            if (match('-') || match('−')) {
                VctVal inner = parseUnary();
                if (inner instanceof VctVal.Num num) return new VctVal.Num(-num.v());
                if (inner instanceof VctVal.Vct vct) return new VctVal.Vct(vct.v().scale(-1.0));
            }
            if (matchWord("abs") || matchWord("Abs")) {
                return parseAbs();
            }
            if (matchWord("angle") || matchWord("Angle")) {
                return parseAngle();
            }
            return parsePrimary();
        }

        private VctVal parseAbs() {
            skipWhitespace();
            if (!match('(')) throw new IllegalArgumentException("Syntax ERROR: Abs requires (");
            VctVal arg = parseAddSub();
            skipWhitespace();
            if (!match(')')) throw new IllegalArgumentException("Syntax ERROR: Abs missing )");
            if (arg instanceof VctVal.Vct vct) {
                return new VctVal.Num(vct.v().norm());
            }
            if (arg instanceof VctVal.Num num) {
                return new VctVal.Num(Math.abs(num.v()));
            }
            throw new IllegalArgumentException("Syntax ERROR");
        }

        private VctVal parseAngle() {
            skipWhitespace();
            if (!match('(')) throw new IllegalArgumentException("Syntax ERROR: Angle requires (");
            VctVal arg1 = parseAddSub();
            skipWhitespace();
            if (!match(',')) throw new IllegalArgumentException("Syntax ERROR: Angle requires two comma-separated vectors");
            VctVal arg2 = parseAddSub();
            skipWhitespace();
            if (!match(')')) throw new IllegalArgumentException("Syntax ERROR: Angle missing )");
            if (arg1 instanceof VctVal.Vct v1 && arg2 instanceof VctVal.Vct v2) {
                return new VctVal.Num(v1.v().angle(v2.v(), isDegrees));
            }
            throw new IllegalArgumentException("Dimension ERROR: Angle requires two vector operands");
        }

        private VctVal parsePrimary() {
            skipWhitespace();
            if (isAtEnd()) throw new IllegalArgumentException("Syntax ERROR");
            if (match('(')) {
                VctVal val = parseAddSub();
                skipWhitespace();
                if (!match(')')) throw new IllegalArgumentException("Syntax ERROR: Missing )");
                return val;
            }

            if (Character.isDigit(peek()) || peek() == '.') {
                return new VctVal.Num(parseNumber());
            }

            String id = parseIdentifier();
            String upper = id.toUpperCase(Locale.ROOT);
            VectorEngine.Vector3D v = VectorEngine.getVector(upper);
            if (v == null && (upper.equals("ANS") || upper.equals("VCTANS"))) {
                v = VectorEngine.getVector("VCTANS");
            }
            if (v != null) {
                return new VctVal.Vct(v);
            }
            throw new IllegalArgumentException("Syntax ERROR: Unknown vector symbol '" + id + "'");
        }

        private boolean isKeywordAhead(String word) {
            int len = word.length();
            if (pos + len <= input.length()) {
                String sub = input.substring(pos, pos + len);
                if (sub.equalsIgnoreCase(word)) {
                    if (pos + len == input.length() || !Character.isLetterOrDigit(input.charAt(pos + len))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean startsVectorFactor() {
            skipWhitespace();
            if (isAtEnd()) return false;
            char c = peek();
            if (c == '(' || Character.isDigit(c)) return true;
            if (Character.isLetter(c)) {
                if (isKeywordAhead("dot") || isKeywordAhead("cross")) return false;
                return true;
            }
            return false;
        }

        private VctVal add(VctVal a, VctVal b) {
            if (a instanceof VctVal.Vct v1 && b instanceof VctVal.Vct v2) {
                return new VctVal.Vct(v1.v().add(v2.v()));
            }
            if (a instanceof VctVal.Num n1 && b instanceof VctVal.Num n2) {
                return new VctVal.Num(n1.v() + n2.v());
            }
            throw new IllegalArgumentException("Dimension ERROR: Cannot add vector and scalar");
        }

        private VctVal sub(VctVal a, VctVal b) {
            if (a instanceof VctVal.Vct v1 && b instanceof VctVal.Vct v2) {
                return new VctVal.Vct(v1.v().subtract(v2.v()));
            }
            if (a instanceof VctVal.Num n1 && b instanceof VctVal.Num n2) {
                return new VctVal.Num(n1.v() - n2.v());
            }
            throw new IllegalArgumentException("Dimension ERROR: Cannot subtract vector and scalar");
        }

        private VctVal dot(VctVal a, VctVal b) {
            if (a instanceof VctVal.Vct v1 && b instanceof VctVal.Vct v2) {
                return new VctVal.Num(v1.v().dot(v2.v()));
            }
            throw new IllegalArgumentException("Dimension ERROR: Dot product requires two vectors");
        }

        private VctVal cross(VctVal a, VctVal b) {
            if (a instanceof VctVal.Vct v1 && b instanceof VctVal.Vct v2) {
                return new VctVal.Vct(v1.v().cross(v2.v()));
            }
            throw new IllegalArgumentException("Dimension ERROR: Cross product requires two vectors");
        }

        private VctVal mul(VctVal a, VctVal b) {
            if (a instanceof VctVal.Num s && b instanceof VctVal.Vct v) {
                return new VctVal.Vct(v.v().scale(s.v()));
            }
            if (a instanceof VctVal.Vct v && b instanceof VctVal.Num s) {
                return new VctVal.Vct(v.v().scale(s.v()));
            }
            if (a instanceof VctVal.Num n1 && b instanceof VctVal.Num n2) {
                return new VctVal.Num(n1.v() * n2.v());
            }
            throw new IllegalArgumentException("Dimension ERROR: Use 'dot' or 'cross' for vector-vector multiplication");
        }

        private double parseNumber() {
            int start = pos;
            while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) pos++;
            return Double.parseDouble(input.substring(start, pos));
        }

        private String parseIdentifier() {
            int start = pos;
            while (!isAtEnd() && Character.isLetterOrDigit(peek())) pos++;
            return input.substring(start, pos);
        }

        private boolean matchWord(String word) {
            int len = word.length();
            if (pos + len <= input.length() && input.substring(pos, pos + len).equalsIgnoreCase(word)) {
                if (pos + len == input.length() || !Character.isLetterOrDigit(input.charAt(pos + len))) {
                    pos += len;
                    return true;
                }
            }
            return false;
        }

        private boolean match(char c) {
            if (!isAtEnd() && input.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        private char peek() { return input.charAt(pos); }
        private boolean isAtEnd() { return pos >= input.length(); }
        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(input.charAt(pos))) pos++;
        }
    }
}
