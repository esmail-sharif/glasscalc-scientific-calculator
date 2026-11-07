package com.esmail.calculator;

import com.esmail.calculator.engine.ExpressionParser;
import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.modes.CalculatorMode;
import com.esmail.calculator.modes.CasioConstants;
import com.esmail.calculator.modes.CasioConversions;
import com.esmail.calculator.modes.StatEngine;
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
import javafx.scene.Scene;
import javafx.stage.Stage;
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

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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

    @Test
    void setupStatFreqColumnSupportsOneVariableWeightedRowsAndOffClearsData() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "SHIFT");
            click(fixture, "MODE");
            click(fixture, "▼");
            assertEquals("SETUP_MENU_EXT", field(fixture.app(), "uiState").toString());
            click(fixture, "4");
            assertEquals("SETUP_STAT_FREQ", field(fixture.app(), "uiState").toString());
            click(fixture, "1");
            assertEquals(true, field(fixture.app(), "statFreqEnabled"));

            click(fixture, "MODE");
            click(fixture, "3");
            click(fixture, "1");
            typeValue(fixture, "2");
            click(fixture, "=");
            assertTrue(textField(fixture.app()).getText().contains("FREQ"));
            typeValue(fixture, "3");
            click(fixture, "=");

            typeValue(fixture, "4");
            click(fixture, "=");
            assertTrue(textField(fixture.app()).getText().contains("FREQ"));
            click(fixture, "="); // blank frequency defaults to one

            StatEngine engine = (StatEngine) field(fixture.app(), "activeStatEngine");
            assertEquals(2, engine.count());
            assertEquals(4.0, engine.n(), 1e-12);
            assertEquals(2.5, engine.meanX(), 1e-12);

            click(fixture, "SHIFT");
            click(fixture, "MODE");
            click(fixture, "▼");
            click(fixture, "4");
            click(fixture, "2");
            assertEquals(false, field(fixture.app(), "statFreqEnabled"));
            assertEquals(0, engine.count(), "Changing STAT Format must clear the statistical editor");
            return null;
        });
    }

    @Test
    void setupStatFreqColumnSupportsPairedVariableWeightedRowsAndRejectsZero() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "SHIFT");
            click(fixture, "MODE");
            click(fixture, "▼");
            click(fixture, "4");
            click(fixture, "1");
            click(fixture, "MODE");
            click(fixture, "3");
            click(fixture, "2");

            typeValue(fixture, "1");
            click(fixture, "=");
            typeValue(fixture, "2");
            click(fixture, "=");
            assertTrue(textField(fixture.app()).getText().contains("FREQ"));
            typeValue(fixture, "0");
            click(fixture, "=");
            StatEngine engine = (StatEngine) field(fixture.app(), "activeStatEngine");
            assertEquals(0, engine.count(), "Zero FREQ must not commit a data row");

            click(fixture, "DEL");
            typeValue(fixture, "2");
            click(fixture, "=");
            typeValue(fixture, "3");
            click(fixture, "=");
            typeValue(fixture, "6");
            click(fixture, "=");
            typeValue(fixture, "3");
            click(fixture, "=");

            assertEquals(2, engine.count());
            assertEquals(5.0, engine.n(), 1e-12);
            assertEquals(2.2, engine.meanX(), 1e-12);
            assertEquals(4.4, engine.meanY(), 1e-12);
            assertEquals(2.0, engine.regB(), 1e-12);
            return null;
        });
    }

    @Test
    void allEightStatModelsCanBeSelectedAndPopulatedThroughTheKeypad() throws Exception {
        onFxThread(() -> {
            double[][][] samples = {
                    {{1, 0}, {2, 0}, {3, 0}},
                    {{1, 3}, {2, 5}, {3, 7}},
                    {{0, 1}, {1, 6}, {2, 17}},
                    {{1, 1}, {2.718282, 3}, {7.389056, 5}},
                    {{0, 2}, {2, 5.436564}, {4, 14.778112}},
                    {{0, 2}, {1, 6}, {2, 18}},
                    {{1, 3}, {2, 12}, {3, 27}},
                    {{1, 6}, {2, 4}, {4, 3}}
            };

            for (int type = 1; type <= 8; type++) {
                Fixture fixture = newFixture();
                click(fixture, "MODE");
                click(fixture, "3");
                click(fixture, Integer.toString(type));

                for (double[] point : samples[type - 1]) {
                    typeValue(fixture, Double.toString(point[0]));
                    click(fixture, "=");
                    if (type != 1) {
                        typeValue(fixture, Double.toString(point[1]));
                        click(fixture, "=");
                    }
                }

                StatEngine engine = (StatEngine) field(fixture.app(), "activeStatEngine");
                assertEquals(type, engine.getType().id(), "Wrong STAT model selected");
                if (type == 1) {
                    assertEquals(3.0, engine.n(), 1e-12);
                    assertEquals(2.0, engine.meanX(), 1e-12);
                } else {
                    assertEquals(3.0, engine.n(), 1e-12);
                    assertTrue(Double.isFinite(engine.regA()), "STAT model " + type + " A is not finite");
                    assertTrue(Double.isFinite(engine.regB()), "STAT model " + type + " B is not finite");
                }
            }
            return null;
        });
    }

    @Test
    void matrixEditorCommitsOnAcAndEscapeCancelsWithoutMutatingStoredMatrix() throws Exception {
        onFxThread(() -> {
            MatrixEngine.setMatrix("MATA", new MatrixEngine.Matrix(2, 2, new double[]{1, 2, 3, 4}));

            Fixture cancel = newFixture();
            invoke(cancel.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.MATRIX);
            click(cancel, "SHIFT");
            click(cancel, "4");
            click(cancel, "2");
            click(cancel, "9");
            click(cancel, "=");
            assertEquals(1.0, MatrixEngine.getMatrix("MATA").get(0, 0), 1e-12,
                    "Uncommitted matrix edits must not mutate stored data");
            invoke(cancel.app(), "handlePhysicalKey", new Class<?>[]{KeyEvent.class},
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
            assertEquals(1.0, MatrixEngine.getMatrix("MATA").get(0, 0), 1e-12);

            Fixture commit = newFixture();
            invoke(commit.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.MATRIX);
            click(commit, "SHIFT");
            click(commit, "4");
            click(commit, "2");
            click(commit, "8");
            click(commit, "=");
            click(commit, "AC");
            assertEquals(8.0, MatrixEngine.getMatrix("MATA").get(0, 0), 1e-12);
            assertEquals(2.0, MatrixEngine.getMatrix("MATA").get(0, 1), 1e-12);
            return null;
        });
    }

    @Test
    void vectorEditorCommitsOnAcAndEscapeCancelsWithoutMutatingStoredVector() throws Exception {
        onFxThread(() -> {
            VectorEngine.setVector("VCTA", new VectorEngine.Vector3D(1, 2, 3));

            Fixture cancel = newFixture();
            invoke(cancel.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.VECTOR);
            click(cancel, "SHIFT");
            click(cancel, "5");
            click(cancel, "2");
            click(cancel, "9");
            click(cancel, "=");
            assertEquals(1.0, VectorEngine.getVector("VCTA").x(), 1e-12);
            invoke(cancel.app(), "handlePhysicalKey", new Class<?>[]{KeyEvent.class},
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
            assertEquals(1.0, VectorEngine.getVector("VCTA").x(), 1e-12);

            Fixture commit = newFixture();
            invoke(commit.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.VECTOR);
            click(commit, "SHIFT");
            click(commit, "5");
            click(commit, "2");
            click(commit, "7");
            click(commit, "=");
            click(commit, "AC");
            assertEquals(7.0, VectorEngine.getVector("VCTA").x(), 1e-12);
            assertEquals(2.0, VectorEngine.getVector("VCTA").y(), 1e-12);
            assertEquals(3.0, VectorEngine.getVector("VCTA").z(), 1e-12);
            return null;
        });
    }

    @Test
    void replayHistoryNavigationAndInsertOverwriteModeWork() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("2+2");
            click(fixture, "=");
            textField(fixture.app()).setText("3+4");
            click(fixture, "=");

            click(fixture, "▲");
            assertEquals("2+2", textField(fixture.app()).getText());
            click(fixture, "▼");
            assertEquals("3+4", textField(fixture.app()).getText());
            click(fixture, "▼");
            assertEquals("", textField(fixture.app()).getText());

            textField(fixture.app()).setText("123");
            textField(fixture.app()).positionCaret(1);
            click(fixture, "SHIFT");
            click(fixture, "DEL");
            assertEquals(false, field(fixture.app(), "isInsertMode"));
            click(fixture, "9");
            assertEquals("193", textField(fixture.app()).getText());

            click(fixture, "SHIFT");
            click(fixture, "DEL");
            assertEquals(true, field(fixture.app(), "isInsertMode"));
            click(fixture, "8");
            assertEquals("1983", textField(fixture.app()).getText());
            return null;
        });
    }

    @Test
    void alphaLegendKeysInsertEveryVariableAndComplexExponentSymbol() throws Exception {
        String[][] cases = {
                {"(-)", "A"}, {"° ' \"", "B"}, {"hyp", "C"},
                {"sin", "D"}, {"cos", "E"}, {"tan", "F"},
                {")", "X"}, {"S⇔D", "Y"}, {"M+", "M"},
                {"ENG", "i"}, {"×10ˣ", "e"}, {".", "RanInt#("}
        };
        onFxThread(() -> {
            for (String[] testCase : cases) {
                Fixture fixture = newFixture();
                click(fixture, "ALPHA");
                click(fixture, testCase[0]);
                assertEquals(testCase[1], textField(fixture.app()).getText(),
                        "ALPHA legend failed for key " + testCase[0]);
            }
            return null;
        });
    }

    @Test
    void shiftedFunctionLegendsReachTheirExpectedInputHandlers() throws Exception {
        String[][] cases = {
                {"∫ dx", "diff("}, {"x⁻¹", "!"}, {"log_■", "Σ("},
                {"■/□", "⌟"}, {"√■", "³√("}, {"x²", "^3"},
                {"x^■", "root("}, {"log", "10^("}, {"ln", "exp("},
                {"(-)", "∠"}, {"hyp", "abs("}, {"sin", "asin("},
                {"cos", "acos("}, {"tan", "atan("}, {"(", "%"},
                {")", ","}
        };
        onFxThread(() -> {
            for (String[] testCase : cases) {
                Fixture fixture = newFixture();
                click(fixture, "SHIFT");
                click(fixture, testCase[0]);
                assertEquals(testCase[1], textField(fixture.app()).getText(),
                        "SHIFT legend failed for key " + testCase[0]);
            }

            Fixture hyperbolic = newFixture();
            click(hyperbolic, "hyp");
            click(hyperbolic, "sin");
            assertEquals("sinh(", textField(hyperbolic.app()).getText());
            return null;
        });
    }

    @Test
    void shiftedDisplayKeysAndPowerOffOnCycleUpdateTheirState() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("25/2");
            click(fixture, "=");
            click(fixture, "SHIFT");
            click(fixture, "° ' \"");
            assertEquals("12°30'0\"", label(fixture.app(), "resultLabel").getText());

            click(fixture, "SHIFT");
            click(fixture, "S⇔D");
            assertTrue(label(fixture.app(), "resultLabel").getText().contains("⌟"));

            click(fixture, "SHIFT");
            click(fixture, "ENG");
            assertTrue(label(fixture.app(), "resultLabel").getText().contains("×10^"));

            click(fixture, "SHIFT");
            click(fixture, "AC");
            assertEquals(false, field(fixture.app(), "isPowerOn"));
            click(fixture, "ON");
            assertEquals(true, field(fixture.app(), "isPowerOn"));
            assertEquals("NORMAL", field(fixture.app(), "uiState").toString());
            return null;
        });
    }

    private static void typeValue(Fixture fixture, String value) throws Exception {
        for (char ch : value.toCharArray()) {
            if (ch == '-') click(fixture, "(-)");
            else if (ch == '.') click(fixture, ".");
            else click(fixture, Character.toString(ch));
        }
    }

    @Test
    void allFortyConstantAndConversionCodesAreReachableFromTheKeypad() throws Exception {
        onFxThread(() -> {
            for (int code = 1; code <= 40; code++) {
                Fixture constant = newFixture();
                click(constant, "SHIFT");
                click(constant, "7");
                String digits = String.format(java.util.Locale.ROOT, "%02d", code);
                click(constant, Character.toString(digits.charAt(0)));
                click(constant, Character.toString(digits.charAt(1)));
                assertEquals(CasioConstants.get(code).symbol(), textField(constant.app()).getText(),
                        "CONST code " + code + " did not insert its symbol");

                Fixture conversion = newFixture();
                textField(conversion.app()).setText("10");
                click(conversion, "=");
                click(conversion, "SHIFT");
                click(conversion, "8");
                click(conversion, Character.toString(digits.charAt(0)));
                click(conversion, Character.toString(digits.charAt(1)));
                double expected = CasioConversions.convert(code, 10.0);
                assertEquals(expected, (double) field(conversion.app(), "answer"),
                        Math.max(Math.ulp(expected) * 4.0, Math.abs(expected) * 1e-10),
                        "CONV code " + code + " did not update Ans correctly");
                assertFalse(label(conversion.app(), "resultLabel").getText().contains("ERROR"),
                        "CONV code " + code + " displayed an error");
            }
            return null;
        });
    }

    @Test
    void setupFixSciAndNormPromptsApplyTheirSelectedValues() throws Exception {
        onFxThread(() -> {
            Fixture fix = newFixture();
            click(fix, "SHIFT");
            click(fix, "MODE");
            click(fix, "6");
            assertEquals("SETUP_FIX_PROMPT", field(fix.app(), "uiState").toString());
            click(fix, "3");
            assertEquals(3, (int) field(fix.app(), "fixDigits"));

            Fixture sci = newFixture();
            click(sci, "SHIFT");
            click(sci, "MODE");
            click(sci, "7");
            assertEquals("SETUP_SCI_PROMPT", field(sci.app(), "uiState").toString());
            click(sci, "5");
            assertEquals(5, (int) field(sci.app(), "sciDigits"));

            Fixture norm = newFixture();
            click(norm, "SHIFT");
            click(norm, "MODE");
            click(norm, "8");
            assertEquals("SETUP_NORM_PROMPT", field(norm.app(), "uiState").toString());
            click(norm, "1");
            assertEquals(1, (int) field(norm.app(), "normMode"));
            return null;
        });
    }

    @Test
    void remainingShiftNumericLegendsReachTheirCalculationModes() throws Exception {
        String[][] cases = {
                {"×", "P"}, {"÷", "C"}, {"+", "pol("}, {"−", "rec("},
                {"0", "Rnd("}, {".", "Ran#"}, {"×10ˣ", "pi"}
        };
        onFxThread(() -> {
            for (String[] testCase : cases) {
                Fixture fixture = newFixture();
                click(fixture, "SHIFT");
                click(fixture, testCase[0]);
                assertEquals(testCase[1], textField(fixture.app()).getText(),
                        "SHIFT numeric legend failed for " + testCase[0]);
            }

            Fixture drg = newFixture();
            click(drg, "SHIFT");
            click(drg, "Ans");
            assertEquals("DRG_MENU", field(drg.app(), "uiState").toString());
            click(drg, "1");
            assertEquals("°", textField(drg.app()).getText());
            return null;
        });
    }

    @Test
    void replayDirectionalKeysMoveTheExpressionCaret() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("12345");
            textField(fixture.app()).positionCaret(3);
            click(fixture, "◀");
            assertEquals(2, textField(fixture.app()).getCaretPosition());
            click(fixture, "▶");
            assertEquals(3, textField(fixture.app()).getCaretPosition());

            invoke(fixture.app(), "handlePhysicalKey", new Class<?>[]{KeyEvent.class},
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, false, false));
            assertEquals(2, textField(fixture.app()).getCaretPosition());
            invoke(fixture.app(), "handlePhysicalKey", new Class<?>[]{KeyEvent.class},
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, false));
            assertEquals(3, textField(fixture.app()).getCaretPosition());
            return null;
        });
    }

    @Test
    void setupDisplayAndAngleMenuChoicesAllApplyTheirValues() throws Exception {
        onFxThread(() -> {
            Fixture mathIo = newFixture();
            click(mathIo, "SHIFT");
            click(mathIo, "MODE");
            click(mathIo, "1");
            assertEquals(true, field(mathIo.app(), "isExactDisplay"));

            Fixture lineIo = newFixture();
            click(lineIo, "SHIFT");
            click(lineIo, "MODE");
            click(lineIo, "2");
            assertEquals(false, field(lineIo.app(), "isExactDisplay"));

            Fixture degrees = newFixture();
            click(degrees, "SHIFT");
            click(degrees, "MODE");
            click(degrees, "3");
            assertEquals(ExpressionParser.AngleUnit.DEG, field(degrees.app(), "angleUnit"));

            Fixture grads = newFixture();
            click(grads, "SHIFT");
            click(grads, "MODE");
            click(grads, "5");
            assertEquals(ExpressionParser.AngleUnit.GRA, field(grads.app(), "angleUnit"));
            return null;
        });
    }

    @Test
    void alphaCalcAndIntegralAlternateLegendsInsertEqualsAndStatementSeparator() throws Exception {
        onFxThread(() -> {
            Fixture equals = newFixture();
            click(equals, "ALPHA");
            click(equals, "CALC");
            assertEquals("=", textField(equals.app()).getText());

            Fixture separator = newFixture();
            click(separator, "ALPHA");
            click(separator, "∫ dx");
            assertEquals(" : ", textField(separator.app()).getText());
            return null;
        });
    }

    @Test
    void twoVariableLinearEquationCanBeEnteredAndNavigatedInEqnUi() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "MODE");
            click(fixture, "5");
            click(fixture, "1");

            String[] coefficients = {"2", "1", "5", "1", "−", "1", "1"};
            // Final input uses the negative-key action before the digit.
            for (int i = 0; i < coefficients.length; i++) {
                String coefficient = coefficients[i];
                if (coefficient.equals("−")) {
                    click(fixture, "−");
                } else {
                    click(fixture, coefficient);
                    click(fixture, "=");
                }
            }

            assertEquals("EQN_RESULT_VIEW", field(fixture.app(), "uiState").toString());
            assertEquals("X", textField(fixture.app()).getText());
            assertEquals("2", label(fixture.app(), "resultLabel").getText());
            click(fixture, "=");
            assertEquals("Y", textField(fixture.app()).getText());
            assertEquals("1", label(fixture.app(), "resultLabel").getText());
            return null;
        });
    }

    @Test
    void tableRejectsZeroStepWithoutEnteringTableView() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "MODE");
            click(fixture, "7");
            textField(fixture.app()).setText("X^2");
            click(fixture, "=");
            click(fixture, "1");
            click(fixture, "=");
            click(fixture, "2");
            click(fixture, "=");
            click(fixture, "0");
            click(fixture, "=");

            assertEquals("TABLE_STEP_INPUT", field(fixture.app(), "uiState").toString());
            assertTrue(label(fixture.app(), "resultLabel").getText().contains("ERROR"));
            return null;
        });
    }

    @Test
    void setupMixedAndImproperFractionFormatsChangeTheCalculatedResult() throws Exception {
        onFxThread(() -> {
            Fixture mixed = newFixture();
            click(mixed, "SHIFT");
            click(mixed, "MODE");
            click(mixed, "▼");
            click(mixed, "1");
            textField(mixed.app()).setText("5/3");
            click(mixed, "=");
            assertEquals("1⌟2⌟3", label(mixed.app(), "resultLabel").getText());

            Fixture improper = newFixture();
            click(improper, "SHIFT");
            click(improper, "MODE");
            click(improper, "▼");
            click(improper, "2");
            textField(improper.app()).setText("5/3");
            click(improper, "=");
            assertEquals("5/3", label(improper.app(), "resultLabel").getText());
            return null;
        });
    }

    @Test
    void setupComplexFormatSwitchesBetweenRectangularAndPolarResults() throws Exception {
        onFxThread(() -> {
            Fixture polar = newFixture();
            click(polar, "SHIFT");
            click(polar, "MODE");
            click(polar, "▼");
            click(polar, "3");
            click(polar, "2");
            click(polar, "MODE");
            click(polar, "2");
            textField(polar.app()).setText("3+4i");
            click(polar, "=");
            assertTrue(label(polar.app(), "resultLabel").getText().contains("∠"));

            Fixture rectangular = newFixture();
            click(rectangular, "SHIFT");
            click(rectangular, "MODE");
            click(rectangular, "▼");
            click(rectangular, "3");
            click(rectangular, "1");
            click(rectangular, "MODE");
            click(rectangular, "2");
            textField(rectangular.app()).setText("3+4i");
            click(rectangular, "=");
            assertEquals("3+4i", label(rectangular.app(), "resultLabel").getText());
            return null;
        });
    }

    @Test
    void setupDisplaySeparatorAndContrastOptionsAreAppliedAndReversible() throws Exception {
        onFxThread(() -> {
            Fixture comma = newFixture();
            click(comma, "SHIFT");
            click(comma, "MODE");
            click(comma, "2"); // LineIO / decimal result format.
            click(comma, "SHIFT");
            click(comma, "MODE");
            click(comma, "▼");
            click(comma, "5");
            click(comma, "2");
            textField(comma.app()).setText("1/2");
            click(comma, "=");
            assertEquals("0,5", label(comma.app(), "resultLabel").getText());
            assertEquals(',', CasioFormatter.getDecimalSeparator());

            Fixture restoreDot = newFixture();
            click(restoreDot, "SHIFT");
            click(restoreDot, "MODE");
            click(restoreDot, "▼");
            click(restoreDot, "5");
            click(restoreDot, "1");
            assertEquals('.', CasioFormatter.getDecimalSeparator());

            Fixture contrast = newFixture();
            click(contrast, "SHIFT");
            click(contrast, "MODE");
            click(contrast, "▼");
            click(contrast, "6");
            click(contrast, "8");
            assertEquals(8, (int) field(contrast.app(), "contrastLevel"));
            assertEquals(0.3, ((javafx.scene.effect.ColorAdjust) field(contrast.app(), "screenContrast")).getContrast(), 1e-12);
            return null;
        });
    }

    @Test
    void visibleCalculatorStageLaysOutEveryKeyWithPositiveBounds() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            Stage stage = new Stage();
            try {
                Scene scene = new Scene(fixture.root(), 480, 820);
                var stylesheet = GlassCalcApp.class.getResource("/styles/casio.css");
                assertNotNull(stylesheet, "CASIO stylesheet must be packaged in the application resources");
                scene.getStylesheets().add(stylesheet.toExternalForm());
                stage.setScene(scene);
                stage.show();

                fixture.root().applyCss();
                fixture.root().layout();

                assertTrue(fixture.root().getWidth() > 0);
                assertTrue(fixture.root().getHeight() > 0);
                assertTrue(label(fixture.app(), "resultLabel").getLayoutBounds().getHeight() > 0);
                assertTrue(textField(fixture.app()).getLayoutBounds().getHeight() > 0);

                List<Button> buttons = findButtons(fixture.root());
                assertTrue(buttons.size() >= 45);
                for (Button button : buttons) {
                    assertTrue(button.isVisible(), "Key must remain visible: " + button.getText());
                    assertTrue(button.getBoundsInParent().getWidth() > 0,
                            "Key width must be positive: " + button.getText());
                    assertTrue(button.getBoundsInParent().getHeight() > 0,
                            "Key height must be positive: " + button.getText());
                }
            } finally {
                stage.close();
            }
            return null;
        });
    }

    @Test
    void matrixDimensionMenuCreatesAndCommitsSelectedTwoByTwoMatrix() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "SHIFT");
            click(fixture, "4");
            click(fixture, "1"); // Dim
            click(fixture, "1"); // MatA
            click(fixture, "5"); // 2x2
            assertEquals("MAT_DATA_INPUT", field(fixture.app(), "uiState").toString());

            typeValue(fixture, "1"); click(fixture, "=");
            typeValue(fixture, "2"); click(fixture, "=");
            typeValue(fixture, "3"); click(fixture, "=");
            typeValue(fixture, "4"); click(fixture, "=");
            click(fixture, "AC");

            MatrixEngine.Matrix stored = MatrixEngine.getMatrix("MATA");
            assertEquals(2, stored.rows);
            assertEquals(2, stored.cols);
            assertArrayEquals(new double[]{1, 2, 3, 4}, stored.data, 1e-12);
            return null;
        });
    }

    @Test
    void vectorDimensionMenuCreatesAndCommitsTwoDimensionalVector() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            click(fixture, "SHIFT");
            click(fixture, "5");
            click(fixture, "1"); // Dim
            click(fixture, "1"); // VctA
            click(fixture, "2"); // 2D
            assertEquals("VCT_DATA_INPUT", field(fixture.app(), "uiState").toString());

            typeValue(fixture, "6"); click(fixture, "=");
            typeValue(fixture, "8"); click(fixture, "=");
            click(fixture, "AC");

            VectorEngine.Vector3D stored = VectorEngine.getVector("VCTA");
            assertEquals(6.0, stored.x(), 1e-12);
            assertEquals(8.0, stored.y(), 1e-12);
            assertEquals(0.0, stored.z(), 1e-12);
            return null;
        });
    }

    @Test
    void shiftClearCommandResetsVariablesAndMemory() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> variables =
                    (java.util.Map<String, Double>) field(fixture.app(), "variables");
            variables.put("A", 12.0);
            variables.put("a", 12.0);
            setField(fixture.app(), "memoryM", 55.0);

            click(fixture, "SHIFT");
            click(fixture, "9");

            assertTrue(variables.isEmpty());
            assertEquals(0.0, (double) field(fixture.app(), "memoryM"), 1e-12);
            assertEquals("0", label(fixture.app(), "resultLabel").getText());
            return null;
        });
    }

    @Test
    void memorySubtractCommandSubtractsLastResultFromMemory() throws Exception {
        onFxThread(() -> {
            Fixture fixture = newFixture();
            textField(fixture.app()).setText("10");
            click(fixture, "=");
            click(fixture, "M+");

            textField(fixture.app()).setText("3");
            click(fixture, "=");
            click(fixture, "SHIFT");
            click(fixture, "M+");
            assertEquals(7.0, (double) field(fixture.app(), "memoryM"), 1e-12);

            textField(fixture.app()).setText("M");
            click(fixture, "=");
            assertEquals("7", label(fixture.app(), "resultLabel").getText());
            return null;
        });
    }
}
