package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class addCourseController {

    @FXML
    private TextField txtCourseName, txtCourseID, txtLecturer;

    @FXML
    private TextArea txtDescription;

    @FXML
    private Spinner<Integer> spinnerCapacity, spinnerDuration;

    @FXML
    private Button btnSelectStudents, btnAssignClassroom, btnCreateCourse, btnBack;

    @FXML
    private ListView<String> studentListView;

    @FXML
    private ComboBox<String> comboClassroom;

    @FXML
    private ComboBox<String> comboDay, comboTime;

    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        spinnerCapacity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 40));
        spinnerDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));

        comboDay.setItems(FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
        comboTime.setItems(FXCollections.observableArrayList(
                "08:30", "09:25", "10:20", "11:15", "12:10",
                "13:05", "14:00", "14:55", "15:50", "16:45",
                "17:40", "18:35", "19:30", "20:25", "21:20", "22:15"
        ));

        // Fetch real classrooms from the database
        List<String> classrooms = Database.getAllClassroomNames();
        comboClassroom.setItems(FXCollections.observableArrayList(classrooms));

        // Button actions
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
            ObservableList<Student> selected = controller.getSelectedStudents();
            if (selected != null) {
                this.selectedStudents.addAll(selected);
                studentListView.setItems(FXCollections.observableArrayList(
                        this.selectedStudents.stream().map(student ->student.getFullName()).toList()
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

        // If you previously removed classrooms from the list to mark them as assigned,
        // consider not removing them now since we have actual classrooms from DB.
        // If you still want to simulate 'marking as assigned', you could remove it from the comboBox items:
        // comboClassroom.getItems().remove(selectedClassroom);

        showAlert("Success", "Classroom " + selectedClassroom + " assigned successfully.");
    }

    private void createCourse() {
        String courseName = txtCourseName.getText();
        String courseID = txtCourseID.getText();
        String description = txtDescription.getText();
        int capacity = spinnerCapacity.getValue();
        int duration = spinnerDuration.getValue();
        String lecturer = txtLecturer.getText();
        String classroom = comboClassroom.getValue();
        String day = comboDay.getValue();
        String time = comboTime.getValue();

        if (courseName.isEmpty() || courseID.isEmpty() || description.isEmpty() || lecturer.isEmpty() || classroom == null || day == null || time == null) {
            showAlert("Error", "Please fill in all fields, select day/time, assign students/classroom, and set capacity/duration!");
            return;
        }

        List<String> days = new ArrayList<>();
        days.add(day);

        List<String> times = new ArrayList<>();
        times.add(time);

        // Create course
        // Make sure your Course class constructor includes lecturer
        Course newCourse = new Course(
                courseName,
                capacity,
                new ArrayList<>(selectedStudents),
                classroom,
                days,
                times,
                duration,
                lecturer
        );

        // TODO: Add the new course to your timetable or database as needed.
        // e.g., TimetableManager.getTimetable().add(newCourse);
        // or Database.addCourse(...), etc.

        showAlert("Success", "Course " + courseName + " with ID " + courseID + " created successfully with lecturer " + lecturer + " on " + day + " at " + time + ".");
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
