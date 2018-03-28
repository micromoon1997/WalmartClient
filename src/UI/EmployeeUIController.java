package UI;


import Util.Connector;
import Util.SQLBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

import java.sql.ResultSet;
import java.sql.SQLException;


public class EmployeeUIController {
    private Connector connector = Connector.getInstance();
    private Alert error = new Alert(Alert.AlertType.ERROR);
    private String customer;
    private String employee = connector.getAccount();

    @FXML
    private AnchorPane showPane;
    @FXML
    private Button addToCart, setCustomer, search;
    @FXML
    private TextField pidBox, emailBox, searchBox;
    @FXML
    private TableView<Item> cartView;
    @FXML
    private ListView searchView;
    @FXML
    private TableColumn<Item, Item> pidColumn, pnameColumn, pQuantityColumn, ppriceColumn;

    public EmployeeUIController() {
    }

    @FXML
    void initialize() {
        error.setHeaderText(null);
        error.setTitle("Error");
        searchView.setVisible(false);
        pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        pnameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        pQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        ppriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
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
            cartView.getItems().add(new Item(pid, pname, pprice));
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
                    customer = email;
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

    }

    public class Item {
        private String pid;
        private String name;
        private double price;

        private Item(String pid, String name, double price) {
            this.pid = pid;
            this.name = name;
            this.price = price;
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
    }

}
