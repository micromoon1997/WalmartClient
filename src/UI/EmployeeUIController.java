package UI;


import Util.Connector;
import Util.SQLBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


public class EmployeeUIController {
    private Connector connector = Connector.getInstance();
    private Alert error = new Alert(Alert.AlertType.ERROR);
    private String eid = connector.getAccount();

    @FXML
    private AnchorPane showPane;
    @FXML
    private Button addToCart, setCustomer, search, viewSwitch;
    @FXML
    private TextField pidBox, emailBox, searchBox;
    @FXML
    private TableView<Item> cartView;
    @FXML
    private ListView<String> searchView;
    @FXML
    private ChoiceBox<String> choiceBox;
    @FXML
    private TableColumn<Item, Item> pidColumn, pNameColumn, pQuantityColumn, pPriceColumn;

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
        initChoiceBox();
    }


    @FXML
    private void handleAddToCart() throws SQLException{
        searchView.setVisible(false);
        cartView.setVisible(true);
        String pId = pidBox.getText();
        if (pId.isEmpty()) {
            error.setContentText("pid cannot be empty.");
            error.showAndWait();
            return;
        }
        String sql = SQLBuilder.buildSearchProductSQL(null, pId);
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
            System.out.println("aaaaaaa");
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
                connector.showCheckoutComplete();
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
        String column = null;
        if (table.equals("Product")) {
            column = "p_id";
        } else if (table.equals("Customer")) {
            column = "email";
        } else if (table.equals("Employee")) {
            column = "e_id";
        } else {
            error.setContentText("Please choose the table to search.");
            error.showAndWait();
            return;
        }
        String key = searchBox.getText();
        if (key == null || key.isEmpty()) {
            error.setContentText("Please enter the primary key.");
            error.showAndWait();
            return;
        }
        String sql = SQLBuilder.buildSearchKeySQL(key, table, column);
        ResultSet res = connector.sendSQL(sql);
        cartView.setVisible(false);
        searchView.setVisible(true);
        searchView.getItems().clear();
        if (res.next()) {
//            ResultSetMetaData rsmd = res.getMetaData();
//            int columnCount = rsmd.getColumnCount();
//            for (int i = 0; i <= columnCount; i++) {
//                String columnName = rsmd.getColumnName(i);
//
//            }
            System.out.println(res.getString(1));
            searchView.getItems().add(res.getString(1));
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

    private void initChoiceBox() {
        ObservableList<String> choices = FXCollections.observableArrayList(
                "Product",
                "Customer",
                "Employee");
        choiceBox.setItems(choices);
    }

}
