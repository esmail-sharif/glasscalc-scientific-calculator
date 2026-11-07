package com.esmail.calculator.modes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stateful STAT data-entry session, independent of JavaFX controls.
 *
 * <p>Owns the active statistical engine, current X/Y/FREQ cell, unfinished input,
 * and an immutable row snapshot suitable for a future grid editor. A row is only
 * committed after all of its required cells pass validation.</p>
 */
public final class StatEditorSession {
    public record Observation(double x, double y, double frequency) {
        public Observation {
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                throw new ArithmeticException("STAT ERROR: X and Y must be finite numbers");
            }
            if (!Double.isFinite(frequency) || frequency <= 0.0
                    || frequency != Math.rint(frequency)) {
                throw new ArithmeticException("STAT ERROR: Frequency must be a positive integer");
            }
        }
    }

    public record AcceptResult(boolean accepted, String errorMessage, int nextStep) {
        static AcceptResult success(int nextStep) {
            return new AcceptResult(true, null, nextStep);
        }

        static AcceptResult failure(String message, int step) {
            return new AcceptResult(false, message, step);
        }
    }

    private StatEngine engine;
    private boolean frequencyEnabled;
    private int dataStep;
    private double pendingX;
    private double pendingY;
    private String accumulator = "";
    private final List<Observation> rows = new ArrayList<>();

    public StatEditorSession() {
        this(StatEngine.StatType.ONE_VAR);
    }

    public StatEditorSession(StatEngine.StatType type) {
        engine = new StatEngine(Objects.requireNonNull(type, "type"));
    }

    public StatEngine engine() { return engine; }
    public StatEngine.StatType type() { return engine.getType(); }
    public boolean isFrequencyEnabled() { return frequencyEnabled; }
    public int dataStep() { return dataStep; }
    public int rowNumber() { return rows.size() + 1; }
    public double pendingX() { return pendingX; }
    public double pendingY() { return pendingY; }
    public String accumulator() { return accumulator; }
    public List<Observation> rows() { return List.copyOf(rows); }

    public boolean isPaired() {
        return type() != StatEngine.StatType.ONE_VAR;
    }

    public String columnLabel() {
        if (dataStep == 0) return "X";
        if (dataStep == 1 && isPaired()) return "Y";
        return "FREQ";
    }

    public void selectType(StatEngine.StatType type) {
        engine = new StatEngine(Objects.requireNonNull(type, "type"));
        rows.clear();
        resetInput();
    }

    /** Changing the FREQ column format clears data, matching the handheld behavior. */
    public boolean setFrequencyEnabled(boolean enabled) {
        if (frequencyEnabled == enabled) return false;
        frequencyEnabled = enabled;
        engine.clear();
        rows.clear();
        resetInput();
        return true;
    }

    public void clear() {
        engine.clear();
        rows.clear();
        resetInput();
    }

    public void resetInput() {
        dataStep = 0;
        pendingX = 0.0;
        pendingY = 0.0;
        accumulator = "";
    }

    public void append(String text) {
        if (text != null && !text.isEmpty()) accumulator += text;
    }

    public void backspace() {
        if (!accumulator.isEmpty()) {
            accumulator = accumulator.substring(0, accumulator.length() - 1);
        }
    }

    public void toggleSign() {
        accumulator = accumulator.startsWith("-")
                ? accumulator.substring(1) : "-" + accumulator;
    }

    public void appendDecimalPoint() {
        if (!accumulator.contains(".")) {
            accumulator = accumulator.isEmpty() ? "0." : accumulator + ".";
        }
    }

    /**
     * Accepts the current X, Y, or FREQ cell. Blank X/Y cells default to zero;
     * an untouched FREQ cell defaults to one. A failed cell remains editable.
     */
    public AcceptResult acceptCurrentCell() {
        final double value;
        try {
            String raw = accumulator.isBlank()
                    ? (dataStep == 2 ? "1" : "0")
                    : accumulator;
            value = Double.parseDouble(raw);
            if (!Double.isFinite(value)) throw new NumberFormatException("Non-finite input");
        } catch (NumberFormatException invalid) {
            return AcceptResult.failure("Input ERROR", dataStep);
        }

        try {
            if (dataStep == 0) {
                pendingX = value;
                if (isPaired()) {
                    dataStep = 1;
                } else if (frequencyEnabled) {
                    dataStep = 2;
                } else {
                    commitObservation(pendingX, 0.0, 1.0);
                    dataStep = 0;
                }
            } else if (dataStep == 1 && isPaired()) {
                pendingY = value;
                if (frequencyEnabled) {
                    dataStep = 2;
                } else {
                    commitObservation(pendingX, pendingY, 1.0);
                    dataStep = 0;
                }
            } else {
                commitObservation(pendingX, pendingY, value);
                dataStep = 0;
            }
            accumulator = "";
            return AcceptResult.success(dataStep);
        } catch (ArithmeticException invalid) {
            return AcceptResult.failure(
                    invalid.getMessage() == null ? "STAT ERROR" : invalid.getMessage(),
                    dataStep);
        }
    }

    private void commitObservation(double x, double y, double frequency) {
        Observation observation = new Observation(x, y, frequency);
        engine.addPoint(observation.x(), observation.y(), observation.frequency());
        rows.add(observation);
    }

    /** Replaces all rows atomically; invalid input leaves the current data untouched. */
    public void replaceRows(List<Observation> replacementRows) {
        Objects.requireNonNull(replacementRows, "replacementRows");
        StatEngine replacementEngine = new StatEngine(type());
        List<Observation> validated = new ArrayList<>(replacementRows.size());
        for (Observation row : replacementRows) {
            Observation checked = new Observation(row.x(), row.y(), row.frequency());
            replacementEngine.addPoint(checked.x(), checked.y(), checked.frequency());
            validated.add(checked);
        }
        engine = replacementEngine;
        rows.clear();
        rows.addAll(validated);
        resetInput();
    }

    public void updateRow(int index, Observation replacement) {
        if (index < 0 || index >= rows.size()) throw new IndexOutOfBoundsException(index);
        List<Observation> candidate = new ArrayList<>(rows);
        candidate.set(index, Objects.requireNonNull(replacement, "replacement"));
        replaceRows(candidate);
    }

    public void removeRow(int index) {
        if (index < 0 || index >= rows.size()) throw new IndexOutOfBoundsException(index);
        List<Observation> candidate = new ArrayList<>(rows);
        candidate.remove(index);
        replaceRows(candidate);
    }
}
