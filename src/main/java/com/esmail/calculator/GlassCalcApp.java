package com.esmail.calculator;

import com.esmail.calculator.engine.ExpressionParser;
import com.esmail.calculator.engine.MathEngine;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.util.Set;

/** GlassCalc desktop UI with basic and advanced scientific keypads. */
public final class GlassCalcApp extends Application {
    private static final Set<String> FUNCTION_KEYS = Set.of(
            "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
            "sec", "csc", "cot", "sinh", "cosh", "tanh",
            "asinh", "acosh", "atanh", "sqrt", "cbrt", "root", "hypot",
            "ln", "log", "log2", "logbase", "exp", "exp2", "pow",
            "abs", "floor", "ceil", "round", "trunc", "sign",
            "mod", "remainder", "idiv", "gcd", "lcm",
            "fact", "perm", "comb", "npr", "ncr",
            "mean", "median", "variance", "stdev", "samplevar", "samplestdev",
            "sum", "product", "min", "max"
    );

    private boolean arabic = true;
    private boolean degrees = true;
    private double answer;
    private final DecimalFormat format = new DecimalFormat("0.###############");
    private TextField expression;
    private Label result, title, status, historyTitle;
    private ToggleButton angle;
    private Button language;
    private ListView<String> history;
    private Tab basicTab, scientificTab;

    @Override public void start(Stage stage) {
        title = new Label();
        title.getStyleClass().add("title");
        status = new Label();
        status.getStyleClass().add("status");
        angle = new ToggleButton("DEG");
        angle.setSelected(true);
        angle.setOnAction(e -> {
            degrees = angle.isSelected();
            angle.setText(degrees ? "DEG" : "RAD");
        });
        language = new Button();
        language.setOnAction(e -> { arabic = !arabic; localize(); });

        Region spacer = new Region();
        HBox top = new HBox(12, title, spacer, status, angle, language);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.setAlignment(Pos.CENTER_LEFT);

        expression = new TextField();
        expression.setPromptText("sin(30) + sqrt(81)   |   mean(2,4,6)");
        expression.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        expression.setOnAction(e -> calculate());

        result = new Label("0");
        result.getStyleClass().add("result");
        result.setMaxWidth(Double.MAX_VALUE);
        result.setAlignment(Pos.CENTER_RIGHT);

        String[][] basicKeys = {
                {"AC", "⌫", "(", ")", "÷"},
                {"sin", "cos", "tan", "√", "xʸ"},
                {"7", "8", "9", "π", "×"},
                {"4", "5", "6", "e", "−"},
                {"1", "2", "3", "Ans", "+"},
                {"0", ".", "±", "%", "="}
        };
        String[][] scientificKeys = {
                {"asin", "acos", "atan", "atan2", "sinh", "cosh"},
                {"tanh", "asinh", "acosh", "atanh", "sec", "csc"},
                {"cot", "cbrt", "root", "hypot", "exp", "exp2"},
                {"ln", "log", "log2", "logbase", "pow", "1/x"},
                {"abs", "floor", "ceil", "round", "trunc", "sign"},
                {"!", "fact", "mod", "remainder", "idiv", "%"},
                {"gcd", "lcm", "perm", "comb", "npr", "ncr"},
                {"mean", "median", "variance", "stdev", "samplevar", "samplestdev"},
                {"sum", "product", "min", "max", "τ", "φ"}
        };

        basicTab = new Tab();
        basicTab.setClosable(false);
        basicTab.setContent(createKeypad(basicKeys, 5));
        scientificTab = new Tab();
        scientificTab.setClosable(false);
        ScrollPane scientificScroll = new ScrollPane(createKeypad(scientificKeys, 6));
        scientificScroll.setFitToWidth(true);
        scientificScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scientificScroll.getStyleClass().add("scientific-scroll");
        scientificTab.setContent(scientificScroll);
        TabPane tabs = new TabPane(basicTab, scientificTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setTabMinHeight(34);
        tabs.setTabMaxHeight(34);

        VBox calculator = new VBox(14, expression, result, tabs);
        calculator.getStyleClass().add("glass");
        calculator.setPadding(new Insets(22));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        HBox.setHgrow(calculator, Priority.ALWAYS);

        VBox side = new VBox(12);
        historyTitle = new Label();
        historyTitle.getStyleClass().add("section-title");
        history = new ListView<>();
        VBox.setVgrow(history, Priority.ALWAYS);
        history.setOnMouseClicked(e -> {
            String item = history.getSelectionModel().getSelectedItem();
            if (item != null) {
                int separator = item.indexOf(" = ");
                if (separator >= 0) expression.setText(item.substring(0, separator));
            }
        });
        Button clear = new Button("Clear history");
        clear.setOnAction(e -> history.getItems().clear());
        side.getStyleClass().add("glass");
        side.setPadding(new Insets(22));
        side.setPrefWidth(280);
        side.getChildren().addAll(historyTitle, history, clear);

        HBox body = new HBox(18, calculator, side);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox root = new VBox(18, top, body);
        root.setPadding(new Insets(28));
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 1120, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/glass.css").toExternalForm());
        stage.setTitle("GlassCalc — Scientific Calculator");
        stage.setMinWidth(900);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
        localize();
    }

    private GridPane createKeypad(String[][] keys, int columns) {
        GridPane pad = new GridPane();
        pad.setHgap(8);
        pad.setVgap(8);
        for (int c = 0; c < columns; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            pad.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < keys.length; r++) {
            for (int c = 0; c < keys[r].length; c++) {
                String key = keys[r][c];
                Button button = new Button(key);
                button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                button.setMinHeight(43);
                button.getStyleClass().add("key");
                if ("=".equals(key)) button.getStyleClass().add("equals");
                button.setOnAction(e -> press(key));
                pad.add(button, c, r);
            }
        }
        return pad;
    }

    private void localize() {
        title.setText(arabic ? "GlassCalc  |  الآلة الحاسبة العلمية" : "GlassCalc  |  Scientific Calculator");
        status.setText(MathEngine.isAssemblyActive()
                ? (arabic ? "محرك Assembly نشط" : "Assembly engine active")
                : (arabic ? "وضع التطوير — Java" : "Development mode — Java"));
        language.setText(arabic ? "English" : "العربية");
        historyTitle.setText(arabic ? "سجل العمليات" : "History");
        basicTab.setText(arabic ? "أساسي" : "Basic");
        scientificTab.setText(arabic ? "علمي متقدم" : "Advanced Science");
        history.setNodeOrientation(arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        title.setNodeOrientation(arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
    }

    private void press(String key) {
        switch (key) {
            case "AC" -> { expression.clear(); result.setText("0"); }
            case "⌫" -> {
                String text = expression.getText();
                if (!text.isEmpty()) expression.setText(text.substring(0, text.length() - 1));
            }
            case "=" -> calculate();
            case "√" -> expression.appendText("sqrt(");
            case "xʸ" -> expression.appendText("^");
            case "×" -> expression.appendText("*");
            case "÷" -> expression.appendText("/");
            case "−" -> expression.appendText("-");
            case "π" -> expression.appendText("pi");
            case "τ" -> expression.appendText("tau");
            case "φ" -> expression.appendText("phi");
            case "Ans" -> expression.appendText("ans");
            case "1/x" -> expression.appendText("1/(");
            case "±" -> expression.appendText("-");
            case "!" -> expression.appendText("!");
            case "%" -> expression.appendText("%");
            default -> {
                if (FUNCTION_KEYS.contains(key)) expression.appendText(key + "(");
                else expression.appendText(key);
            }
        }
        expression.requestFocus();
    }

    private void calculate() {
        try {
            String source = expression.getText();
            double value = new ExpressionParser(source, degrees, answer).evaluate();
            answer = value;
            String formatted = format.format(value);
            result.setText(formatted);
            history.getItems().add(0, source + " = " + formatted);
        } catch (RuntimeException ex) {
            result.setText((arabic ? "خطأ: " : "Error: ") + ex.getMessage());
        }
    }

    public static void main(String[] args) { launch(args); }
}
