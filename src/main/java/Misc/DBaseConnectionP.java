package Misc;

// -- download MySQL from: http://dev.mysql.com/downloads/
// -- Community Server version
// -- Installation instructions are here: http://dev.mysql.com/doc/refman/5.7/en/installing.html
// -- open MySQL Workbench to see the contents of the database
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import server.SendEmailUsingGMailSMTP;

// -- (If downloading the driver manually)
// -- Make sure the JDBC connector JAR is in the build path
// -- workspace -> properties -> Java Build Path -> Libraries -> Add External JARs...

// -- (If using Maven, which is used in this project)
// -- Refer to the POM.XML file/code in the project, within the <dependencies> block.
// -- (It will automatically install the driver)

// -- https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html for example SQL statements
public class DBaseConnectionP {

    // -- objects to be used for user database access
    private Connection userConnection = null;
    private Statement userStatement = null;
    private ResultSet resultset = null;
    private String[] usernames;
    private String[] passwords;
    private String[] emails;
    private int[] activeStatus;
    private int[] lockedOutStatus;
    private int[] loginAttempts;
    private int connectionId = -1;
    private static int connections = 0;

    private final String accounts = "accounts";

    // -- Database credentials are provided via environment variables
    // -- Ex(Your_DB_Username, Your_DB_Password)
    private String userdatabaseURL = "jdbc:mysql://localhost:3306/csc335?useSSL=false";
    private String first = "jdbc:mysql://";
    private String second = ":3306/csc335?useSSL=false";

    // -- The following are meant to be the credentials used for logging into MySQL
    private String user = "Your_DB_Username";
    private String password = "Your_DB_Password";

    public DBaseConnectionP() {
        String sqlcmd;

        // -- connect to the database
        try {
            userConnection = DriverManager.getConnection(userdatabaseURL, user, password);

            // -- These will be used to send queries to the database
            userStatement = userConnection.createStatement();

            // -- Simple SQL strings as they would be typed into the workbench
            sqlcmd = "SELECT VERSION()";
            resultset = userStatement.executeQuery(sqlcmd);

            if (resultset.next()) {
                System.out.println("MySQL Version: " + resultset.getString(1));
            }

            updatelists();

        } catch (SQLException ex) {
            // -- handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }


    }


    public DBaseConnectionP(String ip) {
        String sqlcmd;

        // -- connect to the database
        try {
            userConnection = DriverManager.getConnection(first + ip + second, user, password);

            // -- These will be used to send queries to the database
            userStatement = userConnection.createStatement();

            // -- simple SQL strings as they would be typed into the workbench
            sqlcmd = "SELECT VERSION()";
            resultset = userStatement.executeQuery(sqlcmd);

            if (resultset.next()) {
                System.out.println("MySQL Version: " + resultset.getString(1));
            }

            try {
                String insertSql = "INSERT INTO connections () VALUES ()";

                try (PreparedStatement pstmt =
                             userConnection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

                    pstmt.executeUpdate();

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            connectionId = generatedKeys.getInt(1);
                            System.out.println("Connection ID: " + connectionId);
                        }
                    }
                }
            } catch (SQLException ex) {
                System.out.println("SQLException while adding connection: " + ex.getMessage());
            }

            updatelists();

        } catch (SQLException ex) {
            // -- handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }


    }

    public int getConnections() {
        return connections;
    }

    public void close() {
        try {
            // -- Remove client's connection row while the database
            // -- connection is still open.
            endConnectionToServer();

            if (resultset != null && !resultset.isClosed()) {
                resultset.close();
            }

            if (userStatement != null && !userStatement.isClosed()) {
                userStatement.close();
            }

            if (userConnection != null && !userConnection.isClosed()) {
                userConnection.close();
                connections = Math.max(0, connections - 1);
            }

        } catch (SQLException ex) {
            System.out.println("SQLException during close: " + ex.getMessage());
        } finally {
            resultset = null;
            userStatement = null;
            userConnection = null;
        }
    }

    public boolean Register(String username, String password, String email) throws Exception {
        if (username == null || password == null || email == null) {
            return false;
        }

        try {
            // -- Check whether the username already exists in the cached list.
            if (usernames != null) {
                for (String existingUsername : usernames) {
                    if (existingUsername != null &&
                            existingUsername.equalsIgnoreCase(username)) {
                        return false;
                    }
                }
            }

            // -- Validate the password.
            if (!PasswordAuthenticator.validPassword(password)) {
                return false;
            }

            // -- Validate the email address.
            if (!AuthenticationService.validateEmailAddress(email)) {
                return false;
            }

            // -- Insert the account using parameters instead of SQL concatenation.
            String sqlcmd =
                    "INSERT INTO " + accounts +
                            " (username, password, email, active, lockedout, attempts) " +
                            "VALUES (?, ?, ?, 0, 0, 0)";

            try (PreparedStatement pstmt =
                         userConnection.prepareStatement(sqlcmd)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, email);

                pstmt.executeUpdate();
            }

            // -- Refresh the cached account lists.
            updatelists();

            System.out.println("User registered successfully.");
            return true;

        } catch (SQLException ex) {
            // -- MySQL error 1062 indicates a duplicate-key violation.
            if (ex.getErrorCode() == 1062) {
                System.out.println("Registration failed: username or email already exists.");
                return false;
            }

            System.err.println("Database error during registration: "
                    + ex.getMessage());
            return false;
        }
    }


    public String[] getUsernames() {
        List<String> usernamesA = new ArrayList<>();
        String sql = "SELECT username FROM " + accounts;

        try (PreparedStatement pstmt = userConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                usernamesA.add(rs.getString("username"));
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving usernames: " + e.getMessage());
        }

        return usernamesA.toArray(new String[0]);
    }

    public int getconnections() {
        String sql = "SELECT COUNT(*) AS cnt FROM connections;";
        try {
            resultset = userStatement.executeQuery(sql);
            if (resultset.next()) {
                return resultset.getInt("cnt");
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }
        return 0;
    }

    private String[] getEmails() {
        List<String> emailsL = new ArrayList<>();
        String sql = "SELECT email FROM " + accounts;

        try (PreparedStatement pstmt = userConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                emailsL.add(rs.getString("email"));
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving emails: " + e.getMessage());
        }

        return emailsL.toArray(new String[0]);
    }

    private String[] getPasswords() {
        List<String> passwordsL = new ArrayList<>();
        String sql = "SELECT password FROM " + accounts;

        try (PreparedStatement pstmt = userConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                passwordsL.add(rs.getString("password"));
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving passwords: " + e.getMessage());
        }

        return passwordsL.toArray(new String[0]);
    }

    private int[] getActiveStatus() {
        List<Integer> activeStatusL = new ArrayList<>();
        String sql = "SELECT active FROM " + accounts;

        try (PreparedStatement pstmt = userConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                activeStatusL.add(rs.getInt("active"));
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving active statuses: " + e.getMessage());
        }

        return activeStatusL.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] getLockedOutStatus() {
        List<Integer> lockedOutStatusL = new ArrayList<>();
        String sql = "SELECT lockedout FROM " + accounts;

        try (PreparedStatement pstmt = userConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                lockedOutStatusL.add(rs.getInt("lockedout"));
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving locked-out statuses: " + e.getMessage());
        }

        return lockedOutStatusL.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] getLoginAttempts() {
        List<Integer> loginAttemptsL = new ArrayList<>();
        String sql = "SELECT attempts FROM " + accounts;

        try (PreparedStatement pstmt = userConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                loginAttemptsL.add(rs.getInt("attempts"));
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving login attempts: " + e.getMessage());
        }

        return loginAttemptsL.stream().mapToInt(Integer::intValue).toArray();
    }

    public String[] getLockedOutUsers() {
        updatelists();
        List<String> lockedUsers = new ArrayList<>();
        if (usernames == null || lockedOutStatus == null) {
            return new String[0];
        }
        int len = Math.min(usernames.length, lockedOutStatus.length);
        for (int i = 0; i < len; i++) {
            if (lockedOutStatus[i] == 1) {
                lockedUsers.add(usernames[i]);
            }
        }
        return lockedUsers.toArray(new String[0]);
    }

    public void updatelists() {
        String sql = "SELECT username, password, email, active, lockedout, attempts FROM " + accounts + ";";

        // -- Create a scrollable statement so we can count rows safely without executing two queries
        try (Statement stmt = userConnection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery(sql)) {

            // -- 1. Get total number of rows (will correctly be 0 if the database is empty)
            int totalRows = 0;
            if (rs.last()) {
                totalRows = rs.getRow();
                rs.beforeFirst(); // -- Move cursor back to the beginning to parse rows
            }

            // -- 2. INITIALIZATION BLOCKS: Keeps the array type structural layout but blocks 'null' values
            this.usernames = new String[totalRows];
            this.passwords = new String[totalRows];
            this.emails = new String[totalRows];
            this.activeStatus = new int[totalRows];
            this.lockedOutStatus = new int[totalRows];
            this.loginAttempts = new int[totalRows];

            // -- 3. Populate array indices (this loop will naturally skip if totalRows is 0)
            int i = 0;
            while (rs.next()) {
                this.usernames[i] = rs.getString("username");
                this.passwords[i] = rs.getString("password");
                this.emails[i] = rs.getString("email");
                this.activeStatus[i] = rs.getInt("active");
                this.lockedOutStatus[i] = rs.getInt("lockedout");
                this.loginAttempts[i] = rs.getInt("attempts");
                i++;
            }

        } catch (SQLException e) {
            System.out.println("SQL connection error while building application lists: " + e.getMessage());
            // Fallback catch to guarantee components never read arrays as completely null
            ensureZeroLengthArrays();
        }
    }

    // Emergency safety fallback method
    private void ensureZeroLengthArrays() {
        if (this.usernames == null) {
            this.usernames = new String[0];
        }
        if (this.passwords == null) {
            this.passwords = new String[0];
        }
        if (this.emails == null) {
            this.emails = new String[0];
        }
        if (this.activeStatus == null) {
            this.activeStatus = new int[0];
        }
        if (this.lockedOutStatus == null) {
            this.lockedOutStatus = new int[0];
        }
        if (this.loginAttempts == null) {
            this.loginAttempts = new int[0];
        }
    }

    private int getUserIndex(String username) {
        for (int i = 0; i < usernames.length; i++) {
            if (usernames[i].equals(username)) {
                return i;
            }
        }
        return -1; // -- User not found
    }

    public void sendEmail(String username) {
        String userEmail = null;
        int index = getUserIndex(username);
        // -- Find the email associated with the username
        userEmail = emails[index];
        String pass = passwords[index];
        try {
            resetLoginAttempts(username);
            unlockAccount(username);
            updatelists();
            SendEmailUsingGMailSMTP.sendMail(userEmail, pass);
        } catch (Exception e) {
            System.out.println("Username not found!");
        }


    }

    private void resetLoginAttempts(String username) {
        String sqlcmd =
                "UPDATE " + accounts +
                        " SET attempts = 0 WHERE username = ?";

        try (PreparedStatement pstmt =
                     userConnection.prepareStatement(sqlcmd)) {
            pstmt.setString(1, username);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Login attempts reset successfully.");
            } else {
                System.out.println("No account found to reset attempts for.");
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Database error resetting login attempts: "
                            + ex.getMessage());
        }
    }

    public boolean accountStatus(String username) {
        String sqlcmd;
        try {
            sqlcmd = "SELECT active FROM " + accounts + " WHERE username = '" + username + "';";
            resultset = userStatement.executeQuery(sqlcmd);
            if (resultset.next()) {
                int active = resultset.getInt("active");
                return active == 1;
            }
        } catch (SQLException ex) {
            // -- handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        return false;
    }

    private void activateAccount(String username) {
        String sqlcmd =
                "UPDATE " + accounts +
                        " SET active = 1 WHERE username = ?";

        try (PreparedStatement pstmt =
                     userConnection.prepareStatement(sqlcmd)) {
            pstmt.setString(1, username);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Account activated successfully.");
            } else {
                System.out.println("No account found to activate.");
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Database error while activating account: "
                            + ex.getMessage());
        }
    }

    private void deactivateAccount(String username) {
        String sqlcmd =
                "UPDATE " + accounts +
                        " SET active = 0 WHERE username = ?";

        try (PreparedStatement pstmt =
                     userConnection.prepareStatement(sqlcmd)) {
            pstmt.setString(1, username);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Account deactivated successfully.");
            } else {
                System.out.println("No account found to deactivate.");
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Database error while deactivating account: "
                            + ex.getMessage());
        }
    }


    public void lockAccount(String username) {
        String sqlcmd =
                "UPDATE " + accounts +
                        " SET lockedout = 1 WHERE username = ?";

        try (PreparedStatement pstmt =
                     userConnection.prepareStatement(sqlcmd)) {
            pstmt.setString(1, username);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Account locked successfully.");
            } else {
                System.out.println("No account found to lock.");
            }

        } catch (SQLException ex) {
            System.err.println(
                    "Database error while locking account: "
                            + ex.getMessage());
        }
    }

    public boolean login(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        int userIndex = getUserIndex(username);

        if (userIndex == -1) {
            return false;
        }

        try {
            // -- Check the account's current lockout status.
            if (isLocked(username)) {
                System.out.println("Account is locked out.");
                return false;
            }

            boolean successfulLogin =
                    AuthenticationService.authenticate(
                            username, password, usernames, passwords);

            if (successfulLogin) {
                resetLoginAttempts(username);
                activateAccount(username);
                updatelists();
                return true;
            }

            // -- Increment failed login attempts safely.
            String sqlcmd =
                    "UPDATE " + accounts +
                            " SET attempts = attempts + 1 WHERE username = ?";

            try (PreparedStatement pstmt =
                         userConnection.prepareStatement(sqlcmd)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }

            // -- Refresh the cached attempt count before checking the threshold.
            updatelists();

            userIndex = getUserIndex(username);

            if (userIndex != -1 && loginAttempts[userIndex] >= 3) {
                lockAccount(username);
                updatelists();

                System.out.println(
                        "Account locked due to multiple failed login attempts.");
            }

            return false;

        } catch (SQLException ex) {
            System.err.println(
                    "Database error during login: " + ex.getMessage());
            return false;
        }
    }

    public void logout(String username) {
        deactivateAccount(username);
        updatelists();
    }

    private void unlockAccount(String username) {
        String sqlcmd =
                "UPDATE " + accounts +
                        " SET lockedout = 0 WHERE username = ?";

        try (PreparedStatement pstmt =
                     userConnection.prepareStatement(sqlcmd)) {

            pstmt.setString(1, username);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Account unlocked successfully.");
            } else {
                System.out.println("No account found to unlock.");
            }

            updatelists();

        } catch (SQLException ex) {
            System.err.println(
                    "Database error while unlocking account: "
                            + ex.getMessage());
        }
    }

    public boolean changePassword(String username, String newPassword) {
        String sqlcmd;
        try {
            if (!(PasswordAuthenticator.validPassword(newPassword))) {
                System.out.println("Password is too weak. It must be at least 8 characters long and include uppercase letters, lowercase letters, and digits.");
                return false;
            }
            sqlcmd = "UPDATE " + accounts + " SET password = '" + newPassword + "' WHERE username = '" + username + "';";
            userStatement.executeUpdate(sqlcmd);
            System.out.println("Password changed successfully.");
            updatelists();
            return true;
        } catch (SQLException ex) {
            // -- handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return false;
        }
    }

    public String[] getActiveUsers() {
        List<String> activeUsers = new ArrayList<>();
        for (int i = 0; i < usernames.length; i++) {
            if (activeStatus[i] == 1) {
                activeUsers.add(usernames[i]);
            }
        }
        return activeUsers.toArray(new String[0]);
    }

    public boolean isLocked(String username) {
        return AuthenticationService.isLockedOut(username, usernames, lockedOutStatus);
    }

    public void processUserDatabase() {
        String sqlcmd;

        try {
            // -- A query will return a ResultSet
            sqlcmd = "SELECT * FROM " + accounts + ";";
            resultset = userStatement.executeQuery(sqlcmd);

            // -- The metadata tells us how many columns in the data
            System.out.println("Table Columns:");
            ResultSetMetaData rsmd = resultset.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
            for (int i = 1; i <= numberOfColumns; ++i) {
                System.out.print(rsmd.getColumnLabel(i) + "\t");
            }
            System.out.println();

            // -- loop through the ResultSet one row at a time
            // -- Note that the ResultSet starts at index 1
            while (resultset.next()) {
                // -- loop through the columns of the ResultSet
                for (int i = 1; i <= numberOfColumns; ++i) {
                    System.out.print(resultset.getString(i) + "\t\t");
                }
                System.out.println();
            }

            String value = "";
            int primarykey = 1;

            // -- Insert some records (no resultset is returned on updates)
            primarykey = 0;
            value = "zero";
            sqlcmd = "INSERT INTO " + accounts + " VALUES('" + primarykey + "', '" + value + "', )";

            sqlcmd = "INSERT INTO " + accounts + "VALUES ('', 'test', 'abcd1234', 'test@gmail.com', '0')";
            userStatement.executeUpdate(sqlcmd);

            primarykey = 1;
            value = "one";
            sqlcmd = "INSERT INTO " + accounts + " VALUES('" + primarykey + "', '" + value + "')";
            userStatement.executeUpdate(sqlcmd);

            primarykey = 2;
            value = "too";
            sqlcmd = "INSERT INTO " + accounts + " VALUES('" + primarykey + "', '" + value + "')";
            userStatement.executeUpdate(sqlcmd);


            // -- Select a specific record
            value = "one";
            sqlcmd = "SELECT * FROM " + accounts + " WHERE value = '" + value + "'";
            resultset = userStatement.executeQuery(sqlcmd);
            // -- Loop through the ResultSet one row at a time
            // -- Note that the ResultSet starts at index 1
            while (resultset.next()) {
                // -- Loop through the columns of the ResultSet
                for (int i = 1; i <= numberOfColumns; ++i) {
                    System.out.print(resultset.getString(i) + "\t\t");
                }
                System.out.println();
            }

            System.out.println("=====================");
            sqlcmd = "SELECT * FROM " + accounts + ";";
            resultset = userStatement.executeQuery(sqlcmd);
            // -- Loop through the ResultSet one row at a time
            // -- Note that the ResultSet starts at index 1
            while (resultset.next()) {
                // -- Loop through the columns of the ResultSet
                for (int i = 1; i < numberOfColumns; ++i) {
                    System.out.print(resultset.getString(i) + "\t\t");
                }
                System.out.println(resultset.getString(numberOfColumns));
            }

            System.out.println("=====================");

            // -- Delete a record
            sqlcmd = "DELETE FROM " + accounts + " WHERE primarykey = " + 0 + ";";
            userStatement.executeUpdate(sqlcmd);

            // -- Update a record
            int two = 2;
            String stwo = "two";
            sqlcmd = "UPDATE accounts SET value = '" + stwo + "' WHERE (PrimaryKey = '" + two + "')";
            System.out.println(sqlcmd);
            userStatement.executeUpdate(sqlcmd);

            sqlcmd = "SELECT * FROM " + accounts + ";";
            resultset = userStatement.executeQuery(sqlcmd);
            // -- Loop through the ResultSet one row at a time
            // -- Note that the ResultSet starts at index 1
            while (resultset.next()) {
                // -- Loop through the columns of the ResultSet
                for (int i = 1; i < numberOfColumns; ++i) {
                    System.out.print(resultset.getString(i) + "\t\t");
                }
                System.out.println(resultset.getString(numberOfColumns));
            }

        } catch (SQLException ex) {
            // -- handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }


    }

    public void endConnectionToServer() {
        if (connectionId == -1) {
            System.out.println("No connection ID to remove.");
            return;
        }

        String deleteSql = "DELETE FROM connections WHERE user = ?";

        try (PreparedStatement pstmt = userConnection.prepareStatement(deleteSql)) {
            pstmt.setInt(1, connectionId);

            int removed = pstmt.executeUpdate();

            System.out.println(
                    "Removed " + removed +
                            " connection row(s) for connection ID " + connectionId
            );

            if (removed > 0) {
                connectionId = -1;
            }

        } catch (SQLException ex) {
            System.out.println("SQLException while removing connection: " + ex.getMessage());
        }
    }

    public void removeConnection() {
        endConnectionToServer();
    }

    public int getSchemaConnections() {
        String sql = "SELECT COUNT(*) AS conn_count FROM information_schema.processlist WHERE db = 'csc335';";
        try {
            resultset = userStatement.executeQuery(sql);
            if (resultset.next()) {
                return resultset.getInt("conn_count");
            }
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }
        return 0;
    }
}

