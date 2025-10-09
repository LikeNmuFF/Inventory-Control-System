/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel; // Added for table model

/**
 *
 * @author Hp
 */
public class home extends javax.swing.JFrame {

    // Variable to hold the username passed from the login screen
    private String loggedInUsername;

    /**
     * Creates new form homie
     */
    public home() {
        initComponents();
        // Load data on startup
        loadDashboardData();
        loadLowStockTableData(); // Added to load table data
    }
    
    /**
     * Overloaded constructor to receive the logged-in username
     */
    public home(String username) {
        this.loggedInUsername = username;
        initComponents();
        setWelcomeAdminLabel(username);
        loadDashboardData();
        loadLowStockTableData();
    }
    
    /**
     * Sets the welcomeAdmin label text.
     * @param username 
     */
    public void setWelcomeAdminLabel(String username) {
        welcomeAdmin.setText(username.toUpperCase());
    }


    private void loadDashboardData() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {

            con = DB_Connection.getConnection(); 
            

            String totalProductsSQL = "SELECT COUNT(product_id) AS TotalProducts FROM products";
            ps = con.prepareStatement(totalProductsSQL);
            rs = ps.executeQuery();
            if (rs.next()) {
                totalProducts1.setText(String.valueOf(rs.getInt("TotalProducts")));
            }
            rs.close();
            ps.close();

            // 2. Total Stock Value (SUM(quantity * price))
            String totalStockValueSQL = "SELECT SUM(quantity * price) AS TotalValue FROM products";
            ps = con.prepareStatement(totalStockValueSQL);
            rs = ps.executeQuery();
            if (rs.next()) {
                // Format the total value as currency
                DecimalFormat df = new DecimalFormat("#,##0.00");
                totalStockValue.setText("₱" + df.format(rs.getDouble("TotalValue")));
            }
            rs.close();
            ps.close();

            // 3. Low Stock Items (quantity <= reorder_point)
            String lowStockItemsSQL = "SELECT COUNT(product_id) AS LowStockCount FROM products WHERE quantity <= reorder_point";
            ps = con.prepareStatement(lowStockItemsSQL);
            rs = ps.executeQuery();
            if (rs.next()) {
                lowStockItem.setText(String.valueOf(rs.getInt("LowStockCount")));
            }
            rs.close();
            ps.close();
            
            // 4. Daily Activity (Total Stock-In today)
            // Note: Assuming 'Daily Activity' means the total quantity stocked-in today.
            String dailyActivitySQL = "SELECT SUM(QuantityIn) AS DailyIn FROM stockin_log WHERE DATE(DateTime) = CURDATE()";
            ps = con.prepareStatement(dailyActivitySQL);
            rs = ps.executeQuery();
            if (rs.next()) {
                 int dailyIn = rs.getInt("DailyIn");
                // If NULL, set to 0, otherwise display the count
                dailyActivity.setText(rs.wasNull() ? "0" : String.valueOf(dailyIn));
            }


        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
              JOptionPane.showMessageDialog(this, "Connection Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                // Log or handle closing exception
            }
        }
    }

    /**
     * Fetches and displays products with quantity <= reorder_point in jTable1.
     */
    private void loadLowStockTableData() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        // Define the table model and column names
        DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Product Name", "Quantity in Stock", "Reorder Point"}, 0);
        jTable1.setModel(model);

        String sql = "SELECT name, quantity, reorder_point FROM products WHERE quantity <= reorder_point ORDER BY quantity ASC";

        try {
            con = DB_Connection.getConnection(); // Use your actual connection method
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();

            // Clear existing data
            model.setRowCount(0);

            // Populate the table model with results
            while (rs.next()) {
                String productName = rs.getString("name");
                int quantity = rs.getInt("quantity");
                int reorderPoint = rs.getInt("reorder_point");
                
                // Add a new row to the table model
                model.addRow(new Object[]{productName, quantity, reorderPoint});
            }
            
            // Check if no low stock items were found
            if (model.getRowCount() == 0) {
                 model.addRow(new Object[]{"No items below reorder point.", "—", "—"});
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error loading low stock: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
             JOptionPane.showMessageDialog(this, "Connection Error loading low stock: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                // Log or handle closing exception
            }
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

        jLabel5 = new javax.swing.JLabel();
        welcomeAdmin = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lowStockItem = new javax.swing.JLabel();
        totalStockValue = new javax.swing.JLabel();
        totalProducts1 = new javax.swing.JLabel();
        dailyActivity = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        manageProduct = new javax.swing.JButton();
        stock_in_purchase = new javax.swing.JButton();
        view_inventory = new javax.swing.JButton();
        REPORTS = new javax.swing.JButton();
        logout = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel5.setFont(new java.awt.Font("Yu Gothic UI", 1, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("WELCOME:");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 6, 290, 40));

        welcomeAdmin.setFont(new java.awt.Font("Yu Gothic UI Semilight", 1, 24)); // NOI18N
        welcomeAdmin.setForeground(new java.awt.Color(255, 0, 51));
        welcomeAdmin.setText("ADMIN");
        getContentPane().add(welcomeAdmin, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 6, 290, 40));

        jLabel2.setFont(new java.awt.Font("Book Antiqua", 1, 48)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 153, 153));
        jLabel2.setText("Inventory Control System");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(463, 72, 600, -1));

        jLabel7.setFont(new java.awt.Font("Trebuchet MS", 1, 36)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 51, 51));
        jLabel7.setText("DASHBOARD");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(853, 132, 210, -1));

        lowStockItem.setBackground(new java.awt.Color(255, 255, 255));
        lowStockItem.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        getContentPane().add(lowStockItem, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 240, 232, 75));

        totalStockValue.setBackground(new java.awt.Color(255, 255, 255));
        totalStockValue.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        getContentPane().add(totalStockValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 240, 222, 75));

        totalProducts1.setBackground(new java.awt.Color(255, 255, 255));
        totalProducts1.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        getContentPane().add(totalProducts1, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 240, 210, 75));

        dailyActivity.setBackground(new java.awt.Color(255, 51, 51));
        dailyActivity.setFont(new java.awt.Font("Cambria", 1, 36)); // NOI18N
        getContentPane().add(dailyActivity, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 240, 166, 75));

        jLabel4.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel4.setText("DAILY ACTIVITY IN");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(910, 210, 134, -1));

        jLabel6.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel6.setText("TOTAL PRODUCTS");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(689, 212, 134, -1));

        jLabel3.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel3.setText("TOTAL STOCK VALUE");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 210, 209, -1));

        jLabel1.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel1.setText("LOW STOCK ITEM");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(189, 212, 134, -1));

        manageProduct.setBackground(new java.awt.Color(255, 153, 153));
        manageProduct.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 18)); // NOI18N
        manageProduct.setText("MANAGE PRODUCT");
        manageProduct.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manageProductActionPerformed(evt);
            }
        });
        getContentPane().add(manageProduct, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 370, 250, 38));

        stock_in_purchase.setBackground(new java.awt.Color(255, 102, 102));
        stock_in_purchase.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 18)); // NOI18N
        stock_in_purchase.setText("STOCK IN PURCHASE");
        stock_in_purchase.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stock_in_purchaseActionPerformed(evt);
            }
        });
        getContentPane().add(stock_in_purchase, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 420, 250, 38));

        view_inventory.setBackground(new java.awt.Color(255, 51, 51));
        view_inventory.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 18)); // NOI18N
        view_inventory.setText("VIEW INVENTORY");
        view_inventory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                view_inventoryActionPerformed(evt);
            }
        });
        getContentPane().add(view_inventory, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 470, 250, 38));

        REPORTS.setBackground(new java.awt.Color(255, 0, 0));
        REPORTS.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 18)); // NOI18N
        REPORTS.setText("REPORTS");
        REPORTS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                REPORTSActionPerformed(evt);
            }
        });
        getContentPane().add(REPORTS, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 520, 250, 38));

        logout.setBackground(new java.awt.Color(255, 0, 0));
        logout.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 18)); // NOI18N
        logout.setText("LOG OUT");
        logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutActionPerformed(evt);
            }
        });
        getContentPane().add(logout, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 570, 250, 38));

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

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(385, 348, 669, 319));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void manageProductActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manageProductActionPerformed
        // TODO add your handling code here:
        manageProd mp = new manageProd();
        mp.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_manageProductActionPerformed

    private void stock_in_purchaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stock_in_purchaseActionPerformed
        // TODO add your handling code here:
        stockInPurchase pro = new stockInPurchase(); // Opens your stock-in screen
        pro.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_stock_in_purchaseActionPerformed

    private void view_inventoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_view_inventoryActionPerformed
        // TODO add your handling code here:
        viewInventory cus = new viewInventory(); // Opens your inventory/stock view screen
        cus.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_view_inventoryActionPerformed

    private void REPORTSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_REPORTSActionPerformed
        // TODO add your handling code here:
        Reports rep = new Reports(); // Opens your inventory/stock view screen
        rep.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_REPORTSActionPerformed

    private void logoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutActionPerformed
        // TODO add your handling code here:
        login log = new login();
        log.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_logoutActionPerformed

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
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(home.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // When run from main, it won't have the username, so it calls the default constructor
                new home().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton REPORTS;
    private javax.swing.JLabel dailyActivity;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton logout;
    private javax.swing.JLabel lowStockItem;
    private javax.swing.JButton manageProduct;
    private javax.swing.JButton stock_in_purchase;
    private javax.swing.JLabel totalProducts1;
    private javax.swing.JLabel totalStockValue;
    private javax.swing.JButton view_inventory;
    private javax.swing.JLabel welcomeAdmin;
    // End of variables declaration//GEN-END:variables
}