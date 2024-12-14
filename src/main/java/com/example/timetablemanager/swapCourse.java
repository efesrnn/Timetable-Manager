package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.fxml.FXMLLoader;
import static com.example.timetablemanager.Database.*;

import java.util.ArrayList;
import java.util.List;

public class swapCourse {
    @FXML
    private Label SwapClasroomLabel, CapacityLabel, EnrollmendLabel, ClasssroomLabel, CapacityLabel2;

    @FXML
    private Button btnSave, btnCancel;

    @FXML
    private ListView CapacityListView, EnrolledListView, ClasroomListView, CapacityListView2;

    @FXML
    private ComboBox SelectCourseCombo, SelectClassroomCombo;

    // Fetch real classrooms from the database
    List<String> classrooms = Database.getAllClassroomNames();
    private static List<Course> courseList = new ArrayList<>();
    @FXML
    public void initialize() {
        SelectCourseCombo.setItems(FXCollections.observableArrayList(Database.getAllCourses()));
    }

    public void All() {


    }



}
