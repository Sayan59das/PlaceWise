package com.placewise.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** A console fed from a fixed list of lines whose output is captured; running out of lines = end of input. */
final class ScriptedConsole {

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final ConsoleIO io;

    ScriptedConsole(String... lines) {
        String text = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
        this.io = new ConsoleIO(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8));
    }

    String output() {
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
