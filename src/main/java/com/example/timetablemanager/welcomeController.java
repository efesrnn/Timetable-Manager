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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

    public void openExistingCSV() {
        // Create a File Chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a CSV file");

        // Filter for CSV files
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        // Create a new Stage (window)
        Stage stage = new Stage();
        // User selects a file
        File selectedFile = fileChooser.showOpenDialog(stage);

        // If a file is selected
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());

            // Project directory
            File projectDirectory = new File(System.getProperty("user.dir"));
            // Path where the file will be copied in the project directory
            File destinationFile = new File(projectDirectory, selectedFile.getName());

            try {
                // Copy the file to the project directory
                Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File successfully copied to project directory: " + destinationFile.getAbsolutePath());

                // Read the CSV file using Timetable class
                TimetableManager.readCSV(selectedFile.getAbsolutePath());

            } catch (IOException e) {
                // Print error message in case of an error
                System.err.println("Error while copying the file: " + e.getMessage());
            }
        } else {
            // Print message if no file is selected
            System.out.println("No file selected.");
        }
    }
        public void openCSV_ClassCapButton()
        {
            // Create a File Chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select a Classroom CSV file");

            // Filter for CSV files
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );

            // Create a new Stage (window)
            Stage stage = new Stage();
            // User selects a file
            File selectedFile = fileChooser.showOpenDialog(stage);

            // If a file is selected
            if (selectedFile != null) {
                System.out.println("Selected classroom file: " + selectedFile.getAbsolutePath());

                // Project directory
                File projectDirectory = new File(System.getProperty("user.dir"));
                // Path where the file will be copied in the project directory
                File destinationFile = new File(projectDirectory, selectedFile.getName());

                try {
                    // Copy the classroom file to the project directory
                    Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Classroom file successfully copied to project directory: " + destinationFile.getAbsolutePath());

                    // Process the classroom CSV file using Timetable class (with a default method)
                    TimetableManager.readClassroomCSV(selectedFile.getAbsolutePath());

                } catch (IOException e) {
                    System.err.println("Error while copying the classroom file: " + e.getMessage());
                }
            } else {
                System.out.println("No classroom file selected.");
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
