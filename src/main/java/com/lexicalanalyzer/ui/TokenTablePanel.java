package com.lexicalanalyzer.ui;

import com.lexicalanalyzer.model.TokenRow;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Right-panel results view: Token Type / Lexeme / Line No, as the brief requires. */
public class TokenTablePanel extends VBox {

    private final TableView<TokenRow> table = new TableView<>();

    @SuppressWarnings("unchecked")
    public TokenTablePanel() {
        getStyleClass().add("token-table-panel");

        TableColumn<TokenRow, String> typeCol = new TableColumn<>("Token Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(140);

        TableColumn<TokenRow, String> lexemeCol = new TableColumn<>("Lexeme");
        lexemeCol.setCellValueFactory(new PropertyValueFactory<>("lexeme"));
        lexemeCol.setPrefWidth(280);

        TableColumn<TokenRow, Number> lineCol = new TableColumn<>("Line No");
        lineCol.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        lineCol.setPrefWidth(70);

        // Sorting would fight the live, in-order replay (and the auto-scroll).
        typeCol.setSortable(false);
        lexemeCol.setSortable(false);
        lineCol.setSortable(false);

        table.getColumns().addAll(typeCol, lexemeCol, lineCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("token-table");

        // Style each row by token type ("row-error", "row-keyword", ...) -- see the CSS.
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TokenRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(s -> s.startsWith("row-"));
                if (!empty && item != null) {
                    getStyleClass().add("row-" + item.getType().toLowerCase());
                }
            }
        });

        Label placeholder = new Label("No tokens yet -- run the tokenizer to see results here.");
        placeholder.getStyleClass().add("table-placeholder");
        table.setPlaceholder(placeholder);

        getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public ObservableList<TokenRow> getItems() {
        return table.getItems();
    }

    public void clear() {
        table.getItems().clear();
    }

    public void scrollToBottom() {
        if (!table.getItems().isEmpty()) {
            table.scrollTo(table.getItems().size() - 1);
        }
    }
}
