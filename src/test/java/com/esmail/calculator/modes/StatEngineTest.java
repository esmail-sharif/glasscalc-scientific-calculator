package com.esmail.calculator.modes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StatEngineTest {

    @Test
    void test1VarBasicAndFreq() {
        StatEngine se = new StatEngine(StatEngine.StatType.ONE_VAR);
        // Points: 2 (freq 1), 4 (freq 2), 6 (freq 1) -> dataset: {2, 4, 4, 6}
        se.addPoint(2, 0, 1);
        se.addPoint(4, 0, 2);
        se.addPoint(6, 0, 1);

        assertEquals(4.0, se.n());
        assertEquals(16.0, se.sumX()); // 2 + 4 + 4 + 6 = 16
        assertEquals(72.0, se.sumX2()); // 4 + 16 + 16 + 36 = 72
        assertEquals(4.0, se.meanX());

        // Population variance = (72 - 64) / 4 = 2.0 -> sigma = sqrt(2)
        assertEquals(Math.sqrt(2.0), se.sigmaX(), 1e-9);

        // Sample variance = (72 - 64) / 3 = 8/3 -> s = sqrt(8/3)
        assertEquals(Math.sqrt(8.0 / 3.0), se.sX(), 1e-9);

        assertEquals(2.0, se.minX());
        assertEquals(6.0, se.maxX());
    }

    @Test
    void testFrequencyValidationRejectsNonPositiveOrFractional() {
        StatEngine se = new StatEngine(StatEngine.StatType.ONE_VAR);
        // Reject negative or zero frequency
        assertThrows(ArithmeticException.class, () -> se.addPoint(10, 0, 0));
        assertThrows(ArithmeticException.class, () -> se.addPoint(10, 0, -1));

        // Reject non-integer frequency (Casio specification)
        assertThrows(ArithmeticException.class, () -> se.addPoint(10, 0, 1.5));
    }

    @Test
    void testLinearRegressionExactCoefficientsAndEstimates() {
        // Line y = 2x + 1
        // Points: (1, 3), (2, 5), (3, 7)
        StatEngine se = new StatEngine(StatEngine.StatType.LINEAR);
        se.addPoint(1, 3);
        se.addPoint(2, 5);
        se.addPoint(3, 7);

        assertEquals(1.0, se.regA(), 1e-9); // A = 1
        assertEquals(2.0, se.regB(), 1e-9); // B = 2
        assertEquals(1.0, se.regR(), 1e-9); // r = 1

        // Estimations
        assertEquals(9.0, se.estimateY(4), 1e-9); // y for x=4 is 1 + 2*4 = 9
        assertEquals(4.0, se.estimateX(9), 1e-9); // x for y=9 is (9 - 1)/2 = 4
    }

    @Test
    void testLogarithmicRegressionExactCoefficients() {
        // Model: y = A + B * ln(x)
        // Let A = 3, B = 2.
        // Points: (1, 3), (e, 5), (e^2, 7)
        StatEngine se = new StatEngine(StatEngine.StatType.LOGARITHMIC);
        se.addPoint(1.0, 3.0);
        se.addPoint(Math.E, 5.0);
        se.addPoint(Math.E * Math.E, 7.0);

        assertEquals(3.0, se.regA(), 1e-6);
        assertEquals(2.0, se.regB(), 1e-6);
        assertEquals(1.0, se.regR(), 1e-6);

        assertEquals(3.0 + 2.0 * Math.log(4.0), se.estimateY(4.0), 1e-6);
    }

    @Test
    void testExponentialERegressionExactCoefficients() {
        // Model: y = A * e^(B*x)
        // Let A = 2, B = 0.5.
        // x=0 -> y=2, x=2 -> y=2*e
        StatEngine se = new StatEngine(StatEngine.StatType.EXPONENTIAL_E);
        se.addPoint(0.0, 2.0);
        se.addPoint(2.0, 2.0 * Math.E);

        assertEquals(2.0, se.regA(), 1e-6);
        assertEquals(0.5, se.regB(), 1e-6);
        assertEquals(1.0, se.regR(), 1e-6);
    }

    @Test
    void testPowerRegressionExactCoefficients() {
        // Model: y = A * x^B
        // Let y = 3 * x^2 -> (1, 3), (2, 12), (3, 27)
        StatEngine se = new StatEngine(StatEngine.StatType.POWER);
        se.addPoint(1.0, 3.0);
        se.addPoint(2.0, 12.0);
        se.addPoint(3.0, 27.0);

        assertEquals(3.0, se.regA(), 1e-6);
        assertEquals(2.0, se.regB(), 1e-6);
        assertEquals(1.0, se.regR(), 1e-6);

        assertEquals(48.0, se.estimateY(4.0), 1e-6);
    }

    @Test
    void testInverseRegressionExactCoefficients() {
        // Model: y = A + B / x
        // Let A = 2, B = 4 -> (1, 6), (2, 4), (4, 3)
        StatEngine se = new StatEngine(StatEngine.StatType.INVERSE);
        se.addPoint(1.0, 6.0);
        se.addPoint(2.0, 4.0);
        se.addPoint(4.0, 3.0);

        assertEquals(2.0, se.regA(), 1e-6);
        assertEquals(4.0, se.regB(), 1e-6);
        assertEquals(1.0, se.regR(), 1e-6);
    }

    @Test
    void testQuadraticRegressionExactCoefficients() {
        // Model: y = A + B*x + C*x^2
        // Let y = 1 + 2x + 3x^2 -> (0, 1), (1, 6), (2, 17)
        StatEngine se = new StatEngine(StatEngine.StatType.QUADRATIC);
        se.addPoint(0.0, 1.0);
        se.addPoint(1.0, 6.0);
        se.addPoint(2.0, 17.0);

        assertEquals(1.0, se.regA(), 1e-6);
        assertEquals(2.0, se.regB(), 1e-6);
        assertEquals(3.0, se.regC(), 1e-6);

        // For x=3: 1 + 6 + 27 = 34
        assertEquals(34.0, se.estimateY(3.0), 1e-6);
    }

    @Test
    void testNormalDistributionCDF() {
        StatEngine se = new StatEngine(StatEngine.StatType.ONE_VAR);
        // Standard normal: P(0) = 0.5, Q(0) = 0.0, R(0) = 0.5
        assertEquals(0.5, se.probP(0.0), 1e-4);
        assertEquals(0.0, se.probQ(0.0), 1e-4);
        assertEquals(0.5, se.probR(0.0), 1e-4);

        // P(t) + R(t) = 1.0
        assertEquals(1.0, se.probP(1.5) + se.probR(1.5), 1e-9);
    }

    @Test
    void testBoundaryConditions() {
        StatEngine se = new StatEngine(StatEngine.StatType.ONE_VAR);
        // Empty dataset
        assertThrows(ArithmeticException.class, se::meanX);
        assertThrows(ArithmeticException.class, se::sigmaX);
        assertThrows(ArithmeticException.class, se::sX);

        // Single point (n = 1)
        se.addPoint(5.0);
        assertEquals(5.0, se.meanX());
        assertEquals(0.0, se.sigmaX());
        // Sample stddev requires n >= 2
        assertThrows(ArithmeticException.class, se::sX);

        // Zero variance
        se.addPoint(5.0);
        se.addPoint(5.0);
        assertEquals(0.0, se.sigmaX());
        assertEquals(0.0, se.sX());
    }
}
