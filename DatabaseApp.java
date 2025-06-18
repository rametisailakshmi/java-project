import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseApp extends Application {

    // --- DBUtil Class ---
    public static class DBUtil {

        // !!! IMPORTANT: CHANGE THESE TO YOUR ACTUAL MySQL CREDENTIALS !!!
        private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";
        private static final String DB_USER = "root";
        private static final String DB_PASSWORD = "12345";

        public static Connection getConnection() throws SQLException {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found.");
                e.printStackTrace();
                throw new SQLException("Database driver not found.", e);
            }
        }

        public static void closeConnection(Connection connection) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    // --- End DBUtil Class ---

    private ToggleGroup operationGroup;
    private BorderPane root;
    private VBox contentPane;
    private Label statusLabel;

    private TableView<DynamicRow> dataTable;
    private ComboBox<String> selectTableComboBox;
    private ObservableList<DynamicRow> tableData;

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        root.setPadding(new Insets(10));

        // --- Radio Buttons for Operations ---
        operationGroup = new ToggleGroup();

        RadioButton createRadio = new RadioButton("Create Table");
        createRadio.setToggleGroup(operationGroup);
        createRadio.setOnAction(e -> showCreateUI());
        createRadio.setStyle("-fx-text-fill: darkblue;");

        RadioButton insertRadio = new RadioButton("Insert Record");
        insertRadio.setToggleGroup(operationGroup);
        insertRadio.setOnAction(e -> showInsertUI());
        insertRadio.setStyle("-fx-text-fill: darkgreen;");

        RadioButton updateRadio = new RadioButton("Update Record");
        updateRadio.setToggleGroup(operationGroup);
        updateRadio.setOnAction(e -> showUpdateUI());
        updateRadio.setStyle("-fx-text-fill: darkorange;");

        RadioButton deleteRadio = new RadioButton("Delete Record");
        deleteRadio.setToggleGroup(operationGroup);
        deleteRadio.setOnAction(e -> showDeleteUI());
        deleteRadio.setStyle("-fx-text-fill: darkred;");

        RadioButton selectRadio = new RadioButton("Select/Manage Records");
        selectRadio.setToggleGroup(operationGroup);
        selectRadio.setOnAction(e -> showSelectUI());
        selectRadio.setStyle("-fx-text-fill: darkviolet;");

        HBox radioButtonsBox = new HBox(15, createRadio, insertRadio, updateRadio, deleteRadio, selectRadio);
        radioButtonsBox.setPadding(new Insets(10));
        radioButtonsBox.setStyle("-fx-background-color: lightgray; -fx-border-color: gray; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        root.setTop(radioButtonsBox);

        // --- Content Pane (initially empty) ---
        contentPane = new VBox(10);
        contentPane.setPadding(new Insets(10));
        root.setCenter(contentPane); // Keep it in the center, just don't put content in it initially

        // --- Status Label ---
        statusLabel = new Label("Choose an operation to begin.");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: blue;");
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("MySQL Database Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        // No default radio button selection here, so the content pane starts empty
    }

    // --- Helper for Status Messages ---
    private void updateStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    // --- UI Methods (Implementations remain the same, just called by radio buttons) ---

    private void showCreateUI() {
        contentPane.getChildren().clear(); // Clear previous content
        contentPane.setStyle("-fx-background-color: lightyellow;");

        VBox createBox = new VBox(10);
        createBox.setPadding(new Insets(10));

        Label tableNameLabel = new Label("Table Name:");
        TextField tableNameField = new TextField();
        tableNameField.setPromptText("e.g., Users");

        Label columnsLabel = new Label("Columns (name type, e.g., id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255)):");
        TextArea columnsArea = new TextArea();
        columnsArea.setPromptText("Enter column definitions separated by commas (e.g., id INT PRIMARY KEY, name VARCHAR(255), age INT)");
        columnsArea.setPrefRowCount(5);

        Button createButton = new Button("Create Table");
        createButton.setStyle("-fx-background-color: darkblue; -fx-text-fill: white; -fx-font-weight: bold;");
        createButton.setOnAction(e -> {
            String tableName = tableNameField.getText().trim();
            String columns = columnsArea.getText().trim();
            if (tableName.isEmpty() || columns.isEmpty()) {
                updateStatus("Table name and columns cannot be empty.", "red");
                return;
            }
            createTable(tableName, columns);
        });

        createBox.getChildren().addAll(tableNameLabel, tableNameField, columnsLabel, columnsArea, createButton);
        contentPane.getChildren().add(createBox);
    }

    private void showInsertUI() {
        contentPane.getChildren().clear();
        contentPane.setStyle("-fx-background-color: lightgreen;");

        VBox insertBox = new VBox(10);
        insertBox.setPadding(new Insets(10));

        ComboBox<String> tableComboBox = new ComboBox<>();
        tableComboBox.setPromptText("Select Table");
        populateTableComboBox(tableComboBox);

        Label dataLabel = new Label("Data (comma-separated values matching column order):");
        TextField dataField = new TextField();
        dataField.setPromptText("e.g., 'John Doe', 30, 'john@example.com'");

        Button insertButton = new Button("Insert Record");
        insertButton.setStyle("-fx-background-color: darkgreen; -fx-text-fill: white; -fx-font-weight: bold;");
        insertButton.setOnAction(e -> {
            String selectedTable = tableComboBox.getSelectionModel().getSelectedItem();
            String data = dataField.getText().trim();
            if (selectedTable == null || selectedTable.isEmpty() || data.isEmpty()) {
                updateStatus("Please select a table and enter data.", "red");
                return;
            }
            insertRecord(selectedTable, data);
        });

        insertBox.getChildren().addAll(new Label("Select Table for Insertion:"), tableComboBox, dataLabel, dataField, insertButton);
        contentPane.getChildren().add(insertBox);
    }

    private void showUpdateUI() {
        contentPane.getChildren().clear();
        contentPane.setStyle("-fx-background-color: lightgoldenrodyellow;");

        VBox updateBox = new VBox(10);
        updateBox.setPadding(new Insets(10));

        ComboBox<String> tableComboBox = new ComboBox<>();
        tableComboBox.setPromptText("Select Table");
        populateTableComboBox(tableComboBox);

        Label setLabel = new Label("SET (e.g., name = 'Jane Doe', age = 31):");
        TextField setField = new TextField();
        setField.setPromptText("column = 'new_value', another_column = new_value");

        Label whereLabel = new Label("WHERE Clause (e.g., id = 1):");
        TextField whereField = new TextField();
        whereField.setPromptText("condition (e.g., id = 1 or name = 'old_name')");

        Button updateButton = new Button("Update Record");
        updateButton.setStyle("-fx-background-color: darkorange; -fx-text-fill: white; -fx-font-weight: bold;");
        updateButton.setOnAction(e -> {
            String selectedTable = tableComboBox.getSelectionModel().getSelectedItem();
            String setClause = setField.getText().trim();
            String whereClause = whereField.getText().trim();
            if (selectedTable == null || selectedTable.isEmpty() || setClause.isEmpty() || whereClause.isEmpty()) {
                updateStatus("Please select a table and provide SET/WHERE clauses.", "red");
                return;
            }
            updateRecord(selectedTable, setClause, whereClause);
        });

        updateBox.getChildren().addAll(new Label("Select Table for Update:"), tableComboBox, setLabel, setField, whereLabel, whereField, updateButton);
        contentPane.getChildren().add(updateBox);
    }

    private void showDeleteUI() {
        updateStatus("Deletion is managed via checkboxes in the 'Select/Manage Records' section.", "orange");
        showSelectUI();
    }

    private void showSelectUI() {
        contentPane.getChildren().clear();
        contentPane.setStyle("-fx-background-color: lightblue;");

        VBox selectBox = new VBox(10);
        selectBox.setPadding(new Insets(10));

        selectTableComboBox = new ComboBox<>();
        selectTableComboBox.setPromptText("Select Table to View/Manage");
        populateTableComboBox(selectTableComboBox);
        selectTableComboBox.setOnAction(e -> loadTableData(selectTableComboBox.getSelectionModel().getSelectedItem()));

        dataTable = new TableView<>();
        dataTable.setPlaceholder(new Label("Select a table to view its records."));

        Button deleteSelectedButton = new Button("Delete Selected Records");
        deleteSelectedButton.setStyle("-fx-background-color: darkred; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteSelectedButton.setOnAction(e -> deleteSelectedRecords());

        selectBox.getChildren().addAll(selectTableComboBox, dataTable, deleteSelectedButton);
        contentPane.getChildren().add(selectBox);
    }

    // --- Database Operations (remain the same) ---

    private void createTable(String tableName, String columns) {
        String sql = "CREATE TABLE " + tableName + " (" + columns + ")";
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            updateStatus("Table '" + tableName + "' created successfully!", "green");
            populateTableComboBox(selectTableComboBox);
            ComboBox<String> insertTableCb = findComboBox(contentPane, "Select Table for Insertion:");
            if (insertTableCb != null) populateTableComboBox(insertTableCb);
            ComboBox<String> updateTableCb = findComboBox(contentPane, "Select Table for Update:");
            if (updateTableCb != null) populateTableComboBox(updateTableCb);

        } catch (SQLException e) {
            updateStatus("Error creating table: " + e.getMessage(), "red");
            e.printStackTrace();
        } finally {
            DBUtil.closeConnection(conn);
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    private ComboBox<String> findComboBox(VBox parent, String labelText) {
        for (javafx.scene.Node node : parent.getChildren()) {
            if (node instanceof VBox) {
                for (javafx.scene.Node innerNode : ((VBox) node).getChildren()) {
                    if (innerNode instanceof Label && ((Label) innerNode).getText().equals(labelText)) {
                        int index = ((VBox) node).getChildren().indexOf(innerNode);
                        if (index + 1 < ((VBox) node).getChildren().size() && ((VBox) node).getChildren().get(index + 1) instanceof ComboBox) {
                            return (ComboBox<String>) ((VBox) node).getChildren().get(index + 1);
                        } else if (index + 2 < ((VBox) node).getChildren().size() && ((VBox) node).getChildren().get(index + 2) instanceof ComboBox) {
                            return (ComboBox<String>) ((VBox) node).getChildren().get(index + 2);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void insertRecord(String tableName, String data) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rsColumns = metaData.getColumns(null, null, tableName, null);

            StringBuilder columnsPart = new StringBuilder();
            StringBuilder valuesPart = new StringBuilder();
            boolean firstColumn = true;
            int columnCount = 0;
            while(rsColumns.next()) {
                if (!firstColumn) {
                    columnsPart.append(", ");
                    valuesPart.append(", ");
                }
                columnsPart.append(rsColumns.getString("COLUMN_NAME"));
                valuesPart.append("?");
                firstColumn = false;
                columnCount++;
            }
            rsColumns.close();

            String sql = "INSERT INTO " + tableName + " (" + columnsPart.toString() + ") VALUES (" + valuesPart.toString() + ")";
            pstmt = conn.prepareStatement(sql);

            String[] values = data.split(",");
            if (values.length != columnCount) {
                updateStatus("Mismatch in data values provided and actual column count. Expected " + columnCount + " values.", "red");
                return;
            }

            for (int i = 0; i < values.length; i++) {
                String cleanedValue = values[i].trim();
                if (cleanedValue.startsWith("'") && cleanedValue.endsWith("'")) {
                    cleanedValue = cleanedValue.substring(1, cleanedValue.length() - 1);
                }
                pstmt.setString(i + 1, cleanedValue);
            }

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                updateStatus("Record inserted successfully into '" + tableName + "'!", "green");
                if (selectTableComboBox.getSelectionModel().getSelectedItem() != null && selectTableComboBox.getSelectionModel().getSelectedItem().equals(tableName)) {
                    loadTableData(tableName);
                }
            } else {
                updateStatus("Failed to insert record into '" + tableName + "'.", "orange");
            }
        } catch (SQLException e) {
            updateStatus("Error inserting record: " + e.getMessage(), "red");
            e.printStackTrace();
        } finally {
            DBUtil.closeConnection(conn);
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    private void updateRecord(String tableName, String setClause, String whereClause) {
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();
            int rowsAffected = stmt.executeUpdate(sql);
            if (rowsAffected > 0) {
                updateStatus(rowsAffected + " record(s) updated successfully in '" + tableName + "'!", "green");
                if (selectTableComboBox.getSelectionModel().getSelectedItem() != null && selectTableComboBox.getSelectionModel().getSelectedItem().equals(tableName)) {
                    loadTableData(tableName);
                }
            } else {
                updateStatus("No records updated in '" + tableName + "'. Check WHERE clause.", "orange");
            }
        } catch (SQLException e) {
            updateStatus("Error updating record: " + e.getMessage(), "red");
            e.printStackTrace();
        } finally {
            DBUtil.closeConnection(conn);
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    private void deleteSelectedRecords() {
        String tableName = selectTableComboBox.getSelectionModel().getSelectedItem();
        if (tableName == null || tableName.isEmpty()) {
            updateStatus("Please select a table first.", "red");
            return;
        }

        ObservableList<DynamicRow> itemsToDelete = FXCollections.observableArrayList();
        for (DynamicRow row : dataTable.getItems()) {
            if (row.isChecked()) {
                itemsToDelete.add(row);
            }
        }

        if (itemsToDelete.isEmpty()) {
            updateStatus("No records selected for deletion.", "orange");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rsPrimaryKeys = metaData.getPrimaryKeys(null, null, tableName);
            String primaryKeyColumn = null;
            if (rsPrimaryKeys.next()) {
                primaryKeyColumn = rsPrimaryKeys.getString("COLUMN_NAME");
            } else {
                ResultSet rsColumns = metaData.getColumns(null, null, tableName, null);
                while(rsColumns.next()) {
                    String colName = rsColumns.getString("COLUMN_NAME").toLowerCase();
                    if (colName.equals("id") || colName.endsWith("_id")) {
                        primaryKeyColumn = rsColumns.getString("COLUMN_NAME");
                        break;
                    }
                }
                rsColumns.close();
            }
            rsPrimaryKeys.close();

            if (primaryKeyColumn == null) {
                updateStatus("Cannot delete: No clear primary key found for table '" + tableName + "'. Manual WHERE clause required.", "red");
                conn.rollback();
                return;
            }

            Map<String, Integer> columnNameToIndexMap = new LinkedHashMap<>();
            ResultSetMetaData rsmdForPK = conn.createStatement().executeQuery("SELECT * FROM " + tableName + " LIMIT 0").getMetaData();
            for (int i = 1; i <= rsmdForPK.getColumnCount(); i++) {
                columnNameToIndexMap.put(rsmdForPK.getColumnName(i).toLowerCase(), i);
            }

            Integer pkColumnJdbcIndex = columnNameToIndexMap.get(primaryKeyColumn.toLowerCase());
            if (pkColumnJdbcIndex == null) {
                updateStatus("Primary key column '" + primaryKeyColumn + "' not found in table metadata.", "red");
                conn.rollback();
                return;
            }

            String sql = "DELETE FROM " + tableName + " WHERE " + primaryKeyColumn + " = ?";
            pstmt = conn.prepareStatement(sql);

            int deletedCount = 0;
            for (DynamicRow row : itemsToDelete) {
                String pkValue = row.getColumnValue(primaryKeyColumn);

                if (pkValue == null) {
                    updateStatus("Could not retrieve primary key value for a row selected for deletion.", "orange");
                    continue;
                }

                try {
                    int intValue = Integer.parseInt(pkValue);
                    pstmt.setInt(1, intValue);
                } catch (NumberFormatException e) {
                    pstmt.setString(1, pkValue);
                }
                deletedCount += pstmt.executeUpdate();
            }

            conn.commit();
            updateStatus(deletedCount + " record(s) deleted successfully from '" + tableName + "'!", "green");
            loadTableData(tableName);
        } catch (SQLException e) {
            updateStatus("Error deleting records: " + e.getMessage(), "red");
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) { /* ignore */ }
            }
        } finally {
            DBUtil.closeConnection(conn);
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    private void populateTableComboBox(ComboBox<String> comboBox) {
        ObservableList<String> tableNames = FXCollections.observableArrayList();
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            // Specify the catalog (database) name here to only show tables from 'testdb'
            rs = metaData.getTables("testdb", null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"));
            }
            comboBox.setItems(tableNames);
        } catch (SQLException e) {
            updateStatus("Error fetching table names: " + e.getMessage(), "red");
            e.printStackTrace();
        } finally {
            DBUtil.closeConnection(conn);
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    private void loadTableData(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            dataTable.getColumns().clear();
            dataTable.setItems(FXCollections.observableArrayList());
            return;
        }

        tableData = FXCollections.observableArrayList();
        dataTable.getColumns().clear();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM " + tableName);

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            TableColumn<DynamicRow, Boolean> checkColumn = new TableColumn<>("Delete?");
            checkColumn.setCellValueFactory(param -> param.getValue().checkedProperty());
            checkColumn.setCellFactory(column -> new TableCell<DynamicRow, Boolean>() {
                private final CheckBox checkBox = new CheckBox();
                {
                    checkBox.setOnAction(event -> {
                        DynamicRow row = getTableView().getItems().get(getIndex());
                        row.setChecked(checkBox.isSelected());
                    });
                }
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        checkBox.setSelected(item != null && item);
                        setGraphic(checkBox);
                    }
                }
            });
            checkColumn.setPrefWidth(70);
            dataTable.getColumns().add(checkColumn);

            ObservableList<String> columnNames = FXCollections.observableArrayList();
            for (int i = 1; i <= columnCount; i++) {
                String colName = rsmd.getColumnName(i);
                columnNames.add(colName);
                TableColumn<DynamicRow, String> col = new TableColumn<>(colName);
                col.setCellValueFactory(param -> param.getValue().getColumnValueProperty(colName));
                col.setPrefWidth(120);
                dataTable.getColumns().add(col);
            }

            while (rs.next()) {
                DynamicRow row = new DynamicRow();
                for (String colName : columnNames) {
                    String value = rs.getString(colName);
                    // Convert all string values to uppercase for display
                    row.addColumnValue(colName, value != null ? value.toUpperCase() : null);
                }
                tableData.add(row);
            }
            dataTable.setItems(tableData);
            updateStatus("Table '" + tableName + "' loaded successfully.", "blue");

        } catch (SQLException e) {
            updateStatus("Error loading table data: " + e.getMessage(), "red");
            e.printStackTrace();
        } finally {
            DBUtil.closeConnection(conn);
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* ignore */ }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) { /* ignore */ }
            }
        }
    }

    public static class DynamicRow {
        private final BooleanProperty checked = new SimpleBooleanProperty(false);
        private final Map<String, StringProperty> columnValues = new LinkedHashMap<>();

        public BooleanProperty checkedProperty() {
            return checked;
        }

        public boolean isChecked() {
            return checked.get();
        }

        public void setChecked(boolean checked) {
            this.checked.set(checked);
        }

        public void addColumnValue(String columnName, String value) {
            columnValues.put(columnName, new SimpleStringProperty(value));
        }

        public StringProperty getColumnValueProperty(String columnName) {
            return columnValues.get(columnName);
        }

        public String getColumnValue(String columnName) {
            StringProperty prop = columnValues.get(columnName);
            return (prop != null) ? prop.get() : null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
