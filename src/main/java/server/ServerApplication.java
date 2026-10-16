package server;

import Misc.DBaseConnectionP;
import java.awt.*;
import java.io.Serializable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ServerApplication extends JFrame implements Runnable, Serializable {

    private static final long serialVersionUID = -8776438726683578403L;

    private JTable table;
    private DefaultTableModel tableModel;
    private JButton queryButton;
    private JScrollPane tableScroll;
    private java.util.concurrent.ScheduledExecutorService scheduler;
    private JButton backButton;
    private int i = 0;
    private final Color backgroundCol = new Color(182, 212, 214);  // light teal
    private final Color sidePanelCol  = new Color(60, 60, 60);     // dark grey
    private String[] users;
    private String[] registered;
    private String[] locked;
    private boolean running = true;
    private String[] cols = {
            "Registered Users",
            "Logged-In Users",
            "Locked-Out Users",
    };
    private JLabel connections = new JLabel("Number of Connections: ", SwingConstants.CENTER);
    private int number = 0;

    public ServerApplication(String UsernameString, DBaseConnectionP dbc) {
        super();

        try {

            this.setTitle("Server Control Panel");
            this.setSize(1100, 700);
            this.setLocationRelativeTo(null);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setLayout(null);
            getContentPane().setBackground(backgroundCol);

            // -- Side panel
            JPanel leftPanel = new JPanel();
            leftPanel.setBackground(sidePanelCol);
            leftPanel.setBounds(0, 0, 230, 700);
            leftPanel.setLayout(null);
            // -- leftPanel.add(backButton)
            this.add(leftPanel);

            // -- Query button
            queryButton = new JButton("[ ? ]  Query");
            queryButton.setBounds(40, 220, 150, 40);
            // -- leftPanel.add(queryButton);

            backButton = new JButton("[ <- ]  Back");
            backButton.setBounds(40, 30, 150, 40);
            leftPanel.add(backButton);

            backButton.addActionListener(e -> {
                scheduler.shutdownNow();
                this.dispose();

                new client.ClientApp(UsernameString, dbc);
            });
            // -- Title
            JLabel title = new JLabel("Admin Control Panel", SwingConstants.CENTER);
            title.setForeground(Color.BLACK);
            title.setFont(new Font("Arial", Font.BOLD, 48));
            title.setBounds(230, 40, 850, 60);
            this.add(title);

            connections.setForeground(Color.BLACK);
            connections.setFont(new Font("Arial", Font.BOLD, 24));

            connections.setBounds(250, 110, 425, 30);

            this.add(connections);

            // -- Get a list of users from the database of active sessions
            users = dbc.getActiveUsers();
            tableModel = new DefaultTableModel(cols, 100);
            table = new JTable(tableModel);
            table.setRowHeight(35);
            table.setFont(new Font("Arial", Font.PLAIN, 16));


            tableScroll = new JScrollPane(table);
            tableScroll.setBounds(260, 160, 780, 420);
            tableScroll.setVisible(true);       // hidden till query
            this.add(tableScroll);

            scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    dbc.updatelists();
                    users = dbc.getActiveUsers();
                    registered = dbc.getUsernames();
                    locked = dbc.getLockedOutUsers();
                    // -- Perform GUI update on the EDT
                    javax.swing.SwingUtilities.invokeLater(this::run);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);

            number = dbc.getconnections();

                // TODO – get real data from:
                // UserDatabase, AuthenticationService, ServerApplication sessions
                dbc.updatelists();
                users = dbc.getActiveUsers();
                registered = dbc.getUsernames();
                locked = dbc.getLockedOutUsers();
                number = dbc.getconnections();

            this.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    scheduler.shutdownNow();
                    dbc.logout(UsernameString);
                    dbc.endConnectionToServer();
                }
            });
            this.setVisible(true);

        } catch (Exception e) {
        }
    }

    public void run(){
        int regLen = (registered == null) ? 0 : registered.length;
        int usersLen = (users == null) ? 0 : users.length;
        int lockedLen = (locked == null) ? 0 : locked.length;
        int rows = Math.max(Math.max(regLen, Math.max(usersLen, lockedLen)), 1);

        // -- Preserve existing column names
        java.util.Vector<String> colNames = new java.util.Vector<>();
        for (int c = 0; c < cols.length; c++) {
            String column = cols[c];
            if (c == 1) {
                colNames.add(column + ": " + usersLen);
            } else {
                colNames.add(column);
            }
        }
        DefaultTableModel model = new DefaultTableModel(colNames, rows);

        for (int row = 0; row < rows; row++) {
            if (row < regLen) {
                model.setValueAt(registered[row], row, 0);
            }
            if (row < usersLen) {
                model.setValueAt(users[row], row, 1);
            }

            if (row < lockedLen) {
                model.setValueAt(locked[row], row, 2);
            }
        }
        connections.setText("Number of Connections: " + number);
        table.setModel(model);
        tableModel = model;
    }
    private int queryThreadsConnected(java.sql.Connection conn) {
        if (conn == null) return 0;
        try (java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SHOW STATUS LIKE 'Threads_connected'")) {
            if (rs.next()) {
                // -- Value column name can be "Value" or index 2
                try {
                    return rs.getInt("Value");
                } catch (java.sql.SQLException ex) {
                    return rs.getInt(2);
                }
            }
        } catch (java.sql.SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private void refreshConnectionCount(DBaseConnectionP dbc) {
        try {
            number = dbc.getconnections();
            return;
        } catch (Throwable ignored) {
        }

        // -- Fallback: try to obtain a java.sql.Connection from dbc via reflection and query SHOW STATUS
        try {
            java.lang.reflect.Method m = dbc.getClass().getMethod("getConnection");
            Object connObj = m.invoke(dbc);
            if (connObj instanceof java.sql.Connection) {
                number = queryThreadsConnected((java.sql.Connection) connObj);
                return;
            }
        } catch (Throwable ignored) {
        }

        // Final fallback: try INFORMATION_SCHEMA processlist count (if getConnection provided)
        try {
            java.lang.reflect.Method m2 = dbc.getClass().getMethod("getConnection");
            Object connObj = m2.invoke(dbc);
            if (connObj instanceof java.sql.Connection) {
                try (java.sql.Statement st = ((java.sql.Connection) connObj).createStatement();
                     java.sql.ResultSet rs = st.executeQuery(
                             "SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.PROCESSLIST")) {
                    if (rs.next()) number = rs.getInt("c");
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}