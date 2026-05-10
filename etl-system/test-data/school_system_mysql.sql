-- School System Database for MySQL
-- Test database for ETL system Phase 3

DROP DATABASE IF EXISTS school_system;
CREATE DATABASE school_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE school_system;

-- Table: departments
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    building VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: teachers
CREATE TABLE teachers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    department_id BIGINT NOT NULL,
    hire_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- Table: courses
CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    credits INT NOT NULL,
    department_id BIGINT NOT NULL,
    teacher_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
);

-- Table: students
CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    enrollment_date DATE NOT NULL,
    graduation_year INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: enrollments (many-to-many relationship between students and courses)
CREATE TABLE enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_date DATE NOT NULL,
    grade DECIMAL(3, 2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment (student_id, course_id)
);

-- Insert sample data

-- Departments
INSERT INTO departments (name, building) VALUES
('Computer Science', 'Building A'),
('Mathematics', 'Building B'),
('Physics', 'Building C'),
('Literature', 'Building D');

-- Teachers
INSERT INTO teachers (first_name, last_name, email, phone, department_id, hire_date) VALUES
('John', 'Smith', 'john.smith@school.edu', '555-0101', 1, '2018-09-01'),
('Emily', 'Johnson', 'emily.johnson@school.edu', '555-0102', 1, '2019-01-15'),
('Michael', 'Williams', 'michael.williams@school.edu', '555-0103', 2, '2017-08-20'),
('Sarah', 'Brown', 'sarah.brown@school.edu', '555-0104', 3, '2020-02-10'),
('David', 'Jones', 'david.jones@school.edu', '555-0105', 4, '2016-09-05');

-- Courses
INSERT INTO courses (course_code, course_name, credits, department_id, teacher_id) VALUES
('CS101', 'Introduction to Programming', 4, 1, 1),
('CS201', 'Data Structures', 4, 1, 2),
('MATH101', 'Calculus I', 3, 2, 3),
('MATH201', 'Linear Algebra', 3, 2, 3),
('PHYS101', 'Physics I', 4, 3, 4),
('LIT101', 'World Literature', 3, 4, 5);

-- Students
INSERT INTO students (first_name, last_name, email, phone, enrollment_date, graduation_year) VALUES
('Alice', 'Anderson', 'alice.anderson@student.edu', '555-1001', '2022-09-01', 2026),
('Bob', 'Baker', 'bob.baker@student.edu', '555-1002', '2022-09-01', 2026),
('Carol', 'Clark', 'carol.clark@student.edu', '555-1003', '2021-09-01', 2025),
('Daniel', 'Davis', 'daniel.davis@student.edu', '555-1004', '2023-09-01', 2027),
('Eva', 'Evans', 'eva.evans@student.edu', '555-1005', '2022-09-01', 2026);

-- Enrollments
INSERT INTO enrollments (student_id, course_id, enrollment_date, grade, status) VALUES
(1, 1, '2022-09-01', 3.75, 'COMPLETED'),
(1, 2, '2023-01-15', NULL, 'ACTIVE'),
(1, 3, '2022-09-01', 3.50, 'COMPLETED'),
(2, 1, '2022-09-01', 3.25, 'COMPLETED'),
(2, 3, '2022-09-01', 3.00, 'COMPLETED'),
(2, 5, '2023-01-15', NULL, 'ACTIVE'),
(3, 2, '2023-01-15', 3.90, 'COMPLETED'),
(3, 4, '2023-01-15', NULL, 'ACTIVE'),
(4, 1, '2023-09-01', NULL, 'ACTIVE'),
(4, 6, '2023-09-01', NULL, 'ACTIVE'),
(5, 1, '2022-09-01', 4.00, 'COMPLETED'),
(5, 2, '2023-01-15', 3.85, 'COMPLETED'),
(5, 3, '2022-09-01', 3.75, 'COMPLETED');

-- Display summary
SELECT 'Database schema created successfully!' AS status;
SELECT COUNT(*) AS departments_count FROM departments;
SELECT COUNT(*) AS teachers_count FROM teachers;
SELECT COUNT(*) AS courses_count FROM courses;
SELECT COUNT(*) AS students_count FROM students;
SELECT COUNT(*) AS enrollments_count FROM enrollments;
