package com.esmail.calculator.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded-lifetime calculation history and REPLAY cursor.
 *
 * The UI owns rendering; this class owns entry ordering and navigation rules.
 * A newly calculated value becomes the current entry. A seeded showcase entry
 * starts with the cursor at -1 so the first REPLAY-Up opens the latest entry.
 */
public final class CalculationHistory {
    public record Entry(String expression, double result) {
        public Entry {
            Objects.requireNonNull(expression, "expression");
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int cursor = -1;

    /** Adds an initial/example entry without selecting it in the replay cursor. */
    public void seed(String expression, double result) {
        entries.add(new Entry(expression, result));
        cursor = -1;
    }

    /** Records a fresh calculation and selects its entry as the current cursor. */
    public void record(String expression, double result) {
        entries.add(new Entry(expression, result));
        cursor = entries.size() - 1;
    }

    /** Selects the previous entry, or the latest entry when the cursor is idle (-1). */
    public Entry previous() {
        if (entries.isEmpty()) return null;
        if (cursor == -1) {
            cursor = entries.size() - 1;
        } else if (cursor > 0) {
            cursor--;
        } else {
            return null;
        }
        return entries.get(cursor);
    }

    /**
     * Selects the next entry. Moving past the newest entry returns null and
     * resets the cursor to idle, signalling the UI to clear the replay display.
     */
    public Entry next() {
        if (entries.isEmpty()) return null;
        if (cursor >= 0 && cursor < entries.size() - 1) {
            cursor++;
            return entries.get(cursor);
        }
        cursor = -1;
        return null;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public int cursor() {
        return cursor;
    }

    public boolean canPrevious() {
        return !entries.isEmpty() && (cursor == -1 || cursor > 0);
    }

    public boolean canNext() {
        return cursor >= 0 && cursor < entries.size() - 1;
    }
}
