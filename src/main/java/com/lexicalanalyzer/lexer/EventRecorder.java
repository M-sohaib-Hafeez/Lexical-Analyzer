package com.lexicalanalyzer.lexer;

import java.util.ArrayList;
import java.util.List;

/** A LexerListener that simply records everything it is told, in order. */
public final class EventRecorder implements LexerListener {

    private final List<LexEvent> events = new ArrayList<>();

    @Override
    public void onStateChange(LexerState state, int line) {
        events.add(LexEvent.state(state, line));
    }

    @Override
    public void onToken(Token token) {
        events.add(LexEvent.token(token));
    }

    @Override
    public void onCommentSkipped(int startLine, int endLine) {
        events.add(LexEvent.comment(startLine, endLine));
    }

    public List<LexEvent> getEvents() {
        return events;
    }
}
