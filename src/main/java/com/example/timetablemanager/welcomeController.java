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
import java.text.SimpleDateFormat;
import java.util.*;

public class welcomeController {

    @FXML
    private Button startBlankButton, openCSVButton, cancelButton;

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

    /**
     * Loads the welcome screen logo if available.
     */
    private void loadLogo() {
        try {
            Image logoImage = new Image(
                    getClass().getResourceAsStream("/com/example/timetablemanager/icons/welcomeIcon.png"));
            logoImageView.setImage(logoImage);
        } catch (Exception e) {
            System.err.println("Error loading logo image: " + e.getMessage());
        }
    }

    /**
     * Called from TimetableManager after the welcome screen is loaded.
     * 1) Checks if TimetableManagement.db exists in Documents/TimetableManagement.
     * 2) If yes, ask user if they'd like to continue from that DB.
     *    - If user says Yes -> load DB, go to main layout (no CSV re-import).
     *    - If user says No -> remain on welcome screen to either Start Blank or Open CSV.
     */
    public void checkAndLoadCSVFiles() {
        File dbDir = new File(System.getProperty("user.home"), "Documents" + File.separator + "TimetableManagement");
        File dbFile = new File(dbDir, "TimetableManagement.db");

        if (dbFile.exists()) {
            boolean userWantsExistingDB = showLoadDatabaseAlert();
            if (userWantsExistingDB) {
                // user wants to keep using existing DB
                try {
                    Connection conn = Database.connect();
                    if (conn == null) {
                        throw new SQLException("Failed to connect to the existing database.");
                    }
                    // Clear and re-fetch in-memory data
                    TimetableManager.getTimetable().clear();
                    TimetableManager.getTimetable().addAll(Database.getAllCourses());

                    // Navigate straight to main layout
                    navigateToMainLayout();
                    return; // done
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Unable to use existing DB: " + e.getMessage());
                }
            }
        }
        // If user said No or no DB file is found,
        // we do nothing else here; the user must pick "Start Blank" or "Open CSV."
    }

    /**
     * "Start Blank" means:
     *  1) Backup old DB if it exists
     *  2) Clear all tables => effectively a brand new, empty DB
     *  3) Then go to main layout (no CSV import)
     */
    private void startWithBlankCSV() {
        // 1) Backup existing DB if present
        backupExistingDbFile();

        try {
            // 2) Clear the DB so it's truly blank
            Connection conn = Database.connect();
            clearDatabase(conn); // new method (see below)

            // 3) Load the main layout
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("mainLayout.fxml"));
            Parent root = fxmlLoader.load();

            Stage stage = (Stage) startBlankButton.getScene().getWindow();
            stage.setTitle("Timetable Manager - New Timetable");
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load the main layout.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to clear database for blank start: " + e.getMessage());
        }
    }

    /**
     * "Open CSV" button => user picks 2 CSV files manually.
     * Once chosen, we:
     *  1) Backup existing DB if it exists
     *  2) Clear the DB
     *  3) Integrate CSV data into the now-empty DB
     *  4) Load main layout
     */
    @FXML
    public void selectAndAnalyzeCSVFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Classroom and Course CSV Files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = (Stage) openCSVButton.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles == null || selectedFiles.size() != 2) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must select exactly two CSV files.");
            return;
        }

        File classroomFile = null;
        File courseFile = null;

        // Decide which is classroom vs course by scanning keywords
        for (File file : selectedFiles) {
            String fileType = analyzeFileContent(file);
            if ("classroom".equals(fileType)) {
                classroomFile = file;
            } else if ("course".equals(fileType)) {
                courseFile = file;
            }
        }

        if (classroomFile == null || courseFile == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Please select valid Classroom and Course CSV files.\n" +
                            "(The files must contain a keyword 'classroom' or 'course').");
            return;
        }

        // 1) Backup existing DB if present
        backupExistingDbFile();

        try {
            // 2) Clear the DB so it's truly blank
            Connection conn = Database.connect();
            clearDatabase(conn);

            // Show progress + integrate files
            selectedFilesLabel.setText("Selected Course File: " + courseFile.getName() +
                    "\nSelected Classroom File: " + classroomFile.getName());
            selectedFilesLabel.setVisible(true);
            startBlankButton.setVisible(false);
            openCSVButton.setVisible(false);
            loadingLabel.setVisible(true);
            progressBar.setVisible(true);
            cancelButton.setVisible(true);

            // 3) Now run database integration
            runDatabaseIntegrationTask(courseFile, classroomFile);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error",
                    "Failed to clear database before CSV import: " + e.getMessage());
        }
    }

    /**
     * Utility: If a TimetableManagement.db already exists in Documents, copy it
     * to `saves/` directory under your project root, named with a timestamp,
     * e.g. dbLog[22.12.24](13.25).db
     */
    private void backupExistingDbFile() {
        File dbDir = new File(System.getProperty("user.home"),
                "Documents" + File.separator + "TimetableManagement");
        File dbFile = new File(dbDir, "TimetableManagement.db");

        if (!dbFile.exists()) {
            return; // no DB => no backup needed
        }

        // Build saves/ subdirectory in your project root
        File savesDir = new File("saves");
        if (!savesDir.exists()) {
            savesDir.mkdirs();
        }

        // Create a date-time stamp for the backup name
        String timeStamp = new SimpleDateFormat("dd.MM.yy_HH.mm")
                .format(new Date());
        String backupName = "dbLog[" + timeStamp + "].db";

        File backupFile = new File(savesDir, backupName);
        try {
            Files.copy(dbFile.toPath(), backupFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backed up existing DB to: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "Backup Failed",
                    "Couldn't backup existing DB: " + e.getMessage());
        }
    }

    /**
     * Clears all rows from every table, effectively leaving an empty DB.
     * Adjust if you have constraints or special table dependencies.
     */
    private void clearDatabase(Connection conn) throws SQLException {
        // Because of foreign keys, might need to disable/enable constraints or
        // delete in correct order. For example:
        try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM Allocated");
             PreparedStatement ps2 = conn.prepareStatement("DELETE FROM Enrollments");
             PreparedStatement ps3 = conn.prepareStatement("DELETE FROM Students");
             PreparedStatement ps4 = conn.prepareStatement("DELETE FROM Classrooms");
             PreparedStatement ps5 = conn.prepareStatement("DELETE FROM Courses")) {
            ps1.executeUpdate();
            ps2.executeUpdate();
            ps3.executeUpdate();
            ps4.executeUpdate();
            ps5.executeUpdate();
        }
        // Also clear in-memory lists:
        TimetableManager.getTimetable().clear();
        System.out.println("Database tables cleared. Now empty.");
    }

    /**
     * Reads the lines from a CSV to guess "classroom" or "course" by keywords.
     */
    private String analyzeFileContent(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains("classroom")) {
                    return "classroom";
                } else if (line.contains("course")) {
                    return "course";
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * Run a background Task to read and integrate the CSV data into the (now-blank) DB.
     */
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

                String insertCourseSQL =
                        "INSERT OR REPLACE INTO Courses (courseName, lecturer, duration, timeToStart) "
                                + "VALUES (?, ?, ?, ?)";
                PreparedStatement courseStmt = conn.prepareStatement(insertCourseSQL);

                String insertStudentSQL =
                        "INSERT OR REPLACE INTO Students (studentName) VALUES (?)";
                PreparedStatement studentStmt = conn.prepareStatement(insertStudentSQL);

                String insertEnrollmentSQL =
                        "INSERT OR REPLACE INTO Enrollments (courseName, studentName) VALUES (?, ?)";
                PreparedStatement enrollmentStmt = conn.prepareStatement(insertEnrollmentSQL);

                String insertClassroomSQL =
                        "INSERT OR REPLACE INTO Classrooms (classroomName, capacity) VALUES (?, ?)";
                PreparedStatement classroomStmt = conn.prepareStatement(insertClassroomSQL);

                // 1) Process Course CSV
                try (BufferedReader br = new BufferedReader(new FileReader(courseCSV))) {
                    String line = br.readLine(); // skip header
                    while ((line = br.readLine()) != null) {
                        String[] columns = line.split(";");
                        if (columns.length < 4) {
                            System.err.println("Skipping invalid line: " + line);
                            continue;
                        }
                        String courseName = columns[0];
                        String startTime = columns[1];

                        String durStr = columns[2].replaceAll("[^0-9]", "");
                        int duration = 0;
                        if (!durStr.isEmpty()) {
                            try {
                                duration = Integer.parseInt(durStr);
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid duration for " + courseName + ": " + columns[2]);
                                continue;
                            }
                        }
                        String lecturer = columns[3];

                        // Next columns might be students
                        List<String> students = new ArrayList<>();
                        for (int i = 4; i < columns.length; i++) {
                            students.add(columns[i]);
                        }

                        // Insert course
                        courseStmt.setString(1, courseName);
                        courseStmt.setString(2, lecturer);
                        courseStmt.setInt(3, duration);
                        courseStmt.setString(4, startTime);
                        courseStmt.addBatch();

                        // Insert students + enrollments
                        for (String s : students) {
                            studentStmt.setString(1, s);
                            studentStmt.addBatch();

                            enrollmentStmt.setString(1, courseName);
                            enrollmentStmt.setString(2, s);
                            enrollmentStmt.addBatch();
                        }

                        currentStep++;
                        updateProgress(currentStep, totalSteps);
                    }
                }

                // 2) Process Classroom CSV
                try (BufferedReader br = new BufferedReader(new FileReader(classroomCSV))) {
                    String line = br.readLine(); // skip header
                    while ((line = br.readLine()) != null) {
                        String[] columns = line.split(";");
                        if (columns.length < 2) {
                            System.err.println("Skipping invalid line: " + line);
                            continue;
                        }
                        String classroomName = columns[0];
                        String capacityStr = columns[1];

                        int capacity;
                        try {
                            capacity = Integer.parseInt(capacityStr);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid capacity for classroom " + classroomName);
                            continue;
                        }

                        // Insert classroom
                        classroomStmt.setString(1, classroomName);
                        classroomStmt.setInt(2, capacity);
                        classroomStmt.addBatch();

                        currentStep++;
                        updateProgress(currentStep, totalSteps);
                    }
                }

                // Execute batches
                courseStmt.executeBatch();
                studentStmt.executeBatch();
                enrollmentStmt.executeBatch();
                classroomStmt.executeBatch();

                // commit
                conn.commit();
                conn.setAutoCommit(true);

                // Reload into in-memory list
                TimetableManager.getTimetable().clear();
                TimetableManager.getTimetable().addAll(Database.getAllCourses());

                return null;
            }
        };

        // On success
        integrationTask.setOnSucceeded(event -> navigateToMainLayout());

        // On fail
        integrationTask.setOnFailed(event -> {
            Throwable ex = integrationTask.getException();
            showAlert(Alert.AlertType.ERROR,
                    "Error", "Database integration failed: " + ex.getMessage());
            System.err.println("DB integration failed: " + ex.getMessage());
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

    /**
     * Once DB is integrated or loaded, navigate to main layout.
     */
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

            // Refresh table or other UI logic
            controller.refreshTable();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load the main layout.");
        }
    }

    /**
     * Cancel integration = hide progress, re-show initial buttons, disconnect DB, etc.
     */
    public void cancelIntegration() {
        loadingLabel.setVisible(false);
        progressBar.setVisible(false);
        selectedFilesLabel.setVisible(false);
        cancelButton.setVisible(false);

        startBlankButton.setVisible(true);
        openCSVButton.setVisible(true);

        // Disconnect from DB and clear in-memory timetable
        Database.close();
        TimetableManager.getTimetable().clear();
    }

    /**
     * Existing DB alert
     */
    private boolean showLoadDatabaseAlert() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        // Attempt to set the alert icon
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(
                    getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon");
            e.printStackTrace();
        }

        alert.setTitle("Existing Database Found");
        alert.setHeaderText(null);
        alert.setContentText(
                "A TimetableManagement.db file was found in your Documents/TimetableManagement folder.\n\n"
                        + "Would you like to continue from that existing database?\n"
                        + "(If you choose No, you'll remain here to import CSV or start blank.)"
        );

        // Style the Alert's root DialogPane
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: #f9f9f9;"
                        + "-fx-border-color: #e0e0e0;"
                        + "-fx-border-width: 1px;"
                        + "-fx-border-radius: 8px;"
                        + "-fx-background-radius: 8px;"
                        + "-fx-padding: 20px;"
                        + "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);"
                        + "-fx-font-family: 'Segoe UI', sans-serif;"
                        + "-fx-font-size: 14px;"
                        + "-fx-text-fill: #333333;"
        );

        // Custom label
        Label contentLabel = new Label(alert.getContentText());
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                "-fx-text-fill: #1c1c1c;"
                        + "-fx-font-size: 14px;"
                        + "-fx-line-spacing: 1.2;"
                        + "-fx-font-family: 'Segoe UI', sans-serif;"
        );
        dialogPane.setContent(contentLabel);

        ButtonType useDBButton = new ButtonType("Use Existing DB", ButtonBar.ButtonData.OK_DONE);
        ButtonType csvButton   = new ButtonType("No, I'll Decide", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(useDBButton, csvButton);

        // Style them
        Button useDBBtnRef = (Button) alert.getDialogPane().lookupButton(useDBButton);
        useDBBtnRef.setStyle(
                "-fx-background-color: #4CAF50;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 5 15;"
                        + "-fx-cursor: hand;"
                        + "-fx-font-size: 14px;"
        );
        useDBBtnRef.setOnMouseEntered(e -> useDBBtnRef.setStyle(
                "-fx-background-color: #45A049;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 5 15;"
                        + "-fx-cursor: hand;"
                        + "-fx-font-size: 14px;"
        ));
        useDBBtnRef.setOnMouseExited(e -> useDBBtnRef.setStyle(
                "-fx-background-color: #4CAF50;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 5 15;"
                        + "-fx-cursor: hand;"
                        + "-fx-font-size: 14px;"
        ));

        Button csvBtnRef = (Button) alert.getDialogPane().lookupButton(csvButton);
        csvBtnRef.setStyle(
                "-fx-background-color: #9E9E9E;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 5 15;"
                        + "-fx-cursor: hand;"
                        + "-fx-font-size: 14px;"
        );
        csvBtnRef.setOnMouseEntered(e -> csvBtnRef.setStyle(
                "-fx-background-color: #7e7e7e;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 5 15;"
                        + "-fx-cursor: hand;"
                        + "-fx-font-size: 14px;"
        ));
        csvBtnRef.setOnMouseExited(e -> csvBtnRef.setStyle(
                "-fx-background-color: #9E9E9E;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-border-radius: 5px;"
                        + "-fx-background-radius: 5px;"
                        + "-fx-padding: 5 15;"
                        + "-fx-cursor: hand;"
                        + "-fx-font-size: 14px;"
        ));

        return alert.showAndWait().orElse(csvButton) == useDBButton;
    }

    /**
     * Quick utility for showing a simple alert.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
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
