package com.esmail.calculator;

import com.esmail.calculator.modes.BaseNEngine;
import com.esmail.calculator.modes.CalculatorMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Keypad View component for GlassCalc fx-991ES PLUS.
 *
 * <p>Constructs and manages the top oval controls, 4-way direction REPLAY pad,
 * the 5-row scientific function keypad, and the 4-row numeric/operator keypad.</p>
 */
public class KeypadView extends VBox {
    private final GlassCalcApp app;
    private final CalculatorKeyFactory keyFactory;

    public KeypadView(GlassCalcApp app, CalculatorKeyFactory keyFactory) {
        super(10);
        this.app = app;
        this.keyFactory = keyFactory;

        HBox topControls = buildTopControls();
        VBox functionKeypad = buildFunctionKeypad();
        VBox numericKeypad = buildNumericKeypad();

        getChildren().addAll(topControls, functionKeypad, numericKeypad);
    }

    private HBox buildTopControls() {
        Button btnShift = keyFactory.createOvalButton("SHIFT", "text-shift-btn", e -> app.toggleShift());
        Button btnAlpha = keyFactory.createOvalButton("ALPHA", "text-alpha-btn", e -> app.toggleAlpha());

        StackPane replayPad = buildReplayPad();

        VBox modeBox = new VBox(1);
        modeBox.setAlignment(Pos.CENTER);
        Label modeSub = new Label("SET UP");
        modeSub.getStyleClass().add("label-shift");
        Button btnMode = keyFactory.createOvalButton("MODE", "text-mode-btn", e -> app.handleModeButton());
        modeBox.getChildren().addAll(modeSub, btnMode);

        Button btnOn = keyFactory.createOvalButton("ON", "text-mode-btn", e -> app.turnOn());

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
        up.setOnAction(e -> app.handlePadUp());
        StackPane.setAlignment(up, Pos.TOP_CENTER);

        Button down = new Button("▼");
        down.setFocusTraversable(false);
        down.getStyleClass().add("replay-btn");
        down.setOnAction(e -> app.handlePadDown());
        StackPane.setAlignment(down, Pos.BOTTOM_CENTER);

        Button left = new Button("◀");
        left.setFocusTraversable(false);
        left.getStyleClass().add("replay-btn");
        left.setOnAction(e -> app.cursorLeft());
        StackPane.setAlignment(left, Pos.CENTER_LEFT);

        Button right = new Button("▶");
        right.setFocusTraversable(false);
        right.getStyleClass().add("replay-btn");
        right.setOnAction(e -> app.cursorRight());
        StackPane.setAlignment(right, Pos.CENTER_RIGHT);

        pad.getChildren().addAll(centerText, up, down, left, right);
        return pad;
    }

    private VBox buildFunctionKeypad() {
        VBox pad = new VBox(6);
        pad.setPadding(new Insets(2, 0, 4, 0));

        // Row 1: CALC, ∫dx, x^-1, log_■
        HBox r1 = new HBox(8);
        r1.setAlignment(Pos.CENTER);
        r1.getChildren().addAll(
                keyFactory.createFuncKey("CALC", "SOLVE", "=", null, () -> {
                    if (app.isShift) {
                        app.isShift = false;
                        app.handleShiftCalcSolve();
                    } else if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("=");
                    } else {
                        app.handleCalcButton();
                    }
                }),
                keyFactory.createFuncKey("∫ dx", "d/dx", ":", null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("diff("); }
                    else if (app.isAlpha) { app.isAlpha = false; app.insert(" : "); }
                    else {
                        if (app.getLcdDisplayView() != null && app.getLcdDisplayView().isMathIoEnabled()) {
                            app.getLcdDisplayView().insertMathIoIntegral();
                        } else {
                            app.insert("∫(");
                        }
                    }
                }),
                keyFactory.createFuncKey("x⁻¹", "x!", null, null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("!"); }
                    else app.insert("^-1");
                }),
                keyFactory.createFuncKey("log_■", "Σ", null, null, () -> {
                    if (app.isShift) {
                        app.isShift = false;
                        if (app.getLcdDisplayView() != null && app.getLcdDisplayView().isMathIoEnabled()) {
                            app.getLcdDisplayView().insertMathIoSummation();
                        } else {
                            app.insert("Σ(");
                        }
                    } else {
                        app.insert("logbase(");
                    }
                })
        );

        // Row 2: ■/□, √■, x², x^■, log, ln
        HBox r2 = new HBox(6);
        r2.setAlignment(Pos.CENTER);
        r2.getChildren().addAll(
                keyFactory.createFuncKey("■/□", "a b/c", null, null, () -> {
                    if (app.isShift) {
                        app.isShift = false;
                        app.insert("⌟");
                    } else {
                        if (app.getLcdDisplayView() != null && app.getLcdDisplayView().isMathIoEnabled()) {
                            app.getLcdDisplayView().insertMathIoFraction();
                        } else {
                            app.insert("/");
                        }
                    }
                }),
                keyFactory.createFuncKey("√■", "³√", null, null, () -> {
                    if (app.isShift) {
                        app.isShift = false;
                        app.insert("³√(");
                    } else {
                        if (app.getLcdDisplayView() != null && app.getLcdDisplayView().isMathIoEnabled()) {
                            app.getLcdDisplayView().insertMathIoSqrt();
                        } else {
                            app.insert("√(");
                        }
                    }
                }),
                keyFactory.createFuncKey("x²", "x³", null, "DEC", () -> {
                    if (app.currentMode == CalculatorMode.BASE_N) {
                        app.setBase(BaseNEngine.Base.DEC);
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.insert("^3");
                    } else {
                        app.insert("^2");
                    }
                }),
                keyFactory.createFuncKey("x^■", "^x√", null, "HEX", () -> {
                    if (app.currentMode == CalculatorMode.BASE_N) {
                        app.setBase(BaseNEngine.Base.HEX);
                    } else if (app.isShift) {
                        app.isShift = false;
                        if (app.getLcdDisplayView() != null && app.getLcdDisplayView().isMathIoEnabled()) {
                            app.getLcdDisplayView().insertMathIoNthRoot();
                        } else {
                            app.insert("root(");
                        }
                    } else {
                        if (app.getLcdDisplayView() != null && app.getLcdDisplayView().isMathIoEnabled()) {
                            app.getLcdDisplayView().insertMathIoPower();
                        } else {
                            app.insert("^");
                        }
                    }
                }),
                keyFactory.createFuncKey("log", "10^■", null, "BIN", () -> {
                    if (app.currentMode == CalculatorMode.BASE_N) {
                        app.setBase(BaseNEngine.Base.BIN);
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.insert("10^(");
                    } else {
                        app.insert("log(");
                    }
                }),
                keyFactory.createFuncKey("ln", "e^■", null, "OCT", () -> {
                    if (app.currentMode == CalculatorMode.BASE_N) {
                        app.setBase(BaseNEngine.Base.OCT);
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.insert("exp(");
                    } else {
                        app.insert("ln(");
                    }
                })
        );

        // Row 3: (-), °'", hyp, sin, cos, tan
        HBox r3 = new HBox(6);
        r3.setAlignment(Pos.CENTER);
        r3.getChildren().addAll(
                keyFactory.createFuncKey("(-)", "∠", "A", null, () -> {
                    if (app.handleVariableKey("A")) return;
                    if (app.currentMode == CalculatorMode.BASE_N && app.currentBase == BaseNEngine.Base.HEX) {
                        app.insert("A");
                    } else if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("A");
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.insert("∠");
                    } else {
                        app.insert("-");
                    }
                }),
                keyFactory.createFuncKey("° ' \"", "←", "B", null, () -> {
                    if (app.handleVariableKey("B")) return;
                    if (app.currentMode == CalculatorMode.BASE_N && app.currentBase == BaseNEngine.Base.HEX) {
                        app.insert("B");
                    } else if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("B");
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.toggleDmsDisplay();
                    } else {
                        app.insert("°");
                    }
                }),
                keyFactory.createFuncKey("hyp", "Abs", "C", null, () -> {
                    if (app.handleVariableKey("C")) return;
                    if (app.currentMode == CalculatorMode.BASE_N && app.currentBase == BaseNEngine.Base.HEX) {
                        app.insert("C");
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.insert("abs(");
                    } else if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("C");
                    } else {
                        app.toggleHyp();
                    }
                }),
                keyFactory.createFuncKey("sin", "sin⁻¹", "D", null, () -> {
                    if (app.handleVariableKey("D")) return;
                    if (app.currentMode == CalculatorMode.BASE_N && app.currentBase == BaseNEngine.Base.HEX) {
                        app.insert("D");
                    } else if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("D");
                    } else {
                        app.handleTrig("sin");
                    }
                }),
                keyFactory.createFuncKey("cos", "cos⁻¹", "E", null, () -> {
                    if (app.handleVariableKey("E")) return;
                    if (app.currentMode == CalculatorMode.BASE_N && app.currentBase == BaseNEngine.Base.HEX) {
                        app.insert("E");
                    } else if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("E");
                    } else {
                        app.handleTrig("cos");
                    }
                }),
                keyFactory.createFuncKey("tan", "tan⁻¹", "F", null, () -> {
                    if (app.handleVariableKey("F")) return;
                    if (app.currentMode == CalculatorMode.BASE_N && app.currentBase == BaseNEngine.Base.HEX) {
                        app.insert("F");
                    } else if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("F");
                    } else {
                        app.handleTrig("tan");
                    }
                })
        );

        // Row 4: RCL, ENG, (, ), S<=>D, M+
        HBox r4 = new HBox(6);
        r4.setAlignment(Pos.CENTER);
        r4.getChildren().addAll(
                keyFactory.createFuncKey("RCL", "STO", null, null, () -> {
                    if (app.isShift) {
                        app.isShift = false;
                        app.handleShiftRclSto();
                    } else app.insert("Ans");
                }),
                keyFactory.createFuncKey("ENG", "←", "i", null, () -> {
                    if (app.currentMode == CalculatorMode.CMPLX || app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("i");
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.toggleEngDisplayReverse();
                    } else {
                        app.toggleEngDisplay();
                    }
                }),
                keyFactory.createFuncKey("(", "%", null, null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("%"); }
                    else app.insert("(");
                }),
                keyFactory.createFuncKey(")", ",", "X", null, () -> {
                    if (app.handleVariableKey("X")) return;
                    if (app.isAlpha) { app.isAlpha = false; app.insert("X"); }
                    else if (app.isShift) { app.isShift = false; app.insert(","); }
                    else app.insert(")");
                }),
                keyFactory.createFuncKey("S⇔D", "a b/c", "Y", null, () -> {
                    if (app.handleVariableKey("Y")) return;
                    if (app.isAlpha) {
                        app.isAlpha = false;
                        app.insert("Y");
                    } else if (app.isShift) {
                        app.isShift = false;
                        app.toggleMixedFractionDisplay();
                    } else {
                        app.toggleFractionDisplay();
                    }
                }),
                keyFactory.createFuncKey("M+", "M-", "M", null, () -> {
                    if (app.handleVariableKey("M")) return;
                    if (app.isAlpha) { app.isAlpha = false; app.insert("M"); }
                    else if (app.isShift) { app.isShift = false; app.memoryM -= app.currentResultValue; app.updateLcdIndicators(); }
                    else { app.memoryM += app.currentResultValue; app.updateLcdIndicators(); }
                })
        );

        pad.getChildren().addAll(r1, r2, r3, r4);
        return pad;
    }

    private VBox buildNumericKeypad() {
        VBox pad = new VBox(6);
        pad.setPadding(new Insets(4, 0, 2, 0));

        // Row 1: 7, 8, 9, DEL, AC
        HBox r1 = new HBox(8);
        r1.setAlignment(Pos.CENTER);
        r1.getChildren().addAll(
                keyFactory.createNumKey("7", "CONST", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift7Const(); }
                    else app.insertDigit("7");
                }),
                keyFactory.createNumKey("8", "CONV", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift8Conv(); }
                    else app.insertDigit("8");
                }),
                keyFactory.createNumKey("9", "CLR", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift9Reset(); }
                    else app.insertDigit("9");
                }),
                keyFactory.createOrangeKey("DEL", "INS", app::handleDel),
                keyFactory.createOrangeKey("AC", "OFF", app::handleAc)
        );

        // Row 2: 4, 5, 6, ×, ÷
        HBox r2 = new HBox(8);
        r2.setAlignment(Pos.CENTER);
        r2.getChildren().addAll(
                keyFactory.createNumKey("4", "MATRIX", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift4Matrix(); }
                    else app.insertDigit("4");
                }),
                keyFactory.createNumKey("5", "VECTOR", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift5Vector(); }
                    else app.insertDigit("5");
                }),
                keyFactory.createNumKey("6", null, null, () -> app.insertDigit("6")),
                keyFactory.createNumKey("×", "nPr", null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("P"); }
                    else app.insert("*");
                }),
                keyFactory.createNumKey("÷", "nCr", null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("C"); }
                    else app.insert("/");
                })
        );

        // Row 3: 1, 2, 3, +, −
        HBox r3 = new HBox(8);
        r3.setAlignment(Pos.CENTER);
        r3.getChildren().addAll(
                keyFactory.createNumKey("1", "STAT", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift1Stat(); }
                    else app.insertDigit("1");
                }),
                keyFactory.createNumKey("2", "CMPLX", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift2Cmplx(); }
                    else app.insertDigit("2");
                }),
                keyFactory.createNumKey("3", "BASE", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShift3Base(); }
                    else app.insertDigit("3");
                }),
                keyFactory.createNumKey("+", "Pol", null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("pol("); }
                    else app.insert("+");
                }),
                keyFactory.createNumKey("−", "Rec", null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("rec("); }
                    else app.insert("-");
                })
        );

        // Row 4: 0, ., ×10^x, Ans, =
        HBox r4 = new HBox(8);
        r4.setAlignment(Pos.CENTER);
        r4.getChildren().addAll(
                keyFactory.createNumKey("0", "Rnd", null, () -> {
                    if (app.isShift) { app.isShift = false; app.insert("Rnd("); }
                    else app.insertDigit("0");
                }),
                keyFactory.createNumKey(".", "Ran#", "RanInt", () -> {
                    if (app.isShift) { app.isShift = false; app.insert("Ran#"); }
                    else if (app.isAlpha) { app.isAlpha = false; app.insert("RanInt#("); }
                    else app.insert(".");
                }),
                keyFactory.createNumKey("×10ˣ", "π", "e", () -> {
                    if (app.isShift) { app.isShift = false; app.insert("pi"); }
                    else if (app.isAlpha) { app.isAlpha = false; app.insert("e"); }
                    else app.insert("*10^");
                }),
                keyFactory.createNumKey("Ans", "DRG▶", null, () -> {
                    if (app.isShift) { app.isShift = false; app.handleShiftAnsDrg(); }
                    else app.insert("Ans");
                }),
                keyFactory.createNumKey("=", null, null, app::handleEqual)
        );

        pad.getChildren().addAll(r1, r2, r3, r4);
        return pad;
    }
}
