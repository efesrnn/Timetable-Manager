package com.example.timetablemanager;
import java.io.File;
import java.sql.*;
/**
 * This static class is responsible for creating and managing the timetable database.
 * The database consists of 5 tables:
 * - Courses: Stores information about courses.
 * - Classrooms: Stores information about classrooms.
 * - Allocated: Manages the allocation of courses to classrooms.
 * - Students: Stores student information.
 * - Enrollments: Manages the enrollments of students in courses.

 * The database is created and stored in the user's home directory under the Documents folder.
 * Usage of the database can be done using methods like:
 * - Database.connect() to establish a connection.
 * - Database.addCourse() to add a course.
 * - Database.addClassroom() to add a classroom.
 * - Database.allocateCourseToClassroom() to allocate courses to classrooms.
 * - Database.listCourses() to list all courses, etc.
 */

public class Database {
    private static final String dbPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TimetableManagement";
    private static final String url = "jdbc:sqlite:" + dbPath + File.separator + "TimetableManagement.db";
    private static Connection conn = null;

    // Create database connection
    public static Connection connect() {

        File dbDir = new File(dbPath);
        if (!dbDir.exists()) {
            if (dbDir.mkdir()) {
                System.out.println("Folder created successfully: " + dbDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create folder: " + dbDir.getAbsolutePath());
                return null;
            }
        }

        // Check if database file exists
        File dbFile = new File(dbPath + File.separator + "TimetableManagement.db");
        if (!dbFile.exists()) {
            System.out.println("Database file does not exist,creating it.");
        } else {
            System.out.println("Database file found: " + dbFile.getAbsolutePath());
        }

        if (conn == null) {
            try {
                conn = DriverManager.getConnection(url);
                System.out.println("Connected to the database!");
                createTables();
            } catch (SQLException e) {
                System.err.println("Connection error: " + e.getMessage());
            }
        }
        return conn;
    }

    // Close database connection
    public static void close() {
        try {
            if (conn != null) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error while closing connection: " + e.getMessage());
        }
    }

    // CREATE TABLES
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


        String createEnrollmentsTable = """
    CREATE TABLE IF NOT EXISTS Enrollments (
        enrollmentId INTEGER PRIMARY KEY AUTOINCREMENT,
        courseName TEXT NOT NULL,
        studentName TEXT NOT NULL,
        FOREIGN KEY (courseName) REFERENCES Courses (courseName),
        FOREIGN KEY (studentName) REFERENCES Students (studentName)
    );
""";


        String createStudentsTable = """
        CREATE TABLE IF NOT EXISTS Students (
            studentId INTEGER PRIMARY KEY AUTOINCREMENT,
            studentName TEXT NOT NULL
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

    // ADD COURSE
    public static void addCourse(String courseName, String lecturer,
                                 int duration, String timeToStart)
    {
        String sql = "INSERT INTO Courses  (courseName, lecturer, duration, timeToStart) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, lecturer);
            pstmt.setInt(3, duration);
            pstmt.setString(4, timeToStart);
            pstmt.execute();
            System.out.println("Course added successfully!");
        } catch (SQLException e) {
            System.err.println("Error while adding course: " + e.getMessage());
        }
    }

    // Add enrollment (course-student relationship)
    public static void addEnrollment(String courseName, String studentName) {
        String sql = "INSERT INTO Enrollments (courseName, studentName) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);
            pstmt.setString(2, studentName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error while adding enrollment:: " + e.getMessage());
        }
    }



    // Add student
    public static void addStudent(String studentName) {
        String sql = "INSERT INTO Students (studentName) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.executeUpdate();
            System.out.println("Student " + studentName + " added successfully.");
        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
        }
    }

    // Add classroom
    public static void addClassroom(String classroomName, int capacity) {
        String sql = "INSERT INTO Classrooms (classroomName, capacity) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, classroomName);
            pstmt.setInt(2, capacity);
            pstmt.executeUpdate();
            System.out.println("Classroom added successfully!");
        } catch (SQLException e) {
            System.err.println("Error while adding classroom: " + e.getMessage());
        }
    }

    // Allocate course to classroom
    public static void allocateCourseToClassroom(String courseName, String classroomName) {
        String sql = "INSERT INTO Allocated (courseName, classroomName) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseName);      // courseName'i doğrudan ekliyoruz
            pstmt.setString(2, classroomName);   // classroomName'i doğrudan ekliyoruz
            pstmt.executeUpdate();
            System.out.println("Course successfully allocated to classroom!");
        } catch (SQLException e) {
            System.err.println("Error while allocating course to classroom: " + e.getMessage());
        }
    }

    // List course information
    public static void listCourses() {
        String sql = "SELECT * FROM Courses";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("ID: %s, Name: %s, Lecturer: %s\n",
                        rs.getString("courseId"), rs.getString("courseName"), rs.getString("lecturer"));
            }
        } catch (SQLException e) {
            System.err.println("Error while fetching course details:  " + e.getMessage());
        }
    }

    // Change the classroom for a course
    public static void changeClassroom(String course, String classroom) {
        try {
            PreparedStatement updateAllocated = conn.prepareStatement("""
            UPDATE Allocated
            SET classroomName = ?
            WHERE courseName = ?;
        """);

            updateAllocated.setString(1, classroom);
            updateAllocated.setString(2, course);
            updateAllocated.executeUpdate();
            System.out.println("Classroom change successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Match course with classroom
    public static void matchClassroom(String course, String classroom) {
        try {
            PreparedStatement insertAllocated = conn.prepareStatement("""
            INSERT INTO Allocated (courseName, classroomName)
            VALUES (?, ?);
        """);

            insertAllocated.setString(1, course);
            insertAllocated.setString(2, classroom);
            insertAllocated.executeUpdate();
            System.out.println("Classroom matched successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getClassroomOfCourse(String courseName) {
        String classroom = null;
        try {
            PreparedStatement getClassroom = conn.prepareStatement("""
            SELECT classroomName
            FROM Allocated
            WHERE courseName = ?;
        """);
            getClassroom.setString(1, courseName);
            ResultSet rs = getClassroom.executeQuery();

            if (rs.next()) {
                classroom = rs.getString("classroomName");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classroom;
    }

}
