package com.esmail.calculator.engine.mathio;

/**
 * A square root box containing a radicand container.
 */
public class MathSqrt implements MathNode {
    private final MathContainer radicand;

    public MathSqrt(MathContainer parent) {
        this.radicand = new MathContainer(parent);
    }

    public MathSqrt(MathContainer radicand, boolean isExplicit) {
        this.radicand = radicand;
    }

    public MathContainer getRadicand() {
        return radicand;
    }

    @Override
    public String toExpressionString() {
        String radStr = radicand.isEmpty() ? "0" : radicand.toExpressionString();
        return "√(" + radStr + ")";
    }

    @Override
    public String toDisplayString() {
        return "√(" + radicand.toDisplayString() + ")";
    }
}
