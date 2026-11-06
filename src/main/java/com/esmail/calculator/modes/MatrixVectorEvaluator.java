package com.esmail.calculator.modes;

import java.util.Locale;

/**
 * Evaluates matrix and vector expressions in MATRIX and VECTOR modes.
 * Calls MatrixEngine and VectorEngine routines backed by x86-64 NASM Assembly.
 */
public final class MatrixVectorEvaluator {

    public static String evaluateMatrix(String expr) {
        if (expr == null || expr.isBlank()) return "[0]";
        String s = expr.trim().replaceAll("\\s+", "");

        // det(MatX)
        if (s.toLowerCase(Locale.ROOT).startsWith("det(") && s.endsWith(")")) {
            String matName = s.substring(4, s.length() - 1).toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            return String.valueOf(m.determinant());
        }

        // Trn(MatX)
        if (s.toLowerCase(Locale.ROOT).startsWith("trn(") && s.endsWith(")")) {
            String matName = s.substring(4, s.length() - 1).toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.transpose();
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // MatX^-1
        if (s.toUpperCase(Locale.ROOT).endsWith("^-1")) {
            String matName = s.substring(0, s.length() - 3).toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.inverse();
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // MatA * MatB or MatA × MatB
        if (s.contains("*") || s.contains("×")) {
            String[] parts = s.split("[*×]");
            MatrixEngine.Matrix m1 = MatrixEngine.getMatrix(parts[0].toUpperCase(Locale.ROOT));
            MatrixEngine.Matrix m2 = MatrixEngine.getMatrix(parts[1].toUpperCase(Locale.ROOT));
            if (m1 == null || m2 == null) throw new IllegalArgumentException("Invalid matrix multiplication operands");
            MatrixEngine.Matrix res = m1.multiply(m2);
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // MatA + MatB
        if (s.contains("+")) {
            String[] parts = s.split("\\+");
            MatrixEngine.Matrix m1 = MatrixEngine.getMatrix(parts[0].toUpperCase(Locale.ROOT));
            MatrixEngine.Matrix m2 = MatrixEngine.getMatrix(parts[1].toUpperCase(Locale.ROOT));
            if (m1 == null || m2 == null) throw new IllegalArgumentException("Invalid matrix addition operands");
            MatrixEngine.Matrix res = m1.add(m2);
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // MatA - MatB
        if (s.contains("-") || s.contains("−")) {
            String[] parts = s.split("[-−]");
            MatrixEngine.Matrix m1 = MatrixEngine.getMatrix(parts[0].toUpperCase(Locale.ROOT));
            MatrixEngine.Matrix m2 = MatrixEngine.getMatrix(parts[1].toUpperCase(Locale.ROOT));
            if (m1 == null || m2 == null) throw new IllegalArgumentException("Invalid matrix subtraction operands");
            MatrixEngine.Matrix res = m1.subtract(m2);
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // Single matrix: MatA, MatB, MatC, MatAns
        MatrixEngine.Matrix single = MatrixEngine.getMatrix(s.toUpperCase(Locale.ROOT));
        if (single != null) {
            MatrixEngine.setMatrix("MATANS", single);
            return single.format();
        }

        throw new IllegalArgumentException("Invalid matrix expression: " + expr);
    }

    public static String evaluateVector(String expr) {
        if (expr == null || expr.isBlank()) return "[0, 0, 0]";
        String s = expr.trim();

        // Abs(VctX)
        if (s.toLowerCase(Locale.ROOT).startsWith("abs(") && s.endsWith(")")) {
            String vctName = s.substring(4, s.length() - 1).trim().toUpperCase(Locale.ROOT);
            VectorEngine.Vector3D v = VectorEngine.getVector(vctName);
            if (v == null) throw new IllegalArgumentException("Unknown vector: " + vctName);
            return String.valueOf(v.norm());
        }

        // VctA dot VctB
        if (s.toLowerCase(Locale.ROOT).contains("dot")) {
            String[] parts = s.toLowerCase(Locale.ROOT).split("dot");
            VectorEngine.Vector3D v1 = VectorEngine.getVector(parts[0].trim().toUpperCase(Locale.ROOT));
            VectorEngine.Vector3D v2 = VectorEngine.getVector(parts[1].trim().toUpperCase(Locale.ROOT));
            if (v1 == null || v2 == null) throw new IllegalArgumentException("Invalid vector dot operands");
            return String.valueOf(v1.dot(v2));
        }

        // VctA * VctB or VctA × VctB (cross product)
        if (s.contains("*") || s.contains("×")) {
            String[] parts = s.split("[*×]");
            VectorEngine.Vector3D v1 = VectorEngine.getVector(parts[0].trim().toUpperCase(Locale.ROOT));
            VectorEngine.Vector3D v2 = VectorEngine.getVector(parts[1].trim().toUpperCase(Locale.ROOT));
            if (v1 == null || v2 == null) throw new IllegalArgumentException("Invalid vector cross operands");
            VectorEngine.Vector3D res = v1.cross(v2);
            VectorEngine.setVector("VCTANS", res);
            return res.format();
        }

        // VctA + VctB
        if (s.contains("+")) {
            String[] parts = s.split("\\+");
            VectorEngine.Vector3D v1 = VectorEngine.getVector(parts[0].trim().toUpperCase(Locale.ROOT));
            VectorEngine.Vector3D v2 = VectorEngine.getVector(parts[1].trim().toUpperCase(Locale.ROOT));
            if (v1 == null || v2 == null) throw new IllegalArgumentException("Invalid vector addition operands");
            VectorEngine.Vector3D res = v1.add(v2);
            VectorEngine.setVector("VCTANS", res);
            return res.format();
        }

        // VctA - VctB
        if (s.contains("-") || s.contains("−")) {
            String[] parts = s.split("[-−]");
            VectorEngine.Vector3D v1 = VectorEngine.getVector(parts[0].trim().toUpperCase(Locale.ROOT));
            VectorEngine.Vector3D v2 = VectorEngine.getVector(parts[1].trim().toUpperCase(Locale.ROOT));
            if (v1 == null || v2 == null) throw new IllegalArgumentException("Invalid vector subtraction operands");
            VectorEngine.Vector3D res = v1.subtract(v2);
            VectorEngine.setVector("VCTANS", res);
            return res.format();
        }

        // Single vector: VctA, VctB, VctC, VctAns
        VectorEngine.Vector3D single = VectorEngine.getVector(s.trim().toUpperCase(Locale.ROOT));
        if (single != null) {
            VectorEngine.setVector("VCTANS", single);
            return single.format();
        }

        throw new IllegalArgumentException("Invalid vector expression: " + expr);
    }
}
