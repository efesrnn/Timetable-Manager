package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ttManagerController {

    @FXML
    private Button btnAddCourse, btnEnrollStudent, btnAssignClassroom, btnSwapClassroom, btnSearch;

    @FXML
    private MenuItem menuImportCSV, menuLoadTimetable, menuSaveTimetable, menuExportTimetable, menuExit;
    @FXML
    private MenuItem menuUserManual, menuAbout;

    @FXML
    private TableView<Course> timetableTable;

    @FXML
    private TableColumn<Course, String> courseColumn,lecturerColumn,dayColumn,timeColumn,enrolledStudentsColumn,classroomColumn;    // New Column for Course ID

    @FXML
    private TableColumn<Course, Number> capacityColumn;


    @FXML
    public void initialize() {
        // Course Name
        courseColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getCourseName()));

        // Lecturer
        lecturerColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getLecturer()));

        // Days
        dayColumn.setCellValueFactory(data -> {
            // Join all days into a single string
            String joinedDays = String.join(", ", data.getValue().getDays());
            return new javafx.beans.property.SimpleStringProperty(joinedDays);
        });

        // Times
        timeColumn.setCellValueFactory(data -> {
            // Join all times into a single string
            String joinedTimes = String.join(", ", data.getValue().getTimes());
            return new javafx.beans.property.SimpleStringProperty(joinedTimes);
        });

        // Classroom
        classroomColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getClassroom()));

        // Capacity
        capacityColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getCapacity()));

        // Enrolled Students (show count instead of names)
        enrolledStudentsColumn.setCellValueFactory(data -> {
            int count = data.getValue().getStudents().size();
            return new javafx.beans.property.SimpleStringProperty(String.valueOf(count));
        });



        // Populate table with current timetable courses
        timetableTable.setItems(FXCollections.observableArrayList(TimetableManager.getTimetable()));

        // Buttons and menu actions
        btnSearch.setOnAction(event -> showAlert("Search", "Search logic not attached yet."));
        btnAddCourse.setOnAction(event -> switchScene("addCourseLayout.fxml"));
        btnEnrollStudent.setOnAction(event -> showAlert("Enroll Student", "Enroll Student logic not attached yet."));
        btnAssignClassroom.setOnAction(event -> switchScene("assignClassroomLayout.fxml"));
        btnSwapClassroom.setOnAction(event -> showAlert("Swap Classroom", "Swap Classroom logic not attached yet."));

        menuImportCSV.setOnAction(event -> showAlert("Import CSV", "Import CSV not attached yet."));
        menuLoadTimetable.setOnAction(event -> showAlert("Load Timetable", "Load Timetable not attached yet."));
        menuSaveTimetable.setOnAction(event -> showAlert("Save Timetable", "Save Timetable not attached yet."));
        menuExportTimetable.setOnAction(event -> showAlert("Export Timetable", "Export Timetable not attached yet."));
        menuExit.setOnAction(event -> System.exit(0));
        menuUserManual.setOnAction(event -> showAlert("User Manual", "User Manual not attached yet."));
        menuAbout.setOnAction(event -> showAlert("About", "About not attached yet."));
        timetableTable.setItems(FXCollections.observableArrayList(TimetableManager.getTimetable()));

    }


    //TODO: Unique courseID aand StudentName
    public void refreshTable() {
        //Removing duplicate courses by courseName
        List<Course> original = TimetableManager.getTimetable();
        LinkedHashMap<String, Course> uniqueCourses = new LinkedHashMap<>();
        for (Course c : original) {
            uniqueCourses.put(c.getCourseName(), c);
        }

        //For each unique course, remove duplicate students
        for (Course c : uniqueCourses.values()) {
            LinkedHashMap<String, Student> uniqueStudents = new LinkedHashMap<>();
            for (Student s : c.getStudents()) {
                // Use getFullName(), getStudentId(), or another unique field as the key
                // Assuming fullName is unique for demonstration:
                uniqueStudents.put(s.getFullName(), s);
            }
            // Replace the course's student list with a new list of unique students
            c.setStudents(new ArrayList<>(uniqueStudents.values()));
        }

        // Step 3: Update the table with the filtered, unique courses
        timetableTable.setItems(FXCollections.observableArrayList(uniqueCourses.values()));
    }




    public void loadTimetableFromCSV(File file) {
        if (file != null) {
            System.out.println("Loading timetable from: " + file.getAbsolutePath());
            // TODO: Add CSV parsing and data population logic
        } else {
            System.err.println("File is null, cannot load timetable.");
        }
    }

    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            Object newRoot = loader.load();

            if (!(newRoot instanceof javafx.scene.Parent)) {
                throw new IllegalArgumentException("Loaded root is not a valid JavaFX parent node");
            }

            Stage stage = (Stage) btnAddCourse.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot((javafx.scene.Parent) newRoot);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load: " + fxmlFile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showAlert("Error", "Invalid root type in FXML: " + fxmlFile);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load icon");
            e.printStackTrace();
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
