package com.example.timetablemanager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClassroomSchedulerController {

    @FXML
    private GridPane scheduleGrid;

    private static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String DB_URL = "jdbc:sqlite:" + dbPath + File.separator + "TimetableManagement.db";
    private Connection conn;

    public void loadClassroomSchedule(String classroomName) {
        String query = "SELECT c.timeToStart, a.classroomName " +
                "FROM Allocated a " +
                "JOIN Courses c ON a.courseName = c.courseName " +
                "WHERE a.classroomName = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, classroomName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String timeToStart = rs.getString("timeToStart");  // Example: "Friday 8:30"
                String classroom = rs.getString("classroomName");

                System.out.println("Fetched Data -> timeToStart: " + timeToStart + ", classroom: " + classroom);

                // Parse timeToStart
                String[] timeParts = timeToStart.split(" ");
                if (timeParts.length != 2) {
                    System.err.println("Invalid timeToStart format: " + timeToStart);
                    continue;
                }

                String day = timeParts[0];
                String time = timeParts[1];

                int colIndex = getDayColumnIndex(day);
                int rowIndex = getTimeRowIndex(time);

                System.out.println("Parsed Indices -> Row: " + rowIndex + ", Column: " + colIndex);

                if (colIndex == 0 || rowIndex == 0) {
                    System.err.println("Invalid indices for Row: " + rowIndex + ", Column: " + colIndex);
                    continue;
                }

                Label classLabel = new Label(classroom);
                classLabel.setStyle(
                        "-fx-background-color: #CCCC66; " +
                                "-fx-padding: 10; " +
                                "-fx-font-size: 18px; " +
                                "-fx-font-weight: normal; " +
                                "-fx-text-fill: #FFFFFF; " +
                                "-fx-wrap-text: true; " +
                                "-fx-alignment: center;");

                classLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Etiketi GridPane'e sığdırma
                scheduleGrid.add(classLabel, colIndex, rowIndex); // Label'ı GridPane'e ekleme

            }

        } catch (SQLException e) {
            System.err.println("Error loading schedule data: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Classroom Scheduler: Veritabanına bağlanıldı.");
            scheduleGrid.setGridLinesVisible(true); // Grid çizgilerini görünür yap
        } catch (SQLException e) {
            System.err.println("Veritabanı bağlantısı başarısız: " + e.getMessage());
        }
    }

    private int getDayColumnIndex(String day) {
        switch (day.toLowerCase()) {
            case "monday":
                return 1;
            case "tuesday":
                return 2;
            case "wednesday":
                return 3;
            case "thursday":
                return 4;
            case "friday":
                return 5;
            default:
                return 0;  // In case there's an unexpected day
        }
    }

    private int getTimeRowIndex(String time) {
        switch (time) {
            case "08:30":
                return 1;
            case "09:25":
                return 2;
            case "10:20":
                return 3;
            case "11:15":
                return 4;
            case "12:10":
                return 5;
            case "13:05":
                return 6;
            case "14:00":
                return 7;
            case "14:55":
                return 8;
            case "15:50":
                return 9;
            case "16:45":
                return 10;
            case "17:40":
                return 11;
            default:
                return 0;  // Default to first row if time is unexpected
        }
    }
}