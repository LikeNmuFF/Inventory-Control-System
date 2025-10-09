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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Hp
 */
public class stockInPurchase extends javax.swing.JFrame {

    // 1. DATABASE CONNECTION CONSTANTS
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ICS_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; 
    
    /**
     * Creates new form stockInPurchase
     */
    public stockInPurchase() {
        initComponents();
        this.setLocationRelativeTo(null); // Center the frame
        
        // Custom methods called on startup
        FillCombo(); // Load product names into JComboBox
        SelectStockInLog(); // Load log data into JTable
        setDateTime(); // Set current date and time
        
        // Add action listeners to buttons
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });
        
        btnHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHomeActionPerformed(evt);
            }
        });
        // btnClear is already linked via initComponents
    }

    // =========================================================================
    // DATABASE HELPER METHODS
    // =========================================================================

    private Connection getConnection() throws SQLException {
        // Ensures the MySQL JDBC Driver is loaded
        try {
             Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
             throw new SQLException("MySQL Driver not found.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    
    // =========================================================================
    // UI POPULATION METHODS
    // =========================================================================
    
    /**
     * Fills the comboProduct JComboBox with product names from the database.
     */
    private void FillCombo() {
        Connection Con = null;
        Statement St = null;
        ResultSet Rs = null;
        try {
            Con = getConnection();
            St = Con.createStatement();
            Rs = St.executeQuery("SELECT name FROM products");
            
            // Clear existing default items
            comboProduct.removeAllItems();
            
            while (Rs.next()) {
                // FIX 1: Change "ProductName" to "name" as per the 'products' table schema.
                String productName = Rs.getString("name"); 
                comboProduct.addItem(productName);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error loading Products: " + e.getMessage());
        } finally {
            try { if (Rs != null) Rs.close(); } catch (Exception e) {}
            try { if (St != null) St.close(); } catch (Exception e) {}
            try { if (Con != null) Con.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * Loads the stock-in log data from the database into jTable1.
     */
    private void SelectStockInLog() {
        Connection Con = null;
        Statement St = null;
        ResultSet Rs = null;
        try {
            Con = getConnection();
            St = Con.createStatement();
            // Select all columns from the stockin_log table
            Rs = St.executeQuery("SELECT LogID, ProductName, SupplierName, QuantityIn, UnitCost, DateTime FROM stockin_log");
            
            // Get the table model
            DefaultTableModel tableModel = new DefaultTableModel();
            
            // Set Column Headers
            tableModel.setColumnIdentifiers(new Object[]{"Log ID", "Product Name", "Supplier", "Quantity In", "Unit Cost", "Date/Time"});

            // Populate the table data
            while (Rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(Rs.getInt("LogID"));
                row.add(Rs.getString("ProductName"));
                row.add(Rs.getString("SupplierName"));
                row.add(Rs.getInt("QuantityIn"));
                row.add(Rs.getDouble("UnitCost"));
                row.add(Rs.getTimestamp("DateTime"));
                tableModel.addRow(row);
            }
            
            jTable1.setModel(tableModel);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error loading Supplier Logs: " + e.getMessage());
        } finally {
            try { if (Rs != null) Rs.close(); } catch (Exception e) {}
            try { if (St != null) St.close(); } catch (Exception e) {}
            try { if (Con != null) Con.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * Sets the current date and time into the txtDatenTime field.
     */
    private void setDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        txtDatenTime.setText(sdf.format(new Date()));
        txtDatenTime.setEditable(false); // Make it read-only as it's system-generated
    }


    // =========================================================================
    // ACTION HANDLERS
    // =========================================================================

    /**
     * Resets all input fields to their initial state.
     */
    private void ClearFields() {
        comboProduct.setSelectedIndex(-1); // Resets selection
        txtSupplier.setText("");
        txtQuantity.setText("");
        txtUnitCost.setText("");
        setDateTime(); // Refresh the date/time field
    }
    
    /**
     * Handles the Add button click: inserts stock-in log and updates product inventory.
     */
    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {
        if (comboProduct.getSelectedItem() == null || 
            txtSupplier.getText().isEmpty() || 
            txtQuantity.getText().isEmpty() || 
            txtUnitCost.getText().isEmpty()) {
            
            JOptionPane.showMessageDialog(this, "Missing Information. Please fill all fields.");
            return;
        }

        Connection Con = null;
        PreparedStatement PstLog = null;
        PreparedStatement PstUpdateProd = null;
        
        try {
            // 1. Validate numerical inputs
            String productName = comboProduct.getSelectedItem().toString();
            String supplierName = txtSupplier.getText();
            int quantityIn = Integer.parseInt(txtQuantity.getText());
            double unitCost = Double.parseDouble(txtUnitCost.getText());
            String dateTime = txtDatenTime.getText(); // Will use this as a string, or parse to Timestamp if needed

            if (quantityIn <= 0) {
                 JOptionPane.showMessageDialog(this, "Quantity In must be greater than zero.");
                 return;
            }
            
            // 2. Establish connection
            Con = getConnection();
            Con.setAutoCommit(false); // Start transaction for atomicity

            // --- A. Insert into stockin_log table ---
            String logQuery = "INSERT INTO stockin_log (ProductName, SupplierName, QuantityIn, UnitCost, DateTime) VALUES (?, ?, ?, ?, ?)";
            PstLog = Con.prepareStatement(logQuery);
            PstLog.setString(1, productName);
            PstLog.setString(2, supplierName);
            PstLog.setInt(3, quantityIn);
            PstLog.setDouble(4, unitCost);
            PstLog.setString(5, dateTime);
            PstLog.executeUpdate();
            
            // --- B. Update products inventory (add the quantity) ---
            // FIX 2 & 3: Change table name to 'products', and column names to 'quantity' and 'name'
            String updateQuery = "UPDATE products SET quantity = quantity + ? WHERE name = ?";
            PstUpdateProd = Con.prepareStatement(updateQuery);
            PstUpdateProd.setInt(1, quantityIn);
            PstUpdateProd.setString(2, productName);
            
            int rowsAffected = PstUpdateProd.executeUpdate();
            
            if (rowsAffected == 0) {
                // This means the product name selected in the combo box no longer exists in products table
                throw new SQLException("Product update failed. Product may not exist.");
            }
            
            // 3. Commit the transaction
            Con.commit(); 
            JOptionPane.showMessageDialog(this, "Stock In Logged and Inventory Updated Successfully!");
            
            // 4. Refresh UI
            ClearFields();
            SelectStockInLog(); // Refresh the table
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid input. Quantity and Unit Cost must be valid numbers.");
        } catch (SQLException e) {
            try {
                if (Con != null) Con.rollback(); // Rollback on error
            } catch (SQLException ex) {
                // Log rollback failure
            }
            JOptionPane.showMessageDialog(this, "Database Operation Failed: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred: " + e.getMessage());
        } finally {
            try { if (PstLog != null) PstLog.close(); } catch (Exception e) {}
            try { if (PstUpdateProd != null) PstUpdateProd.close(); } catch (Exception e) {}
            try { 
                if (Con != null) {
                    Con.setAutoCommit(true); // Restore default
                    Con.close(); 
                }
            } catch (Exception e) {}
        }
    }
    
    /**
     * Handles the Home button click: navigates back to the home screen.
     */
    private void btnHomeActionPerformed(java.awt.event.ActionEvent evt) {
        // Assuming 'home' is the main dashboard class name
        new home().setVisible(true);
        this.dispose(); // Close the current window
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        prodDetails = new javax.swing.JLabel();
        category = new javax.swing.JLabel();
        supplier = new javax.swing.JLabel();
        product = new javax.swing.JLabel();
        price = new javax.swing.JLabel();
        id5 = new javax.swing.JLabel();
        txtSupplier = new javax.swing.JTextField();
        txtQuantity = new javax.swing.JTextField();
        txtUnitCost = new javax.swing.JTextField();
        comboProduct = new javax.swing.JComboBox<>();
        txtDatenTime = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        btnAdd = new javax.swing.JButton();
        btnHome = new javax.swing.JButton();
        btnClear = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        prodList = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 153, 153));

        jPanel2.setBackground(new java.awt.Color(255, 102, 102));

        jLabel2.setFont(new java.awt.Font("Monospaced", 1, 36)); // NOI18N
        jLabel2.setText("MANAGE PRODUCT");

        jLabel1.setFont(new java.awt.Font("Malgun Gothic", 1, 36)); // NOI18N
        jLabel1.setText("INVENTORY CONTROL SYSTEM");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 563, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(157, 157, 157)))
                .addGap(229, 229, 229))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(0, 12, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(255, 204, 204));

        prodDetails.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 24)); // NOI18N
        prodDetails.setText("PRODUCT DETAILS");

        category.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        category.setText("QUANTITY IN");

        supplier.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        supplier.setText("SUPPLIER");

        product.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        product.setText("PRODUCTS");

        price.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        price.setText("DATE & TIME");

        id5.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        id5.setText("UNIT COST");

        txtUnitCost.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtUnitCostActionPerformed(evt);
            }
        });

        comboProduct.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        txtDatenTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDatenTimeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(prodDetails, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtSupplier, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtUnitCost, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboProduct, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(product, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtDatenTime, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(83, 83, 83)
                        .addComponent(supplier))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(70, 70, 70)
                        .addComponent(category))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(93, 93, 93)
                        .addComponent(id5))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addComponent(price, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(prodDetails)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(product)
                .addGap(5, 5, 5)
                .addComponent(comboProduct, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(supplier)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtSupplier, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(category)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addComponent(id5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtUnitCost, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(price)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtDatenTime, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(93, 93, 93))
        );

        jPanel4.setBackground(new java.awt.Color(255, 102, 102));

        btnAdd.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 18)); // NOI18N
        btnAdd.setText("ADD");

        btnHome.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 18)); // NOI18N
        btnHome.setText("HOME");

        btnClear.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 18)); // NOI18N
        btnClear.setText("CLEAR");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(303, 303, 303)
                .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(129, 129, 129)
                .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 284, Short.MAX_VALUE)
                .addComponent(btnHome, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnHome, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(26, Short.MAX_VALUE))
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

        prodList.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 24)); // NOI18N
        prodList.setText("SUPPLIER LOGS");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jScrollPane1))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(291, 291, 291)
                                        .addComponent(prodList, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE)))))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(prodList)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1)))
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtUnitCostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtUnitCostActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtUnitCostActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        ClearFields(); // Implementation added
    }//GEN-LAST:event_btnClearActionPerformed

    private void txtDatenTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDatenTimeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDatenTimeActionPerformed

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
            java.util.logging.Logger.getLogger(stockInPurchase.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(stockInPurchase.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(stockInPurchase.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(stockInPurchase.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new stockInPurchase().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnHome;
    private javax.swing.JLabel category;
    private javax.swing.JComboBox<String> comboProduct;
    private javax.swing.JLabel id5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel price;
    private javax.swing.JLabel prodDetails;
    private javax.swing.JLabel prodList;
    private javax.swing.JLabel product;
    private javax.swing.JLabel supplier;
    private javax.swing.JTextField txtDatenTime;
    private javax.swing.JTextField txtQuantity;
    private javax.swing.JTextField txtSupplier;
    private javax.swing.JTextField txtUnitCost;
    // End of variables declaration//GEN-END:variables
}