package com.example.timetablemanager;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private String studentId;
    private String fullName;
    private String email;
    private List<Course> enrolledCourses;

    public Student(String studentId, String fullName, String email, List<Course> enrolledCourses) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
        this.enrolledCourses = enrolledCourses;
    }

    public boolean enrollInCourse(Course course) {
        if (!enrolledCourses.contains(course)) {
            enrolledCourses.add(course);
            System.out.println("Successfully enrolled returning true");
            return true;
        }
        System.out.println("Already enrolled returning false");
        return false;
    }

    public List<String> getSchedule() {
        List<String> schedule = new ArrayList<>();
        for (Course course : enrolledCourses) {
            schedule.add(course.getSchedule());
        }
        return schedule;
    }
    @Override
    public String toString() {
        return fullName + " (" + studentId + ")";
    }

    //Geter and setters
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Course> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(List<Course> enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }
}
