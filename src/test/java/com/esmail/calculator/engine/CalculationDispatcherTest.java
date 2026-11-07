package com.esmail.calculator.engine;

import com.esmail.calculator.modes.BaseNEngine;
import com.esmail.calculator.modes.CalculatorMode;
import com.esmail.calculator.modes.MatrixEngine;
import com.esmail.calculator.modes.VectorEngine;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculationDispatcherTest {

    private CalculationDispatcher.Request request(String expression, CalculatorMode mode,
            Map<String, Double> variables) {
        return new CalculationDispatcher.Request(
                expression, mode, ExpressionParser.AngleUnit.DEG,
                0.0, 0.0, variables, true, false, true, false,
                -1, -1, 2);
    }

    @Test
    void compAndStatReturnTypedScalarAndHistoryEffects() {
        CalculationDispatcher dispatcher = new CalculationDispatcher();
        Map<String, Double> variables = new HashMap<>();

        CalculationDispatcher.Result result = dispatcher.dispatch(
                request("2+3", CalculatorMode.COMP, variables));

        assertEquals("5", result.displayText());
        assertEquals(5.0, result.answerUpdate());
        assertEquals(5.0, result.currentResultUpdate());
        assertEquals(5.0, result.historyValue());
        assertEquals(0.0, variables.get("M"));
        assertEquals(0.0, variables.get("ANS"));
    }

    @Test
    void baseNRequiresAndUsesTheExplicitRadix() {
        CalculationDispatcher dispatcher = new CalculationDispatcher();
        CalculationDispatcher.Request request =
                request("10+1", CalculatorMode.BASE_N, new HashMap<>());

        assertThrows(IllegalArgumentException.class, () -> dispatcher.dispatch(request));
        CalculationDispatcher.Result result = dispatcher.dispatch(request, BaseNEngine.Base.BIN);

        assertEquals(BaseNEngine.format(11, BaseNEngine.Base.BIN), result.displayText());
        assertNull(result.answerUpdate());
        assertNull(result.currentResultUpdate());
        assertEquals(11.0, result.historyValue());
    }

    @Test
    void matrixAndVectorScalarResultsSynchronizeAns() {
        MatrixEngine.setMatrix("MATA", new MatrixEngine.Matrix(3, 3, new double[]{
                1, 0, 0, 0, 1, 0, 0, 0, 1
        }));
        CalculationDispatcher dispatcher = new CalculationDispatcher();

        CalculationDispatcher.Result matrix = dispatcher.dispatch(
                request("det(MatA)", CalculatorMode.MATRIX, new HashMap<>()));
        assertEquals("1", matrix.displayText());
        assertEquals(1.0, matrix.answerUpdate());
        assertEquals(1.0, matrix.currentResultUpdate());

        VectorEngine.setVector("VCTA", new VectorEngine.Vector3D(1, 2, 3));
        VectorEngine.setVector("VCTB", new VectorEngine.Vector3D(4, 5, 6));
        CalculationDispatcher.Result vector = dispatcher.dispatch(
                request("VctA dot VctB", CalculatorMode.VECTOR, new HashMap<>()));
        assertEquals("32", vector.displayText());
        assertEquals(32.0, vector.answerUpdate());
        assertEquals(32.0, vector.historyValue());
    }

    @Test
    void complexResultDoesNotOverwriteScalarAnsButIsRecordedForReplay() {
        CalculationDispatcher dispatcher = new CalculationDispatcher();
        CalculationDispatcher.Result result = dispatcher.dispatch(
                request("1+2i", CalculatorMode.CMPLX, new HashMap<>()));

        assertEquals("1+2i", result.displayText());
        assertNull(result.answerUpdate());
        assertNull(result.currentResultUpdate());
        assertEquals(1.0, result.historyValue());
    }
}
