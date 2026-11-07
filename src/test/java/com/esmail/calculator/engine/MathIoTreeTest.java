package com.esmail.calculator.engine;

import com.esmail.calculator.engine.mathio.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MathIoTreeTest {

    @Test
    void testFractionHierarchical2DNavigationAndEvaluation() {
        MathContainer root = new MathContainer();
        root.insertChar('3');
        root.insertChar('+');

        // Insert fraction -> cursor moves to numerator
        MathContainer num = root.insertFraction();
        num.insertText("1");

        // Move down into denominator
        MathContainer den = num.moveDown();
        assertNotSame(num, den);
        den.insertText("2");

        // Move right to step out of fraction back to root
        MathContainer after = den.moveRight();
        assertSame(root, after);
        after.insertText("*4");

        // Expected expression: "3+(1)/(2)*4"
        String expr = root.toExpressionString();
        assertEquals("3+(1)/(2)*4", expr);

        // Verify with ExpressionParser
        double result = new ExpressionParser(expr, true, 0).evaluate();
        assertEquals(5.0, result, 1e-9);

        // Move Up from denominator
        den.setCursorIndex(0);
        MathContainer movedUp = den.moveUp();
        assertSame(num, movedUp);
    }

    @Test
    void testSqrtAndNthRootHierarchicalBoxes() {
        MathContainer root = new MathContainer();
        MathContainer rad = root.insertSqrt();
        rad.insertText("16");

        MathContainer after = rad.moveRight();
        assertSame(root, after);
        after.insertChar('+');

        MathContainer deg = after.insertNthRoot();
        deg.insertChar('3');
        MathContainer rad2 = deg.moveRight();
        rad2.insertText("27");

        String expr = root.toExpressionString();
        assertEquals("√(16)+root(27, 3)", expr);

        double result = new ExpressionParser(expr, true, 0).evaluate();
        assertEquals(7.0, result, 1e-9);
    }

    @Test
    void testIntegralHierarchical3LevelNavigation() {
        MathContainer root = new MathContainer();
        MathContainer integrand = root.insertIntegral();
        integrand.insertText("X^2");

        // Move down to lower bound
        MathContainer lower = integrand.moveDown();
        assertNotSame(integrand, lower);
        lower.insertChar('0');

        // Move up from lower to integrand, then to upper bound
        MathContainer mid = lower.moveUp();
        assertSame(integrand, mid);
        MathContainer upper = mid.moveUp();
        assertNotSame(mid, upper);
        upper.insertChar('3');

        // Step out to root
        MathContainer after = upper.moveRight();
        assertSame(root, after);

        String expr = root.toExpressionString();
        assertEquals("∫(X^2, 0, 3)", expr);

        // Numerical integration of X^2 from 0 to 3 should be 9.0
        double result = new ExpressionParser(expr, true, 0).evaluate();
        assertEquals(9.0, result, 1e-3);
    }

    @Test
    void testStructuralDelete() {
        MathContainer root = new MathContainer();
        MathContainer num = root.insertFraction();
        num.insertChar('5');
        // Delete '5'
        num.delete();
        assertTrue(num.isEmpty());

        // Deleting when empty removes the fraction from parent
        MathContainer parent = num.delete();
        assertSame(root, parent);
        assertTrue(root.isEmpty());
    }
}
