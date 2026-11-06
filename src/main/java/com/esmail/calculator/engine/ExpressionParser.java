package com.esmail.calculator.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Recursive-descent expression parser with right-associative powers and implicit multiplication. */
public final class ExpressionParser {
    private final String source;
    private final boolean degrees;
    private final double answer;
    private int cursor;

    public ExpressionParser(String source, boolean degrees, double answer) {
        this.source = source == null ? "" : source;
        this.degrees = degrees;
        this.answer = answer;
    }

    public double evaluate() {
        if (source.isBlank()) throw error("Enter an expression");
        double value = parseExpression();
        skipSpaces();
        if (!isAtEnd()) throw error("Unexpected character '" + peek() + "'");
        if (!Double.isFinite(value)) throw error("The result is outside the supported numeric range");
        return value;
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            skipSpaces();
            if (match('+')) value = MathEngine.add(value, parseTerm());
            else if (match('-')) value = MathEngine.subtract(value, parseTerm());
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseUnary();
        while (true) {
            skipSpaces();
            if (match('*')) value = MathEngine.multiply(value, parseUnary());
            else if (match('/')) value = MathEngine.divide(value, parseUnary());
            else if (startsImplicitFactor()) value = MathEngine.multiply(value, parseUnary());
            else return value;
        }
    }

    private boolean startsImplicitFactor() {
        if (isAtEnd()) return false;
        char c = peek();
        return c == '(' || Character.isLetter(c) || c == 'π' || c == 'τ' || c == 'φ';
    }

    private double parseUnary() {
        skipSpaces();
        if (match('+')) return parseUnary();
        if (match('-')) return -parseUnary();
        return parsePower();
    }

    private double parsePower() {
        double value = parsePostfix();
        skipSpaces();
        if (match('^')) value = MathEngine.pow(value, parseUnary());
        return value;
    }

    private double parsePostfix() {
        double value = parsePrimary();
        while (true) {
            skipSpaces();
            if (match('!')) value = MathEngine.factorial(value);
            else if (match('%')) value = MathEngine.divide(value, 100.0);
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
        if (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) return parseNumber();
        if (!isAtEnd() && (Character.isLetter(peek()) || peek() == 'π' || peek() == 'τ' || peek() == 'φ')) {
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
        while (!isAtEnd() && (Character.isLetter(peek()) || peek() == 'π' || peek() == 'τ' || peek() == 'φ')) cursor++;
        String name = source.substring(start, cursor).toLowerCase(Locale.ROOT);
        skipSpaces();
        if (!match('(')) {
            return switch (name) {
                case "pi", "π" -> Math.PI;
                case "e" -> Math.E;
                case "tau", "τ" -> Math.PI * 2.0;
                case "phi", "φ" -> (1.0 + Math.sqrt(5.0)) / 2.0;
                case "ans" -> answer;
                default -> throw error("Unknown constant: " + name);
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
            case "asin" -> one(name, a, x -> fromRadians(MathEngine.asin(x)));
            case "acos" -> one(name, a, x -> fromRadians(MathEngine.acos(x)));
            case "atan" -> one(name, a, x -> fromRadians(MathEngine.atan(x)));
            case "atan2" -> two(name, a, (y, x) -> fromRadians(MathEngine.atan2(y, x)));
            case "sinh" -> one(name, a, MathEngine::sinh);
            case "cosh" -> one(name, a, MathEngine::cosh);
            case "tanh" -> one(name, a, MathEngine::tanh);
            case "asinh" -> one(name, a, MathEngine::asinh);
            case "acosh" -> one(name, a, MathEngine::acosh);
            case "atanh" -> one(name, a, MathEngine::atanh);
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
            case "mod" -> two(name, a, MathEngine::modulo);
            case "remainder", "rem" -> two(name, a, MathEngine::remainder);
            case "idiv" -> two(name, a, MathEngine::integerDivide);
            case "gcd" -> two(name, a, MathEngine::gcd);
            case "lcm" -> two(name, a, MathEngine::lcm);
            case "fact", "factorial" -> one(name, a, MathEngine::factorial);
            case "perm", "npr" -> two(name, a, MathEngine::permutation);
            case "comb", "ncr" -> two(name, a, MathEngine::combination);
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

    private double toRadians(double value) { return degrees ? Math.toRadians(value) : value; }
    private double fromRadians(double value) { return degrees ? Math.toDegrees(value) : value; }

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
