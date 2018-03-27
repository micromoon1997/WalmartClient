package UI;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

import Util.Connector;
import Util.SQLBuilder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import javax.xml.soap.Text;

public class CustomerUIController {

    private VBox cartPane = new VBox();
    private FlowPane itemPane = new FlowPane();
    private ObservableList<String> choices;
    private static Connector connector = Connector.getInstance();

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button searchButton, myCart, checkout;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private ChoiceBox<String> choiceBox;

    @FXML
    private AnchorPane topPane;

    @FXML
    private TextField searchBox;

    @FXML
    void initialize() {
        assert searchButton != null : "fx:id=\"searchButton\" was not injected: check your FXML file 'CustomerUI.fxml'.";
        assert itemPane != null : "fx:id=\"itemPane\" was not injected: check your FXML file 'CustomerUI.fxml'.";
        scrollPane.setContent(itemPane);
        itemPane.setId("itemPane");
        cartPane.setId("cartPane");
        initChoiceBox();
        checkout.setVisible(false);
        choiceBox.setValue("");
    }

    @FXML
    private void handleSearch() throws SQLException{
        itemPane.getChildren().clear();
        String category = choiceBox.getValue().toString();
        System.out.println(category);
        String searchKeys = searchBox.getText();
        String sql = SQLBuilder.buildSearchSQL(category, searchKeys);
        ResultSet res = connector.sendSQL(sql);
        while (res.next()) {
            String pId = res.getString("p_id");
            String pName = res.getString("p_name");
            double price = res.getFloat("saleprice");
            String thumbnail = res.getString("thumbnail");
            FlowPane item = buildItem(pId, pName, price, thumbnail);
            itemPane.getChildren().add(item);
        }
    }

    @FXML
    private void handleSearchEnter(KeyEvent ke) throws SQLException{
        if (ke.getCode().equals(KeyCode.ENTER)) {
            handleSearch();
        }
    }

    @FXML
    private void handleMyCart() {
        String currId = scrollPane.getContent().getId();
        if (currId.equals("itemPane")) {
            scrollPane.setContent(cartPane);
            myCart.setText("Back");
            checkout.setVisible(true);
        } else {
            scrollPane.setContent(itemPane);
            myCart.setText("MyCart");
            checkout.setVisible(false);
        }
    }

    @FXML
    private void handleCheckout() throws SQLException {
        String maxTransNumSql = "SELECT MAX(TRANSACTIONNUM) FROM TRANSACTION_DEALWITH_PAY";
        ResultSet rs = connector.sendSQL(maxTransNumSql);
        String transNum = "0000000000";
        rs.next();
        String maxTransNum = rs.getString("MAX(TRANSACTIONNUM)");
        if (maxTransNum != null) {
            transNum = String.format("%010d", Integer.parseInt(maxTransNum) + 1);
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String dateTime = now.format(dtf);
        String email = connector.getAccount();
        double total = 0.00;
        for (Node n: cartPane.getChildren()) {
            FlowPane item = (FlowPane) n;
            TextField quantityField = (TextField) item.getChildren().get(2);
            int quantity = Integer.parseInt(quantityField.getText());
            if (!checkInventory(item.getId(), quantity)) {
                Label nameLabel = (Label) item.getChildren().get(0);
                Alert quantityError = new Alert(Alert.AlertType.ERROR);
                quantityError.setHeaderText(null);
                quantityError.setTitle("No Enough Inventory");
                quantityError.setContentText(nameLabel.getText() + " does not have enough inventory.");
                quantityError.showAndWait();
                return;
            }
            Label itemTotalLabel = (Label) item.getChildren().get(3);
            double itemTotal = Double.parseDouble(itemTotalLabel.getText().substring(1));
            total += itemTotal;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Payment Method");
        alert.setContentText("Please choose your payment method");
        alert.setHeaderText(null);
        ButtonType creditCard = new ButtonType("Credit Card");
        ButtonType debitCard = new ButtonType("Debit Card");
        ButtonType cash = new ButtonType("Cash");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(creditCard, debitCard, cash, cancel);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != cancel) {
            String pm = result.get().getText();
            String insertTransactionSql = SQLBuilder.buildInsertTransactionSQL(transNum, dateTime, pm, email,
                    "", total);
            connector.sendSQL(insertTransactionSql);
            for (Node n: cartPane.getChildren()) {
                FlowPane item = (FlowPane) n;
                String insertIncludeSql = SQLBuilder.buildInsertIncludeSQL(transNum, item.getId());
                connector.sendSQL(insertIncludeSql);
            }
            connector.commit();
            cartPane.getChildren().clear();
            Alert successInfo = new Alert(Alert.AlertType.INFORMATION);
            successInfo.setTitle("Checkout Completed");
            successInfo.setHeaderText(null);
            successInfo.setContentText("Checkout completed. Thank you!");
            successInfo.showAndWait();
        }
    }


    private FlowPane buildItem(String pId, String pName, double price, String thumbnail) {
        FlowPane item = new FlowPane();
        ImageView lthumbnail = new ImageView();
        lthumbnail.setFitHeight(150);
        lthumbnail.setFitWidth(150);
        try {
            Image img = new Image(thumbnail, true);
            lthumbnail.setImage(img);
        } catch (IllegalArgumentException e) {

        }
        Label lname = new Label(pName);
        Label lprice = new Label(String.format("$ %.2f", price));
        Button addToCart = new Button("Add");
        item.setPrefSize(150.0, 250.0);
        lname.setPrefSize(150.0, 50.0);
        lprice.setPrefSize(100.0, 50.0);
        addToCart.setPrefSize(50.0, 50.0);
        item.getChildren().add(lthumbnail);
        item.getChildren().add(lname);
        item.getChildren().add(lprice);
        item.getChildren().add(addToCart);
        addToCart.setOnMouseReleased(getAddHandler(pId, pName, price));
        return item;
    }

    private EventHandler<MouseEvent> getAddHandler(String pId, String pName, double price) {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                for (Node n: cartPane.getChildren()) {
                    if (n.getId().equals(pId)) {
                        FlowPane pane = (FlowPane) n;
                        TextField tf = (TextField) pane.getChildren().get(2);
                        Label cprice = (Label) pane.getChildren().get(3);
                        Integer num = Integer.parseInt(tf.getText()) + 1;
                        tf.setText(String.format("%d", num));
                        cprice.setText(String.format("$%.2f", price*num));
                        return;
                    }
                }
                FlowPane cartItem = new FlowPane();
                cartItem.setId(pId);
                cartItem.setPrefHeight(50);
                cartItem.setHgap(50);
                Label cname = new Label(pName);
                cname.setPrefSize(550, 50);
                Label x = new Label("x");
                x.setPrefSize(20, 20);
                TextField quantityBox = new TextField("1");
                quantityBox.setPrefSize(150, 30);
                Label cprice = new Label(String.format("$%.2f", price));
                cprice.setPrefWidth(80);
                Button remove = new Button("remove");
                quantityBox.setOnKeyPressed(new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        if (event.getCode().equals(KeyCode.ENTER)) {
                            int quantity = Integer.parseInt(quantityBox.getText());
                            double totalPrice = quantity * price;
                            cprice.setText(String.format("$%.2f", totalPrice));
                        }
                    }
                });
                remove.setOnMouseReleased(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        cartPane.getChildren().remove(cartItem);
                    }
                });
                cartItem.getChildren().addAll(cname, x, quantityBox, cprice, remove);
                cartPane.getChildren().add(cartItem);
            }
        };
    }

    private void initChoiceBox() {
        choices = FXCollections.observableArrayList(
                "",
                "Patio and Garden",
                "Clothing",
                "Party and Occasions",
                "Health",
                "Pets",
                "Auto and Tires",
                "Jewelry",
                "Baby",
                "Sports and Outdoors",
                "Video Games",
                "Beauty",
                "Office",
                "Industrial and Scientific",
                "Personal Care",
                "Electronics",
                "Movies and TV Shows",
                "Walmart for Business",
                "Food",
                "Seasonal",
                "Arts, Crafts and Sewing",
                "Home",
                "Photo Center",
                "Musical Instruments",
                "UNNAV",
                "Cell Phones",
                "Home Improvement",
                "Toys"
        );
        choiceBox.setItems(choices);
    }

    private boolean checkInventory(String pId, int quantiry) throws SQLException{
        String sql = SQLBuilder.buildSelectInventorySQL(pId);
        ResultSet res = connector.sendSQL(sql);
        if (res.next()) {
            return res.getInt("inventory") >= quantiry;
        } else {
            return false;
        }
    }
}
