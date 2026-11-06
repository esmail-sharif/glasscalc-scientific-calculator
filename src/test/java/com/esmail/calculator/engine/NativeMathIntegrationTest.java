package com.esmail.calculator.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for the real Windows NASM DLL through JNI. */
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
    void nativeTranscendentalsExecuteInAssembly() {
        assertEquals(0.5, MathEngine.sin(Math.PI / 6.0), EPSILON);
        assertEquals(Math.sqrt(3.0) / 2.0, MathEngine.cos(Math.PI / 6.0), EPSILON);
        assertEquals(1.0, MathEngine.tan(Math.PI / 4.0), EPSILON);
        assertEquals(3.0, MathEngine.sqrt(9.0), EPSILON);
        assertEquals(1.0, MathEngine.ln(Math.E), EPSILON);
        assertEquals(3.0, MathEngine.log10(1000.0), EPSILON);
        assertEquals(3.0, MathEngine.log2(8.0), EPSILON);
        assertEquals(8.0, MathEngine.exp2(3.0), EPSILON);
        assertEquals(2.0, MathEngine.exp(Math.log(2.0)), EPSILON);
        assertEquals(Math.PI / 4.0, MathEngine.atan(1.0), EPSILON);
        assertEquals(Math.PI / 4.0, MathEngine.atan2(1.0, 1.0), EPSILON);
        assertEquals(-8.0, NativeMath.pow(-2.0, 3.0), EPSILON);
    }

    @Test
    void x87RangeFailuresDoNotCorruptTheFpuStack() {
        assertTrue(Double.isNaN(NativeMath.sin(1.0e100)));
        assertTrue(Double.isNaN(NativeMath.cos(1.0e100)));
        assertTrue(Double.isNaN(NativeMath.tan(1.0e100)));
        assertEquals(Math.sin(1.0e100), MathEngine.sin(1.0e100), EPSILON);
        assertEquals(Math.cos(1.0e100), MathEngine.cos(1.0e100), EPSILON);
        assertEquals(Math.tan(1.0e100), MathEngine.tan(1.0e100), EPSILON);
        assertEquals(0.5, MathEngine.sin(Math.PI / 6.0), EPSILON);
    }

    @Test
    void factorialRejectsInvalidNativeInputAndHandlesOverflow() {
        assertEquals(1.0, NativeMath.factorial(0), EPSILON);
        assertEquals(120.0, NativeMath.factorial(5), EPSILON);
        assertEquals(3628800.0, NativeMath.factorial(10), EPSILON);
        assertTrue(Double.isNaN(NativeMath.factorial(-1)));
        assertEquals(Double.POSITIVE_INFINITY, NativeMath.factorial(171));
        assertEquals(Double.POSITIVE_INFINITY, MathEngine.factorial(171));
    }
}
