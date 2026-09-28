package com.lexicalanalyzer;

import com.lexicalanalyzer.cli.HeadlessRunner;
import javafx.application.Application;

/**
 * Plain launcher -- deliberately does NOT extend Application, so the JVM's
 * launcher doesn't demand JavaFX on the module-path (this project runs JavaFX
 * from the classpath). With no arguments it opens the GUI; with arguments it
 * runs headless:  Main input.txt [output.txt]
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            System.exit(HeadlessRunner.run(args));
        }
        Application.launch(App.class, args);
    }
}
