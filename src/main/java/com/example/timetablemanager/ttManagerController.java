package com.example.timetablemanager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ttManagerController {

    @FXML
    private Button btnAddCourse,btnEnrollStudent,btnAssignClassroom,btnSwapClassroom,btnSearch;

    @FXML
    private MenuItem menuImportCSV,menuLoadTimetable,menuSaveTimetable,menuExportTimetable,menuExit;

    @FXML
    private MenuItem menuUserManual,menuAbout;

    @FXML
    public void initialize() {

        //BUTTONS:
        //TODO: Search Button
        btnSearch.setOnAction(event -> {

            //The code for search or call for the method of search logic goes here:
            showAlert("Search","Search logic has not attached yet.");
        });

        //TODO: Add Course Button
        btnAddCourse.setOnAction(event -> {
            switchToScene("addCourseLayout.fxml");
        });


        //TODO: Enroll Student Button
        btnEnrollStudent.setOnAction(event ->

                //The code for enroll section or call for the method of enroll section goes here:
                showAlert("Enroll Student", "Enroll Student has not attached yet.")
        );

        //TODO: Assign Classroom Button
        btnAssignClassroom.setOnAction(event ->

                //The code for assign section or call for the method of assign section goes here:
                showAlert("Assign Classroom", "Assign Classroom has not attached yet.")
        );

        //TODO: Swap Classroom Button
        btnSwapClassroom.setOnAction(event ->

                //The code for swap classroom or call for the method of swap classroom goes here:
                showAlert("Swap Classroom", "Swap Classroom has not attached yet.")
        );

        //------------------------------------------------------------------------------------------------

        //MENUBAR

        //TODO: Import CSV Menu Item
        menuImportCSV.setOnAction(event ->

                //The code for import CSV file or call for the method of import CSV file goes here:
                showAlert("Import CSV", "Import CSV has not attached yet.")
        );

        //TODO: Load Timetable Menu Item
        menuLoadTimetable.setOnAction(event ->

                //The code for import CSV file or call for the method of import CSV file goes here:
                showAlert("Load Timetable", "Load Timetable has not attached yet.")
                );

        //TODO: Save Timetable Menu Item
        menuSaveTimetable.setOnAction(event ->

                //The code for save timetable or call for the method of save timetable goes here:
                showAlert("Save Timetable", "Save Timetable has not attached yet.")
                );

        //TODO: Export Timetable Menu Item
        menuExportTimetable.setOnAction(event ->

                //The code for export or call for the method of export goes here:
                showAlert("Export Timetable", "Export Timetable has not attached yet.")
                );

        //TODO: Exit Menu Item (Must check if it saved, if not Alert!)
        menuExit.setOnAction(event ->

                //The code to check if the timetable updated if not exit, if changed show alert to warn user.
                System.exit(0)
        );

        //TODO: User Manual Menu Item
        menuUserManual.setOnAction(event ->

                //Instructions goes here:
                showAlert("User Manual", "User Manual has not attached yet.")
        );

        //TODO: About Menu Item
        menuAbout.setOnAction(event ->

                //About us goes here:
                showAlert("About", "About has not attached yet.")
        );

    }

    //TODO: Database integration logic goes here:
    public void loadTimetableFromCSV(File file) {
        if (file != null) {
            System.out.println("Loading timetable from: " + file.getAbsolutePath());
            //------------------------------------------------------------
            // TODO: Add actual CSV parsing and data population logic here
            //------------------------------------------------------------
        } else {
            System.err.println("File is null, cannot load timetable.");
        }
    }

    private void switchToScene(String fxmlFile) {
        try {
            // Load the new root layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            Object newRoot = loader.load(); // Using Object to avoid type mismatch...

            //TODO: Note for the future: Do not create new scene that will cause visual glitches.
            //TODO: Use parent-children relationship.

            if (!(newRoot instanceof javafx.scene.Parent)) {
                throw new IllegalArgumentException("Loaded root is not a valid JavaFX parent node");
            }

            // Getting the current scene and setting the new root.
            Stage stage = (Stage) btnAddCourse.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot((javafx.scene.Parent) newRoot);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the scene: " + fxmlFile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showAlert("Error", "Invalid root type in FXML: " + fxmlFile);
        }
    }


    //Alert creator method to test all buttons work properly.
    private void showAlert(String title, String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        //Alert icon initialization:
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/alert.png")));
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
