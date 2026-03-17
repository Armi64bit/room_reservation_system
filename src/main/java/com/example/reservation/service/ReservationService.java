package com.example.reservation.service;

import com.example.reservation.dao.DatabaseManager;
import java.sql.*;

public class ReservationService {

    public boolean isAvailable(int roomId, String date, String start, String end) throws Exception {

        String sql = "SELECT * FROM reservations WHERE room_id = ? AND date = ?";
        Connection conn = DatabaseManager.connect();

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, roomId);
        ps.setString(2, date);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String s = rs.getString("start_time");
            String e = rs.getString("end_time");

            if (!(end.compareTo(s) <= 0 || start.compareTo(e) >= 0)) {
                return false;
            }
        }
        return true;
    }
}