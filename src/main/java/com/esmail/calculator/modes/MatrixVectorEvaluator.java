package com.esmail.calculator.modes;

import com.esmail.calculator.engine.CasioFormatter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates matrix and vector expressions in MATRIX and VECTOR modes.
 * Calls MatrixEngine and VectorEngine routines backed by x86-64 NASM Assembly.
 */
public final class MatrixVectorEvaluator {

    private static final Pattern SCALAR_MAT_PREFIX = Pattern.compile("^([+-]?[0-9]+(?:\\.[0-9]+)?)[*×]?(MAT[A-C]|MATANS)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCALAR_MAT_SUFFIX = Pattern.compile("^(MAT[A-C]|MATANS)[*×]([+-]?[0-9]+(?:\\.[0-9]+)?)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern SCALAR_VCT_PREFIX = Pattern.compile("^([+-]?[0-9]+(?:\\.[0-9]+)?)[*×]?(VCT[A-C]|VCTANS)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCALAR_VCT_SUFFIX = Pattern.compile("^(VCT[A-C]|VCTANS)[*×]([+-]?[0-9]+(?:\\.[0-9]+)?)$", Pattern.CASE_INSENSITIVE);

    public static String evaluateMatrix(String expr) {
        if (expr == null || expr.isBlank()) return "[0]";
        String s = expr.trim().replaceAll("\\s+", "");

        // Abs(MatX) -> Determinant
        if (s.toLowerCase(Locale.ROOT).startsWith("abs(") && s.endsWith(")")) {
            String inner = s.substring(4, s.length() - 1).trim();
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(inner.toUpperCase(Locale.ROOT));
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + inner);
            return CasioFormatter.toExactOrFraction(m.determinant());
        }

        // det(MatX)
        if (s.toLowerCase(Locale.ROOT).startsWith("det(") && s.endsWith(")")) {
            String matName = s.substring(4, s.length() - 1).trim().toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            return CasioFormatter.toExactOrFraction(m.determinant());
        }

        // Trn(MatX)
        if (s.toLowerCase(Locale.ROOT).startsWith("trn(") && s.endsWith(")")) {
            String matName = s.substring(4, s.length() - 1).trim().toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.transpose();
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // MatX^2 or MatX²
        if (s.toUpperCase(Locale.ROOT).endsWith("^2") || s.endsWith("²")) {
            int cut = s.endsWith("²") ? 1 : 2;
            String matName = s.substring(0, s.length() - cut).toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.multiply(m);
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // MatX^3 or MatX³
        if (s.toUpperCase(Locale.ROOT).endsWith("^3") || s.endsWith("³")) {
            int cut = s.endsWith("³") ? 1 : 2;
            String matName = s.substring(0, s.length() - cut).toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.multiply(m).multiply(m);
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // MatX^-1 or MatX⁻¹
        if (s.toUpperCase(Locale.ROOT).endsWith("^-1") || s.endsWith("⁻¹")) {
            int cut = s.endsWith("⁻¹") ? 2 : 3;
            String matName = s.substring(0, s.length() - cut).toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.inverse();
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }

        // Scalar multiplication (e.g. 2MatA, 2*MatA, MatA*2)
        Matcher smPre = SCALAR_MAT_PREFIX.matcher(s);
        if (smPre.matches()) {
            double scalar = Double.parseDouble(smPre.group(1));
            String matName = smPre.group(2).toUpperCase(Locale.ROOT);
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.scale(scalar);
            MatrixEngine.setMatrix("MATANS", res);
            return res.format();
        }
        Matcher smSuf = SCALAR_MAT_SUFFIX.matcher(s);
        if (smSuf.matches()) {
            String matName = smSuf.group(1).toUpperCase(Locale.ROOT);
            double scalar = Double.parseDouble(smSuf.group(2));
            MatrixEngine.Matrix m = MatrixEngine.getMatrix(matName);
            if (m == null) throw new IllegalArgumentException("Unknown matrix: " + matName);
            MatrixEngine.Matrix res = m.scale(scalar);
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
            return CasioFormatter.toExactOrFraction(v.norm());
        }

        // Angle(VctA, VctB)
        if (s.toLowerCase(Locale.ROOT).startsWith("angle(") && s.endsWith(")")) {
            String inside = s.substring(6, s.length() - 1);
            String[] parts = inside.split(",");
            if (parts.length == 2) {
                VectorEngine.Vector3D v1 = VectorEngine.getVector(parts[0].trim().toUpperCase(Locale.ROOT));
                VectorEngine.Vector3D v2 = VectorEngine.getVector(parts[1].trim().toUpperCase(Locale.ROOT));
                if (v1 != null && v2 != null) {
                    double cosVal = v1.dot(v2) / (v1.norm() * v2.norm());
                    cosVal = Math.max(-1.0, Math.min(1.0, cosVal));
                    double rad = Math.acos(cosVal);
                    return CasioFormatter.toExactOrFraction(Math.toDegrees(rad));
                }
            }
        }

        // Scalar multiplication (e.g. 2VctA, 2*VctA, VctA*2)
        String noSpace = s.replaceAll("\\s+", "");
        Matcher svPre = SCALAR_VCT_PREFIX.matcher(noSpace);
        if (svPre.matches()) {
            double scalar = Double.parseDouble(svPre.group(1));
            String vctName = svPre.group(2).toUpperCase(Locale.ROOT);
            VectorEngine.Vector3D v = VectorEngine.getVector(vctName);
            if (v == null) throw new IllegalArgumentException("Unknown vector: " + vctName);
            VectorEngine.Vector3D res = v.scale(scalar);
            VectorEngine.setVector("VCTANS", res);
            return res.format();
        }
        Matcher svSuf = SCALAR_VCT_SUFFIX.matcher(noSpace);
        if (svSuf.matches()) {
            String vctName = svSuf.group(1).toUpperCase(Locale.ROOT);
            double scalar = Double.parseDouble(svSuf.group(2));
            VectorEngine.Vector3D v = VectorEngine.getVector(vctName);
            if (v == null) throw new IllegalArgumentException("Unknown vector: " + vctName);
            VectorEngine.Vector3D res = v.scale(scalar);
            VectorEngine.setVector("VCTANS", res);
            return res.format();
        }

        // VctA dot VctB
        if (s.toLowerCase(Locale.ROOT).contains("dot")) {
            String[] parts = s.toLowerCase(Locale.ROOT).split("dot");
            VectorEngine.Vector3D v1 = VectorEngine.getVector(parts[0].trim().toUpperCase(Locale.ROOT));
            VectorEngine.Vector3D v2 = VectorEngine.getVector(parts[1].trim().toUpperCase(Locale.ROOT));
            if (v1 == null || v2 == null) throw new IllegalArgumentException("Invalid vector dot operands");
            return CasioFormatter.toExactOrFraction(v1.dot(v2));
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
