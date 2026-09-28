package com.lexicalanalyzer.ui;

import com.lexicalanalyzer.io.FileLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * Drag-and-drop / Browse... zone for loading a .c or .txt source file.
 * Reports the loaded (path, content) pair back to whoever is listening;
 * MainView pipes the content into the shared code editor.
 */
public class FileIngestionPanel extends VBox {

    private final Label headline = new Label("Drag & Drop File");
    private final Label subtext = new Label("or use Browse... to pick a .c / .txt file");
    private final Button browseButton = new Button("Browse...");
    private final Label statusLabel = new Label("Awaiting input");

    private BiConsumer<Path, String> onFileLoaded;

    public FileIngestionPanel() {
        getStyleClass().add("file-ingestion-panel");
        setAlignment(Pos.CENTER);
        setSpacing(12);
        setPadding(new Insets(24));

        headline.getStyleClass().add("ingestion-headline");
        subtext.getStyleClass().add("ingestion-subtext");
        statusLabel.getStyleClass().add("ingestion-status");
        browseButton.getStyleClass().add("browse-button");

        browseButton.setOnAction(e -> browseForFile());

        getChildren().addAll(headline, subtext, browseButton, statusLabel);

        setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                if (!getStyleClass().contains("drag-over")) {
                    getStyleClass().add("drag-over");
                }
            }
            event.consume();
        });
        setOnDragExited(event -> getStyleClass().remove("drag-over"));
        setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                loadFile(db.getFiles().get(0).toPath());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void browseForFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select C source file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("C source / text files", "*.c", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        var file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            loadFile(file.toPath());
        }
    }

    /** Public so MainView can also accept files dropped anywhere in the window. */
    public void loadFile(Path path) {
        try {
            String content = FileLoader.read(path);
            statusLabel.setText("Loaded: " + path.getFileName());
            if (onFileLoaded != null) {
                onFileLoaded.accept(path, content);
            }
        } catch (IOException e) {
            statusLabel.setText("Failed to read file: " + e.getMessage());
        }
    }

    public void setOnFileLoaded(BiConsumer<Path, String> callback) {
        this.onFileLoaded = callback;
    }

    public void reset() {
        statusLabel.setText("Awaiting input");
    }
}
