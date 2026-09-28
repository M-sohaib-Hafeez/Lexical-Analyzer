# Lexical Analyzer (made for Compilor Constructon phase 1)

A JavaFX desktop app implementing the Phase 1 lexical analyzer for the C subset in the
assignment brief.

- **Two input modes** -- type code directly, or load a `.c` / `.txt` file (drag-and-drop
  anywhere in the window, or **Browse...**).
- **Live tokenization view** -- after **Run Tokenizer** (or `Ctrl+Enter`), the FSM diagram, console,
  tokens-per-line chart and results table update token by token. **Skip Animation** jumps to
  the end; **Edit Code** (or either mode toggle) returns to the editor.
- **One class per file**, in `lexer` (backend), `io`, `cli`, `model` and `ui` packages.
- **Headless mode** for the literal "input file in, output file out" flow:
  `Main input.txt [output.txt]` (no arguments = GUI).

---

## Lexical rules (what the code implements)

| Item | Rule |
|---|---|
| Identifier | `(letter \| _) (letter \| digit \| _)*`, ASCII only, case-sensitive |
| Integer constant | `digit+`, unsigned |
| String constant | `"..."`, single line; escapes such as `\"` and `\\` stay inside the string |
| Char constant | exactly ONE character **or one escape sequence** (`'A'`, `'\n'`, `'\''`, `'\\'`) |
| Comment | `/* ... */`, may span lines, no nesting, produces no token |
| Operators / punctuators | `+ - * / % = < > ! & \|` and `== != <= >= && \|\| ++ -- += -= *= /= %=`; `; , { } ( ) [ ] .` |
| Errors | `Unterminated comment`, `String constant exceeds line`, `Char constant too long`, `Undefined symbol 'x'`, plus `Empty char constant` for `''` |

On every error the rest of that physical line is discarded and no further tokens are produced
from it; scanning resumes on the next line.

### Things worth knowing before you demo
- The brief lists `~` and `#` as invalid characters, so `#include <stdio.h>` reports
  `Undefined symbol '#'` and the line is skipped. Implemented literally; if your instructor
  wants preprocessor lines tolerated, change the `else` branch at the end of `Scan.run()` in `Lexer.java`.
- No `//` comments and no floating-point constants (the brief defines neither). `3.14` scans as
  `3`, `.`, `14`.
- `''` (empty char constant) is reported as an error. To accept it instead, delete the
  `ERR_EMPTY_CHAR` check at the top of `Scan.scanChar()`.
- After an error, the rest of that line is skipped -- even a `/*` opening later on it.
  That is what the brief says, but the comment's closing `*/` then shows up as operators.

---

## Output file

`TokenFileWriter` writes each token as `Token / Lexeme / Line No`, then a `=== Lexical Errors ===`
section (`Line N: message`) and a totals line.

- **Export Output** button: choose where to save.
- When the code came straight from a loaded file and is unmodified, the output is also written
  automatically next to it as `<name>_tokens.txt` (the console log says where).
- Headless: `java -cp target/classes;<javafx jars> com.lexicalanalyzer.Main input.txt output.txt`,
  or add `input.txt output.txt` as *Program arguments* in the IntelliJ run configuration.

---

## Project structure

```
Lexical analyzer/
+-- pom.xml
+-- sample-input/  c_sample.c (valid code + every error), clean_sample.c (no errors)
+-- src/main/java/com/lexicalanalyzer/
|   +-- Main.java                  launcher (does NOT extend Application; supports headless args)
|   +-- App.java                   the JavaFX Application
|   +-- lexer/   Lexer, LexerListener, EventRecorder, LexEvent, LexerState, Token, TokenType,
|   |            Keywords, SourceReader
|   +-- io/      FileLoader (UTF-8 -> Windows-1252 fallback, BOM stripped), TokenFileWriter
|   +-- cli/     HeadlessRunner
|   +-- model/   TokenRow
|   +-- ui/      MainView, HeaderBar, CodeEditorPanel, FileIngestionPanel, TokenTablePanel,
|                DiagnosticsPanel, ConsoleLogView, FsmDiagramView, TokensPerLineChartView,
|                StatusBarPanel
+-- src/main/resources/styles/dark-theme.css
+-- src/test/java/com/lexicalanalyzer/lexer/LexerSelfTest.java   28 backend checks, no JUnit needed
```

## How the "live" view works
The Lexer runs first (microseconds) and reports every state change, token and skipped comment to a
`LexerListener`. `MainView` replays that recorded list on a timeline, so the FSM diagram shows the
scanner's *real* state. Large inputs are batched so the replay always takes a few seconds at most.

## Running in IntelliJ
1. **File -> Open...** the `Lexical analyzer` folder (the one containing `pom.xml`); let Maven import.
2. Project SDK: JDK 17 or newer.
3. Run `Main.main()`. To run the checks, run `LexerSelfTest.main()` (prints `28 passed, 0 failed`).

## Verification
Compiled with `--release 17` with no warnings; `LexerSelfTest` passes 28/28; the GUI was launched under a
virtual display (against the OpenJFX 11 runtime) and driven through load -> run -> replay -> skip ->
back to editor -> re-run. It has not been run on your Windows/JavaFX 21 setup, so tell me if anything looks off.
