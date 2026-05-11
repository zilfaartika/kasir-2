package com.kasir;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/** Helper untuk dialog Alert agar tidak berulang. */
public class AlertUtil {

    public static void info(String msg) {
        show(Alert.AlertType.INFORMATION, "Info", msg);
    }

    public static void warn(String msg) {
        show(Alert.AlertType.WARNING, "Peringatan", msg);
    }

    public static void error(String title, String msg) {
        show(Alert.AlertType.ERROR, title, msg);
    }

    public static boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.setTitle("Konfirmasi");
        Optional<ButtonType> result = a.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private static void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }
}