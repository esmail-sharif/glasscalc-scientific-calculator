package com.esmail.calculator.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A small recursive-descent parser. Parsing is Java-side; every numeric primitive is delegated to MathEngine. */
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
        return c == '(' || Character.isLetter(c) || c == 'π';
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
        if (!isAtEnd() && (Character.isLetter(peek()) || peek() == 'π')) return parseIdentifier();
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
        while (!isAtEnd() && (Character.isLetter(peek()) || peek() == 'π')) cursor++;
        String name = source.substring(start, cursor).toLowerCase(Locale.ROOT);
        skipSpaces();
        if (!match('(')) {
            return switch (name) {
                case "pi", "π" -> Math.PI;
                case "e" -> Math.E;
                case "ans" -> answer;
                case "tau", "τ" -> Math.PI * 2.0;
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

    private double applyFunction(String name, List<Double> a) {
        return switch (name) {
            case "sin" -> one(name, a, x -> MathEngine.sin(toRadians(x)));
            case "cos" -> one(name, a, x -> MathEngine.cos(toRadians(x)));
            case "tan" -> one(name, a, x -> {
                double radians = toRadians(x);
                if (Math.abs(Math.cos(radians)) < 1e-14) throw new ArithmeticException("tan is undefined at this angle");
                return MathEngine.tan(radians);
            });
            case "asin" -> one(name, a, x -> fromRadians(MathEngine.asin(x)));
            case "acos" -> one(name, a, x -> fromRadians(MathEngine.acos(x)));
            case "atan" -> one(name, a, x -> fromRadians(MathEngine.atan(x)));
            case "sqrt" -> one(name, a, MathEngine::sqrt);
            case "ln" -> one(name, a, MathEngine::ln);
            case "log", "log10" -> one(name, a, MathEngine::log10);
            case "abs" -> one(name, a, Math::abs);
            case "exp" -> one(name, a, x -> MathEngine.pow(Math.E, x));
            case "fact" -> one(name, a, MathEngine::factorial);
            case "pow" -> two(name, a, MathEngine::pow);
            case "min" -> two(name, a, Math::min);
            case "max" -> two(name, a, Math::max);
            default -> throw error("Unknown function: " + name);
        };
    }

    private double toRadians(double value) { return degrees ? Math.toRadians(value) : value; }
    private double fromRadians(double value) { return degrees ? Math.toDegrees(value) : value; }

    @FunctionalInterface private interface UnaryFunction { double apply(double value); }
    @FunctionalInterface private interface BinaryFunction { double apply(double left, double right); }

    private double one(String name, List<Double> values, UnaryFunction function) {
        if (values.size() != 1) throw error(name + " expects one argument");
        return function.apply(values.get(0));
    }

    private double two(String name, List<Double> values, BinaryFunction function) {
        if (values.size() != 2) throw error(name + " expects two arguments");
        return function.apply(values.get(0), values.get(1));
    }

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
