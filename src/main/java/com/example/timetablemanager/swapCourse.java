package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import static com.example.timetablemanager.Database.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class swapCourse {
    @FXML
    private Label SwapClasroomLabel, CapacityLabel, EnrollmendLabel, ClasssroomLabel, CapacityLabel2;

    @FXML
    private Button btnSave, btnCancel, btnBack;

    @FXML
    private ListView CapacityListView, EnrolledListView, ClasroomListView, CapacityListView2;

    @FXML
    private ComboBox SelectCourseCombo, SelectClassroomCombo;

    private String courseCapacity;

    private String classroomCapacity;

    private  String selectedClass;

    private int numberOfStudents;

    // Fetch real classrooms from the database
    List<String> classrooms = Database.getAllClassroomNames();
    private static List<Course> courseList = new ArrayList<>();
    @FXML
    public void initialize() {


        SelectCourseCombo.setItems(FXCollections.observableArrayList(Database.getAllCourseNames()));
        SelectCourseCombo.setOnAction(event -> {
            String selectedCourse = SelectCourseCombo.getValue().toString(); // Seçilen değeri alır

            List<String> enrolledStudents = Database.getStudentsEnrolledInCourse(selectedCourse);
             numberOfStudents = enrolledStudents.size();
            EnrolledListView.setItems(FXCollections.observableArrayList(numberOfStudents));


            List<Integer> capacities = getCourseCapacities(selectedCourse);
            if (capacities.isEmpty()) {
                courseCapacity = "No data";
            } else {
                courseCapacity = capacities.stream().map(String::valueOf).collect(Collectors.joining(", "));
            }
            System.out.println(courseCapacity);

            CapacityListView.setItems(FXCollections.observableArrayList(courseCapacity));

            System.out.println("Seçilen kurs: " + selectedCourse);

        });




        SelectClassroomCombo.setItems(FXCollections.observableArrayList(Database.getAllClassroomNames()));
        SelectClassroomCombo.setOnAction(event -> {
             selectedClass = SelectClassroomCombo.getValue().toString();

            List<Integer> capacities = getAllClassroomCapacities(selectedClass);

            if (capacities.isEmpty()) {
                classroomCapacity = "No data";
            } else {
                classroomCapacity = capacities.stream().map(String::valueOf).collect(Collectors.joining(", "));
            }
            System.out.println(selectedClass);
            System.out.println(classroomCapacity);

            CapacityListView2.setItems(FXCollections.observableArrayList(classroomCapacity));
        });





        btnSave.setOnAction(event -> {
            if (!Database.hasSufficientCapacity(selectedClass, numberOfStudents)) {
                showAlert("Error", "The selected classroom does not meet the student capacity for the course.");
                return;
            }

        });
        btnBack.setOnAction(event -> switchScene("mainLayout.fxml"));



    }




    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            Object newRoot = loader.load();

            if (!(newRoot instanceof javafx.scene.Parent)) {
                throw new IllegalArgumentException("Loaded root is not a valid JavaFX parent node.");
            }

            Stage stage = (Stage) btnBack.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot((javafx.scene.Parent) newRoot);

            boolean wasFullScreen = stage.isFullScreen();
            stage.setFullScreen(wasFullScreen);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the scene: " + fxmlFile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showAlert("Error", "Invalid root type in FXML: " + fxmlFile);
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
