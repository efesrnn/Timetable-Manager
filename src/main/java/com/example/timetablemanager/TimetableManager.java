package com.example.timetablemanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class TimetableManager extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(TimetableManager.class.getResource("mainLayout.fxml"));
        //Creating the initial scene with 1080pixel width and 720pixel length.
        Scene scene = new Scene(fxmlLoader.load(),1080,720);

        //ICON OF THE APP
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icon.png")));
        }catch (RuntimeException e){
            System.err.println("Couldn't load icon");
            e.printStackTrace();
        }

        stage.setTitle("Welcome to Timetable Manager");
        stage.setScene(scene);
        stage.show();

        System.out.println("Timetable Manager initialized.");
    }



    public static void main(String[] args) {
        launch();
    }
}
