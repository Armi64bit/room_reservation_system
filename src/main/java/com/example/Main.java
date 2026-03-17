package com.example;

import com.example.reservation.dao.DatabaseManager;
import com.example.reservation.dao.ReservationDAO;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main extends Application {

    private TableView<Room> roomTable;
    private ObservableList<Room> rooms;

    private TableView<Reservation> reservationTable;
    private ObservableList<Reservation> reservations;

    private ReservationDAO reservationDAO;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        reservationDAO = new ReservationDAO();

        createTables();
        insertDefaultRoom();

        rooms = FXCollections.observableArrayList();
        reservations = FXCollections.observableArrayList();

        roomTable = new TableView<>();
        reservationTable = new TableView<>();

        setupRoomTable();
        setupReservationTable();

        // Buttons
        Button addRoomBtn = new Button("Add Room");
        addRoomBtn.setOnAction(e -> showAddRoomDialog());

        Button deleteRoomBtn = new Button("Delete Room");
        deleteRoomBtn.setOnAction(e -> {
            Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
            if (selectedRoom != null) {
                deleteRoom(selectedRoom.getId());
                refreshRoomTable();
                reservations.clear();
            }
        });
        Button importCsvBtn = new Button("Import CSV");
        importCsvBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Import CSV");
            dialog.setHeaderText("Enter CSV file path to import:");
            dialog.setContentText("File Path:");
            dialog.showAndWait().ifPresent(path -> {
                reservationDAO.importFromCSV(path);
                Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
                if (selectedRoom != null) refreshReservations(selectedRoom.getId());
                refreshRoomTable();
            });
        });

        Button exportCsvBtn = new Button("Export CSV");
        exportCsvBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Export CSV");
            dialog.setHeaderText("Enter CSV file path to export:");
            dialog.setContentText("File Path:");
            dialog.showAndWait().ifPresent(path -> {
                reservationDAO.exportToCSV(path);
            });
        });
        Button addReservationBtn = new Button("Add Reservation");
        addReservationBtn.setOnAction(e -> showAddReservationDialog());

        Button deleteReservationBtn = new Button("Delete Reservation");
        deleteReservationBtn.setOnAction(e -> {
            Reservation selected = reservationTable.getSelectionModel().getSelectedItem();
            Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
            if (selected != null && selectedRoom != null) {
                reservationDAO.deleteReservation(selected.getId());
                refreshReservations(selectedRoom.getId());
                refreshRoomTable();
            }
        });
        Button editRoomBtn = new Button("Edit Room");
        editRoomBtn.setOnAction(e -> {
            Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
            if (selectedRoom != null) showEditRoomDialog(selectedRoom);
        });

        Button editReservationBtn = new Button("Edit Reservation");
        editReservationBtn.setOnAction(e -> {
            Reservation selectedRes = reservationTable.getSelectionModel().getSelectedItem();
            Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
            if (selectedRes != null && selectedRoom != null)
                showEditReservationDialog(selectedRes, selectedRoom.getId());
        });
        HBox roomButtons = new HBox(10, addRoomBtn, deleteRoomBtn,editRoomBtn,importCsvBtn, exportCsvBtn);
        HBox reservationButtons = new HBox(10, addReservationBtn, deleteReservationBtn);
        roomButtons.setPadding(new Insets(5));
        reservationButtons.setPadding(new Insets(5));

        VBox leftPane = new VBox(10, roomTable, roomButtons);
        importCsvBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select CSV File to Import");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File selectedFile = fileChooser.showOpenDialog(stage); // 'stage' is your primary Stage
            if (selectedFile != null) {
                reservationDAO.importFromCSV(selectedFile.getAbsolutePath());
                Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
                if (selectedRoom != null) refreshReservations(selectedRoom.getId());
                refreshRoomTable();
            }
        });

        exportCsvBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Location to Save CSV");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fileChooser.setInitialFileName("reservations.csv");
            File selectedFile = fileChooser.showSaveDialog(stage); // 'stage' is your primary Stage
            if (selectedFile != null) {
                reservationDAO.exportToCSV(selectedFile.getAbsolutePath());
            }
        });
        VBox rightPane = new VBox(10, new Label("Reservations Preview"), reservationTable, reservationButtons);
        HBox root = new HBox(10, leftPane, rightPane);
        root.setPadding(new Insets(10));
        leftPane.setPrefWidth(500);
        rightPane.setPrefWidth(400);

        refreshRoomTable();

        Scene scene = new Scene(root, 950, 500);
        stage.setScene(scene);
        stage.setTitle("Room Reservation System");
        stage.show();

        // Update reservation preview on room selection
        roomTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) refreshReservations(newSel.getId());
            else reservations.clear();
        });
    }
    private void showEditRoomDialog(Room room) {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Edit Room: " + room.getName());

        TextField nameField = new TextField(room.getName());
        TextField capacityField = new TextField(String.valueOf(room.getCapacity()));
        TextField locationField = new TextField(room.getLocation());
        CheckBox projectorChk = new CheckBox("Has Projector");
        projectorChk.setSelected(room.hasProjector());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Capacity:"), capacityField);
        grid.addRow(2, new Label("Location:"), locationField);
        grid.addRow(3, projectorChk);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == saveBtn) {
                try (Connection conn = DatabaseManager.connect()) {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE rooms SET name = ?, capacity = ?, location = ?, hasProjector = ? WHERE id = ?"
                    );
                    ps.setString(1, nameField.getText());
                    ps.setInt(2, Integer.parseInt(capacityField.getText()));
                    ps.setString(3, locationField.getText());
                    ps.setInt(4, projectorChk.isSelected() ? 1 : 0);
                    ps.setInt(5, room.getId());
                    ps.executeUpdate();
                    refreshRoomTable();
                } catch (Exception e) { e.printStackTrace(); }
            }
            return null;
        });

        dialog.showAndWait();
    }
    private void showEditReservationDialog(Reservation res, int roomId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Reservation: " + res.getEvent());

        TextField eventField = new TextField(res.getEvent());
        TextField organizerField = new TextField(res.getOrganizer());
        DatePicker datePicker = new DatePicker(java.time.LocalDate.parse(res.getDate()));

        ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 8; h <= 20; h++) {
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        ComboBox<String> startCombo = new ComboBox<>(times);
        startCombo.setValue(res.getStart());
        ComboBox<String> endCombo = new ComboBox<>(times);
        endCombo.setValue(res.getEnd());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Event:"), eventField);
        grid.addRow(1, new Label("Organizer:"), organizerField);
        grid.addRow(2, new Label("Date:"), datePicker);
        grid.addRow(3, new Label("Start Time:"), startCombo);
        grid.addRow(4, new Label("End Time:"), endCombo);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == saveBtn) {
                try {
                    if (!reservationDAO.isAvailable(roomId,
                            datePicker.getValue().toString(),
                            startCombo.getValue(),
                            endCombo.getValue())) {
                        new Alert(Alert.AlertType.WARNING, "Conflict with existing reservation!").showAndWait();
                        return null;
                    }
                    reservationDAO.deleteReservation(res.getId()); // delete old
                    reservationDAO.addReservation(roomId,
                            eventField.getText(),
                            organizerField.getText(),
                            datePicker.getValue().toString(),
                            startCombo.getValue(),
                            endCombo.getValue()); // insert updated
                    refreshReservations(roomId);
                    refreshRoomTable();
                } catch (Exception e) { e.printStackTrace(); }
            }
            return null;
        });

        dialog.showAndWait();
    }
    // ------------------- ROOM TABLE -------------------
    private void setupRoomTable() {
        TableColumn<Room, String> nameCol = new TableColumn<>("Room Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Room, Integer> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCapacity()).asObject());

        TableColumn<Room, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));

        TableColumn<Room, String> projectorCol = new TableColumn<>("Has Projector");
        projectorCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().hasProjector() ? "Yes" : "No"));

        TableColumn<Room, Integer> reservationsCol = new TableColumn<>("#Reservations");
        reservationsCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getReservationCount()).asObject());

        roomTable.getColumns().addAll(nameCol, capacityCol, locationCol, projectorCol, reservationsCol);
        roomTable.setItems(rooms);
    }

    private void refreshRoomTable() {
        rooms.clear();
        try (Connection conn = DatabaseManager.connect()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM rooms");
            while (rs.next()) {
                int roomId = rs.getInt("id");
                int count = reservationDAO.getReservationCount(roomId);
                rooms.add(new Room(
                        roomId,
                        rs.getString("name"),
                        rs.getInt("capacity"),
                        rs.getString("location"),
                        rs.getInt("hasProjector") == 1,
                        count
                ));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showAddRoomDialog() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add Room");

        TextField nameField = new TextField();
        TextField capacityField = new TextField();
        TextField locationField = new TextField();
        CheckBox projectorChk = new CheckBox("Has Projector");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Capacity:"), capacityField);
        grid.addRow(2, new Label("Location:"), locationField);
        grid.addRow(3, projectorChk);

        dialog.getDialogPane().setContent(grid);
        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == addBtn) {
                if (nameField.getText().isEmpty() || capacityField.getText().isEmpty() || locationField.getText().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "All fields must be filled!");
                    alert.showAndWait();
                    return null;
                }
                try {
                    int cap = Integer.parseInt(capacityField.getText());
                    addRoom(nameField.getText(), cap, locationField.getText(), projectorChk.isSelected());
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Capacity must be a number!").showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
        refreshRoomTable();
    }

    private void addRoom(String name, int capacity, String location, boolean hasProjector) {
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO rooms(name, capacity, location, hasProjector) VALUES (?, ?, ?, ?)"
            );
            ps.setString(1, name);
            ps.setInt(2, capacity);
            ps.setString(3, location);
            ps.setInt(4, hasProjector ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deleteRoom(int roomId) {
        try { reservationDAO.deleteRoom(roomId); } catch (Exception e) { e.printStackTrace(); }
    }

    // ------------------- RESERVATION TABLE -------------------
    private void setupReservationTable() {
        TableColumn<Reservation, String> eventCol = new TableColumn<>("Event");
        eventCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEvent()));

        TableColumn<Reservation, String> organizerCol = new TableColumn<>("Organizer");
        organizerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrganizer()));

        TableColumn<Reservation, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate()));

        TableColumn<Reservation, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStart() + " - " + data.getValue().getEnd()));

        reservationTable.getColumns().addAll(eventCol, organizerCol, dateCol, timeCol);
        reservationTable.setItems(reservations);
    }

    private void refreshReservations(int roomId) {
        reservations.clear();
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM reservations WHERE room_id = ? ORDER BY date, start_time"
            );
            ps.setInt(1, roomId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                reservations.add(new Reservation(
                        rs.getInt("id"),
                        rs.getString("event_name"),
                        rs.getString("organizer"),
                        rs.getString("date"),
                        rs.getString("start_time"),
                        rs.getString("end_time")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddReservationDialog() {
        Room selectedRoom = roomTable.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            new Alert(Alert.AlertType.WARNING, "Select a room first!").showAndWait();
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Reservation for " + selectedRoom.getName());

        TextField eventField = new TextField();
        eventField.setPromptText("Event");
        TextField organizerField = new TextField();
        organizerField.setPromptText("Organizer");
        DatePicker datePicker = new DatePicker();

        // Preloaded time slots
        ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 8; h <= 20; h++) {
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        ComboBox<String> startCombo = new ComboBox<>(times);
        startCombo.setPromptText("Start Time");
        ComboBox<String> endCombo = new ComboBox<>(times);
        endCombo.setPromptText("End Time");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("Event:"), eventField);
        grid.addRow(1, new Label("Organizer:"), organizerField);
        grid.addRow(2, new Label("Date:"), datePicker);
        grid.addRow(3, new Label("Start Time:"), startCombo);
        grid.addRow(4, new Label("End Time:"), endCombo);

        dialog.getDialogPane().setContent(grid);
        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == addBtn) {
                if (eventField.getText().isEmpty() || organizerField.getText().isEmpty()
                        || datePicker.getValue() == null || startCombo.getValue() == null || endCombo.getValue() == null) {
                    new Alert(Alert.AlertType.WARNING, "All fields must be filled!").showAndWait();
                    return null;
                }
                try {
                    boolean available = reservationDAO.isAvailable(selectedRoom.getId(),
                            datePicker.getValue().toString(),
                            startCombo.getValue(),
                            endCombo.getValue());
                    if (!available) {
                        new Alert(Alert.AlertType.WARNING, "Conflict with existing reservation!").showAndWait();
                        return null;
                    }
                    reservationDAO.addReservation(selectedRoom.getId(),
                            eventField.getText(),
                            organizerField.getText(),
                            datePicker.getValue().toString(),
                            startCombo.getValue(),
                            endCombo.getValue());
                    refreshReservations(selectedRoom.getId());
                    refreshRoomTable();
                } catch (Exception e) { e.printStackTrace(); }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ------------------- DATABASE -------------------
    private void createTables() throws Exception {
        try (Connection conn = DatabaseManager.connect()) {
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS rooms (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT," +
                            "capacity INTEGER," +
                            "location TEXT," +
                            "hasProjector INTEGER)"
            );
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS reservations (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "room_id INTEGER," +
                            "event_name TEXT," +
                            "organizer TEXT," +
                            "date TEXT," +
                            "start_time TEXT," +
                            "end_time TEXT," +
                            "FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE CASCADE)"
            );
        }
    }

    private void insertDefaultRoom() throws Exception {
        try (Connection conn = DatabaseManager.connect()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS count FROM rooms");
            if (rs.next() && rs.getInt("count") == 0) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO rooms(name, capacity, location, hasProjector) VALUES (?, ?, ?, ?)"
                );
                ps.setString(1, "Conference Room A");
                ps.setInt(2, 20);
                ps.setString(3, "1st Floor");
                ps.setInt(4, 1);
                ps.executeUpdate();
            }
        }
    }

    // ------------------- MODELS -------------------
    public static class Room {
        private final int id;
        private final String name;
        private final int capacity;
        private final String location;
        private final boolean hasProjector;
        private final int reservationCount;
        public Room(int id, String name, int capacity, String location, boolean hasProjector, int reservationCount) {
            this.id = id; this.name = name; this.capacity = capacity; this.location = location;
            this.hasProjector = hasProjector; this.reservationCount = reservationCount;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public int getCapacity() { return capacity; }
        public String getLocation() { return location; }
        public boolean hasProjector() { return hasProjector; }
        public int getReservationCount() { return reservationCount; }
    }

    public static class Reservation {
        private final int id;
        private final String event;
        private final String organizer;
        private final String date;
        private final String start;
        private final String end;
        public Reservation(int id, String event, String organizer, String date, String start, String end) {
            this.id = id; this.event = event; this.organizer = organizer; this.date = date; this.start = start; this.end = end;
        }
        public int getId() { return id; }
        public String getEvent() { return event; }
        public String getOrganizer() { return organizer; }
        public String getDate() { return date; }
        public String getStart() { return start; }
        public String getEnd() { return end; }
    }
}