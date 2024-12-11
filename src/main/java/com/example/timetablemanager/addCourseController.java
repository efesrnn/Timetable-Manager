package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class addCourseController {

    @FXML
    private TextField txtCourseName,txtCourseID;

    @FXML
    private TextArea txtDescription;

    @FXML
    private Spinner<Integer> spinnerCapacity;

    @FXML
    private Button btnSelectStudents,btnAssignClassroom,btnCreateCourse,btnBack;

    @FXML
    private ListView<String> studentListView;

    @FXML
    private ComboBox<String> comboClassroom;

    @FXML
    private ComboBox<String> comboDay, comboTime;

    private List<Student> allStudents = new ArrayList<>(); //TODO: Shall extract data into this list from CSV.
    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();
    private List<String> classrooms = new ArrayList<>(); // Placeholder for classroom list.

    @FXML
    public void initialize() {
        // Configuring Spinner for Capacity.
        spinnerCapacity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 40));

        comboDay.setItems(FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
        comboTime.setItems(FXCollections.observableArrayList(
                "08:30", "09:25", "10:20", "11:15", "12:10",
                "13:05", "14:00", "14:55", "15:50", "16:45",
                "17:40", "18:35", "19:30", "20:25", "21:20", "22:15"
        ));


        //TODO: created fake classroom to test:
        classrooms.add("M-302");
        classrooms.add("C-407");
        classrooms.add("M-103");
        comboClassroom.setItems(FXCollections.observableArrayList(classrooms));


        /*
        //TODO: created fake students to test:
        allStudents = new ArrayList<>(List.of(
                new Student("S001", "Alice Johnson"),
                new Student("S002", "Bob Smith"),
                new Student("S003", "Charlie Davis"),
                new Student("S004", "Diana Brown"),
                new Student("S005", "Ethan Wilson")
        ));
         */

        //BUTTON ACTIONS:

        btnSelectStudents.setOnAction(event -> openStudentSelectionPopup());
        btnAssignClassroom.setOnAction(event -> assignClassroom());
        btnCreateCourse.setOnAction(event -> createCourse());
        btnBack.setOnAction(event -> switchScene("mainLayout.fxml"));
    }



    private void openStudentSelectionPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("studentSelectionLayout.fxml"));
            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(loader.load()));
            try {
                Stage stage = (Stage) popupStage.getScene().getWindow();
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/student.png")));
            } catch (RuntimeException e) {
                System.err.println("Couldn't load icon");
                e.printStackTrace();
            }
            popupStage.setTitle("Student Enroll");
            studentSelectionController controller = loader.getController();
            popupStage.showAndWait();

            // Retrieve selected students
            ObservableList<Student> selectedStudents = controller.getSelectedStudents();
            if (selectedStudents != null) {
                this.selectedStudents.addAll(selectedStudents);
                studentListView.setItems(FXCollections.observableArrayList(
                        this.selectedStudents.stream().map(student -> student.getId() + " | " + student.getName()).toList()
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the student selection popup.");
        }
    }



    private void assignClassroom() {
        String selectedClassroom = comboClassroom.getValue();
        if (selectedClassroom == null) {
            showAlert("Error", "No classroom selected!");
            return;
        }

        classrooms.remove(selectedClassroom); // Mark as claimed
        showAlert("Success", "Classroom " + selectedClassroom + " assigned successfully.");
    }



    private void createCourse() {
        String courseName = txtCourseName.getText();
        String courseID = txtCourseID.getText();
        String description = txtDescription.getText();
        int capacity = spinnerCapacity.getValue();
        String classroom = comboClassroom.getValue();
        String day = comboDay.getValue(); // Selected day
        String time = comboTime.getValue(); // Selected time

        if (courseName.isEmpty() || courseID.isEmpty() || classroom == null || day == null || time == null) {
            showAlert("Error", "Please fill in all fields, select a day/time, and assign students/classroom!");
            return;
        }

        List<String> days = new ArrayList<>();
        days.add(day);

        List<String> times = new ArrayList<>();
        times.add(time);

        Course newCourse = new Course(courseName, courseID, description, capacity, new ArrayList<>(selectedStudents), classroom, days, times);
        showAlert("Success", "Course " + courseName + " created successfully with schedule on " + day + " at " + time + ".");
    }



    private void switchScene(String fxmlFile) {
        try {

            //bknz. ttManagerController.java switchScene method!!!

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

        //Alert icon initialization:
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