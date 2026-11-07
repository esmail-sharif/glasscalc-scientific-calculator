package com.esmail.calculator.engine.mathio;

/**
 * A definite integral box containing integrand, lower bound, and upper bound.
 */
public class MathIntegral implements MathNode {
    private final MathContainer integrand;
    private final MathContainer lowerBound;
    private final MathContainer upperBound;

    public MathIntegral(MathContainer parent) {
        this.integrand = new MathContainer(parent);
        this.lowerBound = new MathContainer(parent);
        this.upperBound = new MathContainer(parent);
    }

    public MathContainer getIntegrand() { return integrand; }
    public MathContainer getLowerBound() { return lowerBound; }
    public MathContainer getUpperBound() { return upperBound; }

    @Override
    public String toExpressionString() {
        String expr = integrand.isEmpty() ? "0" : integrand.toExpressionString();
        String a = lowerBound.isEmpty() ? "0" : lowerBound.toExpressionString();
        String b = upperBound.isEmpty() ? "1" : upperBound.toExpressionString();
        return "∫(" + expr + ", " + a + ", " + b + ")";
    }

    @Override
    public String toDisplayString() {
        return "∫(" + integrand.toDisplayString() + ", " + lowerBound.toDisplayString() + ", " + upperBound.toDisplayString() + ")";
    }
}
