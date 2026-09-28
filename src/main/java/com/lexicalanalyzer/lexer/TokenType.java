package com.lexicalanalyzer.lexer;

/**
 * Every category of lexical unit this analyzer can recognize.
 * ERROR is included deliberately so lexical errors flow through the
 * same pipeline (table, console, export file) as valid tokens instead
 * of needing a parallel error-reporting path.
 */
public enum TokenType {
    KEYWORD,
    IDENTIFIER,
    INTEGER_CONSTANT,
    STRING_CONSTANT,
    CHAR_CONSTANT,
    OPERATOR,
    PUNCTUATOR,
    ERROR
}
