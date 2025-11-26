import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarRental {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/carrental?serverTimezone=UTC";
    private static final String DB_USER = "dbms";
    private static final String DB_PASS = "root";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                        "MySQL JDBC Driver not found. Add connector JAR to classpath.",
                        "Driver Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            CarDAO dao = new CarDAO(DB_URL, DB_USER, DB_PASS);
            MainFrame frame = new MainFrame(dao);
            frame.setVisible(true);
        });
    }

    // ----------------- Data model -----------------
    public static class Car {
        public int id;
        public String make;
        public String model;
        public int year;
        public double ratePerDay;
        public boolean available;

        public Car() {}

        public Car(int id, String make, String model, int year, double ratePerDay, boolean available) {
            this.id = id;
            this.make = make;
            this.model = model;
            this.year = year;
            this.ratePerDay = ratePerDay;
            this.available = available;
        }
    }

    // TODO: Add CarDAO and MainFrame classes below
    // ----------------- DAO (Database Access Object) -----------------
public static class CarDAO {
    private final String url, user, pass;

    public CarDAO(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    // Get all cars
    public List<Car> listCars() {
        List<Car> list = new ArrayList<>();
        String q = "SELECT id, make, model, year, rate_per_day, available FROM cars ORDER BY id";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Car(
                    rs.getInt("id"),
                    rs.getString("make"),
                    rs.getString("model"),
                    rs.getInt("year"),
                    rs.getDouble("rate_per_day"),
                    rs.getBoolean("available")
                ));
            }
        } catch (SQLException ex) {
            showError(ex);
        }
        return list;
    }

    // Add a car
    public Car createCar(Car car) {
        String q = "INSERT INTO cars (make, model, year, rate_per_day, available) VALUES (?,?,?,?,?)";
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, car.make);
            ps.setString(2, car.model);
            ps.setInt(3, car.year);
            ps.setDouble(4, car.ratePerDay);
            ps.setBoolean(5, car.available);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) car.id = keys.getInt(1);
            }
            return car;
        } catch (SQLException ex) {
            showError(ex);
            return null;
        }
    }

    // Update car
    public boolean updateCar(Car car) {
        String q = "UPDATE cars SET make=?, model=?, year=?, rate_per_day=?, available=? WHERE id=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setString(1, car.make);
            ps.setString(2, car.model);
            ps.setInt(3, car.year);
            ps.setDouble(4, car.ratePerDay);
            ps.setBoolean(5, car.available);
            ps.setInt(6, car.id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            showError(ex);
            return false;
        }
    }

    // Delete car
    public boolean deleteCar(int id) {
        String q = "DELETE FROM cars WHERE id=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            showError(ex);
            return false;
        }
    }

    // Change availability (rent/return)
    public boolean setAvailability(int id, boolean avail) {
        String q = "UPDATE cars SET available=? WHERE id=?";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setBoolean(1, avail);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            showError(ex);
            return false;
        }
    }

    private void showError(SQLException ex) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE)
        );
    }
}
// ----------------- GUI -----------------
// ----------------- BEAUTIFIED GUI -----------------
public static class MainFrame extends JFrame {
    private final CarDAO dao;
    private final DefaultTableModel tableModel;
    private final JTable table;

    private final JTextField tfMake = new JTextField(12);
    private final JTextField tfModel = new JTextField(12);
    private final JTextField tfYear = new JTextField(6);
    private final JTextField tfRate = new JTextField(8);
    private final JCheckBox cbAvailable = new JCheckBox("Available", true);

    private int selectedId = -1;

    public MainFrame(CarDAO dao) {
        super("Car Rental Management System");
        this.dao = dao;

        // ===== WINDOW SETTINGS =====
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ===== GLOBAL UI STYLE =====
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));

        getContentPane().setBackground(new Color(245, 248, 250));

        // ===== TITLE BAR =====
        JLabel title = new JLabel("Car Rental Management System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(25, 42, 86));
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(title, BorderLayout.NORTH);

        // ===== TABLE =====
        tableModel = new DefaultTableModel(new String[]{"ID", "Make", "Model", "Year", "Rate/Day", "Available"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(93, 173, 226));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(93, 173, 226));
        header.setForeground(Color.WHITE);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== FORM PANEL =====
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(245, 248, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lblMake = new JLabel("Make:");
        JLabel lblModel = new JLabel("Model:");
        JLabel lblYear = new JLabel("Year:");
        JLabel lblRate = new JLabel("Rate / Day:");
        JLabel lblAvail = new JLabel("Status:");

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(lblMake, gbc);
        gbc.gridx = 1; formPanel.add(tfMake, gbc);
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(lblModel, gbc);
        gbc.gridx = 1; formPanel.add(tfModel, gbc);
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(lblYear, gbc);
        gbc.gridx = 1; formPanel.add(tfYear, gbc);
        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(lblRate, gbc);
        gbc.gridx = 1; formPanel.add(tfRate, gbc);
        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(lblAvail, gbc);
        gbc.gridx = 1; formPanel.add(cbAvailable, gbc);

        // ===== BUTTON PANEL =====
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        btnPanel.setBackground(new Color(245, 248, 250));

        JButton bAdd = createButton("Add", new Color(39, 174, 96));
        JButton bUpdate = createButton("Update", new Color(41, 128, 185));
        JButton bDelete = createButton("Delete", new Color(192, 57, 43));
        JButton bRefresh = createButton("Refresh", new Color(142, 68, 173));
        JButton bRent = createButton("Rent", new Color(243, 156, 18));
        JButton bReturn = createButton("Return", new Color(52, 152, 219));

        btnPanel.add(bAdd);
        btnPanel.add(bUpdate);
        btnPanel.add(bDelete);
        btnPanel.add(bRefresh);
        btnPanel.add(bRent);
        btnPanel.add(bReturn);

        // ===== RIGHT PANEL =====
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.add(formPanel, BorderLayout.NORTH);
        rightPanel.add(btnPanel, BorderLayout.CENTER);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.setBackground(new Color(245, 248, 250));

        add(sp, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // ===== EVENT LISTENERS =====
        table.getSelectionModel().addListSelectionListener(this::onTableSelect);

        bRefresh.addActionListener(e -> refreshTable());
        bAdd.addActionListener(e -> addCar());
        bUpdate.addActionListener(e -> updateCar());
        bDelete.addActionListener(e -> deleteCar());
        bRent.addActionListener(e -> rentCar());
        bReturn.addActionListener(e -> returnCar());

        // Load data
        refreshTable();
    }

    // ----- Helper to create styled buttons -----
    private JButton createButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b.setBackground(color.darker());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b.setBackground(color);
            }
        });
        return b;
    }

    // ======= Existing methods =======
    private void onTableSelect(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int r = table.getSelectedRow();
        if (r == -1) {
            clearForm();
            selectedId = -1;
            return;
        }
        selectedId = Integer.parseInt(String.valueOf(tableModel.getValueAt(r, 0)));
        tfMake.setText(String.valueOf(tableModel.getValueAt(r, 1)));
        tfModel.setText(String.valueOf(tableModel.getValueAt(r, 2)));
        tfYear.setText(String.valueOf(tableModel.getValueAt(r, 3)));
        tfRate.setText(String.valueOf(tableModel.getValueAt(r, 4)));
        cbAvailable.setSelected(Boolean.parseBoolean(String.valueOf(tableModel.getValueAt(r, 5))));
    }

    private void refreshTable() {
        SwingWorker<List<Car>, Void> w = new SwingWorker<>() {
            @Override protected List<Car> doInBackground() { return dao.listCars(); }
            @Override protected void done() {
                try {
                    List<Car> cars = get();
                    tableModel.setRowCount(0);
                    for (Car c : cars) {
                        tableModel.addRow(new Object[]{c.id, c.make, c.model, c.year, c.ratePerDay, c.available});
                    }
                    clearForm();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void addCar() {
        try {
            Car c = readForm();
            Car created = dao.createCar(c);
            if (created != null) refreshTable();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateCar() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Select a car first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Car c = readForm();
            c.id = selectedId;
            if (dao.updateCar(c)) refreshTable();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteCar() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Select a car first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int ans = JOptionPane.showConfirmDialog(this, "Delete selected car?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;
        if (dao.deleteCar(selectedId)) refreshTable();
    }

    private void rentCar() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Select a car first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!cbAvailable.isSelected()) {
            JOptionPane.showMessageDialog(this, "Car already rented.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (dao.setAvailability(selectedId, false)) refreshTable();
    }

    private void returnCar() {
        if (selectedId == -1) {
            JOptionPane.showMessageDialog(this, "Select a car first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (cbAvailable.isSelected()) {
            JOptionPane.showMessageDialog(this, "Car is already available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (dao.setAvailability(selectedId, true)) refreshTable();
    }

    private Car readForm() {
        String make = tfMake.getText().trim();
        String model = tfModel.getText().trim();
        String y = tfYear.getText().trim();
        String r = tfRate.getText().trim();
        boolean avail = cbAvailable.isSelected();

        if (make.isEmpty() || model.isEmpty() || y.isEmpty() || r.isEmpty())
            throw new IllegalArgumentException("All fields are required.");

        int year;
        double rate;
        try {
            year = Integer.parseInt(y);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Year must be an integer.");
        }
        try {
            rate = Double.parseDouble(r);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Rate must be a number.");
        }

        Car c = new Car();
        c.make = make;
        c.model = model;
        c.year = year;
        c.ratePerDay = rate;
        c.available = avail;
        return c;
    }

    private void clearForm() {
        tfMake.setText("");
        tfModel.setText("");
        tfYear.setText("");
        tfRate.setText("");
        cbAvailable.setSelected(true);
    }
}

}

