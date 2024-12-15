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

    // Fetch real classrooms from the database
    List<String> classrooms = Database.getAllClassroomNames();
    private static List<Course> courseList = new ArrayList<>();
    @FXML
    public void initialize() {
        //SelectCourseCombo.getItems().add(data.getValue().getCourseName());
        SelectCourseCombo.setItems(FXCollections.observableArrayList(Database.getAllCourses()));
        SelectCourseCombo.setOnAction(event -> {
            String selectedCourse = SelectCourseCombo.getValue().toString(); // Seçilen değeri alır
            System.out.println("Seçilen kurs: " + selectedCourse);
        });

        ObservableList<Course> courses = FXCollections.observableArrayList(Database.getAllCourses());
        // 1. for-each döngüsü ile yazdırma
        for (Course course : courses) {
            System.out.println(course);
        }

        SelectClassroomCombo.setItems(FXCollections.observableArrayList(Database.getAllClassroomNames()));
        SelectClassroomCombo.setOnAction(event -> {
            String selectedClass = SelectClassroomCombo.getValue().toString();

            List<Integer> capacities = getAllClassroomCapacities(selectedClass);
            String capacity;

            if (capacities.isEmpty()) {
                capacity = "No data";
            } else {
                capacity = capacities.stream().map(String::valueOf).collect(Collectors.joining(", "));
            }
            System.out.println(selectedClass);
            System.out.println(capacity);

            CapacityListView2.setItems(FXCollections.observableArrayList(capacity));
        });

//        SelectClassroomCombo.setItems(FXCollections.observableArrayList(Database.getAllClassroomNames()));
//        SelectClassroomCombo.setOnAction(event -> {
//            String selectedClass = SelectClassroomCombo.getValue().toString();
//
//            String capacity = getAllClassroomCapacities(selectedClass).toString(); // Metodunuz kapasiteyi String döndürmeli.
//            System.out.println(selectedClass);
//            System.out.println(capacity);
//            // CapacityListView2'nin içeriğini güncelle
//            CapacityListView2.setItems(FXCollections.observableArrayList(capacity));
//        });
        btnBack.setOnAction(event -> switchScene("mainLayout.fxml"));


    }

    public void All() {


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
