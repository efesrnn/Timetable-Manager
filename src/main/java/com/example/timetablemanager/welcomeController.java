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

public class welcomeController {

    @FXML
    private Button startBlankButton;

    @FXML
    private Button openCSVButton;

    @FXML
    private ImageView logoImageView;

    @FXML
    public void initialize() {

        loadLogo();
        //"Start with Blank CSV" button calling related method:
        startBlankButton.setOnAction(event -> startWithBlankCSV());

        //"Open Existing CSV" button calling related method:
        openCSVButton.setOnAction(event -> openExistingCSV());
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

    private void openExistingCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        try {
            //TODO: Add a method to copy file to ".../data" directory if user import a .csv file from another directory.

            File initialDirectory = new File("src/main/resources/com/example/timetablemanager/data");
            if (initialDirectory.exists() && initialDirectory.isDirectory()) {
                fileChooser.setInitialDirectory(initialDirectory);
            } else {
                showAlert("Warning", "Default directory not found. Using user home directory.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to locate the default directory. Using user home directory.");
        }

        Stage stage = (Stage) openCSVButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Pass the file path to the main application layout
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainLayout.fxml"));
                javafx.scene.Parent root = fxmlLoader.load();

                // Pass the file path to the controller of the main layout
                ttManagerController mainController = fxmlLoader.getController();
                mainController.loadTimetableFromCSV(selectedFile);
                stage.setTitle("Timetable Manager - " + selectedFile.getName());
                Scene scene = stage.getScene();
                scene.setRoot(root);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to load the layout or the CSV file.");
            }
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
