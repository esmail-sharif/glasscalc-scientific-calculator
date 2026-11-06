package com.esmail.calculator;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.ExpressionParser;
import com.esmail.calculator.engine.MathEngine;
import com.esmail.calculator.engine.NativeMath;
import com.esmail.calculator.modes.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CasioFullSystemTest {

    @Test
    void testAll40ScientificConstants() {
        for (int c = 1; c <= 40; c++) {
            CasioConstants.ConstantEntry entry = CasioConstants.get(c);
            assertNotNull(entry, "Missing constant entry for code " + c);
            assertEquals(c, entry.number());
            assertNotNull(entry.symbol());
            assertFalse(entry.symbol().isBlank());
            assertNotNull(entry.name());
            assertFalse(entry.name().isBlank());
            assertFalse(Double.isNaN(entry.value()));
            assertFalse(Double.isInfinite(entry.value()));
            assertNotEquals(0.0, entry.value());
        }

        // Test specific Casio constants
        assertEquals(299792458.0, CasioConstants.get(28).value()); // c0 speed of light
        assertEquals(9.80665, CasioConstants.get(35).value());     // g standard gravity
        assertEquals(101325.0, CasioConstants.get(40).value());    // atm
    }

    @Test
    void testAll40MetricConversionsAndRoundTrips() {
        for (int k = 1; k <= 40; k++) {
            CasioConversions.ConversionEntry conv = CasioConversions.get(k);
            assertNotNull(conv, "Missing conversion for code " + k);
            assertEquals(k, conv.number());
            assertNotNull(conv.fromUnit());
            assertNotNull(conv.toUnit());
            assertNotNull(conv.label());

            double converted = CasioConversions.convert(k, 50.0);
            assertFalse(Double.isNaN(converted));
            assertFalse(Double.isInfinite(converted));
        }

        // Verify all 20 bidirectional conversion round-trips
        for (int p = 1; p <= 39; p += 2) {
            double testVal = 100.0;
            double fwd = CasioConversions.convert(p, testVal);
            double back = CasioConversions.convert(p + 1, fwd);
            assertEquals(testVal, back, 1e-6, "Round-trip failed for pair " + p + "/" + (p + 1));
        }

        // Reference values
        assertEquals(25.4, CasioConversions.convert(1, 10.0), 1e-9); // 10 in -> 25.4 cm
        assertEquals(100.0, CasioConversions.convert(37, 212.0), 1e-9); // 212 F -> 100 C
        assertEquals(32.0, CasioConversions.convert(38, 0.0), 1e-9);    // 0 C -> 32 F
        assertEquals(101325.0, CasioConversions.convert(25, 1.0), 1e-9); // 1 atm -> 101325 Pa
    }

    @Test
    void testNumericalIntegrationAndDifferentiation() {
        // Numerical integral: ∫(sin(X), 0, π/3) = 1/2
        double integral = new ExpressionParser("∫(sin(X), 0, pi/3)", false, 0).evaluate();
        assertEquals(0.5, integral, 1e-4);
        assertEquals("1/2", CasioFormatter.toExactOrFraction(integral));

        // Numerical diff: d/dx(X^3) at X=2 is 12
        double diff = new ExpressionParser("diff(X^3, 2)", true, 0).evaluate();
        assertEquals(12.0, diff, 1e-3);
    }

    @Test
    void testSequentialSummation() {
        // Σ(X^2, 1, 5) = 1 + 4 + 9 + 16 + 25 = 55
        double sum = new ExpressionParser("Σ(X^2, 1, 5)", true, 0).evaluate();
        assertEquals(55.0, sum, 1e-9);
    }

    @Test
    void testFractionsAndMixedFractions() {
        assertEquals(0.5, new ExpressionParser("1⌟2", true, 0).evaluate(), 1e-9);
        assertEquals(5.0 / 3.0, new ExpressionParser("1⌟2⌟3", true, 0).evaluate(), 1e-9);
        assertEquals(2.75, new ExpressionParser("2⌟3/4", true, 0).evaluate(), 1e-9);

        assertEquals("1⌟2⌟3", CasioFormatter.toMixedFraction(5.0 / 3.0));
        assertEquals("12°30'0\"", CasioFormatter.toDms(12.5));
    }

    @Test
    void testCoordinateConversions() {
        Map<String, Double> polVars = new HashMap<>();
        double r = new ExpressionParser("pol(3, 4)", true, 0, polVars).evaluate();
        assertEquals(5.0, r, 1e-9);
        assertEquals(5.0, polVars.get("X"), 1e-9);
        assertEquals(53.13010235, polVars.get("Y"), 1e-4);

        Map<String, Double> recVars = new HashMap<>();
        double x = new ExpressionParser("rec(2, 60)", true, 0, recVars).evaluate();
        assertEquals(1.0, x, 1e-9);
        assertEquals(Math.sqrt(3.0), recVars.get("Y"), 1e-4);
    }

    @Test
    void testPseudoRandom() {
        double r = new ExpressionParser("Ran#", true, 0).evaluate();
        assertTrue(r >= 0.0 && r < 1.0);

        double ri = new ExpressionParser("RanInt#(1, 6)", true, 0).evaluate();
        assertTrue(ri >= 1.0 && ri <= 6.0);
        assertEquals(ri, Math.floor(ri));
    }

    @Test
    void testPostfixAngleUnits() {
        assertEquals(1.0, new ExpressionParser("sin(90°)", true, 0).evaluate(), 1e-6);
        assertEquals(1.0, new ExpressionParser("sin((π/2) r)", true, 0).evaluate(), 1e-6);
        assertEquals(1.0, new ExpressionParser("sin(100 g)", true, 0).evaluate(), 1e-6);
    }

    @Test
    void testEquationSolver() {
        // 2x2 linear system
        // 2x + y = 5, x - y = 1 -> x = 2, y = 1
        EquationSolver.EquationResult eq2 = EquationSolver.solveLinear2(2, 1, 5, 1, -1, 1);
        assertEquals(2.0, eq2.roots().get(0).value().real(), 1e-9);
        assertEquals(1.0, eq2.roots().get(1).value().real(), 1e-9);

        // Quadratic: X^2 - 7X + 12 = 0 -> X1 = 4, X2 = 3
        EquationSolver.EquationResult qRes = EquationSolver.solveQuadratic(1, -7, 12);
        assertEquals(4.0, qRes.roots().get(0).value().real(), 1e-9);
        assertEquals(3.0, qRes.roots().get(1).value().real(), 1e-9);
    }

    @Test
    void testNumericalRootSolver() {
        NumericalSolve.SolveResult sr = NumericalSolve.solve("3X + 5 = 20", 0, true, null);
        assertEquals(5.0, sr.root(), 1e-9);
        assertEquals(0.0, sr.residual(), 1e-9);
    }

    @Test
    void testTableGenerator() {
        List<TableGenerator.TableRow> table = TableGenerator.generateTable("X^2", 1, 3, 1, true);
        assertEquals(3, table.size());
        assertEquals(1.0, table.get(0).fx(), 1e-9);
        assertEquals(4.0, table.get(1).fx(), 1e-9);
        assertEquals(9.0, table.get(2).fx(), 1e-9);

        // Insufficient MEM when exceeding 30 rows
        assertThrows(ArithmeticException.class, () -> TableGenerator.generateTable("X", 1, 50, 1, true));
    }

    @Test
    void testComplexExpressions() {
        ComplexNumber c1 = new ComplexExpressionParser("(3+4i)*(1-2i)", true).evaluate();
        assertEquals(11.0, c1.real(), 1e-9);
        assertEquals(-2.0, c1.imag(), 1e-9);

        ComplexNumber c2 = new ComplexExpressionParser("(1+i)/(1-i)", true).evaluate();
        assertEquals(0.0, c2.real(), 1e-9);
        assertEquals(1.0, c2.imag(), 1e-9);
    }
}
