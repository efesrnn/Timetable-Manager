package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
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

    private static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String DB_URL = "jdbc:sqlite:" + dbPath + File.separator + "TimetableManagement.db";
    private Connection conn;
    private List<Student> allStudents = new ArrayList<>();
    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to the database.");
            loadStudents();
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
        }

        // Filter students on search input
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            List<String> filtered = allStudents.stream()
                    .filter(student -> student.getFullName().toLowerCase().contains(newValue.toLowerCase()))
                    .map(Student::getFullName)
                    .distinct()
                    .toList();
            listViewAvailable.setItems(FXCollections.observableArrayList(filtered));
        });

        // ADD BUTTON: Move selected students to the selected list
        btnAdd.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewAvailable.getSelectionModel().getSelectedItems();
            selectedItems.stream()
                    .filter(item -> !listViewSelected.getItems().contains(item))
                    .forEach(listViewSelected.getItems()::add);
            listViewAvailable.getItems().removeAll(selectedItems);
        });

        // REMOVE BUTTON: Move students back to the available list
        btnRemove.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewSelected.getSelectionModel().getSelectedItems();
            selectedItems.stream()
                    .filter(item -> !listViewAvailable.getItems().contains(item))
                    .forEach(listViewAvailable.getItems()::add);
            listViewSelected.getItems().removeAll(selectedItems);
        });

        // CANCEL BUTTON: Close the stage without saving
        btnCancel.setOnAction(event -> closeStage());

        // SAVE BUTTON: Save the selected students
        btnSave.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewSelected.getItems();
            selectedStudents.clear();
            selectedStudents.addAll(
                    allStudents.stream()
                            .filter(student -> selectedItems.contains(student.getFullName()))
                            .toList()
            );
            closeStage();
        });
    }

    private void loadStudents() {
        String query = "SELECT studentId, studentName FROM Students";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            allStudents.clear();
            while (rs.next()) {
                String name = rs.getString("studentName");
                allStudents.add(new Student(name, new ArrayList<>()));
            }

            // Populate the ListView with student names (distinct)
            listViewAvailable.setItems(FXCollections.observableArrayList(
                    allStudents.stream()
                            .map(Student::getFullName)
                            .distinct()
                            .toList()
            ));
        } catch (SQLException e) {
            System.err.println("Error loading students: " + e.getMessage());
        }
    }

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public ObservableList<Student> getSelectedStudents() {
        return selectedStudents;
    }
}
