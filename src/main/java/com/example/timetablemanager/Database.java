package com.example.timetablemanager;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

        if (conn == null) {
            try {
                conn = DriverManager.getConnection(url);
                System.out.println("Connected to database!");
                createTables(); // Create tables when connected
                loadAllCourses(); // Load courses into memory
            } catch (SQLException e) {
                System.err.println("Connection error: " + e.getMessage());
            }
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
    private static void createTables() {
        String createCoursesTable = """
            CREATE TABLE IF NOT EXISTS Courses (
                courseId INTEGER PRIMARY KEY AUTOINCREMENT,
                courseName TEXT NOT NULL,
                lecturer TEXT,
                duration INTEGER,
                timeToStart TEXT
            );
        """;

        String createClassroomsTable = """
            CREATE TABLE IF NOT EXISTS Classrooms (
                classroomId INTEGER PRIMARY KEY AUTOINCREMENT,
                classroomName TEXT NOT NULL,
                capacity INTEGER NOT NULL
            );
        """;

        String createAllocatedTable = """
            CREATE TABLE IF NOT EXISTS Allocated (
                allocationID INTEGER PRIMARY KEY AUTOINCREMENT,
                courseName TEXT NOT NULL,
                classroomName TEXT NOT NULL,
                FOREIGN KEY (courseName) REFERENCES Courses (courseName),
                FOREIGN KEY (classroomName) REFERENCES Classrooms (classroomName)
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
    private static void loadAllCourses() {
        courseList.clear();
        String sql = "SELECT DISTINCT courseName, timeToStart, duration, lecturer FROM Courses";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean coursesFound = false;

            while (rs.next()) {
                String courseName = rs.getString("courseName");
                String startTime = rs.getString("timeToStart");
                int duration = rs.getInt("duration");
                String lecturer = rs.getString("lecturer");

                // Fetch enrolled students
                List<String> enrolledStudentNames = getStudentsEnrolledInCourse(courseName);
                List<Student> enrolledStudents = new ArrayList<>();
                for (String studentName : enrolledStudentNames) {
                    enrolledStudents.add(new Student("UnknownID", studentName, new ArrayList<>()));
                }

                // For fields not stored in DB, use placeholders:
                String courseID = "N/A"; // no courseID in DB
                String description = (lecturer == null || lecturer.isEmpty()) ? "No description" : lecturer;
                int capacity = 30; // default capacity
                String classroom = ""; // no classroom info here
                List<String> days = new ArrayList<>();
                List<String> times = new ArrayList<>();
                if (startTime != null && !startTime.isEmpty()) {
                    times.add(startTime);
                }

                Course course = new Course(
                        courseName,
                        courseID,
                        description,
                        capacity,
                        enrolledStudents,
                        classroom,
                        days,
                        times,
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
        // Öncelikle daha önce atanmış mı kontrol et
        String checkAllocation = "SELECT COUNT(*) FROM Allocated WHERE courseName = ? AND classroomName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkAllocation)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, classroomName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("This course is already allocated to the selected classroom.");
                return; // Daha önce atanmışsa, işlem yapılmaz
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
                students.add(new Student(id, name, new ArrayList<>()));
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching all students: " + e.getMessage());
        }
        return students;
    }

}
