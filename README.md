# Placement Management System

## Project Overview
The Placement Management System is a Java-based application developed as an Object-Oriented Programming (OOP) course project. It is designed to automate and streamline the campus recruitment process by managing student profiles, company details, eligibility criteria, and placement records in an organized and efficient manner.

## Objectives
- To reduce manual effort in managing placement activities
- To ensure accurate eligibility verification based on CGPA and skills
- To maintain structured and persistent placement records
- To provide a simple and modular system using OOP principles

## Features
- Student profile management (ID, name, branch, CGPA, skills)
- Company profile management (role, minimum CGPA, required skills)
- Automatic eligibility checking
- Placement drive execution with selection/rejection results
- Storage of placement history records
- Console-based output with clear formatting

## Technologies Used
- Programming Language: Java
- Concepts Used:
  - Object-Oriented Programming
  - Inheritance and Abstraction
  - Collections Framework (ArrayList, HashMap)
  - Serialization
- Platform: Console-based application

## System Design
The system is designed using an object-oriented approach:
- `Person` (abstract class) → Base for Student
- `Student` → Stores student details and skills
- `Company` → Stores company requirements
- `PlacementRecord` → Stores placement results
- `PlacementManager` → Core logic for eligibility and drives

## How to Run the Project
1. Compile the program:
javac PlacementManagementSystem.java
2. Run the program:
java PlacementManagementSystem
## Sample Output
- Displays eligible students for a company
- Executes a placement drive
- Shows placement records with status and date

## Team Members
- Sairam Kanathala (1602-24-737-166)
- Vaishnavi Medicharla (1602-24-737-187)

## Institution
Vasavi College of Engineering (Autonomous)  
Department of Information Technology  

## Conclusion
The Placement Management System effectively automates placement operations and demonstrates the practical application of OOP concepts in Java. It provides a scalable and maintainable solution that can be further enhanced with GUI integration, database support, and analytics features.
