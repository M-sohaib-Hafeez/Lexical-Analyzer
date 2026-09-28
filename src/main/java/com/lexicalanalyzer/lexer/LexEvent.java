package com.lexicalanalyzer.lexer;

/**
 * One recorded step of a scan: a state change, a produced token, or a
 * skipped comment. The UI replays a list of these to animate the run.
 */
public record LexEvent(Kind kind, LexerState state, Token token, int line, int endLine) {

    public enum Kind { STATE, TOKEN, COMMENT }

    public static LexEvent state(LexerState state, int line) {
        return new LexEvent(Kind.STATE, state, null, line, line);
    }

    public static LexEvent token(Token token) {
        return new LexEvent(Kind.TOKEN, null, token, token.getLineNumber(), token.getLineNumber());
    }

    public static LexEvent comment(int startLine, int endLine) {
        return new LexEvent(Kind.COMMENT, LexerState.IN_COMMENT, null, startLine, endLine);
    }
}
