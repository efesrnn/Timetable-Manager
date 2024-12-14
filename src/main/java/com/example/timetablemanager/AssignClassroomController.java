package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class AssignClassroomController {

    @FXML
    private ListView<String> listViewCourses;

    @FXML
    private ListView<String> listViewClassrooms;

    @FXML
    private ListView<String> listViewOutline;

    @FXML
    private Button buttonAssign;

    @FXML
    private Button buttonSave;

    @FXML
    private Button buttonBack;

    private List<Course> allCourses;
    private List<String> allClassroomNames;
    private ObservableList<String> outline = FXCollections.observableArrayList();

    public void initialize() {

        allCourses = Database.getAllCourses();
        allClassroomNames = Database.getAllClassroomNames();
        listViewCourses.getItems().clear();


        listViewCourses.setItems(FXCollections.observableArrayList(
                allCourses.stream()
                        .map(Course::getCourseName)
                        .distinct()
                        .toList()
        ));

        listViewClassrooms.setItems(FXCollections.observableArrayList(allClassroomNames));

        listViewOutline.setItems(outline);


        buttonAssign.setOnAction(event -> {
            String selectedCourseName = listViewCourses.getSelectionModel().getSelectedItem();
            String selectedClassroomName = listViewClassrooms.getSelectionModel().getSelectedItem();

            if (selectedCourseName == null || selectedClassroomName == null) {
                showAlert("Error", "Please select a course and a classroom.");                return;
            }

            // Seçilen kursu bul
            Course selectedCourse = allCourses.stream()
                    .filter(course -> course.getCourseName().equals(selectedCourseName))
                    .findFirst().orElse(null);

            if (selectedCourse == null) {
                showAlert("Error", "Invalid selection. Please try again.");
                return;
            }
            // Get the number of students enrolled in the course
            List<String> enrolledStudents = Database.getStudentsEnrolledInCourse(selectedCourseName);
            int numberOfStudents = enrolledStudents.size();

            // Check if the classroom has sufficient capacity
            if (!Database.hasSufficientCapacity(selectedClassroomName, numberOfStudents)) {
                showAlert("Error", "The selected classroom does not meet the student capacity for the course.");
                return;
            }

            outline.add(selectedCourse.getCourseName() + " -> " + selectedClassroomName);

            // Outline ListView refresh
            listViewOutline.setItems(outline);
        });


        buttonSave.setOnAction(event -> {
            for (String assignment : outline) {
                String[] parts = assignment.split(" -> ");

                String selectedCourseName = listViewCourses.getSelectionModel().getSelectedItem();
                String selectedClassroomName = listViewClassrooms.getSelectionModel().getSelectedItem();

                if (selectedCourseName == null || selectedClassroomName == null) {
                    showAlert("Error", "Please select a course and a classroom.");
                    return;
                }

                Course selectedCourse = allCourses.stream()
                        .filter(course -> course.getCourseName().equals(selectedCourseName))
                        .findFirst().orElse(null);

                if (selectedCourse == null) {
                    showAlert("Error", "Invalid selection. Please try again.");
                    return;
                }


                selectedCourse.setClassroom(selectedClassroomName);
                Database.allocateCourseToClassroom(selectedCourse.getCourseName(), selectedClassroomName);
            }
            showAlert("Success", "Assignments have been successfully saved!");


            outline.clear();
            listViewOutline.setItems(outline);
        });


        buttonBack.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("mainLayout.fxml"));
                Stage stage = (Stage) buttonBack.getScene().getWindow();
                stage.setScene(new Scene(loader.load()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
