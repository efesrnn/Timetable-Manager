package com.example.timetablemanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.example.timetablemanager.Database.*;


public class TimetableManager extends Application {

    private static List<Course> timetable = new ArrayList<>();

    public static List<Course> getTimetable() {
        return timetable;
    }

    public static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    public static final String filePath = dbPath + File.separator + "Courses.csv";
    public static final String filePath2 = dbPath + File.separator + "ClassroomCapacity.csv";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(TimetableManager.class.getResource("welcomeLayout.fxml"));
        Scene scene = new Scene(fxmlLoader.load(),1080,720);

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

    public static void main(String[] args) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File not found at: " + filePath);
        } else {
            connect();
            List<Course> loaded = readCSV(filePath);
            timetable.addAll(loaded);
            File classroomFile = new File(filePath2);
            if (classroomFile.exists()) {
                readClassroomCSV(filePath2);
            } else {
                System.out.println("Classroom file not found at: " + filePath2);
            }
            printTimetable(timetable);
        }
        connect();
        launch();
        close();
    }

    // New CSV format (example):
    // courseName;courseID;description;capacity;classroom;day;time;duration;student1;student2;...
    public static List<Course> readCSV(String filePath) {
        timetable.clear();
        List<Course> courses = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(";");
                if (columns.length < 8) {
                    System.err.println("Skipping invalid line (not enough columns): " + line);
                    continue;
                }

                String courseName = columns[0];
                String courseID = columns[1];
                String description = columns[2];

                int capacity = 0;
                try {
                    capacity = Integer.parseInt(columns[3]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid capacity value for course '" + courseName + "': " + columns[3]);
                    continue;
                }

                String classroom = columns[4];
                String day = columns[5];
                String time = columns[6];

                int duration = 0;
                try {
                    duration = Integer.parseInt(columns[7]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid duration value for course '" + courseName + "': " + columns[7]);
                    continue;
                }

                List<String> days = new ArrayList<>();
                days.add(day);

                List<String> times = new ArrayList<>();
                times.add(time);

                // Students start from column[8] onwards
                List<Student> studentList = new ArrayList<>();
                for (int i = 8; i < columns.length; i++) {
                    String studentName = columns[i];
                    // Add student to DB
                    Database.addStudent(studentName);
                    studentList.add(new Student("UnknownID", studentName, new ArrayList<>()));
                    // Enroll in DB
                    Database.addEnrollment(courseName, studentName);
                }

                // Add course to DB
                // Database.addCourse(courseName, description, duration, time)
                // The Database.addCourse signature might differ; adjust as needed.
                // If needed, modify Database methods or skip if they don't match this logic.
                // For demonstration, call a hypothetical method:
                // Database.addCourse(courseName, "N/A", duration, time); // or skip if irrelevant
                String lecturer = "N/A";

                Course c = new Course(courseName, courseID, description, capacity, studentList, classroom, days, times, duration , lecturer);
                courses.add(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return courses;
    }

    public static void readClassroomCSV(String filePath2) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath2))) {
            String line = br.readLine(); // Skip header
            List<Course> allCourses = Database.getAllCourses(); // DB courses

            while ((line = br.readLine()) != null) {
                String[] columns = line.split(";");
                if (columns.length < 2) {
                    System.err.println("Skipping invalid line (not enough columns): " + line);
                    continue;
                }
                String classroomName = columns[0];
                int capacity = 0;
                try {
                    capacity = Integer.parseInt(columns[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid capacity value for classroom '" + classroomName + "': " + columns[1]);
                    continue;
                }

                Database.addClassroom(classroomName, capacity);
                System.out.println("Classroom added: " + classroomName + " with capacity: " + capacity);

                // Allocate each course to this classroom
                for (Course course : allCourses) {
                    Database.allocateCourseToClassroom(course.getCourseName(), classroomName);
                    System.out.println("Allocated course: " + course.getCourseName() + " to classroom: " + classroomName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printTimetable(List<Course> timetable) {
        System.out.println();
        System.out.printf("%-15s%-15s%-20s%-10s%-15s%-20s%-50s\n", "CourseName", "CourseID", "Description", "Dur", "Classroom", "Days/Times", "Students");
        System.out.println("---------------------------------------------------------------------------------------------------------------");

        for (Course c : timetable) {
            String dayTime = "Days: " + String.join(",", c.getDays()) + " Times: " + String.join(",", c.getTimes());
            List<String> studentNames = new ArrayList<>();
            for (Student s : c.getStudents()) {
                studentNames.add(s.getFullName());
            }
            System.out.printf("%-15s%-15s%-20s%-10d%-15s%-20s%-50s\n",
                    c.getCourseName(),
                    c.getCourseID(),
                    c.getDescription(),
                    c.getDuration(),
                    c.getClassroom(),
                    dayTime,
                    String.join(", ", studentNames));
        }
    }
}
