package com.example.timetablemanager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class studentSchedulerController {

    @FXML
    private GridPane schedulerGrid;

    private List<Course> allCourses;

    private String selectedStudent;

    private Course selectedCourse;
    private CourseSchedulerController controller;

    @FXML
    public void initialize() {
        // Initialize scheduler when controller is loaded
        showStudent();
    }

    public void showStudent() {
        allCourses = Database.getAllCourses();

        List<Course> enrolledCourses = Database.loadCoursesForStudent1(getSelectedStudent());

        if (enrolledCourses == null || enrolledCourses.isEmpty()) {
            System.out.println("No enrolled courses found.");
            return;
        } else {
            System.out.println("Enrolled courses loaded.");
        }

        for (Course course : enrolledCourses) {
            System.out.println("Course Time: " + course.getTimeToStart());
        }

        for (Course course : enrolledCourses) {

            Label classLabel = new Label(course.getCourseID());

            Course selectedCourseObject = allCourses.stream()
                    .filter(e -> e.getCourseID().equals(course.getCourseID()))
                    .findFirst().orElse(null);

            if (selectedCourseObject == null) {
                showAlert("Error", "Invalid selection. Please try again.");
                continue; // Continue to next course instead of returning
            }

            int duration = selectedCourseObject.getDuration();
            String timeToStart = selectedCourseObject.getTimeToStart();

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

            classLabel.setStyle(
                    "-fx-background-color: #CCCC66; " +
                            "-fx-padding: 10; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: normal; " +
                            "-fx-text-fill: #FFFFFF; " +
                            "-fx-wrap-text: true; " +
                            "-fx-alignment: center;");

            classLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Fit Label within GridPane
            schedulerGrid.add(classLabel, colIndex, rowIndex); // Add Label to GridPane
            GridPane.setRowSpan(classLabel, duration); // Span rows based on duration

            classLabel.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    // Prompt withdrawal confirmation
                    Delete("Warning", "Are you sure you want to withdraw from " + course.getCourseID() + "?", course.getCourseID());
                }
            });
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
            case "saturday":
                return 6;
            case "sunday":
                return 7;
            default:
                System.err.println("Unrecognized day: " + day);
                return 0;  // Invalid day
        }
    }

    private int getTimeRowIndex(String time) {
        switch (time) {
            case "8:30":
                return 1;
            case "9:25":
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
                System.err.println("Unrecognized time: " + time);
                return 0;  // Invalid time
        }
    }

    // Getter and Setter methods
    public Course getSelectedCourse() {
        return selectedCourse;
    }

    public void setSelectedCourse(Course selectedCourse) {
        this.selectedCourse = selectedCourse;
    }

    public String getSelectedStudent() {
        return selectedStudent;
    }

    public void setSelectedStudent(String selectedStudent) {
        this.selectedStudent = selectedStudent;
    }

    // Alert utility method
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Refresh the GridPane to reflect changes
    private void refreshGridPane() {
        schedulerGrid.getChildren().clear();
        this.showStudent();
    }

    // Reference to the main controller
    public void setController(CourseSchedulerController controller) {
        this.controller = controller;
    }

    // Delete (Withdraw) method with confirmation
    private void Delete(String title, String message, String courseId) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType withdrawButton = new ButtonType("Withdraw");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(withdrawButton, cancelButton);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == withdrawButton) {
                Database.removeStudentFromCourse(courseId, getSelectedStudent());
                alert.close();
                Stage stage = (Stage) schedulerGrid.getScene().getWindow();

                // Reload courses
                allCourses.clear();
                Database.reloadCourses();
                allCourses = Database.getAllCourses();

                Course selectedCourseObject = allCourses.stream()
                        .filter(e -> e.getCourseID().equals(courseId))
                        .findFirst().orElse(null);

                if (controller != null && selectedCourseObject != null) {
                    controller.setCourseData(controller.getMainController().getSelectedCourse());
                }

                // Refresh the scheduler view
               // refreshGridPane();
                stage.close();
            } else if (result.get() == cancelButton) {
                alert.close();
            }
        }
    }
}
