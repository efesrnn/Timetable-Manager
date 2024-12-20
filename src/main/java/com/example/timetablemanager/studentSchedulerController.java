package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.List;

import static com.example.timetablemanager.Database.*;
import static com.example.timetablemanager.studentSelectionController.*;



public class studentSchedulerController {
    @FXML
    private GridPane GripPane;

    private List<Student> allStudents;

    private String selectedStudent;

    public studentSchedulerController(String selectedStudent) {
        this.selectedStudent = selectedStudent;
    }

    @FXML
    public void initialize() {

    }
    public void showStudent() {

        List<Course> enrolledCourses2  = Database.loadCoursesofStudents(getSelectedStudent());

        if(enrolledCourses2==null) {
            System.out.println("noş");
        } else {
            System.out.println("değikl");
        }

        for (Course course : enrolledCourses2) {
                System.out.println(course);
            }

//        allStudents = Database.getStudents();
//
//
//
//
//        Student selectedStudentObject = allStudents.stream()
//                .filter(student -> student.getFullName().equals(getSelectedStudent()))
//                .findFirst().orElse(null);
//
//        if (selectedStudentObject == null) {
//            showAlert("Error", "Invalid selection. Please try again.");
//            return;
//        }
//        ;
//        List<Course> enrolledCourses = selectedStudentObject.getEnrolledCourses();
//
//        if(enrolledCourses==null) {
//            System.out.println("Boş mu");
//        } else {
//            System.out.println("boş değil");
//        }
//
//        if (enrolledCourses != null && !enrolledCourses.isEmpty()) {
//            System.out.println("Enrolled Courses:");
//            for (Course course : enrolledCourses) {
//                System.out.println(course); // Course sınıfında toString metodu varsa anlamlı şekilde yazdırır.
//            }
//        } else {
//            System.out.println("The student is not enrolled in any courses.");
//        }
//        List<String> c = selectedStudentObject.getSchedule();
//
//
//        if (c != null && !c.isEmpty()) {
//            System.out.println("Student's Schedule:");
//            for (String scheduleEntry : c) {
//                System.out.println(scheduleEntry);
//            }
//        } else {
//            System.out.println("The student has no schedule available.");
//        }









        //ClassroomListView2.setItems(FXCollections.observableArrayList(classroom1));
    }

    public String getSelectedStudent() {
        return selectedStudent;
    }

    public void setSelectedStudent(String selectedStudent) {
        this.selectedStudent = selectedStudent;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }




}
