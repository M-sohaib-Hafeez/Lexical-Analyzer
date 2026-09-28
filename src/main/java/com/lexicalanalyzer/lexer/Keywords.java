package com.lexicalanalyzer.lexer;

import java.util.Set;

/** The reserved words of the (case-sensitive) C grammar this lab targets. */
public final class Keywords {

    private Keywords() {
    }

    public static final Set<String> ALL = Set.of(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "int", "long", "register", "return", "short", "signed", "sizeof",
            "static", "struct", "switch", "typedef", "union", "unsigned", "void",
            "volatile", "while"
    );

    public static boolean isKeyword(String word) {
        return ALL.contains(word);
    }
}
