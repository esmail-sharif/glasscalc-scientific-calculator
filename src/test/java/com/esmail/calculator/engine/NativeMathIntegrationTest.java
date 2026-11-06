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
    void expAndExp2HandleIeeeSpecialValuesAndBinary64Boundaries() {
        assertEquals(1.0, NativeMath.exp2(0.0));
        assertEquals(1.0, NativeMath.exp2(-0.0));
        assertTrue(Double.isNaN(NativeMath.exp2(Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, NativeMath.exp2(Double.POSITIVE_INFINITY));
        assertEquals(0.0, NativeMath.exp2(Double.NEGATIVE_INFINITY));
        assertEquals(Double.POSITIVE_INFINITY, NativeMath.exp2(1024.0));
        assertTrue(Double.isFinite(NativeMath.exp2(1023.0)));
        assertEquals(0L, Double.doubleToRawLongBits(NativeMath.exp2(-1075.0)));
        assertEquals(Double.doubleToRawLongBits(Double.MIN_VALUE),
                Double.doubleToRawLongBits(NativeMath.exp2(-1074.0)));

        assertEquals(Double.doubleToRawLongBits(Math.exp(-745.0)),
                Double.doubleToRawLongBits(NativeMath.exp(-745.0)));
        assertEquals(Double.POSITIVE_INFINITY, NativeMath.exp(710.0));
        assertTrue(Double.isNaN(NativeMath.exp(Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, NativeMath.exp(Double.POSITIVE_INFINITY));
        assertEquals(0.0, NativeMath.exp(Double.NEGATIVE_INFINITY));
        assertEquals(Math.exp(709.0), MathEngine.exp(709.0),
                Math.ulp(Math.exp(709.0)) * 2.0);
    }

    @Test
    void nativePowPreservesSignedZeroAndHandlesIeeeSpecialCases() {
        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(NativeMath.pow(-0.0, 3.0)));
        assertEquals(Double.NEGATIVE_INFINITY, NativeMath.pow(-0.0, -3.0));
        assertEquals(Double.doubleToRawLongBits(0.0),
                Double.doubleToRawLongBits(NativeMath.pow(-0.0, 2.0)));
        assertEquals(Double.POSITIVE_INFINITY, NativeMath.pow(-0.0, -2.0));

        assertEquals(1.0, NativeMath.pow(Double.NaN, 0.0));
        assertEquals(1.0, NativeMath.pow(1.0, Double.NaN));
        assertEquals(1.0, NativeMath.pow(-1.0, Double.POSITIVE_INFINITY));
        assertTrue(Double.isNaN(NativeMath.pow(Double.NaN, 1.0)));
        assertTrue(Double.isNaN(NativeMath.pow(2.0, Double.NaN)));
        assertTrue(Double.isNaN(NativeMath.pow(-2.0, 0.5)));

        assertEquals(Double.POSITIVE_INFINITY,
                NativeMath.pow(Double.POSITIVE_INFINITY, 2.0));
        assertEquals(0L, Double.doubleToRawLongBits(
                NativeMath.pow(Double.POSITIVE_INFINITY, -2.0)));
        assertEquals(Double.NEGATIVE_INFINITY,
                NativeMath.pow(Double.NEGATIVE_INFINITY, 3.0));
        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(NativeMath.pow(Double.NEGATIVE_INFINITY, -3.0)));

        assertEquals(Double.POSITIVE_INFINITY,
                NativeMath.pow(-2.0, 0x1.0p63));
        assertEquals(1.0, NativeMath.pow(-1.0, 0x1.0p63));
        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(MathEngine.pow(-0.0, 3.0)));
    }

    @Test
    void atan2UsesIeeeReferenceForSignedZeroAndInfiniteInputs() {
        assertEquals(Math.PI, NativeMath.atan2(+0.0, -0.0));
        assertEquals(-Math.PI, NativeMath.atan2(-0.0, -0.0));
        assertEquals(Double.doubleToRawLongBits(+0.0),
                Double.doubleToRawLongBits(NativeMath.atan2(+0.0, +0.0)));
        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(NativeMath.atan2(-0.0, +0.0)));
        assertEquals(Math.PI / 4.0,
                NativeMath.atan2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), EPSILON);
        assertEquals(3.0 * Math.PI / 4.0,
                NativeMath.atan2(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), EPSILON);
        assertEquals(-Math.PI / 4.0,
                NativeMath.atan2(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), EPSILON);
    }

    @Test
    void linkedPeUnwindMetadataIsStructurallyValidAndExecutable() {
        assertEquals(11, NativeMath.verifyUnwindMetadata(),
                "All 11 stack-allocating NASM routines must have valid PE unwind entries and restore a synthetic frame.");
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
