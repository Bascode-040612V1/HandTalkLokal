-- Login System Database Schema
-- Database: login_system
-- Created for XAMPP PHP/MySQL application

-- Create database
CREATE DATABASE IF NOT EXISTS login_system;
USE login_system;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Sample data (optional - remove if not needed)
-- INSERT INTO users (username, email, password) VALUES 
-- ('admin', 'admin@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'), -- password: password
-- ('testuser', 'test@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'); -- password: password

-- Grant permissions (adjust as needed for your environment)
GRANT ALL PRIVILEGES ON login_system.* TO 'root'@'localhost';
FLUSH PRIVILEGES;