package com.esmail.calculator.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** JNI declarations for the Windows NASM backend. */
public final class NativeMath {
    private static final boolean AVAILABLE = loadNativeLibrary();

    private NativeMath() { }

    public static boolean isAvailable() { return AVAILABLE; }

    private static boolean loadNativeLibrary() {
        if (Boolean.getBoolean("glasscalc.native.disable")) return false;
        String explicitPath = System.getProperty("glasscalc.native.path");
        if (explicitPath != null && !explicitPath.isBlank()) {
            try {
                System.load(Path.of(explicitPath).toAbsolutePath().toString());
                return true;
            } catch (UnsatisfiedLinkError | RuntimeException ex) {
                System.err.println("Could not load native library from configured path: " + ex.getMessage());
            }
        }

        String resource = "/native/windows-x86_64/glasscalc.dll";
        try (InputStream input = NativeMath.class.getResourceAsStream(resource)) {
            if (input != null) {
                Path extracted = Files.createTempFile("glasscalc-native-", ".dll");
                Files.copy(input, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                extracted.toFile().deleteOnExit();
                System.load(extracted.toAbsolutePath().toString());
                return true;
            }
        } catch (IOException | UnsatisfiedLinkError | RuntimeException ex) {
            System.err.println("Bundled Assembly engine could not be loaded: " + ex.getMessage());
        }

        try {
            System.loadLibrary("glasscalc");
            return true;
        } catch (UnsatisfiedLinkError ex) {
            System.err.println("Assembly DLL not found; using the Java development fallback.");
            return false;
        }
    }

    public static native double add(double a, double b);
    public static native double subtract(double a, double b);
    public static native double multiply(double a, double b);
    public static native double divide(double a, double b);
    public static native double sin(double value);
    public static native double cos(double value);
    public static native double tan(double value);
    public static native double sqrt(double value);
    public static native double ln(double value);
    public static native double log10(double value);
    public static native double log2(double value);
    public static native double exp(double value);
    public static native double exp2(double value);
    public static native double pow(double base, double exponent);
    public static native double atan(double value);
    public static native double atan2(double y, double x);
    public static native double factorial(long value);

    // Vector operations in Assembly
    public static native double vector3dDot(double[] a, double[] b);
    public static native void vector3dCross(double[] a, double[] b, double[] out);
    public static native double vector3dNorm(double[] a);

    // Matrix operations in Assembly
    public static native double matrix2x2Det(double a, double b, double c, double d);
    public static native double matrix3x3Det(double[] m);
    public static native void matrix3x3Multiply(double[] a, double[] b, double[] out);

    // Complex operations in Assembly
    public static native void complexMultiply(double ar, double ai, double br, double bi, double[] out);
    public static native void complexDivide(double ar, double ai, double br, double bi, double[] out);

    // Base-N bitwise logic in Assembly
    public static native long baseAnd(long a, long b);
    public static native long baseOr(long a, long b);
    public static native long baseXor(long a, long b);
    public static native long baseXnor(long a, long b);
    public static native long baseNot(long a);
    public static native long baseNeg(long a);

    // Polynomial / Quadratic in Assembly
    public static native double quadraticDiscriminant(double a, double b, double c);

    // Statistics operations in Assembly
    public static native double statSum(double[] values);
    public static native double statSumSq(double[] values);
    public static native double statMean(double[] values);
    public static native double statStdDev(double[] values, boolean isSample);
    public static native double[] statLinearReg(double[] x, double[] y);

    public static native int verifyUnwindMetadata();
}
