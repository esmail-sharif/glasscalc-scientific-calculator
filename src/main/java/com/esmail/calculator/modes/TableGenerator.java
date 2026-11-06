package com.esmail.calculator.modes;

import com.esmail.calculator.engine.CasioFormatter;
import com.esmail.calculator.engine.ExpressionParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CASIO TABLE Mode Engine (Mode 7).
 * Generates numerical tables of f(X) from Start to End with Step.
 */
public final class TableGenerator {
    public record TableRow(int index, double x, double fx, String xFormatted, String fxFormatted) {}

    public static List<TableRow> generate(String expression, double start, double end, double step, boolean degrees) {
        if (step <= 0.0) throw new ArithmeticException("Step must be positive");
        if (start > end) throw new ArithmeticException("Start must be <= End");

        List<TableRow> rows = new ArrayList<>();
        int count = 0;
        int maxRows = 30; // Casio standard max table size

        for (double x = start; x <= end + 1e-9 && count < maxRows; x += step) {
            count++;
            Map<String, Double> vars = new HashMap<>();
            vars.put("x", x);
            vars.put("X", x);

            double fx;
            String fxStr;
            try {
                fx = new ExpressionParser(expression, degrees, 0.0, vars).evaluate();
                fxStr = CasioFormatter.toExactOrFraction(fx);
            } catch (ArithmeticException e) {
                fx = Double.NaN;
                fxStr = "Math ERROR";
            } catch (Exception e) {
                fx = Double.NaN;
                fxStr = "ERROR";
            }

            rows.add(new TableRow(count, x, fx, CasioFormatter.toExactOrFraction(x), fxStr));
        }

        return rows;
    }

    public static List<TableRow> generateTable(String expression, double start, double end, double step, boolean degrees) {
        return generate(expression, start, end, step, degrees);
    }
}
