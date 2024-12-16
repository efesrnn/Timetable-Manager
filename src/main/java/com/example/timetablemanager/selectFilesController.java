package com.example.timetablemanager;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class selectFilesController {

    @FXML
    private Button selectCourseButton;

    @FXML
    private Button selectClassroomButton;

    @FXML
    private Button okButton;

    @FXML
    private Button backButton;

    @FXML
    private Label selectedCourseLabel;
    @FXML
    private Label selectedClassroomLabel;

    @FXML
    private VBox rootVBox;

    @FXML
    private Label loadingLabel;

    @FXML
    private ProgressBar progressBar;

    private File selectedCourseFile;
    private File selectedClassroomFile;

    private static final String NORMAL_STYLE = "-fx-background-color: #f0f0f0; " +
            "-fx-border-color: #d4d4d4; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-text-fill: #333333;" +
            "-fx-font-weight: bold;";

    private static final String HOVER_STYLE = "-fx-background-color: #e0e0e0; " +
            "-fx-border-color: #d4d4d4; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-text-fill: #333333; " +
            "-fx-cursor: hand;" +
            "-fx-font-weight: bold;";

    private static final String NORMAL_STYLE2 = "-fx-background-color: #f0f0f0; " +
            "-fx-border-color: #d4d4d4; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-text-fill: #333333;";

    private static final String HOVER_STYLE2 = "-fx-background-color: #e0e0e0; " +
            "-fx-border-color: #d4d4d4; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-text-fill: #333333; " +
            "-fx-cursor: hand;";

    @FXML
    public void initialize() {
        selectCourseButton.setOnAction(event -> selectCourseFile());
        selectClassroomButton.setOnAction(event -> selectClassroomFile());
        okButton.setOnAction(event -> {
            processFiles();
            loadingLabel.setVisible(true);
            progressBar.setVisible(true);

        });
        backButton.setOnAction(event -> goBack());

        rootVBox.setFocusTraversable(true);
        rootVBox.requestFocus();

        rootVBox.setOnKeyPressed(this::handleKeyPress);

        setupHoverEffect2(selectCourseButton);
        setupHoverEffect2(selectClassroomButton);
        setupHoverEffect(okButton);
        setupHoverEffect(backButton);

        progressBar.setProgress(0);
    }

    private void setupHoverEffect(Button button) {
        button.setOnMouseEntered(event -> button.setStyle(HOVER_STYLE));
        button.setOnMouseExited(event -> button.setStyle(NORMAL_STYLE));
        button.setStyle(NORMAL_STYLE);
    }

    private void setupHoverEffect2(Button button) {
        button.setOnMouseEntered(event -> button.setStyle(HOVER_STYLE2));
        button.setOnMouseExited(event -> button.setStyle(NORMAL_STYLE2));
        button.setStyle(NORMAL_STYLE2);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            goBack();
        }
    }

    private void selectCourseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Course CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) selectCourseButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedCourseFile = file;
            selectedCourseLabel.setText(file.getName());
            System.out.println("Selected Course file: " + file.getAbsolutePath());
        } else {
            selectedCourseLabel.setText("No file selected");
        }
    }

    private void selectClassroomFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Classroom Capacity CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) selectClassroomButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedClassroomFile = file;
            selectedClassroomLabel.setText(file.getName());
            System.out.println("Selected Classroom Capacity file: " + file.getAbsolutePath());
        } else {
            selectedClassroomLabel.setText("No file selected");
        }
    }

    private void processFiles() {
        if (selectedCourseFile == null || selectedClassroomFile == null) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Selection", "Please select both Course and Classroom Capacity CSV files.");
            return;
        }

        try {
            File projectDirectory = new File(System.getProperty("user.dir"));

            // Copy Course CSV
            File destinationCourse = new File(projectDirectory, selectedCourseFile.getName());
            Files.copy(selectedCourseFile.toPath(), destinationCourse.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Course file copied to: " + destinationCourse.getAbsolutePath());

            // Copy Classroom Capacity CSV
            File destinationClassroom = new File(projectDirectory, selectedClassroomFile.getName());
            Files.copy(selectedClassroomFile.toPath(), destinationClassroom.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Classroom Capacity file copied to: " + destinationClassroom.getAbsolutePath());

            // At this point, we have both CSV files in the project directory.
            // We'll run the database integration (reading these CSVs and inserting into DB) in a Task.

            runDatabaseIntegrationTask(destinationCourse, destinationClassroom);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to process the selected files.");
        }
    }

    private void runDatabaseIntegrationTask(File courseCSV, File classroomCSV) {
        Task<Void> integrationTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Connect to the database
                Database.connect();

                // Count total lines (excluding headers) in both CSV files to know total steps
                int totalSteps = countLines(courseCSV) - 1; // minus 1 for header
                totalSteps += (countLines(classroomCSV) - 1); // minus 1 for header
                if (totalSteps <= 0) totalSteps = 1; // Avoid division by zero if empty

                int currentStep = 0;

                // Process Course CSV (Insert courses, students, enrollments)
                try (BufferedReader br = new BufferedReader(new FileReader(courseCSV))) {
                    // Assume first line is header
                    String line = br.readLine();
                    while ((line = br.readLine()) != null) {
                        String[] columns = line.split(";");
                        if (columns.length < 4) {
                            System.err.println("Skipping invalid line (not enough columns): " + line);
                            continue;
                        }

                        String courseName = columns[0];
                        String startTime = columns[1];

                        // Validate and extract duration
                        String durationStr = columns[2].replaceAll("[^0-9]", "");
                        int duration = 0;
                        if (!durationStr.isEmpty()) {
                            try {
                                duration = Integer.parseInt(durationStr);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid duration value for course '" + courseName + "': " + columns[2]);
                                continue;
                            }
                        } else {
                            System.err.println("Empty or invalid duration value for course '" + courseName + "'");
                            continue;
                        }

                        String lecturer = columns[3];
                        List<String> students = new ArrayList<>();
                        for (int i = 4; i < columns.length; i++) {
                            students.add(columns[i]);
                        }

                        // Insert course
                        Database.addCourse(courseName, lecturer, duration, startTime);

                        // Insert students and enrollments
                        for (String student : students) {
                            Database.addStudent(student);
                            Database.addEnrollment(courseName, student);
                        }

                        currentStep++;
                        updateProgress(currentStep, totalSteps);
                    }
                }

                // Process Classroom CSV (Insert classrooms and allocate courses)
                // We can load all courses from DB once
                List<Course> allCourses = Database.getAllCourses();

                try (BufferedReader br = new BufferedReader(new FileReader(classroomCSV))) {
                    String line = br.readLine(); // header
                    while ((line = br.readLine()) != null) {
                        String[] columns = line.split(";");
                        if (columns.length < 2) {
                            System.err.println("Skipping invalid line (not enough columns): " + line);
                            continue;
                        }
                        String classroomName = columns[0];
                        String capacityStr = columns[1];
                        int capacity;
                        try {
                            capacity = Integer.parseInt(capacityStr);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid capacity value for classroom '" + classroomName + "': " + capacityStr);
                            continue;
                        }

                        Database.addClassroom(classroomName, capacity);
                        System.out.println("Classroom added: " + classroomName + " with capacity: " + capacity);

                        // Allocate each course to the current classroom
                        for (Course course : allCourses) {
                            Database.allocateCourseToClassroom(course.getCourseID(), classroomName);
                            System.out.println("Allocated course: " + course.getCourseID() + " to classroom: " + classroomName);
                        }

                        currentStep++;
                        updateProgress(currentStep, totalSteps);
                    }
                }

                return null;
            }
        };

        integrationTask.setOnSucceeded(event -> navigateToMainLayout());
        integrationTask.setOnFailed(event -> showAlert(Alert.AlertType.ERROR, "Error", "Database integration failed."));

        progressBar.progressProperty().bind(integrationTask.progressProperty());

        Thread thread = new Thread(integrationTask);
        thread.setDaemon(true);
        thread.start();
    }

    private int countLines(File file) throws IOException {
        int lines = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while (br.readLine() != null) {
                lines++;
            }
        }
        return lines;
    }

    private void navigateToMainLayout() {
        try {
            URL mainLayoutUrl = getClass().getResource("/com/example/timetablemanager/mainLayout.fxml");
            if (mainLayoutUrl == null) {
                System.err.println("Cannot find mainLayout.fxml");
                showAlert(Alert.AlertType.ERROR, "Error", "mainLayout.fxml not found.");
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(mainLayoutUrl);
            Parent root = fxmlLoader.load();

            Stage stage = (Stage) okButton.getScene().getWindow();
            stage.setTitle("Timetable Manager - Main Layout");
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load the main layout.");
        }
    }

    private void goBack() {
        try {
            URL welcomeFxmlUrl = getClass().getResource("/com/example/timetablemanager/welcomeLayout.fxml");
            if (welcomeFxmlUrl == null) {
                System.err.println("Cannot find welcomeLayout.fxml");
                showAlert(Alert.AlertType.ERROR, "Error", "welcomeLayout.fxml not found.");
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(welcomeFxmlUrl);
            Parent root = fxmlLoader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setTitle("Timetable Manager - Welcome");
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to return to the welcome screen.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon.");
            e.printStackTrace();
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
