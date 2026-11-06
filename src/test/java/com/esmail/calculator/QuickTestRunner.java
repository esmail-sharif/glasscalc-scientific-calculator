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

        // Test 3: cos(54 with auto-closing paren
        double cos54 = new ExpressionParser("cos(54", true, 0).evaluate();
        double expectedCos54 = Math.cos(Math.toRadians(54));
        System.out.println("[Test 3] cos(54 auto-close: " + cos54 + " (expected: " + expectedCos54 + ")");
        if (Math.abs(cos54 - expectedCos54) > 1e-9) {
            throw new RuntimeException("cos(54 mismatch!");
        }

        // Test 4: Formatter on cos(54)
        String cos54Formatted = CasioFormatter.toExactOrFraction(cos54);
        System.out.println("[Test 4] CasioFormatter cos(54): " + cos54Formatted);

        // Test 5: Integration: ∫(sin(X), 0, pi/3) = 1/2
        double integral = new ExpressionParser("∫(sin(X), 0, pi/3)", false, 0).evaluate();
        System.out.println("[Test 5] Numerical integral ∫(sin(X), 0, pi/3): " + integral);
        if (Math.abs(integral - 0.5) > 1e-4) {
            throw new RuntimeException("Integral result mismatch: " + integral);
        }
        String integralFormatted = CasioFormatter.toExactOrFraction(integral);
        System.out.println("[Test 5b] Formatted: " + integralFormatted + " (expected: 1/2)");
        if (!integralFormatted.equals("1/2")) {
            throw new RuntimeException("Expected 1/2, got: " + integralFormatted);
        }

        // Test 6: Permutations and combinations
        double p52 = new ExpressionParser("5P2", true, 0).evaluate();
        double c52 = new ExpressionParser("5C2", true, 0).evaluate();
        System.out.println("[Test 6] 5P2 = " + p52 + ", 5C2 = " + c52);
        if (p52 != 20.0 || c52 != 10.0) {
            throw new RuntimeException("Permutation/combination error!");
        }

        // Test 7: Ans chaining
        double ansPlus = new ExpressionParser("ans+10", true, 25.0).evaluate();
        System.out.println("[Test 7] ans+10 with ans=25: " + ansPlus);
        if (ansPlus != 35.0) {
            throw new RuntimeException("Ans chaining failed!");
        }

        // Test 8: Assembly math operations
        double addVal = MathEngine.add(12.5, 7.5);
        double subVal = MathEngine.subtract(20.0, 5.0);
        double mulVal = MathEngine.multiply(6.0, 7.0);
        double divVal = MathEngine.divide(100.0, 4.0);
        double sqrtVal = MathEngine.sqrt(144.0);
        double sinVal = MathEngine.sin(Math.toRadians(30));
        System.out.println("[Test 8] Asm ops: add=" + addVal + ", sub=" + subVal + ", mul=" + mulVal + ", div=" + divVal + ", sqrt=" + sqrtVal + ", sin30=" + sinVal);
        if (addVal != 20.0 || subVal != 15.0 || mulVal != 42.0 || divVal != 25.0 || sqrtVal != 12.0 || Math.abs(sinVal - 0.5) > 1e-9) {
            throw new RuntimeException("Assembly operations verification failed!");
        }

        System.out.println(">>> ALL VERIFICATION TESTS PASSED SUCCESSFULLY! <<<");
    }
}
