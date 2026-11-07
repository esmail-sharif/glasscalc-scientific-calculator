package com.esmail.calculator.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculationHistoryTest {

    @Test
    void seededEntryIsAvailableOnFirstReplayUp() {
        CalculationHistory history = new CalculationHistory();
        history.seed("sin(30)", 0.5);

        assertEquals(1, history.size());
        assertEquals(-1, history.cursor());
        assertTrue(history.canPrevious());

        CalculationHistory.Entry entry = history.previous();
        assertNotNull(entry);
        assertEquals("sin(30)", entry.expression());
        assertEquals(0.5, entry.result(), 1e-12);
        assertEquals(0, history.cursor());
    }

    @Test
    void previousAndNextNavigateInOrderAndReturnToIdle() {
        CalculationHistory history = new CalculationHistory();
        history.record("1+1", 2);
        history.record("2+3", 5);
        history.record("3+4", 7);

        assertEquals(2, history.cursor());
        assertTrue(history.canPrevious());
        assertFalse(history.canNext());

        assertEquals("2+3", history.previous().expression());
        assertEquals("1+1", history.previous().expression());
        assertNull(history.previous(), "The first entry has no earlier predecessor");
        assertEquals(0, history.cursor());

        assertEquals("2+3", history.next().expression());
        assertEquals("3+4", history.next().expression());
        assertTrue(history.canPrevious());
        assertFalse(history.canNext());

        assertNull(history.next(), "Moving past the latest entry returns the replay cursor to idle");
        assertEquals(-1, history.cursor());
        assertTrue(history.canPrevious());
        assertFalse(history.canNext());
    }

    @Test
    void newCalculationMovesReplayCursorToNewestEntry() {
        CalculationHistory history = new CalculationHistory();
        history.seed("example", 0.5);
        assertEquals("example", history.previous().expression());

        history.record("Ans+1", 1.5);
        assertEquals(1, history.cursor());
        assertEquals("example", history.previous().expression());
        assertEquals("Ans+1", history.next().expression());
    }

    @Test
    void emptyHistoryNavigationIsSafe() {
        CalculationHistory history = new CalculationHistory();
        assertTrue(history.isEmpty());
        assertFalse(history.canPrevious());
        assertFalse(history.canNext());
        assertNull(history.previous());
        assertNull(history.next());
    }
}
