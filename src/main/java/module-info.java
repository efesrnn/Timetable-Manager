module com.example.macven {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.macven to javafx.fxml;
    exports com.example.macven;
}