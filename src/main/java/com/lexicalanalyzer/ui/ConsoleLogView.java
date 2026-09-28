package com.lexicalanalyzer.ui;

import javafx.scene.control.TextArea;

/** Terminal-style live log of tokens, errors, and final stats (auto-scrolls to the newest line). */
public class ConsoleLogView extends TextArea {

    public ConsoleLogView() {
        setEditable(false);
        setWrapText(false);
        setFocusTraversable(false);
        getStyleClass().add("console-log");
    }

    public void log(String line) {
        appendText(line + "\n");
        setScrollTop(Double.MAX_VALUE);
    }

    public void clearLog() {
        clear();
    }
}
