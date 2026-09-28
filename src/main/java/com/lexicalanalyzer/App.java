package com.lexicalanalyzer;

import com.lexicalanalyzer.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/** The JavaFX application itself (kept separate from Main -- one class per file). */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(new MainView(), 1100, 700);

        URL css = getClass().getResource("/styles/dark-theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("Warning: /styles/dark-theme.css not found. Running without custom styles.");
        }

        primaryStage.setTitle("Lexical Analyzer");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }
}
