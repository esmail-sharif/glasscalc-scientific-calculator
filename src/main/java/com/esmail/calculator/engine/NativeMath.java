package com.esmail.calculator.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** JNI declarations for the native NASM backend. The application keeps a Java fallback for development. */
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
    public static native double pow(double base, double exponent);
    public static native double atan(double value);
    public static native double factorial(long value);
}
