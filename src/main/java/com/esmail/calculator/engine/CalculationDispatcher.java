package com.esmail.calculator.engine;

import com.esmail.calculator.modes.BaseNEngine;
import com.esmail.calculator.modes.CalculatorMode;
import com.esmail.calculator.modes.MatrixVectorEvaluator;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pure calculation router for the calculator's expression-oriented modes.
 *
 * <p>The dispatcher evaluates an expression and returns the display/history
 * effects as data. It does not touch JavaFX controls or application lifecycle.
 * The supplied variable map is shared with ExpressionParser because POL/REC
 * intentionally publish X/Y values to the calculator's variable memory.</p>
 */
public final class CalculationDispatcher {

    public record Request(
            String source,
            CalculatorMode mode,
            ExpressionParser.AngleUnit angleUnit,
            double answer,
            double memory,
            Map<String, Double> variables,
            boolean degrees,
            boolean complexPolarDisplay,
            boolean exactDisplay,
            boolean mixedFractionDisplay,
            int fixDigits,
            int sciDigits,
            int normMode) {
        public Request {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(angleUnit, "angleUnit");
            Objects.requireNonNull(variables, "variables");
        }
    }

    /**
     * Null answer/currentResult fields mean the active mode did not produce a
     * scalar Ans value (e.g. a complex or structured matrix result).
     */
    public record Result(
            String displayText,
            Double answerUpdate,
            Double currentResultUpdate,
            double historyValue) {
        public Result {
            Objects.requireNonNull(displayText, "displayText");
        }
    }

    public Result dispatch(Request request) {
        Objects.requireNonNull(request, "request");
        Map<String, Double> variables = request.variables();
        variables.put("ans", request.answer());
        variables.put("ANS", request.answer());
        variables.put("M", request.memory());

        return switch (request.mode()) {
            case CMPLX -> {
                ComplexNumber value = new ComplexExpressionParser(
                        request.source(), request.degrees()).evaluate();
                String display = request.complexPolarDisplay()
                        ? value.formatPolar(request.degrees())
                        : value.formatRectangular();
                yield new Result(display, null, null, value.real());
            }
            case BASE_N -> throw new IllegalArgumentException(
                    "BASE-N dispatch requires an explicit radix");
            case MATRIX -> dispatchMatrix(request.source(), variables,
                    request.angleUnit(), request.answer());
            case VECTOR -> dispatchVector(request.source(), variables,
                    request.angleUnit(), request.answer());
            case COMP, STAT, EQN, TABLE -> dispatchScalar(request);
        };
    }

    /**
     * Base-N overload preserving the user's selected radix without putting UI
     * state in the dispatcher.
     */
    public Result dispatch(Request request, BaseNEngine.Base base) {
        if (request.mode() != CalculatorMode.BASE_N) return dispatch(request);
        Objects.requireNonNull(base, "base");
        request.variables().put("ans", request.answer());
        request.variables().put("ANS", request.answer());
        request.variables().put("M", request.memory());
        long value = BaseNEngine.evaluate(request.source(), base);
        return new Result(BaseNEngine.format(value, base), null, null, (double) value);
    }

    private Result dispatchMatrix(
            String source, Map<String, Double> variables,
            ExpressionParser.AngleUnit angleUnit, double answer) {
        String display = MatrixVectorEvaluator.evaluateMatrix(source);
        Double scalar = parseScalarResult(display, angleUnit, answer, variables);
        return scalar == null
                ? new Result(display, null, null, 0.0)
                : new Result(display, scalar, scalar, scalar);
    }

    private Result dispatchVector(
            String source, Map<String, Double> variables,
            ExpressionParser.AngleUnit angleUnit, double answer) {
        String display = MatrixVectorEvaluator.evaluateVector(source);
        Double scalar = parseScalarResult(display, angleUnit, answer, variables);
        return scalar == null
                ? new Result(display, null, null, 0.0)
                : new Result(display, scalar, scalar, scalar);
    }

    private Result dispatchScalar(Request request) {
        double value = new ExpressionParser(
                request.source(), request.angleUnit(), request.answer(), request.variables()).evaluate();

        String lower = request.source().toLowerCase(Locale.ROOT);
        String display;
        if (lower.startsWith("pol(")) {
            Double r = request.variables().get("X");
            Double theta = request.variables().get("Y");
            display = r != null && theta != null
                    ? "r=" + CasioFormatter.formatDecimal(r)
                    + ", θ=" + CasioFormatter.formatDecimal(theta)
                    : formatDisplayValue(value, request);
        } else if (lower.startsWith("rec(")) {
            Double x = request.variables().get("X");
            Double y = request.variables().get("Y");
            display = x != null && y != null
                    ? "X=" + CasioFormatter.formatDecimal(x)
                    + ", Y=" + CasioFormatter.formatDecimal(y)
                    : formatDisplayValue(value, request);
        } else {
            display = formatDisplayValue(value, request);
        }
        return new Result(display, value, value, value);
    }

    private Double parseScalarResult(
            String displayValue, ExpressionParser.AngleUnit angleUnit,
            double answer, Map<String, Double> variables) {
        try {
            return new ExpressionParser(displayValue, angleUnit, answer, variables).evaluate();
        } catch (RuntimeException notAScalar) {
            return null;
        }
    }

    private String formatDisplayValue(double value, Request request) {
        if (request.exactDisplay() && request.fixDigits() < 0 && request.sciDigits() < 0) {
            return request.mixedFractionDisplay()
                    ? CasioFormatter.toMixedFraction(value)
                    : CasioFormatter.toExactOrFraction(value);
        }
        return CasioFormatter.formatWithSettings(
                value, request.exactDisplay(), request.fixDigits(),
                request.sciDigits(), request.normMode());
    }
}
