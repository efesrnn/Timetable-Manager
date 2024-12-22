package com.example.timetablemanager;

import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;

import java.io.File;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Controller class for managing classroom schedules.
 */
public class ClassroomSchedulerController {

    @FXML
    private GridPane scheduleGrid;

    private static final String DB_PATH = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH + File.separator + "TimetableManagement.db";
    private Connection conn;

    // Define days including Saturday and Sunday
    private final List<String> days = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    // Define times corresponding to the rows in the GridPane
    private final List<String> times = List.of(
            "08:30", "09:25", "10:20", "11:15", "12:10",
            "13:05", "14:00", "14:55", "15:50", "16:45", "17:40", "18:35", "19:30"
    );

    // Map to hold courseName to Color mapping
    private final Map<String, Color> courseColors = new HashMap<>();

    // 2D array to track allocated cells
    private boolean[][] allocatedCells;

    @FXML
    public void initialize() {
        // Initialize the allocatedCells array
        // rows: times.size() + 1 (including header)
        // cols: days.size() + 1 (including time column)
        allocatedCells = new boolean[times.size() + 1][days.size() + 1];

        try {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Classroom Scheduler: Connected to the database.");
            loadInitialSchedule();
        } catch (SQLException e) {
            System.err.println("Classroom Scheduler: Database connection failed - " + e.getMessage());
        }

        // Initialize the Schedule GridPane
        initializeScheduleGridPane();

        // Populate schedule with existing allocations from the database
        populateScheduleGridPane();
    }

    /**
     * Loads the initial schedule for a specific classroom.
     * Modify this method to accept classroomName dynamically if needed.
     */
    private void loadInitialSchedule() {
        String classroomName = "A101"; // Example classroom, modify as needed
        loadClassroomSchedule(classroomName);
    }

    /**
     * Loads the schedule for a given classroom from the database.
     *
     * @param classroomName The name of the classroom.
     */
    public void loadClassroomSchedule(String classroomName) {  // Ensure this method is public
        String query = "SELECT c.courseName, c.timeToStart, c.duration, c.lecturer " +
                "FROM Courses c " +
                "JOIN Allocated a ON c.courseName = a.courseName " +
                "WHERE a.classroomName = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, classroomName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String timeToStart = rs.getString("timeToStart"); // e.g., "Friday 8:30"
                int duration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                allocateCourseInSchedule(courseName, timeToStart, duration, lecturer);
            }

        } catch (SQLException e) {
            System.err.println("Classroom Scheduler: Failed to load schedule - " + e.getMessage());
        }
    }

    /**
     * Allocates a course in the schedule GridPane.
     *
     * @param courseName  The name of the course.
     * @param timeToStart The start time in "Day HH:mm" format.
     * @param duration    Duration in number of consecutive slots.
     * @param lecturer    The lecturer's name.
     */
    private void allocateCourseInSchedule(String courseName, String timeToStart, int duration, String lecturer) {
        String[] parts = timeToStart.split(" ");
        if (parts.length != 2) {
            System.err.println("Invalid timeToStart format: " + timeToStart);
            return;
        }

        String day = parts[0];
        String time = parts[1];

        // Normalize time to "HH:mm" format
        String normalizedTime;
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("H:mm");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime parsedTime = LocalTime.parse(time, inputFormatter);
            normalizedTime = parsedTime.format(outputFormatter);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid time format for timeToStart: " + timeToStart);
            return;
        }

        int colIndex = getDayColumnIndex(day);
        int rowIndex = getTimeRowIndex(normalizedTime);

        if (colIndex == 0 || rowIndex == 0) {
            System.err.println("Invalid indices for Day: " + day + ", Time: " + normalizedTime);
            return;
        }

        // Check if the label spans beyond the available rows
        if (rowIndex + duration - 1 > times.size()) {
            System.err.println("Duration exceeds available time slots for course: " + courseName);
            return;
        }

        // Check if the cells to be spanned are free
        for (int i = 0; i < duration; i++) {
            if (allocatedCells[rowIndex + i][colIndex]) {
                System.err.println("Time slot already allocated for course: " + courseName + " at " + day + " " + normalizedTime);
                return;
            }
        }

        // Assign a unique color to the course
        Color courseColor = courseColors.computeIfAbsent(courseName, this::generateColorForCourse);
        String colorHex = toRgbString(courseColor);

        // Create the allocation label
        Label allocationLabel = new Label(courseName + "\n" + lecturer);
        allocationLabel.getStyleClass().add("allocation-label");
        allocationLabel.setStyle(
                "-fx-background-color: " + colorHex + ";" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-alignment: CENTER;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5;" +  // Reduced padding to fit multiple rows
                        /* Removed border and shadow to prevent obscuring grid lines */
                        "-fx-wrap-text: true;"
        );

        // Enable the label to grow within the grid cell
        allocationLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setFillWidth(allocationLabel, true);
        GridPane.setFillHeight(allocationLabel, true);
        GridPane.setHalignment(allocationLabel, HPos.CENTER);

        // Tooltip with additional details
        String endTime = calculateEndTime(normalizedTime, duration);
        Tooltip tooltip = new Tooltip(
                "Course: " + courseName + "\n" +
                        "Lecturer: " + lecturer + "\n" +
                        "Time: " + day + " " + normalizedTime + " - " + endTime
        );
        tooltip.getStyleClass().add("tooltip");
        Tooltip.install(allocationLabel, tooltip);

        // Add the label to the GridPane
        scheduleGrid.add(allocationLabel, colIndex, rowIndex);
        GridPane.setRowSpan(allocationLabel, duration);

        // Mark the cells as allocated
        for (int i = 0; i < duration; i++) {
            allocatedCells[rowIndex + i][colIndex] = true;
        }
    }

    /**
     * Generates a unique color for a course based on its name.
     *
     * @param courseName The name of the course.
     * @return A Color object.
     */
    private Color generateColorForCourse(String courseName) {
        // Simple hash-based color generation
        int hash = courseName.hashCode() & 0xFFFFFF; // Ensure positive value

        // Using HSB for better color distribution
        double hue = (hash % 360) / 360.0; // Hue between 0.0 and 1.0
        double saturation = 0.7; // Fixed saturation
        double brightness = 0.7; // Fixed brightness

        return Color.hsb(hue * 360, saturation, brightness);
    }

    /**
     * Converts a Color object to its RGB hex string.
     *
     * @param color The Color object.
     * @return The RGB hex string.
     */
    private String toRgbString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Calculates the end time based on the start time and duration.
     *
     * @param startTime The start time in "HH:mm" format.
     * @param duration  The duration in number of consecutive slots.
     * @return The end time in "HH:mm" format.
     */
    private String calculateEndTime(String startTime, int duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        try {
            LocalTime time = LocalTime.parse(startTime, formatter);
            // Assuming each slot is 55 minutes (45 minutes class + 10 minutes break)
            LocalTime endTime = time.plusMinutes(duration * 55);
            return endTime.format(formatter);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid start time format: " + startTime);
            return startTime;
        }
    }

    /**
     * Retrieves the column index for a given day.
     *
     * @param day The day of the week.
     * @return The column index, or 0 if invalid.
     */
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
            case "saturday":
                return 6;
            case "sunday":
                return 7;
            default:
                return 0; // Invalid day
        }
    }

    /**
     * Retrieves the row index for a given time.
     *
     * @param time The time in "HH:mm" format.
     * @return The row index, or 0 if invalid.
     */
    private int getTimeRowIndex(String time) {
        int index = times.indexOf(time);
        return index >= 0 ? index + 1 : 0; // +1 to account for header row
    }

    /**
     * Closes the database connection when the controller is destroyed.
     */
    public void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Classroom Scheduler: Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Classroom Scheduler: Failed to close database connection - " + e.getMessage());
        }
    }

    /**
     * Clears all course allocations from the schedule GridPane.
     */
    private void clearScheduleGrid() {
        // Remove all allocated labels
        scheduleGrid.getChildren().removeIf(node -> {
            Integer col = GridPane.getColumnIndex(node);
            Integer row = GridPane.getRowIndex(node);
            return (col != null && col > 0) && (row != null && row > 0);
        });

        // Reset allocatedCells
        for (int row = 1; row <= times.size(); row++) {
            for (int col = 1; col <= days.size(); col++) {
                allocatedCells[row][col] = false;
            }
        }
    }

    /**
     * Refreshes the schedule by clearing and reloading allocations.
     *
     * @param classroomName The name of the classroom to refresh.
     */
    public void refreshSchedule(String classroomName) {
        clearScheduleGrid();
        loadClassroomSchedule(classroomName);
    }

    /**
     * Initializes the Schedule GridPane with days as columns and times as rows.
     */
    private void initializeScheduleGridPane() {
        // Clear any existing content
        scheduleGrid.getChildren().clear();
        scheduleGrid.getColumnConstraints().clear();
        scheduleGrid.getRowConstraints().clear();

        // Set padding and gaps
        scheduleGrid.setPadding(new Insets(10));
        scheduleGrid.setHgap(1);
        scheduleGrid.setVgap(1);

        // Define column constraints for days (+1 for time labels)
        ColumnConstraints timeColumn = new ColumnConstraints();
        timeColumn.setPrefWidth(140);
        timeColumn.setHalignment(javafx.geometry.HPos.CENTER);
        scheduleGrid.getColumnConstraints().add(timeColumn); // Column 0 for Time labels

        for (int i = 0; i < days.size(); i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPrefWidth(140);
            colConst.setHalignment(javafx.geometry.HPos.CENTER);
            scheduleGrid.getColumnConstraints().add(colConst);
        }

        // Define row constraints for time slots (+1 for day headers)
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(50); // Header Row
        headerRow.setValignment(javafx.geometry.VPos.CENTER);
        scheduleGrid.getRowConstraints().add(headerRow); // Row 0 for Day headers

        for (int i = 0; i < times.size(); i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPrefHeight(50); // Adjusted to fit within window size
            rowConst.setValignment(javafx.geometry.VPos.CENTER);
            scheduleGrid.getRowConstraints().add(rowConst);
        }

        // Add Day Headers
        scheduleGrid.add(new Label("Time"), 0, 0); // Top-left corner
        for (int col = 1; col <= days.size(); col++) {
            Label dayLabel = new Label(days.get(col - 1));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #48494a; -fx-text-fill: #FFFFFF; -fx-alignment: CENTER; -fx-padding: 15;");
            scheduleGrid.add(dayLabel, col, 0);
        }

        // Add Time Labels
        for (int row = 1; row <= times.size(); row++) {
            // Add Time Label
            Label timeLabel = new Label(times.get(row - 1) + " - " + calculateEndTime(times.get(row - 1), 1));
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555; -fx-alignment: CENTER_LEFT; -fx-padding: 10; -fx-background-color: #ffffff; -fx-border-color: #78797a; -fx-border-width: 1px 0 1px 1px;");
            scheduleGrid.add(timeLabel, 0, row);
        }

        // No pre-added empty cells to prevent conflicts with allocations
    }

    /**
     * Populates the Schedule GridPane with existing course allocations.
     */
    private void populateScheduleGridPane() {
        // Clear existing allocations
        clearScheduleGrid();

        // Reload day headers and time slots
        initializeScheduleGridPane();

        // Load courses from the database
        String classroomName = "A101"; // Modify as needed or make dynamic
        loadClassroomSchedule(classroomName);
    }

    /**
     * Retrieves a node from the GridPane based on column and row indices.
     *
     * @param col The column index.
     * @param row The row index.
     * @return The node at the specified position, or null if none exists.
     */
    private javafx.scene.Node getNodeFromGridPane(int col, int row) {
        for (javafx.scene.Node node : scheduleGrid.getChildren()) {
            Integer nodeCol = GridPane.getColumnIndex(node);
            Integer nodeRow = GridPane.getRowIndex(node);
            if (nodeCol == null) nodeCol = 0;
            if (nodeRow == null) nodeRow = 0;
            if (nodeCol == col && nodeRow == row) {
                return node;
            }
        }
        return null;
    }
}
