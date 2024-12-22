package com.example.timetablemanager;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Controller class for adding a new course.
 */
public class addCourseController { // Renamed to follow Java conventions

    @FXML
    private TextField txtCourseID, txtLecturer;

    @FXML
    private Spinner<Integer>  spinnerDuration;

    @FXML
    private Button btnSelectStudents, btnCreateCourse, btnBack;

    @FXML
    private ListView<String> studentListView;

    @FXML
    private ComboBox<Classroom> comboClassroom;

    @FXML
    private ComboBox<String> comboDay;   // ComboBox for Day

    @FXML
    private ComboBox<String> comboTime;  // ComboBox for Time

    @FXML
    private GridPane scheduleGridPane;    // GridPane for Schedule

    private ObservableList<Student> selectedStudents = FXCollections.observableArrayList();

    // Define possible days and times
    private final List<String> days = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    private final List<String> times = List.of(
            "08:30", "09:25", "10:20", "11:15", "12:10",
            "13:05", "14:00", "14:55", "15:50", "16:45",
            "17:40", "18:35", "19:30", "20:25", "21:20", "22:15"
    );

    // Temporary list to store courses not yet saved to the database
    private final ObservableList<Course> tempCourses = FXCollections.observableArrayList();

    // Track the newly added course for blinking
    private String newlyAddedCourseKey = null;

    // Map to hold courseID to Color mapping
    private final Map<String, Color> courseColors = new HashMap<>();

    // To track previous selection for reverting
    private Classroom previousSelectedClassroom = null;

    // Flag to prevent recursive listener calls
    private boolean isUpdatingSelection = false;

    @FXML
    public void initialize() {
        // Ensure the database is connected
        Database.connect();

        // Initialize Spinners
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 2);
        spinnerDuration.setValueFactory(valueFactory);

        // **Allow only integer typed input** in the Spinner’s text field
        spinnerDuration.setEditable(true);
        TextFormatter<Integer> integerFormatter = new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            // The new text the user is typing or pasting
            String newText = change.getControlNewText();
            // Accept the change only if it's all digits (or empty)
            if (newText.matches("\\d*")) {
                return change;
            }
            // Otherwise, ignore the change -> disallow
            return null;
        });
        spinnerDuration.getEditor().setTextFormatter(integerFormatter);

        // Populate Day ComboBox
        comboDay.setItems(FXCollections.observableArrayList(days));

        // Populate Time ComboBox
        comboTime.setItems(FXCollections.observableArrayList(times));

        // Initialize Classroom ComboBox with custom cell factory
        comboClassroom.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Classroom> call(ListView<Classroom> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Classroom classroom, boolean empty) {
                        super.updateItem(classroom, empty);
                        if (empty || classroom == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(classroom.toString());
                            if (!classroom.isAvailable()) {
                                setTextFill(Color.RED); // Highlight unavailable classrooms in red
                            } else {
                                setTextFill(Color.BLACK); // Available classrooms in black
                            }
                        }
                    }
                };
            }
        });

        // Also set button cell to apply the same styling
        comboClassroom.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Classroom classroom, boolean empty) {
                super.updateItem(classroom, empty);
                if (empty || classroom == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(classroom.toString());
                    if (!classroom.isAvailable()) {
                        setTextFill(Color.RED); // Highlight unavailable classrooms in red
                    } else {
                        setTextFill(Color.BLACK); // Available classrooms in black
                    }
                }
            }
        });

        // Handle selection changes
        comboClassroom.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSelection) {
                return; // Prevent recursion
            }
            if (newVal != null && !newVal.isAvailable()) {
                // Show alert for unavailable classroom
                showAlert("Unavailable Classroom", "The selected classroom is not available at the chosen day and time.");

                // Revert selection using Platform.runLater to ensure it happens after the current event
                Platform.runLater(() -> {
                    isUpdatingSelection = true;
                    if (previousSelectedClassroom != null && comboClassroom.getItems().contains(previousSelectedClassroom)) {
                        comboClassroom.getSelectionModel().select(previousSelectedClassroom);
                    } else {
                        comboClassroom.getSelectionModel().clearSelection();
                    }
                    isUpdatingSelection = false;
                });
            } else {
                // Update previous selection if available
                if (newVal != null && newVal.isAvailable()) {
                    previousSelectedClassroom = newVal;
                }
            }
        });

        // Set default prompt text
        comboClassroom.setPromptText("Select Classroom");

        // Initialize Schedule GridPane
        initializeScheduleGridPane();

        // Add listeners to input fields to update temporary course in real-time
        addInputListeners();

        // Button actions
        btnSelectStudents.setOnAction(event -> openStudentSelectionPopup());
        btnCreateCourse.setOnAction(event -> createCourse());
        btnBack.setOnAction(event -> switchScene("mainLayout.fxml"));

        // Populate schedule with existing allocations from the database
        populateScheduleGridPane();
    }

    /**
     * Adds listeners to input fields to handle real-time course allocation.
     */
    private void addInputListeners() {
        // Listen to changes in all relevant input fields
        txtCourseID.textProperty().addListener((obs, oldVal, newVal) -> updateTempCourse());
        txtLecturer.textProperty().addListener((obs, oldVal, newVal) -> updateTempCourse());
        spinnerDuration.valueProperty().addListener((obs, oldVal, newVal) -> updateTempCourse());
        comboDay.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateClassroomOptions();
            // Populate schedule when day changes
            populateScheduleGridPane();
            updateTempCourse();
        });
        comboTime.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateClassroomOptions();
            // Populate schedule when time changes
            populateScheduleGridPane();
            updateTempCourse();
        });
        comboClassroom.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Populate schedule when classroom changes
            populateScheduleGridPane();
            updateTempCourse();
        });
    }

    /**
     * Updates the temporary course based on current input fields.
     * Adds or removes the temporary course from the tempCourses list.
     */
    private void updateTempCourse() {
        String courseID = txtCourseID.getText().trim();
        String lecturer = txtLecturer.getText().trim();
        Integer capacity = 100;
        Integer duration = spinnerDuration.getValue();
        String day = comboDay.getValue();
        String time = comboTime.getValue();
        Classroom classroom = comboClassroom.getValue();

        // Check if all required fields are filled
        if (!courseID.isEmpty() && !lecturer.isEmpty() && capacity != null && duration != null && day != null && time != null && classroom != null) {
            // All fields are filled; create a temporary course
            Course tempCourse = new Course(
                    courseID,
                    capacity,
                    selectedStudents.stream().distinct().toList(),
                    classroom.getClassroomName(),
                    day + " " + time,
                    duration,
                    lecturer
            );

            // Remove existing temp course with the same courseID before adding the updated one
            tempCourses.removeIf(course -> course.getCourseID().equals(tempCourse.getCourseID()));
            tempCourses.add(tempCourse);
            populateScheduleGridPane(); // Re-populate to reflect changes
        } else {
            // If any field is missing, remove all temporary courses
            if (!tempCourses.isEmpty()) {
                tempCourses.clear();
                populateScheduleGridPane(); // Re-populate to remove temporary allocations
            }
        }
    }

    /**
     * Updates the available classrooms based on selected day, time, and duration.
     */
    private void updateClassroomOptions() {
        String selectedDay = comboDay.getValue();
        String selectedTime = comboTime.getValue();
        int duration = spinnerDuration.getValue();

        if (selectedDay != null && selectedTime != null) {
            List<Classroom> allClassrooms = new ArrayList<>();
            List<String> classroomsWithCapacities = Database.getAllClassroomsWithCapacities();

            for (String classroomEntry : classroomsWithCapacities) {
                String[] parts = classroomEntry.split(" \\| ");
                String classroomName = parts[0];
                int capacity = Integer.parseInt(parts[1]);

                boolean available = Database.isClassroomAvailable(classroomName, selectedDay, selectedTime, duration);
                List<Course> conflictingCourses = new ArrayList<>();

                if (!available) {
                    conflictingCourses = Database.getConflictingCourses(classroomName, selectedDay, selectedTime, duration);
                }

                Classroom classroom = new Classroom(classroomName, capacity, available, conflictingCourses);
                allClassrooms.add(classroom); // Add all classrooms, regardless of availability
            }

            // Update ComboBox with all classrooms
            comboClassroom.setItems(FXCollections.observableArrayList(allClassrooms));

            // Optionally, show a message if no classrooms are available
            boolean anyAvailable = allClassrooms.stream().anyMatch(Classroom::isAvailable);
            if (!anyAvailable) {
                showAlert("Information", "No classrooms are available for the selected day and time.");
            }

            // Clear previous classroom selection
            comboClassroom.getSelectionModel().clearSelection();
            previousSelectedClassroom = null;
        } else {
            // If day or time is not selected, show all classrooms as available
            List<Classroom> allClassrooms = new ArrayList<>();
            List<String> classroomsWithCapacities = Database.getAllClassroomsWithCapacities();

            for (String classroomEntry : classroomsWithCapacities) {
                String[] parts = classroomEntry.split(" \\| ");
                String classroomName = parts[0];
                int capacity = Integer.parseInt(parts[1]);

                // Assume available if day or time is not selected
                Classroom classroom = new Classroom(classroomName, capacity, true, new ArrayList<>());
                allClassrooms.add(classroom);
            }

            comboClassroom.setItems(FXCollections.observableArrayList(allClassrooms));
        }
    }

    /**
     * Initializes the Schedule GridPane with days as columns and times as rows.
     */
    private void initializeScheduleGridPane() {
        // Clear any existing content
        scheduleGridPane.getChildren().clear();
        scheduleGridPane.getColumnConstraints().clear();
        scheduleGridPane.getRowConstraints().clear();

        // Set padding and gaps
        scheduleGridPane.setPadding(new Insets(10));
        scheduleGridPane.setHgap(1);
        scheduleGridPane.setVgap(1);

        // Define column constraints for days (+1 for time labels)
        ColumnConstraints timeColumn = new ColumnConstraints();
        timeColumn.setPrefWidth(120);
        timeColumn.setHalignment(javafx.geometry.HPos.CENTER);
        scheduleGridPane.getColumnConstraints().add(timeColumn); // Column 0 for Time labels

        for (int i = 0; i < days.size(); i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPrefWidth(120);
            colConst.setHalignment(javafx.geometry.HPos.CENTER);
            scheduleGridPane.getColumnConstraints().add(colConst);
        }

        // Define row constraints for time slots (+1 for day headers)
        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(50);
        headerRow.setValignment(javafx.geometry.VPos.CENTER);
        scheduleGridPane.getRowConstraints().add(headerRow); // Row 0 for Day headers

        for (int i = 0; i < times.size(); i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPrefHeight(50);
            rowConst.setValignment(javafx.geometry.VPos.CENTER);
            scheduleGridPane.getRowConstraints().add(rowConst);
        }

        // Add Day Headers
        scheduleGridPane.add(new Label("Time"), 0, 0); // Top-left corner
        for (int col = 1; col <= days.size(); col++) {
            Label dayLabel = new Label(days.get(col - 1));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #d3d3d3; -fx-alignment: CENTER;");
            scheduleGridPane.add(dayLabel, col, 0);
        }

        // Add Time Labels and empty cells
        for (int row = 1; row <= times.size(); row++) {
            // Add Time Label
            Label timeLabel = new Label(times.get(row - 1));
            timeLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #d3d3d3; -fx-alignment: CENTER_LEFT;");
            scheduleGridPane.add(timeLabel, 0, row);

            // Add Empty Cells
            for (int col = 1; col <= days.size(); col++) {
                Label emptyLabel = new Label();
                emptyLabel.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0.5; -fx-background-color: " + (row % 2 == 0 ? "#f0f0f0;" : "#ffffff;"));
                emptyLabel.setPrefSize(120, 50); // Match column width and row height
                scheduleGridPane.add(emptyLabel, col, row);
            }
        }
    }

    /**
     * Allocates a course in the Schedule GridPane.
     *
     * @param course      The course to allocate.
     * @param isTemporary Indicates if the course is temporary (not yet saved).
     */
    private void allocateCourseInSchedule(Course course, boolean isTemporary) {
        String timeToStart = course.getTimeToStart(); // e.g., "Monday 08:30"
        String[] parts = timeToStart.split(" ");
        if (parts.length < 2) return;

        String day = parts[0];
        String time = parts[1];

        // Find column index for the day
        int col = days.indexOf(day) + 1;
        if (col == 0) return; // Day not found

        // Find row index for the time
        int startRow = times.indexOf(time) + 1;
        if (startRow == 0) return; // Time not found

        int duration = course.getDuration(); // Number of consecutive time slots

        // Assign a color to the course if not already assigned
        Color courseColor = courseColors.computeIfAbsent(course.getCourseID(), k -> generateColorForCourse(k));
        String colorHex = toRgbString(courseColor);

        for (int i = 0; i < duration; i++) {
            int row = startRow + i;
            if (row > times.size()) break; // Avoid exceeding grid

            // Remove any existing node in this cell (empty label or previous course label)
            javafx.scene.Node nodeToRemove = getNodeFromGridPane(col, row);
            if (nodeToRemove != null) {
                scheduleGridPane.getChildren().remove(nodeToRemove);
            }

            // Create the label for this part of the course
            Label allocationLabel = new Label(course.getCourseID());
            allocationLabel.setStyle(
                    "-fx-background-color: " + colorHex + ";" +
                            "-fx-text-fill: #ffffff;" +
                            "-fx-alignment: CENTER;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 5px;" +
                            "-fx-border-color: #e0e0e0;" +
                            "-fx-border-width: 0.5px;"
            );

            if (isTemporary) {
                allocationLabel.setStyle(
                        "-fx-background-color: #FFD700;" + // Gold color for temporary
                                "-fx-text-fill: #000000;" +        // Black text for visibility
                                "-fx-alignment: CENTER;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5px;" +
                                "-fx-border-color: #e0e0e0;" +
                                "-fx-border-width: 0.5px;"
                );
                blinkLabel(allocationLabel);
            }

            // Prepare additional course details for the tooltip
            int capacity = course.getCapacity();
            int enrolledCount = course.getStudents() != null ? course.getStudents().size() : 0;
            String endTime = getEndTime(time, duration);

            Tooltip tooltip = new Tooltip(
                    "Course ID: " + course.getCourseID() + "\n" +
                            "Lecturer: " + course.getLecturer() + "\n" +
                            "Capacity: " + capacity + "\n" +
                            "Enrolled Students: " + enrolledCount + "\n" +
                            "Time: " + course.getTimeToStart() + " - " + endTime
            );

            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setHideDelay(Duration.ZERO);
            tooltip.setShowDuration(Duration.INDEFINITE);
            Tooltip.install(allocationLabel, tooltip);

            // Assign the label to the specific cell in the schedule
            scheduleGridPane.add(allocationLabel, col, row);
        }
    }


    /**
     * Applies a blinking animation to a label.
     *
     * @param label The label to apply the blinking effect.
     */
    private void blinkLabel(Label label) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0), event -> label.setOpacity(1)),
                new KeyFrame(Duration.seconds(0.5), event -> label.setOpacity(0)),
                new KeyFrame(Duration.seconds(1), event -> label.setOpacity(1))
        );
        timeline.setCycleCount(5); // Blink 5 times
        timeline.play();
    }

    /**
     * Populates the Schedule GridPane with both saved and temporary course allocations.
     */
    private void populateScheduleGridPane() {
        // Re-initialize the GridPane to clear existing allocations
        initializeScheduleGridPane();

        // Allocate saved courses from the database
        Classroom selectedClassroom = comboClassroom.getValue();
        if (selectedClassroom == null) {
            return; // No classroom selected
        }

        List<Course> savedAllocations = Database.getAllAllocatedClassrooms(selectedClassroom.getClassroomName());
        for (Course course : savedAllocations) {
            allocateCourseInSchedule(course, false);
        }

        // Allocate temporary courses
        for (Course tempCourse : tempCourses) {
            allocateCourseInSchedule(tempCourse, true);
        }

        // Apply alternating row colors for better readability
        applyAlternatingRowColors();
    }

    /**
     * Retrieves a node from the GridPane based on column and row indices.
     *
     * @param col The column index.
     * @param row The row index.
     * @return The node at the specified position, or null if none exists.
     */
    private javafx.scene.Node getNodeFromGridPane(int col, int row) {
        for (javafx.scene.Node node : scheduleGridPane.getChildren()) {
            Integer nodeCol = GridPane.getColumnIndex(node);
            Integer nodeRow = GridPane.getRowIndex(node);
            if (nodeCol == null) nodeCol = 0;
            if (nodeRow == null) nodeRow = 0;
            if (nodeCol == col && nodeRow == row) {
                return node;
            }
        }
        return null;
    }

    /**
     * Calculates the end time based on the start time and duration.
     *
     * @param startTime The start time in "HH:mm" format.
     * @param duration  The duration in number of consecutive slots.
     * @return The end time in "HH:mm" format.
     */
    private String getEndTime(String startTime, int duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime time;
        try {
            time = LocalTime.parse(startTime, formatter);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid start time format: " + startTime);
            return startTime; // Fallback to start time if parsing fails
        }
        // Each slot is 55 minutes apart (45 minutes class + 10 minutes break)
        int minutesToAdd = duration * 55;
        LocalTime endTime = time.plusMinutes(minutesToAdd);
        return endTime.format(formatter);
    }

    /**
     * Opens the student selection popup.
     */
    private void openStudentSelectionPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("studentSelectionLayout.fxml"));
            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(loader.load()));

            // Set window icon (optional)
            try {
                Stage stage = (Stage) popupStage.getScene().getWindow();
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/student.png")));
            } catch (RuntimeException e) {
                System.err.println("Couldn't load icon");
                e.printStackTrace();
            }
            popupStage.setTitle("Select Students");
            studentSelectionController controller = loader.getController();
            controller.setCourseCapacity(40);
            popupStage.showAndWait();

            // Retrieve selected students without duplicates
            ObservableList<Student> selected = controller.getSelectedStudents();
            if (selected != null) {
                for (Student student : selected) {
                    // Add only if the student is not already in the list
                    if (!this.selectedStudents.contains(student)) {
                        this.selectedStudents.add(student);
                    }
                }
                // Update ListView with the latest unique list
                studentListView.setItems(FXCollections.observableArrayList(
                        this.selectedStudents.stream().map(Student::getFullName).distinct().toList()
                ));
                // Update temporary course allocations if all fields are filled
                updateTempCourse();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the student selection popup.");
        }
    }

    /**
     * Creates a new course and updates the schedule GridPane.
     */
    private void createCourse() {
        String courseID = txtCourseID.getText().trim();
        int capacity = 100;
        int duration = spinnerDuration.getValue();
        String lecturer = txtLecturer.getText().trim();

        Classroom selectedClassroom = comboClassroom.getValue();
        if (selectedClassroom == null) {
            showAlert("Error", "No classroom selected!");
            return;
        }

        // Extract only the classroom name
        String classroom = selectedClassroom.getClassroomName();

        String selectedDay = comboDay.getValue();
        String selectedTime = comboTime.getValue();

        if (selectedDay == null || selectedTime == null) {
            showAlert("Error", "Please select both day and time for the course.");
            return;
        }

        // Combine day and time
        String timeToStart = selectedDay + " " + selectedTime;

        // Basic empty-field checks
        if (courseID.isEmpty() || lecturer.isEmpty() || classroom.isEmpty() || timeToStart.isEmpty()) {
            showAlert("Error", "Please fill in all fields, select day/time, assign students/classroom, and set duration!");
            return;
        }

        // 1) **Check if course ID already exists** in the database:
        List<String> existingCourseIDs = Database.getAllCourseNames(); // returns a list of existing IDs
        if (existingCourseIDs.contains(courseID)) {
            showAlert("Error", "Course ID '" + courseID + "' already exists. Please choose a different ID.");
            // Remain on this page so user can change it
            return;
        }

        // === Debugging statements ===
        System.out.println("===== Creating Course =====");
        System.out.println("Course ID: " + courseID);
        System.out.println("Lecturer: " + lecturer);
        System.out.println("Duration: " + duration);
        System.out.println("Selected Classroom: " + classroom);
        System.out.println("Selected Day: " + selectedDay);
        System.out.println("Selected Time: " + selectedTime);
        System.out.println("Time to Start: " + timeToStart);
        System.out.println("Number of Selected Students: " + selectedStudents.size());

        // Validate classroom capacity
        if (!Database.hasSufficientCapacity(classroom, selectedStudents.size())) {
            showAlert("Error", "Selected classroom does not have sufficient capacity for the number of students.");
            System.out.println("Capacity Check Failed: Classroom capacity insufficient.");
            return;
        } else {
            System.out.println("Capacity Check Passed: Classroom can accommodate the students.");
        }

        // Additional Availability Check
        if (!selectedClassroom.isAvailable()) {
            showAlert("Error", "Selected classroom is not available at the chosen day and time.");
            System.out.println("Availability Check Failed: Classroom is not available.");
            return;
        } else {
            System.out.println("Availability Check Passed: Classroom is available.");
        }

        // Prepare course data
        Course newCourse = new Course(
                courseID,
                capacity,
                new ArrayList<>(selectedStudents),
                classroom,
                timeToStart,
                duration,
                lecturer
        );

        // Save the course to the database
        try {
            Database.addCourseWithAllocation(courseID, lecturer, duration, timeToStart, classroom);
            System.out.println("Course added to the database successfully.");
        } catch (SQLException e) {
            showAlert("Error", "Failed to create course and allocate classroom: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Enroll selected students
        for (Student student : selectedStudents) {
            if (!Database.isEnrollmentExists(courseID, student.getFullName())) {
                Database.addEnrollment(courseID, student.getFullName());
                System.out.println("Student enrolled: " + student.getFullName());
            }
        }

        showAlert("Success", "Course created successfully: " + courseID);
        System.out.println("Course creation process completed successfully.");

        // Track the newly added course for blinking (if desired)
        newlyAddedCourseKey = selectedDay + "_" + selectedTime;

        // Remove the temporary course if present
        tempCourses.remove(newCourse);
        populateScheduleGridPane();

        // Optionally, trigger blinking effect for the newly added course
        blinkNewCourse(newCourse);

        // Clear input fields and selections
        clearInputFields();

        // Switch back to mainLayout and refresh the table
        switchScene("mainLayout.fxml");
    }



    /**
     * Implements a blinking effect for the newly added course in the schedule.
     *
     * @param course The newly added course.
     */
    private void blinkNewCourse(Course course) {
        String timeToStart = course.getTimeToStart(); // e.g., "Monday 08:30"
        String[] parts = timeToStart.split(" ");
        if (parts.length < 2) return;

        String day = parts[0];
        String time = parts[1];

        // Find the column index for the day
        int col = days.indexOf(day) + 1;
        if (col == 0) return; // Day not found

        // Find the row index for the time
        int startRow = times.indexOf(time) + 1;
        if (startRow == 0) return; // Time not found

        int duration = course.getDuration(); // Number of consecutive time slots

        // Apply blinking to each block
        for (int i = 0; i < duration; i++) {
            int row = startRow + i;
            if (row > times.size()) break; // Avoid exceeding grid

            javafx.scene.Node targetNode = getNodeFromGridPane(col, row);
            if (targetNode instanceof Label) {
                Label label = (Label) targetNode;
                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.seconds(0), event -> label.setOpacity(1)),
                        new KeyFrame(Duration.seconds(0.5), event -> label.setOpacity(0)),
                        new KeyFrame(Duration.seconds(1), event -> label.setOpacity(1))
                );
                timeline.setCycleCount(4); // Blink 4 times
                timeline.play();
            }
        }

        // Reset the key after blinking
        Timeline resetTimeline = new Timeline(new KeyFrame(Duration.seconds(duration), event -> newlyAddedCourseKey = null));
        resetTimeline.play();
    }

    /**
     * Clears all input fields and selections.
     */
    private void clearInputFields() {
        txtCourseID.clear();
        txtLecturer.clear();
        spinnerDuration.getValueFactory().setValue(2);  // Reset to default
        comboDay.getSelectionModel().clearSelection();
        comboTime.getSelectionModel().clearSelection();
        comboClassroom.getSelectionModel().clearSelection();
        selectedStudents.clear();
        studentListView.getItems().clear();
    }

    /**
     * Switches the scene to the specified FXML file.
     *
     * @param fxmlFile The name of the FXML file.
     */
    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            Object newRoot = loader.load();

            if (!(newRoot instanceof javafx.scene.Parent)) {
                throw new IllegalArgumentException("Loaded root is not a valid JavaFX parent node.");
            }
            ttManagerController controller = loader.getController();
            controller.refreshTable(); // Refresh the TableView

            Stage stage = (Stage) btnBack.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot((javafx.scene.Parent) newRoot);

            boolean wasFullScreen = stage.isFullScreen();
            stage.setFullScreen(wasFullScreen);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the scene: " + fxmlFile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showAlert("Error", "Invalid root type in FXML: " + fxmlFile);
        }
    }

    /**
     * Displays an alert dialog with the specified title and message.
     *
     * @param title   The title of the alert.
     * @param message The content message of the alert.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        try {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/timetablemanager/icons/alert.png")));
        } catch (RuntimeException e) {
            System.err.println("Couldn't load alert icon");
            e.printStackTrace();
        }

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Applies alternating row colors to the Schedule GridPane for better readability.
     */
    private void applyAlternatingRowColors() {
        for (int row = 1; row <= times.size(); row++) {
            for (int col = 1; col <= days.size(); col++) {
                javafx.scene.Node node = getNodeFromGridPane(col, row);
                if (node instanceof Label && ((Label) node).getText().isEmpty()) {
                    // Apply alternating background colors
                    if (row % 2 == 0) {
                        node.setStyle(node.getStyle() + "; -fx-background-color: #f0f0f0;");
                    } else {
                        node.setStyle(node.getStyle() + "; -fx-background-color: #ffffff;");
                    }
                }
            }
        }
    }

    /**
     * Converts a JavaFX Color to its RGB Hex String representation.
     *
     * @param color The Color to convert.
     * @return The RGB Hex String (e.g., "#FFAABB").
     */
    private String toRgbString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Generates a unique color based on the course ID.
     *
     * @param courseID The ID of the course.
     * @return A Color object representing the course's color.
     */
    private Color generateColorForCourse(String courseID) {
        // Simple color generation based on hash code
        Random rand = new Random(courseID.hashCode());
        // Ensure the color is not too light for text visibility
        double r = rand.nextDouble() * 0.6;
        double g = rand.nextDouble() * 0.6;
        double b = rand.nextDouble() * 0.6;
        return Color.color(r, g, b);
    }
}
