package com.esmail.calculator;

import com.esmail.calculator.modes.StatEditorSession;
import com.esmail.calculator.modes.StatEngine;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class StatDataGridDialogTest {

    @BeforeAll
    public static void initJavaFx() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            latch.countDown();
        }
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static <T> T onFx(Callable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testOneVarDialogStructure() throws Exception {
        onFx(() -> {
            StatEditorSession session = new StatEditorSession();
            session.selectType(StatEngine.StatType.ONE_VAR);
            session.setFrequencyEnabled(false);
            session.replaceRows(List.of(
                    new StatEditorSession.Observation(10.0, 0.0, 1.0),
                    new StatEditorSession.Observation(20.0, 0.0, 1.0)
            ));

            Dialog<List<StatEditorSession.Observation>> dialog =
                    StatDataGridDialog.createDialog(null, session);

            assertNotNull(dialog);
            assertEquals("STAT Data Editor", dialog.getTitle());

            VBox content = (VBox) dialog.getDialogPane().getContent();
            assertNotNull(content);

            @SuppressWarnings("unchecked")
            TableView<?> table = (TableView<?>) content.getChildren().get(1);
            assertEquals(1, table.getColumns().size(), "1-VAR without frequency should have 1 column (X)");
            assertEquals("X", table.getColumns().get(0).getText());
            assertEquals(2, table.getItems().size());

            return null;
        });
    }

    @Test
    public void testPairedWithFrequencyDialogStructure() throws Exception {
        onFx(() -> {
            StatEditorSession session = new StatEditorSession();
            session.selectType(StatEngine.StatType.LINEAR); // Paired
            session.setFrequencyEnabled(true);              // With frequency
            session.replaceRows(List.of(
                    new StatEditorSession.Observation(1.0, 2.0, 3.0)
            ));

            Dialog<List<StatEditorSession.Observation>> dialog =
                    StatDataGridDialog.createDialog(null, session);

            VBox content = (VBox) dialog.getDialogPane().getContent();
            @SuppressWarnings("unchecked")
            TableView<?> table = (TableView<?>) content.getChildren().get(1);
            assertEquals(3, table.getColumns().size(), "Paired with frequency should have 3 columns (X, Y, FREQ)");
            assertEquals("X", table.getColumns().get(0).getText());
            assertEquals("Y", table.getColumns().get(1).getText());
            assertEquals("FREQ", table.getColumns().get(2).getText());
            assertEquals(1, table.getItems().size());

            return null;
        });
    }

    @Test
    public void testAddAndRemoveRowButtons() throws Exception {
        onFx(() -> {
            StatEditorSession session = new StatEditorSession();
            session.selectType(StatEngine.StatType.ONE_VAR);
            session.setFrequencyEnabled(false);

            Dialog<List<StatEditorSession.Observation>> dialog =
                    StatDataGridDialog.createDialog(null, session);

            VBox content = (VBox) dialog.getDialogPane().getContent();
            HBox controls = (HBox) content.getChildren().get(0);
            Button addBtn = (Button) controls.getChildren().get(0);
            Button removeBtn = (Button) controls.getChildren().get(1);

            @SuppressWarnings("unchecked")
            TableView<?> table = (TableView<?>) content.getChildren().get(1);
            assertEquals(0, table.getItems().size());

            // Add row
            addBtn.fire();
            assertEquals(1, table.getItems().size());
            addBtn.fire();
            assertEquals(2, table.getItems().size());

            // Remove selected row
            table.getSelectionModel().select(0);
            removeBtn.fire();
            assertEquals(1, table.getItems().size());

            return null;
        });
    }

    @Test
    public void testApplyCommitObservations() throws Exception {
        onFx(() -> {
            StatEditorSession session = new StatEditorSession();
            session.selectType(StatEngine.StatType.ONE_VAR);
            session.setFrequencyEnabled(false);
            session.replaceRows(List.of(
                    new StatEditorSession.Observation(15.0, 0.0, 1.0),
                    new StatEditorSession.Observation(25.0, 0.0, 1.0)
            ));

            Dialog<List<StatEditorSession.Observation>> dialog =
                    StatDataGridDialog.createDialog(null, session);

            ButtonType applyType = dialog.getDialogPane().getButtonTypes().stream()
                    .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    .findFirst().orElseThrow();
            Button applyButton = (Button) dialog.getDialogPane().lookupButton(applyType);

            // Fire action on Apply button
            applyButton.fire();

            List<StatEditorSession.Observation> committed =
                    dialog.getResultConverter().call(applyType);
            assertNotNull(committed);
            assertEquals(2, committed.size());
            assertEquals(15.0, committed.get(0).x(), 1e-9);
            assertEquals(25.0, committed.get(1).x(), 1e-9);

            return null;
        });
    }

    @Test
    public void testCancelDiscardsChanges() throws Exception {
        onFx(() -> {
            StatEditorSession session = new StatEditorSession();
            session.selectType(StatEngine.StatType.ONE_VAR);
            session.setFrequencyEnabled(false);
            session.replaceRows(List.of(
                    new StatEditorSession.Observation(50.0, 0.0, 1.0)
            ));

            Dialog<List<StatEditorSession.Observation>> dialog =
                    StatDataGridDialog.createDialog(null, session);

            ButtonType cancelType = dialog.getDialogPane().getButtonTypes().stream()
                    .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)
                    .findFirst().orElseThrow();

            List<StatEditorSession.Observation> result =
                    dialog.getResultConverter().call(cancelType);
            assertNull(result, "Cancel button should result in null to avoid mutating the session");

            return null;
        });
    }

    @Test
    public void testInvalidFrequencyRejection() throws Exception {
        onFx(() -> {
            StatEditorSession session = new StatEditorSession();
            session.selectType(StatEngine.StatType.ONE_VAR);
            session.setFrequencyEnabled(true); // with frequency
            // Add a row, then mutate its frequency to an invalid value via table items
            Dialog<List<StatEditorSession.Observation>> dialog =
                    StatDataGridDialog.createDialog(null, session);

            VBox content = (VBox) dialog.getDialogPane().getContent();
            HBox controls = (HBox) content.getChildren().get(0);
            Button addBtn = (Button) controls.getChildren().get(0);
            addBtn.fire(); // Add a row

            @SuppressWarnings("unchecked")
            TableView<Object> table = (TableView<Object>) content.getChildren().get(1);
            assertEquals(1, table.getItems().size());

            // Set frequency to invalid negative value via reflection or property
            Object row = table.getItems().get(0);
            java.lang.reflect.Method freqPropMethod = row.getClass().getDeclaredMethod("frequencyProperty");
            freqPropMethod.setAccessible(true);
            javafx.beans.property.StringProperty freqProp =
                    (javafx.beans.property.StringProperty) freqPropMethod.invoke(row);
            freqProp.set("-5"); // Invalid negative frequency!

            ButtonType applyType = dialog.getDialogPane().getButtonTypes().stream()
                    .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    .findFirst().orElseThrow();
            Button applyButton = (Button) dialog.getDialogPane().lookupButton(applyType);

            Label status = (Label) content.getChildren().get(2);

            applyButton.fire();

            // The filter catches the invalid data, updates the status text, and does not submit
            assertTrue(status.getText().contains("Frequency must be a positive integer") || status.getText().contains("Row 1"),
                    "Status should indicate the error: " + status.getText());

            List<StatEditorSession.Observation> result = dialog.getResultConverter().call(applyType);
            assertNull(result, "Invalid data must not produce committed observations");

            return null;
        });
    }
}
