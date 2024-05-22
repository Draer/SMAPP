package javaapplication18;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class JavaApplication18 {
    private JFrame frame;
    private JComboBox<String> tableComboBox;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private JComboBox<String> columnFilterComboBox;
    private JPopupMenu contextMenu;
    private Connection connection;

    public JavaApplication18() {
        initialize();
        connectToDatabase();
        ExecutorService executorService = Executors.newFixedThreadPool(2);//Размер пула установлен на 2
        executorService.execute(() -> loadTableData());
    }
    
    private void connectToDatabase() {
        String url = "jdbc:mysql://sql.freedb.tech:3306/freedb_smapp491?useSSL=false&allowPublicKeyRetrieval=true";
        String username = "freedb_pryme491";
        String password = "8WE8Px#84P3Yy?2";
        try {
            // Register MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connected to the database");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "MySQL JDBC driver not found.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error connecting to the database: " + e.getMessage());
        }
    }

    private void initialize() {
        // Используем Material Design Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        frame = new JFrame("Sales Manager System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        searchField = new JTextField();
        searchField.setColumns(15);
        searchField.addActionListener(e -> performSearch());
        
        columnFilterComboBox = new JComboBox<>();
        columnFilterComboBox.addActionListener(e -> performSearch());
        
        JMenuItem editMenuItem = new JMenuItem("Изменить");
        editMenuItem.addActionListener(e -> {
            try {
                editSelectedRecord();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error editing record.");
            }
        });
        JMenuItem deleteMenuItem = new JMenuItem("Удалить");
        deleteMenuItem.addActionListener(e -> {
            try {
                deleteSelectedRecord();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error deleting record.");
            }
        });
        
        JMenuItem addMenuItem = new JMenuItem("Добавить");
        addMenuItem.addActionListener(e -> {
            try {
                showAddRecordDialog();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при добавлении записи.");
            }
        });
        
        contextMenu = new JPopupMenu();
        contextMenu.add(addMenuItem);
        contextMenu.add(editMenuItem);
        contextMenu.add(deleteMenuItem);
        
        JPanel controlPanel = new JPanel();
        tableComboBox = new JComboBox<>(new String[]{"products", "suppliers","customers","orders"});

        ImageIcon loadicon = createImageIcon("https://cdn-icons-png.flaticon.com/512/72/72651.png", 25, 15);
        JButton loadButton = new JButton("Загрузить данные", loadicon);
        loadButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                loadTableData();
            });
        });

        ImageIcon clearicon = createImageIcon("https://cdn2.iconfinder.com/data/icons/harmonicons-03/64/delete-512.png", 20, 15);
        Image scaledClearImage = clearicon.getImage().getScaledInstance(20, 15, Image.SCALE_DEFAULT);
        clearicon.setImage(scaledClearImage);
        JButton clearButton = new JButton("Очистить таблицу",clearicon);
        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(null, "Вы уверены, что хотите очистить таблицу?", "Подтверждение", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                clearTable();
            }
        });
        
        controlPanel.add(new JLabel("Таблиц:"));
        controlPanel.add(tableComboBox);
        controlPanel.add(loadButton);
        controlPanel.add(clearButton);
        controlPanel.add(new JLabel("Фильтр по столбцу:"));
        controlPanel.add(columnFilterComboBox);
        controlPanel.add(new JLabel("Поиск:"));
        controlPanel.add(searchField);
        
        frame.add(controlPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        dataTable = new JTable(tableModel);
        
        dataTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = dataTable.rowAtPoint(e.getPoint());
                    dataTable.clearSelection(); // Clear any existing selection
                    if (row >= 0 && row < dataTable.getRowCount()) {
                        dataTable.setRowSelectionInterval(row, row);
                    }
                    contextMenu.show(dataTable, e.getX(), e.getY());
                }
            }
        });
        
        sorter = new TableRowSorter<>(tableModel); // Инициализация TableRowSorter
        dataTable.setRowSorter(sorter); // Установка TableRowSorter для JTable
        
        JScrollPane scrollPane = new JScrollPane(dataTable);
        frame.add(scrollPane, BorderLayout.CENTER);

        ImageIcon addicon = createImageIcon("https://icons.iconarchive.com/icons/iconsmind/outline/256/Add-File-icon.png", 15, 15);
        JButton exportButton = new JButton("Экспорт");
        exportButton.addActionListener(e -> {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showSaveDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();
                createTextFileFromTable(selectedTable, filePath);
            }
        });
        controlPanel.add(exportButton);
        
        JButton addButton = new JButton("Добавить запись", addicon);
        addButton.addActionListener(e -> {
            try {
                showAddRecordDialog();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при добавлении записи.");
            }
        });
        frame.add(addButton, BorderLayout.SOUTH);
        
        frame.setSize(1100, 400);
        frame.setVisible(true);
    }

    private void deleteSelectedRecord() throws SQLException {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow >= 0) {
            String selectedTable = (String) tableComboBox.getSelectedItem();

            ResultSetMetaData metaData = getTableMetaData(selectedTable);
            StringBuilder whereClause = new StringBuilder();

            if (metaData != null) {
                try {
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object columnValue = dataTable.getValueAt(selectedRow, i - 1);

                        // Check if the value is null
                        if (columnValue != null) {
                            if (whereClause.length() > 0) {
                                whereClause.append(" AND ");
                            }
                            whereClause.append(columnName).append(" = ?");
                        } else {
                            // Handle NULL values
                            if (whereClause.length() > 0) {
                                whereClause.append(" AND ");
                            }
                            whereClause.append(columnName).append(" IS NULL");
                        }
                    }

                    String query = "DELETE FROM " + selectedTable + " WHERE " + whereClause.toString();

                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        int parameterIndex = 1;

                        // Set parameters for the prepared statement
                        for (int i = 1; i <= columnCount; i++) {
                            Object columnValue = dataTable.getValueAt(selectedRow, i - 1);

                            // Check if the value is null
                            if (columnValue != null) {
                                preparedStatement.setObject(parameterIndex++, columnValue);
                            }
                            // No need to set parameters for NULL values
                        }

                        preparedStatement.executeUpdate();
                        JOptionPane.showMessageDialog(null, "Запись успешно удалена.");
                        loadTableData(); // Update the table after deleting the record
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Ошибка при удалении записи.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Ошибка при получении метаданных таблицы.");
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Выберите запись для удаления.");
        }
    }
    
    private void loadTableData() {
        if (connection == null) {
            JOptionPane.showMessageDialog(null, "Not connected to the database.");
            return;
        }

        String selectedTable = (String) tableComboBox.getSelectedItem();
        String query = "SELECT * FROM " + selectedTable;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Initialize columnFilterComboBox here
            columnFilterComboBox.removeAllItems();
            columnFilterComboBox.addItem("All");

            tableModel.setColumnCount(0);
            tableModel.setRowCount(0);

            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(metaData.getColumnName(i));
                columnFilterComboBox.addItem(metaData.getColumnName(i));
            }

            while (resultSet.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = resultSet.getObject(i);
                }
                tableModel.addRow(row);
            }

            // Call performSearch after loading data
            performSearch();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading data from the database.");
        }
    }

    
    private ResultSetMetaData getTableMetaData(String tableName) throws SQLException {
        String query = "SELECT * FROM " + tableName + " WHERE 1 = 0";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        return resultSet.getMetaData();
    }
    
    private void showAddRecordDialog() throws SQLException {
        String selectedTable = (String) tableComboBox.getSelectedItem();
        ResultSetMetaData metaData = getTableMetaData(selectedTable);

        if (metaData != null) {
            try {
                int columnCount = metaData.getColumnCount();

                // Separate lists to keep track of column names and values
                List<String> columnNamesList = new ArrayList<>();
                List<String> valuesList = new ArrayList<>();

                JPanel panel = new JPanel(new GridLayout(columnCount, 2));

                Font cyrillicFont = new Font("Arial", Font.PLAIN, 12); // Change the font to one that supports Cyrillic characters

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);

                    // Ignore the ID column
                    if (!columnName.equalsIgnoreCase("ID")) {
                        JTextField textField = new JTextField();
                        textField.setFont(cyrillicFont); // Set the font to support Cyrillic characters
                        JLabel label = new JLabel(" " + columnName + ":");
                        panel.add(label);
                        panel.add(textField);

                        // Add column name to the list
                        columnNamesList.add(columnName);
                    }
                }

                int result = JOptionPane.showConfirmDialog(null, panel, "Введите данные",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    StringBuilder columnNamesForQuery = new StringBuilder();
                    StringBuilder valuesForQuery = new StringBuilder();

                    Component[] components = panel.getComponents();
                    for (int i = 1; i < components.length; i += 2) {
                        JTextField textField = (JTextField) components[i];
                        String columnValue = textField.getText().trim();

                        if (columnNamesForQuery.length() > 0) {
                            columnNamesForQuery.append(", ");
                            valuesForQuery.append(", ");
                        }

                        // Use the column names list to construct the SQL query
                        columnNamesForQuery.append(columnNamesList.get(i / 2));
                        valuesForQuery.append("?");
                    }

                    if (columnNamesForQuery.length() > 0) {
                        String query = "INSERT INTO " + selectedTable + " (" + columnNamesForQuery.toString() + ") VALUES (" + valuesForQuery.toString() + ")";
                        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                            int parameterIndex = 1;

                            for (int i = 1; i < components.length; i += 2) {
                                JTextField textField = (JTextField) components[i];
                                String columnValue = textField.getText().trim();
                                preparedStatement.setString(parameterIndex++, columnValue);
                            }

                            preparedStatement.executeUpdate();
                            JOptionPane.showMessageDialog(null, "Запись успешно добавлена.");
                            loadTableData(); // Update the table after adding the record
                        } catch (SQLException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Ошибка при добавлении записи: " + e.getMessage());
                        }
                    } else {
                        // Handle the case where no values are entered (optional)
                    }
                } else {
                    // User canceled the operation
                    JOptionPane.showMessageDialog(null, "Добавление записи отменено.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка при добавлении записи.");
            }
        }
    }

    private void clearTable() {
        String selectedTable = (String) tableComboBox.getSelectedItem();
        String query = "DELETE FROM " + selectedTable;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
            JOptionPane.showMessageDialog(null, "Таблица успешно очищена.");
            loadTableData(); // Обновляем таблицу после очистки
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Ошибка при очистке таблицы.");
        }
    }
    
    private ImageIcon createImageIcon(String path, int width, int height) {
        try {
            URL url = new URL(path);
            ImageIcon icon = new ImageIcon(url);
            Image image = icon.getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT);
            return new ImageIcon(image);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void performSearch() {
        String searchText = searchField.getText();
        String selectedColumn = (String) columnFilterComboBox.getSelectedItem();

        RowFilter<DefaultTableModel, Object> rowFilter;

        // Check if "Все" is selected
        if (selectedColumn == null || selectedColumn.equals("Все")) {
            rowFilter = RowFilter.regexFilter("(?iu)" + Pattern.quote(searchText));
        } else {
            int columnIndex = columnFilterComboBox.getSelectedIndex() - 1;

            // Check if the selected index is valid
            if (columnIndex >= 0 && columnIndex < tableModel.getColumnCount()) {
                rowFilter = RowFilter.regexFilter("(?iu)" + Pattern.quote(searchText), columnIndex);
            } else {
                rowFilter = RowFilter.regexFilter("(?iu)" + Pattern.quote(searchText));
            }
        }

        sorter.setRowFilter(rowFilter);
    }

    private void editSelectedRecord() throws SQLException {
        int selectedRow = dataTable.getSelectedRow();

        // Check if a row is selected
        if (selectedRow >= 0) {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            ResultSetMetaData metaData = getTableMetaData(selectedTable);

            // Check if metadata is available
            if (metaData != null) {
                try {
                    int columnCount = metaData.getColumnCount();

                    // Separate lists to keep track of column names and values
                    List<String> columnNamesList = new ArrayList<>();
                    List<String> valuesList = new ArrayList<>();

                    JPanel panel = new JPanel(new GridLayout(columnCount, 2));

                    Font cyrillicFont = new Font("Arial", Font.PLAIN, 12); // Change the font to one that supports Cyrillic characters

                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);

                        // Ignore the ID column
                        if (!columnName.equalsIgnoreCase("ID")) {
                            Object cellValue = dataTable.getValueAt(selectedRow, i - 1);

                            JTextField textField = new JTextField(String.valueOf(cellValue));
                            textField.setFont(cyrillicFont); // Set the font to support Cyrillic characters

                            JLabel label = new JLabel(" " + columnName + ":");
                            panel.add(label);
                            panel.add(textField);

                            // Add column name to the list
                            columnNamesList.add(columnName);
                            valuesList.add(String.valueOf(cellValue));
                        }
                    }

                    int result = JOptionPane.showConfirmDialog(null, panel, "Изменить запись",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        StringBuilder updateClause = new StringBuilder();
                        boolean changesMade = false;

                        // Iterate through the panel components
                        for (int i = 0; i < panel.getComponentCount(); i += 2) {
                            JLabel label = (JLabel) panel.getComponent(i);
                            JTextField textField = (JTextField) panel.getComponent(i + 1);

                            // Extract column name from label text
                            String columnName = label.getText().trim().replace(":", "");

                            // Find the index of the column name in the list
                            int columnIndex = columnNamesList.indexOf(columnName);

                            if (columnIndex != -1) {
                                String updatedValue = textField.getText().trim();

                                // Check if the value has changed
                                if (!Objects.equals(updatedValue, valuesList.get(columnIndex))) {
                                    changesMade = true;
                                }

                                // Check if the updated value is not empty
                                if (!updatedValue.isEmpty()) {
                                    if (updateClause.length() > 0) {
                                        updateClause.append(", ");
                                    }

                                    // Use the N prefix for string literals
                                    updateClause.append(columnName).append(" = ?");
                                }
                            } else {
                                // Handle the case where the column name is not found
                                System.err.println("Название стобца не найдено: " + columnName);
                            }
                        }

                        // Check if changes were made
                        if (changesMade) {
                            String query = "UPDATE " + selectedTable + " SET " + updateClause.toString() +
                                    " WHERE ID = ?";

                            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                                // Set parameters for the prepared statement
                                int parameterIndex = 1;
                                for (int i = 0; i < panel.getComponentCount(); i += 2) {
                                    JTextField textField = (JTextField) panel.getComponent(i + 1);
                                    String updatedValue = textField.getText().trim();
                                    preparedStatement.setObject(parameterIndex++, updatedValue);
                                }

                                // Set ID parameter using N prefix
                                preparedStatement.setObject(parameterIndex, dataTable.getValueAt(selectedRow, 0).toString());

                                preparedStatement.executeUpdate();
                                JOptionPane.showMessageDialog(null, "Запись успешно изменена.");

                                // Reload the table data after updating
                                loadTableData();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(null, "Ошибка редактирования записи: " + ex.getMessage());
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Нет изменеий записи.");
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Ошибка получения метадаты из таблицы.");
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Выберите запись для редактирования.");
        }
    }
    private void createTextFileFromTable(String tableName, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            String query = "SELECT * FROM " + tableName;
            try (ResultSet resultSet = connection.createStatement().executeQuery(query)) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Write column names to the file
                for (int i = 1; i <= columnCount; i++) {
                    writer.write(metaData.getColumnName(i));
                    if (i < columnCount) {
                        writer.write("\t");
                    } else {
                        writer.write("\n");
                    }
                }

                // Write data rows to the file
                while (resultSet.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        writer.write(resultSet.getString(i));
                        if (i < columnCount) {
                            writer.write("\t");
                        } else {
                            writer.write("\n");
                        }
                    }
                }

                System.out.println("Text file created successfully.");
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error creating text file: " + e.getMessage());
        }
    }

    
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        UIManager.put("OptionPane.messageCharset", "UTF-8");
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new JavaApplication18());
    }
}
