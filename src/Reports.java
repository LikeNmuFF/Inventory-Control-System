import com.toedter.calendar.JDateChooser;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Hp
 */
public class Reports extends javax.swing.JFrame {

    // =========================================================
    // 1. CLASS FIELDS & UTILITY COMPONENTS
    // =========================================================
    
    // Group to ensure only one radio button can be selected
    private ButtonGroup reportGroup;
    
    // Date formatter for consistent date string output (e.g., for SQL)
    private final SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    
    /**
     * Attempts to establish a connection to the MySQL database.
     * !!! IMPORTANT: YOU MUST CHANGE THE USER and PASSWORD BELOW !!!
     */
    private Connection getConnection() {
        // ⚠️ CRITICAL: UPDATE THESE THREE VALUES TO MATCH YOUR XAMPP/WAMP/MAMP CONFIGURATION
        final String JDBC_URL = "jdbc:mysql://localhost:3306/ics_db"; 
        final String USER = "root";    // <--- Change this if your DB user isn't 'root'
        final String PASSWORD = "";    // <--- Change this to your actual password (empty by default on XAMPP)

        try {
            // Register the JDBC driver (optional for modern Java, but safer)
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Establish the connection
            return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
            
        } catch (ClassNotFoundException e) {
            Logger.getLogger(Reports.class.getName()).log(Level.SEVERE, "MySQL JDBC Driver not found. Ensure the connector JAR is in your project.", e);
            return null; 
        } catch (SQLException e) {
            // Logs detailed error for developer and returns a generic error to the user interface
            Logger.getLogger(Reports.class.getName()).log(Level.SEVERE, "SQL Connection failed. Check URL, USER, and PASSWORD.", e);
            return null; 
        }
    }
    
    /**
     * Populates the supplier Combo Box by querying the database.
     */
    private void populateSupplierCombo() {
        ComboSupplier.removeAllItems();
        ComboSupplier.addItem("All Suppliers");
        
        // Correct SQL using SupplierName from the schema
        String sql = "SELECT DISTINCT SupplierName FROM stockin_log ORDER BY SupplierName";
        try (Connection conn = getConnection();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null;
             ResultSet rs = (pst != null) ? pst.executeQuery() : null) {

            if (conn == null) throw new SQLException("Connection failed.");
            
            if (rs != null) {
                while (rs.next()) {
                    ComboSupplier.addItem(rs.getString("SupplierName"));
                }
            }
        } catch (SQLException e) {
            Logger.getLogger(Reports.class.getName()).log(Level.WARNING, "Error loading suppliers from DB. Using mock data.", e);
            // Fallback mock data if DB fails or is unreachable
            ComboSupplier.addItem("cabbages supplier (MOCK)");
            ComboSupplier.addItem("Drink Distributors (MOCK)");
        }
    }
    
    // =========================================================
    // 2. CONSTRUCTOR & SETUP
    // =========================================================

    public Reports() {
        initComponents();
        setupComponents();
        this.setLocationRelativeTo(null); 
    }
    
    private void setupComponents() {
        
        // Configure JDateChoosers
        jDateFrom.setDateFormatString("yyyy-MM-dd");
        jDateTo.setDateFormatString("yyyy-MM-dd");
        
        // Setup Radio Button Group (Report Scope)
        reportGroup = new ButtonGroup();
        reportGroup.add(summaryReport);
        reportGroup.add(detailedStockInLog);
        reportGroup.add(lowStockList);
        detailedStockInLog.setSelected(true);
        
        // Populate Output Format Combo Box - MODIFIED to remove Excel
        comboOutputFormat.removeAllItems(); 
        comboOutputFormat.addItem("PDF File (.pdf)");
        comboOutputFormat.addItem("CSV File (.csv)");
        
        // Populate Supplier Combo Box
        populateSupplierCombo();
        
        // Attach Action Listener to the Generate button
        btnGenerateReport.addActionListener(this::btnGenerateReportActionPerformed);
    }

    // =========================================================
    // 3. REPORT GENERATION CORE LOGIC
    // =========================================================
    
    private void btnGenerateReportActionPerformed(ActionEvent evt) { 
        
        // 3.1. Input Validation and Data Collection
        String selectedSupplier = (String) ComboSupplier.getSelectedItem();
        Date dateStart = jDateFrom.getDate();
        Date dateEnd = jDateTo.getDate();
        String outputFormat = (String) comboOutputFormat.getSelectedItem();
        
        String reportType = "";
        if (summaryReport.isSelected()) reportType = "Summary Report";
        else if (detailedStockInLog.isSelected()) reportType = "Detailed Stock-In Log";
        else if (lowStockList.isSelected()) reportType = "Low Stock Alert List";
        
        // Validation for date fields (not required for Low Stock Alert)
        if (!reportType.equals("Low Stock Alert List")) { 
            if (dateStart == null || dateEnd == null) {
                JOptionPane.showMessageDialog(this, 
                        "Please select both a 'Date From' and 'To' date.", 
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (dateStart.after(dateEnd)) {
                JOptionPane.showMessageDialog(this, 
                        "'Date From' cannot be after 'Date To'.", 
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        
        String startDateString = (dateStart != null) ? SQL_DATE_FORMAT.format(dateStart) : null;
        String endDateString = (dateEnd != null) ? SQL_DATE_FORMAT.format(dateEnd) : null;

        // 3.2. Generate and Execute SQL Query
        try (Connection conn = getConnection()) {
            
            if (conn == null) {
                // This is the source of the "Database Error" dialog.
                JOptionPane.showMessageDialog(this, "Failed to connect to the database. Cannot generate report.", "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String sql = getReportSQL(reportType, selectedSupplier);
            
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                
                // Set parameters based on the query type
                setStatementParameters(pst, reportType, selectedSupplier, startDateString, endDateString);
                
                // Execute and process result set
                try (ResultSet rs = pst.executeQuery()) {
                    
                    // 3.3. Output File Generation
                    switch (outputFormat) {
                        case "PDF File (.pdf)":
                            createPdfReport(rs, reportType);
                            break;
                        case "CSV File (.csv)":
                            createCsvReport(rs, reportType);
                            break;
                    }
                    
                    JOptionPane.showMessageDialog(this, 
                                    "Report '" + reportType + "' successfully generated!", 
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                    "SQL Query Error: " + e.getMessage(), 
                    "Database Query Error", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(Reports.class.getName()).log(Level.SEVERE, "SQL Error during report generation", e);
        } catch (Exception e) {
            // Check if the error was a user-cancellation (JFileChooser)
            if (e.getMessage() != null && e.getMessage().contains("cancelled by user")) {
                 Logger.getLogger(Reports.class.getName()).log(Level.INFO, "Report generation cancelled by user.");
            } else {
                JOptionPane.showMessageDialog(this, 
                        "An unexpected error occurred during report file creation: " + e.getMessage(), 
                        "System Error", JOptionPane.ERROR_MESSAGE);
                Logger.getLogger(Reports.class.getName()).log(Level.SEVERE, "File Creation Error", e);
            }
        }
    }
    
    // =========================================================
    // 4. SQL QUERY BUILDER & PARAMETER SETTER (Schema-Correct)
    // =========================================================
    
    private String getReportSQL(String reportType, String supplier) {
        String baseSql = "";
        
        // 4.1. Summary Report (Total Stock In/Value)
        if (reportType.equals("Summary Report")) {
            // Uses schema-correct DateTime and QuantityIn
            baseSql = "SELECT DATE_FORMAT(DateTime, '%Y-%m') AS Month, " + 
                      "COUNT(LogID) AS TotalLogs, " + 
                      "SUM(QuantityIn) AS TotalQty, " + 
                      "SUM(UnitCost * QuantityIn) AS TotalValue " + 
                      "FROM stockin_log ";
            
            if (!supplier.equals("All Suppliers")) {
                baseSql += "WHERE SupplierName = ? AND DateTime BETWEEN ? AND ? ";
            } else {
                baseSql += "WHERE DateTime BETWEEN ? AND ? ";
            }
            baseSql += "GROUP BY Month ORDER BY Month";
            
        // 4.2. Detailed Stock-In Log (Product/Supplier/Date)
        } else if (reportType.equals("Detailed Stock-In Log")) {
            // Uses schema-correct DateTime, SupplierName, and QuantityIn
            baseSql = "SELECT DateTime, ProductName, SupplierName, QuantityIn, UnitCost, (QuantityIn * UnitCost) AS TotalCost " +
                      "FROM stockin_log ";
            
            if (!supplier.equals("All Suppliers")) {
                baseSql += "WHERE SupplierName = ? AND DateTime BETWEEN ? AND ? ";
            } else {
                baseSql += "WHERE DateTime BETWEEN ? AND ? ";
            }
            baseSql += "ORDER BY DateTime DESC";
            
        // 4.3. Low Stock Alert List
        } else if (reportType.equals("Low Stock Alert List")) {
            // Uses schema-correct column names from 'products': name, quantity, reorder_point
            baseSql = "SELECT name, quantity, reorder_point " +
                      "FROM products " +
                      "WHERE quantity <= reorder_point AND quantity > 0 " +
                      "ORDER BY quantity ASC";
        }
        
        return baseSql;
    }
    
    private void setStatementParameters(PreparedStatement pst, String reportType, String supplier, String startDate, String endDate) throws SQLException {
        int index = 1;
        
        if (reportType.equals("Summary Report") || reportType.equals("Detailed Stock-In Log")) {
            if (!supplier.equals("All Suppliers")) {
                pst.setString(index++, supplier); 
            }
            // Use full date range for timestamp columns
            pst.setString(index++, startDate + " 00:00:00"); 
            pst.setString(index++, endDate + " 23:59:59");  
        }
        // Low Stock Alert List has no parameters
    }

    // =========================================================
    // 5. FILE GENERATION IMPLEMENTATION (iText and CSV)
    // =========================================================
    
    /**
     * Creates a PDF report using iText 5.5.13.3 from the given ResultSet.
     */
    private void createPdfReport(ResultSet rs, String reportType) throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save PDF Report");
        String defaultFileName = reportType.replace(" ", "") + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf";
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
            }

            // Use A4 Landscape for better data display
            Document document = new Document(PageSize.A4.rotate());

            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                PdfWriter.getInstance(document, fos);
                document.open();

                // 1. Title
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.NORMAL);
                Paragraph title = new Paragraph("Inventory Control System - " + reportType, titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);
                
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                
                // 2. Create Table
                PdfPTable table = new PdfPTable(columnCount);
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);

                // Set widths for better layout (Optional, depends on report type)
                if (reportType.equals("Detailed Stock-In Log")) {
                    float[] columnWidths = {2.5f, 3f, 2f, 1f, 1.5f, 1.5f}; // DateTime, Name, Supplier, Qty, Cost, Total
                    table.setWidths(columnWidths);
                }

                // 3. Table Header
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL);
                for (int i = 1; i <= columnCount; i++) {
                    PdfPCell header = new PdfPCell(new Phrase(rsmd.getColumnLabel(i), headerFont));
                    header.setHorizontalAlignment(Element.ALIGN_CENTER);
                    header.setBackgroundColor(new com.itextpdf.text.BaseColor(200, 200, 200));
                    header.setPadding(5);
                    table.addCell(header);
                }

                // 4. Table Data
                Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
                boolean hasData = false;
                while (rs.next()) {
                    hasData = true;
                    for (int i = 1; i <= columnCount; i++) {
                        PdfPCell cell = new PdfPCell(new Phrase(rs.getString(i), cellFont));
                        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        cell.setPadding(3);
                        table.addCell(cell);
                    }
                }
                
                // 5. Add table or no data message
                if (hasData) {
                    document.add(table);
                } else {
                    document.add(new Paragraph("No data found for the selected criteria.", cellFont));
                }

                document.close();
                
            } catch (Exception e) {
                if (document.isOpen()) {
                    document.close();
                }
                Logger.getLogger(Reports.class.getName()).log(Level.SEVERE, "Error creating PDF file.", e);
                throw new Exception("Failed to create PDF file: " + e.getMessage());
            }
        } else {
            throw new Exception("File save operation cancelled by user.");
        }
    }
    
    /**
     * Creates a CSV report from the given ResultSet.
     */
    private void createCsvReport(ResultSet rs, String reportType) throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save CSV Report");
        String defaultFileName = reportType.replace(" ", "") + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (FileWriter fw = new FileWriter(fileToSave);
                 BufferedWriter bw = new BufferedWriter(fw)) {

                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                // 1. Write CSV Header (Column Names)
                for (int i = 1; i <= columnCount; i++) {
                    bw.write(rsmd.getColumnLabel(i));
                    if (i < columnCount) {
                        bw.write(",");
                    }
                }
                bw.newLine();

                // 2. Write CSV Data (Rows)
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        String columnValue = rs.getString(i);
                        
                        // Basic CSV sanitization (Handling commas/quotes in data)
                        if (columnValue != null) {
                            columnValue = columnValue.replace("\"", "\"\""); // Escape double quotes
                            if (columnValue.contains(",") || columnValue.contains("\"") || columnValue.contains("\n")) {
                                columnValue = "\"" + columnValue + "\""; // Enclose in quotes
                            }
                        } else {
                            columnValue = "";
                        }
                        bw.write(columnValue);
                        if (i < columnCount) {
                            bw.write(",");
                        }
                    }
                    bw.newLine();
                }

            } catch (IOException e) {
                Logger.getLogger(Reports.class.getName()).log(Level.SEVERE, "Error saving CSV file.", e);
                throw new Exception("Failed to write CSV file: " + e.getMessage());
            }
        } else {
            throw new Exception("File save operation cancelled by user.");
        }
    }
    
    // The createExcelReport method has been removed as per request to only support PDF and CSV.
    
    // =========================================================
    // 6. EVENT HANDLERS
    // =========================================================

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose(); 
    }

    private void ComboSupplierActionPerformed(java.awt.event.ActionEvent evt) {
        // Optional logic when supplier changes
    }
    
    // =========================================================
    // 7. NETBEANS GENERATED CODE (DO NOT MODIFY)
    // =========================================================

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
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        ComboSupplier = new javax.swing.JComboBox<>();
        supplier = new javax.swing.JLabel();
        dateFrom = new javax.swing.JLabel();
        dateTo = new javax.swing.JLabel();
        summaryReport = new javax.swing.JRadioButton();
        detailedStockInLog = new javax.swing.JRadioButton();
        lowStockList = new javax.swing.JRadioButton();
        outputFormat = new javax.swing.JLabel();
        comboOutputFormat = new javax.swing.JComboBox<>();
        btnGenerateReport = new javax.swing.JButton();
        jDateFrom = new com.toedter.calendar.JDateChooser();
        jDateTo = new com.toedter.calendar.JDateChooser();
        jButton1 = new javax.swing.JButton();

        jRadioButton1.setText("jRadioButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 204, 204));

        jPanel2.setBackground(new java.awt.Color(255, 102, 102));

        jLabel2.setFont(new java.awt.Font("Monospaced", 1, 36)); // NOI18N
        jLabel2.setText("GENERATE REPORTS");

        jLabel1.setFont(new java.awt.Font("Malgun Gothic", 1, 36)); // NOI18N
        jLabel1.setText("INVENTORY CONTROL SYSTEM");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 563, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(107, 107, 107)))
                .addGap(80, 80, 80))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(0, 12, Short.MAX_VALUE))
        );

        jLabel3.setFont(new java.awt.Font("Impact", 0, 18)); // NOI18N
        jLabel3.setText("FILTER OPTIONS");

        ComboSupplier.setToolTipText("");
        ComboSupplier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ComboSupplierActionPerformed(evt);
            }
        });

        supplier.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        supplier.setText("SUPPLIER");

        dateFrom.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        dateFrom.setText("DATE FROM:");

        dateTo.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        dateTo.setText("TO:");

        summaryReport.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        summaryReport.setText("Summary Report (Total Stock In/Value) ");

        detailedStockInLog.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        detailedStockInLog.setText("Detailed Stock-In Log (Product/Supplier/Date)");

        lowStockList.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lowStockList.setText("Low Stock Alert List ");

        outputFormat.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        outputFormat.setText("Output Format");

        comboOutputFormat.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        comboOutputFormat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnGenerateReport.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnGenerateReport.setText("GENERATE REPORT");

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jButton1.setText("BACK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGap(9, 9, 9)
                                    .addComponent(outputFormat, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(comboOutputFormat, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(lowStockList, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(detailedStockInLog, javax.swing.GroupLayout.PREFERRED_SIZE, 448, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(supplier, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(ComboSupplier, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(26, 26, 26)
                                        .addComponent(dateFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(91, 91, 91)
                                        .addComponent(dateTo, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jDateFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(43, 43, 43)
                                        .addComponent(jDateTo, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnGenerateReport)))
                        .addGap(20, 20, 20))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(summaryReport, javax.swing.GroupLayout.PREFERRED_SIZE, 409, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 83, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(62, 62, 62))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(supplier)
                    .addComponent(ComboSupplier, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(dateFrom)
                            .addComponent(dateTo))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jDateFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jDateTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(btnGenerateReport, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 19, 19)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(summaryReport)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addComponent(detailedStockInLog)
                .addGap(18, 18, 18)
                .addComponent(lowStockList)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputFormat)
                    .addComponent(comboOutputFormat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        home rep = new home(); // Opens your inventory/stock view screen
        rep.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

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
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Reports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Reports().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> ComboSupplier;
    private javax.swing.JButton btnGenerateReport;
    private javax.swing.JComboBox<String> comboOutputFormat;
    private javax.swing.JLabel dateFrom;
    private javax.swing.JLabel dateTo;
    private javax.swing.JRadioButton detailedStockInLog;
    private javax.swing.JButton jButton1;
    private com.toedter.calendar.JDateChooser jDateFrom;
    private com.toedter.calendar.JDateChooser jDateTo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton lowStockList;
    private javax.swing.JLabel outputFormat;
    private javax.swing.JRadioButton summaryReport;
    private javax.swing.JLabel supplier;
    // End of variables declaration//GEN-END:variables
}