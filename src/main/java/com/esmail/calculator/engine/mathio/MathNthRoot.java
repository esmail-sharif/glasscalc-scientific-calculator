package com.esmail.calculator.engine.mathio;

/**
 * An n-th root box containing an index/degree container and a radicand container.
 */
public class MathNthRoot implements MathNode {
    private final MathContainer index;
    private final MathContainer radicand;

    public MathNthRoot(MathContainer parent) {
        this.index = new MathContainer(parent);
        this.radicand = new MathContainer(parent);
    }

    public MathNthRoot(MathContainer index, MathContainer radicand) {
        this.index = index;
        this.radicand = radicand;
    }

    public MathContainer getIndex() {
        return index;
    }

    public MathContainer getRadicand() {
        return radicand;
    }

    @Override
    public String toExpressionString() {
        String idxStr = index.isEmpty() ? "2" : index.toExpressionString();
        String radStr = radicand.isEmpty() ? "0" : radicand.toExpressionString();
        return "root(" + radStr + ", " + idxStr + ")";
    }

    @Override
    public String toDisplayString() {
        return "root(" + index.toDisplayString() + ", " + radicand.toDisplayString() + ")";
    }
}
