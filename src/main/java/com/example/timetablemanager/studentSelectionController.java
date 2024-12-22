package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
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
    private Button btnAdd, btnRemove, btnSave, btnCancel;

    @FXML
    private Label lblSelectionInfo;  // <-- The label from FXML

    private static final String dbPath = System.getProperty("user.home")
            + File.separator
            + "Documents"
            + File.separator
            + "TimetableManagement";
    private static final String DB_URL = "jdbc:sqlite:" + dbPath + File.separator + "TimetableManagement.db";
    private Connection conn;

    private List<Student> allStudents = new ArrayList<>();
    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();

    /**
     * If we know the actual classroom capacity, store it here.
     * If not, set it to -1 => means "unknown capacity => '?'"
     */
    private int courseCapacity = -1;

    /**
     * Optionally, set the capacity from outside.
     * If it is unknown, you can call setCourseCapacity(-1).
     */
    public void setCourseCapacity(int capacity) {
        // If capacity < 0 => unknown => show '?'
        this.courseCapacity = capacity;
        updateSelectionInfo();
    }

    @FXML
    public void initialize() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to the database.");
            loadStudents();
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database: " + e.getMessage());
        }

        // Enable multiple selection in ListView
        listViewAvailable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listViewSelected.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Filter students on search input
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            List<String> filtered = allStudents.stream()
                    .filter(student -> student.getFullName().toLowerCase()
                            .contains(newValue.toLowerCase()))
                    .map(Student::getFullName)
                    .distinct()
                    .toList();
            listViewAvailable.setItems(FXCollections.observableArrayList(filtered));
        });

        // ADD BUTTON
        btnAdd.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewAvailable.getSelectionModel().getSelectedItems();
            listViewSelected.getItems().addAll(selectedItems);
            listViewAvailable.getItems().removeAll(selectedItems);
            updateSelectionInfo();
        });

        // REMOVE BUTTON
        btnRemove.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewSelected.getSelectionModel().getSelectedItems();
            listViewAvailable.getItems().addAll(selectedItems);
            listViewSelected.getItems().removeAll(selectedItems);
            updateSelectionInfo();
        });

        // CANCEL BUTTON => close popup
        btnCancel.setOnAction(event -> closeStage());

        // SAVE BUTTON => finalize selection
        btnSave.setOnAction(event -> {
            // Build a new list of selectedStudents from listViewSelected
            ObservableList<String> selectedItems = listViewSelected.getItems();
            selectedStudents.clear();
            selectedStudents.addAll(
                    allStudents.stream()
                            .filter(stu -> selectedItems.contains(stu.getFullName()))
                            .toList()
            );
            // Close popup => or do capacity checks, etc.
            closeStage();
        });
    }

    /**
     * Load all student names from DB into allStudents + show them in listViewAvailable.
     */
    private void loadStudents() {
        String query = "SELECT DISTINCT studentName FROM Students"; // Using DISTINCT
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            allStudents.clear();
            while (rs.next()) {
                String name = rs.getString("studentName");
                allStudents.add(new Student(name, new ArrayList<>()));
            }
            // Remove duplicates if any, just to be safe
            allStudents = allStudents.stream().distinct().toList();

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

    /**
     * If we want to pre-select certain students before the popup.
     */
    public void setInitiallySelectedStudents(List<String> selectedStudentNames) {
        // Move these students from available to selected initially
        List<String> toSelect = new ArrayList<>();
        for (String name : selectedStudentNames) {
            if (listViewAvailable.getItems().contains(name)) {
                toSelect.add(name);
            }
        }
        listViewAvailable.getItems().removeAll(toSelect);
        listViewSelected.getItems().addAll(toSelect);

        updateSelectionInfo();
    }

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public ObservableList<Student> getSelectedStudents() {
        return selectedStudents;
    }

    /**
     * Update the info label => "Selected Students: X / capacity"
     * If capacity == -1 => show '?' instead.
     */
    private void updateSelectionInfo() {
        if (lblSelectionInfo != null) {
            int selectedCount = listViewSelected.getItems().size();
            String capStr = (courseCapacity < 0) ? "?" : String.valueOf(courseCapacity);
            lblSelectionInfo.setText("Selected Students: " + selectedCount + " / " + capStr);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass()
                    .getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
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
