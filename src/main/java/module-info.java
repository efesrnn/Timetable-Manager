module com.example.timetablemanager {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.timetablemanager to javafx.fxml;
    exports com.example.timetablemanager;
}