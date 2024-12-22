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
    private static List<Student> allStudents = new ArrayList<>();

    // Transaction Management Methods

    /**
     * Begins a database transaction by setting auto-commit to false.
     */
    public static void beginTransaction() throws SQLException {
        if (conn == null || conn.isClosed()) {
            connect();
        }
        conn.setAutoCommit(false);
        System.out.println("Transaction started.");
    }

    /**
     * Commits the current database transaction and sets auto-commit back to true.
     */
    public static void commitTransaction() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("Transaction committed.");
        }
    }

    /**
     * Rolls back the current database transaction and sets auto-commit back to true.
     */
    public static void rollbackTransaction() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                conn.setAutoCommit(true);
                System.out.println("Transaction rolled back.");
            }
        } catch (SQLException e) {
            System.err.println("Error during transaction rollback: " + e.getMessage());
        }
    }

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
                loadStudents();
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
                System.out.println("Database connection closed!");
            }
        } catch (SQLException e) {
            System.err.println("An error occurred while closing the connection: " + e.getMessage());
        }
    }

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
                studentName TEXT NOT NULL UNIQUE
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
            System.out.println("Tables created or verified successfully!");
        } catch (SQLException e) {
            System.err.println("Error while creating tables: " + e.getMessage());
        }
    }

    // Load all courses into the in-memory list (Includes courses and enrolled students)
    public static void loadAllCourses() {
        courseList.clear();
        String sql = "SELECT DISTINCT courseName, timeToStart, duration, lecturer FROM Courses";
        String sql2 = "SELECT classroomName FROM Allocated WHERE courseName = ?";
        String sql3 = "SELECT capacity FROM Classrooms WHERE classroomName = ?";
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
                String classroomName = ""; // no classroom info here
                String startTime = rs.getString("timeToStart");

                // Fetch allocated classroom
                try (PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {
                    pstmt2.setString(1, courseName);
                    try (ResultSet rs2 = pstmt2.executeQuery()) {
                        if (rs2.next()) {
                            classroomName = rs2.getString("classroomName");
                        }
                    }
                }

                // Fetch classroom capacity
                if (!classroomName.isEmpty()) {
                    try (PreparedStatement pstmt3 = conn.prepareStatement(sql3)) {
                        pstmt3.setString(1, classroomName);
                        try (ResultSet rs3 = pstmt3.executeQuery()) {
                            if (rs3.next()) {
                                capacity = rs3.getInt("capacity");
                            }
                        }
                    }
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

    public static List<Course> loadCoursesofStudents(String student) {
        String query = """
                SELECT DISTINCT * FROM Courses
                     WHERE  courseName IN (
                             SELECT DISTINCT courseName
                             FROM Enrollments
                             WHERE studentName = ?)""";

        List<Course> courses = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(query)
        ) {
            pstmt.setString(1, student);
            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                Course course = new Course(rs.getString("courseName"),0,null,null,rs.getString("timeToStart"),
                        rs.getInt("duration"),rs.getString("lecturer"));
                courses.add(course);
            }

        } catch (SQLException e) {
            System.err.println("Error loading students: " + e.getMessage());
        }

        return courses;

    }

    public static List<Course> loadCoursesForStudent1(String studentName) {
        String query = """
            SELECT DISTINCT c.courseName, c.timeToStart, c.duration, c.lecturer
            FROM Courses c
            JOIN Enrollments e ON c.courseName = e.courseName
            WHERE e.studentName = ?
    """;

        String queryAllocated = "SELECT classroomName FROM Allocated WHERE courseName = ?";
        String queryCapacity = "SELECT capacity FROM Classrooms WHERE classroomName = ?";

        List<Course> courses = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentName);
            ResultSet rs = pstmt.executeQuery();

            boolean coursesFound = false;

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String timeToStart = rs.getString("timeToStart");
                int duration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                // Fetch enrolled students for this course
                List<String> enrolledStudentNames = getStudentsEnrolledInCourse(courseName);
                List<Student> enrolledStudents = new ArrayList<>();
                for (String name : enrolledStudentNames) {
                    enrolledStudents.add(new Student(name, new ArrayList<>()));
                }

                // Fetch allocated classroom
                String classroomName = "";
                try (PreparedStatement pstmtAllocated = conn.prepareStatement(queryAllocated)) {
                    pstmtAllocated.setString(1, courseName);
                    ResultSet rsAllocated = pstmtAllocated.executeQuery();
                    if (rsAllocated.next()) {
                        classroomName = rsAllocated.getString("classroomName");
                    }
                    rsAllocated.close();
                }

                // Fetch classroom capacity
                int capacity = 0;
                if (!classroomName.isEmpty()) {
                    try (PreparedStatement pstmtCapacity = conn.prepareStatement(queryCapacity)) {
                        pstmtCapacity.setString(1, classroomName);
                        ResultSet rsCapacity = pstmtCapacity.executeQuery();
                        if (rsCapacity.next()) {
                            capacity = rsCapacity.getInt("capacity");
                        }
                        rsCapacity.close();
                    }
                }

                // Handle day and time separation if needed
                List<String> days = new ArrayList<>();
                List<String> times = new ArrayList<>();

                if (timeToStart != null && !timeToStart.isEmpty()) {
                    String[] parts = timeToStart.split(" ");
                    if (parts.length >= 2) {
                        days.add(parts[0]);   // Day (e.g., "Monday")
                        times.add(parts[1]);  // Time (e.g., "10:00")
                    } else {
                        System.err.println("Unexpected timeToStart format: " + timeToStart);
                    }
                }

                // Create and add the course to the list
                Course course = new Course(
                        courseName,
                        capacity,
                        enrolledStudents,
                        classroomName,
                        timeToStart,
                        duration,
                        lecturer
                );

                courses.add(course);
                coursesFound = true;
            }

            rs.close();

            if (coursesFound) {
                System.out.println("Courses for student '" + studentName + "' loaded into memory.");
            } else {
                System.out.println("No courses found for student '" + studentName + "'.");
            }

        } catch (SQLException e) {
            System.err.println("Error loading courses for student '" + studentName + "': " + e.getMessage());
        }

        return courses;
    }

    public static void loadStudents() {
        String query = "SELECT studentId, studentName FROM Students";

        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            allStudents.clear();
            while (rs.next()) {
                String name = rs.getString("studentName");
                allStudents.add(new Student(name, new ArrayList<>()));
            }

        } catch (SQLException e) {
            System.err.println("Error loading students: " + e.getMessage());
        }
    }

    public static List<Student> getStudents() {
        return new ArrayList<>(allStudents);
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

        // Check if the classroom is available excluding the current course
        if (!isClassroomAvailable(classroomName, day, time, duration, courseName)) {
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

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    /**
     * Enhanced method to check if a classroom is available at a given day and time,
     * excluding a specific course (useful during swaps).
     *
     * @param classroomName   The name of the classroom to check.
     * @param day             The day of the week (e.g., "Monday").
     * @param startTime       The start time in "HH:mm" format.
     * @param duration        The duration in hours.
     * @param currentCourseId The course ID to exclude from availability checks.
     * @return True if the classroom is available, false otherwise.
     */
    public static boolean isClassroomAvailable(String classroomName, String day, String startTime, int duration, String currentCourseId) {
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

                // Skip the current course being swapped
                if (courseName.equals(currentCourseId)) {
                    continue;
                }

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

    /**
     * Overloaded method to check classroom availability without excluding any course.
     * Useful when assigning classrooms initially.
     *
     * @param classroomName The name of the classroom to check.
     * @param day           The day of the week (e.g., "Monday").
     * @param startTime     The start time in "HH:mm" format.
     * @param duration      The duration in hours.
     * @return True if the classroom is available, false otherwise.
     */
    public static boolean isClassroomAvailable(String classroomName, String day, String startTime, int duration) {
        // Call the enhanced method without excluding any course
        return isClassroomAvailable(classroomName, day, startTime, duration, "");
    }

    /**
     * Retrieves all courses that have conflicting schedules in a given classroom.
     *
     * @param classroomName The name of the classroom.
     * @param day           The day of the week.
     * @param time          The start time in "HH:mm" format.
     * @param duration      The duration in hours.
     * @return A list of conflicting courses.
     */
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

    /**
     * Changes the classroom allocation for a given course.
     *
     * @param course    The name of the course.
     * @param classroom The new classroom name.
     */
    public static void changeClassroom(String course, String classroom) {
        String checkAllocation = "SELECT COUNT(*) FROM Allocated WHERE courseName = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkAllocation)) {
            checkStmt.setString(1, course);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                String updateStmtStr = "UPDATE Allocated SET classroomName = ? WHERE courseName = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateStmtStr)) {
                    updateStmt.setString(1, classroom);
                    updateStmt.setString(2, course);
                    updateStmt.executeUpdate();
                    System.out.println("Classroom updated successfully for course: " + course);
                }
            } else {
                System.out.println("No existing allocation found for course: " + course);
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error while changing classroom: " + e.getMessage());
        }
    }

    public static void removeStudentFromCourse(String courseName, String studentName) {
        String sql = "DELETE FROM Enrollments WHERE courseName = ? AND studentName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, studentName);
            int rowsAffected = pstmt.executeUpdate();

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

    // New method to get all classroom capacities from the DB
    public static List<Integer> getAllClassroomCapacities(String classroomName) {
        List<Integer> classroomCapacities = new ArrayList<>();
        String sql = "SELECT DISTINCT capacity FROM Classrooms WHERE classroomName = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String id = String.valueOf(rs.getInt("studentId"));
                String name = rs.getString("studentName");
                // Assuming Student has a constructor Student(String studentName, List<Course> enrolledCourses)
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

    /**
     * Inner class to represent a time range.
     * Helps in checking overlapping schedules.
     */
    private static class timeRange {
        LocalTime start;
        LocalTime end;

        public timeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        /**
         * Checks if this time range overlaps with another.
         *
         * @param other The other time range to check against.
         * @return True if there is an overlap, false otherwise.
         */
        public boolean overlapsWith(timeRange other) {
            return (this.start.isBefore(other.end) && other.start.isBefore(this.end));
        }
    }
}
