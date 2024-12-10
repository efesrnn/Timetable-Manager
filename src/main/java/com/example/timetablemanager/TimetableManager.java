package com.example.timetablemanager;

import java.util.ArrayList;
import java.util.List;

public class TimetableManager {

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
            if (course.getCourseId().equals(courseId)) {
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
}
