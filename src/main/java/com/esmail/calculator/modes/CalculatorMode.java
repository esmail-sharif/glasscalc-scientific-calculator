package com.esmail.calculator.modes;

/**
 * Official CASIO fx-991ES PLUS / fx-570ES PLUS calculation modes (MODE 1 to 8).
 */
public enum CalculatorMode {
    COMP(1, "COMP", "General calculations"),
    CMPLX(2, "CMPLX", "Complex number calculations"),
    STAT(3, "STAT", "Statistical and regression calculations"),
    BASE_N(4, "BASE-N", "Binary, octal, decimal, hexadecimal calculations"),
    EQN(5, "EQN", "Equation solution calculations"),
    MATRIX(6, "MATRIX", "Matrix calculations"),
    TABLE(7, "TABLE", "Function value numerical table generation"),
    VECTOR(8, "VECTOR", "Vector calculations");

    private final int number;
    private final String label;
    private final String description;

    CalculatorMode(int number, String label, String description) {
        this.number = number;
        this.label = label;
        this.description = description;
    }

    public int getNumber() { return number; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }

    public String badge() {
        return switch (this) {
            case COMP -> "";
            case CMPLX -> "CMPLX";
            case STAT -> "STAT";
            case BASE_N -> "BASE";
            case EQN -> "EQN";
            case MATRIX -> "MAT";
            case TABLE -> "TABLE";
            case VECTOR -> "VCT";
        };
    }
}
