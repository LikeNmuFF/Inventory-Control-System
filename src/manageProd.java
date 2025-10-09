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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


/**
 *
 * @author Hp
 */
public class manageProd extends javax.swing.JFrame {

    // =========================================================================
    // 1. DATABASE CONNECTION CONSTANTS
    // =========================================================================
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ICS_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // Adjust if you have a password
    
    // Assume a Home class exists for navigation
    // NOTE: If your main menu is named differently, update the btnHomeActionPerformed method.

    /**
     * Creates new form manageProd
     */
    
    public manageProd() {
        initComponents();
        this.setLocationRelativeTo(null); 
        
        // 👇 ADD THIS LINE TO REGISTER THE MOUSE LISTENER
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        // 👆 END OF FIX

        // Initial data load when the frame is created
        LoadCategoriesToCombo();
        DisplayProducts();
        
        // Ensure ID field is read-only as it's typically auto-generated or selected from the table
        txtId.setEditable(false); 
    }
    
    // =========================================================================
    // 2. DATABASE UTILITY METHODS
    // =========================================================================

    /**
     * Establishes and returns a database connection.
     * @return The active Connection object.
     * @throws SQLException if connection fails.
     */
    private Connection getConnection() throws SQLException {
        try {
             Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
             System.err.println("MySQL JDBC Driver not found.");
             throw new SQLException("JDBC Driver not found. Ensure the JAR is in project libraries.", e);
        }
        // Connection is automatically closed by try-with-resources in calling methods
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    
    /**
     * Retrieves the category_id from the categories table based on the selected category_name.
     * @return The category ID or -1 if not found.
     */
    private int getSelectedCategoryId() {
        String selectedName = (String) comboCategory.getSelectedItem();
        if (selectedName == null || selectedName.isEmpty()) return -1;
        
        String sql = "SELECT category_id FROM categories WHERE category_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, selectedName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("category_id");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error finding category ID: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return -1;
    }
    
    // =========================================================================
    // 3. DATA LOADING AND VIEWING (READ)
    // =========================================================================

    /**
     * Loads category names into the comboCategory dropdown.
     */
    private void LoadCategoriesToCombo() {
        String sql = "SELECT category_name FROM categories ORDER BY category_name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            comboCategory.removeAllItems(); // Clear previous items
            while (rs.next()) {
                comboCategory.addItem(rs.getString("category_name"));
            }
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading categories: " + ex.getMessage() + "\nEnsure 'categories' table exists.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Fetches all product data and displays it in jTable1.
     */
    private void DisplayProducts() {
        // SQL JOIN query to fetch category name instead of just the ID
        String sql = "SELECT p.product_id, p.name, c.category_name, p.quantity, p.price, p.reorder_point "
                   + "FROM products p JOIN categories c ON p.category_id = c.category_id ORDER BY p.product_id ASC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DefaultTableModel model = new DefaultTableModel();
            // Define columns (must match the SELECT statement order)
            model.setColumnIdentifiers(new Object[]{"ID", "Product Name", "Category", "Quantity", "Price", "Reorder Pt"});
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("product_id"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getInt("reorder_point")
                });
            }
            
            jTable1.setModel(model);
            
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading product data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // =========================================================================
    // 4. CRUD OPERATIONS (CREATE, UPDATE, DELETE)
    // =========================================================================
    
    /**
     * Implements the CREATE operation (Add Product).
     */
    private void AddProduct() {
        if (txtName.getText().isEmpty() || txtQuantity.getText().isEmpty() || txtPrice.getText().isEmpty() || txtReorderPt.getText().isEmpty() || comboCategory.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this, "Please fill in all product details.", "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int categoryId = getSelectedCategoryId();
        if (categoryId == -1) {
             JOptionPane.showMessageDialog(this, "Invalid category selected or category table is empty.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }

        String sql = "INSERT INTO products (name, category_id, quantity, price, reorder_point) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            pstmt.setString(1, txtName.getText());
            pstmt.setInt(2, categoryId);
            pstmt.setInt(3, Integer.parseInt(txtQuantity.getText()));
            pstmt.setDouble(4, Double.parseDouble(txtPrice.getText()));
            pstmt.setInt(5, Integer.parseInt(txtReorderPt.getText()));

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                ClearFields();
                DisplayProducts(); // Refresh the table
            }
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity, Price, and Reorder Point must be valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Implements the UPDATE operation.
     */
    private void UpdateProduct() {
        if (txtId.getText().isEmpty() || txtName.getText().isEmpty() || txtQuantity.getText().isEmpty() || txtPrice.getText().isEmpty() || txtReorderPt.getText().isEmpty() || comboCategory.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to update and fill all fields.", "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int categoryId = getSelectedCategoryId();
        if (categoryId == -1) {
             JOptionPane.showMessageDialog(this, "Invalid category selected.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }

        String sql = "UPDATE products SET name = ?, category_id = ?, quantity = ?, price = ?, reorder_point = ? WHERE product_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Set parameters
            pstmt.setString(1, txtName.getText());
            pstmt.setInt(2, categoryId);
            pstmt.setInt(3, Integer.parseInt(txtQuantity.getText()));
            pstmt.setDouble(4, Double.parseDouble(txtPrice.getText()));
            pstmt.setInt(5, Integer.parseInt(txtReorderPt.getText()));
            pstmt.setInt(6, Integer.parseInt(txtId.getText())); // WHERE clause ID

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                ClearFields();
                DisplayProducts(); // Refresh the table
            } else {
                 JOptionPane.showMessageDialog(this, "No product found with that ID or no changes were made.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity, Price, and Reorder Point must be valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Implements the DELETE operation.
     */
    private void DeleteProduct() {
        if (txtId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a product ID to delete first.", "Missing Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Product ID: " + txtId.getText() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(txtId.getText()));

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Product deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                ClearFields();
                DisplayProducts(); // Refresh the table
            } else {
                 JOptionPane.showMessageDialog(this, "No product found with that ID.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Product ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Clears all input fields.
     */
    private void ClearFields() {
        txtId.setText("");
        txtName.setText("");
        if (comboCategory.getItemCount() > 0) {
            comboCategory.setSelectedIndex(0); 
        }
        txtQuantity.setText("");
        txtPrice.setText("");
        txtReorderPt.setText("");
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
        name = new javax.swing.JLabel();
        id = new javax.swing.JLabel();
        reorderPt = new javax.swing.JLabel();
        price = new javax.swing.JLabel();
        id5 = new javax.swing.JLabel();
        txtId = new javax.swing.JTextField();
        txtName = new javax.swing.JTextField();
        txtQuantity = new javax.swing.JTextField();
        txtPrice = new javax.swing.JTextField();
        txtReorderPt = new javax.swing.JTextField();
        comboCategory = new javax.swing.JComboBox<>();
        jPanel4 = new javax.swing.JPanel();
        btnAdd = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
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
        category.setText("CATEGORY");

        name.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        name.setText("NAME");

        id.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        id.setText("ID");

        reorderPt.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        reorderPt.setText("REORDER POINT");

        price.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        price.setText("PRICE");

        id5.setFont(new java.awt.Font("Microsoft New Tai Lue", 0, 18)); // NOI18N
        id5.setText("QUANTITY");

        txtPrice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPriceActionPerformed(evt);
            }
        });

        txtReorderPt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtReorderPtActionPerformed(evt);
            }
        });

        comboCategory.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

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
                            .addComponent(txtId, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(76, 76, 76)
                                .addComponent(id5))
                            .addComponent(txtPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(92, 92, 92)
                                .addComponent(price, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(txtReorderPt, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(comboCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(104, 104, 104)
                        .addComponent(id, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(83, 83, 83)
                        .addComponent(name))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(70, 70, 70)
                        .addComponent(category))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(66, 66, 66)
                        .addComponent(reorderPt)))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(prodDetails)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(id)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtId, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(name)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(category)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(comboCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(id5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(price)
                .addGap(18, 18, 18)
                .addComponent(txtPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(reorderPt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtReorderPt, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19))
        );

        jPanel4.setBackground(new java.awt.Color(255, 102, 102));

        btnAdd.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 18)); // NOI18N
        btnAdd.setText("ADD");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnUpdate.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 18)); // NOI18N
        btnUpdate.setText("UPDATE");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnDelete.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 18)); // NOI18N
        btnDelete.setText("DELETE");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnHome.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 18)); // NOI18N
        btnHome.setText("HOME");
        btnHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHomeActionPerformed(evt);
            }
        });

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
                .addGap(43, 43, 43)
                .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(70, 70, 70)
                .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(61, 61, 61)
                .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(70, 70, 70)
                .addComponent(btnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(73, 73, 73)
                .addComponent(btnHome, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(39, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
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
        prodList.setText("PRODUCT LIST");

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

    // =========================================================================
    // 5. EVENT HANDLERS (Linked to UI Actions)
    // =========================================================================

    private void txtPriceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPriceActionPerformed
        // TODO add your handling code here: (Keeping original structure for empty handlers)
    }//GEN-LAST:event_txtPriceActionPerformed

    private void txtReorderPtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtReorderPtActionPerformed
        // TODO add your handling code here: (Keeping original structure for empty handlers)
    }//GEN-LAST:event_txtReorderPtActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        ClearFields(); // Implementation added
    }//GEN-LAST:event_btnClearActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        DeleteProduct(); // Implementation added
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        AddProduct(); // Implementation added
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        UpdateProduct(); // Implementation added
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHomeActionPerformed
        // Assuming a separate 'home' class exists for the main dashboard
        try {
            // Use reflection to create an instance of the Home class and make it visible
            Class<?> homeClass = Class.forName("home");
            Object homeInstance = homeClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method setVisibleMethod = homeClass.getMethod("setVisible", boolean.class);
            setVisibleMethod.invoke(homeInstance, true);
            this.dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error navigating to Home screen. Ensure 'home.java' exists: " + e.getMessage(), "Navigation Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnHomeActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {
        // Implementation for loading selected row data into fields
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int Myindex = jTable1.getSelectedRow();
        
        if (Myindex != -1) {
            // The column index must match the order in DisplayProducts column identifiers:
            // 0: ID, 1: Product Name, 2: Category, 3: Quantity, 4: Price, 5: Reorder Pointt
            
            String productId = model.getValueAt(Myindex, 0).toString();
            String name = model.getValueAt(Myindex, 1).toString();
            String category = model.getValueAt(Myindex, 2).toString();
            String quantity = model.getValueAt(Myindex, 3).toString();
            String price = model.getValueAt(Myindex, 4).toString();
            String reorderPoint = model.getValueAt(Myindex, 5).toString();
            
            // Load data into the text fields and combo box
            txtId.setText(productId); // ID is loaded, but remains non-editable
            txtName.setText(name);
            comboCategory.setSelectedItem(category);
            txtQuantity.setText(quantity);
            txtPrice.setText(price);
            txtReorderPt.setText(reorderPoint);
        }
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
            java.util.logging.Logger.getLogger(manageProd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(manageProd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(manageProd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(manageProd.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new manageProd().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnHome;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JLabel category;
    private javax.swing.JComboBox<String> comboCategory;
    private javax.swing.JLabel id;
    private javax.swing.JLabel id5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel name;
    private javax.swing.JLabel price;
    private javax.swing.JLabel prodDetails;
    private javax.swing.JLabel prodList;
    private javax.swing.JLabel reorderPt;
    private javax.swing.JTextField txtId;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextField txtPrice;
    private javax.swing.JTextField txtQuantity;
    private javax.swing.JTextField txtReorderPt;
    // End of variables declaration//GEN-END:variables
}