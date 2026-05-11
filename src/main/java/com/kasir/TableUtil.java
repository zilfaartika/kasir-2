package com.kasir;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/** Mengisi TableView<ObservableList<String>> secara generik dari ResultSet. */
public class TableUtil {

    /**
     * Hapus kolom lama, buat kolom baru sesuai ResultSet, lalu isi baris.
     */
    public static void populate(TableView<ObservableList<String>> table, ResultSet rs) throws Exception {
        table.getColumns().clear();
        table.getItems().clear();

        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        for (int i = 1; i <= colCount; i++) {
            final int idx = i - 1;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(meta.getColumnLabel(i));
            col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(idx)));
            col.setPrefWidth(130);
            table.getColumns().add(col);
        }

        while (rs.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= colCount; i++) {
                String val = rs.getString(i);
                row.add(val == null ? "" : val);
            }
            table.getItems().add(row);
        }
    }
}