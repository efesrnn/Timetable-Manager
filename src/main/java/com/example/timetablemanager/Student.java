package com.example.timetablemanager;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private String studentId;
    private String name;
    private String surname;
    private List<Course> enrolledCourses;

    public Student(String studentId, String name, String surname, List<Course> enrolledCourses) {
        this.studentId = studentId;
        this.name = name;
        this.surname = surname;
        this.enrolledCourses = enrolledCourses;
    }

    public boolean enrollInCourse(Course course) {
        if (!enrolledCourses.contains(course)) {
            enrolledCourses.add(course);
            return true; // Successfully enrolled
        }
        return false; // Already enrolled
    }

    public List<String> getSchedule() {
        List<String> schedule = new ArrayList<>();
        for (Course course : enrolledCourses) {
            schedule.add(course.getSchedule()); // Assuming Course has a getDetails method
        }
        return schedule;
    }



    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public List<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(List<Course> enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }
}
