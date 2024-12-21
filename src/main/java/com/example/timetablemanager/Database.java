package com.example.timetablemanager;

import java.io.File;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static java.sql.DriverManager.getConnection;

/**
 * This static class is responsible for creating and managing the timetable database.
 * The database consists of 5 tables:
 * - Courses: Stores information about courses.
 * - Classrooms: Stores information about classrooms.
 * - Allocated: Manages the allocation of courses to classrooms.
 * - Students: Stores student information.
 * - Enrollments: Manages the enrollments of students in courses.
 *
 * The database is created and stored in the user's home directory under the Documents folder.
 */
public class Database {
    private static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String url = "jdbc:sqlite:" + dbPath + File.separator + "TimetableManagement.db";
    private static Connection conn = null;

    // In-memory course list
    private static List<Course> courseList = new ArrayList<>();

    // Create database connection
    public static Connection connect() {
        File dbDir = new File(dbPath);
        if (!dbDir.exists()) {
            dbDir.mkdir();
        }

        try {
            if (conn == null || conn.isClosed()) {
                conn = getConnection(url);
                System.out.println("Connected to database!");
                createTables(); // Create tables when connected
                loadAllCourses(); // Load courses into memory
            }
        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
        return conn;
    }

    public static void close() {
        try {
            if (conn != null) {
                conn.close();
                System.out.println("Database connection interrupted!");
            }
        } catch (SQLException e) {
            System.err.println("An error occurred while closing the connection: " + e.getMessage());
        }
    }

    // CREATE TABLES (Creates five tables: Courses,Classrooms,Allocated,Students,Enrollments)
    // CREATE TABLES (Creates five tables: Courses,Classrooms,Allocated,Students,Enrollments)
    private static void createTables() {
        String createCoursesTable = """
                CREATE TABLE IF NOT EXISTS Courses (
            courseId INTEGER PRIMARY KEY AUTOINCREMENT,
            courseName TEXT NOT NULL UNIQUE,
            lecturer TEXT,
            duration INTEGER,
            timeToStart TEXT
        );
        """;

        String createClassroomsTable = """
                CREATE TABLE IF NOT EXISTS Classrooms (
            classroomId INTEGER PRIMARY KEY AUTOINCREMENT,
            classroomName TEXT NOT NULL UNIQUE,
            capacity INTEGER NOT NULL
        );
        """;

        String createAllocatedTable = """
                CREATE TABLE IF NOT EXISTS Allocated (
            allocationID INTEGER PRIMARY KEY AUTOINCREMENT,
            courseName TEXT NOT NULL,
            classroomName TEXT NOT NULL,
            FOREIGN KEY (courseName) REFERENCES Courses (courseName) ON DELETE CASCADE,
            FOREIGN KEY (classroomName) REFERENCES Classrooms (classroomName) ON DELETE CASCADE
        );
        """;

        String createStudentsTable = """
            CREATE TABLE IF NOT EXISTS Students (
                studentId INTEGER PRIMARY KEY AUTOINCREMENT,
                studentName TEXT NOT NULL
            );
        """;

        String createEnrollmentsTable = """
            CREATE TABLE IF NOT EXISTS Enrollments (
                enrollmentId INTEGER PRIMARY KEY AUTOINCREMENT,
                courseName TEXT NOT NULL,
                studentName TEXT NOT NULL,
                FOREIGN KEY (courseName) REFERENCES Courses (courseName),
                FOREIGN KEY (studentName) REFERENCES Students (studentName)
            );
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createCoursesTable);
            stmt.execute(createClassroomsTable);
            stmt.execute(createAllocatedTable);
            stmt.execute(createStudentsTable);
            stmt.execute(createEnrollmentsTable);
            System.out.println("Tables created!");
        } catch (SQLException e) {
            System.err.println("Error while creating tables: " + e.getMessage());
        }
    }

    // Load all courses into the in-memory list (Includes courses and enrolled students)
    public static void loadAllCourses() {
        courseList.clear();
        String sql = "SELECT DISTINCT courseName, timeToStart, duration, lecturer FROM Courses";
        String sql2 = "SELECT * FROM Allocated WHERE courseName = ? ";
        String sql3 = "SELECT * FROM Classrooms WHERE classroomName = ? ";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean coursesFound = false;

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                int duration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                // Fetch enrolled students
                List<String> enrolledStudentNames = getStudentsEnrolledInCourse(courseName);
                List<Student> enrolledStudents = new ArrayList<>();
                for (String studentName : enrolledStudentNames) {
                    enrolledStudents.add(new Student(studentName, new ArrayList<>()));
                }


                int capacity = 0; // default capacity
                String classroom = ""; // no classroom info here
                String startTime = rs.getString("timeToStart");

                //The part where we separate day and start time
                List<String> days = new ArrayList<>();
                List<String> times = new ArrayList<>();

                if (startTime != null && !startTime.isEmpty()) {
                    // Split the string into day and time
                    String[] parts = startTime.split(" ");
                    if (parts.length >= 2) {
                        days.add(parts[0]);   // Day (e.g., "Monday")
                        times.add(parts[1]);  // Time (e.g., "10:00")
                    } else {
                        // Handle cases where the format is unexpected
                        System.err.println("Unexpected timeToStart format: " + startTime);
                    }
                }

                PreparedStatement stmt2 = conn.prepareStatement(sql2);
                stmt2.setString(1, courseName);
                ResultSet rs2 = stmt2.executeQuery();
                String classroomName = "";

                while (rs2.next()) {
                    classroomName = rs2.getString("classroomName");
                }

                PreparedStatement stmt3 = conn.prepareStatement(sql3);
                stmt3.setString(1, classroomName);
                ResultSet rs3 = stmt3.executeQuery();
                while (rs3.next()) {
                    capacity = rs3.getInt("capacity");
                }

                Course course = new Course(
                        courseName,
                        capacity,
                        enrolledStudents,
                        classroomName,
                        startTime,
                        duration,
                        lecturer
                );

                courseList.add(course);
                coursesFound = true;
            }

            if (coursesFound) {
                System.out.println("Courses loaded into memory.");
            } else {
                System.out.println("No courses found in the database.");
            }

        } catch (SQLException e) {
            System.err.println("Error while loading courses: " + e.getMessage());
        }
    }

    public static List<Course> getAllCourses() {

        return new ArrayList<>(courseList);
    }

    public static void reloadCourses() {
        loadAllCourses();
    }

    public static List<String> getStudentsEnrolledInCourse(String courseName) {
        List<String> students = new ArrayList<>();
        String sql = "SELECT DISTINCT studentName FROM Enrollments WHERE courseName = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                students.add(rs.getString("studentName"));
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching students for course '" + courseName + "': " + e.getMessage());
        }
        return students;
    }

    public static void addCourse(String courseName, String lecturer, int duration, String timeToStart) {
        String sql = "INSERT INTO Courses (courseName, lecturer, duration, timeToStart) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, lecturer);
            pstmt.setInt(3, duration);
            pstmt.setString(4, timeToStart);
            pstmt.executeUpdate();
            System.out.println("Course added successfully!");
        } catch (SQLException e) {
            System.err.println("Error while adding course: " + e.getMessage());
        }
    }

    public static void addCourseWithAllocation(String courseName, String lecturer, int duration, String timeToStart, String classroomName) throws SQLException {
        // Split timeToStart into day and time
        String[] parts = timeToStart.split(" ");
        if (parts.length < 2) {
            throw new SQLException("Invalid timeToStart format: " + timeToStart);
        }
        String day = parts[0];
        String time = parts[1];

        // Check if the classroom is available
        if (!isClassroomAvailable(classroomName, day, time, duration)) {
            throw new SQLException("Classroom " + classroomName + " is not available on " + day + " at " + time + " for duration " + duration + " hours.");
        }

        String insertCourseSQL = "INSERT INTO Courses (courseName, lecturer, duration, timeToStart) VALUES (?, ?, ?, ?)";
        String allocateClassroomSQL = "INSERT INTO Allocated (courseName, classroomName) VALUES (?, ?)";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt1 = conn.prepareStatement(insertCourseSQL)) {
                pstmt1.setString(1, courseName);
                pstmt1.setString(2, lecturer);
                pstmt1.setInt(3, duration);
                pstmt1.setString(4, timeToStart);
                pstmt1.executeUpdate();
            }

            try (PreparedStatement pstmt2 = conn.prepareStatement(allocateClassroomSQL)) {
                pstmt2.setString(1, courseName);
                pstmt2.setString(2, classroomName);
                pstmt2.executeUpdate();
            }

            conn.commit();
            System.out.println("Course added and classroom allocated successfully.");
        } catch (SQLException e) {
            conn.rollback();
            System.err.println("Transaction failed: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }


    public static void addEnrollment(String courseName, String studentName) {
        String sql = "INSERT INTO Enrollments (courseName, studentName) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, studentName);
            pstmt.executeUpdate();
            System.out.println("Enrollment added successfully: " + studentName + " -> " + courseName);
        } catch (SQLException e) {
            System.err.println("Error while adding enrollment: " + e.getMessage());
        }
    }

    public static boolean isEnrollmentExists(String courseName, String studentName) {
        String query = "SELECT 1 FROM Enrollments WHERE courseName = ? AND studentName = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, courseName);
            stmt.setString(2, studentName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Returns true if an entry exists
            }
        } catch (SQLException e) {
            System.err.println("Error checking enrollment: " + e.getMessage());
            return false;
        }
    }


    public static void addStudent(String studentName) {
        String sql = "INSERT INTO Students (studentName) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.executeUpdate();
            System.out.println("Student added successfully: " + studentName);
        } catch (SQLException e) {
            System.err.println("Error while adding student: " + e.getMessage());
        }
    }

    public static void addClassroom(String classroomName, int capacity) {
        String sql = "INSERT INTO Classrooms (classroomName, capacity) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            pstmt.setInt(2, capacity);
            pstmt.executeUpdate();
            System.out.println("Classroom added successfully: " + classroomName);
        } catch (SQLException e) {
            System.err.println("Error while adding classroom: " + e.getMessage());
        }
    }
    //*****
    // Method to check if the classroom has enough capacity
    public static boolean hasSufficientCapacity(String classroomName, int numberOfStudents) {
        String sql = "SELECT capacity FROM Classrooms WHERE classroomName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int classroomCapacity = rs.getInt("capacity");
                return classroomCapacity >= numberOfStudents;
            }
        } catch (SQLException e) {
            System.err.println("Error while checking classroom capacity: " + e.getMessage());
        }
        return false;
    }

    public static void allocateCourseToClassroom(String courseName, String classroomName) {
        // Check if already allocated
        String checkAllocation = "SELECT COUNT(*) FROM Allocated WHERE courseName = ? AND classroomName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkAllocation)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, classroomName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("This course is already allocated to the selected classroom.");
                return; // Already allocated
            }
        } catch (SQLException e) {
            System.err.println("Error while checking existing allocation: " + e.getMessage());
        }

        List<String> enrolledStudents = getStudentsEnrolledInCourse(courseName);
        int numberOfStudents = enrolledStudents.size();

        // Check if the classroom has enough capacity
        if (!hasSufficientCapacity(classroomName, numberOfStudents)) {
            System.out.println("Error: The classroom does not have enough capacity for this course.");
            return;
        }


        String sql = "INSERT INTO Allocated (courseName, classroomName) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, classroomName);
            pstmt.executeUpdate();
            System.out.println("Course allocated to classroom successfully: " + courseName + " -> " + classroomName);
        } catch (SQLException e) {
            System.err.println("Error while allocating course to classroom: " + e.getMessage());
        }
    }

    public static List<Course> getAllAllocatedClassrooms(String classroomName) {
        List<Course> allocations = new ArrayList<>();
        String sql = """
                SELECT c.courseName, c.timeToStart, c.duration, c.lecturer
                FROM Allocated a
                JOIN Courses c ON a.courseName = c.courseName
                WHERE a.classroomName = ?
                """;

        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String timeToStart = rs.getString("timeToStart");
                int duration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                // Fetch enrolled students
                List<String> enrolledStudentNames = getStudentsEnrolledInCourse(courseName);
                List<Student> enrolledStudents = new ArrayList<>();
                for (String studentName : enrolledStudentNames) {
                    enrolledStudents.add(new Student(studentName, new ArrayList<>()));
                }

                Course course = new Course(
                        courseName,
                        0, // capacity not needed here
                        enrolledStudents,
                        classroomName,
                        timeToStart,
                        duration,
                        lecturer
                );

                allocations.add(course);
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error fetching allocations for classroom " + classroomName + ": " + e.getMessage());
        }

        return allocations;
    }

    public static void deallocateCourseFromClassroom(String courseName, String classroomName) {
        String sql = "DELETE FROM Allocated WHERE courseName = ? AND classroomName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, classroomName);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Course deallocated from classroom successfully: " + courseName + " -> " + classroomName);
            } else {
                System.out.println("No allocation found for course " + courseName + " in classroom " + classroomName);
            }
        } catch (SQLException e) {
            System.err.println("Error while deallocating course from classroom: " + e.getMessage());
        }
    }

    public static List<Course> getAllAllocationsForClassroom(String classroomName) {
        List<Course> allocations = new ArrayList<>();
        String sql = """
                SELECT c.courseName, c.timeToStart, c.duration, c.lecturer
                FROM Allocated a
                JOIN Courses c ON a.courseName = c.courseName
                WHERE a.classroomName = ?
                """;

        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String timeToStart = rs.getString("timeToStart");
                int duration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                // Fetch enrolled students
                List<String> enrolledStudentNames = getStudentsEnrolledInCourse(courseName);
                List<Student> enrolledStudents = new ArrayList<>();
                for (String studentName : enrolledStudentNames) {
                    enrolledStudents.add(new Student(studentName, new ArrayList<>()));
                }

                Course course = new Course(
                        courseName,
                        0, // capacity not needed here
                        enrolledStudents,
                        classroomName,
                        timeToStart,
                        duration,
                        lecturer
                );

                allocations.add(course);
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error fetching allocations for classroom " + classroomName + ": " + e.getMessage());
        }

        return allocations;
    }
    public static List<String> getAllClassroomsWithCapacities() {
        List<String> classrooms = new ArrayList<>();
        String sql = "SELECT DISTINCT classroomName, capacity FROM Classrooms";

        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String classroomName = rs.getString("classroomName");
                int capacity = rs.getInt("capacity");
                classrooms.add(classroomName + " | " + capacity);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error fetching classrooms with capacities: " + e.getMessage());
        }
        return classrooms;
    }

    public static boolean isClassroomAvailable(String classroomName, String day, String startTime, int duration) {
        // Define time formatter
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime desiredStart;
        try {
            desiredStart = LocalTime.parse(startTime, timeFormatter);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid time format: " + startTime);
            return false;
        }

        // Calculate desired time slots based on duration
        List<timeRange> desiredSlots = new ArrayList<>();
        for (int i = 0; i < duration; i++) {
            LocalTime slotStart = desiredStart.plusMinutes(i * (45 + 10)); // Lecture + Break
            LocalTime slotEnd = slotStart.plusMinutes(45); // Lecture duration
            desiredSlots.add(new timeRange(slotStart, slotEnd));
        }

        // SQL query to fetch existing allocations for the classroom on the selected day
        String sql = """
            SELECT c.courseName, c.timeToStart, c.duration, c.lecturer
            FROM Allocated a
            JOIN Courses c ON a.courseName = c.courseName
            WHERE a.classroomName = ? AND c.timeToStart LIKE ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            pstmt.setString(2, day + " %"); // Only courses on the selected day
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String timeToStart = rs.getString("timeToStart");
                int existingDuration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                // Split the timeToStart into day and time
                String[] parts = timeToStart.split(" ");
                if (parts.length < 2) {
                    System.err.println("Invalid timeToStart format: " + timeToStart);
                    continue;
                }
                String existingDay = parts[0];
                String existingTime = parts[1];

                LocalTime existingStart;
                try {
                    existingStart = LocalTime.parse(existingTime, timeFormatter);
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid time format in database: " + existingTime);
                    continue;
                }

                // Calculate existing time slots based on duration
                List<timeRange> existingSlots = new ArrayList<>();
                for (int i = 0; i < existingDuration; i++) {
                    LocalTime slotStart = existingStart.plusMinutes(i * (45 + 10));
                    LocalTime slotEnd = slotStart.plusMinutes(45);
                    existingSlots.add(new timeRange(slotStart, slotEnd));
                }

                // Check for any overlapping slots
                for (timeRange desired : desiredSlots) {
                    for (timeRange existing : existingSlots) {
                        if (desired.overlapsWith(existing)) {
                            return false; // Overlap found
                        }
                    }
                }
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error while checking classroom availability: " + e.getMessage());
            return false;
        }

        return true; // No overlaps found
    }

    public static List<Course> getConflictingCourses(String classroomName, String day, String time, int duration) {
        List<Course> conflictingCourses = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime desiredStart;
        try {
            desiredStart = LocalTime.parse(time, timeFormatter);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid time format: " + time);
            return conflictingCourses;
        }

        // Calculate desired time slots based on duration
        List<timeRange> desiredSlots = new ArrayList<>();
        for (int i = 0; i < duration; i++) {
            LocalTime slotStart = desiredStart.plusMinutes(i * (45 + 10));
            LocalTime slotEnd = slotStart.plusMinutes(45);
            desiredSlots.add(new timeRange(slotStart, slotEnd));
        }

        // SQL query to fetch existing allocations for the classroom on the selected day
        String sql = """
            SELECT c.courseName, c.timeToStart, c.duration, c.lecturer
            FROM Allocated a
            JOIN Courses c ON a.courseName = c.courseName
            WHERE a.classroomName = ? AND c.timeToStart LIKE ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            pstmt.setString(2, day + " %"); // Only courses on the selected day
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String timeToStart = rs.getString("timeToStart");
                int existingDuration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                // Split the timeToStart into day and time
                String[] parts = timeToStart.split(" ");
                if (parts.length < 2) {
                    System.err.println("Invalid timeToStart format: " + timeToStart);
                    continue;
                }
                String existingDay = parts[0];
                String existingTime = parts[1];

                LocalTime existingStart;
                try {
                    existingStart = LocalTime.parse(existingTime, timeFormatter);
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid time format in database: " + existingTime);
                    continue;
                }

                // Calculate existing time slots based on duration
                List<timeRange> existingSlots = new ArrayList<>();
                for (int i = 0; i < existingDuration; i++) {
                    LocalTime slotStart = existingStart.plusMinutes(i * (45 + 10));
                    LocalTime slotEnd = slotStart.plusMinutes(45);
                    existingSlots.add(new timeRange(slotStart, slotEnd));
                }

                // Check for any overlapping slots
                boolean hasOverlap = false;
                for (timeRange desired : desiredSlots) {
                    for (timeRange existing : existingSlots) {
                        if (desired.overlapsWith(existing)) {
                            hasOverlap = true;
                            break;
                        }
                    }
                    if (hasOverlap) break;
                }

                if (hasOverlap) {
                    Course conflictingCourse = new Course(
                            courseName,
                            0, // capacity not needed here
                            new ArrayList<>(), // students not needed here
                            classroomName,
                            timeToStart,
                            existingDuration,
                            lecturer
                    );
                    conflictingCourses.add(conflictingCourse);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error while fetching conflicting courses: " + e.getMessage());
        }

        return conflictingCourses;
    }

    public static void changeClassroom(String course, String classroom) {
        try {
            String checkAllocation = "SELECT COUNT(*) FROM Allocated WHERE courseName = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkAllocation);
            checkStmt.setString(1, course);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                PreparedStatement updateStmt = conn.prepareStatement("""
                UPDATE Allocated
                SET classroomName = ?
                WHERE courseName = ?;
            """);
                updateStmt.setString(1, classroom);
                updateStmt.setString(2, course);
                updateStmt.executeUpdate();
                System.out.println("Classroom updated successfully for course: " + course);
            } else {
                System.out.println("No existing allocation found for course: " + course);
            }
        } catch (SQLException e) {
            System.err.println("Error while changing classroom: " + e.getMessage());
        }
    }

    public static void removeStudentFromCourse(String courseName, String studentName) {
        try {
            PreparedStatement deleteEnrollment = conn.prepareStatement("""
            DELETE FROM Enrollments
            WHERE courseName = ? AND studentName = ?;
        """);

            deleteEnrollment.setString(1, courseName);
            deleteEnrollment.setString(2, studentName);
            int rowsAffected = deleteEnrollment.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Student removed from course successfully: " + studentName + " -> " + courseName);
            } else {
                System.out.println("No enrollment found for student " + studentName + " in course " + courseName);
            }
        } catch (SQLException e) {
            System.err.println("Error while removing student from course: " + e.getMessage());
        }
    }

    // New method to get all classroom names from the DB
    public static List<String> getAllClassroomNames() {
        List<String> classroomNames = new ArrayList<>();
        String sql = "SELECT DISTINCT classroomName FROM Classrooms";
        try (Statement stmt = connect().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                classroomNames.add(rs.getString("classroomName"));
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching classroom names: " + e.getMessage());
        }
        return classroomNames;
    }

    // New method to get all classroom capacity from the DB
    public static List<Integer> getAllClassroomCapacities(String classroomName) {
        List<Integer> classroomCapacities = new ArrayList<>();
        String sql = "SELECT DISTINCT capacity FROM Classrooms WHERE classroomName = ?";

        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, classroomName); // Set the parameter value
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    classroomCapacities.add(rs.getInt("capacity")); // Fetch the capacity as an integer
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching classroom capacities: " + e.getMessage());
        }

        return classroomCapacities;
    }



    public static List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT studentId, studentName FROM Students";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = String.valueOf(rs.getInt("studentId"));
                String name = rs.getString("studentName");
                // Assuming Student has a constructor Student(String studentId, String fullName, List<Course> enrolledCourses)
                students.add(new Student(name, new ArrayList<>()));
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching all students: " + e.getMessage());
        }
        return students;
    }

    // New method to get all Course names from the DB
    public static List<String> getAllCourseNames() {
        List<String> courseNames = new ArrayList<>();
        String sql = "SELECT DISTINCT courseName FROM Courses";
        try (Statement stmt = connect().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                courseNames.add(rs.getString("courseName"));
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching course names: " + e.getMessage());
        }
        return courseNames;
    }

        // New method to get all classroom capacity from the DB
//    public static List<Integer> getCourseCapacities(String courseName) {
//        List<Integer> courseCapacities = new ArrayList<>();
//        String sql = "SELECT DISTINCT En FROM Courses WHERE courseName = ?";
//
//        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
//            pstmt.setString(1, courseName); // Set the parameter value
//            try (ResultSet rs = pstmt.executeQuery()) {
//                while (rs.next()) {
//                    courseCapacities.add(rs.getInt("capacity")); // Fetch the capacity as an integer
//                }
//            }
//        } catch (SQLException e) {
//            System.err.println("Error while fetching course capacities: " + e.getMessage());
//        }
//
//        return courseCapacities;
//    }




}
