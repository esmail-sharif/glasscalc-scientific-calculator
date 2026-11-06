package com.esmail.calculator.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CasioFeaturesTest {
    @Test
    void testFractionFormatting() {
        assertEquals("1/2", CasioFormatter.toExactOrFraction(0.5));
        assertEquals("1/3", CasioFormatter.toExactOrFraction(1.0 / 3.0));
        assertEquals("3/4", CasioFormatter.toExactOrFraction(0.75));
        assertEquals("-1/2", CasioFormatter.toExactOrFraction(-0.5));
        assertEquals("5", CasioFormatter.toExactOrFraction(5.0));
    }

    @Test
    void testPiMultiplesFormatting() {
        assertEquals("π", CasioFormatter.toExactOrFraction(Math.PI));
        assertEquals("π/2", CasioFormatter.toExactOrFraction(Math.PI / 2.0));
        assertEquals("π/3", CasioFormatter.toExactOrFraction(Math.PI / 3.0));
        assertEquals("2π/3", CasioFormatter.toExactOrFraction(2.0 * Math.PI / 3.0));
    }

    @Test
    void testSquareRootFormatting() {
        assertEquals("√2", CasioFormatter.toExactOrFraction(Math.sqrt(2)));
        assertEquals("√3", CasioFormatter.toExactOrFraction(Math.sqrt(3)));
        assertEquals("2√3", CasioFormatter.toExactOrFraction(Math.sqrt(12)));
    }

    @Test
    void testNumericalIntegration() {
        // The exact showcase expression in the image:
        // \int_0^{\pi/3} \sin(X)dx = 1/2
        double result = new ExpressionParser("∫(sin(X), 0, pi/3)", false, 0).evaluate();
        assertEquals(0.5, result, 1e-4);
        assertEquals("1/2", CasioFormatter.toExactOrFraction(result));
    }

    @Test
    void testPermutationAndCombinationOperators() {
        assertEquals(20.0, new ExpressionParser("5P2", true, 0).evaluate(), 1e-9);
        assertEquals(10.0, new ExpressionParser("5C2", true, 0).evaluate(), 1e-9);
    }
}
