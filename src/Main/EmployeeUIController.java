package UI;


import Util.Connector;
import Util.SQLBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class EmployeeUIController {
    private Connector connector = Connector.getInstance();
    private Alert error = new Alert(Alert.AlertType.ERROR);
    private String eid = connector.getAccount();
    private String currTable;

    @FXML
    private AnchorPane showPane;
    @FXML
    private FlowPane managePane;
    @FXML
    private Button addToCart, setCustomer, search, viewSwitch;
    @FXML
    private TextField pidBox, emailBox, searchBox, startDateBox, endDateBox;
    @FXML
    private TableView<Item> cartView;
    @FXML
    private TableView<SearchItem> searchView;
    @FXML
    private ChoiceBox<String> choiceBox, bestOrWorst;
    @FXML
    private TableColumn<Item, Item> pidColumn, pNameColumn, pQuantityColumn, pPriceColumn;
    @FXML
    private TableColumn<SearchItem, SearchItem> keyColumn, nameColumn;

    public EmployeeUIController() {
    }

    @FXML
    void initialize() {
        error.setHeaderText(null);
        error.setTitle("Error");
        searchView.setVisible(false);
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        pNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        pQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        pPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        initChoiceBox();
    }


    @FXML
    private void handleAddToCart() throws SQLException{
        String pId = pidBox.getText();
        if (pId.isEmpty()) {
            error.setContentText("pid cannot be empty.");
            error.showAndWait();
            return;
        }
        searchView.setVisible(false);
        cartView.setVisible(true);
        viewSwitch.setText("View Search Result");
        String sql = SQLBuilder.buildSearchProductSQL(pId);
        ResultSet res = connector.sendSQL(sql);
        if (res.next()) {
            String pid = res.getString("p_id");
            String pname = res.getString("p_name");
            double pprice = res.getDouble("saleprice");
            for (Item item: cartView.getItems()) {
                if (item.getPid().equals(pId)) {
                    int quantity = item.getQuantity() + 1;
                    double price = quantity * pprice;
                    price = Math.round(price * 100) / 100;
                    cartView.getItems().add(new Item(pid, pname, quantity, price));
                    cartView.getItems().remove(item);
                    return;
                }
            }
            cartView.getItems().add(new Item(pid, pname, 1, pprice));
        } else {
            error.setContentText("Product with pid: " + pId + " not found.");
            error.showAndWait();
        }

    }

    @FXML
    private void handleSetCustomer() throws SQLException {
        if (emailBox.isDisable()) {
            emailBox.setDisable(false);
            emailBox.setText(null);
            setCustomer.setText("Set Customer");
        } else {
            String email = emailBox.getText();
            if (email.isEmpty()) {
                error.setContentText("Customer email cannot be empty.");
                error.showAndWait();
            } else {
                String sql = String.format("select * from customer where email like '%s'", email);
                ResultSet res = connector.sendSQL(sql);
                if (res.next()) {
                    setCustomer.setText("Reset");
                    emailBox.setDisable(true);
                } else {
                    error.setContentText("Customer eamil not found.");
                    error.showAndWait();
                }
            }
        }
    }

    @FXML
    private void handleRemove() {
        Item item = cartView.getSelectionModel().getSelectedItem();
        cartView.getItems().remove(item);
    }

    @FXML
    private void handleCheckout() throws SQLException{
        String transNum = connector.getNextTransNum();
        String dateTime = connector.getDateTime();
        String email = null;
        if (cartView.getItems().isEmpty()) {
            error.setContentText("No item added.");
            error.showAndWait();
            return;
        }
        if (emailBox.isDisable()) {
            email = emailBox.getText();
        }
        double total = 0;
        for (Item item: cartView.getItems()) {
            total += item.getPrice();
        }
        String pm = connector.getPaymentMethod();
        if (!pm.equals("Cancel")) {
            try {
                String insertTransactionSQL = SQLBuilder.buildInsertTransactionSQL(transNum, dateTime, pm, email, eid, total);
                connector.sendSQL(insertTransactionSQL);
                for (Item item : cartView.getItems()) {
                    String pid = item.getPid();
                    int quantity = item.getQuantity();
                    String insertIncludeSQL = SQLBuilder.buildInsertIncludeSQL(transNum, pid, quantity);
                    connector.sendSQL(insertIncludeSQL);
                }
                connector.commit();
                cartView.getItems().clear();
                connector.showCompleteDialog();
            } catch (Exception e) {
                connector.rollback(); // roll back if caught any error
                error.setContentText("Error: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    @FXML
    private void handleSearch() throws SQLException {
        String table = choiceBox.getSelectionModel().getSelectedItem();
        String searchKey = searchBox.getText();
        cartView.setVisible(false);
        searchView.setVisible(true);
        viewSwitch.setText("View Cart");
        searchView.getItems().clear();
        if (table == null) {
            error.setContentText("Please choose the table to search.");
            error.showAndWait();
            return;
        }
        if (table.equals("Product")) {
            String sql = SQLBuilder.buildSearchProductSQL(searchKey);
            ResultSet rs = connector.sendSQL(sql);
            while (rs.next()) {
                String key = rs.getString("p_id");
                String name = rs.getString("p_name");
                searchView.getItems().add(new SearchItem(key, name, table));
            }
        } else if (table.equals("Customer")) {
            String sql = SQLBuilder.buildSearchCustomerSQL(searchKey);
            ResultSet rs = connector.sendSQL(sql);
            while (rs.next()) {
                String key = rs.getString("email");
                String name = rs.getString("c_name");
                searchView.getItems().add(new SearchItem(key, name, table));
            }
        } else if (table.equals("Employee")) {
            String sql = SQLBuilder.buildSearchEmployeeSQL(searchKey);
            ResultSet rs = connector.sendSQL(sql);
            while (rs.next()) {
                String key = rs.getString("e_id");
                String name = rs.getString("e_name");
                searchView.getItems().add(new SearchItem(key, name, table));
            }
        }
    }

    @FXML
    private void handleViewSwitch() {
        if (searchView.isVisible()) {
            searchView.setVisible(false);
            cartView.setVisible(true);
            viewSwitch.setText("View Search Result");
        } else {
            cartView.setVisible(false);
            searchView.setVisible(true);
            viewSwitch.setText("View Cart");
        }
    }

    @FXML
    private void handleEdit() throws SQLException {
        SearchItem searchItem = searchView.getSelectionModel().getSelectedItem();
        if (searchItem == null) {
            error.setContentText("No item is selected.");
            error.showAndWait();
            return;
        }
        managePane.getChildren().clear();
        String table = searchItem.getTable();
        currTable = table;
        String key = searchItem.getKey();
        String sql;
        switch (table) {
            case "Product":
                sql = SQLBuilder.buildSearchProductSQL(key);
                break;
            case "Customer":
                sql = SQLBuilder.buildSearchCustomerSQL(key);
                break;
            case "Employee":
                sql = SQLBuilder.buildSearchEmployeeSQL(key);
                break;
            default:
                error.setContentText("Invalid table.");
                error.showAndWait();
                return;
        }
        ResultSet rs = connector.sendSQL(sql);
        if (rs.next()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String colName = rsmd.getColumnName(i);
                Label col = new Label(colName);
                col.setPrefSize(100, 30);
                col.setFont(new Font("System", 12));
                TextField colValue = new TextField(rs.getString(colName));
                colValue.setPrefSize(200, 30);
                if (i == 1) {
                    colValue.setDisable(true);
                }
                managePane.getChildren().addAll(col, colValue);
            }
            managePane.setId("Edit");
        }

    }

    @FXML
    private void handleSubmit() throws SQLException {
        if (currTable == null) {
            error.setContentText("No item is being edited.");
            error.showAndWait();
            return;
        }
        try {
            ObservableList<Node> list = managePane.getChildren();
            char type;
            if (managePane.getId().equals("Edit"))
                type = 'u';
            else
                type = 'i';
            if (currTable.equals("Product")) {
                String pid, category, pname, psize, color, brandname, thumbnail;
                double sp, pp, rating;
                int inventory;
                pid = ((TextField) list.get(1)).getText();
                category = ((TextField) list.get(3)).getText();
                sp = Double.parseDouble(((TextField) list.get(5)).getText());
                pp = Double.parseDouble(((TextField) list.get(7)).getText());
                inventory = Integer.parseInt(((TextField) list.get(9)).getText());
                pname = ((TextField) list.get(11)).getText();
                psize = ((TextField) list.get(13)).getText();
                color = ((TextField) list.get(15)).getText();
                rating = Double.parseDouble(((TextField) list.get(17)).getText());
                brandname = ((TextField) list.get(19)).getText();
                thumbnail = ((TextField) list.get(21)).getText();
                String sql = SQLBuilder.buildProductSQL(pid, category, sp, pp, inventory, pname,
                        psize, color, rating, brandname, thumbnail, type);
                connector.sendSQL(sql);
                connector.showCompleteDialog();
            } else if (currTable.equals("Customer")) {
                String email, cname, address, password;
                email = ((TextField) list.get(1)).getText();
                cname = ((TextField) list.get(3)).getText();
                address = ((TextField) list.get(5)).getText();
                password = ((TextField) list.get(7)).getText();
                String sql = SQLBuilder.buildCustomerSQL(email, cname, address, password, type);
                connector.sendSQL(sql);
                connector.showCompleteDialog();
            } else if (currTable.equals("Employee")) {
                String eid, ename, startdate, etype, password;
                double salary;
                eid = ((TextField) list.get(1)).getText();
                ename = ((TextField) list.get(3)).getText();
                salary = Double.parseDouble(((TextField) list.get(5)).getText());
                startdate = ((TextField) list.get(7)).getText();
                etype = ((TextField) list.get(9)).getText();
                password = ((TextField) list.get(11)).getText();
                String sql = SQLBuilder.buildEmployeeSQL(eid, ename, salary, startdate, etype, password, type);
                connector.sendSQL(sql);
                connector.showCompleteDialog();
            }
            connector.commit();
        } catch (NumberFormatException e) {
            error.setContentText("Malformed number.");
            error.showAndWait();
        } catch (SQLException e) {
            connector.rollback();
            error.setContentText("Fail to submit: " + e.getMessage());
            error.showAndWait();
        }
    }

    @FXML
    private void handleDelete() throws SQLException{
        SearchItem searchItem = searchView.getSelectionModel().getSelectedItem();
        String table = searchItem.getTable();
        String key = searchItem.getKey();
        String colName = connector.getPKColName(table);
        String sql = SQLBuilder.buildDeleteSQL(key, table, colName);
        connector.sendSQL(sql);
        searchView.getItems().remove(searchItem);
        connector.commit();
        System.out.println("commit2");
    }

    @FXML
    private void handleAdd() throws SQLException {
        String table = choiceBox.getValue();
        if (table == null) {
            error.setContentText("No table is selected.");
            error.showAndWait();
            return;
        }
        managePane.getChildren().clear();
        currTable = table;
        ResultSet rs = connector.sendSQL(String.format("select * from %s where rownum=1", table));
        ResultSetMetaData rsmd = rs.getMetaData();
        int max = rsmd.getColumnCount();
        for (int i = 1; i <= max; i++) {
            String colName = rsmd.getColumnName(i);
            Label col = new Label(colName);
            col.setPrefSize(100, 30);
            col.setFont(new Font("System", 12));
            TextField colValue = new TextField();
            colValue.setPrefSize(200, 30);
            managePane.getChildren().addAll(col, colValue);
        }
        managePane.setId("Add");
    }

    @FXML
    private void handleSellingCategory() throws SQLException{
        String val = bestOrWorst.getValue();
        ResultSet rs = connector.sendSQL(SQLBuilder.buildSellingCategory(val.equals("Best")));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setTitle(val + " Selling Category");
        rs.next();
        String category = rs.getString("category");
        String avg = String.format("%.2f" ,rs.getDouble("avg"));
        alert.setContentText(String.format("Category: %s\nAverage Sales: %s", category, avg));
        alert.showAndWait();
    }

    @FXML
    private void handleProfitReport() throws SQLException{
        String startDate = startDateBox.getText();
        String endDate = endDateBox.getText();
        String timePeriod = "where ";
        if (!startDate.isEmpty()) timePeriod += String.format("datetime >= '%s 00:00:00'  and ",startDate);
        if (!endDate.isEmpty()) timePeriod += String.format("datetime <= '%s 23:59:59'  and ", endDate);
        timePeriod = timePeriod.substring(0, timePeriod.length() - 6);
        String turnoverSql = "select sum(TOTAL) sum from TRANSACTION_DEALWITH_PAY " + timePeriod;
        String profitSql = "select sum(SALEPRICE - PURCHASEPRICE) sum " +
                "from INCLUDE natural join TRANSACTION_DEALWITH_PAY natural join PRODUCT " + timePeriod;
        ResultSet rs = connector.sendSQL(turnoverSql);
        rs.next();
        String turnover = rs.getString("sum");
        rs = connector.sendSQL(profitSql);
        rs.next();
        String profit = rs.getString("sum");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setTitle("Profit");
        alert.setContentText(String.format("Turnover: %s\nProfit: %s", turnover, profit));
        alert.showAndWait();
    }

    private void initChoiceBox() {
        ObservableList<String> choicesM = FXCollections.observableArrayList(
                "Product",
                "Customer",
                "Employee");
        ObservableList<String> choicesE = FXCollections.observableArrayList(
                "Product",
                "Customer");
        ObservableList<String> choicesS = FXCollections.observableArrayList(
                "Best",
                "Worst"
        );
        String sql = String.format("select e_type from employee where e_id like '%s'", eid);
        try {
            ResultSet rs = connector.sendSQL(sql);
            if (rs.next()) {
                if (rs.getString("e_type").equals("Manager")) {
                    choiceBox.setItems(choicesM);
                } else {
                    choiceBox.setItems(choicesE);
                }
            }
        } catch (SQLException e) {
            choiceBox.setItems(choicesE);
        }
        bestOrWorst.setItems(choicesS);
        choiceBox.setValue("Product");
        bestOrWorst.setValue("Best");
    }

    public class Item {
        private String pid;
        private String name;
        private double price;
        private int quantity;

        public Item(String pid, String name, int quantity, double price) {
            this.pid = pid;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }
        public String getPid() {
            return pid;
        }
        public double getPrice() {
            return price;
        }
        public int getQuantity() {
            return quantity;
        }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    public class SearchItem {
        private String key;
        private String name;
        private String table;
        public SearchItem(String key, String name, String table) {
            this.key = key;
            this.name = name;
            this.table = table;
        }
        public String getKey() {
            return key;
        }
        public String getName() {
            return name;
        }

        public String getTable() {
            return table;
        }
    }

}
