package com.kasir;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;

public class MainView {

    private final ObservableList<ObservableList<String>> keranjang = FXCollections.observableArrayList();
    private double totalBelanja = 0;
    private StackPane contentArea;

    public Scene build(Stage stage) {
        // ── TOP BAR ──────────────────────────────────────────────────────────
        Label lblWelcome = new Label("Halo, " + MainApp.loggedUser + "  (" + MainApp.loggedRole + ")");
        lblWelcome.getStyleClass().add("topbar-label");

        Button btnLogout = new Button("Keluar");
        btnLogout.getStyleClass().add("btn-danger");
        btnLogout.setOnAction(e -> {
            MainApp.loggedId = 0;
            MainApp.loggedUser = "";
            MainApp.loggedRole = "";
            stage.setScene(new LoginView().build(stage));
            stage.setResizable(false);
            stage.setWidth(480);
            stage.setHeight(380);
            stage.centerOnScreen();
        });

        HBox topBar = new HBox(lblWelcome, new Spacer(), btnLogout);
        topBar.getStyleClass().add("topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // ── SIDEBAR ───────────────────────────────────────────────────────────
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        boolean isAdmin = "admin".equals(MainApp.loggedRole);

        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");

        Label lblMenu = new Label("MENU");
        lblMenu.getStyleClass().add("sidebar-header");
        sidebar.getChildren().add(lblMenu);

        sidebar.getChildren().add(navBtn("📦 Produk", e -> showProduk()));
        sidebar.getChildren().add(navBtn("🏷 Kategori", e -> showKategori()));
        sidebar.getChildren().add(navBtn("🚚 Supplier", e -> showSupplier()));
        if (isAdmin)
            sidebar.getChildren().add(navBtn("👤 Users", e -> showUser()));
        sidebar.getChildren().add(navBtn("📥 Stok Masuk", e -> showStok()));
        sidebar.getChildren().add(navBtn("🛒 Transaksi", e -> showTransaksi()));
        sidebar.getChildren().add(navBtn("📋 Riwayat", e -> showRiwayat()));

        // Separator laporan
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 8 0;");
        Label lblLaporan = new Label("LAPORAN");
        lblLaporan.getStyleClass().add("sidebar-header");
        sidebar.getChildren().addAll(sep, lblLaporan);
        sidebar.getChildren()
                .add(navBtn("📊 Lap. Transaksi", e -> openBrowser("http://localhost/kasir_laporan/transaksi.php")));
        sidebar.getChildren()
                .add(navBtn("📦 Lap. Produk", e -> openBrowser("http://localhost/kasir_laporan/produk.php")));
        sidebar.getChildren()
                .add(navBtn("📥 Lap. Stok Masuk", e -> openBrowser("http://localhost/kasir_laporan/stok_masuk.php")));

        // ── ROOT LAYOUT ───────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(sidebar);
        root.setCenter(contentArea);

        showProduk();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/com/kasir/style.css").toExternalForm());
        return scene;
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    private Button navBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.getStyleClass().add("sidebar-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(handler);
        return b;
    }

    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().setAll(node);
    }

    private void openBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            AlertUtil.error("Buka Browser Gagal",
                    "Pastikan XAMPP aktif dan folder kasir_laporan ada di htdocs.\n\n" + e.getMessage());
        }
    }

    // =========================================================================
    // PRODUK
    // =========================================================================

    private void showProduk() {
        ComboBox<String> cbKategori = new ComboBox<>();
        ComboBox<String> cbSupplier = new ComboBox<>();
        loadCombo(cbKategori, "SELECT id_kategori, nama_kategori FROM kategori");
        loadCombo(cbSupplier, "SELECT id_supplier, nama_supplier FROM supplier");

        TextField tfKode = new TextField();
        tfKode.setPromptText("Kode Produk");
        TextField tfNama = new TextField();
        tfNama.setPromptText("Nama Produk");
        TextField tfHarga = new TextField();
        tfHarga.setPromptText("Harga");
        TextField tfStok = new TextField();
        tfStok.setPromptText("Stok");
        TextField tfSatuan = new TextField();
        tfSatuan.setPromptText("Satuan (pcs)");

        TableView<ObservableList<String>> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable loadData = () -> {
            try (Connection c = DB.getConnection();
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery(
                            "SELECT p.kode_produk AS Kode, p.nama_produk AS Nama, " +
                                    "k.nama_kategori AS Kategori, sp.nama_supplier AS Supplier, " +
                                    "FORMAT(p.harga,0) AS Harga, p.stok AS Stok, p.satuan AS Satuan " +
                                    "FROM produk p " +
                                    "JOIN kategori k  ON p.id_kategori=k.id_kategori " +
                                    "JOIN supplier sp ON p.id_supplier=sp.id_supplier " +
                                    "ORDER BY p.kode_produk")) {
                TableUtil.populate(tbl, rs);
            } catch (Exception e) {
                AlertUtil.error("Load produk gagal", e.getMessage());
            }
        };
        loadData.run();

        tbl.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null)
                return;
            tfKode.setText(sel.get(0));
            tfNama.setText(sel.get(1));
            tfHarga.setText(sel.get(4).replace(",", ""));
            tfStok.setText(sel.get(5));
            tfSatuan.setText(sel.get(6));
            setCbByLabel(cbKategori, sel.get(2));
            setCbByLabel(cbSupplier, sel.get(3));
        });

        Button btnSimpan = new Button("💾 Simpan");
        btnSimpan.getStyleClass().add("btn-primary");
        btnSimpan.setOnAction(e -> {
            if (tfKode.getText().isEmpty() || tfNama.getText().isEmpty()) {
                AlertUtil.warn("Kode dan Nama wajib diisi!");
                return;
            }
            try (Connection c = DB.getConnection()) {
                int idKat = parseId(cbKategori), idSup = parseId(cbSupplier);
                if (idKat < 0 || idSup < 0) {
                    AlertUtil.warn("Pilih Kategori dan Supplier!");
                    return;
                }
                PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO produk (id_kategori,id_supplier,kode_produk,nama_produk,harga,stok,satuan) " +
                                "VALUES (?,?,?,?,?,?,?) " +
                                "ON DUPLICATE KEY UPDATE id_kategori=VALUES(id_kategori),id_supplier=VALUES(id_supplier),"
                                +
                                "nama_produk=VALUES(nama_produk),harga=VALUES(harga),stok=VALUES(stok),satuan=VALUES(satuan)");
                ps.setInt(1, idKat);
                ps.setInt(2, idSup);
                ps.setString(3, tfKode.getText().trim());
                ps.setString(4, tfNama.getText().trim());
                ps.setDouble(5, Double.parseDouble(tfHarga.getText().trim()));
                ps.setInt(6, Integer.parseInt(tfStok.getText().trim()));
                ps.setString(7, tfSatuan.getText().isEmpty() ? "pcs" : tfSatuan.getText().trim());
                ps.executeUpdate();
                clearAll(tfKode, tfNama, tfHarga, tfStok, tfSatuan);
                cbKategori.setValue(null);
                cbSupplier.setValue(null);
                loadData.run();
                AlertUtil.info("Produk berhasil disimpan!");
            } catch (Exception ex) {
                AlertUtil.error("Simpan gagal", ex.getMessage());
            }
        });

        Button btnHapus = new Button("🗑 Hapus");
        btnHapus.getStyleClass().add("btn-danger");
        btnHapus.setOnAction(e -> {
            ObservableList<String> sel = tbl.getSelectionModel().getSelectedItem();
            if (sel == null) {
                AlertUtil.warn("Pilih produk!");
                return;
            }
            if (!AlertUtil.confirm("Hapus produk \"" + sel.get(1) + "\"?"))
                return;
            try (Connection c = DB.getConnection();
                    PreparedStatement ps = c.prepareStatement("DELETE FROM produk WHERE kode_produk=?")) {
                ps.setString(1, sel.get(0));
                ps.executeUpdate();
                loadData.run();
                AlertUtil.info("Produk berhasil dihapus!");
            } catch (Exception ex) {
                AlertUtil.error("Hapus gagal", ex.getMessage());
            }
        });

        Button btnBatal = new Button("✖ Batal");
        btnBatal.getStyleClass().add("btn-secondary");
        btnBatal.setOnAction(e -> {
            clearAll(tfKode, tfNama, tfHarga, tfStok, tfSatuan);
            cbKategori.setValue(null);
            cbSupplier.setValue(null);
            tbl.getSelectionModel().clearSelection();
        });

        VBox page = pageBox("Manajemen Produk",
                hbox(8, tfKode, tfNama, tfHarga, tfStok, tfSatuan),
                hbox(8, labeled("Kategori", cbKategori), labeled("Supplier", cbSupplier)),
                hbox(8, btnSimpan, btnHapus, btnBatal),
                new Separator(), tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);
        setContent(page);
    }

    // =========================================================================
    // KATEGORI
    // =========================================================================

    private void showKategori() {
        int[] selectedId = { -1 };
        TextField tfNama = new TextField();
        tfNama.setPromptText("Nama Kategori");
        TableView<ObservableList<String>> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable loadData = () -> {
            try (Connection c = DB.getConnection();
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery(
                            "SELECT id_kategori AS ID, nama_kategori AS Nama FROM kategori ORDER BY id_kategori")) {
                TableUtil.populate(tbl, rs);
            } catch (Exception e) {
                AlertUtil.error("Load kategori gagal", e.getMessage());
            }
        };
        loadData.run();

        tbl.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null)
                return;
            selectedId[0] = Integer.parseInt(sel.get(0));
            tfNama.setText(sel.get(1));
        });

        Button btnSimpan = new Button("💾 Simpan");
        btnSimpan.getStyleClass().add("btn-primary");
        btnSimpan.setOnAction(e -> {
            String nama = tfNama.getText().trim();
            if (nama.isEmpty()) {
                AlertUtil.warn("Nama kategori wajib diisi!");
                return;
            }
            try (Connection c = DB.getConnection()) {
                PreparedStatement ps;
                if (selectedId[0] > 0) {
                    ps = c.prepareStatement("UPDATE kategori SET nama_kategori=? WHERE id_kategori=?");
                    ps.setString(1, nama);
                    ps.setInt(2, selectedId[0]);
                } else {
                    ps = c.prepareStatement("INSERT INTO kategori (nama_kategori) VALUES (?)");
                    ps.setString(1, nama);
                }
                ps.executeUpdate();
                selectedId[0] = -1;
                tfNama.clear();
                loadData.run();
                AlertUtil.info("Kategori disimpan!");
            } catch (Exception ex) {
                AlertUtil.error("Simpan gagal", ex.getMessage());
            }
        });

        Button btnHapus = new Button("🗑 Hapus");
        btnHapus.getStyleClass().add("btn-danger");
        btnHapus.setOnAction(e -> {
            ObservableList<String> sel = tbl.getSelectionModel().getSelectedItem();
            if (sel == null) {
                AlertUtil.warn("Pilih kategori!");
                return;
            }
            if (!AlertUtil.confirm("Hapus kategori \"" + sel.get(1) + "\"?"))
                return;
            try (Connection c = DB.getConnection();
                    PreparedStatement ps = c.prepareStatement("DELETE FROM kategori WHERE id_kategori=?")) {
                ps.setInt(1, Integer.parseInt(sel.get(0)));
                ps.executeUpdate();
                selectedId[0] = -1;
                tfNama.clear();
                loadData.run();
                AlertUtil.info("Kategori dihapus!");
            } catch (Exception ex) {
                AlertUtil.error("Hapus gagal", ex.getMessage());
            }
        });

        Button btnBatal = new Button("✖ Batal");
        btnBatal.getStyleClass().add("btn-secondary");
        btnBatal.setOnAction(e -> {
            selectedId[0] = -1;
            tfNama.clear();
            tbl.getSelectionModel().clearSelection();
        });

        VBox page = pageBox("Manajemen Kategori",
                hbox(8, tfNama, btnSimpan, btnHapus, btnBatal),
                new Separator(), tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);
        setContent(page);
    }

    // =========================================================================
    // SUPPLIER
    // =========================================================================

    private void showSupplier() {
        int[] selectedId = { -1 };
        TextField tfNama = new TextField();
        tfNama.setPromptText("Nama Supplier");
        TextField tfKontak = new TextField();
        tfKontak.setPromptText("Kontak");
        TextField tfAlamat = new TextField();
        tfAlamat.setPromptText("Alamat");
        TextField tfEmail = new TextField();
        tfEmail.setPromptText("Email");

        TableView<ObservableList<String>> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable loadData = () -> {
            try (Connection c = DB.getConnection();
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery(
                            "SELECT id_supplier AS ID, nama_supplier AS Nama, kontak AS Kontak, alamat AS Alamat, email AS Email FROM supplier ORDER BY id_supplier")) {
                TableUtil.populate(tbl, rs);
            } catch (Exception e) {
                AlertUtil.error("Load supplier gagal", e.getMessage());
            }
        };
        loadData.run();

        tbl.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null)
                return;
            selectedId[0] = Integer.parseInt(sel.get(0));
            tfNama.setText(sel.get(1));
            tfKontak.setText(sel.get(2));
            tfAlamat.setText(sel.get(3));
            tfEmail.setText(sel.get(4));
        });

        Button btnSimpan = new Button("💾 Simpan");
        btnSimpan.getStyleClass().add("btn-primary");
        btnSimpan.setOnAction(e -> {
            if (tfNama.getText().trim().isEmpty()) {
                AlertUtil.warn("Nama wajib diisi!");
                return;
            }
            try (Connection c = DB.getConnection()) {
                PreparedStatement ps;
                if (selectedId[0] > 0) {
                    ps = c.prepareStatement(
                            "UPDATE supplier SET nama_supplier=?,kontak=?,alamat=?,email=? WHERE id_supplier=?");
                    ps.setString(1, tfNama.getText().trim());
                    ps.setString(2, tfKontak.getText().trim());
                    ps.setString(3, tfAlamat.getText().trim());
                    ps.setString(4, tfEmail.getText().trim());
                    ps.setInt(5, selectedId[0]);
                } else {
                    ps = c.prepareStatement(
                            "INSERT INTO supplier (nama_supplier,kontak,alamat,email) VALUES (?,?,?,?)");
                    ps.setString(1, tfNama.getText().trim());
                    ps.setString(2, tfKontak.getText().trim());
                    ps.setString(3, tfAlamat.getText().trim());
                    ps.setString(4, tfEmail.getText().trim());
                }
                ps.executeUpdate();
                selectedId[0] = -1;
                clearAll(tfNama, tfKontak, tfAlamat, tfEmail);
                loadData.run();
                AlertUtil.info("Supplier disimpan!");
            } catch (Exception ex) {
                AlertUtil.error("Simpan gagal", ex.getMessage());
            }
        });

        Button btnHapus = new Button("🗑 Hapus");
        btnHapus.getStyleClass().add("btn-danger");
        btnHapus.setOnAction(e -> {
            ObservableList<String> sel = tbl.getSelectionModel().getSelectedItem();
            if (sel == null) {
                AlertUtil.warn("Pilih supplier!");
                return;
            }
            if (!AlertUtil.confirm("Hapus supplier \"" + sel.get(1) + "\"?"))
                return;
            try (Connection c = DB.getConnection();
                    PreparedStatement ps = c.prepareStatement("DELETE FROM supplier WHERE id_supplier=?")) {
                ps.setInt(1, Integer.parseInt(sel.get(0)));
                ps.executeUpdate();
                selectedId[0] = -1;
                clearAll(tfNama, tfKontak, tfAlamat, tfEmail);
                loadData.run();
                AlertUtil.info("Supplier dihapus!");
            } catch (Exception ex) {
                AlertUtil.error("Hapus gagal", ex.getMessage());
            }
        });

        Button btnBatal = new Button("✖ Batal");
        btnBatal.getStyleClass().add("btn-secondary");
        btnBatal.setOnAction(e -> {
            selectedId[0] = -1;
            clearAll(tfNama, tfKontak, tfAlamat, tfEmail);
            tbl.getSelectionModel().clearSelection();
        });

        VBox page = pageBox("Manajemen Supplier",
                hbox(8, tfNama, tfKontak, tfEmail),
                hbox(8, tfAlamat, btnSimpan, btnHapus, btnBatal),
                new Separator(), tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);
        setContent(page);
    }

    // =========================================================================
    // USERS
    // =========================================================================

    private void showUser() {
        int[] selectedId = { -1 };
        TextField tfNama = new TextField();
        tfNama.setPromptText("Nama Lengkap");
        TextField tfUsername = new TextField();
        tfUsername.setPromptText("Username");
        PasswordField tfPassword = new PasswordField();
        tfPassword.setPromptText("Password (kosong = tidak ganti)");
        ComboBox<String> cbRole = new ComboBox<>(FXCollections.observableArrayList("admin", "kasir"));

        TableView<ObservableList<String>> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable loadData = () -> {
            try (Connection c = DB.getConnection();
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery(
                            "SELECT id_user AS ID, nama AS Nama, username AS Username, role AS Role, " +
                                    "DATE_FORMAT(created_at,'%d-%m-%Y') AS Dibuat FROM users ORDER BY id_user")) {
                TableUtil.populate(tbl, rs);
            } catch (Exception e) {
                AlertUtil.error("Load users gagal", e.getMessage());
            }
        };
        loadData.run();

        tbl.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null)
                return;
            selectedId[0] = Integer.parseInt(sel.get(0));
            tfNama.setText(sel.get(1));
            tfUsername.setText(sel.get(2));
            tfPassword.clear();
            cbRole.setValue(sel.get(3));
        });

        Button btnSimpan = new Button("💾 Simpan");
        btnSimpan.getStyleClass().add("btn-primary");
        btnSimpan.setOnAction(e -> {
            String nama = tfNama.getText().trim(), uname = tfUsername.getText().trim(),
                    pass = tfPassword.getText().trim(), role = cbRole.getValue();
            if (nama.isEmpty() || uname.isEmpty() || role == null) {
                AlertUtil.warn("Nama, Username, Role wajib diisi!");
                return;
            }
            try (Connection c = DB.getConnection()) {
                PreparedStatement ps;
                if (selectedId[0] > 0) {
                    if (pass.isEmpty()) {
                        ps = c.prepareStatement("UPDATE users SET nama=?,username=?,role=? WHERE id_user=?");
                        ps.setString(1, nama);
                        ps.setString(2, uname);
                        ps.setString(3, role);
                        ps.setInt(4, selectedId[0]);
                    } else {
                        ps = c.prepareStatement("UPDATE users SET nama=?,username=?,password=?,role=? WHERE id_user=?");
                        ps.setString(1, nama);
                        ps.setString(2, uname);
                        ps.setString(3, pass);
                        ps.setString(4, role);
                        ps.setInt(5, selectedId[0]);
                    }
                } else {
                    if (pass.isEmpty()) {
                        AlertUtil.warn("Password wajib untuk user baru!");
                        return;
                    }
                    ps = c.prepareStatement("INSERT INTO users (nama,username,password,role) VALUES (?,?,?,?)");
                    ps.setString(1, nama);
                    ps.setString(2, uname);
                    ps.setString(3, pass);
                    ps.setString(4, role);
                }
                ps.executeUpdate();
                selectedId[0] = -1;
                clearAll(tfNama, tfUsername, tfPassword);
                cbRole.setValue(null);
                loadData.run();
                AlertUtil.info("User disimpan!");
            } catch (Exception ex) {
                AlertUtil.error("Simpan gagal", ex.getMessage());
            }
        });

        Button btnHapus = new Button("🗑 Hapus");
        btnHapus.getStyleClass().add("btn-danger");
        btnHapus.setOnAction(e -> {
            ObservableList<String> sel = tbl.getSelectionModel().getSelectedItem();
            if (sel == null) {
                AlertUtil.warn("Pilih user!");
                return;
            }
            if (Integer.parseInt(sel.get(0)) == MainApp.loggedId) {
                AlertUtil.warn("Tidak bisa hapus akun sendiri!");
                return;
            }
            if (!AlertUtil.confirm("Hapus user \"" + sel.get(1) + "\"?"))
                return;
            try (Connection c = DB.getConnection();
                    PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id_user=?")) {
                ps.setInt(1, Integer.parseInt(sel.get(0)));
                ps.executeUpdate();
                selectedId[0] = -1;
                clearAll(tfNama, tfUsername, tfPassword);
                cbRole.setValue(null);
                loadData.run();
                AlertUtil.info("User dihapus!");
            } catch (Exception ex) {
                AlertUtil.error("Hapus gagal", ex.getMessage());
            }
        });

        Button btnBatal = new Button("✖ Batal");
        btnBatal.getStyleClass().add("btn-secondary");
        btnBatal.setOnAction(e -> {
            selectedId[0] = -1;
            clearAll(tfNama, tfUsername, tfPassword);
            cbRole.setValue(null);
            tbl.getSelectionModel().clearSelection();
        });

        VBox page = pageBox("Manajemen Users",
                hbox(8, tfNama, tfUsername, tfPassword, labeled("Role", cbRole)),
                hbox(8, btnSimpan, btnHapus, btnBatal),
                new Separator(), tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);
        setContent(page);
    }

    // =========================================================================
    // STOK MASUK
    // =========================================================================

    private void showStok() {
        ComboBox<String> cbProduk = new ComboBox<>();
        ComboBox<String> cbSupplier = new ComboBox<>();
        loadCombo(cbProduk, "SELECT id_produk, nama_produk FROM produk ORDER BY nama_produk");
        loadCombo(cbSupplier, "SELECT id_supplier, nama_supplier FROM supplier ORDER BY nama_supplier");

        TextField tfJumlah = new TextField();
        tfJumlah.setPromptText("Jumlah");
        TextField tfHargaBeli = new TextField();
        tfHargaBeli.setPromptText("Harga Beli");
        TextField tfTanggal = new TextField();
        tfTanggal.setPromptText("Tanggal (YYYY-MM-DD)");
        TextField tfKet = new TextField();
        tfKet.setPromptText("Keterangan (opsional)");

        TableView<ObservableList<String>> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable loadData = () -> {
            try (Connection c = DB.getConnection();
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery(
                            "SELECT sm.id_stok_masuk AS ID, p.nama_produk AS Produk, sp.nama_supplier AS Supplier, " +
                                    "u.nama AS Dicatat_Oleh, sm.jumlah AS Jumlah, FORMAT(sm.harga_beli,0) AS Harga_Beli, "
                                    +
                                    "sm.tanggal AS Tanggal, IFNULL(sm.keterangan,'') AS Keterangan " +
                                    "FROM stok_masuk sm " +
                                    "JOIN produk p    ON sm.id_produk=p.id_produk " +
                                    "JOIN supplier sp ON sm.id_supplier=sp.id_supplier " +
                                    "JOIN users u     ON sm.id_user=u.id_user " +
                                    "ORDER BY sm.tanggal DESC, sm.id_stok_masuk DESC")) {
                TableUtil.populate(tbl, rs);
            } catch (Exception e) {
                AlertUtil.error("Load stok gagal", e.getMessage());
            }
        };
        loadData.run();

        Button btnSimpan = new Button("💾 Simpan Stok");
        btnSimpan.getStyleClass().add("btn-primary");
        btnSimpan.setOnAction(e -> {
            int idProduk = parseId(cbProduk), idSup = parseId(cbSupplier);
            if (idProduk < 0 || idSup < 0) {
                AlertUtil.warn("Pilih Produk dan Supplier!");
                return;
            }
            String tgl = tfTanggal.getText().trim();
            if (tgl.isEmpty()) {
                AlertUtil.warn("Tanggal wajib diisi!");
                return;
            }
            try (Connection c = DB.getConnection()) {
                int jml = Integer.parseInt(tfJumlah.getText().trim());
                PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO stok_masuk (id_produk,id_supplier,id_user,jumlah,harga_beli,tanggal,keterangan) VALUES (?,?,?,?,?,?,?)");
                ps.setInt(1, idProduk);
                ps.setInt(2, idSup);
                ps.setInt(3, MainApp.loggedId);
                ps.setInt(4, jml);
                ps.setDouble(5, Double.parseDouble(tfHargaBeli.getText().trim()));
                ps.setString(6, tgl);
                ps.setString(7, tfKet.getText().trim());
                ps.executeUpdate();

                PreparedStatement psUpd = c.prepareStatement("UPDATE produk SET stok=stok+? WHERE id_produk=?");
                psUpd.setInt(1, jml);
                psUpd.setInt(2, idProduk);
                psUpd.executeUpdate();

                clearAll(tfJumlah, tfHargaBeli, tfTanggal, tfKet);
                cbProduk.setValue(null);
                cbSupplier.setValue(null);
                loadData.run();
                AlertUtil.info("Stok masuk disimpan!");
            } catch (Exception ex) {
                AlertUtil.error("Simpan gagal", ex.getMessage());
            }
        });

        VBox page = pageBox("Stok Masuk",
                hbox(8, labeled("Produk", cbProduk), labeled("Supplier", cbSupplier)),
                hbox(8, tfJumlah, tfHargaBeli, tfTanggal, tfKet),
                hbox(8, btnSimpan),
                new Separator(), tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);
        setContent(page);
    }

    // =========================================================================
    // TRANSAKSI
    // =========================================================================

    private void showTransaksi() {
        ComboBox<String> cbProduk = new ComboBox<>();
        ComboBox<String> cbMetode = new ComboBox<>(FXCollections.observableArrayList("tunai", "debit", "qris"));
        cbMetode.setValue("tunai");

        loadCombo(cbProduk,
                "SELECT id_produk, CONCAT(nama_produk,' (stok: ',stok,') | Rp ',FORMAT(harga,0)) " +
                        "FROM produk WHERE stok>0 ORDER BY nama_produk");

        TextField tfJumlah = new TextField();
        tfJumlah.setPromptText("Jumlah");
        TextField tfBayar = new TextField();
        tfBayar.setPromptText("Uang Bayar");
        Label lblTotal = new Label("Total: Rp 0");
        lblTotal.getStyleClass().add("trx-total");
        Label lblKembalian = new Label("Kembalian: Rp 0");
        lblKembalian.getStyleClass().add("trx-kembalian");

        TableView<ObservableList<String>> tblKeranjang = new TableView<>();
        String[] kCols = { "Produk", "Jumlah", "Harga Satuan", "Subtotal" };
        for (int i = 0; i < kCols.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(kCols[i]);
            col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().get(idx)));
            col.setPrefWidth(160);
            tblKeranjang.getColumns().add(col);
        }
        tblKeranjang.setItems(keranjang);
        tblKeranjang.setMaxHeight(220);

        Button btnTambah = new Button("➕ Tambah");
        btnTambah.getStyleClass().add("btn-primary");
        btnTambah.setOnAction(e -> {
            if (cbProduk.getValue() == null || tfJumlah.getText().isEmpty()) {
                AlertUtil.warn("Pilih produk dan isi jumlah!");
                return;
            }
            int idProduk = parseId(cbProduk);
            int jumlah;
            try {
                jumlah = Integer.parseInt(tfJumlah.getText().trim());
            } catch (NumberFormatException ex) {
                AlertUtil.warn("Jumlah harus angka!");
                return;
            }
            try (Connection c = DB.getConnection();
                    PreparedStatement ps = c
                            .prepareStatement("SELECT nama_produk, harga, stok FROM produk WHERE id_produk=?")) {
                ps.setInt(1, idProduk);
                ResultSet rs = ps.executeQuery();
                if (!rs.next())
                    return;
                int stok = rs.getInt("stok");
                if (jumlah > stok) {
                    AlertUtil.warn("Stok tidak cukup! Tersedia: " + stok);
                    return;
                }
                double harga = rs.getDouble("harga"), sub = harga * jumlah;
                keranjang.add(FXCollections.observableArrayList(
                        rs.getString("nama_produk"), String.valueOf(jumlah),
                        String.format("%,.0f", harga), String.format("%,.0f", sub)));
                totalBelanja += sub;
                lblTotal.setText("Total: Rp " + String.format("%,.0f", totalBelanja));
                tfJumlah.clear();
            } catch (Exception ex) {
                AlertUtil.error("Tambah gagal", ex.getMessage());
            }
        });

        Button btnHapusItem = new Button("🗑 Hapus Item");
        btnHapusItem.getStyleClass().add("btn-danger");
        btnHapusItem.setOnAction(e -> {
            ObservableList<String> sel = tblKeranjang.getSelectionModel().getSelectedItem();
            if (sel == null) {
                AlertUtil.warn("Pilih item!");
                return;
            }
            totalBelanja -= Double.parseDouble(sel.get(3).replace(",", ""));
            keranjang.remove(sel);
            lblTotal.setText("Total: Rp " + String.format("%,.0f", totalBelanja));
        });

        Button btnHitung = new Button("🧮 Hitung");
        btnHitung.getStyleClass().add("btn-secondary");
        btnHitung.setOnAction(e -> {
            try {
                double bayar = Double.parseDouble(tfBayar.getText().trim());
                double kembalian = bayar - totalBelanja;
                lblKembalian.setText(kembalian >= 0
                        ? "Kembalian: Rp " + String.format("%,.0f", kembalian)
                        : "⚠ Uang kurang!");
            } catch (Exception ex) {
                lblKembalian.setText("Kembalian: Rp 0");
            }
        });

        Button btnBayar = new Button("✅ Bayar");
        btnBayar.getStyleClass().add("btn-success");
        btnBayar.setOnAction(e -> {
            if (keranjang.isEmpty()) {
                AlertUtil.warn("Keranjang kosong!");
                return;
            }
            String metode = cbMetode.getValue();
            double bayar = 0;
            try {
                bayar = Double.parseDouble(tfBayar.getText().trim());
            } catch (Exception ignored) {
            }
            if ("tunai".equals(metode) && bayar < totalBelanja) {
                AlertUtil.warn("Uang bayar kurang!");
                return;
            }
            double kembalian = bayar - totalBelanja;
            try (Connection c = DB.getConnection()) {
                c.setAutoCommit(false);
                try {
                    String kode = "TRX-" + System.currentTimeMillis();
                    PreparedStatement psTrx = c.prepareStatement(
                            "INSERT INTO transaksi (id_user,kode_transaksi,tgl_transaksi,total_bayar,uang_bayar,kembalian,metode_bayar) "
                                    +
                                    "VALUES (?,?,NOW(),?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    psTrx.setInt(1, MainApp.loggedId);
                    psTrx.setString(2, kode);
                    psTrx.setDouble(3, totalBelanja);
                    psTrx.setDouble(4, bayar);
                    psTrx.setDouble(5, kembalian);
                    psTrx.setString(6, metode);
                    psTrx.executeUpdate();
                    ResultSet keys = psTrx.getGeneratedKeys();
                    keys.next();
                    int idTrx = keys.getInt(1);

                    for (ObservableList<String> row : keranjang) {
                        PreparedStatement psId = c
                                .prepareStatement("SELECT id_produk, harga FROM produk WHERE nama_produk=?");
                        psId.setString(1, row.get(0));
                        ResultSet rsId = psId.executeQuery();
                        rsId.next();
                        int idProd = rsId.getInt(1);
                        double harga = rsId.getDouble(2);
                        int jml = Integer.parseInt(row.get(1));

                        PreparedStatement psDet = c.prepareStatement(
                                "INSERT INTO detail_transaksi (id_transaksi,id_produk,jumlah,harga_satuan,subtotal) VALUES (?,?,?,?,?)");
                        psDet.setInt(1, idTrx);
                        psDet.setInt(2, idProd);
                        psDet.setInt(3, jml);
                        psDet.setDouble(4, harga);
                        psDet.setDouble(5, harga * jml);
                        psDet.executeUpdate();

                        PreparedStatement psStok = c
                                .prepareStatement("UPDATE produk SET stok=stok-? WHERE id_produk=?");
                        psStok.setInt(1, jml);
                        psStok.setInt(2, idProd);
                        psStok.executeUpdate();
                    }
                    c.commit();
                    keranjang.clear();
                    totalBelanja = 0;
                    lblTotal.setText("Total: Rp 0");
                    lblKembalian.setText("Kembalian: Rp 0");
                    tfBayar.clear();
                    loadCombo(cbProduk,
                            "SELECT id_produk, CONCAT(nama_produk,' (stok: ',stok,') | Rp ',FORMAT(harga,0)) " +
                                    "FROM produk WHERE stok>0 ORDER BY nama_produk");
                    AlertUtil.info("Transaksi berhasil!\nKode: " + kode);
                } catch (Exception ex) {
                    c.rollback();
                    throw ex;
                } finally {
                    c.setAutoCommit(true);
                }
            } catch (Exception ex) {
                AlertUtil.error("Transaksi gagal", ex.getMessage());
            }
        });

        VBox page = pageBox("Transaksi Penjualan",
                hbox(8, labeled("Produk", cbProduk), tfJumlah, btnTambah, btnHapusItem),
                tblKeranjang,
                new Separator(),
                hbox(16, lblTotal, lblKembalian),
                hbox(8, labeled("Metode", cbMetode), tfBayar, btnHitung, btnBayar));
        setContent(page);
    }

    // =========================================================================
    // RIWAYAT
    // =========================================================================

    private void showRiwayat() {
        TextField tfCari = new TextField();
        tfCari.setPromptText("Cari kode / nama kasir...");
        TableView<ObservableList<String>> tbl = new TableView<>();
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable loadData = () -> {
            String like = "%" + tfCari.getText().trim() + "%";
            try (Connection c = DB.getConnection();
                    PreparedStatement ps = c.prepareStatement(
                            "SELECT t.kode_transaksi AS Kode, u.nama AS Kasir, " +
                                    "DATE_FORMAT(t.tgl_transaksi,'%d-%m-%Y %H:%i') AS Tanggal, " +
                                    "FORMAT(t.total_bayar,0) AS Total, t.metode_bayar AS Metode, " +
                                    "FORMAT(t.uang_bayar,0) AS Bayar, FORMAT(t.kembalian,0) AS Kembalian " +
                                    "FROM transaksi t JOIN users u ON t.id_user=u.id_user " +
                                    "WHERE t.kode_transaksi LIKE ? OR u.nama LIKE ? " +
                                    "ORDER BY t.tgl_transaksi DESC")) {
                ps.setString(1, like);
                ps.setString(2, like);
                TableUtil.populate(tbl, ps.executeQuery());
            } catch (Exception e) {
                AlertUtil.error("Load riwayat gagal", e.getMessage());
            }
        };
        loadData.run();

        Button btnCari = new Button("🔍 Cari");
        btnCari.getStyleClass().add("btn-primary");
        Button btnRefresh = new Button("🔄 Refresh");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnCari.setOnAction(e -> loadData.run());
        btnRefresh.setOnAction(e -> {
            tfCari.clear();
            loadData.run();
        });
        tfCari.setOnAction(e -> loadData.run());

        VBox page = pageBox("Riwayat Transaksi",
                hbox(8, tfCari, btnCari, btnRefresh),
                new Separator(), tbl);
        VBox.setVgrow(tbl, Priority.ALWAYS);
        setContent(page);
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private void loadCombo(ComboBox<String> cb, String sql) {
        cb.getItems().clear();
        try (Connection c = DB.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                cb.getItems().add(rs.getInt(1) + "|" + rs.getString(2));
        } catch (Exception e) {
            AlertUtil.error("loadCombo gagal", e.getMessage());
        }
    }

    private int parseId(ComboBox<String> cb) {
        if (cb.getValue() == null)
            return -1;
        return Integer.parseInt(cb.getValue().split("\\|")[0]);
    }

    private void setCbByLabel(ComboBox<String> cb, String label) {
        cb.getItems().stream().filter(i -> i.contains(label)).findFirst().ifPresent(cb::setValue);
    }

    private void clearAll(TextField... fields) {
        for (TextField f : fields)
            f.clear();
    }

    private VBox pageBox(String title, javafx.scene.Node... nodes) {
        Label lbl = new Label(title);
        lbl.getStyleClass().add("page-title");
        VBox box = new VBox(10, lbl);
        box.getChildren().addAll(nodes);
        box.getStyleClass().add("page-box");
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox hbox(double spacing, javafx.scene.Node... nodes) {
        HBox box = new HBox(spacing, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        for (javafx.scene.Node n : nodes) {
            if (n instanceof TextField || n instanceof PasswordField || n instanceof ComboBox) {
                HBox.setHgrow(n, Priority.ALWAYS);
            }
        }
        return box;
    }

    private VBox labeled(String text, Control ctrl) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("field-label");
        VBox box = new VBox(2, lbl, ctrl);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static class Spacer extends Region {
        Spacer() {
            HBox.setHgrow(this, Priority.ALWAYS);
        }
    }
}