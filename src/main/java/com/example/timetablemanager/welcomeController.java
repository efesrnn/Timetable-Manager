package com.example.timetablemanager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class welcomeController {

    @FXML
    private Button startBlankButton;

    @FXML
    private ImageView logoImageView;

    @FXML
    public void initialize() {

        loadLogo();
        //"Start with Blank CSV" button calling related method:
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

    private void startWithBlankCSV() {
        try {
            // Load the main application layout (with an empty timetable)
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainLayout.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();

            // Getting the current stage:
            Stage stage = (Stage) startBlankButton.getScene().getWindow();
            stage.setTitle("Timetable Manager - New Timetable");
            Scene scene = stage.getScene();
            scene.setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the main layout.");
        }
    }

    @FXML
    public void selectAndAnalyzeCSVFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Classroom and Course CSV Files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = new Stage();
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
                    TimetableManager.readClassroomCSV(classroomFile.getAbsolutePath());
                    TimetableManager.readCSV(courseFile.getAbsolutePath());
                    System.out.println("Files successfully processed.");

                    // Dosyalar başarıyla yüklendikten sonra ana ekranı yükle
                    loadMainLayout();

                } catch (Exception e) {
                    showAlert("Error", "Error processing files: " + e.getMessage());
                }
            } else {
                showAlert("Error", "Please select valid Classroom and Course CSV files.");
            }
        } else {
            showAlert("Error", "You must select exactly two CSV files.");
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

    private void loadMainLayout() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainLayout.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();

            Stage stage = (Stage) startBlankButton.getScene().getWindow();
            stage.setTitle("Timetable Manager - Main View");
            Scene scene = stage.getScene();
            scene.setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the main layout.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
