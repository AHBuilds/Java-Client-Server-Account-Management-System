# Java-Client-Server-Account-Management-System

A desktop user authentication system built in Java with a Swing GUI and MySQL persistence. Users can register, log in, change their password, and recover a locked account by email. An admin control panel shows registered users, logged-in users, locked-out accounts, and active connections in real time.

This was a four-person team project for CSC335 (Software Engineering) at California Lutheran University. The team followed a requirements-first process, producing an RFP response, use cases, user stories, UML diagrams, and a test plan before writing code.

---

## Features

- **Registration** with email format validation and password rules (at least 8 characters, including a lowercase letter and a number)
- **Login** with credential checks against the MySQL database
- **Account lockout** after 3 consecutive failed login attempts
- **Password recovery** by email through Gmail SMTP
- **Password change** for logged-in users
- **Admin control panel** that refreshes every second with live user and connection data
- **Background threading** so database work doesn't freeze the GUI

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 27 |
| GUI | Java Swing |
| Database | MySQL 8.4+ via JDBC (`mysql-connector-j`) |
| Email | Gmail SMTP via `javax.mail` |
| Design | UMLet (class, sequence, state machine, and use case diagrams) |

---

## Design Documents

Before implementation, the team produced a full set of design artifacts.

| Document | Purpose |
|---|---|
| Use Case Diagram | Actors and system interactions for client and server |
| Class Diagram | Planned classes and their relationships |
| Sequence Diagrams | Message flow for each use case |
| State Machine Diagram | The client experience from launch to shutdown |
| Test Plan | Functional and integration test cases derived from user stories |
| Summary Document | Project overview and outcomes |

---

## Design vs. Implementation

The class design was completed before development began. As the system was built, the team simplified the architecture, and two planned classes were dropped entirely. These are marked in red in the original class descriptions.

| Planned class | Implemented as | Notes |
|---|---|---|
| `Network` | Not implemented | The planned TCP socket layer was descoped. The client connects to MySQL directly over JDBC instead of routing requests through a server process. |
| `User` | `accounts` table | A separate class would have duplicated the database row, so user data lives only in MySQL. |
| `ClientApplication` + `ClientGUI` | `ClientApp` | GUI and application logic merged into one class. |
| `ServerApplication` + `ServerGUI` | `ServerApplication` | Became an admin dashboard opened from the client after login. |
| `UserDatabase` | `DBaseConnectionP` | Handles all queries and updates. |
| `PasswordValidator` | `PasswordAuthernticator` | Regex-based password rule check. |
| `EmailService` | `SendEmailUsingGMailSMTP` | Sends recovery and lockout emails. |
| `Session` | `connections` table | Connections are counted in the database rather than tracked as objects. |
| `AuthenticationService` | `AuthenticationService` | Implemented as planned. |

The biggest change is architectural. The original design was a three-tier client/server system (client → network → server → database). The delivered system is two-tier (client → database), with the "server" acting as an admin view over the same database.

---

## Testing

A full [test plan](docs/Test%20Plan%20Document.pdf) covering connection, registration, login, lockout, recovery, password change, and shutdown was written during the design phase using a grey-box, bottom-up integration approach. It was not formally executed before the course ended.

While setting the project up again, after the course, multiple additional tests were conducted against the original test plans, and updated test plans, which resulted in the following bugs listed below.

---

## Bugs Found and Fixed

The following were identified and fixed while reviving the project:

- **SQL Injection and unsafe query construction.** Several database operations used string concatenation to build SQL queries, creating SQL injection risks and causing errors with special characters. Fixed by replacing unsafe queries with parameterized PreparedStatement queries in registration, account activation and deactivation, login-attempt updates, account locking and unlocking, and login-attempt resets.
- **Usernames containing apostrophes.** Usernames such as dash'test caused SQL queries to fail when values were inserted directly into query strings. Fixed by using parameterized SQL statements and verified by testing a username containing an apostrophe.
- **Login attempt tracking.** Failed login attempts needed to be recorded correctly to support account lockout. Fixed by updating the login-attempt database operations and testing repeated failed login attempts.
- **Account activation, deactivation, and unlocking.** Account status changes needed to update the database reliably. Fixed by updating the related SQL operations to use parameterized PreparedStatement queries and testing the account-management functionality.
- **Multi-client functionality.** The application needed to be tested with multiple clients interacting with the server. Tested multiple client instances to verify core account-management functionality during concurrent use.
---

## Known Issues and Limitations

These are documented intentionally as areas for future improvement:

- **Plain-text passwords.** Passwords are stored as-is. A production system would hash them with an algorithm, such as bcrypt.
- **SQL injection risk.** Some queries are built with string concatenation. They should be rewritten with `PreparedStatement` parameters.
- **No network layer.** The planned TCP client/server layer was never built, so the client must run on the same machine as the database.
- **No unique constraint on usernames.** Duplicate prevention relies on application code rather than the database schema.
- **SSL disabled.** Acceptable for local development only.

---

## How to Run

### 1. Set up MySQL

Install MySQL Community Server 8.4 or newer, then create the database and tables:

```sql
CREATE DATABASE csc335;
USE csc335;

CREATE TABLE accounts (
  id INT NOT NULL AUTO_INCREMENT,
  username VARCHAR(45) NOT NULL,
  password VARCHAR(45) NOT NULL,
  email VARCHAR(45) NOT NULL,
  active INT UNSIGNED NOT NULL,
  lockedout INT UNSIGNED NOT NULL,
  attempts INT NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE connections (
  user INT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (user)
);

INSERT INTO accounts (username, password, email, active, lockedout, attempts)
VALUES ('demo_user', 'Password123', 'demo@example.com', 0, 0, 0);
```

The original database dumps are also in the [`sql`](sql) folder.

### 2. Open the project in IntelliJ IDEA

Open the repository folder and set the project SDK. Mark `src` as a Sources Root if needed.

### 3. Add dependencies

In **File → Project Structure → Libraries → + → From Maven**, add:

```
com.mysql:mysql-connector-j:9.1.0
com.sun.mail:javax.mail:1.6.2
```

### 4. Set environment variables

The code reads credentials with `System.getenv`. In the run configuration for `client.ClientApp`, set:

```
DB_USER = root/"your_DB_Password"; DB_Password = "your_DB_password"
```

To enable password recovery emails, also add `SMTP_USER` and `SMTP_PASS`, using a Gmail App Password rather than your normal password.

### 5. Run

Run `client.ClientApp`, enter `localhost` as the server address, click **Connect**, and log in as `demo_user` with password `Password123`. After logging in, the **Query** button opens the admin control panel.

---

## Team

Built by Jacob Glasby, Abraham Horta, Levi Horta, and David Membreno. The team planned and designed the system together and split implementation work as the project evolved, so most of the codebase reflects shared effort.

During the course, I mainly focused on the design side of the project, which included the UML diagrams, use cases, and executing the test plans. After the course ended, I revisited the project and started with the previous results as a blueprint, rebuilt the database, conducted further testing and debugging, and made code adjustments/improvements to address the bugs, mentioned previously.
