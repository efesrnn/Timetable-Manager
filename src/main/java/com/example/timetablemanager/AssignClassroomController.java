package com.example.timetablemanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import java.util.logging.*;

import java.io.IOException;
import java.util.*;

/**
 * Controller class for assigning classrooms to courses.
 */
public class AssignClassroomController {

    @FXML
    private ListView<String> listViewCourses;

    @FXML
    private ListView<String> listViewClassrooms;

    @FXML
    private ListView<String> listViewOutline;

    @FXML
    private Button buttonAssign;

    @FXML
    private Button buttonSave;

    @FXML
    private Button buttonBack;

    @FXML
    private Button buttonDelete;

    private static final Logger LOGGER = Logger.getLogger(AssignClassroomController.class.getName());

    // The list that holds "Course -> Classroom" outline items
    private ObservableList<String> outline = FXCollections.observableArrayList();

    // Used for assigning a color for each courseID
    private final Map<String, Color> courseColors = new HashMap<>();

    public void initialize() {
        // Initialize the Database connection
        Database.connect();

        // Populate the list views
        populateCoursesListView();
        populateClassroomsListView();

        // Outline
        listViewOutline.setItems(outline);

        // Setup custom cell factories (tooltips)
        setCustomCellFactories();

        // Button actions
        buttonAssign.setOnAction(event -> handleAssign());
        buttonSave.setOnAction(event -> handleSave());
        buttonBack.setOnAction(event -> switchScene("mainLayout.fxml"));
        buttonDelete.setOnAction(event -> handleDelete());
    }

    /**
     * Sets custom cell factories for listViewCourses and listViewClassrooms to enable rich tooltips.
     */
    private void setCustomCellFactories() {
        // COURSE cells
        listViewCourses.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new CourseListCell();
            }
        });

        // CLASSROOM cells
        listViewClassrooms.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ClassroomListCell();
            }
        });
    }

    /**
     * Populates the Courses ListView with entries like "SE115 | Enrolled Students: 15".
     */
    private void populateCoursesListView() {
        List<Course> allCourses = Database.getAllCourses();
        ObservableList<String> courseItems = FXCollections.observableArrayList();

        for (Course course : allCourses) {
            int enrolled = (course.getStudents() != null) ? course.getStudents().size() : 0;
            String courseEntry = String.format("%s | Enrolled Students: %d",
                    course.getCourseID(), enrolled);
            courseItems.add(courseEntry);
        }

        listViewCourses.setItems(courseItems);
    }

    /**
     * Populates the Classrooms ListView with entries like "M101 | Capacity: 25".
     */
    private void populateClassroomsListView() {
        List<String> classroomsWithCapacities = Database.getAllClassroomsWithCapacities();
        listViewClassrooms.setItems(FXCollections.observableArrayList(classroomsWithCapacities));
    }

    /**
     * Handles the Assign button action.
     */
    /**
     * Handles the Assign button action.
     */
    private void handleAssign() {
        String selectedCourseItem = listViewCourses.getSelectionModel().getSelectedItem();
        String selectedClassroomItem = listViewClassrooms.getSelectionModel().getSelectedItem();

        if (selectedCourseItem == null || selectedClassroomItem == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select a course and a classroom.");
            return;
        }

        // Example: "SE115 | Enrolled Students: 15"
        String[] courseParts = selectedCourseItem.split(" \\| ");
        String courseName = courseParts[0].trim();

        // Example: "M101 | Capacity: 25"
        String[] classroomParts = selectedClassroomItem.split(" \\| ");
        String classroomName = classroomParts[0].trim();

        // Check if this assignment is already in the outline
        String assignment = String.format("%s -> %s", courseName, classroomName);
        if (outline.contains(assignment)) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "This assignment already exists in the outline.");
            return;
        }

        // Capacity check
        int enrolledStudents = Database.getStudentsEnrolledInCourse(courseName).size();
        if (!Database.hasSufficientCapacity(classroomName, enrolledStudents)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Selected classroom does not meet capacity requirements.");
            return;
        }

        // Availability check
        Course selectedCourse = Database.getAllCourses().stream()
                .filter(c -> c.getCourseID().equals(courseName))
                .findFirst()
                .orElse(null);

        if (selectedCourse == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Selected course not found.");
            return;
        }

        String timeToStart = selectedCourse.getTimeToStart();
        LOGGER.log(Level.INFO, "Assigning course: {0}, timeToStart: {1}", new Object[]{courseName, timeToStart});

        if (timeToStart == null || timeToStart.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Course time is not set.");
            return;
        }

        String[] timeParts = timeToStart.trim().split(" ");
        if (timeParts.length < 2) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid course time format: \"" + timeToStart + "\". Expected format: 'Day HH:MM' (e.g., 'Monday 08:30').");
            return;
        }

        String day = capitalizeFirstLetter(timeParts[0]);
        String time = timeParts[1];

        // Additional validation for day and time
        if (!isValidDay(day)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid day format: \"" + day + "\". Please enter a valid day (e.g., 'Monday').");
            return;
        }

        if (!isValidTimeFormat(time)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid time format: \"" + time + "\". Expected format: 'HH:MM' (e.g., '08:30' or '8:30').");
            return;
        }

        // Normalize time to two digits
        time = normalizeTimeFormat(time);

        if (!Database.isClassroomAvailable(classroomName, day, time, selectedCourse.getDuration())) {
            showAlert(Alert.AlertType.ERROR, "Error", "The selected classroom is not available at the course's time.");
            return;
        }

        // Add to outline & allocate
        outline.add(assignment);
        Database.allocateCourseToClassroom(courseName, classroomName);

        listViewOutline.setItems(outline);

    }
    /**
     * Normalizes the time format to ensure two-digit hours (e.g., "8:30" -> "08:30").
     */
    private String normalizeTimeFormat(String time) {
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return time; // Return as is if format is unexpected
        }

        String hour = parts[0];
        String minute = parts[1];

        if (hour.length() == 1) {
            hour = "0" + hour;
        }

        return hour + ":" + minute;
    }


    /**
     * Capitalizes the first letter of the given string.
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    /**
     * Handles the Save button action.
     */
    private void handleSave() {
        if (outline.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "No assignments to save.");
            return;
        }

        for (String assignment : outline) {
            String[] parts = assignment.split(" -> ");
            if (parts.length == 2) {
                String courseName = parts[0].trim();
                String classroomName = parts[1].trim();
                Database.allocateCourseToClassroom(courseName, classroomName);
            }
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Assignments have been successfully saved!");
        outline.clear();
        listViewOutline.setItems(outline);

        // Refresh the courses list
        populateCoursesListView();
    }

    /**
     * Handles the Delete button action.
     */
    private void handleDelete() {
        String selectedOutlineItem = listViewOutline.getSelectionModel().getSelectedItem();
        if (selectedOutlineItem == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an assignment to delete.");
            return;
        }

        outline.remove(selectedOutlineItem);
        listViewOutline.setItems(outline);

        String[] parts = selectedOutlineItem.split(" -> ");
        if (parts.length == 2) {
            String courseName = parts[0].trim();
            String classroomName = parts[1].trim();
            Database.deallocateCourseFromClassroom(courseName, classroomName);
        }

        showAlert(Alert.AlertType.INFORMATION, "Success", "Assignment has been successfully deleted.");
    }

    /**
     * Switches the current scene to the specified FXML file.
     */
    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/timetablemanager/" + fxmlFile));
            Object newRoot = loader.load();

            if (!(newRoot instanceof javafx.scene.Parent)) {
                throw new IllegalArgumentException("Loaded root is not a valid JavaFX parent node.");
            }

            ttManagerController controller = loader.getController();
            controller.refreshTable();

            Stage stage = (Stage) buttonBack.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot((javafx.scene.Parent) newRoot);

            boolean wasFullScreen = stage.isFullScreen();
            stage.setFullScreen(wasFullScreen);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load the scene: " + fxmlFile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid root type in FXML: " + fxmlFile);
        }
    }

    /**
     * Displays an alert dialog with the specified type, title, and message.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* *********************************************************************
     *  Custom ListCell for COURSE (Detailed Tooltip, using .tooltip in CSS)
     * *********************************************************************/
    private class CourseListCell extends ListCell<String> {
        private final Tooltip tooltip;

        public CourseListCell() {
            tooltip = new Tooltip();
            // Configure tooltip display properties
            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setHideDelay(Duration.ZERO);
            tooltip.setShowDuration(Duration.INDEFINITE);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setTooltip(null);
                setStyle(""); // Reset style
            } else {
                setText(item);

                // Extract the Course ID
                String[] parts = item.split(" \\| ");
                String courseID = parts[0].trim();

                Course course = Database.getAllCourses().stream()
                        .filter(c -> c.getCourseID().equals(courseID))
                        .findFirst()
                        .orElse(null);

                if (course != null) {
                    // Determine if the course is assigned to a classroom
                    String assignedClassroom = course.getClassroom();
                    boolean isAssigned = assignedClassroom != null && !assignedClassroom.trim().isEmpty();

                    // Change text color to blue if assigned
                    if (isAssigned) {
                        setStyle("-fx-text-fill: blue;");
                    } else {
                        setStyle("-fx-text-fill: black;"); // Default color
                    }

                    // Build a GridPane for course details in the tooltip
                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(5);
                    grid.setPadding(new Insets(5));

                    int rowIndex = 0;

                    // Course ID
                    Label lblIdKey = new Label("Course ID:");
                    Label lblIdVal = new Label(course.getCourseID());

                    // Lecturer
                    Label lblLectKey = new Label("Lecturer:");
                    Label lblLectVal = new Label(course.getLecturer());

                    // Time to Start
                    Label lblTimeKey = new Label("Time to Start:");
                    Label lblTimeVal = new Label(course.getTimeToStart());

                    // Duration
                    Label lblDurKey = new Label("Duration:");
                    Label lblDurVal = new Label(String.valueOf(course.getDuration()));

                    // Capacity
                    Label lblCapKey = new Label("Capacity:");
                    Label lblCapVal = new Label(String.valueOf(course.getCapacity()));

                    // Enrolled
                    int enrolled = (course.getStudents() != null) ? course.getStudents().size() : 0;
                    Label lblEnrKey = new Label("Enrolled:");
                    Label lblEnrVal = new Label(String.valueOf(enrolled));

                    // Assigned Classroom
                    Label lblAssignKey = new Label("Assigned Classroom:");
                    Label lblAssignVal = new Label(isAssigned ? assignedClassroom : "N/A");
                    if(isAssigned){
                        lblAssignVal.setStyle("-fx-font-weight: bold;");
                        lblAssignVal.setStyle("-fx-text-fill: blue;");
                    }else{
                        lblAssignVal.setStyle("-fx-font-weight: bold;");
                        lblAssignVal.setStyle("-fx-text-fill: black;");
                    }
                    // Add elements to the grid
                    grid.add(lblIdKey, 0, rowIndex);
                    grid.add(lblIdVal, 1, rowIndex++);
                    grid.add(lblLectKey, 0, rowIndex);
                    grid.add(lblLectVal, 1, rowIndex++);
                    grid.add(lblTimeKey, 0, rowIndex);
                    grid.add(lblTimeVal, 1, rowIndex++);
                    grid.add(lblDurKey, 0, rowIndex);
                    grid.add(lblDurVal, 1, rowIndex++);
                    grid.add(lblCapKey, 0, rowIndex);
                    grid.add(lblCapVal, 1, rowIndex++);
                    grid.add(lblEnrKey, 0, rowIndex);
                    grid.add(lblEnrVal, 1, rowIndex++);
                    grid.add(lblAssignKey, 0, rowIndex);
                    grid.add(lblAssignVal, 1, rowIndex++);

                    tooltip.setGraphic(grid);
                    setTooltip(tooltip);

                } else {
                    setTooltip(null);
                    setStyle("-fx-text-fill: black;"); // Default color
                }
            }
        }
    }
    /* *********************************************************************
     *  Custom ListCell for CLASSROOM (Mini-Schedule Grid, using .tooltip in CSS)
     * *********************************************************************/
    private class ClassroomListCell extends ListCell<String> {
        private final Tooltip tooltip;

        public ClassroomListCell() {
            tooltip = new Tooltip();
            // No inline style, let the .tooltip CSS rule do the job
            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setHideDelay(Duration.ZERO);
            tooltip.setShowDuration(Duration.INDEFINITE);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setTooltip(null);
            } else {
                setText(item);

                String[] parts = item.split(" \\| ");
                String classroomName = parts[0].trim();

                GridPane miniSchedule = createClassroomScheduleGridPane(classroomName);
                if (miniSchedule != null) {
                    tooltip.setGraphic(miniSchedule);
                    setTooltip(tooltip);
                } else {
                    setTooltip(null);
                }
            }
        }
    }

    /**
     * Creates a "mini" schedule GridPane (Days vs Times) for the given classroom,
     * coloring each course differently and handling multi-slot durations.
     */
    private GridPane createClassroomScheduleGridPane(String classroomName) {
        // Day/time lists
        List<String> days = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        List<String> times = List.of(
                "08:30", "09:25", "10:20", "11:15", "12:10",
                "13:05", "14:00", "14:55", "15:50", "16:45",
                "17:40", "18:35", "19:30", "20:25", "21:20", "22:15"
        );

        List<Course> scheduledCourses = Database.getAllAllocationsForClassroom(classroomName);
        if (scheduledCourses == null) {
            scheduledCourses = new ArrayList<>();
        }

        // If you'd like a "No courses assigned" placeholder, handle here:
        if (scheduledCourses.isEmpty()) {
            GridPane emptyGrid = new GridPane();
            emptyGrid.setPadding(new Insets(10));
            Label placeholder = new Label("There is no course assigned for this classroom yet");
            placeholder.setStyle("-fx-text-fill: #333; -fx-font-size: 14px;");
            emptyGrid.add(placeholder, 0, 0);
            return emptyGrid;
        }

        // Build the schedule grid
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(5));

        // Top-left corner
        Label timeHeader = new Label("Time");
        timeHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #d3d3d3; -fx-alignment: CENTER;");
        grid.add(timeHeader, 0, 0);

        // Day headers
        for (int i = 0; i < days.size(); i++) {
            Label dayLabel = new Label(days.get(i));
            dayLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #d3d3d3; -fx-alignment: CENTER;");
            grid.add(dayLabel, i + 1, 0);
        }

        // Time labels
        for (int i = 0; i < times.size(); i++) {
            Label timeLabel = new Label(times.get(i));
            timeLabel.setStyle("-fx-font-weight: bold; -fx-background-color: #d3d3d3; -fx-alignment: CENTER_LEFT;");
            grid.add(timeLabel, 0, i + 1);
        }

        // Fill cells with empty placeholders
        for (int col = 1; col <= days.size(); col++) {
            for (int row = 1; row <= times.size(); row++) {
                Label emptyLabel = new Label();
                emptyLabel.setStyle(
                        "-fx-background-color: #ffffff;" +
                                "-fx-border-color: #e0e0e0;" +
                                "-fx-border-width: 0.5px;"
                );
                grid.add(emptyLabel, col, row);
            }
        }

        // Place each scheduled course
        for (Course c : scheduledCourses) {
            String timeToStart = c.getTimeToStart();
            if (timeToStart == null || !timeToStart.contains(" ")) {
                continue; // Skip invalid entries
            }

            String[] tParts = timeToStart.split(" ");
            if (tParts.length < 2) continue;

            String day = capitalizeFirstLetter(tParts[0]);
            String startTime = tParts[1];

            int dayIndex = days.indexOf(day);
            if (dayIndex < 0) continue;

            int rowIndex = times.indexOf(startTime);
            if (rowIndex < 0) continue;

            int duration = c.getDuration();
            for (int offset = 0; offset < duration; offset++) {
                int currentRow = rowIndex + offset;
                if (currentRow >= times.size()) break;

                String colorHex = toRgbString(getColorForCourse(c.getCourseID()));

                Label courseLabel = new Label(c.getCourseID());
                courseLabel.setStyle(
                        "-fx-background-color: " + colorHex + ";" +
                                "-fx-text-fill: #ffffff;" +
                                "-fx-alignment: CENTER;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 3px;" +
                                "-fx-border-color: #e0e0e0;" +
                                "-fx-border-width: 0.5px;"
                );

                removeNodeIfExists(grid, dayIndex + 1, currentRow + 1);
                grid.add(courseLabel, dayIndex + 1, currentRow + 1);
            }
        }

        return grid;
    }

    /**
     * Remove any existing node from the specified (col, row) in the grid.
     */
    private void removeNodeIfExists(GridPane grid, int col, int row) {
        List<javafx.scene.Node> toRemove = new ArrayList<>();
        for (javafx.scene.Node n : grid.getChildren()) {
            Integer cIndex = GridPane.getColumnIndex(n);
            Integer rIndex = GridPane.getRowIndex(n);
            if (cIndex != null && rIndex != null && cIndex == col && rIndex == row) {
                toRemove.add(n);
            }
        }
        grid.getChildren().removeAll(toRemove);
    }

    /**
     * Get or generate a color for each courseID, ensuring the same courseID has the same color.
     */
    private Color getColorForCourse(String courseID) {
        if (!courseColors.containsKey(courseID)) {
            int hash = courseID.hashCode();
            // Keep the color from being too bright/dim by using * 0.6
            double r = ((hash >> 16) & 0xFF) / 255.0 * 0.6;
            double g = ((hash >> 8) & 0xFF) / 255.0 * 0.6;
            double b = (hash & 0xFF) / 255.0 * 0.6;
            courseColors.put(courseID, Color.color(Math.abs(r), Math.abs(g), Math.abs(b)));
        }
        return courseColors.get(courseID);
    }

    /**
     * Converts a Color object to #RGB for CSS usage.
     */
    private String toRgbString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Validates if the provided day is a valid day of the week.
     */
    private boolean isValidDay(String day) {
        List<String> validDays = Arrays.asList(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        );
        return validDays.contains(day);
    }

    /**
     * Validates the time format (e.g., "08:30" or "8:30").
     */
    private boolean isValidTimeFormat(String time) {
        return time.matches("^([0-9]{1,2}):([0-5][0-9])$");
    }
}
