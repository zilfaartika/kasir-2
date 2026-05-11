package com.kasir;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Entry point aplikasi Kasir Swalayan — tanpa FXML.
 */
public class MainApp extends Application {

    // Session state (diisi saat login)
    public static int loggedId = 0;
    public static String loggedUser = "";
    public static String loggedRole = "";

    @Override
    public void start(Stage stage) {
        stage.setTitle("Kasir Swalayan — Login");
        stage.setResizable(false);
        stage.setScene(new LoginView().build(stage));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}