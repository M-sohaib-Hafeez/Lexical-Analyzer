package com.lexicalanalyzer.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * Top strip: title, Manual Code / File Upload switch, contextual buttons
 * (Back to Editor, Skip Animation), Run / Export, and a status badge.
 */
public class HeaderBar extends HBox {

    public enum Mode { MANUAL, FILE_UPLOAD }

    private final ToggleButton manualToggle = new ToggleButton("Manual Code");
    private final ToggleButton fileToggle = new ToggleButton("File Upload");
    private final Button backButton = new Button("\u2190 Edit Code");
    private final Button skipButton = new Button("Skip Animation");
    private final Button runButton = new Button("Run Tokenizer");
    private final Button exportButton = new Button("Export Output");
    private final Label statusBadge = new Label("Idle");

    public HeaderBar() {
        getStyleClass().add("header-bar");
        setPadding(new Insets(12, 18, 12, 18));
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Lexical Analyzer");
        title.getStyleClass().add("app-title");

        ToggleGroup modeGroup = new ToggleGroup();
        manualToggle.setToggleGroup(modeGroup);
        fileToggle.setToggleGroup(modeGroup);
        manualToggle.setSelected(true);
        manualToggle.getStyleClass().add("mode-toggle");
        fileToggle.getStyleClass().add("mode-toggle");
        // Never let both toggles end up deselected.
        modeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
            }
        });

        HBox modeBox = new HBox(4, manualToggle, fileToggle);
        modeBox.getStyleClass().add("mode-box");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backButton.getStyleClass().add("export-button");
        skipButton.getStyleClass().add("export-button");
        runButton.getStyleClass().add("run-button");
        exportButton.getStyleClass().add("export-button");
        exportButton.setDisable(true);
        statusBadge.getStyleClass().addAll("status-badge", "status-idle");
        setBackVisible(false);
        setSkipVisible(false);

        getChildren().addAll(title, modeBox, spacer, backButton, skipButton, runButton, exportButton, statusBadge);
    }

    public void setOnRunTokenizer(Runnable handler) {
        runButton.setOnAction(e -> handler.run());
    }

    public void setOnExport(Runnable handler) {
        exportButton.setOnAction(e -> handler.run());
    }

    public void setOnBack(Runnable handler) {
        backButton.setOnAction(e -> handler.run());
    }

    public void setOnSkip(Runnable handler) {
        skipButton.setOnAction(e -> handler.run());
    }

    public void setOnModeChanged(Consumer<Mode> handler) {
        manualToggle.setOnAction(e -> handler.accept(Mode.MANUAL));
        fileToggle.setOnAction(e -> handler.accept(Mode.FILE_UPLOAD));
    }

    /** Reflects a file having been loaded, without firing the mode-change action. */
    public void selectFileUploadMode() {
        fileToggle.setSelected(true);
    }

    public void setRunEnabled(boolean enabled) {
        runButton.setDisable(!enabled);
    }

    public boolean isRunEnabled() {
        return !runButton.isDisable();
    }

    public void fireRun() {
        runButton.fire();
    }

    public void setExportEnabled(boolean enabled) {
        exportButton.setDisable(!enabled);
    }

    public void setBackVisible(boolean visible) {
        backButton.setVisible(visible);
        backButton.setManaged(visible);
    }

    public void setSkipVisible(boolean visible) {
        skipButton.setVisible(visible);
        skipButton.setManaged(visible);
    }

    public void setStatus(String text, String styleClass) {
        statusBadge.setText(text);
        statusBadge.getStyleClass().removeIf(s -> s.startsWith("status-"));
        statusBadge.getStyleClass().add(styleClass);
    }
}
