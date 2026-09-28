package com.lexicalanalyzer.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hand-written FSM scanner for the CS-3205 Phase 1 grammar.
 *
 * <pre>
 *   id            = (letter | '_') (letter | digit | '_')*     ASCII only
 *   integer       = digit+                                     unsigned only
 *   string        = '"' (char | '\' any)* '"'                  must close on the same line
 *   char literal  = '\'' (one char | '\' any) '\''             exactly ONE character or ONE escape
 *   comment       = slash-star ... star-slash                  may span lines, no nesting, no token
 * </pre>
 *
 * On any lexical error the rest of the current physical line is discarded
 * and scanning resumes on the next line, exactly as the brief asks
 * ("skip the remaining line ... should not generate any more tokens on
 * that line").
 *
 * The brief lists '~' and '#' as invalid characters and defines only block
 * comments, so "#include" lines report "Undefined symbol" and are skipped.
 * That is intentional (see README).
 *
 * A {@link LexerListener} can observe every state change, token and
 * skipped comment, which is how the UI shows the real automaton at work.
 */
public class Lexer {

    static final String ERR_UNTERMINATED_COMMENT = "Unterminated comment";
    static final String ERR_STRING_EXCEEDS_LINE = "String constant exceeds line";
    static final String ERR_CHAR_TOO_LONG = "Char constant too long";
    static final String ERR_EMPTY_CHAR = "Empty char constant";
    static final String ERR_UNDEFINED_SYMBOL = "Undefined symbol '%s'";

    public List<Token> tokenize(String source) {
        return tokenize(source, LexerListener.NONE);
    }

    public List<Token> tokenize(String source, LexerListener listener) {
        return new Scan(source, listener == null ? LexerListener.NONE : listener).run();
    }

    // ---------------------------------------------------------------- helpers

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentifierStart(char c) {
        return isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }

    private static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

    private static boolean isOperatorChar(char c) {
        return "+-*/%=<>!&|".indexOf(c) >= 0;
    }

    private static boolean isPunctuator(char c) {
        return ";,{}()[].".indexOf(c) >= 0;
    }

    private static boolean isTwoCharOperator(String two) {
        return switch (two) {
            case "==", "!=", "<=", ">=", "&&", "||", "++", "--",
                 "+=", "-=", "*=", "/=", "%=" -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------- one scan

    /** State for a single tokenize() call, so Lexer itself stays reusable. */
    private static final class Scan {

        private final SourceReader reader;
        private final LexerListener listener;
        private final List<Token> tokens = new ArrayList<>();

        Scan(String source, LexerListener listener) {
            this.reader = new SourceReader(source);
            this.listener = listener;
        }

        List<Token> run() {
            while (reader.hasNext()) {
                char c = reader.peek().orElseThrow();

                if (Character.isWhitespace(c)) {
                    reader.next();
                } else if (c == '/' && reader.peek(1).filter(n -> n == '*').isPresent()) {
                    scanComment();
                } else if (isIdentifierStart(c)) {
                    scanIdentifier();
                } else if (isDigit(c)) {
                    scanNumber();
                } else if (c == '"') {
                    scanString();
                } else if (c == '\'') {
                    scanChar();
                } else if (isOperatorChar(c)) {
                    scanOperator();
                } else if (isPunctuator(c)) {
                    scanPunctuator();
                } else {
                    // '~', '#', '@', '$', '`', non-ASCII, ... are undefined per the brief.
                    enter(LexerState.ERROR);
                    error(String.format(ERR_UNDEFINED_SYMBOL, c), reader.getLine());
                }
            }
            return tokens;
        }

        // ---- notifications

        private void enter(LexerState state) {
            listener.onStateChange(state, reader.getLine());
        }

        private void emit(Token token) {
            tokens.add(token);
            listener.onToken(token);
            enter(LexerState.START);
        }

        private void error(String message, int line) {
            Token token = new Token(TokenType.ERROR, message, line);
            tokens.add(token);
            listener.onToken(token);
            reader.skipRestOfLine();
            enter(LexerState.START);
        }

        private char nextChar() {
            return reader.next().orElseThrow();
        }

        /** True when there is a next character and it is not a line break. */
        private boolean hasCharOnThisLine() {
            return reader.peek().filter(c -> !isLineBreak(c)).isPresent();
        }

        // ---- scanners

        private void scanIdentifier() {
            enter(LexerState.IN_IDENTIFIER);
            int line = reader.getLine();
            StringBuilder sb = new StringBuilder();
            sb.append(nextChar());
            while (reader.peek().filter(Lexer::isIdentifierPart).isPresent()) {
                sb.append(nextChar());
            }
            String word = sb.toString();
            TokenType type = Keywords.isKeyword(word) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
            emit(new Token(type, word, line));
        }

        private void scanNumber() {
            enter(LexerState.IN_NUMBER);
            int line = reader.getLine();
            StringBuilder sb = new StringBuilder();
            while (reader.peek().filter(Lexer::isDigit).isPresent()) {
                sb.append(nextChar());
            }
            emit(new Token(TokenType.INTEGER_CONSTANT, sb.toString(), line));
        }

        private void scanString() {
            enter(LexerState.IN_STRING);
            int line = reader.getLine();
            StringBuilder sb = new StringBuilder();
            sb.append(nextChar()); // opening quote

            while (true) {
                if (!hasCharOnThisLine()) {
                    enter(LexerState.ERROR);
                    error(ERR_STRING_EXCEEDS_LINE, line);
                    return;
                }
                char c = nextChar();
                sb.append(c);
                if (c == '\\') {
                    // An escape sequence: the next character is part of it (covers \" and \\).
                    if (!hasCharOnThisLine()) {
                        enter(LexerState.ERROR);
                        error(ERR_STRING_EXCEEDS_LINE, line);
                        return;
                    }
                    sb.append(nextChar());
                } else if (c == '"') {
                    emit(new Token(TokenType.STRING_CONSTANT, sb.toString(), line));
                    return;
                }
            }
        }

        private void scanChar() {
            enter(LexerState.IN_CHAR);
            int line = reader.getLine();
            StringBuilder sb = new StringBuilder();
            sb.append(nextChar()); // opening quote

            Optional<Character> first = reader.peek();
            if (first.isPresent() && first.get() == '\'') {
                enter(LexerState.ERROR);
                error(ERR_EMPTY_CHAR, line);
                return;
            }

            if (hasCharOnThisLine()) {
                char c = nextChar();
                sb.append(c);
                if (c == '\\' && hasCharOnThisLine()) {
                    sb.append(nextChar()); // escape sequence such as \n, \t, \\, \'
                }
            }

            if (reader.peek().filter(c -> c == '\'').isPresent()) {
                sb.append(nextChar());
                emit(new Token(TokenType.CHAR_CONSTANT, sb.toString(), line));
                return;
            }

            enter(LexerState.ERROR);
            error(ERR_CHAR_TOO_LONG, line);
        }

        private void scanComment() {
            enter(LexerState.IN_COMMENT);
            int startLine = reader.getLine();
            reader.next(); // '/'
            reader.next(); // '*'

            while (true) {
                if (!reader.hasNext()) {
                    // Unterminated: the comment swallowed the rest of the file,
                    // so there is no "rest of line" to skip to.
                    enter(LexerState.ERROR);
                    error(ERR_UNTERMINATED_COMMENT, startLine);
                    return;
                }
                char c = nextChar();
                if (c == '*' && reader.peek().filter(n -> n == '/').isPresent()) {
                    reader.next();
                    listener.onCommentSkipped(startLine, reader.getLine());
                    enter(LexerState.START);
                    return; // closed cleanly; comments produce no token
                }
            }
        }

        private void scanOperator() {
            enter(LexerState.IN_OPERATOR);
            int line = reader.getLine();
            char first = nextChar();
            StringBuilder sb = new StringBuilder().append(first);

            Optional<Character> second = reader.peek();
            if (second.isPresent() && isTwoCharOperator("" + first + second.get())) {
                sb.append(nextChar());
            }
            emit(new Token(TokenType.OPERATOR, sb.toString(), line));
        }

        private void scanPunctuator() {
            enter(LexerState.IN_OPERATOR);
            int line = reader.getLine();
            emit(new Token(TokenType.PUNCTUATOR, String.valueOf(nextChar()), line));
        }
    }
}
