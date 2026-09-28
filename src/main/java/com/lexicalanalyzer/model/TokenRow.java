package com.lexicalanalyzer.model;

import com.lexicalanalyzer.lexer.Token;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * JavaFX bean wrapper around a {@link Token}, shaped for TableView's
 * PropertyValueFactory (which looks for "xProperty()" accessors).
 */
public final class TokenRow {

    private final SimpleStringProperty type;
    private final SimpleStringProperty lexeme;
    private final SimpleIntegerProperty lineNumber;

    public TokenRow(Token token) {
        this.type = new SimpleStringProperty(token.getType().name());
        this.lexeme = new SimpleStringProperty(token.getLexeme());
        this.lineNumber = new SimpleIntegerProperty(token.getLineNumber());
    }

    public String getType() {
        return type.get();
    }

    public String getLexeme() {
        return lexeme.get();
    }

    public int getLineNumber() {
        return lineNumber.get();
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public SimpleStringProperty lexemeProperty() {
        return lexeme;
    }

    public SimpleIntegerProperty lineNumberProperty() {
        return lineNumber;
    }
}
