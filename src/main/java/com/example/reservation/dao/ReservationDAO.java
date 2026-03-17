package com.example.reservation.dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReservationDAO {

    public void addReservation(int roomId, String event, String organizer,
                               String date, String start, String end) throws Exception {

        String sql = "INSERT INTO reservations(room_id, event_name, organizer, date, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseManager.connect();
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, roomId);
        ps.setString(2, event);
        ps.setString(3, organizer);
        ps.setString(4, date);
        ps.setString(5, start);
        ps.setString(6, end);

        ps.executeUpdate();
        conn.close();
    }

    public boolean isAvailable(int roomId, String date, String startTime, String endTime) {
        String query = "SELECT COUNT(*) AS count FROM reservations " +
                "WHERE room_id = ? AND date = ? " +
                "AND NOT (end_time <= ? OR start_time >= ?)"; // overlap check

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, roomId);
            ps.setString(2, date);
            ps.setString(3, startTime); // reservation ends before new start → ok
            ps.setString(4, endTime);   // reservation starts after new end → ok

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") == 0; // available if no conflicts
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false; // default to not available if error occurs
    }
    public int getReservationCount(int roomId) {
        String query = "SELECT COUNT(*) AS count FROM reservations WHERE room_id = ?";

        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0; // default to 0 if error occurs
    }

    public void deleteReservation(int id) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteRoom(int id) {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // -------------------- CSV IMPORT --------------------
    public void importFromCSV(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
             Connection conn = DatabaseManager.connect()) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skip header
                String[] parts = line.split(",");
                if (parts.length != 7) continue; // skip invalid lines

                int roomId = Integer.parseInt(parts[1]);
                String event = parts[2];
                String organizer = parts[3];
                String date = parts[4];
                String start = parts[5];
                String end = parts[6];

                // Check if room exists; create if missing
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS count FROM rooms WHERE id = ?")) {
                    ps.setInt(1, roomId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt("count") == 0) {
                        // Room doesn't exist → create it with default values
                        try (PreparedStatement insertRoom = conn.prepareStatement(
                                "INSERT INTO rooms(id, name, capacity, location, hasProjector) VALUES (?, ?, ?, ?, ?)")) {
                            insertRoom.setInt(1, roomId);
                            insertRoom.setString(2, "Room " + roomId);        // default name
                            insertRoom.setInt(3, 10 + roomId % 10);           // capacity 10–19
                            insertRoom.setString(4, "Floor " + ((roomId % 5) + 1)); // location
                            insertRoom.setInt(5, roomId % 2);                 // projector: 0 or 1
                            insertRoom.executeUpdate();
                            System.out.println("Created default room: " + roomId);
                        }
                    }
                }

                // Check for conflicts before adding
                if (isAvailable(roomId, date, start, end)) {
                    addReservation(roomId, event, organizer, date, start, end);
                } else {
                    System.out.println("Skipped conflicting reservation: " + event + " in room " + roomId + " on " + date);
                }
            }

            System.out.println("CSV import completed: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }   public void exportToCSV(String path) {
        String query = "SELECT * FROM reservations";
        try (Connection conn = DatabaseManager.connect();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery();
             java.io.PrintWriter writer = new java.io.PrintWriter(path)) {

            // Write CSV header
            writer.println("id,room_id,event_name,organizer,date,start_time,end_time");

            // Write rows
            while (rs.next()) {
                int id = rs.getInt("id");
                int roomId = rs.getInt("room_id");
                String event = rs.getString("event_name");
                String organizer = rs.getString("organizer");
                String date = rs.getString("date");
                String start = rs.getString("start_time");
                String end = rs.getString("end_time");

                writer.printf("%d,%d,%s,%s,%s,%s,%s%n",
                        id, roomId, event, organizer, date, start, end);
            }

            System.out.println("Reservations exported to CSV: " + path);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}