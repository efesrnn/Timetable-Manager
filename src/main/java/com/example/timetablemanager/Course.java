package com.example.timetablemanager;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String courseID;
    private int capacity;
    private List<Student> students;
    private String classroom;
    private String timeToStart; // Combined day and time
    private int duration;
    private String lecturer;

    public Course(String courseID, int capacity,
                  List<Student> students, String classroom, String timeToStart, int duration, String lecturer) {
        this.courseID = courseID;
        this.capacity = capacity;
        this.students = students != null ? students : new ArrayList<>();
        this.classroom = classroom;
        this.timeToStart = timeToStart;
        this.duration = duration;
        this.lecturer = lecturer;
    }

    public String getDay() {
        if (timeToStart != null && !timeToStart.isEmpty()) {
            String[] parts = timeToStart.split(" ");
            if (parts.length >= 1) {
                return parts[0];
            }
        }
        return "N/A";
    }

    public String getStartTime() {
        if (timeToStart != null && !timeToStart.isEmpty()) {
            String[] parts = timeToStart.split(" ");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return "N/A";
    }

    /* */
    public void assignClassroom(String classroom) {
        this.classroom = classroom;
    }

    public String getSchedule() {
        return "Course ID: " + courseID + ", Classroom: " + classroom;
    }
    /* */

    // Getters and Setters

    public String getCourseID() {
        return courseID;
    }

    public void setCourseID(String courseID) {
        this.courseID = courseID;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public String getTimeToStart() {
        return timeToStart;
    }

    public void setTimeToStart(String timeToStart) {
        this.timeToStart = timeToStart;
    }

    public int getDuration() { return duration; }

    public void setDuration(int duration) { this.duration = duration; }

    public String getLecturer() { return lecturer; }

    public void setLecturer(String lecturer) { this.lecturer = lecturer; }

    public boolean assignClassroom(String classroom, int classroomCapacity) {
        if (students.size() <= classroomCapacity) {
            this.classroom = classroom;
            System.out.println("Classroom " + classroom + " assigned successfully.");
            return true;
        } else {
            System.out.println("Error: Classroom " + classroom + " is too small for this course.");
            return false;  // Classroom is too small, assignment failed
        }
    }
}
