/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.Vector;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Date; 
import java.awt.event.ActionListener; // Import for ActionListener

/**
 *
 * @author Hp
 */
public class viewInventory extends javax.swing.JFrame {

    // =========================================================================
    // 1. DATABASE CONNECTION CONSTANTS & FORMATTING (Updated for ics_db)
    // =========================================================================
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ics_db"; // Database name from SQL dump
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // Adjust if you have a password
    // Format for currency output
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("₱#,##0.00");  

    /**
     * Creates new form viewInventory
     */
    public viewInventory() {
        initComponents();
        this.setLocationRelativeTo(null); // Center the frame
        
        // Load data on startup
        LoadInventoryTable("", ""); // Initial load with no search/supplier filter 
        loadDashboardMetrics();
        addSearchKeyListener();
        
        // --- START OF NEW/MODIFIED CODE FOR ComboSupplier ---
        populateComboSupplier(); // Load suppliers into the combo box
        
        // Attach action listener to ComboSupplier for filtering
        ComboSupplier.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ComboSupplierActionPerformed(evt);
            }
        });
        // --- END OF NEW/MODIFIED CODE FOR ComboSupplier ---
        
        // Attach action listeners to buttons
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });
        
        // NOTE: ComboSupplier and btnReport functionality is omitted as it requires 
        // a 'suppliers' table and detailed reporting logic not yet defined.
    }

    // =========================================================================
    // DATABASE HELPER METHODS
    // =========================================================================

    private Connection getConnection() throws SQLException {
        try {
             // Ensure the MySQL JDBC Driver is loaded
             Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
             throw new SQLException("MySQL JDBC Driver not found. Please add the Connector/J JAR file.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    
    // -------------------------------------------------------------------------
    // 2. LOAD DASHBOARD SUMMARY METRICS
    // -------------------------------------------------------------------------
    private void loadDashboardMetrics() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // Query 1: Total Products, Total Stock Value, Low Stock Count
            // Uses 'products' table and columns: product_id, quantity, price, reorder_point
            String summaryQuery = "SELECT "
                                + "COUNT(product_id) AS TotalProducts, "
                                + "COALESCE(SUM(quantity * price), 0.00) AS TotalValue, "
                                + "SUM(CASE WHEN quantity <= reorder_point THEN 1 ELSE 0 END) AS LowStockCount "
                                + "FROM products";
            rs = stmt.executeQuery(summaryQuery);
            if (rs.next()) {
                totalProducts1.setText(String.valueOf(rs.getInt("TotalProducts")));
                totalStockValue.setText(CURRENCY_FORMAT.format(rs.getDouble("TotalValue")));
                lowStockItem.setText(String.valueOf(rs.getInt("LowStockCount")));
            }
            rs.close(); // Close the first result set

            // Query 2: Daily Activity (Total Quantity Stocked In Today)
            // Uses 'stockin_log' table and columns: QuantityIn, DateTime
            // Note: Column capitalization matches the SQL dump
            String dailyActivityQuery = "SELECT COALESCE(SUM(QuantityIn), 0) AS DailyIn "
                                     + "FROM stockin_log "
                                     + "WHERE DATE(DateTime) = CURDATE()"; 
            rs = stmt.executeQuery(dailyActivityQuery);
            if (rs.next()) {
                int dailyIn = rs.getInt("DailyIn");
                dailyActivity.setText(String.valueOf(dailyIn));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Dashboard Load Error: " + e.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            // Close resources
            try { if (rs != null) rs.close(); } catch (SQLException e) { /* log */ }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* log */ }
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* log */ }
        }
    }
    
    // -------------------------------------------------------------------------
    // NEW METHOD: POPULATE COMBO BOX WITH SUPPLIERS
    // -------------------------------------------------------------------------
    private void populateComboSupplier() {
        // Clear existing items but keep the "All Suppliers" default option.
        ComboSupplier.removeAllItems();
        ComboSupplier.addItem("All Suppliers"); 

        // Query to get distinct, non-empty supplier names from stockin_log
        String sql = "SELECT DISTINCT SupplierName FROM stockin_log WHERE SupplierName IS NOT NULL AND SupplierName <> '' ORDER BY SupplierName ASC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String supplier = rs.getString("SupplierName");
                ComboSupplier.addItem(supplier);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading suppliers: " + e.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // -------------------------------------------------------------------------
    // 3. LOAD PRODUCT DATA INTO JTABLE (MODIFIED TO ACCEPT SUPPLIER FILTER)
    // -------------------------------------------------------------------------
// -------------------------------------------------------------------------
// 3. LOAD PRODUCT DATA INTO JTABLE (MODIFIED TO ACCEPT SUPPLIER FILTER)
// -------------------------------------------------------------------------
// -------------------------------------------------------------------------
// 3. LOAD PRODUCT DATA INTO JTABLE (CORRECTED: Aligning Join Key with DB Schema)
// -------------------------------------------------------------------------
    private void LoadInventoryTable(String searchFilter, String supplierFilter) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0); // Clear existing data
        
        // Use StringBuilder to construct the dynamic SQL query
        StringBuilder sql = new StringBuilder();

        // FULLY QUALIFY ALL COLUMNS (p.price * p.quantity) AS StockValue
        sql.append("SELECT ");
        sql.append("p.product_id, p.name, c.category_name, p.quantity, p.price, p.reorder_point, (p.price * p.quantity) AS StockValue ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN categories c ON p.category_id = c.category_id ");

        // Check if a supplier filter is active
        boolean filterBySupplier = supplierFilter != null && !supplierFilter.equals("") && !supplierFilter.equals("All Suppliers");
        
        if (filterBySupplier) {
            // CORRECTION: Join products (p.name) to stockin_log (sl.ProductName)
            // stockin_log only contains ProductName, not product_id.
            sql.append("INNER JOIN (SELECT DISTINCT ProductName FROM stockin_log WHERE SupplierName = ?) AS sl ON p.name = sl.ProductName ");
        }
        
        // Add the common WHERE clause for product/category searching
        sql.append("WHERE p.name LIKE ? OR c.category_name LIKE ?");

        // Table Headers
        String[] columnNames = {"ID", "Product Name", "Category", "Quantity", "Price (₱)", "Reorder Pt", "Stock Value (₱)"};
        model.setColumnIdentifiers(columnNames);

        try (Connection conn = getConnection();
            PreparedStatement pst = conn.prepareStatement(sql.toString())) {
            
            String searchPattern = "%" + searchFilter + "%";
            int paramIndex = 1;
            
            // 1. Set Supplier Parameter (if applicable)
            if (filterBySupplier) {
                pst.setString(paramIndex++, supplierFilter);
            }
            
            // 2. Set Search Parameters
            pst.setString(paramIndex++, searchPattern); // Filter by Product Name (p.name)
            pst.setString(paramIndex++, searchPattern); // Filter by Category Name (c.category_name)

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    // Fetch columns using their full names/aliases from the SELECT list
                    row.add(rs.getInt("product_id")); 
                    row.add(rs.getString("name"));       
                    row.add(rs.getString("category_name") == null ? "N/A" : rs.getString("category_name")); 
                    row.add(rs.getInt("quantity"));    
                    row.add(CURRENCY_FORMAT.format(rs.getDouble("price"))); 
                    row.add(rs.getInt("reorder_point"));
                    row.add(CURRENCY_FORMAT.format(rs.getDouble("StockValue"))); 
                    model.addRow(row);
                }
            }
            
        } catch (SQLException e) {
            // Keep the original error message for debugging
            JOptionPane.showMessageDialog(this, "Failed to load inventory data: " + e.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
        
    // -------------------------------------------------------------------------
    // 4. SEARCH TEXT FIELD KEY LISTENER (MODIFIED TO PASS SUPPLIER FILTER)
    // -------------------------------------------------------------------------
    private void addSearchKeyListener() {
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // Get the current selected supplier for filtering
                String selectedSupplier = (String) ComboSupplier.getSelectedItem();
                // Filter the table data in real-time as the user types, respecting the supplier filter
                LoadInventoryTable(txtSearch.getText(), selectedSupplier);
            }
        });
    }

    // -------------------------------------------------------------------------
    // 5. BUTTON ACTIONS
    // -------------------------------------------------------------------------
    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {                                        
        // Navigate back to the main menu/Home window (Assuming 'home' is your main menu class)
        // You must have a 'home.java' class available for this to work.
        try {
            // Check if the 'home' class exists before attempting to instantiate
            Class.forName("home");
            
            // If it exists, create and display it
            javax.swing.JFrame homeFrame = (javax.swing.JFrame) Class.forName("home").getDeclaredConstructor().newInstance();
            homeFrame.setVisible(true);
            this.dispose(); // Close the current window
            
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "The 'home.java' class was not found. Cannot navigate back.", "Navigation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
             JOptionPane.showMessageDialog(this, "Error returning to Home screen: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }  


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jRadioButton1 = new javax.swing.JRadioButton();
        jLabel7 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        btnBack = new javax.swing.JButton();
        ComboSupplier = new javax.swing.JComboBox<>();
        txtSearch = new javax.swing.JTextField();
        lowStockItem = new javax.swing.JLabel();
        totalStockValue = new javax.swing.JLabel();
        totalProducts1 = new javax.swing.JLabel();
        dailyActivity = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

        jRadioButton1.setText("jRadioButton1");

        jLabel7.setText("jLabel7");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 204, 204));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTable1.setBackground(new java.awt.Color(0, 153, 153));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, 1190, 350));

        btnBack.setBackground(new java.awt.Color(0, 153, 153));
        btnBack.setFont(new java.awt.Font("Segoe UI Black", 1, 12)); // NOI18N
        btnBack.setText("BACK");
        jPanel1.add(btnBack, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 190, 210, 40));

        ComboSupplier.setBackground(new java.awt.Color(0, 153, 153));
        ComboSupplier.setFont(new java.awt.Font("Segoe UI Black", 1, 12)); // NOI18N
        ComboSupplier.setToolTipText("");
        ComboSupplier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ComboSupplierActionPerformed(evt);
            }
        });
        jPanel1.add(ComboSupplier, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 190, 200, 50));

        txtSearch.setBackground(new java.awt.Color(0, 153, 153));
        txtSearch.setFont(new java.awt.Font("Segoe UI Black", 1, 12)); // NOI18N
        jPanel1.add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 190, 310, 50));

        lowStockItem.setBackground(new java.awt.Color(255, 255, 255));
        lowStockItem.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        jPanel1.add(lowStockItem, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 290, 260, 70));

        totalStockValue.setBackground(new java.awt.Color(255, 255, 255));
        totalStockValue.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        jPanel1.add(totalStockValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 300, 240, 70));

        totalProducts1.setBackground(new java.awt.Color(255, 255, 255));
        totalProducts1.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        jPanel1.add(totalProducts1, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 300, 240, 70));

        dailyActivity.setBackground(new java.awt.Color(255, 51, 51));
        dailyActivity.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        jPanel1.add(dailyActivity, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 300, 240, 70));

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/1a.png"))); // NOI18N
        jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1230, 800));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * MODIFIED METHOD: Now implements the logic to filter the table
     * based on the selected supplier, and calls LoadInventoryTable.
     */
    private void ComboSupplierActionPerformed(java.awt.event.ActionEvent evt) {
        // This action listener is called when an item is selected in the combo box.
        if (ComboSupplier.getSelectedItem() == null) return;
        
        String selectedSupplier = (String) ComboSupplier.getSelectedItem();
        String currentSearchText = txtSearch.getText();
        
        // Reload the inventory table with the current search text and the selected supplier filter.
        // The LoadInventoryTable method handles the "All Suppliers" case.
        LoadInventoryTable(currentSearchText, selectedSupplier);
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(viewInventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(viewInventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(viewInventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(viewInventory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new viewInventory().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> ComboSupplier;
    private javax.swing.JButton btnBack;
    private javax.swing.JLabel dailyActivity;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lowStockItem;
    private javax.swing.JLabel totalProducts1;
    private javax.swing.JLabel totalStockValue;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}