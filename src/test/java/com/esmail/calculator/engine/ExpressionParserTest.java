package com.esmail.calculator.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionParserTest {
    private double eval(String expression) {
        return new ExpressionParser(expression, true, 0).evaluate();
    }

    @Test void arithmeticPrecedence() { assertEquals(14, eval("2+3*4"), 1e-10); }
    @Test void parenthesesAndPowers() { assertEquals(512, eval("2^3^2"), 1e-10); }
    @Test void unaryMinusAndNegativeExponent() {
        assertEquals(-4, eval("-2^2"), 1e-10);
        assertEquals(0.25, eval("2^-2"), 1e-10);
    }
    @Test void trigonometryAndRoots() { assertEquals(9.5, eval("sin(30)+sqrt(81)"), 1e-10); }
    @Test void inverseAndHyperbolicFunctions() {
        assertEquals(30, eval("asin(0.5)"), 1e-10);
        assertEquals(Math.sinh(1), eval("sinh(1)"), 1e-10);
        assertEquals(1, eval("atanh(tanh(1))"), 1e-10);
    }
    @Test void logarithmsExponentialsAndRoots() {
        assertEquals(3, eval("log2(8)"), 1e-10);
        assertEquals(5, eval("logbase(32,2)"), 1e-10);
        assertEquals(8, eval("exp2(3)"), 1e-9);
        assertEquals(-3, eval("cbrt(-27)"), 1e-10);
        assertEquals(-3, eval("root(-27,3)"), 1e-10);
    }
    @Test void arithmeticAndIntegerFunctions() {
        assertEquals(2, eval("mod(-4,3)"), 1e-10);
        assertEquals(-1, eval("remainder(-4,3)"), 1e-10);
        assertEquals(-2, eval("idiv(-7,3)"), 1e-10);
        assertEquals(6, eval("gcd(54,24)"), 1e-10);
        assertEquals(216, eval("lcm(54,24)"), 1e-10);
    }
    @Test void combinatoricsAndStatistics() {
        assertEquals(60, eval("perm(5,3)"), 1e-10);
        assertEquals(10, eval("comb(5,2)"), 1e-10);
        assertEquals(4, eval("mean(2,4,6)"), 1e-10);
        assertEquals(4, eval("median(2,4,6)"), 1e-10);
        assertEquals(8.0 / 3.0, eval("variance(2,4,6)"), 1e-10);
        assertEquals(12, eval("sum(2,4,6)"), 1e-10);
        assertEquals(48, eval("product(2,4,6)"), 1e-10);
    }
    @Test void constantsAndImplicitMultiplication() {
        assertEquals(2 * Math.PI, eval("2pi"), 1e-10);
        assertEquals(2 * Math.PI, eval("tau"), 1e-10);
        assertEquals((1 + Math.sqrt(5)) / 2, eval("phi"), 1e-10);
    }
    @Test void factorialAndPercent() {
        assertEquals(120, eval("log(1000)+5! - 3"), 1e-10);
        assertEquals(0.25, eval("25%"), 1e-10);
    }
    @Test void divisionByZeroRejected() { assertThrows(ArithmeticException.class, () -> eval("1/0")); }
    @Test void nonFiniteExpressionResultRejected() { assertThrows(ArithmeticException.class, () -> eval("fact(171)")); }
}
