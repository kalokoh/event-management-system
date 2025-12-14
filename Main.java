import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;

public class Main extends JFrame {
    private Connection conn;
    private JTable eventTable;
    private DefaultTableModel tableModel;
    private String currentUser;

    // UI constants
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Color PRIMARY_COLOR = new Color(0, 0, 0);
    private static final Color BG_COLOR = new Color(245, 247, 250);

    // logo
    ImageIcon logo = new ImageIcon("src/logo.jpeg");

    public Main() {
        initDatabase();
        showLoginDialog();
    }

    // database connection
    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:university_events.db");
            createTables();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = conn.createStatement();

        stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL)
                """);

        stmt.execute("""
                CREATE TABLE IF NOT EXISTS events (
                event_id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_name TEXT NOT NULL,
                event_date TEXT NOT NULL,
                venue TEXT NOT NULL,
                organizer TEXT NOT NULL)
                """);

        stmt.execute("""
                CREATE TABLE IF NOT EXISTS participants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id INTEGER,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                FOREIGN KEY(event_id) REFERENCES events(event_id))
                """);

        PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO users (username, password) VALUES (?, ?)");
        ps.setString(1, "kalokoh");
        ps.setString(2, "kalokoh");
        ps.execute();

        stmt.close();
    }

    private void showLoginDialog() {
        JDialog loginDialog = new JDialog(this, "Login", true);
        loginDialog.setSize(350, 230);
        loginDialog.setLayout(new GridBagLayout());
        loginDialog.setLocationRelativeTo(null);
        loginDialog.getContentPane().setBackground(new Color(245, 247, 250));
        loginDialog.setIconImage(logo.getImage());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);

        JButton loginBtn = new JButton("Login");

        Font font = new Font("Segoe UI", Font.PLAIN, 14);
        userLabel.setFont(font);
        passLabel.setFont(font);
        userField.setFont(font);
        passField.setFont(font);
        loginBtn.setFont(font);

        loginBtn.setBackground(new Color(33, 150, 243));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginDialog.add(userLabel, gbc);

        gbc.gridx = 1;
        loginDialog.add(userField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        loginDialog.add(passLabel, gbc);

        gbc.gridx = 1;
        loginDialog.add(passField, gbc);

        // Login Button (centered)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginDialog.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            try {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM users WHERE username = ? AND password = ?");
                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    currentUser = username;
                    loginDialog.dispose();
                    initializeMainUI();
                    setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(loginDialog,
                            "Invalid username or password",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(loginDialog, ex.getMessage());
            }
        });

        loginDialog.setVisible(true);
    }


    // main ui
    private void initializeMainUI() {
        setTitle("Limkokwing University Event Management System");
        setSize(1100, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(logo.getImage());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("Limkokwing University Event Management System");
        title.setFont(TITLE_FONT);
        title.setForeground(Color.WHITE);

        JLabel user = new JLabel("Logged in as: " + currentUser);
        user.setForeground(Color.WHITE);
        user.setFont(NORMAL_FONT);

        header.add(title, BorderLayout.WEST);
        header.add(user, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"ID", "Name", "Date", "Venue", "Organizer", "Participants"};
        tableModel = new DefaultTableModel(cols, 0);
        eventTable = new JTable(tableModel);
        eventTable.setFont(NORMAL_FONT);
        eventTable.setRowHeight(28);

        JTableHeader th = eventTable.getTableHeader();
        th.setFont(NORMAL_FONT);
        th.setBackground(new Color(220, 220, 220));

        add(new JScrollPane(eventTable), BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(BG_COLOR);

        JButton add = styledButton("Add Event");
        JButton update = styledButton("Update");
        JButton delete = styledButton("Delete");
        JButton register = styledButton("Register Participant");
        JButton report = styledButton("Generate Report");
        JButton refresh = styledButton("Refresh");

        add.addActionListener(e -> addEvent());
        update.addActionListener(e -> updateEvent());
        delete.addActionListener(e -> deleteEvent());
        register.addActionListener(e -> registerParticipant());
        report.addActionListener(e -> generateReport());
        refresh.addActionListener(e -> loadEvents());

        btnPanel.add(add);
        btnPanel.add(update);
        btnPanel.add(delete);
        btnPanel.add(register);
        btnPanel.add(report);
        btnPanel.add(refresh);

        add(btnPanel, BorderLayout.SOUTH);

        loadEvents();
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY_COLOR);
        b.setForeground(Color.WHITE);
        b.setFont(NORMAL_FONT);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 15, 8, 15));
        return b;
    }

    // CRUD operation
    private void loadEvents() {
        tableModel.setRowCount(0);
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT e.*, COUNT(p.id) AS total FROM events e " +
                            "LEFT JOIN participants p ON e.event_id=p.event_id GROUP BY e.event_id");
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("event_id"),
                        rs.getString("event_name"),
                        rs.getString("event_date"),
                        rs.getString("venue"),
                        rs.getString("organizer"),
                        rs.getInt("total")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void addEvent() {
        JTextField n = new JTextField();
        JTextField d = new JTextField();
        JTextField v = new JTextField();
        JTextField o = new JTextField();

        Object[] fields = {
                "Name:", n, "Date (YYYY-MM-DD):", d, "Venue:", v, "Organizer:", o
        };

        if (JOptionPane.showConfirmDialog(this, fields, "Add Event",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO events VALUES(NULL,?,?,?,?)");
                ps.setString(1, n.getText());
                ps.setString(2, d.getText());
                ps.setString(3, v.getText());
                ps.setString(4, o.getText());
                ps.execute();
                loadEvents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    private void updateEvent() {
        int row = eventTable.getSelectedRow();
        if (row == -1) return;

        int id = (int) tableModel.getValueAt(row, 0);

        JTextField n = new JTextField(tableModel.getValueAt(row,1).toString());
        JTextField d = new JTextField(tableModel.getValueAt(row,2).toString());
        JTextField v = new JTextField(tableModel.getValueAt(row,3).toString());
        JTextField o = new JTextField(tableModel.getValueAt(row,4).toString());

        Object[] fields = {"Name:", n, "Date:", d, "Venue:", v, "Organizer:", o};

        if (JOptionPane.showConfirmDialog(this, fields, "Update Event",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE events SET event_name=?, event_date=?, venue=?, organizer=? WHERE event_id=?");
                ps.setString(1, n.getText());
                ps.setString(2, d.getText());
                ps.setString(3, v.getText());
                ps.setString(4, o.getText());
                ps.setInt(5, id);
                ps.execute();
                loadEvents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    private void deleteEvent() {
        int row = eventTable.getSelectedRow();
        if (row == -1) return;

        int id = (int) tableModel.getValueAt(row, 0);
        try {
            conn.prepareStatement("DELETE FROM participants WHERE event_id="+id).execute();
            conn.prepareStatement("DELETE FROM events WHERE event_id="+id).execute();
            loadEvents();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void registerParticipant() {
        int row = eventTable.getSelectedRow();
        if (row == -1) return;

        JTextField name = new JTextField();
        JComboBox<String> type = new JComboBox<>(new String[]{"Student","Staff"});

        Object[] fields = {"Name:", name, "Type:", type};

        if (JOptionPane.showConfirmDialog(this, fields, "Register Participant",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO participants VALUES(NULL,?,?,?)");
                ps.setInt(1, (int)tableModel.getValueAt(row,0));
                ps.setString(2, name.getText());
                ps.setString(3, type.getSelectedItem().toString());
                ps.execute();
                loadEvents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }

    private void generateReport() {
        JDialog dialog = new JDialog(this, "Detailed Event Report", true);
        dialog.setSize(900, 600);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        reportArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(reportArea);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton printBtn = new JButton("Print Report");
        JButton closeBtn = new JButton("Close");

        bottomPanel.add(printBtn);
        bottomPanel.add(closeBtn);

        dialog.add(bottomPanel, BorderLayout.SOUTH);

        StringBuilder report = new StringBuilder();

        report.append("====================================================\n");
        report.append("   LIMKOKWING UNIVERSITY EVENT MANAGEMENT REPORT\n");
        report.append("====================================================\n");
        report.append("Generated By : ").append(currentUser).append("\n");
        report.append("Generated On : ").append(new java.util.Date()).append("\n\n");

        try {
            // Summary
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM events");
            int totalEvents = rs.next() ? rs.getInt(1) : 0;

            rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM participants");
            int totalParticipants = rs.next() ? rs.getInt(1) : 0;

            report.append("SUMMARY STATISTICS\n");
            report.append("----------------------------------------------------\n");
            report.append("Total Events        : ").append(totalEvents).append("\n");
            report.append("Total Participants : ").append(totalParticipants).append("\n");

            if (totalEvents > 0) {
                report.append("Average Participants/Event : ")
                        .append(totalParticipants / totalEvents).append("\n");
            }
            report.append("\n");

            // Upcoming Events
            report.append("UPCOMING EVENTS\n");
            report.append("----------------------------------------------------\n");

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM events WHERE event_date >= date('now') ORDER BY event_date");
            rs = ps.executeQuery();

            boolean hasUpcoming = false;
            while (rs.next()) {
                hasUpcoming = true;
                report.append("Event Name : ").append(rs.getString("event_name")).append("\n");
                report.append("Date       : ").append(rs.getString("event_date")).append("\n");
                report.append("Venue      : ").append(rs.getString("venue")).append("\n");
                report.append("Organizer  : ").append(rs.getString("organizer")).append("\n");
                report.append("----------------------------------------------------\n");
            }

            if (!hasUpcoming) {
                report.append("No upcoming events found.\n");
            }

            report.append("\nDETAILED EVENT BREAKDOWN\n");
            report.append("====================================================\n");

            ps = conn.prepareStatement("SELECT * FROM events ORDER BY event_date");
            rs = ps.executeQuery();

            while (rs.next()) {
                int eventId = rs.getInt("event_id");

                report.append("\nEvent: ").append(rs.getString("event_name")).append("\n");
                report.append("Date : ").append(rs.getString("event_date")).append("\n");
                report.append("Venue: ").append(rs.getString("venue")).append("\n");
                report.append("Organizer: ").append(rs.getString("organizer")).append("\n");

                PreparedStatement psCount = conn.prepareStatement(
                        "SELECT COUNT(*) FROM participants WHERE event_id=?");
                psCount.setInt(1, eventId);
                ResultSet countRs = psCount.executeQuery();
                int total = countRs.next() ? countRs.getInt(1) : 0;

                report.append("Total Participants: ").append(total).append("\n");

                PreparedStatement psType = conn.prepareStatement(
                        "SELECT type, COUNT(*) total FROM participants WHERE event_id=? GROUP BY type");
                psType.setInt(1, eventId);
                ResultSet typeRs = psType.executeQuery();

                while (typeRs.next()) {
                    report.append("  - ")
                            .append(typeRs.getString("type"))
                            .append(": ")
                            .append(typeRs.getInt("total"))
                            .append("\n");
                }

                PreparedStatement psNames = conn.prepareStatement(
                        "SELECT name, type FROM participants WHERE event_id=? ORDER BY name");
                psNames.setInt(1, eventId);
                ResultSet nameRs = psNames.executeQuery();

                if (total > 0) {
                    report.append("Participant List:\n");
                    while (nameRs.next()) {
                        report.append("   • ")
                                .append(nameRs.getString("name"))
                                .append(" (")
                                .append(nameRs.getString("type"))
                                .append(")\n");
                    }
                } else {
                    report.append("No participants registered.\n");
                }

                report.append("----------------------------------------------------\n");
            }

            reportArea.setText(report.toString());

        } catch (SQLException e) {
            reportArea.setText("Error generating report:\n" + e.getMessage());
        }

        // print feature
        printBtn.addActionListener(e -> {
            try {
                boolean printed = reportArea.print();
                if (printed) {
                    JOptionPane.showMessageDialog(dialog, "Report sent to printer successfully.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Printing failed: " + ex.getMessage());
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }


// run the program
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
