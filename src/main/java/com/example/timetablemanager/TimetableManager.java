package com.example.timetablemanager;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class TimetableManager extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(TimetableManager.class.getResource("welcomeLayout.fxml"));
        //Creating the initial scene with 1080pixel width and 720pixel length.
        Scene scene = new Scene(fxmlLoader.load(),1080,720);

        //ICON OF THE APP
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/icon.png")));
        }catch (RuntimeException e){
            System.err.println("Couldn't load icon");
            e.printStackTrace();
        }

        stage.setTitle("Welcome to Timetable Manager");
        stage.setScene(scene);
        stage.show();

        System.out.println("Timetable Manager initialized.");
    }
    private List<Student> students;
    private List<Course> courses;

    public TimetableManager() {
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
    }

    public TimetableManager(List<Course> courses, List<Student> students) {
        this.courses = courses;
        this.students = students;
    }

    public void addStudent(Student student) {
        if (!students.contains(student)) {
            students.add(student);
        }
        System.out.println("Sucseffully added student");
    }

    public Student findStudentById(String studentId) {
        for (Student student : students) {
            if (student.getStudentId().equals(studentId)) {
                System.out.println("StudentId found: " + studentId);
                return student;
            }
        }
        System.out.println("StudentId coudn't found: " + studentId);
        return null;
    }

    public void addCourse(Course course) {
        if (!courses.contains(course)) {
            courses.add(course);
            System.out.println("Course added");
        }
    }

    public Course findCourseById(String courseId) {
        for (Course course : courses) {
            if (course.getCourseID().equals(courseId)) {
                System.out.println("CourseId found: " + courseId);
                return course;
            }
        }
        System.out.println("CourseId coudn't found: " + courseId);
        return null;
    }

    public boolean generateSchedule(Course course, String day, String timeSlot, String classroom) {
        if (checkConflict(day + " " + timeSlot)) {
            course.assignClassroom(classroom);
            System.out.println("Schedule can be crated, returning true");
            return true;
        }
        System.out.println("Schedule can't be crated, returning false");
        return false;
    }

    public boolean checkConflict(String schedule) {
        /* */
        return true;
    }

    public void saveTimetable(String filePath) {
        /* */
    }

    public void loadTimetable(String filePath) {
        /* */
    }

    public List<String> getStudentSchedule(String studentId) {
        Student student = findStudentById(studentId);
        if (student != null) {
            System.out.println("Sucssesful");
            return student.getSchedule();
        } else {
            return new ArrayList<>();
        }
    }

    public List<String> getCourseSchedule(String courseId) {
        Course course = findCourseById(courseId);
        List<String> schedule = new ArrayList<>();
        if (course != null) {
            schedule.add(course.getSchedule());
            System.out.println("Added");
        }
        return schedule;
    }
    public static void main(String[] args) {
        launch();
    }
}
