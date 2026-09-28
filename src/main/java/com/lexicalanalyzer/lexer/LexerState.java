package com.lexicalanalyzer.lexer;

/**
 * The finite states of the scanning automaton. These mirror the nodes
 * FsmDiagramView draws and highlights in the UI while tokens replay,
 * so this enum is effectively shared vocabulary between the backend
 * and the diagnostic dashboard.
 */
public enum LexerState {
    START,
    IN_IDENTIFIER,
    IN_NUMBER,
    IN_STRING,
    IN_CHAR,
    IN_COMMENT,
    IN_OPERATOR,
    ERROR
}
