package com.example.timetablemanager;

import java.util.ArrayList;
import java.util.List;

public class TimetableManager {
    // Attributes
    private List<Student> students;
    private List<Course> courses;

    // Constructor
    public TimetableManager() {
        this.students = new ArrayList<>();
        this.courses = new ArrayList<>();
    }

    // Methods
    public void addStudent(Student student) {
        if (!students.contains(student)) {
            students.add(student);
        }
    }

    public Student findStudentById(String studentId) {
        for (Student student : students) {
            if (student.getStudentId().equals(studentId)) {
                return student;
            }
        }
        return null;
    }

    public void addCourse(Course course) {
        if (!courses.contains(course)) {
            courses.add(course);
        }
    }

    public Course findCourseById(String courseId) {
        for (Course course : courses) {
            if (course.getCourseId().equals(courseId)) {
                return course;
            }
        }
        return null;
    }

    public boolean generateSchedule(Course course, String day, String timeSlot, String classroom) {
        if (checkConflict(day + " " + timeSlot)) {
            course.assignClassroom(classroom);
            return true;
        }
        return false;
    }

    public boolean checkConflict(String schedule) {
        // Logic to check conflicts can be added here
        return true; // For simplicity, no conflicts assumed
    }

    public void saveTimetable(String filePath) {
        // Implementation for saving timetable to a file
    }

    public void loadTimetable(String filePath) {
        // Implementation for loading timetable from a file
    }

    public List<String> getStudentSchedule(String studentId) {
        Student student = findStudentById(studentId);
        return student != null ? student.getSchedule() : new ArrayList<>();
    }

    public List<String> getCourseSchedule(String courseId) {
        Course course = findCourseById(courseId);
        List<String> schedule = new ArrayList<>();
        if (course != null) {
            schedule.add(course.getSchedule());
        }
        return schedule;
    }
}
