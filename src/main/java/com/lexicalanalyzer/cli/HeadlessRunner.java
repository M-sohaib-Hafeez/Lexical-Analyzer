package com.lexicalanalyzer.cli;

import com.lexicalanalyzer.io.FileLoader;
import com.lexicalanalyzer.io.TokenFileWriter;
import com.lexicalanalyzer.lexer.Lexer;
import com.lexicalanalyzer.lexer.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * No-GUI mode, matching the brief literally: input text file in, output
 * text file out.   Usage:  Main &lt;input-file&gt; [output-file]
 */
public final class HeadlessRunner {

    private HeadlessRunner() {
    }

    /** @return process exit code (0 ok, 1 I/O problem, 2 bad usage) */
    public static int run(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: Main <input-file> [output-file]   (no arguments = open the GUI)");
            return 2;
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args.length == 2 ? args[1] : "output.txt");

        if (!Files.isReadable(input)) {
            System.err.println("Cannot read input file: " + input.toAbsolutePath());
            return 1;
        }
        try {
            List<Token> tokens = new Lexer().tokenize(FileLoader.read(input));
            TokenFileWriter.write(output, tokens);
            long errors = tokens.stream().filter(Token::isError).count();
            System.out.println((tokens.size() - errors) + " token(s), " + errors
                    + " error(s) -> " + output.toAbsolutePath());
            return 0;
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            return 1;
        }
    }
}
