package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class studentSelectionController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> listViewAvailable;

    @FXML
    private ListView<String> listViewSelected;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnRemove;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnCancel;

    private List<Student> allStudents;
    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();

    public void initialize() {
        // Fetch actual students from the database
        allStudents = Database.getAllStudents();

        // Initially display all students
        listViewAvailable.setItems(FXCollections.observableArrayList(
                allStudents.stream()
                        .map(student -> student.getStudentId() + " | " + student.getFullName())
                        .toList()
        ));

        // Add listener for searching/filtering available students
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Filter allStudents based on search text
            List<String> filtered = allStudents.stream()
                    .filter(student -> student.getFullName().toLowerCase().contains(newValue.toLowerCase())
                            || student.getStudentId().toLowerCase().contains(newValue.toLowerCase()))
                    .map(student -> student.getStudentId() + " | " + student.getFullName())
                    .toList();
            listViewAvailable.setItems(FXCollections.observableArrayList(filtered));
        });

        // ADD BUTTON: Move selected students from available to selected
        btnAdd.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewAvailable.getSelectionModel().getSelectedItems();
            // Add to selected list
            listViewSelected.getItems().addAll(selectedItems);
            // Remove from available list
            listViewAvailable.getItems().removeAll(selectedItems);
        });

        // REMOVE BUTTON: Move selected students from selected back to available
        btnRemove.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewSelected.getSelectionModel().getSelectedItems();
            // Add back to available
            listViewAvailable.getItems().addAll(selectedItems);
            // Remove from selected
            listViewSelected.getItems().removeAll(selectedItems);
        });

        // CANCEL BUTTON: Just close the stage without saving
        btnCancel.setOnAction(event -> closeStage());

        // SAVE BUTTON: Map selected strings back to Student objects and close
        btnSave.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewSelected.getItems();
            // Clear and refetch actual students matching the selected strings
            selectedStudents.clear();
            selectedStudents.addAll(
                    allStudents.stream()
                            .filter(student -> selectedItems.contains(student.getStudentId() + " | " + student.getFullName()))
                            .toList()
            );
            closeStage();
        });
    }

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public ObservableList<Student> getSelectedStudents() {
        return selectedStudents;
    }
}
