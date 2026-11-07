package com.esmail.calculator.modes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EquationSolverTest {

    @Test
    void solvesTwoVariableLinearSystemAndRejectsSingularCases() {
        EquationSolver.EquationResult result =
                EquationSolver.solveLinear2(2, 1, 5, 1, -1, 1);

        assertEquals(2.0, result.roots().get(0).value().real(), 1e-12);
        assertEquals(1.0, result.roots().get(1).value().real(), 1e-12);
        assertThrows(ArithmeticException.class,
                () -> EquationSolver.solveLinear2(2, 2, 4, 4, 4, 8));
        assertThrows(ArithmeticException.class,
                () -> EquationSolver.solveLinear2(2, 2, 4, 4, 4, 9));
    }

    @Test
    void solvesThreeVariableLinearSystemWithCramersRule() {
        // 2x+y+z=9, x+2y+z=8, x+y+2z=7 -> x=3, y=2, z=1.
        EquationSolver.EquationResult result = EquationSolver.solveLinear3(
                new double[]{2, 1, 1, 9},
                new double[]{1, 2, 1, 8},
                new double[]{1, 1, 2, 7});

        assertEquals(3.0, result.roots().get(0).value().real(), 1e-12);
        assertEquals(2.0, result.roots().get(1).value().real(), 1e-12);
        assertEquals(1.0, result.roots().get(2).value().real(), 1e-12);
        assertThrows(ArithmeticException.class, () -> EquationSolver.solveLinear3(
                new double[]{1, 1, 1, 3},
                new double[]{2, 2, 2, 6},
                new double[]{3, 3, 3, 9}));
    }

    @Test
    void solvesQuadraticRealAndComplexRootsAndReportsVertex() {
        EquationSolver.EquationResult real = EquationSolver.solveQuadratic(1, -7, 12);
        assertEquals(4.0, real.roots().get(0).value().real(), 1e-12);
        assertEquals(3.0, real.roots().get(1).value().real(), 1e-12);
        assertEquals(2, real.vertexInfo().size());
        assertTrue(real.vertexInfo().get(0).contains("3.5") || real.vertexInfo().get(0).contains("7/2"));

        EquationSolver.EquationResult complex = EquationSolver.solveQuadratic(1, 0, 1);
        assertEquals(0.0, complex.roots().get(0).value().real(), 1e-12);
        assertEquals(1.0, Math.abs(complex.roots().get(0).value().imag()), 1e-12);
        assertEquals(0.0, complex.roots().get(1).value().real(), 1e-12);
        assertEquals(-complex.roots().get(0).value().imag(), complex.roots().get(1).value().imag(), 1e-12);
        assertThrows(ArithmeticException.class, () -> EquationSolver.solveQuadratic(0, 2, 1));
    }

    @Test
    void solvesCubicWithThreeRealRoots() {
        // x^3 - 6x^2 + 11x - 6 = 0 -> roots {1,2,3}.
        EquationSolver.EquationResult result = EquationSolver.solveCubic(1, -6, 11, -6);
        double[] roots = result.roots().stream()
                .mapToDouble(root -> root.value().real())
                .sorted()
                .toArray();
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, roots, 1e-10);
        assertTrue(result.roots().stream().allMatch(root -> Math.abs(root.value().imag()) < 1e-12));
    }

    @Test
    void solvesCubicWithOneRealAndTwoComplexRoots() {
        // x^3 + 1 = 0 -> -1 and 0.5 ± 0.8660254i.
        EquationSolver.EquationResult result = EquationSolver.solveCubic(1, 0, 0, 1);
        assertEquals(-1.0, result.roots().get(0).value().real(), 1e-12);
        assertEquals(0.0, result.roots().get(0).value().imag(), 1e-12);
        assertEquals(0.5, result.roots().get(1).value().real(), 1e-12);
        assertEquals(0.5, result.roots().get(2).value().real(), 1e-12);
        assertEquals(Math.sqrt(3.0) / 2.0, result.roots().get(1).value().imag(), 1e-12);
        assertEquals(-Math.sqrt(3.0) / 2.0, result.roots().get(2).value().imag(), 1e-12);
        assertThrows(ArithmeticException.class, () -> EquationSolver.solveCubic(0, 1, 2, 3));
    }
}
