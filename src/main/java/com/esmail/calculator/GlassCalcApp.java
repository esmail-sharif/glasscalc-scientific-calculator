package com.esmail.calculator;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.ExpressionParser;
import com.esmail.calculator.engine.MathEngine;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

/**
 * CASIO fx-991ES PLUS (Natural-V.P.A.M.) Desktop Application.
 * Faithful replica of the physical handheld scientific calculator.
 */
public final class GlassCalcApp extends Application {
    // Calculator States
    private boolean isShift = false;
    private boolean isAlpha = false;
    private boolean isHyp = false;
    private boolean isDegrees = true;
    private boolean isPowerOn = true;
    private boolean isExactDisplay = true;
    private boolean isResultState = false;

    private double answer = 0.0;
    private double memoryM = 0.0;
    private double currentResultValue = 0.0;
    private final Map<String, Double> variables = new HashMap<>();

    // Calculation History
    private final List<String> historyExpressions = new ArrayList<>();
    private final List<Double> historyResults = new ArrayList<>();
    private int historyIndex = -1;

    // UI Nodes
    private Label badgeShift, badgeAlpha, badgeM, badgeSto, badgeDeg, badgeMath, badgeUp, badgeDown;
    private TextField expressionField;
    private Label resultLabel;
    private VBox lcdScreen;

    @Override
    public void start(Stage stage) {
        // Showcase expression available via REPLAY history:
        historyExpressions.add("∫(sin(X), 0, π/3)");
        historyResults.add(0.5);
        historyIndex = -1;

        VBox calculatorFrame = buildCalculatorFrame();
        calculatorFrame.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        StackPane rootPane = new StackPane(calculatorFrame);
        rootPane.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        rootPane.setPadding(new Insets(18));
        rootPane.setAlignment(Pos.CENTER);
        rootPane.getStyleClass().add("root");

        Scene scene = new Scene(rootPane, 500, 930);
        scene.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        scene.getStylesheets().add(getClass().getResource("/styles/casio.css").toExternalForm());

        // Keyboard handler
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePhysicalKey);

        stage.setTitle("CASIO fx-991ES PLUS — Natural V.P.A.M.");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        updateLcdIndicators();
    }

    private VBox buildCalculatorFrame() {
        VBox casing = new VBox();
        casing.getStyleClass().add("casio-body");
        casing.setMaxWidth(460);
        casing.setPrefWidth(460);

        VBox faceplate = new VBox(10);
        faceplate.getStyleClass().add("casio-faceplate");

        // Top Header: CASIO logo + Solar Cell
        HBox header = buildHeader();

        // LCD Display
        VBox lcdFrame = buildLcdDisplay();

        // Top Controls: SHIFT, ALPHA, REPLAY D-PAD, MODE, ON
        HBox topControls = buildTopControls();

        // Middle Functions Keypad (4 rows)
        VBox functionKeypad = buildFunctionKeypad();

        // Bottom Numeric & Operation Keypad (4 rows × 5 cols)
        VBox numericKeypad = buildNumericKeypad();

        faceplate.getChildren().addAll(header, lcdFrame, topControls, functionKeypad, numericKeypad);
        casing.getChildren().add(faceplate);
        return casing;
    }

    private HBox buildHeader() {
        VBox brandBox = new VBox(2);
        Label casio = new Label("CASIO");
        casio.getStyleClass().add("casio-brand");

        HBox modelBox = new HBox(6);
        modelBox.setAlignment(Pos.CENTER_LEFT);
        Label model = new Label("fx-991ES PLUS");
        model.getStyleClass().add("casio-model");
        modelBox.getChildren().add(model);

        Label vpam = new Label("NATURAL-V.P.A.M.");
        vpam.getStyleClass().add("casio-vpam");

        brandBox.getChildren().addAll(casio, modelBox, vpam);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox solarBox = new VBox(3);
        solarBox.setAlignment(Pos.CENTER_RIGHT);

        HBox solarPanel = new HBox();
        solarPanel.getStyleClass().add("solar-panel");
        solarPanel.setPrefSize(92, 28);
        solarPanel.setMinSize(92, 28);
        for (int i = 0; i < 4; i++) {
            Region strip = new Region();
            strip.getStyleClass().add("solar-strip");
            HBox.setHgrow(strip, Priority.ALWAYS);
            solarPanel.getChildren().add(strip);
        }

        Label solarText = new Label("TWO WAY POWER");
        solarText.getStyleClass().add("casio-solar-text");
        solarBox.getChildren().addAll(solarPanel, solarText);

        HBox header = new HBox(brandBox, spacer, solarBox);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(2, 6, 2, 6));
        return header;
    }

    private VBox buildLcdDisplay() {
        VBox frame = new VBox();
        frame.getStyleClass().add("lcd-frame");

        lcdScreen = new VBox(3);
        lcdScreen.getStyleClass().add("lcd-screen");
        lcdScreen.setPrefHeight(106);
        lcdScreen.setMinHeight(106);

        // Status Indicators Row
        badgeShift = new Label("S");
        badgeAlpha = new Label("A");
        badgeM = new Label("M");
        badgeSto = new Label("STO");
        badgeDeg = new Label("D");
        badgeMath = new Label("Math");
        badgeUp = new Label("▲");
        badgeDown = new Label("▼");

        HBox badges = new HBox(8, badgeShift, badgeAlpha, badgeM, badgeSto,
                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                badgeDeg, badgeMath, badgeUp, badgeDown);
        badges.setAlignment(Pos.CENTER_LEFT);

        // Input expression line
        expressionField = new TextField("");
        expressionField.setPromptText("");
        expressionField.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        expressionField.setAlignment(Pos.CENTER_LEFT);
        expressionField.getStyleClass().add("lcd-expression");
        expressionField.setEditable(true);
        expressionField.setOnAction(e -> calculate());

        // Result display line
        resultLabel = new Label("0");
        resultLabel.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        resultLabel.getStyleClass().add("lcd-result");
        resultLabel.setMaxWidth(Double.MAX_VALUE);
        resultLabel.setAlignment(Pos.CENTER_RIGHT);

        VBox.setVgrow(resultLabel, Priority.ALWAYS);

        lcdScreen.getChildren().addAll(badges, expressionField, resultLabel);
        frame.getChildren().add(lcdScreen);
        return frame;
    }

    private HBox buildTopControls() {
        Button btnShift = createOvalButton("SHIFT", "text-shift-btn", e -> toggleShift());
        Button btnAlpha = createOvalButton("ALPHA", "text-alpha-btn", e -> toggleAlpha());

        // REPLAY 4-way direction pad
        StackPane replayPad = buildReplayPad();

        VBox modeBox = new VBox(1);
        modeBox.setAlignment(Pos.CENTER);
        Label modeSub = new Label("SET UP");
        modeSub.getStyleClass().add("label-shift");
        Button btnMode = createOvalButton("MODE", "text-mode-btn", e -> {
            if (isShift) {
                isShift = false;
                toggleAngleUnit();
            } else {
                toggleAngleUnit();
            }
        });
        modeBox.getChildren().addAll(modeSub, btnMode);

        Button btnOn = createOvalButton("ON", "text-mode-btn", e -> turnOn());

        HBox box = new HBox(12, btnShift, btnAlpha,
                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                replayPad,
                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                modeBox, btnOn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4, 4, 4, 4));
        return box;
    }

    private StackPane buildReplayPad() {
        StackPane pad = new StackPane();
        pad.getStyleClass().add("replay-pad");
        pad.setPrefSize(92, 70);
        pad.setMinSize(92, 70);
        pad.setMaxSize(92, 70);

        Label centerText = new Label("REPLAY");
        centerText.getStyleClass().add("replay-center-text");

        Button up = new Button("▲");
        up.setFocusTraversable(false);
        up.getStyleClass().add("replay-btn");
        up.setOnAction(e -> historyPrevious());
        StackPane.setAlignment(up, Pos.TOP_CENTER);

        Button down = new Button("▼");
        down.setFocusTraversable(false);
        down.getStyleClass().add("replay-btn");
        down.setOnAction(e -> historyNext());
        StackPane.setAlignment(down, Pos.BOTTOM_CENTER);

        Button left = new Button("◀");
        left.setFocusTraversable(false);
        left.getStyleClass().add("replay-btn");
        left.setOnAction(e -> cursorLeft());
        StackPane.setAlignment(left, Pos.CENTER_LEFT);

        Button right = new Button("▶");
        right.setFocusTraversable(false);
        right.getStyleClass().add("replay-btn");
        right.setOnAction(e -> cursorRight());
        StackPane.setAlignment(right, Pos.CENTER_RIGHT);

        pad.getChildren().addAll(centerText, up, down, left, right);
        return pad;
    }

    private Button createOvalButton(String text, String styleClass, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(text);
        btn.setFocusTraversable(false);
        btn.getStyleClass().addAll("btn-oval", styleClass);
        btn.setPrefSize(48, 28);
        btn.setMinSize(48, 28);
        btn.setOnAction(action);
        return btn;
    }

    private VBox buildFunctionKeypad() {
        VBox pad = new VBox(6);
        pad.setPadding(new Insets(2, 0, 4, 0));

        // Row 1: CALC, ∫dx, x^-1, log_■
        HBox r1 = new HBox(8);
        r1.setAlignment(Pos.CENTER);
        r1.getChildren().addAll(
                createFuncKey("CALC", "SOLVE", "=", null, () -> insert("=")),
                createFuncKey("∫ dx", "d/dx", ":", null, () -> {
                    if (isShift) { isShift = false; insert("diff("); }
                    else insert("∫(");
                }),
                createFuncKey("x⁻¹", "x!", null, null, () -> {
                    if (isShift) { isShift = false; insert("!"); }
                    else insert("^-1");
                }),
                createFuncKey("log_■", "Σ", null, null, () -> {
                    if (isShift) { isShift = false; insert("sum("); }
                    else insert("logbase(");
                })
        );

        // Row 2: ■/□, √■, x², x^■, log, ln
        HBox r2 = new HBox(6);
        r2.setAlignment(Pos.CENTER);
        r2.getChildren().addAll(
                createFuncKey("■/□", "a b/c", null, null, () -> insert("/")),
                createFuncKey("√■", "³√", null, null, () -> {
                    if (isShift) { isShift = false; insert("cbrt("); }
                    else insert("√(");
                }),
                createFuncKey("x²", "x³", null, "DEC", () -> {
                    if (isShift) { isShift = false; insert("^3"); }
                    else insert("^2");
                }),
                createFuncKey("x^■", "^x√", null, "HEX", () -> {
                    if (isShift) { isShift = false; insert("root("); }
                    else insert("^");
                }),
                createFuncKey("log", "10^■", null, "BIN", () -> {
                    if (isShift) { isShift = false; insert("10^("); }
                    else insert("log(");
                }),
                createFuncKey("ln", "e^■", null, "OCT", () -> {
                    if (isShift) { isShift = false; insert("exp("); }
                    else insert("ln(");
                })
        );

        // Row 3: (-), °'", hyp, sin, cos, tan
        HBox r3 = new HBox(6);
        r3.setAlignment(Pos.CENTER);
        r3.getChildren().addAll(
                createFuncKey("(-)", "∠", "A", null, () -> {
                    if (isAlpha) { isAlpha = false; insert("A"); }
                    else insert("-");
                }),
                createFuncKey("° ' \"", "←", "B", null, () -> {
                    if (isAlpha) { isAlpha = false; insert("B"); }
                    else insert("°");
                }),
                createFuncKey("hyp", "Abs", "C", null, () -> {
                    if (isShift) { isShift = false; insert("abs("); }
                    else if (isAlpha) { isAlpha = false; insert("C"); }
                    else toggleHyp();
                }),
                createFuncKey("sin", "sin⁻¹", "D", null, () -> handleTrig("sin")),
                createFuncKey("cos", "cos⁻¹", "E", null, () -> handleTrig("cos")),
                createFuncKey("tan", "tan⁻¹", "F", null, () -> handleTrig("tan"))
        );

        // Row 4: RCL, ENG, (, ), S<=>D, M+
        HBox r4 = new HBox(6);
        r4.setAlignment(Pos.CENTER);
        r4.getChildren().addAll(
                createFuncKey("RCL", "STO", null, null, () -> {
                    if (isShift) {
                        isShift = false;
                        variables.put("A", currentResultValue);
                        updateLcdIndicators();
                    } else insert("ans");
                }),
                createFuncKey("ENG", "←", "i", null, () -> {
                    if (isAlpha) { isAlpha = false; insert("i"); }
                }),
                createFuncKey("(", "%", null, null, () -> {
                    if (isShift) { isShift = false; insert("%"); }
                    else insert("(");
                }),
                createFuncKey(")", ",", "X", null, () -> {
                    if (isAlpha) { isAlpha = false; insert("X"); }
                    else if (isShift) { isShift = false; insert(","); }
                    else insert(")");
                }),
                createFuncKey("S⇔D", "a b/c", "Y", null, () -> {
                    if (isAlpha) { isAlpha = false; insert("Y"); }
                    else toggleFractionDisplay();
                }),
                createFuncKey("M+", "M-", "M", null, () -> {
                    if (isAlpha) { isAlpha = false; insert("M"); }
                    else if (isShift) { isShift = false; memoryM -= currentResultValue; updateLcdIndicators(); }
                    else { memoryM += currentResultValue; updateLcdIndicators(); }
                })
        );

        pad.getChildren().addAll(r1, r2, r3, r4);
        return pad;
    }

    private VBox createFuncKey(String keyText, String shiftText, String alphaText, String baseText, Runnable action) {
        VBox cell = new VBox(1);
        cell.setAlignment(Pos.CENTER);
        HBox.setHgrow(cell, Priority.ALWAYS);

        HBox labelRow = new HBox(3);
        labelRow.setAlignment(Pos.CENTER);
        if (shiftText != null) {
            Label sl = new Label(shiftText);
            sl.getStyleClass().add("label-shift");
            labelRow.getChildren().add(sl);
        }
        if (baseText != null) {
            Label bl = new Label(baseText);
            bl.getStyleClass().add("label-base");
            labelRow.getChildren().add(bl);
        }
        if (alphaText != null) {
            Label al = new Label(alphaText);
            al.getStyleClass().add("label-alpha");
            labelRow.getChildren().add(al);
        }

        Button btn = new Button(keyText);
        btn.setFocusTraversable(false);
        btn.getStyleClass().add("key-func");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(28);
        btn.setPrefHeight(28);
        btn.setOnAction(e -> { action.run(); updateLcdIndicators(); });

        cell.getChildren().addAll(labelRow, btn);
        return cell;
    }

    private VBox buildNumericKeypad() {
        VBox pad = new VBox(6);
        pad.setPadding(new Insets(4, 0, 2, 0));

        // Row 1: 7, 8, 9, DEL, AC
        HBox r1 = new HBox(8);
        r1.setAlignment(Pos.CENTER);
        r1.getChildren().addAll(
                createNumKey("7", "CONST", null, () -> insert("7")),
                createNumKey("8", "CONV", null, () -> insert("8")),
                createNumKey("9", "CLR", null, () -> insert("9")),
                createOrangeKey("DEL", "INS", this::deleteChar),
                createOrangeKey("AC", "OFF", () -> {
                    if (isShift) turnOff();
                    else allClear();
                })
        );

        // Row 2: 4, 5, 6, ×, ÷
        HBox r2 = new HBox(8);
        r2.setAlignment(Pos.CENTER);
        r2.getChildren().addAll(
                createNumKey("4", "MATRIX", null, () -> insert("4")),
                createNumKey("5", "VECTOR", null, () -> insert("5")),
                createNumKey("6", null, null, () -> insert("6")),
                createNumKey("×", "nPr", null, () -> {
                    if (isShift) { isShift = false; insert("P"); }
                    else insert("*");
                }),
                createNumKey("÷", "nCr", null, () -> {
                    if (isShift) { isShift = false; insert("C"); }
                    else insert("/");
                })
        );

        // Row 3: 1, 2, 3, +, −
        HBox r3 = new HBox(8);
        r3.setAlignment(Pos.CENTER);
        r3.getChildren().addAll(
                createNumKey("1", "STAT", null, () -> insert("1")),
                createNumKey("2", "CMPLX", null, () -> insert("2")),
                createNumKey("3", "BASE", null, () -> insert("3")),
                createNumKey("+", "Pol", null, () -> {
                    if (isShift) { isShift = false; insert("pol("); }
                    else insert("+");
                }),
                createNumKey("−", "Rec", null, () -> {
                    if (isShift) { isShift = false; insert("rec("); }
                    else insert("-");
                })
        );

        // Row 4: 0, ., ×10^x, Ans, =
        HBox r4 = new HBox(8);
        r4.setAlignment(Pos.CENTER);
        r4.getChildren().addAll(
                createNumKey("0", "Rnd", null, () -> insert("0")),
                createNumKey(".", "Ran#", "RanInt", () -> {
                    if (isAlpha) { isAlpha = false; insert("floor(random()*10)"); }
                    else insert(".");
                }),
                createNumKey("×10ˣ", "π", "e", () -> {
                    if (isShift) { isShift = false; insert("pi"); }
                    else if (isAlpha) { isAlpha = false; insert("e"); }
                    else insert("*10^");
                }),
                createNumKey("Ans", "DRG▶", null, () -> {
                    if (isShift) { isShift = false; toggleAngleUnit(); }
                    else insert("ans");
                }),
                createNumKey("=", null, null, this::calculate)
        );

        pad.getChildren().addAll(r1, r2, r3, r4);
        return pad;
    }

    private VBox createNumKey(String keyText, String shiftText, String alphaText, Runnable action) {
        VBox cell = new VBox(1);
        cell.setAlignment(Pos.CENTER);
        HBox.setHgrow(cell, Priority.ALWAYS);

        HBox labelRow = new HBox(4);
        labelRow.setAlignment(Pos.CENTER);
        if (shiftText != null) {
            Label sl = new Label(shiftText);
            sl.getStyleClass().add("label-shift");
            labelRow.getChildren().add(sl);
        }
        if (alphaText != null) {
            Label al = new Label(alphaText);
            al.getStyleClass().add("label-alpha");
            labelRow.getChildren().add(al);
        }
        if (shiftText == null && alphaText == null) {
            Label empty = new Label(" ");
            empty.getStyleClass().add("label-shift");
            labelRow.getChildren().add(empty);
        }

        Button btn = new Button(keyText);
        btn.setFocusTraversable(false);
        btn.getStyleClass().add("key-num");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(36);
        btn.setPrefHeight(36);
        btn.setOnAction(e -> { action.run(); updateLcdIndicators(); });

        cell.getChildren().addAll(labelRow, btn);
        return cell;
    }

    private VBox createOrangeKey(String keyText, String shiftText, Runnable action) {
        VBox cell = new VBox(1);
        cell.setAlignment(Pos.CENTER);
        HBox.setHgrow(cell, Priority.ALWAYS);

        HBox labelRow = new HBox(4);
        labelRow.setAlignment(Pos.CENTER);
        if (shiftText != null) {
            Label sl = new Label(shiftText);
            sl.getStyleClass().add("label-shift");
            labelRow.getChildren().add(sl);
        }

        Button btn = new Button(keyText);
        btn.setFocusTraversable(false);
        btn.getStyleClass().add("key-orange");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMinHeight(36);
        btn.setPrefHeight(36);
        btn.setOnAction(e -> { action.run(); updateLcdIndicators(); });

        cell.getChildren().addAll(labelRow, btn);
        return cell;
    }

    // =========================================================================
    // Calculator Operations & Logic (Authentic Casio Mechanics)
    // =========================================================================

    private void handleTrig(String base) {
        String func;
        if (isHyp) {
            isHyp = false;
            func = isShift ? "a" + base + "h(" : base + "h(";
        } else {
            func = isShift ? "a" + base + "(" : base + "(";
        }
        isShift = false;
        insert(func);
    }

    private void toggleShift() {
        isShift = !isShift;
        if (isShift) isAlpha = false;
        updateLcdIndicators();
    }

    private void toggleAlpha() {
        isAlpha = !isAlpha;
        if (isAlpha) isShift = false;
        updateLcdIndicators();
    }

    private void toggleHyp() {
        isHyp = !isHyp;
        updateLcdIndicators();
    }

    private void toggleAngleUnit() {
        isDegrees = !isDegrees;
        updateLcdIndicators();
        if (!expressionField.getText().isBlank()) {
            calculate();
        }
    }

    private void toggleFractionDisplay() {
        isExactDisplay = !isExactDisplay;
        if (resultLabel.getText().isBlank() || resultLabel.getText().contains("ERROR")) {
            return;
        }
        if (isExactDisplay) {
            resultLabel.setText(CasioFormatter.toExactOrFraction(currentResultValue));
        } else {
            resultLabel.setText(CasioFormatter.formatDecimal(currentResultValue));
        }
    }

    private void turnOff() {
        isPowerOn = false;
        isShift = false;
        isAlpha = false;
        isResultState = false;
        expressionField.clear();
        resultLabel.setText("");
        updateLcdIndicators();
    }

    private void turnOn() {
        isPowerOn = true;
        isShift = false;
        isAlpha = false;
        isResultState = false;
        expressionField.clear();
        resultLabel.setText("0");
        currentResultValue = 0;
        updateLcdIndicators();
    }

    private void allClear() {
        isShift = false;
        isAlpha = false;
        isResultState = false;
        expressionField.clear();
        resultLabel.setText("0");
        currentResultValue = 0;
        updateLcdIndicators();
    }

    private void deleteChar() {
        if (!isPowerOn) return;
        if (isResultState) {
            isResultState = false;
            resultLabel.setText("");
            return;
        }
        expressionField.deletePreviousChar();
    }

    private void insert(String snippet) {
        if (!isPowerOn) return;

        // Authentic Casio Behavior: After '=', typing an operator continues with Ans,
        // while typing a number/function starts a clean new expression!
        if (isResultState) {
            isResultState = false;
            if (isChainingOperator(snippet)) {
                expressionField.clear();
                expressionField.insertText(0, "Ans" + snippet);
                resultLabel.setText("");
                return;
            } else {
                expressionField.clear();
                resultLabel.setText("");
            }
        }

        int caret = expressionField.getCaretPosition();
        if (caret < 0 || caret > expressionField.getText().length()) {
            caret = expressionField.getText().length();
        }
        expressionField.insertText(caret, snippet);
    }

    private boolean isChainingOperator(String s) {
        return s.equals("+") || s.equals("-") || s.equals("−") || s.equals("*") || s.equals("×")
                || s.equals("/") || s.equals("÷") || s.equals("^") || s.equals("^2") || s.equals("^3")
                || s.equals("^-1") || s.equals("!") || s.equals("%");
    }

    private void cursorLeft() {
        expressionField.backward();
    }

    private void cursorRight() {
        expressionField.forward();
    }

    private void historyPrevious() {
        if (historyExpressions.isEmpty()) return;
        if (historyIndex > 0) {
            historyIndex--;
            loadHistoryEntry(historyIndex);
        } else if (historyIndex == -1 && !historyExpressions.isEmpty()) {
            historyIndex = historyExpressions.size() - 1;
            loadHistoryEntry(historyIndex);
        }
    }

    private void historyNext() {
        if (historyExpressions.isEmpty()) return;
        if (historyIndex < historyExpressions.size() - 1) {
            historyIndex++;
            loadHistoryEntry(historyIndex);
        } else {
            historyIndex = -1;
            expressionField.clear();
            resultLabel.setText("0");
        }
    }

    private void loadHistoryEntry(int idx) {
        String expr = historyExpressions.get(idx);
        expressionField.setText(expr);
        expressionField.positionCaret(expr.length());
        currentResultValue = historyResults.get(idx);
        resultLabel.setText(isExactDisplay
                ? CasioFormatter.toExactOrFraction(currentResultValue)
                : CasioFormatter.formatDecimal(currentResultValue));
        isResultState = true;
    }

    private void calculate() {
        if (!isPowerOn) return;
        String raw = expressionField.getText();
        if (raw == null || raw.isBlank()) return;

        // Auto-close trailing unclosed parentheses (Casio natural syntax)
        String source = ExpressionParser.balanceParentheses(raw.trim());
        if (!source.equals(raw)) {
            expressionField.setText(source);
            expressionField.positionCaret(source.length());
        }

        try {
            variables.put("ans", answer);
            variables.put("ANS", answer);
            variables.put("M", memoryM);

            double value = new ExpressionParser(source, isDegrees, answer, variables).evaluate();
            currentResultValue = value;
            answer = value;

            resultLabel.setText(isExactDisplay
                    ? CasioFormatter.toExactOrFraction(value)
                    : CasioFormatter.formatDecimal(value));

            historyExpressions.add(source);
            historyResults.add(value);
            historyIndex = historyExpressions.size() - 1;
            isResultState = true;
        } catch (ArithmeticException ex) {
            resultLabel.setText("Math ERROR");
            isResultState = true;
        } catch (Exception ex) {
            resultLabel.setText("Syntax ERROR");
            isResultState = true;
        }
        updateLcdIndicators();
    }

    private void updateLcdIndicators() {
        if (!isPowerOn) {
            badgeShift.getStyleClass().setAll("lcd-badge-inactive");
            badgeAlpha.getStyleClass().setAll("lcd-badge-inactive");
            badgeM.getStyleClass().setAll("lcd-badge-inactive");
            badgeSto.getStyleClass().setAll("lcd-badge-inactive");
            badgeDeg.getStyleClass().setAll("lcd-badge-inactive");
            badgeMath.getStyleClass().setAll("lcd-badge-inactive");
            badgeUp.getStyleClass().setAll("lcd-badge-inactive");
            badgeDown.getStyleClass().setAll("lcd-badge-inactive");
            return;
        }

        badgeShift.getStyleClass().setAll(isShift ? "lcd-badge-active" : "lcd-badge-inactive");
        badgeAlpha.getStyleClass().setAll(isAlpha ? "lcd-badge-active" : "lcd-badge-inactive");
        badgeM.getStyleClass().setAll(Math.abs(memoryM) > 1e-12 ? "lcd-badge-active" : "lcd-badge-inactive");
        badgeSto.getStyleClass().setAll("lcd-badge-inactive");

        badgeDeg.setText(isDegrees ? "D" : "R");
        badgeDeg.getStyleClass().setAll("lcd-badge-active");

        badgeMath.getStyleClass().setAll("lcd-badge-active");
        badgeUp.getStyleClass().setAll(!historyExpressions.isEmpty() ? "lcd-badge-active" : "lcd-badge-inactive");
        badgeDown.getStyleClass().setAll(historyIndex >= 0 && historyIndex < historyExpressions.size() - 1
                ? "lcd-badge-active" : "lcd-badge-inactive");
    }

    private void handlePhysicalKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            calculate();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            allClear();
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            historyPrevious();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            historyNext();
            event.consume();
        } else if (event.getCode() == KeyCode.LEFT) {
            cursorLeft();
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            cursorRight();
            event.consume();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
