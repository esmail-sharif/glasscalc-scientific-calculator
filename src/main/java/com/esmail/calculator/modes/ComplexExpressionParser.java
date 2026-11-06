package com.esmail.calculator.modes;

import com.esmail.calculator.engine.ExpressionParser;

import java.util.Locale;

/**
 * Expression parser for CASIO CMPLX mode (Mode 2).
 * Parses complex expressions containing real and imaginary components ('i'),
 * arithmetic, powers, and complex functions (abs, arg, conj).
 * All complex multiplication and division are executed directly by x86-64 NASM Assembly.
 */
public final class ComplexExpressionParser {
    private final String source;
    private final boolean degrees;
    private int cursor;

    public ComplexExpressionParser(String source, boolean degrees) {
        this.source = ExpressionParser.balanceParentheses(source == null ? "" : source.trim());
        this.degrees = degrees;
        this.cursor = 0;
    }

    public ComplexNumber evaluate() {
        if (source.isBlank()) throw new IllegalArgumentException("Enter a complex expression");
        ComplexNumber val = parseExpression();
        skipSpaces();
        if (cursor < source.length()) {
            throw new IllegalArgumentException("Unexpected character '" + source.charAt(cursor) + "'");
        }
        return val;
    }

    private ComplexNumber parseExpression() {
        ComplexNumber val = parseTerm();
        while (true) {
            skipSpaces();
            if (match('+')) {
                val = val.add(parseTerm());
            } else if (match('-') || match('−')) {
                val = val.subtract(parseTerm());
            } else {
                return val;
            }
        }
    }

    private ComplexNumber parseTerm() {
        ComplexNumber val = parseUnary();
        while (true) {
            skipSpaces();
            if (match('*') || match('×')) {
                val = val.multiply(parseUnary());
            } else if (match('/') || match('÷')) {
                val = val.divide(parseUnary());
            } else if (startsImplicitFactor()) {
                val = val.multiply(parseUnary());
            } else {
                return val;
            }
        }
    }

    private boolean startsImplicitFactor() {
        if (isAtEnd()) return false;
        char c = peek();
        return c == '(' || c == 'i' || c == 'I' || Character.isLetter(c);
    }

    private ComplexNumber parseUnary() {
        skipSpaces();
        if (match('+')) return parseUnary();
        if (match('-') || match('−')) {
            ComplexNumber u = parseUnary();
            return new ComplexNumber(-u.real(), -u.imag());
        }
        return parsePower();
    }

    private ComplexNumber parsePower() {
        ComplexNumber val = parsePrimary();
        skipSpaces();
        if (match('^')) {
            ComplexNumber exp = parseUnary();
            // Supports real exponentiation
            val = val.pow(exp.real());
        }
        return val;
    }

    private ComplexNumber parsePrimary() {
        skipSpaces();
        if (match('(')) {
            ComplexNumber inner = parseExpression();
            expect(')');
            return inner;
        }

        if (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
            double num = parseNumber();
            skipSpaces();
            if (!isAtEnd() && (peek() == 'i' || peek() == 'I')) {
                cursor++;
                return new ComplexNumber(0, num);
            }
            return new ComplexNumber(num, 0);
        }

        if (!isAtEnd() && (peek() == 'i' || peek() == 'I')) {
            cursor++;
            return ComplexNumber.I;
        }

        if (!isAtEnd() && Character.isLetter(peek())) {
            return parseFunction();
        }

        throw new IllegalArgumentException("Expected complex number or function");
    }

    private ComplexNumber parseFunction() {
        int start = cursor;
        while (!isAtEnd() && Character.isLetter(peek())) cursor++;
        String name = source.substring(start, cursor).toLowerCase(Locale.ROOT);
        skipSpaces();

        if (name.equals("i")) return ComplexNumber.I;

        expect('(');
        ComplexNumber arg = parseExpression();
        expect(')');

        return switch (name) {
            case "abs" -> new ComplexNumber(arg.abs(), 0);
            case "arg" -> new ComplexNumber(arg.arg(degrees), 0);
            case "conj", "conjg" -> arg.conjugate();
            case "re" -> new ComplexNumber(arg.real(), 0);
            case "im" -> new ComplexNumber(arg.imag(), 0);
            default -> throw new IllegalArgumentException("Unknown complex function: " + name);
        };
    }

    private double parseNumber() {
        int start = cursor;
        while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) cursor++;
        return Double.parseDouble(source.substring(start, cursor));
    }

    private boolean match(char c) {
        if (!isAtEnd() && source.charAt(cursor) == c) {
            cursor++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        if (!match(c)) {
            throw new IllegalArgumentException("Expected '" + c + "'");
        }
    }

    private char peek() {
        return source.charAt(cursor);
    }

    private boolean isAtEnd() {
        return cursor >= source.length();
    }

    private void skipSpaces() {
        while (!isAtEnd() && Character.isWhitespace(source.charAt(cursor))) cursor++;
    }
}
