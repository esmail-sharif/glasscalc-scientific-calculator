package com.esmail.calculator.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the real Windows NASM DLL through JNI.
 * The Windows CI workflow builds and embeds glasscalc.dll before Maven runs.
 */
class NativeMathIntegrationTest {
    private static final double EPSILON = 1e-9;

    @Test
    void nativeDllLoadsAndIsSelectedByMathEngine() {
        assertTrue(NativeMath.isAvailable(),
                "The Windows CI run must load glasscalc.dll; Java fallback is not acceptable here.");
        assertTrue(MathEngine.isAssemblyActive());
    }

    @Test
    void arithmeticOperationsExecuteInAssembly() {
        assertEquals(7.0, MathEngine.add(3.0, 4.0), EPSILON);
        assertEquals(6.0, MathEngine.subtract(10.0, 4.0), EPSILON);
        assertEquals(42.0, MathEngine.multiply(6.0, 7.0), EPSILON);
        assertEquals(3.0, MathEngine.divide(21.0, 7.0), EPSILON);
    }

    @Test
    void transcendentalOperationsExecuteInAssembly() {
        assertEquals(0.5, MathEngine.sin(Math.PI / 6.0), EPSILON);
        assertEquals(Math.sqrt(3.0) / 2.0, MathEngine.cos(Math.PI / 6.0), EPSILON);
        assertEquals(1.0, MathEngine.tan(Math.PI / 4.0), EPSILON);
        assertEquals(3.0, MathEngine.sqrt(9.0), EPSILON);
        assertEquals(1.0, MathEngine.ln(Math.E), EPSILON);
        assertEquals(3.0, MathEngine.log10(1000.0), EPSILON);
        assertEquals(8.0, MathEngine.pow(2.0, 3.0), EPSILON);
        assertEquals(Math.PI / 4.0, MathEngine.atan(1.0), EPSILON);
    }

    @Test
    void factorialExecutesInAssembly() {
        assertEquals(1.0, MathEngine.factorial(0.0), EPSILON);
        assertEquals(120.0, MathEngine.factorial(5.0), EPSILON);
        assertEquals(3628800.0, MathEngine.factorial(10.0), EPSILON);
    }
}
