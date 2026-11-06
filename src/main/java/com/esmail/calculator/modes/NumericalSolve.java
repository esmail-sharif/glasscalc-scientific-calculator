package com.esmail.calculator.modes;

import com.esmail.calculator.engine.ExpressionParser;

import java.util.HashMap;
import java.util.Map;

/**
 * CASIO SOLVE Feature (SHIFT + CALC).
 * Uses high-precision Newton-Raphson and Secant root finding with fallback bisection
 * to solve f(X) = 0 for an unknown variable X.
 */
public final class NumericalSolve {

    public record SolveResult(double root, double residual) {}

    public static SolveResult solve(String equation, double initialGuess, boolean degrees, Map<String, Double> externalVars) {
        String funcExpr;
        if (equation.contains("=")) {
            String[] parts = equation.split("=", 2);
            funcExpr = "(" + parts[0].trim() + ") - (" + parts[1].trim() + ")";
        } else {
            funcExpr = equation.trim();
        }

        Map<String, Double> vars = new HashMap<>(externalVars != null ? externalVars : Map.of());

        // Evaluate f(x)
        java.util.function.DoubleUnaryOperator f = x -> {
            vars.put("X", x);
            vars.put("x", x);
            return new ExpressionParser(funcExpr, degrees, 0.0, vars).evaluate();
        };

        double x = initialGuess;
        double fx = f.applyAsDouble(x);
        if (Math.abs(fx) < 1e-12) {
            return new SolveResult(x, fx);
        }

        // 1. Try Newton-Raphson
        double h = 1e-6;
        for (int iter = 0; iter < 80; iter++) {
            fx = f.applyAsDouble(x);
            if (Math.abs(fx) < 1e-11) {
                return new SolveResult(x, fx);
            }
            double df = (f.applyAsDouble(x + h) - f.applyAsDouble(x - h)) / (2.0 * h);
            if (Math.abs(df) < 1e-14) {
                x += 0.1; // nudge
                continue;
            }
            double step = fx / df;
            if (Double.isNaN(step) || Double.isInfinite(step)) {
                break;
            }
            x -= step;
        }

        // 2. Try Secant method
        double x0 = initialGuess - 0.1;
        double x1 = initialGuess + 0.1;
        double f0 = f.applyAsDouble(x0);
        double f1 = f.applyAsDouble(x1);

        for (int iter = 0; iter < 100; iter++) {
            if (Math.abs(f1) < 1e-11) {
                return new SolveResult(x1, f1);
            }
            double denom = f1 - f0;
            if (Math.abs(denom) < 1e-14) break;
            double x2 = x1 - f1 * (x1 - x0) / denom;
            x0 = x1;
            f0 = f1;
            x1 = x2;
            f1 = f.applyAsDouble(x1);
        }

        // 3. Fallback: bracketed search around initial guess
        double bestX = x1;
        double bestF = Math.abs(f1);
        double range = 100.0;
        double scanStep = 0.5;
        double left = initialGuess - range;
        double prevVal = f.applyAsDouble(left);
        for (double s = left + scanStep; s <= initialGuess + range; s += scanStep) {
            double curVal = f.applyAsDouble(s);
            if (Math.abs(curVal) < bestF) {
                bestF = Math.abs(curVal);
                bestX = s;
            }
            if (prevVal * curVal <= 0.0) {
                // Bracket found [s - scanStep, s], use bisection
                double a = s - scanStep;
                double b = s;
                for (int bIter = 0; bIter < 60; bIter++) {
                    double mid = (a + b) / 2.0;
                    double fMid = f.applyAsDouble(mid);
                    if (Math.abs(fMid) < 1e-11) {
                        return new SolveResult(mid, fMid);
                    }
                    if (prevVal * fMid <= 0.0) {
                        b = mid;
                    } else {
                        a = mid;
                        prevVal = fMid;
                    }
                }
                return new SolveResult((a + b) / 2.0, f.applyAsDouble((a + b) / 2.0));
            }
            prevVal = curVal;
        }

        return new SolveResult(bestX, f.applyAsDouble(bestX));
    }
}
