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
    private ListView<String> studentsListView;

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

        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
        }

     /*   studentsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedStudent = studentsListView.getSelectionModel().getSelectedItem();
                if (selectedStudent != null) {
                    openStudentDetails(selectedStudent);
                }
            }
        });

    */

    /*
        // Add double-click event to classroomLbl (Label)
        classroomLbl.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {  // Check if double click
                String selectedClassroom = classroomLbl.getText();
                openClassroomDetails(selectedClassroom);  // Open Classroom Details
            }
        });
    */
    }

    public void setCourseData(Course course) {
        courseLbl.setText(course.getCourseName());
        lecturerLbl.setText(course.getLecturer());
        startTimeLbl.setText(String.join(", ", course.getTimes()));
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

 /*   private void openStudentDetails(String studentName) {
        try {
            // Load the StudentScheduler FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/studentSchedulerLayout.fxml"));
            Parent root = loader.load();

            // Get the controller
            StudentSchedulerController studentController = loader.getController();
          //  studentController.setStudentDetails(studentName);  // Pass classroom name to load details

            // Show the new window (stage)
            Stage stage = new Stage();
            stage.setTitle("Student Scheduler");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  */






