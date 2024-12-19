package com.example.timetablemanager;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class welcomeController {

    @FXML
    private Button startBlankButton,openCSVButton,cancelButton;

    @FXML
    private Label loadingLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label selectedFilesLabel;

    @FXML
    private javafx.scene.image.ImageView logoImageView;

    @FXML
    public void initialize() {
        loadLogo();
        startBlankButton.setOnAction(event -> startWithBlankCSV());

    }

    private void loadLogo() {
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/welcomeIcon.png"));
            logoImageView.setImage(logoImage);
        } catch (Exception e) {
            System.err.println("Error loading logo image: " + e.getMessage());
        }
    }


    public void checkAndLoadCSVFiles() {
        String dataPath = "src/main/resources/com/example/timetablemanager/data";
        File dataDir = new File(dataPath);

        if (dataDir.exists() && dataDir.isDirectory()) {
            File[] csvFiles = dataDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (csvFiles != null && csvFiles.length == 2) {
                File classroomFile = null;
                File courseFile = null;

                for (File file : csvFiles) {
                    String fileType = analyzeFileContent(file);
                    if ("classroom".equals(fileType)) {
                        classroomFile = file;
                    } else if ("course".equals(fileType)) {
                        courseFile = file;
                    }
                }

                if (classroomFile != null && courseFile != null) {
                    boolean userConsent = showLoadFilesAlert(classroomFile.getName(), courseFile.getName());
                    if (userConsent) {
                        try {
                            selectedFilesLabel.setText(
                                    "Selected Course File: " + courseFile.getName() +
                                            "\nSelected Classroom File: " + classroomFile.getName()
                            );
                            selectedFilesLabel.setVisible(true);
                            startBlankButton.setVisible(false);
                            openCSVButton.setVisible(false);

                            // Process and load files
                            runDatabaseIntegrationTask(courseFile, classroomFile);
                        } catch (Exception e) {
                            showAlert(Alert.AlertType.ERROR, "Error", "Error processing files: " + e.getMessage());
                        }
                    }
                }
            }else {
                System.err.println("More or less than 2 .csv file detected in directory. Automatic load cancelled.");
            }
        }
    }


    private void startWithBlankCSV() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainLayout.fxml"));
            Parent root = fxmlLoader.load();

            Stage stage = (Stage) startBlankButton.getScene().getWindow();
            stage.setTitle("Timetable Manager - New Timetable");
            Scene scene = stage.getScene();
            scene.setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,"Error", "Failed to load the main layout.");
        }
    }

    @FXML
    public void selectAndAnalyzeCSVFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Classroom and Course CSV Files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = (Stage) openCSVButton.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null && selectedFiles.size() == 2) {
            File classroomFile = null;
            File courseFile = null;

            for (File file : selectedFiles) {
                String fileType = analyzeFileContent(file);
                if ("classroom".equals(fileType)) {
                    classroomFile = file;
                } else if ("course".equals(fileType)) {
                    courseFile = file;
                }
            }

            if (classroomFile != null && courseFile != null) {
                try {
                    // Copy files to resources/data directory
                    File destinationDir = new File("src/main/resources/com/example/timetablemanager/data");
                    if (!destinationDir.exists()) {
                        destinationDir.mkdirs();
                    }

                    File copiedClassroomFile = new File(destinationDir, classroomFile.getName());
                    File copiedCourseFile = new File(destinationDir, courseFile.getName());

                    Files.copy(classroomFile.toPath(), copiedClassroomFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(courseFile.toPath(), copiedCourseFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Show progress and integrate files
                    selectedFilesLabel.setText("Selected Course File: " + copiedCourseFile.getName() + "\nSelected Classroom File: " + copiedClassroomFile.getName());
                    selectedFilesLabel.setVisible(true);
                    startBlankButton.setVisible(false);
                    openCSVButton.setVisible(false);
                    loadingLabel.setVisible(true);
                    progressBar.setVisible(true);
                    cancelButton.setVisible(true);

                    // Run database integration using copied files
                    runDatabaseIntegrationTask(copiedCourseFile, copiedClassroomFile);

                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR,"Error", "Error processing files: " + e.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.ERROR,"Error", "Please select valid Classroom and Course CSV files.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR,"Error", "You must select exactly two CSV files.");
        }
    }

    private String analyzeFileContent(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("classroom")) {
                    return "classroom";
                } else if (line.toLowerCase().contains("course")) {
                    return "course";
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return "unknown";
    }

    private void runDatabaseIntegrationTask(File courseCSV, File classroomCSV) {
        Task<Void> integrationTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Connection conn = Database.connect();
                if (conn == null) {
                    throw new SQLException("Failed to connect to database.");
                }

                // Disable auto-commit for transaction
                conn.setAutoCommit(false);

                int totalSteps = countLines(courseCSV) - 1; // minus 1 for header
                totalSteps += (countLines(classroomCSV) - 1);
                if (totalSteps <= 0) totalSteps = 1;

                int currentStep = 0;

                // Prepare batch statements
                String insertCourseSQL = "INSERT OR REPLACE INTO Courses (courseName, lecturer, duration, timeToStart) VALUES (?, ?, ?, ?)";
                PreparedStatement courseStmt = conn.prepareStatement(insertCourseSQL);

                String insertStudentSQL = "INSERT OR REPLACE INTO Students (studentName) VALUES (?)";
                PreparedStatement studentStmt = conn.prepareStatement(insertStudentSQL);

                String insertEnrollmentSQL = "INSERT OR REPLACE INTO Enrollments (courseName, studentName) VALUES (?, ?)";
                PreparedStatement enrollmentStmt = conn.prepareStatement(insertEnrollmentSQL);

                String insertClassroomSQL = "INSERT OR REPLACE INTO Classrooms (classroomName, capacity) VALUES (?, ?)";
                PreparedStatement classroomStmt = conn.prepareStatement(insertClassroomSQL);

                String insertAllocatedSQL = "INSERT OR REPLACE INTO Allocated (courseName, classroomName) VALUES (?, ?)";
                PreparedStatement allocatedStmt = conn.prepareStatement(insertAllocatedSQL);

                // Process Course CSV (batch inserts)
                List<String[]> classroomData = new ArrayList<>(); // Store classroom data for later
                List<String> courseNamesForClassroom = new ArrayList<>(); // We'll need course names after loading

                // First, read and insert courses + enrollments in batch
                try (BufferedReader br = new BufferedReader(new FileReader(courseCSV))) {
                    String line = br.readLine(); // header
                    while ((line = br.readLine()) != null) {
                        String[] columns = line.split(";");
                        if (columns.length < 4) {
                            System.err.println("Skipping invalid line (not enough columns): " + line);
                            continue;
                        }

                        String courseName = columns[0];
                        String startTime = columns[1];

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
                            System.err.println("Empty or invalid duration for course '" + courseName + "'");
                            continue;
                        }

                        String lecturer = columns[3];
                        List<String> students = new ArrayList<>();
                        for (int i = 4; i < columns.length; i++) {
                            students.add(columns[i]);
                        }

                        // Add course to batch
                        courseStmt.setString(1, courseName);
                        courseStmt.setString(2, lecturer);
                        courseStmt.setInt(3, duration);
                        courseStmt.setString(4, startTime);
                        courseStmt.addBatch();

                        // Batch students and enrollments
                        for (String student : students) {
                            studentStmt.setString(1, student);
                            studentStmt.addBatch();

                            enrollmentStmt.setString(1, courseName);
                            enrollmentStmt.setString(2, student);
                            enrollmentStmt.addBatch();
                        }

                        courseNamesForClassroom.add(courseName);

                        currentStep++;
                        updateProgress(currentStep, totalSteps);
                    }
                }

                // Now process Classroom CSV
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

                        // Add classroom to batch
                        classroomStmt.setString(1, classroomName);
                        classroomStmt.setInt(2, capacity);
                        classroomStmt.addBatch();
/*
                        // We'll allocate each course to this classroom after we load them from DB,
                        // but since we want speed, let's assume we allocate all known courses:
                        // This is a simplification; if you need logic to allocate only specific courses, adjust accordingly.
                        for (String cname : courseNamesForClassroom) {
                            allocatedStmt.setString(1, cname);
                            allocatedStmt.setString(2, classroomName);
                            allocatedStmt.addBatch();
                        }
*/
                        currentStep++;
                        updateProgress(currentStep, totalSteps);
                    }
                }

                // Execute all batches inside one transaction
                courseStmt.executeBatch();
                studentStmt.executeBatch();
                enrollmentStmt.executeBatch();
                classroomStmt.executeBatch();
                allocatedStmt.executeBatch();

                conn.commit(); // commit once
                conn.setAutoCommit(true);

                // Reload courses from DB into in-memory TimetableManager
                TimetableManager.getTimetable().clear();
                TimetableManager.getTimetable().addAll(Database.getAllCourses());

                return null;
            }
        };

        integrationTask.setOnSucceeded(event -> navigateToMainLayout());
        integrationTask.setOnFailed(event -> {
            Throwable ex = integrationTask.getException();
            showAlert(Alert.AlertType.ERROR, "Error", "Database integration failed: " + ex.getMessage());
            System.err.println("Database integration failed: " + ex.getMessage());
            try {
                Connection c = Database.connect();
                if (c != null) c.rollback();
            } catch (SQLException ignored) {}
        });

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
        System.out.println("Courses from DB: " + Database.getAllCourses().size());
        System.out.println("Courses in Timetable after update: " + TimetableManager.getTimetable().size());

        try {
            URL mainLayoutUrl = getClass().getResource("/com/example/timetablemanager/mainLayout.fxml");
            if (mainLayoutUrl == null) {
                System.err.println("Cannot find mainLayout.fxml");
                showAlert(Alert.AlertType.ERROR, "Error", "mainLayout.fxml not found.");
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(mainLayoutUrl);
            Parent root = fxmlLoader.load();
            ttManagerController controller = fxmlLoader.getController();

            Stage stage = (Stage) openCSVButton.getScene().getWindow();
            stage.setTitle("Timetable Manager - Main Layout");
            stage.getScene().setRoot(root);

            controller.refreshTable();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load the main layout.");
        }
    }




    public void cancelIntegration() {
        // Hide progress and loading
        loadingLabel.setVisible(false);
        progressBar.setVisible(false);
        selectedFilesLabel.setVisible(false);
        cancelButton.setVisible(false);

        // Show the startBlankButton and openCSVButton again
        startBlankButton.setVisible(true);
        openCSVButton.setVisible(true);


        // Disconnect from database and clear in-memory timetable
        Database.close(); // Disconnect from database
        TimetableManager.getTimetable().clear(); // Clear the timetable list

    }

    private boolean showLoadFilesAlert(String classroomFileName, String courseFileName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon");
            e.printStackTrace();
        }

        alert.setTitle("Load Files");
        alert.setHeaderText(null);
        alert.setContentText(
                "📂 Detected Files:\n\n" +
                        "- Classroom: " + classroomFileName + "\n" +
                        "- Course: " + courseFileName + "\n\n" +
                        "Would you like to load these files automatically?"
        );
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-padding: 20px; " +
                        "-fx-font-family: 'Segoe UI', sans-serif; " +
                        "-fx-font-size: 14px; " +
                        "-fx-text-fill: #333333;"
        );

        // Customize Content Text
        Label contentLabel = new Label(alert.getContentText());
        contentLabel.setStyle(
                "-fx-text-fill: #202123; " +
                        "-fx-font-size: 14px; " +
                        "-fx-line-spacing: 1.2;"
        );
        contentLabel.setWrapText(true);
        dialogPane.setContent(contentLabel);

        // Add custom buttons
        ButtonType loadButton = new ButtonType("Load Files", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(loadButton, cancelButton);

        // Style buttons
        Button load = (Button) dialogPane.lookupButton(loadButton);
        Button cancel = (Button) dialogPane.lookupButton(cancelButton);

        load.setStyle(
                "-fx-background-color: #10A37F; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-padding: 5 15;"
        );

        cancel.setStyle(
                "-fx-background-color: #F0F0F0; " +
                        "-fx-text-fill: #333333; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 5px; " +
                        "-fx-padding: 5 15;"
        );
        return alert.showAndWait().orElse(cancelButton) == loadButton;
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
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
