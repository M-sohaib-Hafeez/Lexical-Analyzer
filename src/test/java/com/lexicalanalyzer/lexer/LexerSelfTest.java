package com.lexicalanalyzer.lexer;

import com.lexicalanalyzer.io.FileLoader;
import com.lexicalanalyzer.io.TokenFileWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free self-test for the backend (no JUnit needed).
 * Run it from IntelliJ: right-click this file -> Run 'LexerSelfTest.main()'.
 * The exit code is non-zero if any case fails.
 */
public class LexerSelfTest {

    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // --- valid tokens
        expect("declaration", "int count = 42;",
                "KEYWORD:int:1", "IDENTIFIER:count:1", "OPERATOR:=:1", "INTEGER_CONSTANT:42:1", "PUNCTUATOR:;:1");
        expect("case-sensitive keywords", "Int int INT",
                "IDENTIFIER:Int:1", "KEYWORD:int:1", "IDENTIFIER:INT:1");
        expect("underscore identifiers", "int _count = a_b1;",
                "KEYWORD:int:1", "IDENTIFIER:_count:1", "OPERATOR:=:1", "IDENTIFIER:a_b1:1", "PUNCTUATOR:;:1");
        expect("two-char operators", "a<=b&&c!=d++",
                "IDENTIFIER:a:1", "OPERATOR:<=:1", "IDENTIFIER:b:1", "OPERATOR:&&:1",
                "IDENTIFIER:c:1", "OPERATOR:!=:1", "IDENTIFIER:d:1", "OPERATOR:++:1");

        // --- strings and chars
        expect("plain string", "\"hi there\"", "STRING_CONSTANT:\"hi there\":1");
        expect("escaped quote inside string", "\"say \\\"hi\\\"\" x",
                "STRING_CONSTANT:\"say \\\"hi\\\"\":1", "IDENTIFIER:x:1");
        expect("string with \\n escape", "\"a\\n\"", "STRING_CONSTANT:\"a\\n\":1");
        expect("char constant", "'A'", "CHAR_CONSTANT:'A':1");
        expect("escape char '\\n' is ONE char", "'\\n' x", "CHAR_CONSTANT:'\\n':1", "IDENTIFIER:x:1");
        expect("escape char '\\''", "'\\''", "CHAR_CONSTANT:'\\'':1");
        expect("escape char '\\\\'", "'\\\\'", "CHAR_CONSTANT:'\\\\':1");

        // --- comments
        expect("comment yields no token, lines advance", "a /* one\ntwo\nthree */ b",
                "IDENTIFIER:a:1", "IDENTIFIER:b:3");
        expect("unterminated comment reported at its START line", "int a;\n/* never closes\nint b;",
                "KEYWORD:int:1", "IDENTIFIER:a:1", "PUNCTUATOR:;:1", "ERROR:Unterminated comment:2");

        // --- errors: report, skip rest of that line, resume on next line
        expect("char too long skips rest of line", "x = 'ab' + 1;\ny",
                "IDENTIFIER:x:1", "OPERATOR:=:1", "ERROR:Char constant too long:1", "IDENTIFIER:y:2");
        expect("empty char constant", "'' z\nw", "ERROR:Empty char constant:1", "IDENTIFIER:w:2");
        expect("string exceeding line", "s = \"oops\nnext",
                "IDENTIFIER:s:1", "OPERATOR:=:1", "ERROR:String constant exceeds line:1", "IDENTIFIER:next:2");
        expect("backslash at end of line inside string", "\"abc\\\nq",
                "ERROR:String constant exceeds line:1", "IDENTIFIER:q:2");
        expect("'#' is undefined and skips the line", "#include <stdio.h>\nint a;",
                "ERROR:Undefined symbol '#':1", "KEYWORD:int:2", "IDENTIFIER:a:2", "PUNCTUATOR:;:2");
        expect("'~' is undefined mid-line", "a ~ b\nc",
                "IDENTIFIER:a:1", "ERROR:Undefined symbol '~':1", "IDENTIFIER:c:2");
        expect("non-ASCII letter is undefined", "caf\u00e9 x\nok",
                "IDENTIFIER:caf:1", "ERROR:Undefined symbol '\u00e9':1", "IDENTIFIER:ok:2");
        expect("CRLF line numbers", "a;\r\nb;\r\nc ~ 1;\r\nd",
                "IDENTIFIER:a:1", "PUNCTUATOR:;:1", "IDENTIFIER:b:2", "PUNCTUATOR:;:2",
                "IDENTIFIER:c:3", "ERROR:Undefined symbol '~':3", "IDENTIFIER:d:4");
        expect("empty input", "");

        // --- listener sees the real automaton
        EventRecorder rec = new EventRecorder();
        new Lexer().tokenize("int x;", rec);
        List<String> seen = new ArrayList<>();
        for (LexEvent e : rec.getEvents()) {
            seen.add(e.kind() == LexEvent.Kind.STATE ? "S:" + e.state()
                    : e.kind() == LexEvent.Kind.TOKEN ? "T:" + e.token().getLexeme() : "C");
        }
        check("listener event order", seen.toString(),
                "[S:IN_IDENTIFIER, T:int, S:START, S:IN_IDENTIFIER, T:x, S:START, S:IN_OPERATOR, T:;, S:START]");

        // --- file helpers
        Path tmp = Files.createTempFile("lex", ".c");
        try {
            Files.write(tmp, new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'i', 'n', 't'});
            check("UTF-8 BOM stripped", FileLoader.read(tmp), "int");
            Files.write(tmp, new byte[]{'"', (byte) 0xE9, '"'}); // Latin-1 e-acute, invalid UTF-8
            check("non-UTF-8 file falls back instead of failing", FileLoader.read(tmp), "\"\u00e9\"");
        } finally {
            Files.deleteIfExists(tmp);
        }

        String out = TokenFileWriter.format(new Lexer().tokenize("int a;\n'xy'"));
        check("output file lists tokens", out.contains("Token: KEYWORD") && out.contains("Lexeme: int")
                && out.contains("Line No: 1"), true);
        check("output file lists errors separately", out.contains("=== Lexical Errors ===")
                && out.contains("Line 2: Char constant too long"), true);
        check("output file has totals", out.contains("Total tokens: 3, errors: 1"), true);

        System.out.println(passed + " passed, " + failures.size() + " failed");
        failures.forEach(f -> System.out.println("  FAIL: " + f));
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static void expect(String label, String source, String... expected) {
        List<String> actual = new ArrayList<>();
        for (Token t : new Lexer().tokenize(source)) {
            actual.add(t.getType() + ":" + t.getLexeme() + ":" + t.getLineNumber());
        }
        check(label, actual.toString(), List.of(expected).toString());
    }

    private static void check(String label, Object actual, Object expected) {
        if (actual.equals(expected)) {
            passed++;
        } else {
            failures.add(label + "\n      expected " + expected + "\n      actual   " + actual);
        }
    }
}
