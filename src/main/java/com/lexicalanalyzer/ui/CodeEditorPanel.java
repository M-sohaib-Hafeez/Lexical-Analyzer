package com.lexicalanalyzer.ui;

import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * A plain-text code editor with a line-number gutter. The gutter is a
 * second, read-only TextArea with identical font/padding (see the CSS) whose
 * vertical scroll position simply mirrors the editor's.
 */
public class CodeEditorPanel extends HBox {

    private final TextArea lineNumbers = new TextArea("1");
    private final TextArea editor = new TextArea();

    public CodeEditorPanel() {
        getStyleClass().add("code-editor");

        lineNumbers.setEditable(false);
        lineNumbers.setFocusTraversable(false);
        lineNumbers.setMouseTransparent(true); // only the editor drives scrolling
        lineNumbers.getStyleClass().add("line-number-gutter");
        lineNumbers.setPrefWidth(56);
        lineNumbers.setMinWidth(56);
        lineNumbers.setMaxWidth(56);
        lineNumbers.setWrapText(false);

        editor.getStyleClass().add("code-text-area");
        editor.setWrapText(false);
        editor.setPromptText("Type or paste C source code here...");

        HBox.setHgrow(editor, Priority.ALWAYS);
        getChildren().addAll(lineNumbers, editor);

        editor.textProperty().addListener((obs, oldText, newText) -> refreshLineNumbers(newText));
        editor.scrollTopProperty().addListener((obs, oldV, newV) -> lineNumbers.setScrollTop(newV.doubleValue()));
    }

    private void refreshLineNumbers(String text) {
        int lineCount = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineCount++;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            sb.append(i).append('\n');
        }
        // Two spare lines so the gutter can scroll as far as the editor
        // (which loses height to its horizontal scrollbar on long lines).
        sb.append("\n\n");
        lineNumbers.setText(sb.toString());
        lineNumbers.setScrollTop(editor.getScrollTop());
    }

    public String getCode() {
        return editor.getText();
    }

    public void setCode(String code) {
        editor.setText(code);
    }

    public void setEditable(boolean editable) {
        editor.setEditable(editable);
    }

    public TextArea getEditorArea() {
        return editor;
    }
}
