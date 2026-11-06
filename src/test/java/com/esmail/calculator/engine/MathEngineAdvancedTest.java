package com.esmail.calculator.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathEngineAdvancedTest {
    @Test void hyperbolicInverseAndReciprocalFunctions() {
        assertEquals(0.0, MathEngine.asinh(0.0), 1e-14);
        assertEquals(1.0, MathEngine.asinh(Math.sinh(1.0)), 1e-12);
        assertEquals(1.0, MathEngine.acosh(Math.cosh(1.0)), 1e-12);
        assertEquals(1.0, MathEngine.atanh(Math.tanh(1.0)), 1e-12);
        assertEquals(2.0, MathEngine.sec(Math.acos(0.5)), 1e-12);
        assertEquals(2.0, MathEngine.csc(Math.asin(0.5)), 1e-12);
        assertThrows(ArithmeticException.class, () -> MathEngine.acosh(0.5));
        assertThrows(ArithmeticException.class, () -> MathEngine.atanh(1.0));
    }

    @Test void stableRootsAndLogs() {
        assertEquals(-3.0, MathEngine.cbrt(-27.0), 1e-12);
        assertEquals(-3.0, MathEngine.nthRoot(-27.0, 3.0), 1e-12);
        assertEquals(0.25, MathEngine.nthRoot(16.0, -2.0), 1e-12);
        assertEquals(5.0, MathEngine.logBase(32.0, 2.0), 1e-12);
        assertThrows(ArithmeticException.class, () -> MathEngine.nthRoot(-16.0, 2.0));
    }

    @Test void roundingRemainderAndIntegerOperations() {
        assertEquals(-2.0, MathEngine.round(-1.5), 1e-12);
        assertEquals(1.0, MathEngine.trunc(1.9), 1e-12);
        assertEquals(-1.0, MathEngine.trunc(-1.9), 1e-12);
        assertEquals(2.0, MathEngine.modulo(-4.0, 3.0), 1e-12);
        assertEquals(-1.0, MathEngine.remainder(-4.0, 3.0), 1e-12);
        assertEquals(-2.0, MathEngine.integerDivide(-7.0, 3.0), 1e-12);
    }

    @Test void statisticalAndCombinatoricUtilities() {
        assertEquals(3.0, MathEngine.mean(1, 2, 3, 4, 5), 1e-12);
        assertEquals(3.0, MathEngine.median(5, 1, 4, 2, 3), 1e-12);
        assertEquals(2.0, MathEngine.variance(1, 2, 3, 4, 5), 1e-12);
        assertEquals(2.5, MathEngine.sampleVariance(1, 2, 3, 4, 5), 1e-12);
        assertEquals(10.0, MathEngine.sum(1, 2, 3, 4), 1e-12);
        assertEquals(24.0, MathEngine.product(1, 2, 3, 4), 1e-12);
        assertEquals(120.0, MathEngine.permutation(5, 3), 1e-12);
        assertEquals(10.0, MathEngine.combination(5, 2), 1e-12);
        assertEquals(6.0, MathEngine.gcd(54, 24), 1e-12);
        assertEquals(216.0, MathEngine.lcm(54, 24), 1e-12);
    }

    @Test void integerDomainValidation() {
        assertThrows(ArithmeticException.class, () -> MathEngine.factorial(-1));
        assertThrows(ArithmeticException.class, () -> MathEngine.factorial(3.5));
        assertThrows(ArithmeticException.class, () -> MathEngine.permutation(4.5, 2));
        assertThrows(ArithmeticException.class, () -> MathEngine.combination(4, 5));
    }
}
