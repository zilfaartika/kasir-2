package com.kasir;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Halaman Login — dibangun sepenuhnya secara programatik (tanpa FXML).
 */
public class LoginView {

    public Scene build(Stage stage) {
        // ── Form fields ──────────────────────────────────────────────────────
        TextField tfUser = new TextField();
        PasswordField tfPass = new PasswordField();
        Label lblMsg = new Label();
        Button btnLogin = new Button("Masuk");

        tfUser.setPromptText("Username");
        tfPass.setPromptText("Password");
        tfUser.getStyleClass().add("login-field");
        tfPass.getStyleClass().add("login-field");
        btnLogin.getStyleClass().add("btn-primary");
        lblMsg.getStyleClass().add("login-error");

        // ── Layout ───────────────────────────────────────────────────────────
        Label lblTitle = new Label("Kasir Swalayan");
        lblTitle.getStyleClass().add("login-title");

        Label lblSub = new Label("Silakan masuk untuk melanjutkan");
        lblSub.getStyleClass().add("login-sub");

        VBox card = new VBox(12,
                lblTitle, lblSub,
                fieldBox("Username", tfUser),
                fieldBox("Password", tfPass),
                lblMsg,
                btnLogin);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("login-root");

        // ── Login action ─────────────────────────────────────────────────────
        Runnable doLogin = () -> {
            String username = tfUser.getText().trim();
            String password = tfPass.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                lblMsg.setText("Username dan password wajib diisi!");
                return;
            }

            try (Connection c = DB.getConnection();
                    PreparedStatement ps = c.prepareStatement(
                            "SELECT id_user, nama, role FROM users WHERE username=? AND password=?")) {

                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    MainApp.loggedId = rs.getInt("id_user");
                    MainApp.loggedUser = rs.getString("nama");
                    MainApp.loggedRole = rs.getString("role");

                    MainView mainView = new MainView();
                    stage.setScene(mainView.build(stage));
                    stage.setTitle("Kasir Swalayan — " + MainApp.loggedUser);
                    stage.setResizable(true);
                    stage.setWidth(1150);
                    stage.setHeight(700);
                    stage.centerOnScreen();
                } else {
                    lblMsg.setText("Username atau password salah!");
                }

            } catch (Exception e) {
                lblMsg.setText("Koneksi DB gagal: " + e.getMessage());
            }
        };

        btnLogin.setOnAction(e -> doLogin.run());
        tfPass.setOnAction(e -> doLogin.run());
        btnLogin.setMaxWidth(Double.MAX_VALUE);

        // ── Scene ────────────────────────────────────────────────────────────
        Scene scene = new Scene(root, 480, 380);
        scene.getStylesheets().add(
                getClass().getResource("/com/kasir/style.css").toExternalForm());
        return scene;
    }

    /** Label + field dibungkus VBox kecil. */
    private VBox fieldBox(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("login-label");
        VBox box = new VBox(4, lbl, field);
        if (field instanceof Region) {
            ((Region) field).setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }
}