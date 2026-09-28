package com.lexicalanalyzer.lexer;

/**
 * Observer the Lexer notifies while it scans. This is what makes the UI's
 * FSM diagram show the scanner's REAL state instead of guessing it from
 * finished tokens. All callbacks are optional (default no-ops).
 */
public interface LexerListener {

    /** A listener that ignores everything. */
    LexerListener NONE = new LexerListener() { };

    /** The automaton moved into {@code state} while reading {@code line}. */
    default void onStateChange(LexerState state, int line) {
    }

    /** A token (or an ERROR token) was produced. */
    default void onToken(Token token) {
    }

    /** A complete block comment was consumed (comments produce no token). */
    default void onCommentSkipped(int startLine, int endLine) {
    }
}
