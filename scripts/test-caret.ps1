$jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$m2fx = "$env:USERPROFILE\.m2\repository\org\openjfx"
$jars = @(
    "$m2fx\javafx-base\21.0.13\javafx-base-21.0.13-win.jar",
    "$m2fx\javafx-base\21.0.13\javafx-base-21.0.13.jar",
    "$m2fx\javafx-graphics\21.0.13\javafx-graphics-21.0.13-win.jar",
    "$m2fx\javafx-graphics\21.0.13\javafx-graphics-21.0.13.jar",
    "$m2fx\javafx-controls\21.0.13\javafx-controls-21.0.13-win.jar",
    "$m2fx\javafx-controls\21.0.13\javafx-controls-21.0.13.jar",
    "$m2fx\javafx-swing\21.0.13\javafx-swing-21.0.13-win.jar",
    "$m2fx\javafx-swing\21.0.13\javafx-swing-21.0.13.jar"
)
$mp = ($jars | Where-Object { Test-Path $_ }) -join ';'
& "$jdk\bin\javac.exe" -cp "target/classes;$mp" -d target/test-classes src/test/java/com/esmail/calculator/CaretTest.java
& "$jdk\bin\java.exe" --module-path $mp --add-modules javafx.controls -cp "target/classes;target/test-classes" com.esmail.calculator.CaretTest
