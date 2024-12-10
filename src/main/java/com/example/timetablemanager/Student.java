package com.example.timetablemanager;

public class Student {
    private String id;
    private String name;
    private String email;

    public Student(String id, String name) {
        this.id = id;
        this.name = name;
        //this.lastName = lastName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

     */

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
