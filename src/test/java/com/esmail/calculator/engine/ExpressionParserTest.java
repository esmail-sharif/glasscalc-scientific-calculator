package com.esmail.calculator.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpressionParserTest {
    private double eval(String expression) { return new ExpressionParser(expression, true, 0).evaluate(); }
    @Test void arithmeticPrecedence() { assertEquals(14, eval("2+3*4"), 1e-10); }
    @Test void parenthesesAndPowers() { assertEquals(512, eval("2^3^2"), 1e-10); }
    @Test void trigonometryAndRoots() { assertEquals(9.5, eval("sin(30)+sqrt(81)"), 1e-10); }
    @Test void logarithmsAndFactorial() { assertEquals(123, eval("log(1000)+5! - 3"), 1e-10); }
    @Test void implicitMultiplication() { assertEquals(2*Math.PI, eval("2pi"), 1e-10); }
    @Test void divisionByZeroRejected() { assertThrows(ArithmeticException.class, () -> eval("1/0")); }
}
