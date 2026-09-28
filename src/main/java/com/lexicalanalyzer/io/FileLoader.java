package com.lexicalanalyzer.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads a source file robustly: UTF-8 first, falling back to Windows-1252
 * for files saved by older editors, and stripping a leading byte-order mark
 * (which would otherwise be reported as an "Undefined symbol" on line 1).
 */
public final class FileLoader {

    private FileLoader() {
    }

    public static String read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            text = new String(bytes, fallbackCharset());
        }
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return text;
    }

    private static Charset fallbackCharset() {
        try {
            return Charset.forName("windows-1252");
        } catch (RuntimeException e) {
            return StandardCharsets.ISO_8859_1;
        }
    }
}
