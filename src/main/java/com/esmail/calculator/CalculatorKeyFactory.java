package com.esmail.calculator;

import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Creates the calculator's reusable key controls and their printed legends.
 * Key actions remain injected by the application; this factory owns only
 * presentation, sizing, and the shared post-action indicator refresh.
 */
public final class CalculatorKeyFactory {
    private final Runnable refreshIndicators;

    public CalculatorKeyFactory(Runnable refreshIndicators) {
        this.refreshIndicators = refreshIndicators == null ? () -> {} : refreshIndicators;
    }

    public Button createOvalButton(String text, String styleClass,
                                   EventHandler<ActionEvent> action) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.getStyleClass().addAll("btn-oval", styleClass);
        button.setPrefSize(48, 28);
        button.setMinSize(48, 28);
        button.setOnAction(action);
        return button;
    }

    public VBox createFuncKey(String keyText, String shiftText, String alphaText,
                              String baseText, Runnable action) {
        VBox cell = new VBox(1);
        cell.setAlignment(Pos.CENTER);
        HBox.setHgrow(cell, Priority.ALWAYS);

        HBox labelRow = new HBox(3);
        labelRow.setAlignment(Pos.CENTER);
        if (shiftText != null) {
            Label label = new Label(shiftText);
            label.getStyleClass().add("label-shift");
            labelRow.getChildren().add(label);
        }
        if (baseText != null) {
            Label label = new Label(baseText);
            label.getStyleClass().add("label-base");
            labelRow.getChildren().add(label);
        }
        if (alphaText != null) {
            Label label = new Label(alphaText);
            label.getStyleClass().add("label-alpha");
            labelRow.getChildren().add(label);
        }

        Button button = new Button(keyText);
        button.setFocusTraversable(false);
        button.getStyleClass().add("key-func");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(28);
        button.setPrefHeight(28);
        button.setOnAction(event -> {
            action.run();
            refreshIndicators.run();
        });
        labelRow.setOnMouseClicked(event -> button.fire());

        cell.getChildren().addAll(labelRow, button);
        return cell;
    }

    public VBox createNumKey(String keyText, String shiftText, String alphaText,
                             Runnable action) {
        VBox cell = new VBox(1);
        cell.setAlignment(Pos.CENTER);
        HBox.setHgrow(cell, Priority.ALWAYS);

        HBox labelRow = new HBox(4);
        labelRow.setAlignment(Pos.CENTER);
        if (shiftText != null) {
            Label label = new Label(shiftText);
            label.getStyleClass().add("label-shift");
            labelRow.getChildren().add(label);
        }
        if (alphaText != null) {
            Label label = new Label(alphaText);
            label.getStyleClass().add("label-alpha");
            labelRow.getChildren().add(label);
        }
        if (shiftText == null && alphaText == null) {
            Label empty = new Label(" ");
            empty.getStyleClass().add("label-shift");
            labelRow.getChildren().add(empty);
        }

        Button button = new Button(keyText);
        button.setFocusTraversable(false);
        button.getStyleClass().add("key-num");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(36);
        button.setPrefHeight(36);
        button.setOnAction(event -> {
            action.run();
            refreshIndicators.run();
        });
        labelRow.setOnMouseClicked(event -> button.fire());

        cell.getChildren().addAll(labelRow, button);
        return cell;
    }

    public VBox createOrangeKey(String keyText, String shiftText, Runnable action) {
        VBox cell = new VBox(1);
        cell.setAlignment(Pos.CENTER);
        HBox.setHgrow(cell, Priority.ALWAYS);

        HBox labelRow = new HBox(4);
        labelRow.setAlignment(Pos.CENTER);
        if (shiftText != null) {
            Label label = new Label(shiftText);
            label.getStyleClass().add("label-shift");
            labelRow.getChildren().add(label);
        }

        Button button = new Button(keyText);
        button.setFocusTraversable(false);
        button.getStyleClass().add("key-orange");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(36);
        button.setPrefHeight(36);
        button.setOnAction(event -> {
            action.run();
            refreshIndicators.run();
        });
        labelRow.setOnMouseClicked(event -> button.fire());

        cell.getChildren().addAll(labelRow, button);
        return cell;
    }
}
