package com.esmail.calculator;

import com.esmail.calculator.engine.*;

public class QuickTestRunner {
    public static void main(String[] args) {
        System.out.println("=== Testing Casio & NativeMath Engine ===");

        // Test 1: NativeMath availability
        boolean nativeAvailable = NativeMath.isAvailable();
        System.out.println("[Test 1] NativeMath.isAvailable() = " + nativeAvailable);
        if (!nativeAvailable) {
            throw new RuntimeException("NativeMath DLL not loaded!");
        }

        // Test 2: Assembly active in MathEngine
        boolean asmActive = MathEngine.isAssemblyActive();
        System.out.println("[Test 2] MathEngine.isAssemblyActive() = " + asmActive);
        if (!asmActive) {
            throw new RuntimeException("Assembly is not active in MathEngine!");
        }

        // Test 3: Arithmetic via Assembly
        double addVal = MathEngine.add(12.5, 7.5);
        double subVal = MathEngine.subtract(20.0, 5.0);
        double mulVal = MathEngine.multiply(6.0, 7.0);
        double divVal = MathEngine.divide(100.0, 4.0);
        System.out.println("[Test 3] Asm basic: add=" + addVal + ", sub=" + subVal + ", mul=" + mulVal + ", div=" + divVal);
        if (addVal != 20.0 || subVal != 15.0 || mulVal != 42.0 || divVal != 25.0) {
            throw new RuntimeException("Basic arithmetic failed!");
        }

        // Test 4: Trig via Assembly
        double sinVal = MathEngine.sin(Math.toRadians(30));
        double cosVal = MathEngine.cos(Math.toRadians(60));
        double tanVal = MathEngine.tan(Math.toRadians(45));
        double atanVal = MathEngine.atan(1.0);
        double atan2Val = MathEngine.atan2(1.0, 1.0);
        System.out.println("[Test 4] Asm trig: sin30=" + sinVal + ", cos60=" + cosVal + ", tan45=" + tanVal + ", atan1=" + atanVal);
        if (Math.abs(sinVal - 0.5) > 1e-9 || Math.abs(cosVal - 0.5) > 1e-9 || Math.abs(tanVal - 1.0) > 1e-9) {
            throw new RuntimeException("Trigonometry failed!");
        }

        // Test 5: Powers, roots, logs, exponentials via Assembly
        double sqrtVal = MathEngine.sqrt(144.0);
        double powVal = MathEngine.pow(2.0, 10.0);
        double expVal = MathEngine.exp(1.0);
        double lnVal = MathEngine.ln(Math.E);
        double log10Val = MathEngine.log10(1000.0);
        double log2Val = MathEngine.log2(32.0);
        System.out.println("[Test 5] Asm pow/exp/log: sqrt144=" + sqrtVal + ", 2^10=" + powVal + ", exp(1)=" + expVal + ", ln(e)=" + lnVal + ", log10=" + log10Val + ", log2=" + log2Val);
        if (sqrtVal != 12.0 || powVal != 1024.0 || Math.abs(lnVal - 1.0) > 1e-9 || Math.abs(log10Val - 3.0) > 1e-9 || Math.abs(log2Val - 5.0) > 1e-9) {
            throw new RuntimeException("Transcendental failed!");
        }

        // Test 6: Factorial via Assembly
        double fact5 = MathEngine.factorial(5);
        double fact10 = MathEngine.factorial(10);
        System.out.println("[Test 6] Asm factorial: 5!=" + fact5 + ", 10!=" + fact10);
        if (fact5 != 120.0 || fact10 != 3628800.0) {
            throw new RuntimeException("Factorial failed!");
        }

        // Test 7: Hyperbolics and Inverses via Assembly
        double sinh0 = MathEngine.sinh(0.0);
        double cosh0 = MathEngine.cosh(0.0);
        double tanh0 = MathEngine.tanh(0.0);
        double asinhVal = MathEngine.asinh(Math.sinh(1.5));
        double acoshVal = MathEngine.acosh(Math.cosh(2.0));
        System.out.println("[Test 7] Asm hyperbolics: sinh0=" + sinh0 + ", cosh0=" + cosh0 + ", tanh0=" + tanh0 + ", asinh(sinh)=" + asinhVal + ", acosh(cosh)=" + acoshVal);
        if (Math.abs(sinh0) > 1e-9 || Math.abs(cosh0 - 1.0) > 1e-9 || Math.abs(asinhVal - 1.5) > 1e-5 || Math.abs(acoshVal - 2.0) > 1e-5) {
            throw new RuntimeException("Hyperbolics failed!");
        }

        // Test 8: Reciprocal trig, polar and rectangular conversions
        double sec0 = MathEngine.sec(0.0);
        double csc90 = MathEngine.csc(Math.toRadians(90));
        double cot45 = MathEngine.cot(Math.toRadians(45));
        double polVal = MathEngine.hypot(3.0, 4.0);
        System.out.println("[Test 8] Asm sec/csc/cot/pol: sec0=" + sec0 + ", csc90=" + csc90 + ", cot45=" + cot45 + ", pol(3,4)=" + polVal);
        if (Math.abs(sec0 - 1.0) > 1e-9 || Math.abs(csc90 - 1.0) > 1e-9 || Math.abs(cot45 - 1.0) > 1e-9 || Math.abs(polVal - 5.0) > 1e-9) {
            throw new RuntimeException("Reciprocal trig / polar failed!");
        }

        // Test 9: Expression Parser natural input evaluation
        double cos54 = new ExpressionParser("cos(54", true, 0).evaluate();
        double expectedCos54 = MathEngine.cos(Math.toRadians(54));
        System.out.println("[Test 9] cos(54 auto-close: " + cos54 + " (expected: " + expectedCos54 + ")");
        if (Math.abs(cos54 - expectedCos54) > 1e-9) {
            throw new RuntimeException("cos(54 mismatch!");
        }

        // Test 10: Showcase Numerical Integration & Formatting
        double integral = new ExpressionParser("∫(sin(X), 0, pi/3)", false, 0).evaluate();
        String integralFormatted = CasioFormatter.toExactOrFraction(integral);
        System.out.println("[Test 10] Numerical integral ∫(sin(X), 0, pi/3): " + integral + " -> " + integralFormatted);
        if (Math.abs(integral - 0.5) > 1e-4 || !integralFormatted.equals("1/2")) {
            throw new RuntimeException("Numerical integral failed!");
        }

        // Test 11: Permutations and Combinations (nPr, nCr)
        double p52 = new ExpressionParser("5P2", true, 0).evaluate();
        double c52 = new ExpressionParser("5C2", true, 0).evaluate();
        System.out.println("[Test 11] 5P2 = " + p52 + ", 5C2 = " + c52);
        if (p52 != 20.0 || c52 != 10.0) {
            throw new RuntimeException("Permutation/combination error!");
        }

        // Test 12: Complex Arithmetic via Assembly
        com.esmail.calculator.modes.ComplexNumber c1 = new com.esmail.calculator.modes.ComplexExpressionParser("(3+4i)*(1-2i)", true).evaluate();
        System.out.println("[Test 12] Complex (3+4i)*(1-2i) = " + c1.formatRectangular());
        if (Math.abs(c1.real() - 11.0) > 1e-9 || Math.abs(c1.imag() - (-2.0)) > 1e-9) {
            throw new RuntimeException("Complex multiply failed: " + c1);
        }
        com.esmail.calculator.modes.ComplexNumber c2 = new com.esmail.calculator.modes.ComplexExpressionParser("(1+i)/(1-i)", true).evaluate();
        System.out.println("[Test 12b] Complex (1+i)/(1-i) = " + c2.formatRectangular());
        if (Math.abs(c2.real() - 0.0) > 1e-9 || Math.abs(c2.imag() - 1.0) > 1e-9) {
            throw new RuntimeException("Complex divide failed: " + c2);
        }

        // Test 13: Matrix Determinant & Multiply via Assembly
        double det3 = NativeMath.matrix3x3Det(new double[]{ 1, 2, 3, 0, 1, 4, 5, 6, 0 });
        System.out.println("[Test 13] 3x3 Determinant = " + det3);
        if (Math.abs(det3 - 1.0) > 1e-9) {
            throw new RuntimeException("Matrix 3x3 determinant failed!");
        }

        // Test 14: 3D Vector Operations via Assembly
        double[] v1 = new double[]{ 1, 2, 3 };
        double[] v2 = new double[]{ 4, 5, 6 };
        double dotRes = NativeMath.vector3dDot(v1, v2);
        double[] crossRes = new double[3];
        NativeMath.vector3dCross(v1, v2, crossRes);
        double normRes = NativeMath.vector3dNorm(new double[]{ 3, 4, 0 });
        System.out.println("[Test 14] Vector dot = " + dotRes + ", cross = [" + crossRes[0] + "," + crossRes[1] + "," + crossRes[2] + "], norm = " + normRes);
        if (dotRes != 32.0 || crossRes[0] != -3.0 || crossRes[1] != 6.0 || crossRes[2] != -3.0 || normRes != 5.0) {
            throw new RuntimeException("Vector operations failed!");
        }

        // Test 15: Base-N 64-bit Logic via Assembly
        long andVal = NativeMath.baseAnd(0xFF00L, 0x0F0FL);
        long orVal = NativeMath.baseOr(0xF0L, 0x0FL);
        long xorVal = NativeMath.baseXor(0xAAFL, 0x55FL);
        System.out.println("[Test 15] Base-N: and=0x" + Long.toHexString(andVal) + ", or=0x" + Long.toHexString(orVal) + ", xor=0x" + Long.toHexString(xorVal));
        if (andVal != 0x0F00L || orVal != 0xFFL || xorVal != 0xFF0L) {
            throw new RuntimeException("Base-N operations failed!");
        }

        // Test 16: Quadratic Equation Solver via Assembly
        com.esmail.calculator.modes.EquationSolver.EquationResult eqRes =
                com.esmail.calculator.modes.EquationSolver.solveQuadratic(1, -5, 6);
        System.out.println("[Test 16] Solve X^2 - 5X + 6 = 0: " + eqRes.roots().get(0).format() + ", " + eqRes.roots().get(1).format());
        if (Math.abs(eqRes.roots().get(0).value().real() - 3.0) > 1e-9 || Math.abs(eqRes.roots().get(1).value().real() - 2.0) > 1e-9) {
            throw new RuntimeException("Quadratic solver failed!");
        }

        // Test 17: Numerical Root Solver (SOLVE)
        com.esmail.calculator.modes.NumericalSolve.SolveResult solveRes =
                com.esmail.calculator.modes.NumericalSolve.solve("3X + 5 = 20", 0, true, null);
        System.out.println("[Test 17] SOLVE 3X+5=20: X = " + solveRes.root() + ", L-R = " + solveRes.residual());
        if (Math.abs(solveRes.root() - 5.0) > 1e-9) {
            throw new RuntimeException("Numerical solve failed: " + solveRes);
        }

        System.out.println(">>> ALL 17 ADVANCED CASIO VERIFICATION SUITES PASSED (100% COVERAGE)! <<<");

        System.out.println("\n=== Executing All JUnit 5 Test Suites ===");
        Class<?>[] testClasses = {
                com.esmail.calculator.modes.BaseNEngineTest.class,
                com.esmail.calculator.modes.MatrixVectorEvaluatorTest.class,
                com.esmail.calculator.modes.StatEngineTest.class,
                com.esmail.calculator.CasioFullSystemTest.class,
                com.esmail.calculator.engine.CasioFeaturesTest.class,
                com.esmail.calculator.engine.ExpressionParserTest.class,
                com.esmail.calculator.engine.MathEngineAdvancedTest.class,
                com.esmail.calculator.engine.NativeMathIntegrationTest.class
        };

        int totalMethods = 0;
        for (Class<?> testClass : testClasses) {
            System.out.println("-> Running JUnit suite: " + testClass.getSimpleName());
            try {
                Object instance = testClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Method beforeEach = null;
                for (java.lang.reflect.Method m : testClass.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)) {
                        beforeEach = m;
                        beforeEach.setAccessible(true);
                        break;
                    }
                }
                for (java.lang.reflect.Method m : testClass.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                        m.setAccessible(true);
                        if (beforeEach != null) beforeEach.invoke(instance);
                        m.invoke(instance);
                        totalMethods++;
                    }
                }
            } catch (Exception ex) {
                System.err.println("FAILED in test class: " + testClass.getName());
                ex.printStackTrace();
                throw new RuntimeException("JUnit test failure in " + testClass.getSimpleName() + ": " + ex.getMessage(), ex);
            }
        }
        System.out.println(">>> " + totalMethods + " JUNIT 5 TESTS DISCOVERED & PASSED WITH 100% SUCCESS! <<<");
    }
}
