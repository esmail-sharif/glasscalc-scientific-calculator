package com.esmail.calculator.modes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BaseNEngineTest {

    @Test
    void testPrecedenceMultiplicationOverAddition() {
        // 2 + 3 * 4 should be 14, not 20
        assertEquals(14, BaseNEngine.evaluate("2 + 3 * 4", BaseNEngine.Base.DEC));
        assertEquals(14, BaseNEngine.evaluate("2 + 3 × 4", BaseNEngine.Base.DEC));
    }

    @Test
    void testParentheses() {
        assertEquals(20, BaseNEngine.evaluate("(2 + 3) * 4", BaseNEngine.Base.DEC));
        assertEquals(20, BaseNEngine.evaluate("(2 + 3) × 4", BaseNEngine.Base.DEC));
        assertEquals(30, BaseNEngine.evaluate("2 * (3 + 4 * 3)", BaseNEngine.Base.DEC));
    }

    @Test
    void testChainedMultiOperations() {
        assertEquals(18, BaseNEngine.evaluate("20 - 4 / 2", BaseNEngine.Base.DEC));
        assertEquals(10, BaseNEngine.evaluate("100 / 5 / 2", BaseNEngine.Base.DEC));
        assertEquals(23, BaseNEngine.evaluate("5 + 2 * 10 - 2", BaseNEngine.Base.DEC));
    }

    @Test
    void testBitwisePrecedenceAndOperations() {
        // 'and' has higher precedence than 'or'
        // 10 and 12 = 8. Then 8 or 5 = 13.
        assertEquals(13, BaseNEngine.evaluate("10 and 12 or 5", BaseNEngine.Base.DEC));

        // 'xor' and 'xnor'
        assertEquals(6, BaseNEngine.evaluate("5 xor 3", BaseNEngine.Base.DEC));
        assertEquals(~(5L ^ 3L), BaseNEngine.evaluate("5 xnor 3", BaseNEngine.Base.DEC));
    }

    @Test
    void testUnaryBitwiseAndNegative() {
        assertEquals(~15L, BaseNEngine.evaluate("not(15)", BaseNEngine.Base.DEC));
        assertEquals(~15L, BaseNEngine.evaluate("not 15", BaseNEngine.Base.DEC));
        assertEquals(-15L, BaseNEngine.evaluate("neg(15)", BaseNEngine.Base.DEC));
        assertEquals(-15L, BaseNEngine.evaluate("-15", BaseNEngine.Base.DEC));
    }

    @Test
    void testHexadecimalEvaluation() {
        // In HEX: A = 10, F = 15. A + F = 25 (0x19)
        long res = BaseNEngine.evaluate("A + F", BaseNEngine.Base.HEX);
        assertEquals(25, res);
        assertEquals("19", BaseNEngine.format(res, BaseNEngine.Base.HEX));

        // 2A * 3 = 42 * 3 = 126 (0x7E)
        long mulRes = BaseNEngine.evaluate("2A * 3", BaseNEngine.Base.HEX);
        assertEquals(126, mulRes);
        assertEquals("7E", BaseNEngine.format(mulRes, BaseNEngine.Base.HEX));
    }

    @Test
    void testBinaryEvaluation() {
        // In BIN: 1010 (10) + 101 (5) = 1111 (15)
        long res = BaseNEngine.evaluate("1010 + 101", BaseNEngine.Base.BIN);
        assertEquals(15, res);
        assertEquals("1111", BaseNEngine.format(res, BaseNEngine.Base.BIN));
    }

    @Test
    void testOctalEvaluation() {
        // In OCT: 17 (15) + 1 (1) = 20 (16)
        long res = BaseNEngine.evaluate("17 + 1", BaseNEngine.Base.OCT);
        assertEquals(16, res);
        assertEquals("20", BaseNEngine.format(res, BaseNEngine.Base.OCT));
    }

    @Test
    void testRadixConstraintsEnforcement() {
        // Binary mode should reject digits >= 2
        assertThrows(IllegalArgumentException.class, () -> BaseNEngine.evaluate("102", BaseNEngine.Base.BIN));

        // Octal mode should reject digits 8, 9
        assertThrows(IllegalArgumentException.class, () -> BaseNEngine.evaluate("18", BaseNEngine.Base.OCT));

        // Decimal mode should reject letters A-F
        assertThrows(IllegalArgumentException.class, () -> BaseNEngine.evaluate("1A", BaseNEngine.Base.DEC));
    }

    @Test
    void testDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> BaseNEngine.evaluate("10 / 0", BaseNEngine.Base.DEC));
        assertThrows(ArithmeticException.class, () -> BaseNEngine.evaluate("10 ÷ 0", BaseNEngine.Base.DEC));
    }
}
