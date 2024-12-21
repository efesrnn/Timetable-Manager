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

            loadStudents();
            loadCourseData();

            deleteCourseButton.setOnAction(event -> deleteCourse());

            classroomLbl.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) { // Çift tıklama
                    String selectedClassroom = classroomLbl.getText();
                    openClassroomDetails(selectedClassroom);
                }
            });

        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
        }

        studentsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedStudent = studentsListView.getSelectionModel().getSelectedItem();
                if (selectedStudent != null) {
                    System.out.println(selectedStudent);
                    openStudentDetails(selectedStudent);


                }
            }
        });


    }




    private void openClassroomDetails(String classroomName) {
        try {
            // ClassroomScheduler FXML dosyasını yükle
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/ClassroomSchedulerLayout.fxml"));
            Parent root = loader.load();

            // Kontrolcüyü al ve seçilen sınıf bilgilerini gönder
            ClassroomSchedulerController classroomController = loader.getController();
            classroomController.loadClassroomSchedule(classroomName);

            // Yeni bir pencere (Stage) aç
            Stage stage = new Stage();
            stage.setTitle("Classroom Schedule - " + classroomName);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Sınıf detayları penceresi açılamadı: " + e.getMessage());
        }
    }

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

    private void loadCourseData() {
        // Query: Join Courses and Classrooms tables to get the required data
        String query = "SELECT c.courseName, c.lecturer, c.timeToStart, c.duration, cl.classroomName, cl.capacity " +
                "FROM Courses c " +
                "JOIN Allocated a ON c.courseName = a.courseName " +
                "JOIN Classrooms cl ON a.classroomName = cl.classroomName " +
                "LIMIT 1";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                // Get course and classroom data
                String courseName = rs.getString("courseName");
                String lecturer = rs.getString("lecturer");
                String startTime = rs.getString("timeToStart");
                String duration = rs.getString("duration");
                String classroomName = rs.getString("classroomName");
                int capacity = rs.getInt("capacity");

                // Set course data to labels
                courseLbl.setText(courseName);
                lecturerLbl.setText(lecturer);
                startTimeLbl.setText(startTime);
                durationLbl.setText(duration);
                classroomLbl.setText(classroomName);
                capacityLbl.setText(String.valueOf(capacity));
            }
        } catch (SQLException e) {
            System.err.println("Error loading course data: " + e.getMessage());
        }
    }

    public void loadStudents() {
        String query = "SELECT studentName FROM Students";

        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                studentsListView.getItems().add(rs.getString("studentName"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
                try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
                    pstmt.setString(1, courseName);
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        showAlert(Alert.AlertType.INFORMATION, "Successful", "Course successfully deleted.");
                        refreshTimetableView();
                        closeCourseScheduler();
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Delete Error", "Course not found or could not be deleted.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while deleting the course.");
                }
            }
        });
    }

    private void refreshTimetableView() {
        if (mainController != null) {
            mainController.refreshTable(); // Call the refreshTable method in the main controller


        }
    }


    private void closeCourseScheduler() {
        // Get the current stage (window) and close it
        Stage currentStage = (Stage) deleteCourseButton.getScene().getWindow();
        currentStage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }






  /*  private void openClassroomDetails(String classroomName) {
        try {
            // Load the ClassroomScheduler FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/classroomSchedulerLayout.fxml"));  // Correct path to your fxml
            Parent root = loader.load();

            // Get the controller
            ClassroomSchedulerController classroomController = loader.getController();
            // classroomController.setClassroomDetails(classroomName);  // Pass classroom name to load details

            // Show the new window (stage)
            Stage stage = new Stage();
            stage.setTitle("Classroom Scheduler");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
*/

    private void openStudentDetails(String studentName) {
        try {
            // Load the StudentScheduler FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/studentSchedulerController.fxml"));
            Parent root = loader.load();

            // Get the controller
            studentSchedulerController studentController = loader.getController();
            studentController.setSelectedStudent(studentName);
            studentController.showStudent();
            studentController.setController(this);



            // Show the new window (stage)
            Stage stage = new Stage();
            stage.setTitle("Student Scheduler");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}






