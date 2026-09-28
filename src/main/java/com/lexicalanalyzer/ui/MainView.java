package com.lexicalanalyzer.ui;

import com.lexicalanalyzer.io.TokenFileWriter;
import com.lexicalanalyzer.lexer.EventRecorder;
import com.lexicalanalyzer.lexer.LexEvent;
import com.lexicalanalyzer.lexer.Lexer;
import com.lexicalanalyzer.lexer.LexerState;
import com.lexicalanalyzer.lexer.Token;
import com.lexicalanalyzer.model.TokenRow;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Top-level layout and wiring. Two views share the window:
 *  - CODE view:    editor (left) + drag-and-drop zone (right)
 *  - RESULTS view: diagnostics dashboard (left) + live token table (right)
 * "Run Tokenizer" switches to RESULTS; the mode toggles / "Edit Code" switch back.
 *
 * The Lexer runs first (instantly, recording every state change and token);
 * the recorded events are then replayed on a timeline so the FSM diagram,
 * console, chart and table update as if watching the scan live.
 */
public class MainView extends BorderPane {

    /** The replay is paced to finish in roughly this many frames, however big the input. */
    private static final int TARGET_FRAMES = 100;
    private static final double BIG_INPUT_FRAME_MS = 40;

    private final HeaderBar headerBar = new HeaderBar();
    private final CodeEditorPanel codeEditor = new CodeEditorPanel();
    private final DiagnosticsPanel diagnostics = new DiagnosticsPanel();
    private final FileIngestionPanel fileIngestion = new FileIngestionPanel();
    private final TokenTablePanel tokenTable = new TokenTablePanel();
    private final StatusBarPanel statusBar = new StatusBarPanel();

    private final StackPane leftStack = new StackPane(codeEditor, diagnostics);
    private final StackPane rightStack = new StackPane(fileIngestion, tokenTable);

    private final Lexer lexer = new Lexer();

    // Results of the latest run + replay progress
    private List<Token> lastTokens = List.of();
    private List<LexEvent> events = List.of();
    private Timeline replayTimeline;
    private int eventIndex;
    private int tokensShown;
    private int errorsShown;
    private long replayStartNanos;
    private double analysisMillis;

    // The file (if any) the editor content came from, for auto-saving output next to it
    private Path loadedFile;
    private String loadedContent;

    public MainView() {
        getStyleClass().add("main-view");

        SplitPane splitPane = new SplitPane(leftStack, rightStack);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.55);

        setTop(headerBar);
        setCenter(splitPane);
        setBottom(statusBar);

        headerBar.setRunEnabled(false);
        codeEditor.getEditorArea().textProperty().addListener((obs, oldV, newV) ->
                headerBar.setRunEnabled(newV != null && !newV.isBlank()));

        wireEvents();
        installGlobalFileDrop();
        showCodeView();

        // Ctrl+Enter = Run Tokenizer
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                        () -> {
                            if (headerBar.isRunEnabled()) {
                                headerBar.fireRun();
                            }
                        });
            }
        });
    }

    // ------------------------------------------------------------------ wiring

    private void wireEvents() {
        headerBar.setOnModeChanged(mode -> {
            showCodeView();
            codeEditor.setEditable(mode == HeaderBar.Mode.MANUAL);
        });
        headerBar.setOnBack(this::showCodeView);
        headerBar.setOnSkip(this::skipReplay);
        headerBar.setOnRunTokenizer(this::runTokenizer);
        headerBar.setOnExport(this::exportTokensToFile);

        fileIngestion.setOnFileLoaded((path, content) -> {
            showCodeView();
            loadedFile = path;
            loadedContent = content;
            codeEditor.setCode(content);
            headerBar.selectFileUploadMode();
            codeEditor.setEditable(false);
            statusBar.setProcessing("Loaded " + path.getFileName());
        });
    }

    /** Lets a file be dropped anywhere in the window, not just on the drop zone. */
    private void installGlobalFileDrop() {
        addEventFilter(DragEvent.DRAG_OVER, ev -> {
            if (ev.getDragboard().hasFiles() && !isInside(fileIngestion, ev.getTarget())) {
                ev.acceptTransferModes(TransferMode.COPY);
            }
        });
        addEventFilter(DragEvent.DRAG_DROPPED, ev -> {
            if (ev.getDragboard().hasFiles() && !isInside(fileIngestion, ev.getTarget())) {
                List<File> files = ev.getDragboard().getFiles();
                boolean ok = !files.isEmpty();
                if (ok) {
                    fileIngestion.loadFile(files.get(0).toPath());
                }
                ev.setDropCompleted(ok);
                ev.consume();
            }
        });
    }

    private static boolean isInside(Node ancestor, Object target) {
        if (!(target instanceof Node)) {
            return false;
        }
        for (Node n = (Node) target; n != null; n = n.getParent()) {
            if (n == ancestor) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------- views

    private void showCodeView() {
        stopReplay();
        codeEditor.setVisible(true);
        codeEditor.setManaged(true);
        fileIngestion.setVisible(true);
        fileIngestion.setManaged(true);
        diagnostics.setVisible(false);
        diagnostics.setManaged(false);
        tokenTable.setVisible(false);
        tokenTable.setManaged(false);
        headerBar.setBackVisible(false);
        headerBar.setSkipVisible(false);
        headerBar.setStatus("Idle", "status-idle");
        statusBar.setOverallStatus("Idle");
    }

    private void showResultsView() {
        codeEditor.setVisible(false);
        codeEditor.setManaged(false);
        fileIngestion.setVisible(false);
        fileIngestion.setManaged(false);
        diagnostics.setVisible(true);
        diagnostics.setManaged(true);
        tokenTable.setVisible(true);
        tokenTable.setManaged(true);
        headerBar.setBackVisible(true);
    }

    // --------------------------------------------------------------------- run

    private void runTokenizer() {
        stopReplay();

        String source = codeEditor.getCode();
        EventRecorder recorder = new EventRecorder();
        long t0 = System.nanoTime();
        lastTokens = lexer.tokenize(source, recorder);
        analysisMillis = (System.nanoTime() - t0) / 1_000_000.0;
        events = recorder.getEvents();

        showResultsView();
        tokenTable.clear();
        diagnostics.reset();
        statusBar.setTokensGenerated(0);
        statusBar.setErrors(0);
        statusBar.setOverallStatus("Active");
        statusBar.setElapsedText("0.0s");
        headerBar.setStatus("Processing", "status-processing");
        headerBar.setExportEnabled(!lastTokens.isEmpty());

        int lines = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                lines++;
            }
        }
        diagnostics.getConsole().log("Input received: " + lines + " line(s), " + source.length() + " character(s)");
        autoSaveOutput(source);

        startReplay();
    }

    /** If the code came straight from a file, also write the required output file next to it. */
    private void autoSaveOutput(String source) {
        if (loadedFile == null || !source.equals(loadedContent)) {
            return;
        }
        String name = loadedFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        Path out = loadedFile.resolveSibling(base + "_tokens.txt");
        try {
            TokenFileWriter.write(out, lastTokens);
            diagnostics.getConsole().log("Output file written: " + out);
        } catch (IOException e) {
            diagnostics.getConsole().log("Could not auto-save output file: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ replay

    private void startReplay() {
        eventIndex = 0;
        tokensShown = 0;
        errorsShown = 0;
        replayStartNanos = System.nanoTime();

        int units = 0;
        for (LexEvent e : events) {
            if (e.kind() != LexEvent.Kind.STATE) {
                units++;
            }
        }
        if (units == 0) {
            finishReplay();
            return;
        }

        int perFrame = Math.max(1, (int) Math.ceil(units / (double) TARGET_FRAMES));
        double frameMs = units <= TARGET_FRAMES
                ? Math.max(25, Math.min(120, 4000.0 / units))
                : BIG_INPUT_FRAME_MS;

        headerBar.setSkipVisible(true);
        replayTimeline = new Timeline(new KeyFrame(Duration.millis(frameMs), e -> advance(perFrame)));
        replayTimeline.setCycleCount(Animation.INDEFINITE);
        replayTimeline.play();
    }

    /** Processes events until {@code units} tokens/comments have been shown. */
    private void advance(int units) {
        int consumed = 0;
        while (eventIndex < events.size() && consumed < units) {
            LexEvent ev = events.get(eventIndex++);
            switch (ev.kind()) {
                case STATE -> diagnostics.getFsmDiagram().setActiveState(ev.state());
                case TOKEN -> {
                    showToken(ev.token());
                    consumed++;
                }
                case COMMENT -> {
                    diagnostics.getConsole().log(String.format(
                            "Comment skipped (lines %d-%d)", ev.line(), ev.endLine()));
                    consumed++;
                }
            }
        }
        tokenTable.scrollToBottom();
        statusBar.setTokensGenerated(tokensShown);
        statusBar.setErrors(errorsShown);
        statusBar.setElapsedText(formatSeconds(System.nanoTime() - replayStartNanos));

        if (eventIndex >= events.size()) {
            finishReplay();
        }
    }

    private void showToken(Token token) {
        tokensShown++;
        tokenTable.getItems().add(new TokenRow(token));
        diagnostics.getChart().addToken(token.getLineNumber());
        statusBar.setProcessing("Processing: " + token.getLexeme());

        if (token.isError()) {
            errorsShown++;
            diagnostics.getConsole().log(String.format(
                    "Error: %s (line %d)", token.getLexeme(), token.getLineNumber()));
        } else {
            diagnostics.getConsole().log(String.format(
                    "Token: %-16s '%s' (line %d)", token.getType(), token.getLexeme(), token.getLineNumber()));
        }
    }

    /** Jump straight to the end of the animation. */
    private void skipReplay() {
        if (replayTimeline != null) {
            advance(Integer.MAX_VALUE);
        }
    }

    private void stopReplay() {
        if (replayTimeline != null) {
            replayTimeline.stop();
            replayTimeline = null;
        }
        headerBar.setSkipVisible(false);
    }

    private void finishReplay() {
        long replayNanos = System.nanoTime() - replayStartNanos;
        stopReplay();
        diagnostics.getFsmDiagram().setActiveState(LexerState.START);

        boolean hadErrors = errorsShown > 0;
        statusBar.setOverallStatus(hadErrors ? "Complete (with errors)" : "Complete");
        statusBar.setProcessing("Tokenization complete");
        statusBar.setTokensGenerated(tokensShown);
        statusBar.setErrors(errorsShown);
        statusBar.setElapsedText(formatSeconds(replayNanos)
                + String.format(Locale.ROOT, " (analysis %.2f ms)", analysisMillis));
        headerBar.setStatus(hadErrors ? "Complete*" : "Complete", "status-complete");
        diagnostics.getConsole().log(String.format(Locale.ROOT,
                "Final Stats: %d token(s) incl. %d error(s), analysis time %.2f ms",
                tokensShown, errorsShown, analysisMillis));
    }

    private static String formatSeconds(long nanos) {
        return String.format(Locale.ROOT, "%.1fs", nanos / 1_000_000_000.0);
    }

    // ------------------------------------------------------------------ export

    /** Writes the required "Token / Lexeme / Line No" output file, per the brief. */
    private void exportTokensToFile() {
        if (lastTokens.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Run the tokenizer first -- there's nothing to export yet.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save token output");
        chooser.setInitialFileName("output.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        if (loadedFile != null && loadedFile.getParent() != null && loadedFile.getParent().toFile().isDirectory()) {
            chooser.setInitialDirectory(loadedFile.getParent().toFile());
        }
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            TokenFileWriter.write(file.toPath(), lastTokens);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not save file: " + e.getMessage()).showAndWait();
        }
    }
}
