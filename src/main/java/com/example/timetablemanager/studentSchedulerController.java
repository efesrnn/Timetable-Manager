package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import static com.example.timetablemanager.Database.*;
import static com.example.timetablemanager.studentSelectionController.*;
import java.util.List;
import java.util.Optional;


public class studentSchedulerController {

    @FXML
    private GridPane schedulerGrid;

    private List<Student> allStudents;

    private List<Course> allCourses;

    private String selectedStudent;

    private Course selectedCourse;

    public studentSchedulerController(String selectedStudent) {
        this.selectedStudent = selectedStudent;
    }

    @FXML
    public void initialize() {

    }
    public void showStudent() {
        allCourses = Database.getAllCourses();

        schedulerGrid.setGridLinesVisible(true);

        List<Course> enrolledCourses2  = Database.loadCoursesofStudents(getSelectedStudent());

        if(enrolledCourses2==null) {
            System.out.println("noş");
        } else {
            System.out.println("değikl");
        }

        for (Course course : enrolledCourses2) {

            Label classLabel = new Label(course.getCourseID());
            setSelectedCourse(course);

            Course selectedCourseObject = allCourses.stream()
                    .filter(e -> e.getCourseID().equals(course.getCourseID()))
                    .findFirst().orElse(null);

            if (selectedCourseObject == null) {
                showAlert("Error", "Invalid selection. Please try again.");
                return;
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

            classLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Etiketi GridPane'e sığdırma
            schedulerGrid.add(classLabel, colIndex, rowIndex); // Label'ı GridPane'e ekleme
            GridPane.setRowSpan(classLabel, duration); // Süre kadar satır kaplama

            classLabel.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    showAlert("Warning", "Are you sure you want to withdraw from " + course.getCourseID() + "!");
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
            default:
                return 0;  // In case there's an unexpected day
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
                return 0;  // Default to first row if time is unexpected
        }
    }


    public String getSelectedStudent() {
        return selectedStudent;
    }

    public void setSelectedStudent(String selectedStudent) {
        this.selectedStudent = selectedStudent;
    }

    public Course getSelectedCourse() {
        return selectedCourse;
    }

    public void setSelectedCourse(Course selectedCourse) {
        this.selectedCourse = selectedCourse;
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void Delete(String title, String message) {
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
                Database.removeStudentFromCourse(getSelectedStudent(),getSelectedCourse().getCourseID());
                alert.close();
            } else if (result.get() == cancelButton) {
                alert.close();
            }
        }
    }
}
