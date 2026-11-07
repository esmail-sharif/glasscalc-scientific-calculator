package com.esmail.calculator;

import com.esmail.calculator.engine.ExpressionParser;
import com.esmail.calculator.modes.CalculatorMode;
import com.esmail.calculator.modes.BaseNEngine;
import com.esmail.calculator.modes.MatrixEngine;
import com.esmail.calculator.modes.VectorEngine;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaFX smoke and routing tests for the real GlassCalcApp keypad.
 *
 * These tests build the production keypad and fire its actual Buttons. They do
 * not claim pixel/screenshot equivalence; they verify control construction,
 * action wiring, menu routing, and representative end-to-end calculation paths.
 */
class GlassCalcUiCoverageTest {
    private static volatile Throwable toolkitFailure;
    private static volatile boolean toolkitStarted;

    @BeforeAll
    static void startJavaFxToolkit() {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        } catch (Throwable failure) {
            toolkitFailure = failure;
        }

        try {
            if (toolkitFailure == null && !started.await(15, TimeUnit.SECONDS)) {
                toolkitFailure = new IllegalStateException("JavaFX toolkit startup timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            toolkitFailure = interrupted;
        }

        if (toolkitFailure == null) {
            try {
                Platform.setImplicitExit(false);
                toolkitStarted = true;
            } catch (Throwable failure) {
                toolkitFailure = failure;
            }
        }
    }

    @AfterAll
    static void stopJavaFxToolkit() {
        if (toolkitStarted) Platform.exit();
    }

    @Test
    void everyRenderedButtonHasAnActionHandler() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            List<Button> buttons = findButtons(fixture.root());
            assertTrue(buttons.size() >= 45,
                    "Expected the full physical-style keypad, found " + buttons.size() + " buttons");
            for (Button button : buttons) {
                assertNotNull(button.getOnAction(),
                        "Button is rendered without an action handler: " + button.getText());
            }
            return null;
        });
    }

    @Test
    void allEightModeMenuChoicesRouteToTheirExpectedModeAndState() throws Exception {
        String[] expectedStates = {
                "NORMAL", "NORMAL", "STAT_TYPE_SELECT", "NORMAL",
                "EQN_TYPE_SELECT", "NORMAL", "TABLE_FX_INPUT", "NORMAL"
        };
        CalculatorMode[] expectedModes = {
                CalculatorMode.COMP, CalculatorMode.CMPLX, CalculatorMode.STAT, CalculatorMode.BASE_N,
                CalculatorMode.EQN, CalculatorMode.MATRIX, CalculatorMode.TABLE, CalculatorMode.VECTOR
        };

        for (int i = 0; i < 8; i++) {
            final int selection = i + 1;
            onFxThread(() -> {
                Fixture fixture = newFixture();
                click(fixture, "MODE");
                assertEquals("MODE_MENU", field(fixture.app(), "uiState").toString());
                click(fixture, Integer.toString(selection));
                assertEquals(expectedModes[selection - 1], field(fixture.app(), "currentMode"));
                assertEquals(expectedStates[selection - 1], field(fixture.app(), "uiState").toString());
                return null;
            });
        }
    }

    @Test
    void shiftModeOpensSetupAndDegreeModeSelectionIsApplied() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "SHIFT");
            click(fixture, "MODE");
            assertEquals("SETUP_MENU", field(fixture.app(), "uiState").toString());
            assertTrue(textField(fixture.app()).getText().contains("1:MthIO"));

            click(fixture, "4");
            assertEquals(ExpressionParser.AngleUnit.RAD, field(fixture.app(), "angleUnit"));
            assertEquals("NORMAL", field(fixture.app(), "uiState").toString());
            return null;
        });
    }

    @Test
    void shiftShortcutsReachTheirDedicatedModeMenusAndPrompts() throws Exception {
        String[][] cases = {
                {"7", "CONST_PROMPT"},
                {"8", "CONV_PROMPT"},
                {"1", "STAT_MENU"},
                {"4", "MAT_MENU"},
                {"5", "VCT_MENU"}
        };

        for (String[] testCase : cases) {
            onFxThread(() -> {
                Fixture fixture = newFixture();
                click(fixture, "SHIFT");
                click(fixture, testCase[0]);
                assertEquals(testCase[1], field(fixture.app(), "uiState").toString());
                return null;
            });
        }
    }

    @Test
    void equalsButtonExecutesExpressionAndAnsCanBeChained() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("2+3");
            click(fixture, "=");
            assertEquals("5", label(fixture.app(), "resultLabel").getText());

            textField(fixture.app()).setText("Ans+2");
            click(fixture, "=");
            assertEquals("7", label(fixture.app(), "resultLabel").getText());
            assertEquals(7.0, (double) field(fixture.app(), "answer"), 1e-12);
            return null;
        });
    }

    @Test
    void matrixAndVectorScalarResultsSynchronizeAns() throws Exception {
        onFxThread(() -> {
            MatrixEngine.setMatrix("MATA", new MatrixEngine.Matrix(3, 3, new double[]{
                    1, 0, 0, 0, 1, 0, 0, 0, 1
            }));
            Fixture matrix = newFixture();
            invoke(matrix.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.MATRIX);
            textField(matrix.app()).setText("det(MatA)");
            click(matrix, "=");
            assertEquals(1.0, (double) field(matrix.app(), "answer"), 1e-12);
            assertEquals("1", label(matrix.app(), "resultLabel").getText());

            VectorEngine.setVector("VCTA", new VectorEngine.Vector3D(1, 2, 3));
            VectorEngine.setVector("VCTB", new VectorEngine.Vector3D(4, 5, 6));
            Fixture vector = newFixture();
            invoke(vector.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.VECTOR);
            textField(vector.app()).setText("VctA dot VctB");
            click(vector, "=");
            assertEquals(32.0, (double) field(vector.app(), "answer"), 1e-12);
            assertEquals("32", label(vector.app(), "resultLabel").getText());
            return null;
        });
    }

    private static Fixture newFixture() throws Exception {
        GlassCalcApp app = new GlassCalcApp();
        Method builder = GlassCalcApp.class.getDeclaredMethod("buildCalculatorFrame");
        builder.setAccessible(true);
        VBox root = (VBox) builder.invoke(app);
        return new Fixture(app, root);
    }

    private static void click(Fixture fixture, String text) {
        Button button = findButtons(fixture.root()).stream()
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No button found with text: " + text));
        button.fire();
    }

    private static List<Button> findButtons(Parent root) {
        List<Button> found = new ArrayList<>();
        collectButtons(root, found);
        return found;
    }

    private static void collectButtons(Node node, List<Button> found) {
        if (node instanceof Button button) found.add(button);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) collectButtons(child, found);
        }
    }

    private static TextField textField(GlassCalcApp app) throws Exception {
        return (TextField) field(app, "expressionField");
    }

    private static Label label(GlassCalcApp app, String name) throws Exception {
        return (Label) field(app, name);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static <T> T onFxThread(Callable<T> work) throws Exception {
        if (toolkitFailure != null) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "JavaFX UI test environment unavailable: " + toolkitFailure);
        }
        if (Platform.isFxApplicationThread()) return work.call();

        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(20, TimeUnit.SECONDS);
    }

    private record Fixture(GlassCalcApp app, VBox root) {}

    @Test
    void tablePromptFirstInputReplacesDisplayedDefaultsAndNavigatesRows() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "MODE");
            click(fixture, "7");
            textField(fixture.app()).setText("f(X)=X^2");
            click(fixture, "=");

            assertEquals("TABLE_START_INPUT", field(fixture.app(), "uiState").toString());
            assertEquals("1", label(fixture.app(), "resultLabel").getText());
            click(fixture, "2");
            assertEquals("2", label(fixture.app(), "resultLabel").getText(),
                    "First typed value must replace the default Start value, not append to it");
            click(fixture, "=");

            click(fixture, "4");
            assertEquals("4", label(fixture.app(), "resultLabel").getText());
            click(fixture, "=");

            click(fixture, "2");
            assertEquals("2", label(fixture.app(), "resultLabel").getText());
            click(fixture, "=");

            assertEquals("TABLE_VIEW", field(fixture.app(), "uiState").toString());
            assertEquals("[1] X = 2", textField(fixture.app()).getText());
            assertEquals("F(X)=4", label(fixture.app(), "resultLabel").getText());

            click(fixture, "▼");
            assertEquals("[2] X = 4", textField(fixture.app()).getText());
            assertEquals("F(X)=16", label(fixture.app(), "resultLabel").getText());
            return null;
        });
    }

    @Test
    void quadraticEquationCoefficientsCanBeEnteredThroughTheKeypad() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "MODE");
            click(fixture, "5");
            assertEquals("EQN_TYPE_SELECT", field(fixture.app(), "uiState").toString());
            click(fixture, "3");
            assertEquals("EQN_COEFF_INPUT", field(fixture.app(), "uiState").toString());

            click(fixture, "1");
            click(fixture, "=");
            click(fixture, "−");
            click(fixture, "3");
            click(fixture, "=");
            click(fixture, "2");
            click(fixture, "=");

            assertEquals("EQN_RESULT_VIEW", field(fixture.app(), "uiState").toString());
            assertTrue(label(fixture.app(), "resultLabel").getText().equals("2")
                    || label(fixture.app(), "resultLabel").getText().equals("1"),
                    "Quadratic equation x²-3x+2=0 should show one of its real roots");
            return null;
        });
    }

    @Test
    void statDataEntryAndSummaryMenuReturnCorrectMean() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "MODE");
            click(fixture, "3");
            click(fixture, "1");
            assertEquals("STAT_DATA_INPUT", field(fixture.app(), "uiState").toString());

            click(fixture, "2");
            click(fixture, "=");
            click(fixture, "4");
            click(fixture, "=");
            click(fixture, "SHIFT");
            click(fixture, "1");
            assertEquals("STAT_MENU", field(fixture.app(), "uiState").toString());
            click(fixture, "4");
            click(fixture, "2");
            click(fixture, "=");

            assertEquals("3", label(fixture.app(), "resultLabel").getText());
            assertEquals("NORMAL", field(fixture.app(), "uiState").toString());
            return null;
        });
    }

    @Test
    void complexModeAndBaseNBaseSelectorButtonsReachTheirEngines() throws Exception {
        onFxThread(() -> {
            Fixture complex = newFixture();
            click(complex, "MODE");
            click(complex, "2");
            textField(complex.app()).setText("(2+3i)*(4-5i)");
            click(complex, "=");
            assertTrue(label(complex.app(), "resultLabel").getText().contains("23"));
            assertTrue(label(complex.app(), "resultLabel").getText().contains("2i"));

            Fixture base = newFixture();
            invoke(base.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.BASE_N);
            click(base, "x^■");
            assertEquals(BaseNEngine.Base.HEX, field(base.app(), "currentBase"));
            textField(base.app()).setText("A+F");
            click(base, "=");
            assertEquals("19", label(base.app(), "resultLabel").getText());

            click(base, "log");
            assertEquals(BaseNEngine.Base.BIN, field(base.app(), "currentBase"));
            textField(base.app()).setText("1010+1");
            click(base, "=");
            assertEquals("1011", label(base.app(), "resultLabel").getText());
            return null;
        });
    }

    @Test
    void solveShortcutAcceptsAnEditedInitialGuess() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("2X-10=0");
            click(fixture, "SHIFT");
            click(fixture, "CALC");
            assertEquals("SOLVE_PROMPT", field(fixture.app(), "uiState").toString());
            click(fixture, "4");
            assertEquals("4", label(fixture.app(), "resultLabel").getText());
            click(fixture, "=");

            assertEquals("NORMAL", field(fixture.app(), "uiState").toString());
            assertTrue(textField(fixture.app()).getText().contains("5"),
                    "SOLVE should find X=5 for 2X-10=0");
            assertTrue(label(fixture.app(), "resultLabel").getText().contains("L-R"));
            return null;
        });
    }

    @Test
    void alphaAndShiftModifiersInsertTheirPrintedFunctionLegends() throws Exception {
        onFxThread(() -> {
            Fixture alpha = newFixture();
            click(alpha, "ALPHA");
            click(alpha, "(-)");
            assertEquals("A", textField(alpha.app()).getText());

            Fixture shift = newFixture();
            click(shift, "SHIFT");
            click(shift, "sin");
            assertEquals("asin(", textField(shift.app()).getText());
            return null;
        });
    }

    @Test
    void keyboardEnterBackspaceAndEscapeReachTheirHandlers() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("2+2");
            invoke(fixture.app(), "handlePhysicalKey", new Class<?>[]{KeyEvent.class},
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
            assertEquals("4", label(fixture.app(), "resultLabel").getText());

            click(fixture, "1");
            click(fixture, "2");
            click(fixture, "3");
            textField(fixture.app()).positionCaret(3);
            invoke(fixture.app(), "handlePhysicalKey", new Class<?>[]{KeyEvent.class},
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE, false, false, false, false));
            assertEquals("12", textField(fixture.app()).getText());

            invoke(fixture.app(), "handlePhysicalKey", new Class<?>[]{KeyEvent.class},
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
            assertEquals("", textField(fixture.app()).getText());
            return null;
        });
    }

    @Test
    void calcPromptsForVariablesAndEvaluatesWithTheEnteredValue() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("A+1");
            click(fixture, "CALC");
            assertEquals("CALC_PROMPT", field(fixture.app(), "uiState").toString());
            assertEquals("A?", textField(fixture.app()).getText());

            click(fixture, "4");
            assertEquals("4", label(fixture.app(), "resultLabel").getText());
            click(fixture, "=");

            assertEquals("NORMAL", field(fixture.app(), "uiState").toString());
            assertEquals("5", label(fixture.app(), "resultLabel").getText());
            assertEquals(5.0, (double) field(fixture.app(), "answer"), 1e-12);
        return null;
        });
    }

    @Test
    void stoRecallAndMemoryPlusButtonsUpdateStoredValues() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("42");
            click(fixture, "=");

            click(fixture, "SHIFT");
            click(fixture, "RCL");
            click(fixture, "(-)");
            assertEquals(42.0, (double) ((java.util.Map<?, ?>) field(fixture.app(), "variables")).get("A"), 1e-12);

            textField(fixture.app()).setText("A*2");
            click(fixture, "=");
            assertEquals("84", label(fixture.app(), "resultLabel").getText());

            click(fixture, "M+");
            assertEquals(84.0, (double) field(fixture.app(), "memoryM"), 1e-12);
            textField(fixture.app()).setText("M");
            click(fixture, "=");
            assertEquals("84", label(fixture.app(), "resultLabel").getText());

            click(fixture, "RCL");
            assertEquals("Ans", textField(fixture.app()).getText());
            return null;
        });
    }

    @Test
    void constAndConversionShiftPromptsUseTheSelectedCodes() throws Exception {
        onFxThread(() -> {
            Fixture constant = newFixture();
            click(constant, "SHIFT");
            click(constant, "7");
            assertEquals("CONST_PROMPT", field(constant.app(), "uiState").toString());
            click(constant, "2");
            click(constant, "8");
            assertEquals("NORMAL", field(constant.app(), "uiState").toString());
            assertEquals(com.esmail.calculator.modes.CasioConstants.get(28).symbol(),
                    textField(constant.app()).getText());

            Fixture conversion = newFixture();
            textField(conversion.app()).setText("10");
            click(conversion, "=");
            click(conversion, "SHIFT");
            click(conversion, "8");
            assertEquals("CONV_PROMPT", field(conversion.app(), "uiState").toString());
            click(conversion, "0");
            click(conversion, "1");
            double convertedValue = com.esmail.calculator.modes.CasioConversions.convert(1, 10.0);
            assertEquals(com.esmail.calculator.engine.CasioFormatter.toExactOrFraction(convertedValue),
                    label(conversion.app(), "resultLabel").getText());
            assertEquals(convertedValue, (double) field(conversion.app(), "answer"), 1e-12,
                    "A conversion result must become the next Ans value");
            assertEquals("NORMAL", field(conversion.app(), "uiState").toString());
            return null;
        });
    }

    @Test
    void shiftBaseComplexAndDrgMenusInsertTheirOperations() throws Exception {
        onFxThread(() -> {
            Fixture base = newFixture();
            click(base, "SHIFT");
            click(base, "3");
            assertEquals("BASE_MENU", field(base.app(), "uiState").toString());
            click(base, "3");
            assertTrue(textField(base.app()).getText().contains("xor"));

            Fixture complex = newFixture();
            click(complex, "SHIFT");
            click(complex, "2");
            assertEquals("CMPLX_MENU", field(complex.app(), "uiState").toString());
            click(complex, "5");
            assertEquals("Re(", textField(complex.app()).getText());

            Fixture drg = newFixture();
            click(drg, "SHIFT");
            click(drg, "Ans");
            assertEquals("DRG_MENU", field(drg.app(), "uiState").toString());
            click(drg, "2");
            assertEquals("r", textField(drg.app()).getText());
            return null;
        });
    }

    @Test
    void threeVariableEquationCanBeEnteredAndResultsNavigated() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "MODE");
            click(fixture, "5");
            click(fixture, "2");
            assertEquals("EQN_COEFF_INPUT", field(fixture.app(), "uiState").toString());

            String[] coefficients = {"2", "1", "1", "9", "1", "2", "1", "8", "1", "1", "2", "7"};
            for (String coefficient : coefficients) {
                click(fixture, coefficient);
                click(fixture, "=");
            }

            assertEquals("EQN_RESULT_VIEW", field(fixture.app(), "uiState").toString());
            assertEquals("X", textField(fixture.app()).getText());
            assertEquals("3", label(fixture.app(), "resultLabel").getText());
            click(fixture, "▼");
            assertEquals("Y", textField(fixture.app()).getText());
            assertEquals("2", label(fixture.app(), "resultLabel").getText());
            click(fixture, "▼");
            assertEquals("Z", textField(fixture.app()).getText());
            assertEquals("1", label(fixture.app(), "resultLabel").getText());
            return null;
        });
    }

    @Test
    void cubicEquationCanBeEnteredAndItsThreeRootsNavigated() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "MODE");
            click(fixture, "5");
            click(fixture, "4");
            assertEquals("EQN_COEFF_INPUT", field(fixture.app(), "uiState").toString());

            click(fixture, "1");
            click(fixture, "=");
            click(fixture, "−");
            click(fixture, "6");
            click(fixture, "=");
            click(fixture, "1");
            click(fixture, "1");
            click(fixture, "=");
            click(fixture, "−");
            click(fixture, "6");
            click(fixture, "=");

            assertEquals("EQN_RESULT_VIEW", field(fixture.app(), "uiState").toString());
            assertEquals("X1", textField(fixture.app()).getText());
            assertEquals("3", label(fixture.app(), "resultLabel").getText());
            click(fixture, "▼");
            assertEquals("X2", textField(fixture.app()).getText());
            assertEquals("1", label(fixture.app(), "resultLabel").getText());
            click(fixture, "▼");
            assertEquals("X3", textField(fixture.app()).getText());
            assertEquals("2", label(fixture.app(), "resultLabel").getText());
            return null;
        });
    }
}
