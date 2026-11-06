package com.esmail.calculator;

import com.esmail.calculator.engine.ExpressionParser;
import com.esmail.calculator.modes.CalculatorMode;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
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
            } catch (Throwable failure) {
                toolkitFailure = failure;
            }
        }
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
            Fixture matrix = newFixture();
            invoke(matrix.app(), "setMode", new Class<?>[]{CalculatorMode.class}, CalculatorMode.MATRIX);
            textField(matrix.app()).setText("det(MatA)");
            click(matrix, "=");
            assertEquals(1.0, (double) field(matrix.app(), "answer"), 1e-12);
            assertEquals("1", label(matrix.app(), "resultLabel").getText());

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
}
