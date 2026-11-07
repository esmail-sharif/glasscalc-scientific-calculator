package com.esmail.calculator.modes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatEditorSessionTest {

    @Test
    void oneVariableRowsCommitOnlyAfterXAndBlankCellsDefaultToZero() {
        StatEditorSession session = new StatEditorSession();

        assertEquals("X", session.columnLabel());
        assertTrue(session.acceptCurrentCell().accepted());

        assertEquals(1, session.engine().count());
        assertEquals(1.0, session.engine().n(), 0.0);
        assertEquals(0.0, session.rows().get(0).x(), 0.0);
        assertEquals("X", session.columnLabel());
    }

    @Test
    void pairedRowsCollectXThenYBeforeCommit() {
        StatEditorSession session = new StatEditorSession(StatEngine.StatType.LINEAR);
        session.append("2");
        assertTrue(session.acceptCurrentCell().accepted());
        assertEquals(0, session.engine().count());
        assertEquals("Y", session.columnLabel());

        session.append("5");
        assertTrue(session.acceptCurrentCell().accepted());
        assertEquals(1, session.engine().count());
        assertEquals(2.0, session.rows().get(0).x(), 0.0);
        assertEquals(5.0, session.rows().get(0).y(), 0.0);
        assertEquals(1.0, session.rows().get(0).frequency(), 0.0);
    }

    @Test
    void frequencyColumnIsAnOptionalFinalCellAndBlankMeansOne() {
        StatEditorSession session = new StatEditorSession(StatEngine.StatType.LINEAR);
        session.setFrequencyEnabled(true);

        session.append("3");
        assertTrue(session.acceptCurrentCell().accepted());
        session.append("8");
        assertTrue(session.acceptCurrentCell().accepted());
        assertEquals("FREQ", session.columnLabel());
        assertTrue(session.acceptCurrentCell().accepted());

        assertEquals(1.0, session.engine().n(), 0.0);
        assertEquals(1.0, session.rows().get(0).frequency(), 0.0);
    }

    @Test
    void invalidFrequencyKeepsTheCurrentCellAndDoesNotCommitTheRow() {
        StatEditorSession session = new StatEditorSession(StatEngine.StatType.ONE_VAR);
        session.setFrequencyEnabled(true);
        session.append("12");
        assertTrue(session.acceptCurrentCell().accepted());

        session.append("1.5");
        StatEditorSession.AcceptResult invalid = session.acceptCurrentCell();

        assertFalse(invalid.accepted());
        assertTrue(invalid.errorMessage().contains("positive integer"));
        assertEquals(2, session.dataStep());
        assertEquals("1.5", session.accumulator());
        assertEquals(0, session.engine().count());
        assertEquals(0.0, session.engine().n(), 0.0);
    }

    @Test
    void malformedAndNonFiniteInputRemainsEditable() {
        StatEditorSession session = new StatEditorSession();
        session.append("abc");
        StatEditorSession.AcceptResult malformed = session.acceptCurrentCell();

        assertFalse(malformed.accepted());
        assertEquals("Input ERROR", malformed.errorMessage());
        assertEquals("abc", session.accumulator());

        session.backspace();
        session.backspace();
        session.backspace();
        session.append("NaN");
        assertFalse(session.acceptCurrentCell().accepted());
        assertEquals(0, session.engine().count());
    }

    @Test
    void frequencySettingChangesClearRowsAndResetTheEditor() {
        StatEditorSession session = new StatEditorSession();
        session.append("7");
        assertTrue(session.acceptCurrentCell().accepted());
        assertEquals(1, session.engine().count());

        assertTrue(session.setFrequencyEnabled(true));
        assertEquals(0, session.engine().count());
        assertTrue(session.rows().isEmpty());
        assertEquals(0, session.dataStep());
        assertEquals("", session.accumulator());
        assertFalse(session.setFrequencyEnabled(true));
    }

    @Test
    void gridRowOperationsRebuildTheEngineFromAnAtomicSnapshot() {
        StatEditorSession session = new StatEditorSession(StatEngine.StatType.LINEAR);
        session.replaceRows(List.of(
                new StatEditorSession.Observation(1, 2, 1),
                new StatEditorSession.Observation(2, 4, 3)));

        assertEquals(2, session.engine().count());
        assertEquals(4.0, session.engine().n(), 0.0);

        session.updateRow(0, new StatEditorSession.Observation(3, 6, 2));
        assertEquals(5.0, session.engine().n(), 0.0);
        assertEquals(3.0, session.rows().get(0).x(), 0.0);

        session.removeRow(1);
        assertEquals(1, session.engine().count());
        assertEquals(2.0, session.engine().n(), 0.0);
    }

    @Test
    void invalidGridReplacementDoesNotDestroyExistingData() {
        StatEditorSession session = new StatEditorSession();
        session.replaceRows(List.of(new StatEditorSession.Observation(4, 0, 1)));

        assertThrows(ArithmeticException.class, () -> session.replaceRows(List.of(
                new StatEditorSession.Observation(2, 0, 1),
                new StatEditorSession.Observation(3, 0, 1.5))));

        assertEquals(1, session.engine().count());
        assertEquals(4.0, session.rows().get(0).x(), 0.0);
    }
}
