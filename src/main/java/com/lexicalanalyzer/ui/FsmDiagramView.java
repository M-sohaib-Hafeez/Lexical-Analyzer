package com.lexicalanalyzer.ui;

import com.lexicalanalyzer.lexer.LexerState;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.EnumMap;
import java.util.Map;

/**
 * Map of the scanner's states with the CURRENT state highlighted. The
 * states come straight from the Lexer (via LexerListener), so this shows
 * what the automaton is really doing. Resizes with its container.
 */
public class FsmDiagramView extends Pane {

    private final Canvas canvas = new Canvas();
    private final Map<LexerState, Point2D> nodePositions = new EnumMap<>(LexerState.class);
    private LexerState activeState = LexerState.START;

    public FsmDiagramView() {
        setMinHeight(150);
        setPrefHeight(220);
        getChildren().add(canvas);
    }

    public void setActiveState(LexerState state) {
        if (state != activeState) {
            this.activeState = state;
            draw();
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        canvas.setWidth(Math.max(getWidth(), 1));
        canvas.setHeight(Math.max(getHeight(), 1));
        layoutNodes();
        draw();
    }

    private void layoutNodes() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        nodePositions.clear();
        nodePositions.put(LexerState.START, new Point2D(w * 0.50, h * 0.17));
        nodePositions.put(LexerState.ERROR, new Point2D(w * 0.14, h * 0.17));
        nodePositions.put(LexerState.IN_COMMENT, new Point2D(w * 0.86, h * 0.17));
        nodePositions.put(LexerState.IN_IDENTIFIER, new Point2D(w * 0.14, h * 0.52));
        nodePositions.put(LexerState.IN_OPERATOR, new Point2D(w * 0.50, h * 0.52));
        nodePositions.put(LexerState.IN_CHAR, new Point2D(w * 0.86, h * 0.52));
        nodePositions.put(LexerState.IN_NUMBER, new Point2D(w * 0.32, h * 0.86));
        nodePositions.put(LexerState.IN_STRING, new Point2D(w * 0.68, h * 0.86));
    }

    private double radius(boolean active) {
        double base = Math.max(14, Math.min(24, Math.min(canvas.getWidth(), canvas.getHeight()) * 0.09));
        return active ? base * 1.25 : base;
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Point2D start = nodePositions.get(LexerState.START);
        if (start == null) {
            return;
        }

        // Edges out of START; the one leading to the active state lights up.
        for (var entry : nodePositions.entrySet()) {
            if (entry.getKey() == LexerState.START) {
                continue;
            }
            boolean lit = entry.getKey() == activeState;
            gc.setStroke(lit ? Color.web("#22d3ee") : Color.web("#2c3a4a"));
            gc.setLineWidth(lit ? 2.5 : 1.2);
            Point2D p = entry.getValue();
            gc.strokeLine(start.getX(), start.getY(), p.getX(), p.getY());
        }

        for (var entry : nodePositions.entrySet()) {
            drawNode(gc, entry.getKey(), entry.getValue());
        }
    }

    private void drawNode(GraphicsContext gc, LexerState state, Point2D p) {
        boolean active = state == activeState;
        double r = radius(active);
        boolean error = state == LexerState.ERROR;

        if (active) {
            gc.setFill(error ? Color.web("#f87171", 0.25) : Color.web("#22d3ee", 0.25));
            gc.fillOval(p.getX() - r - 6, p.getY() - r - 6, (r + 6) * 2, (r + 6) * 2);
        }

        gc.setFill(active ? (error ? Color.web("#f87171") : Color.web("#22d3ee")) : Color.web("#1e293b"));
        gc.setStroke(error ? Color.web("#f87171") : Color.web("#38bdf8"));
        gc.setLineWidth(1.5);
        gc.fillOval(p.getX() - r, p.getY() - r, r * 2, r * 2);
        gc.strokeOval(p.getX() - r, p.getY() - r, r * 2, r * 2);

        gc.setFill(active ? Color.web("#052e33") : Color.web("#cbd5e1"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, Math.max(8, r * 0.42)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(shortLabel(state), p.getX(), p.getY());
    }

    private String shortLabel(LexerState state) {
        return switch (state) {
            case START -> "START";
            case IN_IDENTIFIER -> "ID";
            case IN_NUMBER -> "NUM";
            case IN_STRING -> "STR";
            case IN_CHAR -> "CHAR";
            case IN_OPERATOR -> "OP";
            case IN_COMMENT -> "CMT";
            case ERROR -> "ERR";
        };
    }
}
