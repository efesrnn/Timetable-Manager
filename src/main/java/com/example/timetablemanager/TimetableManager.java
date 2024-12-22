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
        Scene scene = new Scene(fxmlLoader.load(), 1080, 720);

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/icon.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load icon");
            e.printStackTrace();
        }

        stage.setTitle("Welcome to Timetable Manager");
        stage.setScene(scene);
        stage.show();

        welcomeController welcome = fxmlLoader.getController();
        welcome.checkAndLoadCSVFiles();

        System.out.println("Timetable Manager initialized.");
    }

    public static void main(String[] args) {
        launch();
    }

}
