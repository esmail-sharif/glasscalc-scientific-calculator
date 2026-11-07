package com.esmail.calculator.engine.mathio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A container of math nodes supporting 2D cursor navigation and structured insertions.
 * Independent of JavaFX to preserve domain architecture boundaries.
 */
public class MathContainer implements MathNode {
    private final List<MathNode> children = new ArrayList<>();
    private int cursorIndex = 0;
    private MathContainer parent;
    private MathNode enclosingNode;

    public MathContainer() {
        this(null, null);
    }

    public MathContainer(MathContainer parent) {
        this(parent, null);
    }

    public MathContainer(MathContainer parent, MathNode enclosingNode) {
        this.parent = parent;
        this.enclosingNode = enclosingNode;
    }

    public MathContainer getParent() {
        return parent;
    }

    public void setParent(MathContainer parent) {
        this.parent = parent;
    }

    public MathNode getEnclosingNode() {
        return enclosingNode;
    }

    public void setEnclosingNode(MathNode enclosingNode) {
        this.enclosingNode = enclosingNode;
    }

    public List<MathNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public void setCursorIndex(int index) {
        if (index < 0) this.cursorIndex = 0;
        else if (index > children.size()) this.cursorIndex = children.size();
        else this.cursorIndex = index;
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public int size() {
        return children.size();
    }

    public void clear() {
        children.clear();
        cursorIndex = 0;
    }

    /**
     * Inserts a single character leaf at the current cursor position.
     */
    public void insertChar(char c) {
        children.add(cursorIndex, new MathChar(c));
        cursorIndex++;
    }

    /**
     * Inserts a string of characters at the current cursor position.
     */
    public void insertText(String text) {
        if (text == null) return;
        for (int i = 0; i < text.length(); i++) {
            insertChar(text.charAt(i));
        }
    }

    /**
     * Inserts a child node at the current cursor position and advances the cursor.
     */
    public void insertNode(MathNode node) {
        if (node == null) return;
        children.add(cursorIndex, node);
        cursorIndex++;
    }

    /**
     * Inserts a fraction at cursor and returns the numerator as the new active container.
     */
    public MathContainer insertFraction() {
        MathFraction fraction = new MathFraction(this);
        fraction.getNumerator().setEnclosingNode(fraction);
        fraction.getDenominator().setEnclosingNode(fraction);
        insertNode(fraction);
        return fraction.getNumerator();
    }

    /**
     * Inserts a square root at cursor and returns the radicand as the new active container.
     */
    public MathContainer insertSqrt() {
        MathSqrt sqrt = new MathSqrt(this);
        sqrt.getRadicand().setEnclosingNode(sqrt);
        insertNode(sqrt);
        return sqrt.getRadicand();
    }

    /**
     * Inserts an n-th root at cursor and returns the degree index as the new active container.
     */
    public MathContainer insertNthRoot() {
        MathNthRoot nthRoot = new MathNthRoot(this);
        nthRoot.getIndex().setEnclosingNode(nthRoot);
        nthRoot.getRadicand().setEnclosingNode(nthRoot);
        insertNode(nthRoot);
        return nthRoot.getIndex();
    }

    /**
     * Inserts an exponent power box at cursor and returns the exponent as the new active container.
     */
    public MathContainer insertPower() {
        MathPower power = new MathPower(this);
        power.getExponent().setEnclosingNode(power);
        insertNode(power);
        return power.getExponent();
    }

    /**
     * Inserts a definite integral at cursor and returns the integrand as the new active container.
     */
    public MathContainer insertIntegral() {
        MathIntegral integral = new MathIntegral(this);
        integral.getIntegrand().setEnclosingNode(integral);
        integral.getLowerBound().setEnclosingNode(integral);
        integral.getUpperBound().setEnclosingNode(integral);
        insertNode(integral);
        return integral.getIntegrand();
    }

    /**
     * Inserts a summation at cursor and returns the summand as the new active container.
     */
    public MathContainer insertSummation() {
        MathSummation summation = new MathSummation(this);
        summation.getSummand().setEnclosingNode(summation);
        summation.getLowerBound().setEnclosingNode(summation);
        summation.getUpperBound().setEnclosingNode(summation);
        insertNode(summation);
        return summation.getSummand();
    }

    /**
     * Deletes the item immediately before the cursor.
     * Returns the active container after deletion.
     */
    public MathContainer delete() {
        if (cursorIndex > 0) {
            MathNode removed = children.remove(cursorIndex - 1);
            cursorIndex--;
            return this;
        }
        // If cursorIndex == 0 and this is an inner container:
        if (parent != null && enclosingNode != null) {
            if (isEmpty()) {
                // If this box is empty, remove the enclosing node from the parent
                int nodeIdx = parent.children.indexOf(enclosingNode);
                if (nodeIdx >= 0) {
                    parent.children.remove(nodeIdx);
                    parent.cursorIndex = nodeIdx;
                }
                return parent;
            } else {
                // Move cursor to parent before this enclosing node
                int nodeIdx = parent.children.indexOf(enclosingNode);
                parent.cursorIndex = Math.max(0, nodeIdx);
                return parent;
            }
        }
        return this;
    }

    /**
     * Horizontal navigation: Move cursor Left.
     * Returns the container that now holds the cursor.
     */
    public MathContainer moveLeft() {
        if (cursorIndex > 0) {
            MathNode prev = children.get(cursorIndex - 1);
            if (prev instanceof MathFraction frac) {
                frac.getDenominator().cursorIndex = frac.getDenominator().size();
                return frac.getDenominator();
            }
            if (prev instanceof MathSqrt sqrt) {
                sqrt.getRadicand().cursorIndex = sqrt.getRadicand().size();
                return sqrt.getRadicand();
            }
            if (prev instanceof MathNthRoot nthRoot) {
                nthRoot.getRadicand().cursorIndex = nthRoot.getRadicand().size();
                return nthRoot.getRadicand();
            }
            if (prev instanceof MathPower pwr) {
                pwr.getExponent().cursorIndex = pwr.getExponent().size();
                return pwr.getExponent();
            }
            if (prev instanceof MathIntegral integ) {
                integ.getUpperBound().cursorIndex = integ.getUpperBound().size();
                return integ.getUpperBound();
            }
            if (prev instanceof MathSummation sum) {
                sum.getUpperBound().cursorIndex = sum.getUpperBound().size();
                return sum.getUpperBound();
            }
            cursorIndex--;
            return this;
        }
        // At start of container: step out to parent or predecessor internal container
        if (enclosingNode instanceof MathNthRoot nth && this == nth.getRadicand()) {
            nth.getIndex().cursorIndex = nth.getIndex().size();
            return nth.getIndex();
        }
        if (enclosingNode instanceof MathFraction frac && this == frac.getDenominator()) {
            frac.getNumerator().cursorIndex = frac.getNumerator().size();
            return frac.getNumerator();
        }
        if (parent != null && enclosingNode != null) {
            int nodeIdx = parent.children.indexOf(enclosingNode);
            parent.cursorIndex = Math.max(0, nodeIdx);
            return parent;
        }
        return this;
    }

    /**
     * Horizontal navigation: Move cursor Right.
     * Returns the container that now holds the cursor.
     */
    public MathContainer moveRight() {
        if (cursorIndex < children.size()) {
            MathNode next = children.get(cursorIndex);
            if (next instanceof MathFraction frac) {
                frac.getNumerator().cursorIndex = 0;
                return frac.getNumerator();
            }
            if (next instanceof MathSqrt sqrt) {
                sqrt.getRadicand().cursorIndex = 0;
                return sqrt.getRadicand();
            }
            if (next instanceof MathNthRoot nthRoot) {
                nthRoot.getIndex().cursorIndex = 0;
                return nthRoot.getIndex();
            }
            if (next instanceof MathPower pwr) {
                pwr.getExponent().cursorIndex = 0;
                return pwr.getExponent();
            }
            if (next instanceof MathIntegral integ) {
                integ.getIntegrand().cursorIndex = 0;
                return integ.getIntegrand();
            }
            if (next instanceof MathSummation sum) {
                sum.getSummand().cursorIndex = 0;
                return sum.getSummand();
            }
            cursorIndex++;
            return this;
        }
        // At end of container: step into sibling container or parent
        if (enclosingNode instanceof MathNthRoot nth && this == nth.getIndex()) {
            nth.getRadicand().cursorIndex = 0;
            return nth.getRadicand();
        }
        if (parent != null && enclosingNode != null) {
            int nodeIdx = parent.children.indexOf(enclosingNode);
            parent.cursorIndex = Math.min(parent.children.size(), nodeIdx + 1);
            return parent;
        }
        return this;
    }

    /**
     * Vertical navigation: Move cursor Down.
     */
    public MathContainer moveDown() {
        if (enclosingNode instanceof MathFraction frac) {
            if (this == frac.getNumerator()) {
                frac.getDenominator().cursorIndex = Math.min(this.cursorIndex, frac.getDenominator().size());
                return frac.getDenominator();
            }
        }
        if (enclosingNode instanceof MathIntegral integ) {
            if (this == integ.getUpperBound()) {
                integ.getIntegrand().cursorIndex = 0;
                return integ.getIntegrand();
            }
            if (this == integ.getIntegrand()) {
                integ.getLowerBound().cursorIndex = 0;
                return integ.getLowerBound();
            }
        }
        if (enclosingNode instanceof MathSummation sum) {
            if (this == sum.getUpperBound()) {
                sum.getSummand().cursorIndex = 0;
                return sum.getSummand();
            }
            if (this == sum.getSummand()) {
                sum.getLowerBound().cursorIndex = 0;
                return sum.getLowerBound();
            }
        }
        if (enclosingNode instanceof MathPower pwr && parent != null) {
            // Exit exponent down to base line
            int nodeIdx = parent.children.indexOf(pwr);
            parent.cursorIndex = Math.min(parent.children.size(), nodeIdx + 1);
            return parent;
        }
        return this;
    }

    /**
     * Vertical navigation: Move cursor Up.
     */
    public MathContainer moveUp() {
        if (enclosingNode instanceof MathFraction frac) {
            if (this == frac.getDenominator()) {
                frac.getNumerator().cursorIndex = Math.min(this.cursorIndex, frac.getNumerator().size());
                return frac.getNumerator();
            }
        }
        if (enclosingNode instanceof MathIntegral integ) {
            if (this == integ.getLowerBound()) {
                integ.getIntegrand().cursorIndex = 0;
                return integ.getIntegrand();
            }
            if (this == integ.getIntegrand()) {
                integ.getUpperBound().cursorIndex = 0;
                return integ.getUpperBound();
            }
        }
        if (enclosingNode instanceof MathSummation sum) {
            if (this == sum.getLowerBound()) {
                sum.getSummand().cursorIndex = 0;
                return sum.getSummand();
            }
            if (this == sum.getSummand()) {
                sum.getUpperBound().cursorIndex = 0;
                return sum.getUpperBound();
            }
        }
        return this;
    }

    @Override
    public String toExpressionString() {
        StringBuilder sb = new StringBuilder();
        for (MathNode child : children) {
            sb.append(child.toExpressionString());
        }
        return sb.toString();
    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        for (MathNode child : children) {
            sb.append(child.toDisplayString());
        }
        return sb.toString();
    }
}
