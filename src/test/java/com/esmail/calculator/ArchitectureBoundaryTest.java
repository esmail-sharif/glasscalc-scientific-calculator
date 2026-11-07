package com.esmail.calculator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureBoundaryTest {
    @Test
    void engineAndModePackagesMustRemainIndependentOfJavaFx() throws IOException {
        for (String packagePath : List.of(
                "src/main/java/com/esmail/calculator/engine",
                "src/main/java/com/esmail/calculator/modes")) {
            Path root = Path.of(packagePath);
            assertTrue(Files.isDirectory(root), "Expected source package: " + packagePath);

            try (Stream<Path> files = Files.walk(root)) {
                List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
                assertFalse(javaFiles.isEmpty(), "Expected Java sources in " + packagePath);
                for (Path file : javaFiles) {
                    String source = Files.readString(file, StandardCharsets.UTF_8);
                    assertFalse(source.contains("import javafx."),
                            () -> "Domain/engine source must not depend on JavaFX: " + file);
                }
            }
        }
    }
}
