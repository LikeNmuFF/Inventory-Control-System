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
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        txtSearch = new javax.swing.JTextField();
        searchProductOrCategory = new javax.swing.JLabel();
        btnBack = new javax.swing.JButton();
        supplier = new javax.swing.JLabel();
        ComboSupplier = new javax.swing.JComboBox<>();
        jPanel4 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        lowStockItem = new javax.swing.JLabel();
        totalStockValue = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        totalProducts1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        dailyActivity = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        jRadioButton1.setText("jRadioButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 204, 204));

        jPanel3.setBackground(new java.awt.Color(255, 102, 102));

        jLabel2.setFont(new java.awt.Font("Monospaced", 1, 36)); // NOI18N
        jLabel2.setText("VIEW INVENTORY");

        jLabel1.setFont(new java.awt.Font("Malgun Gothic", 1, 36)); // NOI18N
        jLabel1.setText("INVENTORY CONTROL SYSTEM");

        jPanel2.setBackground(new java.awt.Color(255, 153, 153));

        searchProductOrCategory.setText("SEARCH BY PRODUCT/CATEGORY:");

        btnBack.setText("BACK");

        supplier.setText("SUPPLIER");

        ComboSupplier.setToolTipText("");
        // NOTE: The action listener for filtering is now added in the constructor
        // private void ComboSupplierActionPerformed(java.awt.event.ActionEvent evt) { ... }
        // The IDE-generated code remains:
        ComboSupplier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ComboSupplierActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(supplier, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(81, 81, 81)
                        .addComponent(searchProductOrCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(btnBack, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(ComboSupplier, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(40, 40, 40))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchProductOrCategory)
                    .addComponent(supplier))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(ComboSupplier, javax.swing.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE)
                        .addComponent(btnBack, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(18, 18, 18))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 563, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(157, 157, 157)))
                .addGap(229, 229, 229))
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(255, 153, 153));

        jLabel5.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel5.setText("LOW STOCK ITEM");

        lowStockItem.setBackground(new java.awt.Color(255, 255, 255));
        lowStockItem.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N

        totalStockValue.setBackground(new java.awt.Color(255, 255, 255));
        totalStockValue.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel3.setText("TOTAL STOCK VALUE");

        jLabel6.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel6.setText("TOTAL PRODUCTS");

        totalProducts1.setBackground(new java.awt.Color(255, 255, 255));
        totalProducts1.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N

        jLabel4.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel4.setText("DAILY STOCK IN");

        dailyActivity.setBackground(new java.awt.Color(255, 51, 51));
        dailyActivity.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lowStockItem, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(totalStockValue, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(31, 31, 31)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(86, 86, 86))
                    .addComponent(totalProducts1, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32))
                    .addComponent(dailyActivity, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(54, 54, 54))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(dailyActivity, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lowStockItem, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(totalStockValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(totalProducts1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                .addGap(42, 42, 42))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lowStockItem;
    private javax.swing.JLabel searchProductOrCategory;
    private javax.swing.JLabel supplier;
    private javax.swing.JLabel totalProducts1;
    private javax.swing.JLabel totalStockValue;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}