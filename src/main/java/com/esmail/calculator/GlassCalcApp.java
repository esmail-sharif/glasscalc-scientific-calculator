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
import java.util.Locale;

/** GlassCalc desktop UI. All expressions are evaluated by ExpressionParser/MathEngine. */
public final class GlassCalcApp extends Application {
    private boolean arabic = true;
    private boolean degrees = true;
    private double answer;
    private final DecimalFormat format = new DecimalFormat("0.###############");
    private TextField expression;
    private Label result, title, status, historyTitle;
    private ToggleButton angle;
    private Button language;
    private ListView<String> history;

    @Override public void start(Stage stage) {
        title = new Label();
        title.getStyleClass().add("title");
        status = new Label();
        status.getStyleClass().add("status");
        angle = new ToggleButton("DEG");
        angle.setSelected(true);
        angle.setOnAction(e -> { degrees = angle.isSelected(); angle.setText(degrees ? "DEG" : "RAD"); });
        language = new Button();
        language.setOnAction(e -> { arabic = !arabic; localize(); });
        HBox top = new HBox(12, title, new Region(), status, angle, language);
        HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        top.setAlignment(Pos.CENTER_LEFT);

        expression = new TextField();
        expression.setPromptText("sin(30) + sqrt(81)");
        expression.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        expression.setOnAction(e -> calculate());
        result = new Label("0");
        result.getStyleClass().add("result");
        result.setMaxWidth(Double.MAX_VALUE);
        result.setAlignment(Pos.CENTER_RIGHT);

        String[][] keys = {
            {"AC","⌫","(",")","÷"}, {"sin","cos","tan","asin","acos"},
            {"atan","√","ln","log","×"}, {"x²","xʸ","!","%","−"},
            {"7","8","9","π","+"}, {"4","5","6","e","1/x"},
            {"1","2","3","Ans","="}, {"0",".","±","exp","fact"}
        };
        GridPane pad = new GridPane();
        pad.setHgap(8); pad.setVgap(8);
        for (int c=0;c<5;c++){ ColumnConstraints cc=new ColumnConstraints(); cc.setPercentWidth(20); cc.setHgrow(Priority.ALWAYS); pad.getColumnConstraints().add(cc); }
        for(int r=0;r<keys.length;r++) for(int c=0;c<5;c++){
            String key=keys[r][c]; Button b=new Button(key); b.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE); b.setMinHeight(48); b.getStyleClass().add("key");
            if ("=".equals(key)) b.getStyleClass().add("equals");
            b.setOnAction(e -> press(key)); pad.add(b,c,r);
        }
        VBox calculator = new VBox(14, expression, result, pad);
        calculator.getStyleClass().add("glass");
        calculator.setPadding(new Insets(22));
        VBox side = new VBox(12);
        historyTitle = new Label(); historyTitle.getStyleClass().add("section-title");
        history = new ListView<>(); VBox.setVgrow(history,Priority.ALWAYS);
        history.setOnMouseClicked(e -> { String item=history.getSelectionModel().getSelectedItem(); if(item!=null) expression.setText(item.substring(0,item.indexOf(" = "))); });
        Button clear = new Button("Clear history"); clear.setOnAction(e -> history.getItems().clear());
        side.getStyleClass().add("glass"); side.setPadding(new Insets(22)); side.setPrefWidth(280); side.getChildren().addAll(historyTitle,history,clear);
        HBox body = new HBox(18,calculator,side); HBox.setHgrow(calculator,Priority.ALWAYS); VBox.setVgrow(body,Priority.ALWAYS);
        VBox root = new VBox(18,top,body); root.setPadding(new Insets(28)); root.getStyleClass().add("root");
        Scene scene = new Scene(root,1080,760); scene.getStylesheets().add(getClass().getResource("/styles/glass.css").toExternalForm());
        stage.setTitle("GlassCalc — Scientific Calculator"); stage.setMinWidth(850); stage.setMinHeight(650); stage.setScene(scene); stage.show();
        localize();
    }

    private void localize() {
        title.setText(arabic ? "GlassCalc  |  الآلة الحاسبة العلمية" : "GlassCalc  |  Scientific Calculator");
        status.setText(MathEngine.isAssemblyActive() ? (arabic ? "محرك Assembly نشط" : "Assembly engine active") : (arabic ? "وضع التطوير — Java" : "Development mode — Java"));
        language.setText(arabic ? "English" : "العربية");
        historyTitle.setText(arabic ? "سجل العمليات" : "History");
        history.setNodeOrientation(arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        title.setNodeOrientation(arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
    }

    private void press(String key) {
        switch(key) {
            case "AC" -> { expression.clear(); result.setText("0"); }
            case "⌫" -> { String s=expression.getText(); if(!s.isEmpty()) expression.setText(s.substring(0,s.length()-1)); }
            case "=" -> calculate();
            case "sin","cos","tan","asin","acos","atan","ln","log","fact","exp" -> expression.appendText(key+"(");
            case "√" -> expression.appendText("sqrt(");
            case "x²" -> expression.appendText("^2");
            case "xʸ" -> expression.appendText("^");
            case "×" -> expression.appendText("*");
            case "÷" -> expression.appendText("/");
            case "−" -> expression.appendText("-");
            case "π" -> expression.appendText("pi");
            case "Ans" -> expression.appendText("ans");
            case "1/x" -> expression.appendText("1/(");
            case "±" -> expression.appendText("-");
            default -> expression.appendText(key);
        }
        expression.requestFocus();
    }

    private void calculate() {
        try {
            String source=expression.getText();
            double value=new ExpressionParser(source,degrees,answer).evaluate();
            answer=value; String formatted=format.format(value); result.setText(formatted);
            history.getItems().add(0,source+" = "+formatted);
        } catch (RuntimeException ex) {
            result.setText((arabic ? "خطأ: " : "Error: ")+ex.getMessage());
        }
    }

    public static void main(String[] args) { launch(args); }
}
