package com.esmail.calculator;

import javafx.application.Platform;
import javafx.scene.control.TextField;

public class CaretTest {
    public static void main(String[] args) {
        Platform.startup(() -> {
            TextField tf = new TextField();
            
            System.out.println("--- 1. Initial State ---");
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- 2. Insert 'cos(' ---");
            tf.insertText(tf.getCaretPosition(), "cos(");
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- 3. Insert '5' ---");
            tf.insertText(tf.getCaretPosition(), "5");
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- 4. Insert '4' ---");
            tf.insertText(tf.getCaretPosition(), "4");
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- 5. deletePreviousChar ---");
            tf.deletePreviousChar();
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- 6. Insert '4' again ---");
            tf.insertText(tf.getCaretPosition(), "4");
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- 7. Backward (cursorLeft) ---");
            tf.backward();
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- 8. Insert '9' at position ---");
            tf.insertText(tf.getCaretPosition(), "9");
            System.out.println("Text: '" + tf.getText() + "', caret: " + tf.getCaretPosition());

            System.out.println("--- Done! ---");
            System.exit(0);
        });
    }
}
