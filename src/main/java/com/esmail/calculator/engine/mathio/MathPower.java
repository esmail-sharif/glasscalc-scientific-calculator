package com.esmail.calculator.engine.mathio;

/**
 * A power box containing an exponent container.
 */
public class MathPower implements MathNode {
    private final MathContainer exponent;

    public MathPower(MathContainer parent) {
        this.exponent = new MathContainer(parent);
    }

    public MathPower(MathContainer exponent, boolean isExplicit) {
        this.exponent = exponent;
    }

    public MathContainer getExponent() {
        return exponent;
    }

    @Override
    public String toExpressionString() {
        String expStr = exponent.isEmpty() ? "1" : exponent.toExpressionString();
        return "^(" + expStr + ")";
    }

    @Override
    public String toDisplayString() {
        return "^(" + exponent.toDisplayString() + ")";
    }
}
