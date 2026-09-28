package com.lexicalanalyzer.ui;

import com.lexicalanalyzer.lexer.LexerState;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Left-panel view shown while/after "Run Tokenizer": the live console log,
 * the FSM state diagram, and the tokens-per-line chart.
 */
public class DiagnosticsPanel extends VBox {

    private final ConsoleLogView console = new ConsoleLogView();
    private final FsmDiagramView fsmDiagram = new FsmDiagramView();
    private final TokensPerLineChartView chart = new TokensPerLineChartView();

    public DiagnosticsPanel() {
        getStyleClass().add("diagnostics-panel");
        setSpacing(10);
        setPadding(new Insets(12));

        VBox consoleBox = new VBox(6, sectionLabel("Live Analysis Console"), console);
        VBox.setVgrow(console, Priority.ALWAYS);

        VBox fsmBox = new VBox(6, sectionLabel("FSM State Diagram"), fsmDiagram);
        VBox.setVgrow(fsmDiagram, Priority.ALWAYS);

        VBox chartBox = new VBox(6, sectionLabel("Tokens per Source Line"), chart);
        VBox.setVgrow(chart, Priority.ALWAYS);

        SplitPane rightSplit = new SplitPane(fsmBox, chartBox);
        rightSplit.setOrientation(Orientation.VERTICAL);
        rightSplit.setDividerPositions(0.5);

        SplitPane mainSplit = new SplitPane(consoleBox, rightSplit);
        mainSplit.setDividerPositions(0.58);

        getChildren().add(mainSplit);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-label");
        return label;
    }

    public ConsoleLogView getConsole() {
        return console;
    }

    public FsmDiagramView getFsmDiagram() {
        return fsmDiagram;
    }

    public TokensPerLineChartView getChart() {
        return chart;
    }

    public void reset() {
        console.clearLog();
        chart.reset();
        fsmDiagram.setActiveState(LexerState.START);
    }
}
