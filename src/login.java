/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
// You may also need ResultSet and JOptionPane if you use them without qualification
import java.sql.ResultSet;
import javax.swing.JOptionPane;
/**
 *
 * @author Hp
 */
public class login extends javax.swing.JFrame {

    // 🌟 ADDED DATABASE CONNECTION CONSTANTS HERE 🌟
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ICS_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // Empty string for no password
    // 🌟 END OF ADDED CONSTANTS 🌟

    /**
     * Creates new form login
     */
    public login() {
        initComponents();
    }

    // ... (rest of initComponents() method is here)

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        uname = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        pass = new javax.swing.JPasswordField();
        Exit = new javax.swing.JButton();
        Login = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 204, 204));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Password");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 320, -1, -1));

        uname.setFont(new java.awt.Font("Segoe UI Variable", 0, 24)); // NOI18N
        uname.setToolTipText("");
        uname.setBorder(null);
        uname.setCaretColor(new java.awt.Color(204, 51, 255));
        uname.setDisabledTextColor(new java.awt.Color(204, 102, 255));
        uname.setOpaque(true);
        uname.setSelectionColor(new java.awt.Color(153, 102, 255));
        uname.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unameActionPerformed(evt);
            }
        });
        jPanel1.add(uname, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 280, 340, 40));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Username");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 260, -1, -1));

        pass.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        pass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passActionPerformed(evt);
            }
        });
        jPanel1.add(pass, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 340, 340, 40));

        Exit.setBackground(new java.awt.Color(255, 153, 153));
        Exit.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        Exit.setForeground(new java.awt.Color(255, 255, 255));
        Exit.setText("EXIT");
        Exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitActionPerformed(evt);
            }
        });
        jPanel1.add(Exit, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 390, 140, 40));

        Login.setBackground(new java.awt.Color(255, 153, 153));
        Login.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        Login.setForeground(new java.awt.Color(255, 255, 255));
        Login.setText("LOGIN");
        Login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoginActionPerformed(evt);
            }
        });
        jPanel1.add(Login, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 390, 140, 40));

        jLabel4.setIcon(new javax.swing.ImageIcon("C:\\Users\\Hp\\Desktop\\paSUNLIGHTkaBOI\\mini system\\ICS\\bg2.png")); // NOI18N
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(-70, -10, 950, 680));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void unameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_unameActionPerformed

    private void passActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_passActionPerformed

    private void ExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitActionPerformed
        // TODO add your handling code here:
        dispose();
    }//GEN-LAST:event_ExitActionPerformed

    private void LoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoginActionPerformed

        String enteredUsername = uname.getText().trim();
        // Retrieving password as String from JPasswordField
        String enteredPassword = new String(pass.getPassword()).trim();

        // 1. Basic Field Validation
        if(enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter both username and password!", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // SQL query to check if a user exists with BOTH the provided username and plaintext password
        // The password column in your database table must be named 'password'
        String sql = "SELECT username FROM users WHERE username = ? AND password = ?";

        try (
            // Establish connection using defined constants
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            // Prepare statement to prevent SQL Injection
            PreparedStatement pstmt = conn.prepareStatement(sql);
        ) {
            // Set the parameters safely
            pstmt.setString(1, enteredUsername);
            pstmt.setString(2, enteredPassword);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Successful Login: A matching record was found
                    JOptionPane.showMessageDialog(null, "Login Successful! Welcome, " + enteredUsername + ".");

                    // Open home frame (assuming 'home' is the name of your next JFrame class)
                    home bahay = new home();
                    bahay.setVisible(true);

                    // Close the login frame
                    this.dispose();
                } else {
                    // Failed Login: No matching record found
                    JOptionPane.showMessageDialog(null, "Invalid Username or Password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    pass.setText(""); // Clear password field
                }
            }
        } catch (SQLException ex) {
            // Handle database connection or query errors
            JOptionPane.showMessageDialog(null, "Database Error: Check connection details and ensure the JDBC driver is added.\n" + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_LoginActionPerformed

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
            java.util.logging.Logger.getLogger(login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new login().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Exit;
    private javax.swing.JButton Login;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPasswordField pass;
    private javax.swing.JTextField uname;
    // End of variables declaration//GEN-END:variables
}