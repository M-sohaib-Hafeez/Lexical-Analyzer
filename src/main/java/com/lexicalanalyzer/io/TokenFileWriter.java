package com.lexicalanalyzer.io;

import com.lexicalanalyzer.lexer.Token;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Produces the output file the brief asks for: every token as
 * Token / Lexeme / Line No, followed by a separate list of lexical errors.
 * Used by the UI (Export + auto-save) and by the headless command-line mode.
 */
public final class TokenFileWriter {

    private TokenFileWriter() {
    }

    public static String format(List<Token> tokens) {
        String nl = System.lineSeparator();
        StringBuilder out = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        int tokenCount = 0;
        int errorCount = 0;

        for (Token t : tokens) {
            if (t.isError()) {
                errorCount++;
                errors.append("Line ").append(t.getLineNumber()).append(": ")
                        .append(t.getLexeme()).append(nl);
            } else {
                tokenCount++;
                out.append("Token: ").append(t.getType()).append(nl);
                out.append("Lexeme: ").append(t.getLexeme()).append(nl);
                out.append("Line No: ").append(t.getLineNumber()).append(nl);
                out.append(nl);
            }
        }

        if (errorCount > 0) {
            out.append("=== Lexical Errors ===").append(nl).append(errors).append(nl);
        }
        out.append("Total tokens: ").append(tokenCount)
                .append(", errors: ").append(errorCount).append(nl);
        return out.toString();
    }

    public static void write(Path path, List<Token> tokens) throws IOException {
        Files.writeString(path, format(tokens), StandardCharsets.UTF_8);
    }
}
