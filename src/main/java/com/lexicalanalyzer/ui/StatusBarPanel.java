package com.lexicalanalyzer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;

/** Bottom status strip: overall state, live processing note, and running counters. */
public class StatusBarPanel extends HBox {

    private final Label overallStatus = new Label("Overall Status: Idle");
    private final Label processingLabel = new Label("Real-time Analysis Console");
    private final Label tokensLabel = new Label("Tokens Generated: 0");
    private final Label errorsLabel = new Label("Errors: 0");
    private final Label elapsedLabel = new Label("Elapsed Time: 0.0s");

    public StatusBarPanel() {
        getStyleClass().add("status-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setPadding(new Insets(8, 18, 8, 18));

        for (Label l : new Label[]{overallStatus, processingLabel, tokensLabel, errorsLabel, elapsedLabel}) {
            l.getStyleClass().add("status-item");
        }

        getChildren().addAll(
                overallStatus, new Separator(Orientation.VERTICAL),
                processingLabel, new Separator(Orientation.VERTICAL),
                tokensLabel, new Separator(Orientation.VERTICAL),
                errorsLabel, new Separator(Orientation.VERTICAL),
                elapsedLabel);
    }

    public void setOverallStatus(String text) {
        overallStatus.setText("Overall Status: " + text);
    }

    public void setProcessing(String text) {
        processingLabel.setText(text);
    }

    public void setTokensGenerated(int count) {
        tokensLabel.setText("Tokens Generated: " + count);
    }

    public void setErrors(int count) {
        errorsLabel.setText("Errors: " + count);
    }

    public void setElapsedText(String text) {
        elapsedLabel.setText("Elapsed Time: " + text);
    }
}
