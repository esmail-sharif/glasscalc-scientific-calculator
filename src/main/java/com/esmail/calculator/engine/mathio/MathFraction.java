package com.esmail.calculator.engine.mathio;

/**
 * A 2D fraction box containing a numerator and denominator container.
 */
public class MathFraction implements MathNode {
    private final MathContainer numerator;
    private final MathContainer denominator;

    public MathFraction(MathContainer parent) {
        this.numerator = new MathContainer(parent);
        this.denominator = new MathContainer(parent);
    }

    public MathFraction(MathContainer numerator, MathContainer denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public MathContainer getNumerator() {
        return numerator;
    }

    public MathContainer getDenominator() {
        return denominator;
    }

    @Override
    public String toExpressionString() {
        String numStr = numerator.isEmpty() ? "0" : numerator.toExpressionString();
        String denStr = denominator.isEmpty() ? "1" : denominator.toExpressionString();
        return "(" + numStr + ")/(" + denStr + ")";
    }

    @Override
    public String toDisplayString() {
        return "(" + numerator.toDisplayString() + ")/(" + denominator.toDisplayString() + ")";
    }
}
