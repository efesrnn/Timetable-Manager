package com.example.timetablemanager;

import java.util.List;
import java.util.Objects;

public class Classroom {
    private String classroomName;
    private int capacity;
    private boolean isAvailable;
    private List<Course> conflictingCourses;

    public Classroom(String classroomName, int capacity, boolean isAvailable, List<Course> conflictingCourses) {
        this.classroomName = classroomName;
        this.capacity = capacity;
        this.isAvailable = isAvailable;
        this.conflictingCourses = conflictingCourses;
    }

    public String getClassroomName() {
        return classroomName;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public List<Course> getConflictingCourses() {
        return conflictingCourses;
    }

    @Override
    public String toString() {
        return classroomName + " | " + capacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Classroom classroom = (Classroom) o;
        return Objects.equals(classroomName, classroom.classroomName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classroomName);
    }
}
