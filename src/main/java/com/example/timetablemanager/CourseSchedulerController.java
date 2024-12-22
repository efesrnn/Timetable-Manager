package com.example.timetablemanager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class CourseSchedulerController {

    @FXML
    private Label courseLbl, lecturerLbl, startTimeLbl, capacityLbl, classroomLbl, durationLbl;

    @FXML
    private Button deleteCourseButton;

    @FXML
    private Button backButton;

    @FXML
    private ListView<String> studentsListView;

    private ttManagerController mainController;  // Declare a reference to the main controller

    public void setMainController(ttManagerController mainController) {
        this.mainController = mainController;
    }

    private static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String DB_URL = "jdbc:sqlite:" + dbPath + File.separator + "TimetableManagement.db";
    private Connection conn = null;

    @FXML
    public void initialize() {

        try {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to the database.");

            loadCourseData();
            loadStudents();

            deleteCourseButton.setOnAction(event -> deleteCourse());

            // Double-click on classroom label to open Classroom Scheduler
            classroomLbl.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) { // Double-click
                    String selectedClassroom = classroomLbl.getText();
                    openClassroomDetails(selectedClassroom);
                }
            });

            // Double-click on student to open Student Details
            studentsListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String selectedStudent = studentsListView.getSelectionModel().getSelectedItem();
                    if (selectedStudent != null) {
                        System.out.println(selectedStudent);
                        openStudentDetails(selectedStudent);
                    }
                }
            });

            // Handle Back Button
            backButton.setOnAction(event -> handleBackButton());

        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Connection Error", "Failed to connect to the database.");
        }
    }

    /**
     * Opens the Classroom Scheduler view for the selected classroom.
     *
     * @param classroomName The name of the selected classroom.
     */
    private void openClassroomDetails(String classroomName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/ClassroomSchedulerLayout.fxml"));
            Parent root = loader.load();

            ClassroomSchedulerController classroomController = loader.getController();
            classroomController.loadClassroomSchedule(classroomName); // Ensure method is public

            Stage stage = new Stage();
            stage.setTitle("Classroom Schedule - " + classroomName);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open Classroom Scheduler.");
            e.printStackTrace();
        } catch (NullPointerException e) {
            showAlert(Alert.AlertType.ERROR, "Controller Error", "ClassroomSchedulerController is not set correctly.");
            e.printStackTrace();
        }
    }

    /**
     * Opens the Student Scheduler view for the selected student.
     *
     * @param studentName The name of the selected student.
     */
    private void openStudentDetails(String studentName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/studentSchedulerController.fxml"));
            Parent root = loader.load();

            studentSchedulerController studentController = loader.getController();
            studentController.setSelectedStudent(studentName);
            studentController.showStudent();
            //studentController.setController(this); // If needed

            Stage stage = new Stage();
            stage.setTitle("Student Scheduler - " + studentName);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open Student Scheduler.");
            e.printStackTrace();
        } catch (NullPointerException e) {
            showAlert(Alert.AlertType.ERROR, "Controller Error", "StudentSchedulerController is not set correctly.");
            e.printStackTrace();
        }
    }

    /**
     * Sets course data to the UI elements.
     *
     * @param course The course object containing data.
     */
    public void setCourseData(Course course) {
        courseLbl.setText(course.getCourseID());
        lecturerLbl.setText(course.getLecturer());
        startTimeLbl.setText(String.join(", ", course.getTimeToStart()));
        capacityLbl.setText(String.valueOf(course.getCapacity()));
        classroomLbl.setText(course.getClassroom());
        durationLbl.setText(String.valueOf(course.getDuration()));

        studentsListView.getItems().clear();
        for (Student s : course.getStudents()) {
            studentsListView.getItems().add(s.getFullName());
        }
    }

    /**
     * Loads course data from the database and sets it to the UI.
     */
    private void loadCourseData() {
        // Query: Join Courses and Classrooms tables to get the required data
        String query = "SELECT c.courseName, c.lecturer, c.timeToStart, c.duration, cl.classroomName, cl.capacity " +
                "FROM Courses c " +
                "JOIN Allocated a ON c.courseName = a.courseName " +
                "JOIN Classrooms cl ON a.classroomName = cl.classroomName " +
                "LIMIT 1"; // Adjust LIMIT as needed

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                // Get course and classroom data
                String courseName = rs.getString("courseName");
                String lecturer = rs.getString("lecturer");
                String startTime = rs.getString("timeToStart");
                int duration = rs.getInt("duration");
                String classroomName = rs.getString("classroomName");
                int capacity = rs.getInt("capacity");

                // Set course data to labels
                courseLbl.setText(courseName);
                lecturerLbl.setText(lecturer);
                startTimeLbl.setText(startTime);
                durationLbl.setText(String.valueOf(duration));
                classroomLbl.setText(classroomName);
                capacityLbl.setText(String.valueOf(capacity));
            } else {
                showAlert(Alert.AlertType.INFORMATION, "No Data", "No course data found.");
            }
        } catch (SQLException e) {
            System.err.println("Error loading course data: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load course data.");
        }
    }

    /**
     * Loads the list of students from the database and displays them.
     */
    public void loadStudents() {
        String query = "SELECT studentName FROM Students";

        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                studentsListView.getItems().add(rs.getString("studentName"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading students: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load students.");
        }
    }

    /**
     * Deletes the currently displayed course from the database.
     */
    private void deleteCourse() {
        String courseName = courseLbl.getText();
        if (courseName == null || courseName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Delete Error", "No course selected for deletion.");
            return;
        }

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Delete Course Confirmation");
        confirmationDialog.setHeaderText("Are you sure you want to delete this course?");
        confirmationDialog.setContentText("Course: " + courseName);

        confirmationDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Delete course from database
                String deleteQuery = "DELETE FROM Courses WHERE courseName = ?";
                String deleteAllocationQuery = "DELETE FROM Allocated WHERE courseName = ?";
                String deleteEnrollmentsQuery = "DELETE FROM Enrollments WHERE courseName = ?";

                try (PreparedStatement pstmtCourse = conn.prepareStatement(deleteQuery);
                     PreparedStatement pstmtAllocation = conn.prepareStatement(deleteAllocationQuery);
                     PreparedStatement pstmtEnrollments = conn.prepareStatement(deleteEnrollmentsQuery)) {

                    // Start transaction
                    conn.setAutoCommit(false);

                    // Delete from Enrollments
                    pstmtEnrollments.setString(1, courseName);
                    pstmtEnrollments.executeUpdate();

                    // Delete from Allocated
                    pstmtAllocation.setString(1, courseName);
                    pstmtAllocation.executeUpdate();

                    // Delete from Courses
                    pstmtCourse.setString(1, courseName);
                    int rowsAffected = pstmtCourse.executeUpdate();

                    if (rowsAffected > 0) {
                        // Commit transaction
                        conn.commit();
                        showAlert(Alert.AlertType.INFORMATION, "Successful", "Course successfully deleted.");
                        refreshTimetableView();
                        closeCourseScheduler();
                    } else {
                        conn.rollback();
                        showAlert(Alert.AlertType.WARNING, "Delete Error", "Course not found or could not be deleted.");
                    }

                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("Failed to rollback transaction: " + ex.getMessage());
                    }
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while deleting the course.");
                } finally {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException e) {
                        System.err.println("Failed to reset auto-commit: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Refreshes the timetable view in the main controller.
     */
    private void refreshTimetableView() {
        if (mainController != null) {
            mainController.refreshTable(); // Call the refreshTable method in the main controller
        } else {
            System.err.println("Main controller reference is null.");
        }
    }

    /**
     * Closes the current Course Scheduler window.
     */
    private void closeCourseScheduler() {
        // Get the current stage (window) and close it
        Stage currentStage = (Stage) deleteCourseButton.getScene().getWindow();
        currentStage.close();
    }

    /**
     * Handles the back button action.
     */
    @FXML
    private void handleBackButton() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to close the current screen.");
        }
    }

    /**
     * Displays an alert dialog with the specified type, title, and message.
     *
     * @param alertType The type of the alert.
     * @param title     The title of the alert.
     * @param message   The content message of the alert.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);

        // Optionally, set an icon if desired
        /*
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon");
            e.printStackTrace();
        }
        */

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
