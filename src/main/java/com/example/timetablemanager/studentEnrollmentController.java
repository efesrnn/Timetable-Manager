package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class studentEnrollmentController {

    @FXML
    private ComboBox<Course> comboCourses;

    @FXML
    private ListView<String> listViewEnrolledStudents;

    @FXML
    private Button btnEditEnrollment, btnSaveChanges, btnCancel;

    // Labels for course details
    @FXML
    private Label lblCourseName, lblLecturer, lblDuration, lblTimeToStart, lblClassroom;

    // New Label for Enrolled Students Count
    @FXML
    private Label lblEnrolledCount;

    private static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String DB_URL = "jdbc:sqlite:" + dbPath + File.separator + "TimetableManagement.db";
    private Connection conn;

    // Store all courses for selection
    private List<Course> allCourses = new ArrayList<>();
    // Store currently enrolled students for the selected course
    private ObservableList<Student> enrolledStudents = FXCollections.observableArrayList();
    // Store updated enrollments after editing
    private ObservableList<Student> updatedStudents = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to the database.");
            loadCourses();
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Connection Error", "Unable to connect to the database.");
        }

        // Populate ComboBox with courses
        comboCourses.setItems(FXCollections.observableArrayList(allCourses));

        // Set a cell factory to show the courseID in the ComboBox
        comboCourses.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                } else {
                    setText(course.getCourseID());
                }
            }
        });
        comboCourses.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                } else {
                    setText(course.getCourseID());
                }
            }
        });

        // When a course is selected, load its enrolled students and display course details
        comboCourses.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadEnrolledStudentsForCourse(newVal);
                displayCourseDetails(newVal);
            } else {
                listViewEnrolledStudents.getItems().clear();
                clearCourseDetails();
                lblEnrolledCount.setText("Enrolled Students: 0");
            }
        });

        btnEditEnrollment.setOnAction(event -> openStudentSelectionPopup());
        btnSaveChanges.setOnAction(event -> saveUpdatedEnrollments());
        btnCancel.setOnAction(event -> switchScene("mainLayout.fxml"));
    }

    private void loadCourses() {
        String query = "SELECT courseName, lecturer, duration, timeToStart FROM Courses";
        allCourses.clear();
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String lecturer = rs.getString("lecturer");
                int duration = rs.getInt("duration");
                String timeToStart = rs.getString("timeToStart");

                // Find if this course is allocated to a classroom
                String allocatedQuery = "SELECT classroomName FROM Allocated WHERE courseName = ?";
                String classroomName = "";
                try (PreparedStatement allocStmt = conn.prepareStatement(allocatedQuery)) {
                    allocStmt.setString(1, courseName);
                    try (ResultSet allocRS = allocStmt.executeQuery()) {
                        if (allocRS.next()) {
                            classroomName = allocRS.getString("classroomName");
                        }
                    }
                }

                // If classroom is assigned, get its capacity
                int classroomCapacity = 0;
                if (!classroomName.isEmpty()) {
                    String capacityQuery = "SELECT capacity FROM Classrooms WHERE classroomName = ?";
                    try (PreparedStatement capStmt = conn.prepareStatement(capacityQuery)) {
                        capStmt.setString(1, classroomName);
                        try (ResultSet capRS = capStmt.executeQuery()) {
                            if (capRS.next()) {
                                classroomCapacity = capRS.getInt("capacity");
                            }
                        }
                    }
                }

                // Create the course object with classroom capacity
                Course c = new Course(courseName, classroomCapacity, new ArrayList<>(), classroomName, timeToStart, duration, lecturer);
                allCourses.add(c);
            }

        } catch (SQLException e) {
            System.err.println("Error loading courses: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to load courses from the database.");
        }
    }


    /**
     * Fetches the capacity of the given classroom from the database.
     */
    private int getClassroomCapacity(String classroomName) {
        String sql = "SELECT capacity FROM Classrooms WHERE classroomName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("capacity");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching classroom capacity: " + e.getMessage());
        }
        return 0;
    }

    private void loadEnrolledStudentsForCourse(Course course) {
        // Clear previous data
        enrolledStudents.clear();

        String query = "SELECT DISTINCT studentName FROM Enrollments WHERE courseName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, course.getCourseID());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("studentName");
                    enrolledStudents.add(new Student(name, new ArrayList<>()));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading enrolled students: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Unable to load enrolled students.");
        }

        // Update the ListView with student names
        listViewEnrolledStudents.setItems(FXCollections.observableArrayList(
                enrolledStudents.stream().map(Student::getFullName).distinct().toList()
        ));

        // Update the Enrolled Students Count Label
        lblEnrolledCount.setText("Enrolled Students: " + enrolledStudents.size());

        // Initially, updatedStudents = enrolledStudents
        updatedStudents.clear();
        updatedStudents.addAll(enrolledStudents);
    }

    private void displayCourseDetails(Course course) {
        lblCourseName.setText(course.getCourseID());
        lblLecturer.setText(course.getLecturer());
        lblDuration.setText(String.valueOf(course.getDuration()) + " hours");
        lblTimeToStart.setText(course.getTimeToStart());
        lblClassroom.setText(course.getClassroom());
    }

    private void clearCourseDetails() {
        lblCourseName.setText("");
        lblLecturer.setText("");
        lblDuration.setText("");
        lblTimeToStart.setText("");
        lblClassroom.setText("");
    }

    private void openStudentSelectionPopup() {
        Course selectedCourse = comboCourses.getValue();
        if (selectedCourse == null) {
            showAlert(Alert.AlertType.WARNING, "No Course Selected", "Please select a course before editing enrollments.");
            return;
        }

        // If no classroom assigned, show alert with options
        if (selectedCourse.getClassroom() == null || selectedCourse.getClassroom().trim().isEmpty()) {
            showNoClassroomAssignedAlert();
            return;
        }

        // Retrieve classroom capacity from the course
        int classroomCapacity = selectedCourse.getCapacity(); // Assuming capacity is set to classroom capacity

        // Proceed to open the student selection popup
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("studentSelectionLayout.fxml"));
            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(loader.load()));
            popupStage.setTitle("Select Students");
            try {
                Stage stage = (Stage) popupStage.getScene().getWindow();
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/student.png")));
            } catch (RuntimeException e) {
                System.err.println("Couldn't load icon");
                e.printStackTrace();
            }
            studentSelectionController controller = loader.getController();
            // Use the capacity from the assigned classroom
            controller.setCourseCapacity(classroomCapacity);

            // Pre-select currently enrolled students from updatedStudents
            List<String> currentlyEnrolledNames = updatedStudents.stream()
                    .map(Student::getFullName)
                    .toList();
            controller.setInitiallySelectedStudents(currentlyEnrolledNames);

            // Show popup and wait for it to close
            popupStage.showAndWait();

            // Retrieve selected students
            ObservableList<Student> selected = controller.getSelectedStudents();
            if (selected != null) {
                int newEnrollmentCount = selected.size();

                if (newEnrollmentCount > classroomCapacity) {
                    // Show the improved alert with three options
                    showOverCapacityAlert(classroomCapacity, newEnrollmentCount);
                } else {
                    // Update updatedStudents since it's within capacity
                    updatedStudents.clear();
                    updatedStudents.addAll(selected);
                    // Update ListView
                    listViewEnrolledStudents.setItems(FXCollections.observableArrayList(
                            updatedStudents.stream().map(Student::getFullName).distinct().toList()
                    ));

                    // Update the Enrolled Students Count Label
                    lblEnrolledCount.setText("Enrolled Students: " + updatedStudents.size());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load the student selection popup.");
        }
    }

    private void saveUpdatedEnrollments() {
        Course selectedCourse = comboCourses.getValue();
        if (selectedCourse == null) {
            showAlert(Alert.AlertType.WARNING, "No Course Selected", "Please select a course before saving changes.");
            return;
        }

        // If no classroom assigned, show alert with options
        if (selectedCourse.getClassroom() == null || selectedCourse.getClassroom().trim().isEmpty()) {
            showNoClassroomAssignedAlert();
            return;
        }

        // Retrieve classroom capacity from the course
        int classroomCapacity = selectedCourse.getCapacity(); // Assuming capacity is set to classroom capacity

        int newEnrollmentCount = updatedStudents.size();

        if (newEnrollmentCount > classroomCapacity) {
            // Show the improved alert with three options
            showOverCapacityAlert(classroomCapacity, newEnrollmentCount);
            return; // Exit after handling the alert
        }

        // Proceed with saving enrollments
        try {
            conn.setAutoCommit(false); // Start transaction

            // Delete existing enrollments for the course
            String deleteSQL = "DELETE FROM Enrollments WHERE courseName = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL)) {
                deleteStmt.setString(1, selectedCourse.getCourseID());
                deleteStmt.executeUpdate();
            }

            // Insert the new enrollments
            String insertSQL = "INSERT INTO Enrollments (courseName, studentName) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                for (Student student : updatedStudents) {
                    insertStmt.setString(1, selectedCourse.getCourseID());
                    insertStmt.setString(2, student.getFullName());
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit(); // Commit transaction
            conn.setAutoCommit(true);

            showAlert(Alert.AlertType.INFORMATION, "Success", "Enrollments updated for course: " + selectedCourse.getCourseID());
            switchScene("mainLayout.fxml");

        } catch (SQLException e) {
            try {
                conn.rollback(); // Rollback transaction on error
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update enrollments: " + e.getMessage());
        }
    }

    private void showNoClassroomAssignedAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No Classroom Assigned");
        alert.setHeaderText("This course has no classroom assigned.");
        alert.setContentText("Would you like to assign this course to an available classroom?");

        ButtonType btnYes = new ButtonType("Yes");
        ButtonType btnNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnYes, btnNo);

        // Enhance layout with custom content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Text warningText = new Text("Assign Classroom");
        warningText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        grid.add(warningText, 0, 0, 2, 1);

        Label label = new Label("Please assign a classroom to proceed.");
        grid.add(label, 0, 1, 2, 1);

        alert.getDialogPane().setContent(grid);

        // Set the icon if available
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon");
            e.printStackTrace();
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            switchScene("assignClassroomLayout.fxml");
        }
    }

    private void showOverCapacityAlert(int capacity, int enrolled) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Enrollment Over Capacity");
        dialog.setHeaderText("Enrolled students exceed classroom capacity.");
        dialog.setResizable(true);

        // Set the icon if available
        try {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon");
            e.printStackTrace();
        }

        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Add labels
        Text title = new Text("Enrollment Over Capacity");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        grid.add(title, 0, 0, 2, 1);

        Label capacityLabel = new Label("Classroom Capacity:");
        capacityLabel.setTextFill(Color.DARKBLUE);
        grid.add(capacityLabel, 0, 1);
        Label capacityValue = new Label(String.valueOf(capacity));
        grid.add(capacityValue, 1, 1);

        Label enrolledLabel = new Label("Enrolled Students:");
        enrolledLabel.setTextFill(Color.DARKBLUE);
        grid.add(enrolledLabel, 0, 2);
        Label enrolledValue = new Label(String.valueOf(enrolled));
        grid.add(enrolledValue, 1, 2);

        Label actionLabel = new Label("Choose an action:");
        grid.add(actionLabel, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Add buttons
        ButtonType btnSwap = new ButtonType("Swap Classroom", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCreateSection = new ButtonType("Create New Section", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(btnSwap, btnCreateSection, btnCancel);

        // Handle button actions
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnSwap) {
                switchScene("swapClassroom.fxml");
            } else if (result.get() == btnCreateSection) {
                switchScene("addCourseLayout.fxml");
            }
            // Cancel does nothing
        }
    }

    public void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            javafx.scene.Parent newRoot = loader.load();

            // Assuming the target controller has a method called refreshData() or similar
            Object controller = loader.getController();
            if (controller instanceof ttManagerController) {
                ((ttManagerController) controller).refreshTable(); // Adjust method name as needed
            }

            Stage stage = (Stage) btnCancel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(newRoot);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load: " + fxmlFile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid root type in FXML: " + fxmlFile);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
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
