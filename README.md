# Java-Client-Server-Account-Management-System
Java Swing client-server account management system using JDBC and MySQL.
A desktop user authentication system built in Java with a Swing GUI and MySQL persistence. Users can register, log in, change their password, and recover a locked account by email. An admin control panel shows registered users, logged-in users, locked-out accounts, and active connections in real time.

This was a four-person team project for CSC335 (Software Engineering) at California Lutheran University. The team followed a requirements-first process, producing an RFP response, use cases, user stories, UML diagrams, and a test plan before writing code.

Features
Registration with email format validation and password rules (at least 8 characters, including a lowercase letter and a number)
Login with credential checks against the MySQL database
Account lockout after 3 consecutive failed login attempts
Password recovery by email through Gmail SMTP
Password change for logged-in users
Admin control panel that refreshes every second with live user and connection data
Background threading so database work doesn't freeze the GUI

Tech Stack
Layer	Technology
Language	Java 27
GUI	Java Swing
Database	MySQL 8.4+ via JDBC (mysql-connector-j)
Email	Gmail SMTP via javax.mail
Design	UMLet (class, sequence, state machine, and use case diagrams)

Design Documents
Before implementation, the team produced a full set of design artifacts.

Document	Purpose
Use Case Diagram	Actors and system interactions for client and server
Class Diagram	Planned classes and their relationships
Sequence Diagrams	Message flow for each use case
State Machine Diagram	The client experience from launch to shutdown
Test Plan	Functional and integration test cases derived from user stories
Summary Document	Project overview and outcomes

