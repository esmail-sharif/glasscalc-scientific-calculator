package com.esmail.calculator.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recursive-descent expression parser supporting Casio natural textbook syntax,
 * auto-closing parentheses, right-associative powers, implicit multiplication,
 * variables (A-F, X, Y, M), numerical integration (∫ dx), and Casio scientific operators.
 */
public final class ExpressionParser {
    public enum AngleUnit {
        DEG, RAD, GRA
    }

    private final String source;
    private final AngleUnit angleUnit;
    private final double answer;
    private final Map<String, Double> variables;
    private int cursor;

    public ExpressionParser(String source, boolean degrees, double answer) {
        this(source, degrees ? AngleUnit.DEG : AngleUnit.RAD, answer, new HashMap<>());
    }

    public ExpressionParser(String source, boolean degrees, double answer, Map<String, Double> variables) {
        this(source, degrees ? AngleUnit.DEG : AngleUnit.RAD, answer, variables);
    }

    public ExpressionParser(String source, AngleUnit angleUnit, double answer) {
        this(source, angleUnit, answer, new HashMap<>());
    }

    public ExpressionParser(String source, AngleUnit angleUnit, double answer, Map<String, Double> variables) {
        this.source = balanceParentheses(source == null ? "" : source.trim());
        this.angleUnit = angleUnit == null ? AngleUnit.DEG : angleUnit;
        this.answer = answer;
        this.variables = variables == null ? new HashMap<>() : variables;
    }

    public boolean isDegrees() {
        return angleUnit == AngleUnit.DEG;
    }

    public AngleUnit getAngleUnit() {
        return angleUnit;
    }

    public Map<String, Double> getVariables() {
        return variables;
    }

    /**
     * Casio Natural Syntax: Automatically balances unclosed trailing parentheses
     * so expressions like cos(54 or sin(30 or ((1+2)*(3+4 evaluate seamlessly.
     */
    public static String balanceParentheses(String expr) {
        if (expr == null || expr.isBlank()) return "";
        int open = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') open++;
            else if (c == ')') {
                if (open > 0) open--;
            }
        }
        if (open > 0) {
            StringBuilder sb = new StringBuilder(expr);
            for (int i = 0; i < open; i++) {
                sb.append(')');
            }
            return sb.toString();
        }
        return expr;
    }

    public double evaluate() {
        if (source.isBlank()) throw error("Enter an expression");
        double value = parseExpression();
        skipSpaces();
        if (!isAtEnd()) throw error("Unexpected character '" + peek() + "'");
        if (!Double.isFinite(value)) throw new ArithmeticException("Result is outside numeric range");
        return value;
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            skipSpaces();
            if (match('+')) value = MathEngine.add(value, parseTerm());
            else if (match('-') || match('−')) value = MathEngine.subtract(value, parseTerm());
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseUnary();
        while (true) {
            skipSpaces();
            if (match('*') || match('×')) value = MathEngine.multiply(value, parseUnary());
            else if (match('/') || match('÷')) value = MathEngine.divide(value, parseUnary());
            else if (match('⌟')) {
                double next = parseUnary();
                skipSpaces();
                if (match('⌟') || match('/')) {
                    double den = parseUnary();
                    value = MathEngine.add(value, MathEngine.divide(next, den));
                } else {
                    value = MathEngine.divide(value, next);
                }
            }
            else if (startsImplicitFactor()) value = MathEngine.multiply(value, parseUnary());
            else return value;
        }
    }

    private boolean startsImplicitFactor() {
        if (isAtEnd()) return false;
        char c = peek();
        return c == '(' || Character.isLetter(c) || c == 'π' || c == 'τ' || c == 'φ' || c == '√'
                || c == '³' || c == '∛' || c == 'μ' || c == 'ħ' || c == 'λ' || c == 'γ' || c == 'α' || c == '∫'
                || c == 'Σ' || c == '∑';
    }

    private double parseUnary() {
        skipSpaces();
        if (match('+')) return parseUnary();
        if (match('-') || match('−')) return -parseUnary();
        return parsePower();
    }

    private double parsePower() {
        double value = parsePostfix();
        skipSpaces();
        if (match('^')) {
            value = MathEngine.pow(value, parseUnary());
        } else if (!isAtEnd() && (peek() == 'P' || peek() == 'p')
                && (cursor + 1 < source.length() && !Character.isLetter(source.charAt(cursor + 1)))) {
            cursor++;
            value = MathEngine.permutation(value, parseUnary());
        } else if (!isAtEnd() && (peek() == 'C' || peek() == 'c')
                && (cursor + 1 < source.length() && !Character.isLetter(source.charAt(cursor + 1)))) {
            cursor++;
            value = MathEngine.combination(value, parseUnary());
        }
        return value;
    }

    private double parsePostfix() {
        double value = parsePrimary();
        while (true) {
            skipSpaces();
            if (match('!')) value = MathEngine.factorial(value);
            else if (match('%')) value = MathEngine.divide(value, 100.0);
            else if (match('°')) {
                value = fromRadians(Math.toRadians(value));
            } else if (cursor < source.length() && (source.charAt(cursor) == 'r' || source.charAt(cursor) == 'R')
                    && (cursor + 1 >= source.length() || !Character.isLetterOrDigit(source.charAt(cursor + 1)))) {
                cursor++;
                value = fromRadians(value);
            } else if (cursor < source.length() && (source.charAt(cursor) == 'g' || source.charAt(cursor) == 'G')
                    && (cursor + 1 >= source.length() || !Character.isLetterOrDigit(source.charAt(cursor + 1)))) {
                cursor++;
                value = fromRadians(value * Math.PI / 200.0);
            }
            else return value;
        }
    }

    private double parsePrimary() {
        skipSpaces();
        if (match('(')) {
            double value = parseExpression();
            expect(')');
            return value;
        }
        if (match('⌟')) {
            double num = parseUnary();
            skipSpaces();
            if (match('⌟') || match('/')) {
                double den = parseUnary();
                return MathEngine.divide(num, den);
            }
            return num;
        }
        if (match('∛')) {
            if (match('(')) {
                double val = parseExpression();
                expect(')');
                return MathEngine.cbrt(val);
            }
            return MathEngine.cbrt(parsePrimary());
        }
        if (match('³')) {
            expect('√');
            if (match('(')) {
                double val = parseExpression();
                expect(')');
                return MathEngine.cbrt(val);
            }
            return MathEngine.cbrt(parsePrimary());
        }
        if (!isAtEnd() && peek() == '3' && cursor + 1 < source.length() && source.charAt(cursor + 1) == '√') {
            cursor += 2;
            if (match('(')) {
                double val = parseExpression();
                expect(')');
                return MathEngine.cbrt(val);
            }
            return MathEngine.cbrt(parsePrimary());
        }
        if (match('√')) {
            if (match('(')) {
                double val = parseExpression();
                expect(')');
                return MathEngine.sqrt(val);
            }
            return MathEngine.sqrt(parsePrimary());
        }
        if (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) return parseNumber();
        if (!isAtEnd() && (Character.isLetter(peek()) || peek() == 'π' || peek() == 'τ' || peek() == 'φ' || peek() == '∫')) {
            return parseIdentifier();
        }
        throw error("Expected a number, constant, or function");
    }

    private double parseNumber() {
        int start = cursor;
        boolean hasDigit = false;
        while (!isAtEnd() && Character.isDigit(peek())) { cursor++; hasDigit = true; }
        if (match('.')) {
            while (!isAtEnd() && Character.isDigit(peek())) { cursor++; hasDigit = true; }
        }
        if (!hasDigit) throw error("Invalid number");
        if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
            int exponentStart = cursor++;
            if (!isAtEnd() && (peek() == '+' || peek() == '-')) cursor++;
            int digitsStart = cursor;
            while (!isAtEnd() && Character.isDigit(peek())) cursor++;
            if (digitsStart == cursor) cursor = exponentStart;
        }
        try {
            return Double.parseDouble(source.substring(start, cursor));
        } catch (NumberFormatException ex) {
            throw error("Invalid number format");
        }
    }

    private double parseIdentifier() {
        int start = cursor;
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '∞'
                || peek() == 'π' || peek() == 'τ' || peek() == 'φ' || peek() == '∫' || peek() == '#'
                || peek() == 'Σ' || peek() == '∑'
                || peek() == 'μ' || peek() == 'ħ' || peek() == 'λ' || peek() == 'γ' || peek() == 'α' || peek() == 'ε' || peek() == 'σ' || peek() == 'φ'
                || peek() == '⁻' || peek() == '¹' || peek() == '²' || peek() == '³')) {
            cursor++;
        }
        String name = source.substring(start, cursor).toLowerCase(Locale.ROOT);
        if (name.equals("sin⁻¹") || name.equals("sin^-1")) name = "asin";
        if (name.equals("cos⁻¹") || name.equals("cos^-1")) name = "acos";
        if (name.equals("tan⁻¹") || name.equals("tan^-1")) name = "atan";
        if (name.equals("sinh⁻¹") || name.equals("sinh^-1")) name = "asinh";
        if (name.equals("cosh⁻¹") || name.equals("cosh^-1")) name = "acosh";
        if (name.equals("tanh⁻¹") || name.equals("tanh^-1")) name = "atanh";
        skipSpaces();

        // Casio Numerical Integration: ∫(expr, a, b) or integrate(expr, a, b)
        if (name.equals("integrate") || name.equals("∫")) {
            expect('(');
            int exprStart = cursor;
            int depth = 0;
            while (!isAtEnd()) {
                char ch = peek();
                if (ch == '(') depth++;
                else if (ch == ')') {
                    if (depth == 0) break;
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    break;
                }
                cursor++;
            }
            String subExpr = source.substring(exprStart, cursor).trim();
            expect(',');
            double a = parseExpression();
            expect(',');
            double b = parseExpression();
            expect(')');
            return evaluateIntegral(subExpr, a, b);
        }

        // Casio Numerical Differentiation: diff(expr, x0) or d/dx(expr, x0)
        if (name.equals("diff") || name.equals("d/dx")) {
            expect('(');
            int exprStart = cursor;
            int depth = 0;
            while (!isAtEnd()) {
                char ch = peek();
                if (ch == '(') depth++;
                else if (ch == ')') {
                    if (depth == 0) break;
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    break;
                }
                cursor++;
            }
            String subExpr = source.substring(exprStart, cursor).trim();
            expect(',');
            double x0 = parseExpression();
            expect(')');
            return evaluateDerivative(subExpr, x0);
        }

        // Casio Sequential Summation: Σ(expr, a, b)
        if (name.equals("Σ") || name.equals("∑") || name.equals("σ") || name.equals("sigma")) {
            expect('(');
            int exprStart = cursor;
            int depth = 0;
            while (!isAtEnd()) {
                char ch = peek();
                if (ch == '(') depth++;
                else if (ch == ')') {
                    if (depth == 0) break;
                    depth--;
                } else if (ch == ',' && depth == 0) {
                    break;
                }
                cursor++;
            }
            String subExpr = source.substring(exprStart, cursor).trim();
            expect(',');
            double a = parseExpression();
            expect(',');
            double b = parseExpression();
            expect(')');
            return evaluateSummation(subExpr, (long) Math.round(a), (long) Math.round(b));
        }

        // Casio Pseudo-Random Number: Ran# (0.000 to 0.999)
        if (name.equals("ran#") || name.equals("ran") || name.equals("random")) {
            if (match('(')) {
                expect(')');
            }
            return Math.floor(Math.random() * 1000.0) / 1000.0;
        }

        if (!match('(')) {
            if (variables.containsKey(name)) return variables.get(name);
            String upper = name.toUpperCase(Locale.ROOT);
            if (variables.containsKey(upper)) return variables.get(upper);

            com.esmail.calculator.modes.CasioConstants.ConstantEntry ce = com.esmail.calculator.modes.CasioConstants.getBySymbol(name);
            if (ce != null) return ce.value();

            return switch (name) {
                case "pi", "π" -> Math.PI;
                case "e" -> Math.E;
                case "tau", "τ" -> Math.PI * 2.0;
                case "phi", "φ" -> (1.0 + Math.sqrt(5.0)) / 2.0;
                case "ans" -> answer;
                case "x", "y", "a", "b", "c", "d", "m" -> 0.0;
                default -> throw error("Unknown constant or variable: " + name);
            };
        }

        List<Double> arguments = new ArrayList<>();
        skipSpaces();
        if (!match(')')) {
            do { arguments.add(parseExpression()); skipSpaces(); } while (match(','));
            expect(')');
        }
        return applyFunction(name, arguments);
    }

    private double evaluateIntegral(String exprStr, double a, double b) {
        int n = 100;
        double h = (b - a) / n;
        double sum = 0.0;
        for (int i = 0; i <= n; i++) {
            double xVal = a + i * h;
            Map<String, Double> vars = new HashMap<>(this.variables);
            vars.put("x", xVal);
            vars.put("X", xVal);
            double fVal = new ExpressionParser(exprStr, angleUnit, answer, vars).evaluate();
            double weight = (i == 0 || i == n) ? 1.0 : (i % 2 == 1 ? 4.0 : 2.0);
            sum += weight * fVal;
        }
        return (h / 3.0) * sum;
    }

    private double evaluateDerivative(String exprStr, double x0) {
        double h = 1e-6;
        Map<String, Double> varsPlus = new HashMap<>(this.variables);
        varsPlus.put("x", x0 + h);
        varsPlus.put("X", x0 + h);
        double fPlus = new ExpressionParser(exprStr, angleUnit, answer, varsPlus).evaluate();

        Map<String, Double> varsMinus = new HashMap<>(this.variables);
        varsMinus.put("x", x0 - h);
        varsMinus.put("X", x0 - h);
        double fMinus = new ExpressionParser(exprStr, angleUnit, answer, varsMinus).evaluate();

        return (fPlus - fMinus) / (2.0 * h);
    }

    private double evaluateSummation(String exprStr, long a, long b) {
        if (b < a) throw new ArithmeticException("Math ERROR: Summation upper bound must be >= lower bound");
        if (b - a > 100_000) throw new ArithmeticException("Math ERROR: Summation range exceeds limits");
        double total = 0.0;
        for (long i = a; i <= b; i++) {
            Map<String, Double> vars = new HashMap<>(this.variables);
            vars.put("x", (double) i);
            vars.put("X", (double) i);
            double val = new ExpressionParser(exprStr, angleUnit, answer, vars).evaluate();
            total = MathEngine.add(total, val);
        }
        return total;
    }

    private double applyFunction(String name, List<Double> values) {
        double[] a = values.stream().mapToDouble(Double::doubleValue).toArray();
        return switch (name) {
            case "sin" -> one(name, a, x -> MathEngine.sin(toRadians(x)));
            case "cos" -> one(name, a, x -> MathEngine.cos(toRadians(x)));
            case "tan" -> one(name, a, x -> {
                double radians = toRadians(x);
                if (Math.abs(Math.cos(radians)) < 1e-14) throw new ArithmeticException("tan is undefined at this angle");
                return MathEngine.tan(radians);
            });
            case "sec" -> one(name, a, x -> MathEngine.sec(toRadians(x)));
            case "csc" -> one(name, a, x -> MathEngine.csc(toRadians(x)));
            case "cot" -> one(name, a, x -> MathEngine.cot(toRadians(x)));
            case "asin", "sin^-1" -> one(name, a, x -> fromRadians(MathEngine.asin(x)));
            case "acos", "cos^-1" -> one(name, a, x -> fromRadians(MathEngine.acos(x)));
            case "atan", "tan^-1" -> one(name, a, x -> fromRadians(MathEngine.atan(x)));
            case "atan2" -> two(name, a, (y, x) -> fromRadians(MathEngine.atan2(y, x)));
            case "sinh" -> one(name, a, MathEngine::sinh);
            case "cosh" -> one(name, a, MathEngine::cosh);
            case "tanh" -> one(name, a, MathEngine::tanh);
            case "asinh", "sinh^-1" -> one(name, a, MathEngine::asinh);
            case "acosh", "cosh^-1" -> one(name, a, MathEngine::acosh);
            case "atanh", "tanh^-1" -> one(name, a, MathEngine::atanh);
            case "sqrt" -> one(name, a, MathEngine::sqrt);
            case "cbrt" -> one(name, a, MathEngine::cbrt);
            case "root", "nroot" -> two(name, a, MathEngine::nthRoot);
            case "ln" -> one(name, a, MathEngine::ln);
            case "log", "log10" -> one(name, a, MathEngine::log10);
            case "log2" -> one(name, a, MathEngine::log2);
            case "logbase" -> two(name, a, MathEngine::logBase);
            case "exp" -> one(name, a, MathEngine::exp);
            case "exp2" -> one(name, a, MathEngine::exp2);
            case "pow" -> two(name, a, MathEngine::pow);
            case "abs" -> one(name, a, MathEngine::abs);
            case "floor" -> one(name, a, MathEngine::floor);
            case "ceil" -> one(name, a, MathEngine::ceil);
            case "round" -> one(name, a, MathEngine::round);
            case "trunc" -> one(name, a, MathEngine::trunc);
            case "sign", "signum" -> one(name, a, MathEngine::sign);
            case "hypot" -> two(name, a, MathEngine::hypot);
            case "pol" -> two(name, a, (x, y) -> {
                double r = MathEngine.hypot(x, y);
                double theta = fromRadians(MathEngine.atan2(y, x));
                variables.put("x", r);
                variables.put("X", r);
                variables.put("y", theta);
                variables.put("Y", theta);
                return r;
            });
            case "rec" -> two(name, a, (r, theta) -> {
                double rad = toRadians(theta);
                double x = MathEngine.multiply(r, MathEngine.cos(rad));
                double y = MathEngine.multiply(r, MathEngine.sin(rad));
                variables.put("x", x);
                variables.put("X", x);
                variables.put("y", y);
                variables.put("Y", y);
                return x;
            });
            case "mod" -> two(name, a, MathEngine::modulo);
            case "remainder", "rem" -> two(name, a, MathEngine::remainder);
            case "idiv" -> two(name, a, MathEngine::integerDivide);
            case "gcd" -> two(name, a, MathEngine::gcd);
            case "lcm" -> two(name, a, MathEngine::lcm);
            case "fact", "factorial" -> one(name, a, MathEngine::factorial);
            case "perm", "npr" -> two(name, a, MathEngine::permutation);
            case "comb", "ncr" -> two(name, a, MathEngine::combination);
            case "ranint", "ranint#" -> two(name, a, (min, max) -> min + Math.floor(Math.random() * (max - min + 1)));
            case "rnd" -> one(name, a, x -> Math.round(x * 1e10) / 1e10);
            case "p" -> one(name, a, MathEngine::probP);
            case "q" -> one(name, a, MathEngine::probQ);
            case "r" -> one(name, a, MathEngine::probR);
            case "mean", "average" -> MathEngine.mean(many(name, a, 1));
            case "median" -> MathEngine.median(many(name, a, 1));
            case "variance", "var" -> MathEngine.variance(many(name, a, 1));
            case "stdev", "stddev" -> MathEngine.standardDeviation(many(name, a, 1));
            case "samplevar", "samplevariance" -> MathEngine.sampleVariance(many(name, a, 2));
            case "samplestdev", "samplestddev" -> MathEngine.sampleStandardDeviation(many(name, a, 2));
            case "sum" -> MathEngine.sum(many(name, a, 1));
            case "product" -> MathEngine.product(many(name, a, 1));
            case "min" -> MathEngine.min(many(name, a, 2));
            case "max" -> MathEngine.max(many(name, a, 2));
            case "and" -> two(name, a, (x, y) -> (double) NativeMath.baseAnd((long) x, (long) y));
            case "or" -> two(name, a, (x, y) -> (double) NativeMath.baseOr((long) x, (long) y));
            case "xor" -> two(name, a, (x, y) -> (double) NativeMath.baseXor((long) x, (long) y));
            case "xnor" -> two(name, a, (x, y) -> (double) NativeMath.baseXnor((long) x, (long) y));
            case "not" -> one(name, a, x -> (double) NativeMath.baseNot((long) x));
            case "neg" -> one(name, a, x -> (double) NativeMath.baseNeg((long) x));
            case "conv" -> two(name, a, (code, val) -> com.esmail.calculator.modes.CasioConversions.convert((int) code, val));
            default -> throw error("Unknown function: " + name);
        };
    }

    private static double[] many(String name, double[] values, int minimum) {
        if (values.length < minimum) {
            throw new IllegalArgumentException(name + " expects at least " + minimum + " argument(s)");
        }
        return values;
    }

    private static double one(String name, double[] values, UnaryFunction function) {
        if (values.length != 1) throw new IllegalArgumentException(name + " expects one argument");
        return function.apply(values[0]);
    }

    private static double two(String name, double[] values, BinaryFunction function) {
        if (values.length != 2) throw new IllegalArgumentException(name + " expects two arguments");
        return function.apply(values[0], values[1]);
    }

    private double toRadians(double value) {
        return switch (angleUnit) {
            case DEG -> Math.toRadians(value);
            case RAD -> value;
            case GRA -> value * Math.PI / 200.0;
        };
    }

    private double fromRadians(double value) {
        return switch (angleUnit) {
            case DEG -> Math.toDegrees(value);
            case RAD -> value;
            case GRA -> value * 200.0 / Math.PI;
        };
    }

    @FunctionalInterface private interface UnaryFunction { double apply(double value); }
    @FunctionalInterface private interface BinaryFunction { double apply(double left, double right); }

    private void expect(char expected) {
        skipSpaces();
        if (!match(expected)) throw error("Expected '" + expected + "'");
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(cursor) != expected) return false;
        cursor++;
        return true;
    }

    private char peek() { return source.charAt(cursor); }
    private boolean isAtEnd() { return cursor >= source.length(); }
    private void skipSpaces() { while (!isAtEnd() && Character.isWhitespace(peek())) cursor++; }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + (cursor + 1));
    }
}
