package com.example.timetablemanager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ttManagerController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}