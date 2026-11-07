package com.esmail.calculator.engine.mathio;

/**
 * A leaf node representing a single character or token in the MathIO tree.
 */
public record MathChar(char character) implements MathNode {
    @Override
    public String toExpressionString() {
        return String.valueOf(character);
    }

    @Override
    public String toDisplayString() {
        return String.valueOf(character);
    }
}
