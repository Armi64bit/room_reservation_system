package com.example.reservation.dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseManager {

    public static Connection connect() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:reservations.db");
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }
}