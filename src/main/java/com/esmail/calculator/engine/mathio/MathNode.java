package com.esmail.calculator.engine.mathio;

/**
 * Base interface for all nodes in the hierarchical 2D MathIO expression tree.
 * Independent of JavaFX to preserve architecture boundaries.
 */
public interface MathNode {
    /**
     * Converts this node and its children into an expression string compatible
     * with {@link com.esmail.calculator.engine.ExpressionParser}.
     */
    String toExpressionString();

    /**
     * Returns a human-readable display string representation of this node.
     */
    String toDisplayString();
}
