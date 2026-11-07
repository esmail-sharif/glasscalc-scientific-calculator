package com.esmail.calculator.engine.mathio;

/**
 * A summation box containing summand, lower bound, and upper bound.
 */
public class MathSummation implements MathNode {
    private final MathContainer summand;
    private final MathContainer lowerBound;
    private final MathContainer upperBound;

    public MathSummation(MathContainer parent) {
        this.summand = new MathContainer(parent);
        this.lowerBound = new MathContainer(parent);
        this.upperBound = new MathContainer(parent);
    }

    public MathContainer getSummand() { return summand; }
    public MathContainer getLowerBound() { return lowerBound; }
    public MathContainer getUpperBound() { return upperBound; }

    @Override
    public String toExpressionString() {
        String expr = summand.isEmpty() ? "0" : summand.toExpressionString();
        String a = lowerBound.isEmpty() ? "1" : lowerBound.toExpressionString();
        String b = upperBound.isEmpty() ? "5" : upperBound.toExpressionString();
        return "Σ(" + expr + ", " + a + ", " + b + ")";
    }

    @Override
    public String toDisplayString() {
        return "Σ(" + summand.toDisplayString() + ", " + lowerBound.toDisplayString() + ", " + upperBound.toDisplayString() + ")";
    }
}
