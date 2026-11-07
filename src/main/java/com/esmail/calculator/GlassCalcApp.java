package com.esmail.calculator;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.ExpressionParser;
import com.esmail.calculator.engine.NativeMath;
import com.esmail.calculator.modes.*;
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
import java.util.regex.Pattern;

/**
 * CASIO fx-991ES PLUS / fx-570ES PLUS (Natural-V.P.A.M.) Desktop Application.
 * Faithful replica of the physical handheld scientific calculator.
 * All mathematical calculations, advanced modes (COMP, CMPLX, BASE-N, EQN, MATRIX, VECTOR, TABLE),
 * and scientific constants/conversions are executed directly by the x86-64 NASM Assembly engine.
 */
public final class GlassCalcApp extends Application {

    public enum UiState {
        NORMAL,
        MODE_MENU,
        SETUP_MENU,
        SETUP_FIX_PROMPT,
        SETUP_SCI_PROMPT,
        SETUP_NORM_PROMPT,
        CALC_PROMPT,
        CONST_PROMPT,
        CONV_PROMPT,
        EQN_TYPE_SELECT,
        EQN_COEFF_INPUT,
        EQN_RESULT_VIEW,
        TABLE_FX_INPUT,
        TABLE_START_INPUT,
        TABLE_END_INPUT,
        TABLE_STEP_INPUT,
        TABLE_VIEW,
        MAT_MENU,
        MAT_DIM_SELECT_MAT,
        MAT_DIM_SELECT_SIZE,
        MAT_DATA_INPUT,
        VCT_MENU,
        VCT_DIM_SELECT_VCT,
        VCT_DIM_SELECT_SIZE,
        VCT_DATA_INPUT,
        SOLVE_PROMPT,
        STAT_TYPE_SELECT,
        STAT_DATA_INPUT,
        STAT_MENU,
        STAT_SUBMENU_SUM,
        STAT_SUBMENU_VAR,
        STAT_SUBMENU_DISTR,
        STAT_SUBMENU_REG,
        STAT_SUBMENU_MINMAX,
        CMPLX_MENU,
        BASE_MENU,
        DRG_MENU
    }

    // Calculator Modes & States
    private CalculatorMode currentMode = CalculatorMode.COMP;
    private BaseNEngine.Base currentBase = BaseNEngine.Base.DEC;
    private UiState uiState = UiState.NORMAL;

    private boolean isShift = false;
    private boolean isAlpha = false;
    private boolean isHyp = false;
    private boolean isSto = false;
    private boolean isDegrees = true;
    private ExpressionParser.AngleUnit angleUnit = ExpressionParser.AngleUnit.DEG;
    private int fixDigits = -1;
    private int sciDigits = -1;
    private int normMode = 2;
    private boolean isPowerOn = true;
    private boolean isExactDisplay = true;
    private boolean isResultState = false;

    private double answer = 0.0;
    private double memoryM = 0.0;
    private double currentResultValue = 0.0;
    private final Map<String, Double> variables = new HashMap<>();

    // CALC Mode State
    private String calcExpression = "";
    private final List<String> calcVariableNames = new ArrayList<>();
    private int calcVarIndex = 0;
    private String calcAccumulator = "";

    // MATRIX Interactive Editor State
    private String matEditName = "MATA";
    private MatrixEngine.Matrix matEditMatrix = new MatrixEngine.Matrix(3, 3);
    private int matRow = 0;
    private int matCol = 0;
    private String matAccumulator = "";

    // VECTOR Interactive Editor State
    private String vctEditName = "VCTA";
    private double[] vctEditData = new double[3];
    private int vctEditSize = 3;
    private int vctIndex = 0;
    private String vctAccumulator = "";

    // Multi-statement support (: separator)
    private final List<String> multiStatements = new ArrayList<>();
    private int multiStatementIndex = 0;

    // STAT Mode State (Mode 3)
    private StatEngine activeStatEngine = new StatEngine(StatEngine.StatType.ONE_VAR);
    private int statDataStep = 0; // 0: X, 1: Y
    private double statTempX = 0.0;
    private String statAccumulator = "";

    // Calculation History
    private final List<String> historyExpressions = new ArrayList<>();
    private final List<Double> historyResults = new ArrayList<>();
    private int historyIndex = -1;

    // EQN Mode State
    private int eqnType = 0; // 1: 2-unknowns, 2: 3-unknowns, 3: quadratic, 4: cubic
    private double[] eqnCoeffs;
    private String[] eqnCoeffNames;
    private int eqnCoeffIndex = 0;
    private final List<String> eqnResultLines = new ArrayList<>();
    private int eqnResultLineIndex = 0;

    // TABLE Mode State
    private String tableFxExpr = "";
    private double tableStart = 1.0;
    private double tableEnd = 5.0;
    private double tableStep = 1.0;
    private List<TableGenerator.TableRow> tableRows = new ArrayList<>();
    private int tableRowIndex = 0;

    // CONST & CONV & SOLVE Prompt States
    private String promptAccumulator = "";
    private String solveEquation = "";
    // First user input replaces the displayed TABLE/EQN/SOLVE default value.
    private boolean promptInputStarted = false;

    // UI Nodes
    private Label badgeShift, badgeAlpha, badgeM, badgeSto, badgeMode, badgeDeg, badgeMath, badgeUp, badgeDown;
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

        HBox header = buildHeader();
        VBox lcdFrame = buildLcdDisplay();
        HBox topControls = buildTopControls();
        VBox functionKeypad = buildFunctionKeypad();
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
        badgeMode = new Label("");
        badgeDeg = new Label("D");
        badgeMath = new Label("Math");
        badgeUp = new Label("▲");
        badgeDown = new Label("▼");

        HBox badges = new HBox(8, badgeShift, badgeAlpha, badgeM, badgeSto, badgeMode,
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
        expressionField.setOnAction(e -> handleEqual());

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
        Button btnMode = createOvalButton("MODE", "text-mode-btn", e -> handleModeButton());
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
        up.setOnAction(e -> handlePadUp());
        StackPane.setAlignment(up, Pos.TOP_CENTER);

        Button down = new Button("▼");
        down.setFocusTraversable(false);
        down.getStyleClass().add("replay-btn");
        down.setOnAction(e -> handlePadDown());
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
                createFuncKey("CALC", "SOLVE", "=", null, () -> {
                    if (isShift) {
                        isShift = false;
                        handleShiftCalcSolve();
                    } else if (isAlpha) {
                        isAlpha = false;
                        insert("=");
                    } else {
                        handleCalcButton();
                    }
                }),
                createFuncKey("∫ dx", "d/dx", ":", null, () -> {
                    if (isShift) { isShift = false; insert("diff("); }
                    else if (isAlpha) { isAlpha = false; insert(" : "); }
                    else insert("∫(");
                }),
                createFuncKey("x⁻¹", "x!", null, null, () -> {
                    if (isShift) { isShift = false; insert("!"); }
                    else insert("^-1");
                }),
                createFuncKey("log_■", "Σ", null, null, () -> {
                    if (isShift) { isShift = false; insert("Σ("); }
                    else insert("logbase(");
                })
        );

        // Row 2: ■/□, √■, x², x^■, log, ln
        HBox r2 = new HBox(6);
        r2.setAlignment(Pos.CENTER);
        r2.getChildren().addAll(
                createFuncKey("■/□", "a b/c", null, null, () -> {
                    if (isShift) {
                        isShift = false;
                        insert("⌟");
                    } else {
                        insert("/");
                    }
                }),
                createFuncKey("√■", "³√", null, null, () -> {
                    if (isShift) {
                        isShift = false;
                        insert("³√(");
                    } else {
                        insert("√(");
                    }
                }),
                createFuncKey("x²", "x³", null, "DEC", () -> {
                    if (currentMode == CalculatorMode.BASE_N) {
                        setBase(BaseNEngine.Base.DEC);
                    } else if (isShift) {
                        isShift = false;
                        insert("^3");
                    } else {
                        insert("^2");
                    }
                }),
                createFuncKey("x^■", "^x√", null, "HEX", () -> {
                    if (currentMode == CalculatorMode.BASE_N) {
                        setBase(BaseNEngine.Base.HEX);
                    } else if (isShift) {
                        isShift = false;
                        insert("root(");
                    } else {
                        insert("^");
                    }
                }),
                createFuncKey("log", "10^■", null, "BIN", () -> {
                    if (currentMode == CalculatorMode.BASE_N) {
                        setBase(BaseNEngine.Base.BIN);
                    } else if (isShift) {
                        isShift = false;
                        insert("10^(");
                    } else {
                        insert("log(");
                    }
                }),
                createFuncKey("ln", "e^■", null, "OCT", () -> {
                    if (currentMode == CalculatorMode.BASE_N) {
                        setBase(BaseNEngine.Base.OCT);
                    } else if (isShift) {
                        isShift = false;
                        insert("exp(");
                    } else {
                        insert("ln(");
                    }
                })
        );

        // Row 3: (-), °'", hyp, sin, cos, tan
        HBox r3 = new HBox(6);
        r3.setAlignment(Pos.CENTER);
        r3.getChildren().addAll(
                createFuncKey("(-)", "∠", "A", null, () -> {
                    if (handleVariableKey("A")) return;
                    if (currentMode == CalculatorMode.BASE_N && currentBase == BaseNEngine.Base.HEX) {
                        insert("A");
                    } else if (isAlpha) {
                        isAlpha = false;
                        insert("A");
                    } else if (isShift) {
                        isShift = false;
                        insert("∠");
                    } else {
                        insert("-");
                    }
                }),
                createFuncKey("° ' \"", "←", "B", null, () -> {
                    if (handleVariableKey("B")) return;
                    if (currentMode == CalculatorMode.BASE_N && currentBase == BaseNEngine.Base.HEX) {
                        insert("B");
                    } else if (isAlpha) {
                        isAlpha = false;
                        insert("B");
                    } else if (isShift) {
                        isShift = false;
                        toggleDmsDisplay();
                    } else {
                        insert("°");
                    }
                }),
                createFuncKey("hyp", "Abs", "C", null, () -> {
                    if (handleVariableKey("C")) return;
                    if (currentMode == CalculatorMode.BASE_N && currentBase == BaseNEngine.Base.HEX) {
                        insert("C");
                    } else if (isShift) {
                        isShift = false;
                        insert("abs(");
                    } else if (isAlpha) {
                        isAlpha = false;
                        insert("C");
                    } else {
                        toggleHyp();
                    }
                }),
                createFuncKey("sin", "sin⁻¹", "D", null, () -> {
                    if (handleVariableKey("D")) return;
                    if (currentMode == CalculatorMode.BASE_N && currentBase == BaseNEngine.Base.HEX) {
                        insert("D");
                    } else if (isAlpha) {
                        isAlpha = false;
                        insert("D");
                    } else {
                        handleTrig("sin");
                    }
                }),
                createFuncKey("cos", "cos⁻¹", "E", null, () -> {
                    if (handleVariableKey("E")) return;
                    if (currentMode == CalculatorMode.BASE_N && currentBase == BaseNEngine.Base.HEX) {
                        insert("E");
                    } else if (isAlpha) {
                        isAlpha = false;
                        insert("E");
                    } else {
                        handleTrig("cos");
                    }
                }),
                createFuncKey("tan", "tan⁻¹", "F", null, () -> {
                    if (handleVariableKey("F")) return;
                    if (currentMode == CalculatorMode.BASE_N && currentBase == BaseNEngine.Base.HEX) {
                        insert("F");
                    } else if (isAlpha) {
                        isAlpha = false;
                        insert("F");
                    } else {
                        handleTrig("tan");
                    }
                })
        );

        // Row 4: RCL, ENG, (, ), S<=>D, M+
        HBox r4 = new HBox(6);
        r4.setAlignment(Pos.CENTER);
        r4.getChildren().addAll(
                createFuncKey("RCL", "STO", null, null, () -> {
                    if (isShift) {
                        isShift = false;
                        handleShiftRclSto();
                    } else insert("Ans");
                }),
                createFuncKey("ENG", "←", "i", null, () -> {
                    if (currentMode == CalculatorMode.CMPLX || isAlpha) {
                        isAlpha = false;
                        insert("i");
                    } else if (isShift) {
                        isShift = false;
                        toggleEngDisplayReverse();
                    } else {
                        toggleEngDisplay();
                    }
                }),
                createFuncKey("(", "%", null, null, () -> {
                    if (isShift) { isShift = false; insert("%"); }
                    else insert("(");
                }),
                createFuncKey(")", ",", "X", null, () -> {
                    if (handleVariableKey("X")) return;
                    if (isAlpha) { isAlpha = false; insert("X"); }
                    else if (isShift) { isShift = false; insert(","); }
                    else insert(")");
                }),
                createFuncKey("S⇔D", "a b/c", "Y", null, () -> {
                    if (handleVariableKey("Y")) return;
                    if (isAlpha) {
                        isAlpha = false;
                        insert("Y");
                    } else if (isShift) {
                        isShift = false;
                        toggleMixedFractionDisplay();
                    } else {
                        toggleFractionDisplay();
                    }
                }),
                createFuncKey("M+", "M-", "M", null, () -> {
                    if (handleVariableKey("M")) return;
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
        labelRow.setOnMouseClicked(e -> btn.fire());

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
                createNumKey("7", "CONST", null, () -> {
                    if (isShift) { isShift = false; handleShift7Const(); }
                    else insertDigit("7");
                }),
                createNumKey("8", "CONV", null, () -> {
                    if (isShift) { isShift = false; handleShift8Conv(); }
                    else insertDigit("8");
                }),
                createNumKey("9", "CLR", null, () -> {
                    if (isShift) {
                        isShift = false;
                        allClear();
                        variables.clear();
                        memoryM = 0;
                        updateLcdIndicators();
                    } else insertDigit("9");
                }),
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
                createNumKey("4", "MATRIX", null, () -> {
                    if (isShift) { isShift = false; handleShift4Matrix(); }
                    else insertDigit("4");
                }),
                createNumKey("5", "VECTOR", null, () -> {
                    if (isShift) { isShift = false; handleShift5Vector(); }
                    else insertDigit("5");
                }),
                createNumKey("6", null, null, () -> insertDigit("6")),
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
                createNumKey("1", "STAT", null, () -> {
                    if (isShift) { isShift = false; handleShift1Stat(); }
                    else insertDigit("1");
                }),
                createNumKey("2", "CMPLX", null, () -> {
                    if (isShift) { isShift = false; handleShift2Cmplx(); }
                    else insertDigit("2");
                }),
                createNumKey("3", "BASE", null, () -> {
                    if (isShift) { isShift = false; handleShift3Base(); }
                    else insertDigit("3");
                }),
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
                createNumKey("0", "Rnd", null, () -> {
                    if (isShift) { isShift = false; insert("Rnd("); }
                    else insertDigit("0");
                }),
                createNumKey(".", "Ran#", "RanInt", () -> {
                    if (isShift) { isShift = false; insert("Ran#"); }
                    else if (isAlpha) { isAlpha = false; insert("RanInt#("); }
                    else insert(".");
                }),
                createNumKey("×10ˣ", "π", "e", () -> {
                    if (isShift) { isShift = false; insert("pi"); }
                    else if (isAlpha) { isAlpha = false; insert("e"); }
                    else insert("*10^");
                }),
                createNumKey("Ans", "DRG▶", null, () -> {
                    if (isShift) { isShift = false; handleShiftAnsDrg(); }
                    else insert("Ans");
                }),
                createNumKey("=", null, null, this::handleEqual)
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
        labelRow.setOnMouseClicked(e -> btn.fire());

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
        labelRow.setOnMouseClicked(e -> btn.fire());

        cell.getChildren().addAll(labelRow, btn);
        return cell;
    }

    // =========================================================================
    // Mode Switching & Prompt Navigation (Authentic Casio Mechanics)
    // =========================================================================

    /**
     * Matrix/vector operations can return either a structured value or a scalar.
     * When the display result is a scalar (including a fraction), synchronize
     * Ans/currentResultValue; structured matrix/vector results remain in MatAns/VctAns.
     */
    private Double parseScalarResult(String displayValue) {
        try {
            return new ExpressionParser(displayValue, angleUnit, answer, variables).evaluate();
        } catch (RuntimeException notAScalar) {
            return null;
        }
    }

    private void setResultText(String text) {
        if (text == null) text = "";
        resultLabel.setText(text);
        int len = text.length();
        if (len > 24) {
            resultLabel.setStyle("-fx-font-size: 15px;");
        } else if (len > 18) {
            resultLabel.setStyle("-fx-font-size: 19px;");
        } else if (len > 12) {
            resultLabel.setStyle("-fx-font-size: 23px;");
        } else {
            resultLabel.setStyle("-fx-font-size: 28px;");
        }
    }

    private void handleModeButton() {
        if (!isPowerOn) return;
        if (isShift) {
            isShift = false;
            uiState = UiState.SETUP_MENU;
            expressionField.setText("1:MthIO 2:LineIO 3:Deg 4:Rad");
            setResultText("5:Gra 6:Fix 7:Sci 8:Norm");
        } else {
            uiState = UiState.MODE_MENU;
            expressionField.setText("1:COMP 2:CMPLX 3:STAT 4:BASE");
            setResultText("5:EQN  6:MAT   7:TAB  8:VCT");
        }
        updateLcdIndicators();
    }

    private void handleShift7Const() {
        promptAccumulator = "";
        uiState = UiState.CONST_PROMPT;
        expressionField.setText("CONST Number 01~40?");
        setResultText("");
        updateLcdIndicators();
    }

    private void handleShift8Conv() {
        promptAccumulator = "";
        uiState = UiState.CONV_PROMPT;
        expressionField.setText("CONV Number 01~40?");
        setResultText("");
        updateLcdIndicators();
    }

    private void handleShift4Matrix() {
        uiState = UiState.MAT_MENU;
        expressionField.setText("1:Dim  2:Data 3:MatA 4:MatB");
        setResultText("5:MatC 6:MatAns 7:det 8:Trn");
        updateLcdIndicators();
    }

    private void handleShift5Vector() {
        uiState = UiState.VCT_MENU;
        expressionField.setText("1:Dim  2:Data 3:VctA 4:VctB");
        setResultText("5:VctC 6:VctAns 7:Dot 8:Abs");
        updateLcdIndicators();
    }

    private void handleShift1Stat() {
        uiState = UiState.STAT_MENU;
        if (activeStatEngine.getType() == StatEngine.StatType.ONE_VAR) {
            expressionField.setText("1:Type 2:Data 3:Sum 4:Var");
            setResultText("5:Distr 6:MinMax");
        } else {
            expressionField.setText("1:Type 2:Data 3:Sum 4:Var");
            setResultText("5:Reg 6:MinMax");
        }
        updateLcdIndicators();
    }

    private void handleShift2Cmplx() {
        uiState = UiState.CMPLX_MENU;
        expressionField.setText("1:arg 2:Conjg 3:▶r∠θ 4:▶a+bi");
        setResultText("5:ReP 6:ImP");
        updateLcdIndicators();
    }

    private void handleCalcButton() {
        String expr = expressionField.getText();
        if (expr == null || expr.isBlank()) return;

        calcVariableNames.clear();
        for (String var : new String[]{"A", "B", "C", "D", "E", "F", "X", "Y", "M"}) {
            Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + var + "(?![A-Za-z0-9_])");
            if (p.matcher(expr).find()) {
                calcVariableNames.add(var);
            }
        }

        calcExpression = expr;
        if (calcVariableNames.isEmpty()) {
            calculate();
            return;
        }

        calcVarIndex = 0;
        promptNextCalcVariable();
    }

    private void promptNextCalcVariable() {
        if (calcVarIndex >= calcVariableNames.size()) {
            uiState = UiState.NORMAL;
            try {
                double res = new ExpressionParser(calcExpression, angleUnit, answer, variables).evaluate();
                currentResultValue = res;
                answer = res;
                expressionField.setText(calcExpression);
                setResultText(CasioFormatter.formatWithSettings(res, isExactDisplay, fixDigits, sciDigits, normMode));
                isResultState = true;
            } catch (Exception ex) {
                setResultText(ex.getMessage() == null ? "Math ERROR" : ex.getMessage());
            }
            updateLcdIndicators();
            return;
        }

        String curVar = calcVariableNames.get(calcVarIndex);
        uiState = UiState.CALC_PROMPT;
        calcAccumulator = "";
        double curVal = variables.getOrDefault(curVar, variables.getOrDefault(curVar.toLowerCase(Locale.ROOT), 0.0));
        expressionField.setText(curVar + "?");
        setResultText(CasioFormatter.formatDecimal(curVal));
        updateLcdIndicators();
    }

    private void handleCalcPromptInput() {
        if (calcVarIndex < calcVariableNames.size()) {
            String curVar = calcVariableNames.get(calcVarIndex);
            if (!calcAccumulator.isBlank()) {
                try {
                    double val = Double.parseDouble(calcAccumulator);
                    variables.put(curVar, val);
                    variables.put(curVar.toLowerCase(Locale.ROOT), val);
                } catch (Exception ignored) {}
            }
            calcVarIndex++;
            promptNextCalcVariable();
        }
    }

    private void displayMatrixCell() {
        expressionField.setText(matEditName + "[" + (matRow + 1) + "," + (matCol + 1) + "] =");
        setResultText(matAccumulator.isEmpty() ? CasioFormatter.toExactOrFraction(matEditMatrix.get(matRow, matCol)) : matAccumulator);
        updateLcdIndicators();
    }

    private void displayVectorCell() {
        expressionField.setText(vctEditName + "[" + (vctIndex + 1) + "] =");
        setResultText(vctAccumulator.isEmpty() ? CasioFormatter.toExactOrFraction(vctEditData[vctIndex]) : vctAccumulator);
        updateLcdIndicators();
    }

    private void handleShift3Base() {
        uiState = UiState.BASE_MENU;
        expressionField.setText("1:and 2:or 3:xor 4:xnor");
        setResultText("5:not 6:neg");
        updateLcdIndicators();
    }

    private void handleShiftAnsDrg() {
        uiState = UiState.DRG_MENU;
        expressionField.setText("1:° (Deg) 2:r (Rad) 3:g (Gra)");
        setResultText("");
        updateLcdIndicators();
    }

    private void handleShiftRclSto() {
        isSto = !isSto;
        updateLcdIndicators();
    }

    private boolean handleVariableKey(String varName) {
        if (isSto) {
            isSto = false;
            double val = currentResultValue;
            if (varName.equalsIgnoreCase("M")) {
                memoryM = val;
            }
            variables.put(varName, val);
            variables.put(varName.toLowerCase(Locale.ROOT), val);
            expressionField.setText("Ans → " + varName);
            setResultText(CasioFormatter.toExactOrFraction(val));
            updateLcdIndicators();
            return true;
        }
        return false;
    }

    private void handleShiftCalcSolve() {
        String expr = expressionField.getText();
        if (expr.isBlank()) return;
        solveEquation = expr;
        uiState = UiState.SOLVE_PROMPT;
        promptInputStarted = false;
        expressionField.setText("Solve for X");
        setResultText("0");
        updateLcdIndicators();
    }

    private void setBase(BaseNEngine.Base base) {
        currentBase = base;
        setResultText("0 " + currentBase.suffix());
        updateLcdIndicators();
    }

    private void insertDigit(String digit) {
        if (!isPowerOn) return;

        if (uiState == UiState.MODE_MENU) {
            int sel = Integer.parseInt(digit);
            switch (sel) {
                case 1 -> setMode(CalculatorMode.COMP);
                case 2 -> setMode(CalculatorMode.CMPLX);
                case 3 -> setMode(CalculatorMode.STAT);
                case 4 -> setMode(CalculatorMode.BASE_N);
                case 5 -> setMode(CalculatorMode.EQN);
                case 6 -> setMode(CalculatorMode.MATRIX);
                case 7 -> setMode(CalculatorMode.TABLE);
                case 8 -> setMode(CalculatorMode.VECTOR);
            }
            return;
        }

        if (uiState == UiState.SETUP_MENU) {
            int sel = Integer.parseInt(digit);
            switch (sel) {
                case 1 -> { isExactDisplay = true; resetToNormal(); }
                case 2 -> { isExactDisplay = false; resetToNormal(); }
                case 3 -> { angleUnit = ExpressionParser.AngleUnit.DEG; isDegrees = true; resetToNormal(); }
                case 4 -> { angleUnit = ExpressionParser.AngleUnit.RAD; isDegrees = false; resetToNormal(); }
                case 5 -> { angleUnit = ExpressionParser.AngleUnit.GRA; isDegrees = false; resetToNormal(); }
                case 6 -> {
                    uiState = UiState.SETUP_FIX_PROMPT;
                    expressionField.setText("Fix 0~9?");
                    setResultText("");
                }
                case 7 -> {
                    uiState = UiState.SETUP_SCI_PROMPT;
                    expressionField.setText("Sci 0~9?");
                    setResultText("");
                }
                case 8 -> {
                    uiState = UiState.SETUP_NORM_PROMPT;
                    expressionField.setText("Norm 1~2?");
                    setResultText("");
                }
            }
            updateLcdIndicators();
            return;
        }

        if (uiState == UiState.SETUP_FIX_PROMPT) {
            fixDigits = Integer.parseInt(digit);
            sciDigits = -1;
            resetToNormal();
            return;
        }

        if (uiState == UiState.SETUP_SCI_PROMPT) {
            sciDigits = Integer.parseInt(digit);
            fixDigits = -1;
            resetToNormal();
            return;
        }

        if (uiState == UiState.SETUP_NORM_PROMPT) {
            normMode = Integer.parseInt(digit);
            fixDigits = -1;
            sciDigits = -1;
            resetToNormal();
            return;
        }

        if (uiState == UiState.CALC_PROMPT) {
            calcAccumulator += digit;
            setResultText(calcAccumulator);
            return;
        }

        if (uiState == UiState.CONST_PROMPT) {
            promptAccumulator += digit;
            setResultText(promptAccumulator);
            if (promptAccumulator.length() >= 2) {
                int code = Integer.parseInt(promptAccumulator);
                CasioConstants.ConstantEntry ce = CasioConstants.get(code);
                resetToNormal();
                if (ce != null) {
                    insert(ce.symbol());
                } else {
                    setResultText("Math ERROR");
                }
            }
            return;
        }

        if (uiState == UiState.CONV_PROMPT) {
            promptAccumulator += digit;
            setResultText(promptAccumulator);
            if (promptAccumulator.length() >= 2) {
                int code = Integer.parseInt(promptAccumulator);
                CasioConversions.ConversionEntry conv = CasioConversions.get(code);
                resetToNormal();
                if (conv != null) {
                    try {
                        double converted = CasioConversions.convert(code, currentResultValue);
                        currentResultValue = converted;
                        answer = converted;
                        setResultText(CasioFormatter.toExactOrFraction(converted));
                    } catch (Exception ex) {
                        setResultText("Math ERROR");
                    }
                } else {
                    setResultText("Math ERROR");
                }
            }
            return;
        }

        if (uiState == UiState.MAT_MENU) {
            int sel = Integer.parseInt(digit);
            switch (sel) {
                case 1 -> {
                    uiState = UiState.MAT_DIM_SELECT_MAT;
                    expressionField.setText("Dim: 1:MatA 2:MatB 3:MatC");
                    setResultText("");
                }
                case 2 -> {
                    matEditName = "MATA";
                    matEditMatrix = MatrixEngine.getMatrix("MATA");
                    if (matEditMatrix == null) matEditMatrix = new MatrixEngine.Matrix(3, 3);
                    matRow = 0; matCol = 0;
                    matAccumulator = "";
                    uiState = UiState.MAT_DATA_INPUT;
                    displayMatrixCell();
                }
                case 3 -> { resetToNormal(); insert("MatA"); }
                case 4 -> { resetToNormal(); insert("MatB"); }
                case 5 -> { resetToNormal(); insert("MatC"); }
                case 6 -> { resetToNormal(); insert("MatAns"); }
                case 7 -> { resetToNormal(); insert("det("); }
                case 8 -> { resetToNormal(); insert("Trn("); }
            }
            return;
        }

        if (uiState == UiState.MAT_DIM_SELECT_MAT) {
            int sel = Integer.parseInt(digit);
            matEditName = switch (sel) {
                case 2 -> "MATB";
                case 3 -> "MATC";
                default -> "MATA";
            };
            uiState = UiState.MAT_DIM_SELECT_SIZE;
            expressionField.setText("1:3x3 2:3x2 3:3x1");
            setResultText("4:2x3 5:2x2 6:2x1");
            return;
        }

        if (uiState == UiState.MAT_DIM_SELECT_SIZE) {
            int sel = Integer.parseInt(digit);
            int r = 3, c = 3;
            switch (sel) {
                case 1 -> { r = 3; c = 3; }
                case 2 -> { r = 3; c = 2; }
                case 3 -> { r = 3; c = 1; }
                case 4 -> { r = 2; c = 3; }
                case 5 -> { r = 2; c = 2; }
                case 6 -> { r = 2; c = 1; }
            }
            matEditMatrix = new MatrixEngine.Matrix(r, c);
            matRow = 0; matCol = 0;
            matAccumulator = "";
            uiState = UiState.MAT_DATA_INPUT;
            displayMatrixCell();
            return;
        }

        if (uiState == UiState.MAT_DATA_INPUT) {
            matAccumulator += digit;
            setResultText(matAccumulator);
            return;
        }

        if (uiState == UiState.VCT_MENU) {
            int sel = Integer.parseInt(digit);
            switch (sel) {
                case 1 -> {
                    uiState = UiState.VCT_DIM_SELECT_VCT;
                    expressionField.setText("Dim: 1:VctA 2:VctB 3:VctC");
                    setResultText("");
                }
                case 2 -> {
                    vctEditName = "VCTA";
                    VectorEngine.Vector3D v = VectorEngine.getVector("VCTA");
                    vctEditData = (v != null) ? v.v.clone() : new double[3];
                    vctEditSize = 3;
                    vctIndex = 0;
                    vctAccumulator = "";
                    uiState = UiState.VCT_DATA_INPUT;
                    displayVectorCell();
                }
                case 3 -> { resetToNormal(); insert("VctA"); }
                case 4 -> { resetToNormal(); insert("VctB"); }
                case 5 -> { resetToNormal(); insert("VctC"); }
                case 6 -> { resetToNormal(); insert("VctAns"); }
                case 7 -> { resetToNormal(); insert(" dot "); }
                case 8 -> { resetToNormal(); insert("Abs("); }
            }
            return;
        }

        if (uiState == UiState.VCT_DIM_SELECT_VCT) {
            int sel = Integer.parseInt(digit);
            vctEditName = switch (sel) {
                case 2 -> "VCTB";
                case 3 -> "VCTC";
                default -> "VCTA";
            };
            uiState = UiState.VCT_DIM_SELECT_SIZE;
            expressionField.setText("1: 3  2: 2");
            setResultText("");
            return;
        }

        if (uiState == UiState.VCT_DIM_SELECT_SIZE) {
            int sel = Integer.parseInt(digit);
            vctEditSize = (sel == 2) ? 2 : 3;
            vctEditData = new double[3];
            vctIndex = 0;
            vctAccumulator = "";
            uiState = UiState.VCT_DATA_INPUT;
            displayVectorCell();
            return;
        }

        if (uiState == UiState.VCT_DATA_INPUT) {
            vctAccumulator += digit;
            setResultText(vctAccumulator);
            return;
        }

        if (uiState == UiState.EQN_TYPE_SELECT) {
            int sel = Integer.parseInt(digit);
            if (sel >= 1 && sel <= 4) {
                startEqnCoeffInput(sel);
            }
            return;
        }

        if (uiState == UiState.STAT_TYPE_SELECT) {
            int sel = Integer.parseInt(digit);
            StatEngine.StatType st = switch (sel) {
                case 1 -> StatEngine.StatType.ONE_VAR;
                case 2 -> StatEngine.StatType.LINEAR;
                case 3 -> StatEngine.StatType.QUADRATIC;
                case 4 -> StatEngine.StatType.LOGARITHMIC;
                case 5 -> StatEngine.StatType.EXPONENTIAL_E;
                case 6 -> StatEngine.StatType.EXPONENTIAL_AB;
                case 7 -> StatEngine.StatType.POWER;
                case 8 -> StatEngine.StatType.INVERSE;
                default -> StatEngine.StatType.ONE_VAR;
            };
            activeStatEngine = new StatEngine(st);
            uiState = UiState.STAT_DATA_INPUT;
            statDataStep = 0;
            statAccumulator = "";
            expressionField.setText("[1] X = ");
            setResultText("n = 0");
            return;
        }

        if (uiState == UiState.STAT_DATA_INPUT) {
            statAccumulator += digit;
            expressionField.setText("[" + (activeStatEngine.count() + 1) + "] "
                    + (statDataStep == 0 ? "X" : "Y") + " = " + statAccumulator);
            return;
        }

        if (uiState == UiState.STAT_MENU) {
            int sel = Integer.parseInt(digit);
            switch (sel) {
                case 1 -> {
                    uiState = UiState.STAT_TYPE_SELECT;
                    expressionField.setText("1:1-VAR 2:A+BX 3:_+CX² 4:ln X");
                    setResultText("5:e^X 6:A•B^X 7:A•X^B 8:1/X");
                }
                case 2 -> {
                    uiState = UiState.STAT_DATA_INPUT;
                    statDataStep = 0;
                    statAccumulator = "";
                    expressionField.setText("[" + (activeStatEngine.count() + 1) + "] X = ");
                    setResultText("n = " + activeStatEngine.count());
                }
                case 3 -> {
                    uiState = UiState.STAT_SUBMENU_SUM;
                    if (activeStatEngine.getType() == StatEngine.StatType.ONE_VAR) {
                        expressionField.setText("1:Σx²   2:Σx");
                        setResultText("");
                    } else {
                        expressionField.setText("1:Σx² 2:Σx 3:Σy² 4:Σy");
                        setResultText("5:Σxy 6:Σx³ 7:Σx⁴ 8:Σx²y");
                    }
                }
                case 4 -> {
                    uiState = UiState.STAT_SUBMENU_VAR;
                    if (activeStatEngine.getType() == StatEngine.StatType.ONE_VAR) {
                        expressionField.setText("1:n 2:x̄ 3:σx 4:sx");
                        setResultText("");
                    } else {
                        expressionField.setText("1:n 2:x̄ 3:σx 4:sx");
                        setResultText("5:ȳ 6:σy 7:sy");
                    }
                }
                case 5 -> {
                    if (activeStatEngine.getType() == StatEngine.StatType.ONE_VAR) {
                        uiState = UiState.STAT_SUBMENU_DISTR;
                        expressionField.setText("1:P( 2:Q( 3:R( 4:▶t");
                        setResultText("");
                    } else {
                        uiState = UiState.STAT_SUBMENU_REG;
                        if (activeStatEngine.getType() == StatEngine.StatType.QUADRATIC) {
                            expressionField.setText("1:A 2:B 3:C 4:x̂1");
                            setResultText("5:x̂2 6:ŷ");
                        } else {
                            expressionField.setText("1:A 2:B 3:r 4:x̂ 5:ŷ");
                            setResultText("");
                        }
                    }
                }
                case 6 -> {
                    uiState = UiState.STAT_SUBMENU_MINMAX;
                    if (activeStatEngine.getType() == StatEngine.StatType.ONE_VAR) {
                        expressionField.setText("1:minX   2:maxX");
                        setResultText("");
                    } else {
                        expressionField.setText("1:minX 2:maxX 3:minY 4:maxY");
                        setResultText("");
                    }
                }
            }
            return;
        }

        if (uiState == UiState.STAT_SUBMENU_SUM) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumX2()));
                case 2 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumX()));
                case 3 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumY2()));
                case 4 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumY()));
                case 5 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumXY()));
                case 6 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumX3()));
                case 7 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumX4()));
                case 8 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sumX2Y()));
            }
            return;
        }

        if (uiState == UiState.STAT_SUBMENU_VAR) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert(String.valueOf(activeStatEngine.n()));
                case 2 -> insert(CasioFormatter.formatDecimal(activeStatEngine.meanX()));
                case 3 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sigmaX()));
                case 4 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sX()));
                case 5 -> insert(CasioFormatter.formatDecimal(activeStatEngine.meanY()));
                case 6 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sigmaY()));
                case 7 -> insert(CasioFormatter.formatDecimal(activeStatEngine.sY()));
            }
            return;
        }

        if (uiState == UiState.STAT_SUBMENU_DISTR) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert("P(");
                case 2 -> insert("Q(");
                case 3 -> insert("R(");
                case 4 -> insert(CasioFormatter.formatDecimal(activeStatEngine.tStandard(currentResultValue)));
            }
            return;
        }

        if (uiState == UiState.STAT_SUBMENU_REG) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert(CasioFormatter.formatDecimal(activeStatEngine.regA()));
                case 2 -> insert(CasioFormatter.formatDecimal(activeStatEngine.regB()));
                case 3 -> {
                    if (activeStatEngine.getType() == StatEngine.StatType.QUADRATIC) {
                        insert(CasioFormatter.formatDecimal(activeStatEngine.regC()));
                    } else {
                        insert(CasioFormatter.formatDecimal(activeStatEngine.regR()));
                    }
                }
                case 4 -> insert(CasioFormatter.formatDecimal(activeStatEngine.estimateX(currentResultValue)));
                case 5 -> {
                    if (activeStatEngine.getType() == StatEngine.StatType.QUADRATIC) {
                        insert(CasioFormatter.formatDecimal(activeStatEngine.estimateX(currentResultValue)));
                    } else {
                        insert(CasioFormatter.formatDecimal(activeStatEngine.estimateY(currentResultValue)));
                    }
                }
                case 6 -> insert(CasioFormatter.formatDecimal(activeStatEngine.estimateY(currentResultValue)));
            }
            return;
        }

        if (uiState == UiState.STAT_SUBMENU_MINMAX) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert(CasioFormatter.formatDecimal(activeStatEngine.minX()));
                case 2 -> insert(CasioFormatter.formatDecimal(activeStatEngine.maxX()));
                case 3 -> insert(CasioFormatter.formatDecimal(activeStatEngine.minY()));
                case 4 -> insert(CasioFormatter.formatDecimal(activeStatEngine.maxY()));
            }
            return;
        }

        if (uiState == UiState.CMPLX_MENU) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert("arg(");
                case 2 -> insert("Conjg(");
                case 3 -> {
                    if (!resultLabel.getText().isBlank()) {
                        try {
                            ComplexNumber c = new ComplexExpressionParser(resultLabel.getText(), isDegrees).evaluate();
                            setResultText(c.formatPolar(isDegrees));
                        } catch (Exception ignored) {}
                    }
                }
                case 4 -> {
                    if (!resultLabel.getText().isBlank()) {
                        try {
                            ComplexNumber c = new ComplexExpressionParser(resultLabel.getText(), isDegrees).evaluate();
                            setResultText(c.formatRectangular());
                        } catch (Exception ignored) {}
                    }
                }
                case 5 -> insert("Re(");
                case 6 -> insert("Im(");
            }
            return;
        }

        if (uiState == UiState.BASE_MENU) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert(" and ");
                case 2 -> insert(" or ");
                case 3 -> insert(" xor ");
                case 4 -> insert(" xnor ");
                case 5 -> insert(" not ");
                case 6 -> insert(" neg ");
            }
            return;
        }

        if (uiState == UiState.DRG_MENU) {
            int sel = Integer.parseInt(digit);
            resetToNormal();
            switch (sel) {
                case 1 -> insert("°");
                case 2 -> insert("r");
                case 3 -> insert("g");
            }
            return;
        }

        insert(digit);
    }

    private void setMode(CalculatorMode mode) {
        currentMode = mode;
        switch (mode) {
            case COMP, CMPLX -> resetToNormal();
            case BASE_N -> {
                resetToNormal();
                setResultText("0 " + currentBase.suffix());
            }
            case EQN -> {
                uiState = UiState.EQN_TYPE_SELECT;
                expressionField.setText("1:anX+bnY=cn 2:anX+..");
                setResultText("3:aX²+bX+c=0 4:aX³+..");
            }
            case MATRIX -> {
                resetToNormal();
                setResultText("Mat");
            }
            case TABLE -> {
                uiState = UiState.TABLE_FX_INPUT;
                expressionField.setText("f(X)=");
                expressionField.positionCaret(5);
                setResultText("");
            }
            case VECTOR -> {
                resetToNormal();
                setResultText("Vct");
            }
            case STAT -> {
                uiState = UiState.STAT_TYPE_SELECT;
                expressionField.setText("1: 1-VAR   2: A+BX");
                setResultText("");
            }
        }
        updateLcdIndicators();
    }

    private void resetToNormal() {
        uiState = UiState.NORMAL;
        expressionField.clear();
        setResultText("0");
        updateLcdIndicators();
    }

    private void startEqnCoeffInput(int type) {
        eqnType = type;
        eqnCoeffIndex = 0;
        uiState = UiState.EQN_COEFF_INPUT;

        switch (type) {
            case 1 -> {
                eqnCoeffs = new double[6];
                eqnCoeffNames = new String[] { "a1", "b1", "c1", "a2", "b2", "c2" };
            }
            case 2 -> {
                eqnCoeffs = new double[12];
                eqnCoeffNames = new String[] { "a1", "b1", "c1", "d1", "a2", "b2", "c2", "d2", "a3", "b3", "c3", "d3" };
            }
            case 3 -> {
                eqnCoeffs = new double[3];
                eqnCoeffNames = new String[] { "a", "b", "c" };
            }
            case 4 -> {
                eqnCoeffs = new double[4];
                eqnCoeffNames = new String[] { "a", "b", "c", "d" };
            }
        }

        promptInputStarted = false;
        expressionField.setText(eqnCoeffNames[0] + "?");
        resultLabel.setText("0");
        updateLcdIndicators();
    }

    private void handleEqnStep() {
        String input = resultLabel.getText().isBlank() ? "0" : resultLabel.getText();
        try {
            double val = new ExpressionParser(input, isDegrees, answer, variables).evaluate();
            eqnCoeffs[eqnCoeffIndex] = val;
            eqnCoeffIndex++;

            if (eqnCoeffIndex < eqnCoeffNames.length) {
                promptInputStarted = false;
                expressionField.setText(eqnCoeffNames[eqnCoeffIndex] + "?");
                setResultText("0");
            } else {
                // Compute equation solutions using pure NASM Assembly
                solveActiveEquation();
            }
        } catch (Exception ex) {
            setResultText("Syntax ERROR");
        }
    }

    private void solveActiveEquation() {
        eqnResultLines.clear();
        eqnResultLineIndex = 0;

        try {
            EquationSolver.EquationResult res;
            if (eqnType == 1) {
                res = EquationSolver.solveLinear2(eqnCoeffs[0], eqnCoeffs[1], eqnCoeffs[2],
                                                  eqnCoeffs[3], eqnCoeffs[4], eqnCoeffs[5]);
            } else if (eqnType == 2) {
                res = EquationSolver.solveLinear3(
                        new double[]{ eqnCoeffs[0], eqnCoeffs[1], eqnCoeffs[2], eqnCoeffs[3] },
                        new double[]{ eqnCoeffs[4], eqnCoeffs[5], eqnCoeffs[6], eqnCoeffs[7] },
                        new double[]{ eqnCoeffs[8], eqnCoeffs[9], eqnCoeffs[10], eqnCoeffs[11] }
                );
            } else if (eqnType == 3) {
                res = EquationSolver.solveQuadratic(eqnCoeffs[0], eqnCoeffs[1], eqnCoeffs[2]);
            } else {
                res = EquationSolver.solveCubic(eqnCoeffs[0], eqnCoeffs[1], eqnCoeffs[2], eqnCoeffs[3]);
            }

            for (EquationSolver.Root root : res.roots()) {
                eqnResultLines.add(root.format());
            }
            eqnResultLines.addAll(res.vertexInfo());

            uiState = UiState.EQN_RESULT_VIEW;
            displayEqnResult();
        } catch (ArithmeticException ex) {
            setResultText(ex.getMessage().contains("ERROR") ? ex.getMessage() : "Math ERROR");
        } catch (Exception ex) {
            setResultText("Math ERROR");
        }
    }

    private void displayEqnResult() {
        if (eqnResultLines.isEmpty()) return;
        String line = eqnResultLines.get(eqnResultLineIndex);
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            expressionField.setText(parts[0].trim());
            setResultText(parts[1].trim());
        } else {
            expressionField.setText(line);
            setResultText("");
        }
        updateLcdIndicators();
    }

    private void handlePadUp() {
        if (uiState == UiState.TABLE_VIEW) {
            if (tableRowIndex > 0) {
                tableRowIndex--;
                displayTableRow();
            }
        } else if (uiState == UiState.EQN_RESULT_VIEW) {
            if (eqnResultLineIndex > 0) {
                eqnResultLineIndex--;
                displayEqnResult();
            }
        } else {
            historyPrevious();
        }
    }

    private void handlePadDown() {
        if (uiState == UiState.TABLE_VIEW) {
            if (tableRowIndex < tableRows.size() - 1) {
                tableRowIndex++;
                displayTableRow();
            }
        } else if (uiState == UiState.EQN_RESULT_VIEW) {
            if (eqnResultLineIndex < eqnResultLines.size() - 1) {
                eqnResultLineIndex++;
                displayEqnResult();
            }
        } else {
            historyNext();
        }
    }

    private void displayTableRow() {
        if (tableRows.isEmpty() || tableRowIndex < 0 || tableRowIndex >= tableRows.size()) return;
        TableGenerator.TableRow r = tableRows.get(tableRowIndex);
        expressionField.setText("[" + (tableRowIndex + 1) + "] X = " + CasioFormatter.toExactOrFraction(r.x()));
        setResultText("F(X)=" + (Double.isNaN(r.fx()) ? "ERROR" : CasioFormatter.toExactOrFraction(r.fx())));
        updateLcdIndicators();
    }

    private void handleEqual() {
        if (!isPowerOn) return;

        if (uiState == UiState.EQN_COEFF_INPUT) {
            handleEqnStep();
            return;
        }

        if (uiState == UiState.EQN_RESULT_VIEW) {
            handlePadDown();
            return;
        }

        if (uiState == UiState.TABLE_FX_INPUT) {
            tableFxExpr = expressionField.getText().replace("f(X)=", "").trim();
            uiState = UiState.TABLE_START_INPUT;
            expressionField.setText("Start?");
            promptInputStarted = false;
            setResultText("1");
            return;
        }

        if (uiState == UiState.TABLE_START_INPUT) {
            try {
                tableStart = Double.parseDouble(resultLabel.getText());
            } catch (Exception ex) {
                tableStart = 1.0;
            }
            uiState = UiState.TABLE_END_INPUT;
            expressionField.setText("End?");
            promptInputStarted = false;
            setResultText("5");
            return;
        }

        if (uiState == UiState.TABLE_END_INPUT) {
            try {
                tableEnd = Double.parseDouble(resultLabel.getText());
            } catch (Exception ex) {
                tableEnd = 5.0;
            }
            uiState = UiState.TABLE_STEP_INPUT;
            expressionField.setText("Step?");
            promptInputStarted = false;
            setResultText("1");
            return;
        }

        if (uiState == UiState.TABLE_STEP_INPUT) {
            try {
                tableStep = Double.parseDouble(resultLabel.getText());
            } catch (Exception ex) {
                tableStep = 1.0;
            }
            try {
                tableRows = TableGenerator.generateTable(tableFxExpr, tableStart, tableEnd, tableStep, isDegrees);
                uiState = UiState.TABLE_VIEW;
                tableRowIndex = 0;
                displayTableRow();
            } catch (Exception ex) {
                setResultText("Math ERROR");
            }
            return;
        }

        if (uiState == UiState.SOLVE_PROMPT) {
            double guess = 0.0;
            try {
                guess = Double.parseDouble(resultLabel.getText());
            } catch (Exception ignored) {}
            try {
                NumericalSolve.SolveResult sr = NumericalSolve.solve(solveEquation, guess, isDegrees, variables);
                expressionField.setText("X = " + CasioFormatter.formatDecimal(sr.root()));
                setResultText("L-R = " + CasioFormatter.formatDecimal(sr.residual()));
                uiState = UiState.NORMAL;
                isResultState = true;
            } catch (Exception ex) {
                setResultText("Can't Solve");
            }
            return;
        }

        if (uiState == UiState.CALC_PROMPT) {
            handleCalcPromptInput();
            return;
        }

        if (uiState == UiState.MAT_DATA_INPUT) {
            double val = matEditMatrix.get(matRow, matCol);
            if (!matAccumulator.isBlank()) {
                try { val = Double.parseDouble(matAccumulator); } catch (Exception ignored) {}
            }
            matEditMatrix.set(matRow, matCol, val);
            matAccumulator = "";
            matCol++;
            if (matCol >= matEditMatrix.cols) {
                matCol = 0;
                matRow++;
                if (matRow >= matEditMatrix.rows) {
                    matRow = 0;
                }
            }
            displayMatrixCell();
            return;
        }

        if (uiState == UiState.VCT_DATA_INPUT) {
            double val = vctEditData[vctIndex];
            if (!vctAccumulator.isBlank()) {
                try { val = Double.parseDouble(vctAccumulator); } catch (Exception ignored) {}
            }
            vctEditData[vctIndex] = val;
            vctAccumulator = "";
            vctIndex++;
            if (vctIndex >= vctEditSize) {
                vctIndex = 0;
            }
            displayVectorCell();
            return;
        }

        if (uiState == UiState.STAT_DATA_INPUT) {
            handleStatDataInput();
            return;
        }

        if (!multiStatements.isEmpty() && multiStatementIndex < multiStatements.size() - 1) {
            multiStatementIndex++;
            executeStatement(multiStatements.get(multiStatementIndex));
            return;
        } else {
            multiStatements.clear();
            multiStatementIndex = 0;
        }

        // Standard calculate in active mode
        calculate();
    }

    private void handleStatDataInput() {
        double val = 0.0;
        try {
            val = Double.parseDouble(statAccumulator.isBlank() ? "0" : statAccumulator);
        } catch (Exception ignored) {}
        statAccumulator = "";

        if (activeStatEngine.getType() == StatEngine.StatType.ONE_VAR) {
            activeStatEngine.addPoint(val);
            expressionField.setText("[" + (activeStatEngine.count() + 1) + "] X = ");
            setResultText("n = " + activeStatEngine.count());
        } else {
            if (statDataStep == 0) {
                statTempX = val;
                statDataStep = 1;
                expressionField.setText("[" + (activeStatEngine.count() + 1) + "] Y = ");
            } else {
                activeStatEngine.addPoint(statTempX, val);
                statDataStep = 0;
                expressionField.setText("[" + (activeStatEngine.count() + 1) + "] X = ");
                setResultText("n = " + activeStatEngine.count());
            }
        }
        updateLcdIndicators();
    }

    // =========================================================================
    // Core Calculation Router (COMP, CMPLX, BASE-N, MATRIX, VECTOR, STAT)
    // =========================================================================

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

        // Multi-statement handling with colon ':'
        if (source.contains(":")) {
            String[] stmts = source.split(":");
            multiStatements.clear();
            for (String s : stmts) {
                if (!s.isBlank()) multiStatements.add(s.trim());
            }
            if (!multiStatements.isEmpty()) {
                multiStatementIndex = 0;
                executeStatement(multiStatements.get(0));
                return;
            }
        }

        executeStatement(source);
    }

    private void executeStatement(String source) {
        try {
            variables.put("ans", answer);
            variables.put("ANS", answer);
            variables.put("M", memoryM);

            if (currentMode == CalculatorMode.CMPLX) {
                ComplexNumber cVal = new ComplexExpressionParser(source, isDegrees).evaluate();
                setResultText(cVal.formatRectangular());
                historyExpressions.add(source);
                historyResults.add(cVal.real());
                historyIndex = historyExpressions.size() - 1;
                isResultState = true;
                updateLcdIndicators();
                return;
            }

            if (currentMode == CalculatorMode.BASE_N) {
                long baseVal = BaseNEngine.evaluate(source, currentBase);
                setResultText(BaseNEngine.format(baseVal, currentBase));
                historyExpressions.add(source);
                historyResults.add((double) baseVal);
                historyIndex = historyExpressions.size() - 1;
                isResultState = true;
                updateLcdIndicators();
                return;
            }

            if (currentMode == CalculatorMode.MATRIX) {
                String mRes = MatrixVectorEvaluator.evaluateMatrix(source);
                setResultText(mRes);
                Double scalarResult = parseScalarResult(mRes);
                if (scalarResult != null) {
                    currentResultValue = scalarResult;
                    answer = scalarResult;
                }
                historyExpressions.add(source);
                historyResults.add(scalarResult == null ? 0.0 : scalarResult);
                historyIndex = historyExpressions.size() - 1;
                isResultState = true;
                updateLcdIndicators();
                return;
            }

            if (currentMode == CalculatorMode.VECTOR) {
                String vRes = MatrixVectorEvaluator.evaluateVector(source);
                setResultText(vRes);
                Double scalarResult = parseScalarResult(vRes);
                if (scalarResult != null) {
                    currentResultValue = scalarResult;
                    answer = scalarResult;
                }
                historyExpressions.add(source);
                historyResults.add(scalarResult == null ? 0.0 : scalarResult);
                historyIndex = historyExpressions.size() - 1;
                isResultState = true;
                updateLcdIndicators();
                return;
            }

            // Normal COMP / STAT Mode
            double value = new ExpressionParser(source, angleUnit, answer, variables).evaluate();
            currentResultValue = value;
            answer = value;

            String lower = source.toLowerCase(Locale.ROOT);
            if (lower.startsWith("pol(")) {
                Double r = variables.get("X");
                Double theta = variables.get("Y");
                if (r != null && theta != null) {
                    setResultText("r=" + CasioFormatter.formatDecimal(r) + ", θ=" + CasioFormatter.formatDecimal(theta));
                } else {
                    setResultText(CasioFormatter.formatDecimal(value));
                }
            } else if (lower.startsWith("rec(")) {
                Double xVal = variables.get("X");
                Double yVal = variables.get("Y");
                if (xVal != null && yVal != null) {
                    setResultText("X=" + CasioFormatter.formatDecimal(xVal) + ", Y=" + CasioFormatter.formatDecimal(yVal));
                } else {
                    setResultText(CasioFormatter.formatDecimal(value));
                }
            } else {
                setResultText(CasioFormatter.formatWithSettings(value, isExactDisplay, fixDigits, sciDigits, normMode));
            }

            historyExpressions.add(source);
            historyResults.add(value);
            historyIndex = historyExpressions.size() - 1;
            isResultState = true;
        } catch (ArithmeticException ex) {
            setResultText(ex.getMessage().contains("ERROR") ? ex.getMessage() : "Math ERROR");
            isResultState = true;
        } catch (Exception ex) {
            setResultText("Syntax ERROR");
            isResultState = true;
        }
        updateLcdIndicators();
    }

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
        angleUnit = switch (angleUnit) {
            case DEG -> ExpressionParser.AngleUnit.RAD;
            case RAD -> ExpressionParser.AngleUnit.GRA;
            case GRA -> ExpressionParser.AngleUnit.DEG;
        };
        isDegrees = (angleUnit == ExpressionParser.AngleUnit.DEG);
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
            setResultText(CasioFormatter.toExactOrFraction(currentResultValue));
        } else {
            setResultText(CasioFormatter.formatDecimal(currentResultValue));
        }
    }

    private void toggleMixedFractionDisplay() {
        if (resultLabel.getText().isBlank() || resultLabel.getText().contains("ERROR")) {
            return;
        }
        String curr = resultLabel.getText();
        if (curr.contains("⌟")) {
            setResultText(CasioFormatter.toExactOrFraction(currentResultValue));
        } else {
            setResultText(CasioFormatter.toMixedFraction(currentResultValue));
        }
    }

    private void toggleDmsDisplay() {
        if (resultLabel.getText().isBlank() || resultLabel.getText().contains("ERROR")) {
            return;
        }
        String curr = resultLabel.getText();
        if (curr.contains("°")) {
            setResultText(CasioFormatter.formatDecimal(currentResultValue));
        } else {
            setResultText(CasioFormatter.toDms(currentResultValue));
        }
    }

    private void toggleEngDisplay() {
        if (resultLabel.getText().isBlank() || resultLabel.getText().contains("ERROR")) {
            return;
        }
        double val = currentResultValue;
        if (val == 0.0) {
            setResultText("0×10⁰");
            return;
        }
        int exp = (int) Math.floor(Math.log10(Math.abs(val)));
        int engExp = exp - Math.floorMod(exp, 3);
        double mantissa = val / Math.pow(10, engExp);
        setResultText(CasioFormatter.formatDecimal(mantissa) + "×10^" + engExp);
    }

    private void toggleEngDisplayReverse() {
        if (resultLabel.getText().isBlank() || resultLabel.getText().contains("ERROR")) {
            return;
        }
        double val = currentResultValue;
        if (val == 0.0) {
            setResultText("0×10⁰");
            return;
        }
        int exp = (int) Math.floor(Math.log10(Math.abs(val)));
        int engExp = exp - Math.floorMod(exp, 3) + 3;
        double mantissa = val / Math.pow(10, engExp);
        setResultText(CasioFormatter.formatDecimal(mantissa) + "×10^" + engExp);
    }

    private void turnOff() {
        isPowerOn = false;
        isShift = false;
        isAlpha = false;
        isResultState = false;
        expressionField.clear();
        setResultText("");
        updateLcdIndicators();
    }

    private void turnOn() {
        isPowerOn = true;
        isShift = false;
        isAlpha = false;
        isResultState = false;
        uiState = UiState.NORMAL;
        expressionField.clear();
        setResultText("0");
        currentResultValue = 0;
        updateLcdIndicators();
    }

    private void allClear() {
        isShift = false;
        isAlpha = false;
        isResultState = false;
        if (uiState == UiState.STAT_DATA_INPUT) {
            uiState = UiState.NORMAL;
            expressionField.clear();
            setResultText("0");
            updateLcdIndicators();
            return;
        }
        if (uiState == UiState.MAT_DATA_INPUT) {
            MatrixEngine.setMatrix(matEditName, matEditMatrix);
            resetToNormal();
            return;
        }
        if (uiState == UiState.VCT_DATA_INPUT) {
            VectorEngine.setVector(vctEditName, new VectorEngine.Vector3D(vctEditData));
            resetToNormal();
            return;
        }
        if (uiState == UiState.CALC_PROMPT) {
            resetToNormal();
            return;
        }
        if (uiState != UiState.NORMAL) {
            setMode(currentMode);
            return;
        }
        expressionField.clear();
        setResultText(currentMode == CalculatorMode.BASE_N ? "0 " + currentBase.suffix() : "0");
        currentResultValue = 0;
        updateLcdIndicators();
    }

    private void deleteChar() {
        if (!isPowerOn) return;
        if (uiState == UiState.EQN_COEFF_INPUT || uiState == UiState.TABLE_START_INPUT
                || uiState == UiState.TABLE_END_INPUT || uiState == UiState.TABLE_STEP_INPUT
                || uiState == UiState.SOLVE_PROMPT) {
            String value = resultLabel.getText();
            if (!value.isEmpty()) setResultText(value.substring(0, value.length() - 1));
            promptInputStarted = true;
            return;
        }
        if (uiState == UiState.STAT_DATA_INPUT) {
            if (!statAccumulator.isEmpty()) {
                statAccumulator = statAccumulator.substring(0, statAccumulator.length() - 1);
                expressionField.setText("[" + (activeStatEngine.count() + 1) + "] "
                        + (statDataStep == 0 ? "X" : "Y") + " = " + statAccumulator);
            }
            return;
        }
        if (uiState == UiState.CALC_PROMPT) {
            if (!calcAccumulator.isEmpty()) {
                calcAccumulator = calcAccumulator.substring(0, calcAccumulator.length() - 1);
                setResultText(calcAccumulator);
            }
            return;
        }
        if (uiState == UiState.MAT_DATA_INPUT) {
            if (!matAccumulator.isEmpty()) {
                matAccumulator = matAccumulator.substring(0, matAccumulator.length() - 1);
                setResultText(matAccumulator);
            }
            return;
        }
        if (uiState == UiState.VCT_DATA_INPUT) {
            if (!vctAccumulator.isEmpty()) {
                vctAccumulator = vctAccumulator.substring(0, vctAccumulator.length() - 1);
                setResultText(vctAccumulator);
            }
            return;
        }
        if (isResultState) {
            isResultState = false;
            setResultText("");
            return;
        }
        expressionField.deletePreviousChar();
    }

    private void insert(String snippet) {
        if (!isPowerOn) return;

        if (uiState == UiState.STAT_DATA_INPUT) {
            if (snippet.equals("-")) {
                if (statAccumulator.startsWith("-")) statAccumulator = statAccumulator.substring(1);
                else statAccumulator = "-" + statAccumulator;
            } else if (snippet.equals(".")) {
                if (!statAccumulator.contains(".")) statAccumulator += ".";
            }
            expressionField.setText("[" + (activeStatEngine.count() + 1) + "] "
                    + (statDataStep == 0 ? "X" : "Y") + " = " + statAccumulator);
            return;
        }

        if (uiState == UiState.CALC_PROMPT) {
            if (snippet.equals("-") || snippet.equals("−")) {
                if (calcAccumulator.startsWith("-")) calcAccumulator = calcAccumulator.substring(1);
                else calcAccumulator = "-" + calcAccumulator;
            } else if (snippet.equals(".")) {
                if (!calcAccumulator.contains(".")) calcAccumulator += ".";
            }
            setResultText(calcAccumulator);
            return;
        }

        if (uiState == UiState.MAT_DATA_INPUT) {
            if (snippet.equals("-") || snippet.equals("−")) {
                if (matAccumulator.startsWith("-")) matAccumulator = matAccumulator.substring(1);
                else matAccumulator = "-" + matAccumulator;
            } else if (snippet.equals(".")) {
                if (!matAccumulator.contains(".")) matAccumulator += ".";
            }
            setResultText(matAccumulator);
            return;
        }

        if (uiState == UiState.VCT_DATA_INPUT) {
            if (snippet.equals("-") || snippet.equals("−")) {
                if (vctAccumulator.startsWith("-")) vctAccumulator = vctAccumulator.substring(1);
                else vctAccumulator = "-" + vctAccumulator;
            } else if (snippet.equals(".")) {
                if (!vctAccumulator.contains(".")) vctAccumulator += ".";
            }
            setResultText(vctAccumulator);
            return;
        }

        // In EQN, TABLE, and SOLVE prompt states, edit the value shown on the result line.
        // Defaults remain visible until the first keystroke, which replaces the default.
        if (uiState == UiState.EQN_COEFF_INPUT || uiState == UiState.TABLE_START_INPUT
                || uiState == UiState.TABLE_END_INPUT || uiState == UiState.TABLE_STEP_INPUT
                || uiState == UiState.SOLVE_PROMPT) {
            String curr = resultLabel.getText();
            if (!promptInputStarted) {
                curr = "";
                promptInputStarted = true;
            }
            if (snippet.equals("-") || snippet.equals("−")) {
                if (curr.startsWith("-")) curr = curr.substring(1);
                else curr = "-" + curr;
            } else if (snippet.equals(".")) {
                if (!curr.contains(".")) curr += ".";
            } else {
                curr += snippet;
            }
            setResultText(curr);
            return;
        }

        // Authentic Casio Behavior: After '=', typing an operator continues with Ans,
        // while typing a number/function starts a clean new expression!
        if (isResultState) {
            isResultState = false;
            if (isChainingOperator(snippet)) {
                expressionField.clear();
                expressionField.insertText(0, "Ans" + snippet);
                setResultText("");
                return;
            } else {
                expressionField.clear();
                setResultText("");
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
            setResultText("0");
        }
    }

    private void loadHistoryEntry(int idx) {
        String expr = historyExpressions.get(idx);
        expressionField.setText(expr);
        expressionField.positionCaret(expr.length());
        currentResultValue = historyResults.get(idx);
        setResultText(isExactDisplay
                ? CasioFormatter.toExactOrFraction(currentResultValue)
                : CasioFormatter.formatDecimal(currentResultValue));
        isResultState = true;
    }

    private void updateLcdIndicators() {
        if (!isPowerOn) {
            badgeShift.getStyleClass().setAll("lcd-badge-inactive");
            badgeAlpha.getStyleClass().setAll("lcd-badge-inactive");
            badgeM.getStyleClass().setAll("lcd-badge-inactive");
            badgeSto.getStyleClass().setAll("lcd-badge-inactive");
            badgeMode.getStyleClass().setAll("lcd-badge-inactive");
            badgeDeg.getStyleClass().setAll("lcd-badge-inactive");
            badgeMath.getStyleClass().setAll("lcd-badge-inactive");
            badgeUp.getStyleClass().setAll("lcd-badge-inactive");
            badgeDown.getStyleClass().setAll("lcd-badge-inactive");
            return;
        }

        badgeShift.getStyleClass().setAll(isShift ? "lcd-badge-active" : "lcd-badge-inactive");
        badgeAlpha.getStyleClass().setAll(isAlpha ? "lcd-badge-active" : "lcd-badge-inactive");
        badgeM.getStyleClass().setAll(Math.abs(memoryM) > 1e-12 ? "lcd-badge-active" : "lcd-badge-inactive");
        badgeSto.getStyleClass().setAll(isSto ? "lcd-badge-active" : "lcd-badge-inactive");

        // Casio Mode Badge
        if (currentMode != CalculatorMode.COMP) {
            badgeMode.setText(currentMode.badge());
            badgeMode.getStyleClass().setAll("lcd-badge-active");
        } else {
            badgeMode.setText("");
            badgeMode.getStyleClass().setAll("lcd-badge-inactive");
        }

        badgeDeg.setText(switch (angleUnit) {
            case DEG -> "D";
            case RAD -> "R";
            case GRA -> "G";
        });
        badgeDeg.getStyleClass().setAll("lcd-badge-active");

        badgeMath.getStyleClass().setAll(isExactDisplay ? "lcd-badge-active" : "lcd-badge-inactive");

        boolean canScrollUp = (uiState == UiState.TABLE_VIEW && tableRowIndex > 0)
                || (uiState == UiState.EQN_RESULT_VIEW && eqnResultLineIndex > 0)
                || (uiState == UiState.NORMAL && !historyExpressions.isEmpty());
        badgeUp.getStyleClass().setAll(canScrollUp ? "lcd-badge-active" : "lcd-badge-inactive");

        boolean canScrollDown = (uiState == UiState.TABLE_VIEW && tableRowIndex < tableRows.size() - 1)
                || (uiState == UiState.EQN_RESULT_VIEW && eqnResultLineIndex < eqnResultLines.size() - 1)
                || (uiState == UiState.NORMAL && historyIndex >= 0 && historyIndex < historyExpressions.size() - 1);
        badgeDown.getStyleClass().setAll(canScrollDown ? "lcd-badge-active" : "lcd-badge-inactive");
    }

    private void handlePhysicalKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleEqual();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            allClear();
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            handlePadUp();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            handlePadDown();
            event.consume();
        } else if (event.getCode() == KeyCode.LEFT) {
            cursorLeft();
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            cursorRight();
            event.consume();
        } else if (event.getCode() == KeyCode.BACK_SPACE) {
            deleteChar();
            event.consume();
        } else if (event.getCode().isDigitKey()) {
            if (uiState != UiState.NORMAL) {
                String digit = event.getText();
                if (!digit.isEmpty() && Character.isDigit(digit.charAt(0))) {
                    insertDigit(digit);
                    event.consume();
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
