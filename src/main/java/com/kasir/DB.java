package com.kasir;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Helper koneksi MySQL.
 * Sesuaikan URL, USER, PASS dengan konfigurasi lokal Anda.
 */
public class DB {
    private static final String URL = "jdbc:mysql://localhost:3306/kasir_swalayan_kel7?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    private DB() {
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}