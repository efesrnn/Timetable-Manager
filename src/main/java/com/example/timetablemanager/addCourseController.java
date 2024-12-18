package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class addCourseController {

    @FXML
    private TextField txtCourseID, txtLecturer;

    @FXML
    private Spinner<Integer> spinnerCapacity, spinnerDuration;

    @FXML
    private Button btnSelectStudents, btnCreateCourse, btnBack;

    @FXML
    private ListView<String> studentListView;

    @FXML
    private ComboBox<String> comboClassroom;

    @FXML
    private ComboBox<String> comboTimeToStart; // New ComboBox for TimeToStart

    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();
    private List<String> assignedClassrooms = Database.getAllAllocatedClassrooms();


    // Define possible days and times
    private final List<String> days = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    private final List<String> times = List.of(
            "08:30", "09:25", "10:20", "11:15", "12:10",
            "13:05", "14:00", "14:55", "15:50", "16:45",
            "17:40", "18:35", "19:30", "20:25", "21:20", "22:15"
    );

    @FXML
    public void initialize() {
        // Ensure the database is connected
        Database.connect();

        // Initialize Spinners
        spinnerCapacity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 40));
        spinnerDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2));

        // Populate TimeToStart ComboBox with combined day and time options
        List<String> timeToStartOptions = new ArrayList<>();
        for (String day : days) {
            for (String time : times) {
                timeToStartOptions.add(day + " " + time);
            }
        }
        comboTimeToStart.setItems(FXCollections.observableArrayList(timeToStartOptions));

        // Fetch classrooms with capacities from the database and exclude assigned ones
        assignedClassrooms = Database.getAllAllocatedClassrooms(); // Update the assignedClassrooms list
        List<String> classroomsWithCapacities = Database.getAllClassroomsWithCapacities();
        classroomsWithCapacities.removeIf(classroom -> assignedClassrooms.contains(classroom.split(" \\| ")[0]));
        comboClassroom.setItems(FXCollections.observableArrayList(classroomsWithCapacities));

        // Button actions
        btnSelectStudents.setOnAction(event -> openStudentSelectionPopup());
        btnCreateCourse.setOnAction(event -> createCourse());
        btnBack.setOnAction(event -> switchScene("mainLayout.fxml"));
    }

    private void openStudentSelectionPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("studentSelectionLayout.fxml"));
            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(loader.load()));

            // Set window icon (optional)
            try {
                Stage stage = (Stage) popupStage.getScene().getWindow();
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/student.png")));
            } catch (RuntimeException e) {
                System.err.println("Couldn't load icon");
                e.printStackTrace();
            }
            popupStage.setTitle("Student Enroll");
            studentSelectionController controller = loader.getController();
            controller.setCourseCapacity(spinnerCapacity.getValue());
            popupStage.showAndWait();

            // Retrieve selected students without duplicates
            ObservableList<Student> selected = controller.getSelectedStudents();
            if (selected != null) {
                for (Student student : selected) {
                    // Add only if the student is not already in the list
                    if (!this.selectedStudents.contains(student)) {
                        this.selectedStudents.add(student);
                    }
                }
                // Update ListView with the latest unique list
                studentListView.setItems(FXCollections.observableArrayList(
                        this.selectedStudents.stream().map(Student::getFullName).distinct().toList()
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the student selection popup.");
        }
    }


    private void createCourse() {
        String courseID = txtCourseID.getText().trim();
        int capacity = spinnerCapacity.getValue();
        int duration = spinnerDuration.getValue();
        String lecturer = txtLecturer.getText().trim();

        String selectedClassroomEntry = comboClassroom.getValue();
        if (selectedClassroomEntry == null) {
            showAlert("Error", "No classroom selected!");
            return;
        }

        // Extract only the classroom name
        String classroom = selectedClassroomEntry.split(" \\| ")[0];

        String timeToStart = comboTimeToStart.getValue();

        if (courseID.isEmpty() || lecturer.isEmpty() || classroom == null || timeToStart == null) {
            showAlert("Error", "Please fill in all fields, select time, assign students/classroom, and set capacity/duration!");
            return;
        }

        // Validate classroom capacity
        if (!Database.hasSufficientCapacity(classroom, selectedStudents.size())) {
            showAlert("Error", "Selected classroom does not have sufficient capacity for the number of students.");
            return;
        }

        // Prepare course data
        Course newCourse = new Course(
                courseID,
                capacity,
                new ArrayList<>(selectedStudents),
                classroom,
                timeToStart,
                duration,
                lecturer
        );

        // Add the new course to the TimetableManager
        TimetableManager.getTimetable().add(newCourse);

        // Save the course to the database
        try {
            Database.addCourseWithAllocation(courseID, lecturer, duration, timeToStart, classroom);
        } catch (SQLException e) {
            showAlert("Error", "Failed to create course and allocate classroom: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Removed redundant allocateClassroom call
        // Database.allocateCourseToClassroom(courseID, classroom);

        for (Student student : selectedStudents) {
            if (!Database.isEnrollmentExists(courseID, student.getFullName())) {
                Database.addEnrollment(courseID, student.getFullName());
            }
        }

        showAlert("Success", "Course created successfully: " + courseID);

        // Switch back to mainLayout and refresh the table
        switchScene("mainLayout.fxml");
    }



    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            Object newRoot = loader.load();

            if (!(newRoot instanceof javafx.scene.Parent)) {
                throw new IllegalArgumentException("Loaded root is not a valid JavaFX parent node.");
            }
            ttManagerController controller = loader.getController();
            controller.refreshTable(); // Refresh the TableView

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
