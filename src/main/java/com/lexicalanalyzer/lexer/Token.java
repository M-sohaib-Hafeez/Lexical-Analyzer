package com.lexicalanalyzer.lexer;

import java.util.Objects;

/**
 * One lexical unit: its category, the exact text matched, and the
 * source line it was found on. Errors are represented as a Token too
 * (type == ERROR), with the lexeme holding the human-readable message.
 */
public final class Token {

    private final TokenType type;
    private final String lexeme;
    private final int lineNumber;

    public Token(TokenType type, String lexeme, int lineNumber) {
        this.type = Objects.requireNonNull(type, "type");
        this.lexeme = Objects.requireNonNull(lexeme, "lexeme");
        this.lineNumber = lineNumber;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isError() {
        return type == TokenType.ERROR;
    }

    @Override
    public String toString() {
        return "Token{type=" + type + ", lexeme='" + lexeme + "', line=" + lineNumber + '}';
    }
}
