package com.esmail.calculator;

import com.esmail.calculator.engine.*;
import com.esmail.calculator.modes.*;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UiIntegrationTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Testing Full UI & Casio Operations Engine ===");

        // Initialize JavaFX Toolkit
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException ignored) {
            latch.countDown();
        }
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("JavaFX Toolkit failed to initialize!");
        }

        // Test 1: Auto-balancing parentheses on complex trigonometric input
        String expr1 = "cos(54";
        String balanced1 = ExpressionParser.balanceParentheses(expr1);
        double val1 = new ExpressionParser(balanced1, true, 0).evaluate();
        System.out.println("[UI Test 1] cos(54 auto-closed -> " + balanced1 + " = " + val1);
        if (Math.abs(val1 - Math.cos(Math.toRadians(54))) > 1e-9) throw new RuntimeException("cos(54 failed!");

        // Test 2: Chaining calculation with Ans
        double ans = val1;
        double val2 = new ExpressionParser("ans + 10", true, ans, Map.of("ans", ans)).evaluate();
        System.out.println("[UI Test 2] Ans + 10 = " + val2);
        if (Math.abs(val2 - (ans + 10)) > 1e-9) throw new RuntimeException("Ans chaining failed!");

        // Test 3: S<=>D Fraction and exact formatting
        double fracVal = 0.5;
        String formattedFrac = CasioFormatter.toExactOrFraction(fracVal);
        String formattedDec = CasioFormatter.formatDecimal(fracVal);
        System.out.println("[UI Test 3] S<=>D toggle: " + formattedFrac + " <-> " + formattedDec);
        if (!formattedFrac.equals("1/2") || !formattedDec.equals("0.5")) throw new RuntimeException("S<=>D formatting failed!");

        // Test 4: Constant lookup (Code 28 = c0)
        CasioConstants.ConstantEntry c28 = CasioConstants.get(28);
        System.out.println("[UI Test 4] CONST 28: " + c28.symbol() + " = " + c28.value() + " " + c28.unit());
        if (c28.value() != 299792458.0) throw new RuntimeException("Constant 28 mismatch!");

        // Test 5: Metric Conversion (Code 01 = in -> cm)
        double cm = CasioConversions.convert(1, 10.0); // 10 inches = 25.4 cm
        System.out.println("[UI Test 5] CONV 01: 10 in = " + cm + " cm");
        if (Math.abs(cm - 25.4) > 1e-9) throw new RuntimeException("Conversion 01 mismatch!");

        // Test 6: CMPLX Mode (Mode 2)
        ComplexNumber compRes = new ComplexExpressionParser("(2+3i)*(4-5i)", true).evaluate();
        System.out.println("[UI Test 6] CMPLX (2+3i)*(4-5i) = " + compRes.formatRectangular());
        if (Math.abs(compRes.real() - 23.0) > 1e-9 || Math.abs(compRes.imag() - 2.0) > 1e-9) {
            throw new RuntimeException("Complex calculation mismatch!");
        }

        // Test 7: BASE-N Mode (Mode 4)
        long baseDec = BaseNEngine.evaluate("15 + 1", BaseNEngine.Base.DEC);
        String hexStr = BaseNEngine.format(baseDec, BaseNEngine.Base.HEX);
        String binStr = BaseNEngine.format(baseDec, BaseNEngine.Base.BIN);
        System.out.println("[UI Test 7] BASE-N 15+1 = " + baseDec + " Dec = " + hexStr + " Hex = " + binStr + " Bin");
        if (baseDec != 16 || !hexStr.equals("10") || !binStr.equals("10000")) {
            throw new RuntimeException("Base-N mismatch!");
        }

        // Test 8: EQN Mode (Mode 5) Quadratic solution
        EquationSolver.EquationResult eqRes = EquationSolver.solveQuadratic(1, -7, 12);
        System.out.println("[UI Test 8] EQN X^2 - 7X + 12 = 0 -> " + eqRes.roots().get(0).format() + ", " + eqRes.roots().get(1).format());
        if (Math.abs(eqRes.roots().get(0).value().real() - 4.0) > 1e-9 || Math.abs(eqRes.roots().get(1).value().real() - 3.0) > 1e-9) {
            throw new RuntimeException("Equation quadratic mismatch!");
        }

        // Test 9: TABLE Mode (Mode 7) f(X) = X^2
        List<TableGenerator.TableRow> table = TableGenerator.generateTable("X^2", 1, 3, 1, true);
        System.out.println("[UI Test 9] TABLE f(X)=X^2 (1 to 3):");
        for (TableGenerator.TableRow r : table) {
            System.out.println("   [Row " + r.index() + "] X = " + r.x() + ", F(X) = " + r.fx());
        }
        if (table.size() != 3 || table.get(0).fx() != 1.0 || table.get(1).fx() != 4.0 || table.get(2).fx() != 9.0) {
            throw new RuntimeException("Table generation mismatch!");
        }

        // Test 10: MATRIX Mode (Mode 6) Det & Multiply
        String matDet = MatrixVectorEvaluator.evaluateMatrix("det(MatA)");
        System.out.println("[UI Test 10] MATRIX det(MatA) = " + matDet);
        if (Double.parseDouble(matDet) != 1.0) throw new RuntimeException("Matrix det mismatch!");

        // Test 11: VECTOR Mode (Mode 8) Dot & Norm
        String vctDot = MatrixVectorEvaluator.evaluateVector("VctA dot VctB");
        System.out.println("[UI Test 11] VECTOR VctA dot VctB = " + vctDot);
        if (Double.parseDouble(vctDot) != 32.0) throw new RuntimeException("Vector dot mismatch!");

        // Test 12: SOLVE (Shift + CALC) Root Finding
        NumericalSolve.SolveResult sr = NumericalSolve.solve("2X - 10 = 0", 0, true, null);
        System.out.println("[UI Test 12] SOLVE 2X - 10 = 0 -> X = " + sr.root() + ", L-R = " + sr.residual());
        if (Math.abs(sr.root() - 5.0) > 1e-9 || Math.abs(sr.residual()) > 1e-9) {
            throw new RuntimeException("Solve feature mismatch!");
        }

        // Test 13: Cube Root (³√) via Shift + √■
        double cbrt27 = new ExpressionParser("³√(27)", true, 0).evaluate();
        double cbrtNeg8 = new ExpressionParser("³√(-8)", true, 0).evaluate();
        System.out.println("[UI Test 13] ³√(27) = " + cbrt27 + ", ³√(-8) = " + cbrtNeg8);
        if (Math.abs(cbrt27 - 3.0) > 1e-9 || Math.abs(cbrtNeg8 - (-2.0)) > 1e-9) {
            throw new RuntimeException("Cube root ³√ failed!");
        }

        // Test 14: Fraction (■/□) and Mixed Fraction (a b/c / ⌟) via Shift + ■/□
        double f1 = new ExpressionParser("1⌟2", true, 0).evaluate();
        double f2 = new ExpressionParser("1⌟2⌟3", true, 0).evaluate();
        double f3 = new ExpressionParser("2⌟3/4", true, 0).evaluate();
        System.out.println("[UI Test 14] Fractions: 1⌟2 = " + f1 + ", 1⌟2⌟3 = " + f2 + ", 2⌟3/4 = " + f3);
        if (Math.abs(f1 - 0.5) > 1e-9 || Math.abs(f2 - (5.0 / 3.0)) > 1e-9 || Math.abs(f3 - 2.75) > 1e-9) {
            throw new RuntimeException("Fraction and mixed fraction ⌟ failed!");
        }

        // Test 15: Inverse Trig via Shift: sin⁻¹(0.5)
        double asinVal = new ExpressionParser("sin⁻¹(0.5)", true, 0).evaluate();
        System.out.println("[UI Test 15] sin⁻¹(0.5) = " + asinVal + "°");
        if (Math.abs(asinVal - 30.0) > 1e-9) {
            throw new RuntimeException("sin⁻¹ inverse trig failed!");
        }

        // Test 16: CasioFormatter Mixed Fraction & DMS toggle
        String mixedStr = CasioFormatter.toMixedFraction(5.0 / 3.0);
        String dmsStr = CasioFormatter.toDms(12.5);
        System.out.println("[UI Test 16] toMixedFraction(5/3) = " + mixedStr + ", toDms(12.5) = " + dmsStr);
        if (!mixedStr.equals("1⌟2⌟3") || !dmsStr.equals("12°30'0\"")) {
            throw new RuntimeException("Mixed fraction / DMS formatter failed!");
        }

        System.out.println(">>> ALL 16 UI & OPERATION INTEGRATION TESTS PASSED WITH 100% SUCCESS! <<<");
        Platform.exit();
    }
}
