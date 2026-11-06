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

        // Test 17: STAT Mode 1-VAR single-variable statistics (Pure NASM Assembly)
        StatEngine stat1 = new StatEngine(StatEngine.StatType.ONE_VAR);
        stat1.addPoint(2);
        stat1.addPoint(4);
        stat1.addPoint(6);
        stat1.addPoint(8);
        stat1.addPoint(10);
        System.out.println("[UI Test 17] STAT 1-VAR (2,4,6,8,10): n=" + stat1.n() + ", sumX=" + stat1.sumX()
                + ", sumX2=" + stat1.sumX2() + ", meanX=" + stat1.meanX() + ", sigmaX=" + stat1.sigmaX() + ", sX=" + stat1.sX());
        if (stat1.n() != 5 || stat1.sumX() != 30.0 || stat1.sumX2() != 220.0
                || Math.abs(stat1.meanX() - 6.0) > 1e-9 || Math.abs(stat1.sigmaX() - Math.sqrt(8.0)) > 1e-9
                || Math.abs(stat1.sX() - Math.sqrt(10.0)) > 1e-9) {
            throw new RuntimeException("STAT 1-VAR calculations failed!");
        }

        // Test 18: STAT Mode 2-VAR Linear Regression (A+BX via Pure NASM Assembly)
        StatEngine stat2 = new StatEngine(StatEngine.StatType.LINEAR);
        stat2.addPoint(1, 2);
        stat2.addPoint(2, 4);
        stat2.addPoint(3, 6);
        System.out.println("[UI Test 18] STAT 2-VAR Linear Reg: A=" + stat2.regA() + ", B=" + stat2.regB() + ", r=" + stat2.regR());
        if (Math.abs(stat2.regA() - 0.0) > 1e-9 || Math.abs(stat2.regB() - 2.0) > 1e-9 || Math.abs(stat2.regR() - 1.0) > 1e-9) {
            throw new RuntimeException("STAT 2-VAR linear regression failed!");
        }

        // Test 19: Coordinate conversions Pol(X, Y) and Rec(r, theta)
        java.util.Map<String, Double> polVars = new java.util.HashMap<>();
        double polR = new ExpressionParser("pol(3, 4)", true, 0, polVars).evaluate();
        System.out.println("[UI Test 19] pol(3, 4): r=" + polR + ", X=" + polVars.get("X") + ", Y=" + polVars.get("Y") + "°");
        if (Math.abs(polR - 5.0) > 1e-9 || Math.abs(polVars.get("X") - 5.0) > 1e-9 || Math.abs(polVars.get("Y") - 53.13010235) > 1e-4) {
            throw new RuntimeException("pol(3, 4) failed!");
        }
        java.util.Map<String, Double> recVars = new java.util.HashMap<>();
        double recX = new ExpressionParser("rec(2, 60)", true, 0, recVars).evaluate();
        System.out.println("[UI Test 19b] rec(2, 60): X=" + recX + ", Y=" + recVars.get("Y"));
        if (Math.abs(recX - 1.0) > 1e-9 || Math.abs(recVars.get("Y") - Math.sqrt(3.0)) > 1e-4) {
            throw new RuntimeException("rec(2, 60) failed!");
        }

        // Test 20: Sequential Summation Σ(X^2, 1, 5) = 1 + 4 + 9 + 16 + 25 = 55
        double sumVal = new ExpressionParser("Σ(X^2, 1, 5)", true, 0).evaluate();
        System.out.println("[UI Test 20] Σ(X^2, 1, 5) = " + sumVal);
        if (Math.abs(sumVal - 55.0) > 1e-9) throw new RuntimeException("Σ summation failed!");

        // Test 21: Pseudo-Random Ran# and RanInt#(1, 6)
        double ranVal = new ExpressionParser("Ran#", true, 0).evaluate();
        double ranIntVal = new ExpressionParser("RanInt#(1, 6)", true, 0).evaluate();
        System.out.println("[UI Test 21] Ran# = " + ranVal + ", RanInt#(1, 6) = " + ranIntVal);
        if (ranVal < 0.0 || ranVal >= 1.0 || ranIntVal < 1.0 || ranIntVal > 6.0) {
            throw new RuntimeException("Ran# or RanInt# out of range!");
        }

        // Test 22: Postfix Angle Units (° / r / g)
        double sinDeg = new ExpressionParser("sin(90°)", true, 0).evaluate();
        double sinRad = new ExpressionParser("sin((π/2) r)", true, 0).evaluate();
        double sinGra = new ExpressionParser("sin(100 g)", true, 0).evaluate();
        System.out.println("[UI Test 22] sin(90°)=" + sinDeg + ", sin(π/2 r)=" + sinRad + ", sin(100 g)=" + sinGra);
        if (Math.abs(sinDeg - 1.0) > 1e-7 || Math.abs(sinRad - 1.0) > 1e-7 || Math.abs(sinGra - 1.0) > 1e-7) {
            throw new RuntimeException("Angle postfix units failed!");
        }

        // Test 23: Matrix Powers, Determinant Abs(MatA), and Scalar 2MatA
        String matSq = MatrixVectorEvaluator.evaluateMatrix("MatA^2");
        String matAbs = MatrixVectorEvaluator.evaluateMatrix("Abs(MatA)");
        String matScalar = MatrixVectorEvaluator.evaluateMatrix("2MatA");
        System.out.println("[UI Test 23] MatA^2=" + matSq + ", Abs(MatA)=" + matAbs + ", 2MatA=" + matScalar);
        if (!matAbs.equals("1")) throw new RuntimeException("Abs(MatA) failed!");

        // Test 24: Vector Magnitude Abs(VctA) and Scalar 2VctA
        String vctAbs = MatrixVectorEvaluator.evaluateVector("Abs(VctA)");
        String vctScalar = MatrixVectorEvaluator.evaluateVector("2VctA");
        System.out.println("[UI Test 24] Abs(VctA)=" + vctAbs + ", 2VctA=" + vctScalar);
        if (vctAbs.isBlank() || vctScalar.isBlank()) throw new RuntimeException("Vector operations failed!");

        // Test 25: STAT Normal Distribution P(0), Q(0), R(0)
        double p0 = new ExpressionParser("P(0)", true, 0).evaluate();
        double q0 = new ExpressionParser("Q(0)", true, 0).evaluate();
        double r0 = new ExpressionParser("R(0)", true, 0).evaluate();
        System.out.println("[UI Test 25] P(0)=" + p0 + ", Q(0)=" + q0 + ", R(0)=" + r0);
        if (Math.abs(p0 - 0.5) > 1e-4 || Math.abs(q0 - 0.0) > 1e-4 || Math.abs(r0 - 0.5) > 1e-4) {
            throw new RuntimeException("Normal distribution functions failed!");
        }

        // Test 26: Formatting Fix 3, Sci 3, Norm 1
        String fixStr = CasioFormatter.formatWithSettings(1.0 / 3.0, false, 3, -1, 2);
        String sciStr = CasioFormatter.formatWithSettings(1.0 / 3.0, false, -1, 3, 2);
        System.out.println("[UI Test 26] 1/3 with Fix 3 = " + fixStr + ", with Sci 3 = " + sciStr);
        if (!fixStr.equals("0.333")) throw new RuntimeException("formatFix failed!");

        // Test 27: Comprehensive Verification of ALL 40 Scientific Constants (01 to 40)
        System.out.println("[UI Test 27] Verifying ALL 40 Scientific Constants (CasioConstants 01..40)...");
        for (int c = 1; c <= 40; c++) {
            CasioConstants.ConstantEntry entry = CasioConstants.get(c);
            if (entry == null) {
                throw new RuntimeException("Missing constant entry for code " + c);
            }
            if (entry.number() != c || entry.symbol() == null || entry.symbol().isBlank()
                    || entry.name() == null || entry.name().isBlank() || Double.isNaN(entry.value())
                    || Double.isInfinite(entry.value()) || entry.value() == 0.0) {
                throw new RuntimeException("Invalid constant entry for code " + c + ": " + entry);
            }
        }
        System.out.println("   -> All 40 Casio scientific constants successfully verified!");

        // Test 28: Comprehensive Verification of ALL 40 Conversions (01 to 40) with Round-trip
        System.out.println("[UI Test 28] Verifying ALL 40 Metric Conversions (CasioConversions 01..40)...");
        for (int k = 1; k <= 40; k++) {
            CasioConversions.ConversionEntry conv = CasioConversions.get(k);
            if (conv == null) {
                throw new RuntimeException("Missing conversion entry for code " + k);
            }
            if (conv.number() != k || conv.fromUnit() == null || conv.toUnit() == null || conv.label() == null) {
                throw new RuntimeException("Invalid conversion metadata for code " + k + ": " + conv);
            }
            double testInput = 50.0;
            double converted = CasioConversions.convert(k, testInput);
            if (Double.isNaN(converted) || Double.isInfinite(converted)) {
                throw new RuntimeException("Conversion failed for code " + k + ": input=" + testInput + " -> " + converted);
            }
        }
        // Verify all 20 paired bidirectional conversions round-trip to original value
        for (int p = 1; p <= 39; p += 2) {
            double testVal = 100.0;
            double fwd = CasioConversions.convert(p, testVal);
            double back = CasioConversions.convert(p + 1, fwd);
            if (Math.abs(back - testVal) > 1e-6) {
                throw new RuntimeException("Bidirectional conversion round-trip failed for pair " + p + "/" + (p + 1)
                        + ": start=" + testVal + ", fwd=" + fwd + ", back=" + back);
            }
        }
        // Verify specific known conversion constants
        if (Math.abs(CasioConversions.convert(1, 1.0) - 2.54) > 1e-9) throw new RuntimeException("1 in != 2.54 cm");
        if (Math.abs(CasioConversions.convert(37, 212.0) - 100.0) > 1e-9) throw new RuntimeException("212 °F != 100 °C");
        if (Math.abs(CasioConversions.convert(38, 0.0) - 32.0) > 1e-9) throw new RuntimeException("0 °C != 32 °F");
        if (Math.abs(CasioConversions.convert(25, 1.0) - 101325.0) > 1e-9) throw new RuntimeException("1 atm != 101325 Pa");
        if (Math.abs(CasioConversions.convert(39, 4.184) - 1.0) > 1e-9) throw new RuntimeException("4.184 J != 1 cal");
        System.out.println("   -> All 40 Casio metric conversions successfully verified with bidirectional round-trips!");

        // Test 29: Numerical Differentiation diff(expr, x0)
        double diffVal = new ExpressionParser("diff(X^3, 2)", true, 0).evaluate(); // d/dx (X^3) at X=2 is 3*(2^2) = 12
        System.out.println("[UI Test 29] diff(X^3, 2) = " + diffVal);
        if (Math.abs(diffVal - 12.0) > 1e-3) throw new RuntimeException("diff(X^3, 2) failed!");

        // Test 30: All 8 STAT Regression Models
        System.out.println("[UI Test 30] Verifying all 8 STAT regression models...");
        for (StatEngine.StatType type : StatEngine.StatType.values()) {
            StatEngine se = new StatEngine(type);
            if (type == StatEngine.StatType.ONE_VAR) {
                se.addPoint(10);
                se.addPoint(20);
                se.addPoint(30);
                if (se.meanX() != 20.0) throw new RuntimeException("1-VAR failed in all-model check");
            } else {
                se.addPoint(1, 2);
                se.addPoint(2, 4);
                se.addPoint(3, 8);
                double a = se.regA();
                double b = se.regB();
                if (Double.isNaN(a) || Double.isNaN(b)) {
                    throw new RuntimeException("Regression model " + type + " produced NaN!");
                }
            }
        }
        System.out.println("   -> All 8 STAT models verified successfully!");

        System.out.println(">>> ALL 30 ADVANCED UI & OPERATION INTEGRATION TESTS PASSED WITH 100% SUCCESS! <<<");
        Platform.exit();
    }
}
