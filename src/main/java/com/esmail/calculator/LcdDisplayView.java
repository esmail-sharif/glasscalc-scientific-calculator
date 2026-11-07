package com.esmail.calculator;

import com.esmail.calculator.engine.mathio.MathContainer;
import com.esmail.calculator.modes.CalculatorMode;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Authentic CASIO fx-991ES PLUS LCD Display View.
 *
 * <p>Encapsulates the complete Casio annunciator indicators bar:
 * {@code [S], [A], [M], [STO], [RCL], [STAT], [CMPLX], [MAT], [VCT], [D]/[R]/[G], [FIX], [SCI], [Math], [▲]/[▼], [Disp]},
 * the 2-line Dot-Matrix text/MathIO expression display, and the bottom-aligned exact/decimal result label.</p>
 */
public class LcdDisplayView extends VBox {

    // Casio Top Annunciator Status Badges
    private final Label badgeShift = new Label("S");
    private final Label badgeAlpha = new Label("A");
    private final Label badgeM = new Label("M");
    private final Label badgeSto = new Label("STO");
    private final Label badgeRcl = new Label("RCL");
    private final Label badgeMode = new Label("");
    private final Label badgeStat = new Label("STAT");
    private final Label badgeCmplx = new Label("CMPLX");
    private final Label badgeMat = new Label("MAT");
    private final Label badgeVct = new Label("VCT");
    private final Label badgeAngle = new Label("D");
    private final Label badgeFix = new Label("FIX");
    private final Label badgeSci = new Label("SCI");
    private final Label badgeMath = new Label("Math");
    private final Label badgeUp = new Label("▲");
    private final Label badgeDown = new Label("▼");
    private final Label badgeDisp = new Label("Disp");

    // Display Fields
    private final VBox lcdScreen = new VBox(3);
    private final TextField expressionField = new TextField("");
    private final Label resultLabel = new Label("0");

    // Hierarchical 2D MathIO state
    private final MathContainer mathIoRoot = new MathContainer();
    private MathContainer activeContainer = mathIoRoot;
    private boolean mathIoEnabled = false;

    public LcdDisplayView(Runnable onEnterAction) {
        getStyleClass().add("lcd-frame");

        lcdScreen.getStyleClass().add("lcd-screen");
        lcdScreen.setPrefHeight(106);
        lcdScreen.setMinHeight(106);

        // Setup All Annunciator Badges Styling
        setupAnnunciators();

        HBox badges = new HBox(6,
                badgeShift, badgeAlpha, badgeM, badgeSto, badgeRcl, badgeMode,
                badgeStat, badgeCmplx, badgeMat, badgeVct,
                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                badgeAngle, badgeFix, badgeSci, badgeMath, badgeUp, badgeDown, badgeDisp);
        badges.setAlignment(Pos.CENTER_LEFT);

        // Input expression line
        expressionField.setPromptText("");
        expressionField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        expressionField.setAlignment(Pos.CENTER_LEFT);
        expressionField.getStyleClass().add("lcd-expression");
        expressionField.setEditable(true);
        if (onEnterAction != null) {
            expressionField.setOnAction(e -> onEnterAction.run());
        }

        // Result display line
        resultLabel.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        resultLabel.getStyleClass().add("lcd-result");
        resultLabel.setMaxWidth(Double.MAX_VALUE);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);
        VBox.setVgrow(resultLabel, Priority.ALWAYS);

        lcdScreen.getChildren().addAll(badges, expressionField, resultLabel);
        getChildren().add(lcdScreen);
    }

    private void setupAnnunciators() {
        Label[] badges = {
                badgeShift, badgeAlpha, badgeM, badgeSto, badgeRcl, badgeMode,
                badgeStat, badgeCmplx, badgeMat, badgeVct, badgeAngle,
                badgeFix, badgeSci, badgeMath, badgeUp, badgeDown, badgeDisp
        };
        for (Label b : badges) {
            setBadgeState(b, false);
            b.setManaged(true);
        }
        badgeAngle.setText("D");
        setBadgeState(badgeAngle, true);
        setBadgeState(badgeMath, true);
    }

    private void setBadgeState(Label badge, boolean active) {
        badge.getStyleClass().setAll(active ? "lcd-badge-active" : "lcd-badge-inactive");
    }

    /**
     * Updates all Casio annunciators dynamically.
     */
    public void updateIndicators(boolean powerOn, boolean shift, boolean alpha, boolean memoryHasValue,
                                 boolean isStore, boolean isRecall,
                                 CalculatorMode mode, int angleUnit,
                                 boolean isFix, boolean isSci,
                                 boolean canReplayUp, boolean canReplayDown, boolean hasDisp) {
        if (!powerOn) {
            Label[] badges = {
                    badgeShift, badgeAlpha, badgeM, badgeSto, badgeRcl, badgeMode,
                    badgeStat, badgeCmplx, badgeMat, badgeVct, badgeAngle,
                    badgeFix, badgeSci, badgeMath, badgeUp, badgeDown, badgeDisp
            };
            for (Label b : badges) setBadgeState(b, false);
            return;
        }

        setBadgeState(badgeShift, shift);
        setBadgeState(badgeAlpha, alpha);
        setBadgeState(badgeM, memoryHasValue);
        setBadgeState(badgeSto, isStore);
        setBadgeState(badgeRcl, isRecall);

        // Mode specific badges
        if (mode != null && mode != CalculatorMode.COMP) {
            badgeMode.setText(mode.badge());
            setBadgeState(badgeMode, true);
        } else {
            badgeMode.setText("");
            setBadgeState(badgeMode, false);
        }
        setBadgeState(badgeStat, mode == CalculatorMode.STAT);
        setBadgeState(badgeCmplx, mode == CalculatorMode.CMPLX);
        setBadgeState(badgeMat, mode == CalculatorMode.MATRIX);
        setBadgeState(badgeVct, mode == CalculatorMode.VECTOR);

        // Angle unit: 0=DEG ("D"), 1=RAD ("R"), 2=GRA ("G")
        if (angleUnit == 1) {
            badgeAngle.setText("R");
        } else if (angleUnit == 2) {
            badgeAngle.setText("G");
        } else {
            badgeAngle.setText("D");
        }
        setBadgeState(badgeAngle, true);

        setBadgeState(badgeFix, isFix);
        setBadgeState(badgeSci, isSci);
        setBadgeState(badgeMath, mathIoEnabled);

        setBadgeState(badgeUp, canReplayUp);
        setBadgeState(badgeDown, canReplayDown);
        setBadgeState(badgeDisp, hasDisp);
    }

    // --- Expression & Result Accessors ---

    public String getExpression() {
        return expressionField.getText();
    }

    public void setExpression(String text) {
        expressionField.setText(text);
        if (text != null) {
            expressionField.positionCaret(text.length());
        }
    }

    public void insertExpressionText(String str) {
        if (str == null || str.isEmpty()) return;
        int caret = expressionField.getCaretPosition();
        String current = expressionField.getText();
        String updated = current.substring(0, caret) + str + current.substring(caret);
        expressionField.setText(updated);
        expressionField.positionCaret(caret + str.length());
    }

    public void deleteExpressionChar() {
        int caret = expressionField.getCaretPosition();
        if (caret > 0) {
            String current = expressionField.getText();
            String updated = current.substring(0, caret - 1) + current.substring(caret);
            expressionField.setText(updated);
            expressionField.positionCaret(caret - 1);
        }
    }

    public void clearExpression() {
        expressionField.setText("");
        mathIoRoot.clear();
        activeContainer = mathIoRoot;
    }

    public String getResult() {
        return resultLabel.getText();
    }

    public void setResult(String text) {
        resultLabel.setText(text);
    }

    public Label getBadgeShift() { return badgeShift; }
    public Label getBadgeAlpha() { return badgeAlpha; }
    public Label getBadgeM() { return badgeM; }
    public Label getBadgeSto() { return badgeSto; }
    public Label getBadgeRcl() { return badgeRcl; }
    public Label getBadgeMode() { return badgeMode; }
    public Label getBadgeDeg() { return badgeAngle; }
    public Label getBadgeAngle() { return badgeAngle; }
    public Label getBadgeStat() { return badgeStat; }
    public Label getBadgeCmplx() { return badgeCmplx; }
    public Label getBadgeMat() { return badgeMat; }
    public Label getBadgeVct() { return badgeVct; }
    public Label getBadgeFix() { return badgeFix; }
    public Label getBadgeSci() { return badgeSci; }
    public Label getBadgeMath() { return badgeMath; }
    public Label getBadgeUp() { return badgeUp; }
    public Label getBadgeDown() { return badgeDown; }
    public Label getBadgeDisp() { return badgeDisp; }

    public TextField getExpressionField() {
        return expressionField;
    }

    public Label getResultLabel() {
        return resultLabel;
    }

    public VBox getLcdScreen() {
        return lcdScreen;
    }

    // --- MathIO 2D Operations ---

    public boolean isMathIoEnabled() {
        return mathIoEnabled;
    }

    public void setMathIoEnabled(boolean enabled) {
        this.mathIoEnabled = enabled;
        badgeMath.setVisible(enabled);
    }

    public MathContainer getMathIoRoot() {
        return mathIoRoot;
    }

    public MathContainer getActiveContainer() {
        return activeContainer;
    }

    public void setActiveContainer(MathContainer container) {
        this.activeContainer = container != null ? container : mathIoRoot;
    }

    public void insertMathIoChar(char c) {
        activeContainer.insertChar(c);
        syncMathIo();
    }

    public void insertMathIoText(String text) {
        activeContainer.insertText(text);
        syncMathIo();
    }

    public void insertMathIoFraction() {
        activeContainer = activeContainer.insertFraction();
        syncMathIo();
    }

    public void insertMathIoSqrt() {
        activeContainer = activeContainer.insertSqrt();
        syncMathIo();
    }

    public void insertMathIoNthRoot() {
        activeContainer = activeContainer.insertNthRoot();
        syncMathIo();
    }

    public void insertMathIoPower() {
        activeContainer = activeContainer.insertPower();
        syncMathIo();
    }

    public void insertMathIoIntegral() {
        activeContainer = activeContainer.insertIntegral();
        syncMathIo();
    }

    public void insertMathIoSummation() {
        activeContainer = activeContainer.insertSummation();
        syncMathIo();
    }

    public void navigateMathIoLeft() {
        activeContainer = activeContainer.moveLeft();
        syncMathIo();
    }

    public void navigateMathIoRight() {
        activeContainer = activeContainer.moveRight();
        syncMathIo();
    }

    public void navigateMathIoUp() {
        activeContainer = activeContainer.moveUp();
        syncMathIo();
    }

    public void navigateMathIoDown() {
        activeContainer = activeContainer.moveDown();
        syncMathIo();
    }

    public void deleteMathIo() {
        activeContainer = activeContainer.delete();
        syncMathIo();
    }

    private void syncMathIo() {
        String expr = mathIoRoot.toExpressionString();
        setExpression(expr);
    }
}
