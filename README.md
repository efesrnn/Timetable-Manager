# Overview

Timetable Manager is a desktop application designed to streamline the management of academic schedules. It offers features for efficiently assigning classrooms, organizing course timetables, and generating weekly schedules for students, instructors, and classrooms. The application is developed using Java, with JavaFX for the graphical user interface (GUI) and SQLite for database management.

# Installation
## Prerequisites
* Operating system: Windows
* Ensure you have Java Runtime Environment (JRE) installed (version 8 or higher). [Download JRE here](https://www.oracle.com/java/technologies/downloads/#java8).
## Windows Installation Guide
1. Download the [latest release](https://github.com/sudedaka/TimetableManager/releases) of the Timetable Manager
2. Open setup.exe in the project folder
3. Follow the installation guide:
* Choose an install folder
* Decide on creating
4. Launch the app from the Start Menu or Desktop Shortcut

# Features and Usage
**Data import:** The system supports data import via an import button, allowing users to upload Courses.csv and ClassroomCapacity.csv files. The imported files are processed, and their content is stored in TimetableManager.db


**Add New Courses:**  Add new courses by entering details such as course name, start time, classroom, capacity, duration, and lecturer. Also showing a schedule timetable.


**Enrollment Students:** Allows adding and removing students from the selected course.


**Assignment Classroom and Course:** Enables matching the selected classroom with the chosen course.


**Swap Course Classrooms:** Enables users to change the classrooms of courses without issues related to classroom capacities directly from the course page.


**View Weekly Schedules:** Users can view predefined weekly schedules for courses,capcaity, duration, start time, classrooms and lecturers.


**Search and Refresh:** Quickly refresh schedules and search for specific data.Welcome to the TimetableManager wiki!

# User Interface
![proje1](https://github.com/user-attachments/assets/23abe9c7-3556-46ea-b4a6-7f6ec5c94f7a)

![proje2](https://github.com/user-attachments/assets/03900a2d-c690-49cb-ac16-3fe90be2482b)

# File Structure
**Database:** The system stores data in an SQLite database file located in the user's Documents folder.


**CSV Support:** The application supports importing classroom and course data from CSV files.  
* [Courses.csv](https://github.com/user-attachments/files/18218603/Courses.csv)

* [ClassroomCapacity.csv](https://github.com/user-attachments/files/18218605/ClassroomCapacity.csv)


# Help
Access the inbuilt Help menu for detailed instructions on using the application. No internet connection is required to view the manual.


# Reporting Issues
If you find a bug or want to suggest a new feature, please open an issue on the GitHub repository.

# Project Management
We use Trello to manage tasks and track the progress of this project. You can view our Trello board [here](https://trello.com/invite/b/671be8cc9f3366183f6870ee/ATTI72dfdda74b2d1cdf4391fc1670f7752cC3B9EFAB/timetable-manager). 

# Documents
Link to the Design Document (PDF) [here](https://github.com/user-attachments/files/18218509/Design_Document_for_Timetable_Manager.pdf).


Link to the Requirements Document (PDF) [here](https://github.com/user-attachments/files/18218503/Software_Requirements_Specification_for_Timetable_Manager.pdf).

