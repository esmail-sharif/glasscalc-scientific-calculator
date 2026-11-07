package com.esmail.calculator;

import com.esmail.calculator.modes.StatEditorSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Editable spreadsheet-style STAT data editor.
 *
 * <p>The dialog edits a detached row snapshot. The caller applies the returned
 * rows to StatEditorSession only after the user presses Apply, so Cancel never
 * mutates the active statistical dataset.</p>
 */
public final class StatDataGridDialog {
    private static final ButtonType APPLY =
            new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);

    private StatDataGridDialog() {}

    public static Optional<List<StatEditorSession.Observation>> showAndEdit(
            Window owner, StatEditorSession session) {
        Dialog<List<StatEditorSession.Observation>> dialog = new Dialog<>();
        dialog.setTitle("STAT Data Editor");
        dialog.setHeaderText("Edit statistical observations");
        if (owner != null) dialog.initOwner(owner);

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(APPLY, cancel);

        ObservableList<GridRow> rows = FXCollections.observableArrayList();
        for (StatEditorSession.Observation observation : session.rows()) {
            rows.add(new GridRow(observation));
        }
        if (rows.isEmpty()) rows.add(new GridRow());

        TableView<GridRow> table = new TableView<>(rows);
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(300);

        table.getColumns().add(column("X", GridRow::xProperty));
        if (session.isPaired()) table.getColumns().add(column("Y", GridRow::yProperty));
        if (session.isFrequencyEnabled()) table.getColumns().add(column("FREQ", GridRow::frequencyProperty));

        Button add = new Button("Add row");
        add.setOnAction(event -> {
            GridRow row = new GridRow();
            rows.add(row);
            table.getSelectionModel().select(row);
            table.scrollTo(row);
        });
        Button remove = new Button("Delete row");
        remove.setOnAction(event -> {
            int selected = table.getSelectionModel().getSelectedIndex();
            if (selected >= 0) rows.remove(selected);
        });
        HBox controls = new HBox(8, add, remove);

        Label status = new Label("Ctrl+G opens this editor while entering STAT data.");
        status.setWrapText(true);
        VBox content = new VBox(10, controls, table, status);
        content.setPrefWidth(440);
        VBox.setVgrow(table, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);

        AtomicReference<List<StatEditorSession.Observation>> submitted = new AtomicReference<>();
        Button applyButton = (Button) dialog.getDialogPane().lookupButton(APPLY);
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                submitted.set(parseRows(rows, session));
            } catch (IllegalArgumentException invalid) {
                status.setText(invalid.getMessage());
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == APPLY ? submitted.get() : null);
        return dialog.showAndWait();
    }

    private static TableColumn<GridRow, String> column(
            String title, java.util.function.Function<GridRow, javafx.beans.property.StringProperty> property) {
        TableColumn<GridRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> property.apply(cell.getValue()));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> property.apply(event.getRowValue()).set(event.getNewValue()));
        column.setPrefWidth(120);
        return column;
    }

    private static List<StatEditorSession.Observation> parseRows(
            List<GridRow> rows, StatEditorSession session) {
        List<StatEditorSession.Observation> result = new ArrayList<>(rows.size());
        com.esmail.calculator.modes.StatEngine validator =
                new com.esmail.calculator.modes.StatEngine(session.type());
        for (int i = 0; i < rows.size(); i++) {
            GridRow row = rows.get(i);
            double x = parseCell(row.x(), "X", i, 0.0);
            double y = session.isPaired() ? parseCell(row.y(), "Y", i, 0.0) : 0.0;
            double frequency = session.isFrequencyEnabled()
                    ? parseCell(row.frequency(), "FREQ", i, 1.0) : 1.0;
            try {
                StatEditorSession.Observation observation =
                        new StatEditorSession.Observation(x, y, frequency);
                validator.addPoint(observation.x(), observation.y(), observation.frequency());
                result.add(observation);
            } catch (ArithmeticException invalid) {
                throw new IllegalArgumentException("Row " + (i + 1) + ": " + invalid.getMessage());
            }
        }
        return List.copyOf(result);
    }

    private static double parseCell(String raw, String column, int row, double emptyValue) {
        try {
            double value = Double.parseDouble(raw == null || raw.isBlank()
                    ? Double.toString(emptyValue) : raw.trim());
            if (!Double.isFinite(value)) throw new NumberFormatException("not finite");
            return value;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Row " + (row + 1) + ": invalid " + column + " value");
        }
    }

    private static final class GridRow {
        private final javafx.beans.property.StringProperty x =
                new javafx.beans.property.SimpleStringProperty("0");
        private final javafx.beans.property.StringProperty y =
                new javafx.beans.property.SimpleStringProperty("0");
        private final javafx.beans.property.StringProperty frequency =
                new javafx.beans.property.SimpleStringProperty("1");

        private GridRow() {}

        private GridRow(StatEditorSession.Observation observation) {
            x.set(Double.toString(observation.x()));
            y.set(Double.toString(observation.y()));
            frequency.set(Double.toString(observation.frequency()));
        }

        String x() { return x.get(); }
        String y() { return y.get(); }
        String frequency() { return frequency.get(); }
        javafx.beans.property.StringProperty xProperty() { return x; }
        javafx.beans.property.StringProperty yProperty() { return y; }
        javafx.beans.property.StringProperty frequencyProperty() { return frequency; }
    }
}
