package com.example.timetablemanager;

public class Classroom {
    private String classroom;
    private int capacity;

    //Classroom Constructor
    public Classroom(String classroom, int capacity){
        this.capacity=capacity;
        this.classroom=classroom;
    }

    //Setters & Getters
    public String getClassroom() {
        return classroom;
    }

    public void setClassroom(String classroom) {
        this.classroom = classroom;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}