package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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

    private List<Student> allStudents = new ArrayList<>();
    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();

    public void initialize() {
        //Sample data for test use for now:
        allStudents = List.of(
                new Student("S001", "Alice Johnson",null),
                new Student("S002", "Bob Smith",null),
                new Student("S003", "Charlie Davis",null),
                new Student("S004", "Diana Brown",null),
                new Student("S005", "Ethan Wilson",null)
        );

        listViewAvailable.setItems(FXCollections.observableArrayList(
                allStudents.stream().map(student -> student.getStudentId() + " | " + student.getFullName()).toList()
        ));

        //Listener to filter available students:
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            listViewAvailable.setItems(FXCollections.observableArrayList(
                    allStudents.stream()
                            .filter(student -> student.getFullName().toLowerCase().contains(newValue.toLowerCase()) ||
                                    student.getStudentId().toLowerCase().contains(newValue.toLowerCase()))
                            .map(student -> student.getStudentId() + " | " + student.getFullName())
                            .toList()
            ));
        });

        //ADD BUTTON:
        btnAdd.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewAvailable.getSelectionModel().getSelectedItems();
            listViewSelected.getItems().addAll(selectedItems);
            listViewAvailable.getItems().removeAll(selectedItems);
        });

        //REMOVE BUTTON:
        btnRemove.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewSelected.getSelectionModel().getSelectedItems();
            listViewAvailable.getItems().addAll(selectedItems);
            listViewSelected.getItems().removeAll(selectedItems);
        });

        //CANCEL BUTTON:
        btnCancel.setOnAction(event -> closeStage());

        //SAVE BUTTON:
        btnSave.setOnAction(event -> {
            ObservableList<String> selectedItems = listViewSelected.getItems();

            //Map selected strings back to Student objects
            selectedStudents.clear();
            selectedStudents.addAll(allStudents.stream()
                    .filter(student -> selectedItems.contains(student.getStudentId() + " | " + student.getFullName()))
                    .toList());

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
