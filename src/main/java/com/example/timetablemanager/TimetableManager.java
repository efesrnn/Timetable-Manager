package com.example.timetablemanager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TimetableManager {

    public static class Course {
        String name;
        String startTime;
        int duration;
        String lecturer;
        List<String> students;

        public Course(String name, String startTime, int duration, String lecturer, List<String> students) {
            this.name = name;
            this.startTime = startTime;
            this.duration = duration;
            this.lecturer = lecturer;
            this.students = students;
        }
    }

    private static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String filePath = dbPath + File.separator + "Courses.csv";
    private static final String filePath2 = dbPath + File.separator + "ClassroomCapacity.csv";


    public static void main(String[] args) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File not found at: " + filePath);
        } else {
            Database.connect();
            List<Course> timetable = readCSV(filePath);

            File classroomFile = new File(filePath2);
            if (classroomFile.exists()) {
                readClassroomCSV(filePath2,timetable);
            } else {
                System.out.println("Classroom file not found at: " + filePath2);
            }
            printTimetable(timetable);
        }
    }

    // Read CSV file and return list of courses
    private static List<Course> readCSV(String filePath) {
        List<Course> courses = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(";");
                if (columns.length < 4) {
                    System.err.println("Skipping invalid line (not enough columns): " + line);
                    continue;
                }

                String courseName = columns[0];
                String startTime = columns[1];

                // Validate and extract duration
                String durationStr = columns[2].replaceAll("[^0-9]", "");
                int duration = 0;
                if (!durationStr.isEmpty()) {
                    try {
                        duration = Integer.parseInt(durationStr);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid duration value for course '" + courseName + "': " + columns[2]);
                        continue;
                    }
                } else {
                    System.err.println("Empty or invalid duration value for course '" + courseName + "'");
                    continue;
                }

                String lecturer = columns[3];
                List<String> students = new ArrayList<>();
                for (int i = 4; i < columns.length; i++) {
                    students.add(columns[i]);
                }

                // Add course and students to the database
                Database.addCourse(courseName,lecturer,duration,startTime);

                for (String student : students) {
                    Database.addStudent(student);  // Add student to the database
                    Database.addEnrollment(courseName,student);  // Enroll student in the course
                }

                // Add the course to the list
                courses.add(new Course(courseName, startTime, duration, lecturer, students));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // Read and process the classroom CSV file
    private static void readClassroomCSV(String filePath2, List<Course> courses) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath2))) {
            String line;
            br.readLine(); // Skip the header line
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(";");
                if (columns.length < 2) {
                    System.err.println("Skipping invalid line (not enough columns): " + line);
                    continue;
                }
                String classroomName = columns[0];
                String capacityStr = columns[1];
                int capacity = 0;
                try {
                    capacity = Integer.parseInt(capacityStr);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid capacity value for classroom '" + classroomName + "': " + capacityStr);
                    continue;
                }

                // Add the classroom to the database
                Database.addClassroom(classroomName, capacity);
                System.out.println("Classroom: " + classroomName + " with capacity: " + capacity); // Log statement for debugging

                // Allocate each course to the current classroom
                for (Course course : courses) {
                    System.out.println("Allocating course: " + course.name + " to classroom: " + classroomName); // Log statement for debugging
                    Database.allocateCourseToClassroom(course.name, classroomName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Print the timetable in a table-like format
    private static void printTimetable(List<Course> timetable) {
        System.out.println();
        // Print header
        System.out.printf("%-15s%-20s%-10s%-20s%-50s\n", "Course", "Time", "Duration", "Lecturer", "Students");
        System.out.println("---------------------------------------------------------------------------------------------");

        // Print each course in a formatted table
        for (Course course : timetable) {
            String durationString = (course.duration > 0) ? String.valueOf(course.duration) : "";

            System.out.printf("%-15s%-20s%-10s%-20s%-50s\n",
                    course.name,
                    course.startTime,
                    durationString,
                    course.lecturer,
                    String.join(", ", course.students));
        }
    }
}
