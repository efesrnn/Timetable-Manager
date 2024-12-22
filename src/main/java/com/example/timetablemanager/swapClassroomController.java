package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration; // Import added for Tooltip delays

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class swapClassroomController {

    @FXML
    private Label SwapClasroomLabel, EnrollmendLabel, ClasssroomLabel, EnrollmendLabel2,
            ClasssroomLabel2, CapacityLabel2, CapacityLabel;

    @FXML
    private Button btnSave, btnBack;

    @FXML
    private ListView<Integer> EnrolledListView, EnrolledListView2;
    @FXML
    private ListView<String> ClassroomListView, ClassroomListView2,
            CapacityListView, CapacityListView2;

    @FXML
    private ComboBox<Course> SelectCourseCombo, SelectCourse2;

    // Keep references to the currently selected courses
    private Course selectedCourseA;
    private Course selectedCourseB;

    // Keep references to capacity strings
    private String classroomCapacityA;
    private String classroomCapacityB;

    // For quick checks
    private int enrolledCountA;
    private int enrolledCountB;

    private List<Course> allCourses;
    private ttManagerController mainController;

    public void setMainController(ttManagerController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        // Load courses from DB
        allCourses = Database.getAllCourses();  // includes those with & without classrooms

        // 1) Configure ComboBoxes to display all courses
        //    but highlight those with no classroom assigned in RED + tooltip
        SelectCourseCombo.setItems(FXCollections.observableArrayList(allCourses));
        SelectCourse2.setItems(FXCollections.observableArrayList(allCourses));

        // Use a cell factory to show red text if no classroom:
        SelectCourseCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setTooltip(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(course.getCourseID());
                    if (course.getClassroom() == null || course.getClassroom().isBlank()) {
                        setTextFill(Color.RED);
                        Tooltip tooltip = new Tooltip("No classroom assigned");
                        tooltip.setShowDelay(Duration.ZERO); // Set show delay to 0
                        tooltip.setHideDelay(Duration.ZERO); // Set hide delay to 0
                        setTooltip(tooltip);
                    } else {
                        setTextFill(Color.BLACK);
                        setTooltip(null);
                    }
                }
            }
        });
        // Also set a button cell to handle the selected item display consistently
        SelectCourseCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(course.getCourseID());
                    if (course.getClassroom() == null || course.getClassroom().isBlank()) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        // Do the same for SelectCourse2
        SelectCourse2.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setTooltip(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(course.getCourseID());
                    if (course.getClassroom() == null || course.getClassroom().isBlank()) {
                        setTextFill(Color.RED);
                        Tooltip tooltip = new Tooltip("No classroom assigned");
                        tooltip.setShowDelay(Duration.ZERO); // Set show delay to 0
                        tooltip.setHideDelay(Duration.ZERO); // Set hide delay to 0
                        setTooltip(tooltip);
                    } else {
                        setTextFill(Color.BLACK);
                        setTooltip(null);
                    }
                }
            }
        });
        SelectCourse2.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setTextFill(Color.BLACK);
                } else {
                    setText(course.getCourseID());
                    if (course.getClassroom() == null || course.getClassroom().isBlank()) {
                        setTextFill(Color.RED);
                    } else {
                        setTextFill(Color.BLACK);
                    }
                }
            }
        });

        // 2) Add action listeners
        SelectCourseCombo.setOnAction(event -> handleCourseSelectionA());
        SelectCourse2.setOnAction(event -> handleCourseSelectionB());

        // Save button => attempt swap
        btnSave.setOnAction(event -> handleSwap());

        // Back button => switch scene
        btnBack.setOnAction(event -> switchScene("mainLayout.fxml"));

        // Attempt to refresh main table (if needed)
        refreshTimetableView();
    }

    /**
     * Called when first ComboBox changes (course A).
     */
    private void handleCourseSelectionA() {
        selectedCourseA = SelectCourseCombo.getValue();
        if (selectedCourseA == null) {
            clearCourseAFields();
            return;
        }

        // Enrolled students
        List<String> enrolledStudents = Database.getStudentsEnrolledInCourse(selectedCourseA.getCourseID());
        enrolledCountA = enrolledStudents.size();
        EnrolledListView.setItems(FXCollections.observableArrayList(enrolledCountA));

        // Current classroom
        String classroomA = (selectedCourseA.getClassroom() == null) ? "" : selectedCourseA.getClassroom();
        ClassroomListView.setItems(FXCollections.observableArrayList(classroomA));

        // Classroom capacity
        List<Integer> capacities = Database.getAllClassroomCapacities(classroomA);
        if (capacities.isEmpty()) {
            classroomCapacityA = ""; // means no assigned classroom or not in DB
        } else {
            classroomCapacityA = capacities.stream()
                    .map(String::valueOf).collect(Collectors.joining(", "));
        }
        CapacityListView.setItems(FXCollections.observableArrayList(classroomCapacityA));
    }

    /**
     * Called when second ComboBox changes (course B).
     */
    private void handleCourseSelectionB() {
        selectedCourseB = SelectCourse2.getValue();
        if (selectedCourseB == null) {
            clearCourseBFields();
            return;
        }

        // Enrolled students
        List<String> enrolledStudents = Database.getStudentsEnrolledInCourse(selectedCourseB.getCourseID());
        enrolledCountB = enrolledStudents.size();
        EnrolledListView2.setItems(FXCollections.observableArrayList(enrolledCountB));

        // Current classroom
        String classroomB = (selectedCourseB.getClassroom() == null) ? "" : selectedCourseB.getClassroom();
        ClassroomListView2.setItems(FXCollections.observableArrayList(classroomB));

        // Classroom capacity
        List<Integer> capacities = Database.getAllClassroomCapacities(classroomB);
        if (capacities.isEmpty()) {
            classroomCapacityB = "";
        } else {
            classroomCapacityB = capacities.stream()
                    .map(String::valueOf).collect(Collectors.joining(", "));
        }
        CapacityListView2.setItems(FXCollections.observableArrayList(classroomCapacityB));
    }

    /**
     * Logic to handle swapping the two selected courses' classrooms
     * if no capacity or schedule conflict.
     */
    private void handleSwap() {
        if (selectedCourseA == null || selectedCourseB == null) {
            showAlert("Error", "Please select two courses first.");
            return;
        }

        String oldClassA = (selectedCourseA.getClassroom() == null) ? "" : selectedCourseA.getClassroom();
        String oldClassB = (selectedCourseB.getClassroom() == null) ? "" : selectedCourseB.getClassroom();

        // Check if both courses are assigned to the same classroom
        if (oldClassA.equals(oldClassB)) {
            showAlert("Information", "Both courses are already assigned to the same classroom. No swap needed.");
            return;
        }

        // 1) Capacity checks:
        //    - Is oldClassB big enough for selectedCourseA's student count?
        //    - Is oldClassA big enough for selectedCourseB's student count?
        if (!oldClassB.isBlank()) {
            boolean enoughCapB = Database.hasSufficientCapacity(oldClassB, enrolledCountA);
            if (!enoughCapB) {
                showAlert("Error", "Classroom '" + oldClassB + "' cannot hold " + enrolledCountA + " students.");
                return;
            }
        } else {
            showAlert("Error", "Course A is not assigned to any classroom to swap.");
            return;
        }

        if (!oldClassA.isBlank()) {
            boolean enoughCapA = Database.hasSufficientCapacity(oldClassA, enrolledCountB);
            if (!enoughCapA) {
                showAlert("Error", "Classroom '" + oldClassA + "' cannot hold " + enrolledCountB + " students.");
                return;
            }
        } else {
            showAlert("Error", "Course B is not assigned to any classroom to swap.");
            return;
        }

        // 2) Schedule checks:
        //    - Check if oldClassB is available for Course A's time slot
        //    - Check if oldClassA is available for Course B's time slot

        String dayTimeA = selectedCourseA.getTimeToStart();
        String dayTimeB = selectedCourseB.getTimeToStart();

        // Parse day and time for Course A
        if (dayTimeA != null && !dayTimeA.isBlank()) {
            String[] partsA = dayTimeA.split(" ");
            if (partsA.length >= 2) {
                String dayA = partsA[0];
                String timeA = partsA[1];
                // Check availability excluding Course A itself
                boolean availableForA = Database.isClassroomAvailable(oldClassB, dayA, timeA, selectedCourseA.getDuration(), selectedCourseA.getCourseID());
                if (!availableForA) {
                    showAlert("Error", "Classroom '" + oldClassB + "' is not available at " + dayTimeA + " for " + selectedCourseA.getDuration() + " slot(s).");
                    return;
                }
            }
        }

        // Parse day and time for Course B
        if (dayTimeB != null && !dayTimeB.isBlank()) {
            String[] partsB = dayTimeB.split(" ");
            if (partsB.length >= 2) {
                String dayB = partsB[0];
                String timeB = partsB[1];
                // Check availability excluding Course B itself
                boolean availableForB = Database.isClassroomAvailable(oldClassA, dayB, timeB, selectedCourseB.getDuration(), selectedCourseB.getCourseID());
                if (!availableForB) {
                    showAlert("Error", "Classroom '" + oldClassA + "' is not available at " + dayTimeB + " for " + selectedCourseB.getDuration() + " slot(s).");
                    return;
                }
            }
        }

        // 3) Transactional Swap:
        //    - First, assign Course A to oldClassB
        //    - Then, assign Course B to oldClassA
        //    - If any step fails, rollback the changes

        try {
            // Begin Transaction (Assuming Database has transaction management)
            Database.beginTransaction();

            // Assign Course A to oldClassB
            Database.changeClassroom(selectedCourseA.getCourseID(), oldClassB);

            // Assign Course B to oldClassA
            Database.changeClassroom(selectedCourseB.getCourseID(), oldClassA);

            // Commit Transaction
            Database.commitTransaction();

            // Update local references
            selectedCourseA.setClassroom(oldClassB);
            selectedCourseB.setClassroom(oldClassA);

            // Show success
            showAlert("Success", "Courses' classrooms have been swapped successfully!");

            // Refresh the displayed classroom info
            handleCourseSelectionA();
            handleCourseSelectionB();

            // Refresh Timetable
            refreshTimetableView();

        } catch (Exception e) {
            // Rollback Transaction in case of any failure
            Database.rollbackTransaction();
            e.printStackTrace();
            showAlert("Error", "Failed to swap classrooms due to an unexpected error.");
        }
    }

    private void clearCourseAFields() {
        EnrolledListView.getItems().clear();
        ClassroomListView.getItems().clear();
        CapacityListView.getItems().clear();
        enrolledCountA = 0;
        classroomCapacityA = "";
    }

    private void clearCourseBFields() {
        EnrolledListView2.getItems().clear();
        ClassroomListView2.getItems().clear();
        CapacityListView2.getItems().clear();
        enrolledCountB = 0;
        classroomCapacityB = "";
    }

    private void refreshTimetableView() {
        if (mainController != null) {
            mainController.refreshTable();
        }
    }

    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            Parent newRoot = loader.load();
            if (!(newRoot instanceof javafx.scene.Parent)) {
                throw new IllegalArgumentException("Loaded root is not a valid JavaFX parent node.");
            }

            Stage stage = (Stage) btnBack.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(newRoot);

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
        Alert alert;
        Alert.AlertType type;

        if (title.equalsIgnoreCase("Error")) {
            type = Alert.AlertType.ERROR;
        } else if (title.equalsIgnoreCase("Success")) {
            type = Alert.AlertType.INFORMATION;
        } else {
            type = Alert.AlertType.INFORMATION;
        }

        alert = new Alert(type);
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(
                    getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon");
            e.printStackTrace();
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}