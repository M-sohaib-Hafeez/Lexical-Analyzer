package com.lexicalanalyzer.lexer;

import java.util.Optional;

/**
 * Thin cursor over the raw source text, in the same spirit as the
 * {@code Expression} helper from the arithmetic-lexer lab (Optional-based
 * next()/hasNext()), extended with one-character lookahead and line
 * tracking, which the C grammar needs for multi-char operators, block
 * comments, and per-line error recovery.
 */
public final class SourceReader {

    private final String source;
    private int index = 0;
    private int line = 1;

    public SourceReader(String source) {
        this.source = source != null ? source : "";
    }

    public boolean hasNext() {
        return index < source.length();
    }

    public Optional<Character> next() {
        if (!hasNext()) {
            return Optional.empty();
        }
        char c = source.charAt(index++);
        if (c == '\n') {
            line++;
        }
        return Optional.of(c);
    }

    /** Looks at the current character without consuming it. */
    public Optional<Character> peek() {
        return peek(0);
    }

    /** Looks {@code offset} characters ahead without consuming anything. */
    public Optional<Character> peek(int offset) {
        int at = index + offset;
        if (at < 0 || at >= source.length()) {
            return Optional.empty();
        }
        return Optional.of(source.charAt(at));
    }

    public int getLine() {
        return line;
    }

    /** Discards the remainder of the current physical line (error recovery). */
    public void skipRestOfLine() {
        while (hasNext() && peek().filter(c -> c != '\n').isPresent()) {
            next();
        }
    }
}
