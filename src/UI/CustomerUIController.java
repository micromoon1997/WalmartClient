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

public class CustomerUIController {

    private VBox cartPane = new VBox();
    private FlowPane itemPane = new FlowPane();
    private static Connector connector = Connector.getInstance();
    private Alert error = new Alert(Alert.AlertType.ERROR);

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
        error.setHeaderText(null);
        error.setTitle("Error");
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
        String sql = SQLBuilder.buildSearchProductSQL(category, searchKeys);
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
        String transNum = connector.getNextTransNum();
        String dateTime = connector.getDateTime();
        String email = connector.getAccount();
        double total = 0.00;
        for (Node n: cartPane.getChildren()) {
            FlowPane item = (FlowPane) n;
            TextField quantityField = (TextField) item.getChildren().get(2);
            int quantity = Integer.parseInt(quantityField.getText());
            if (!checkInventory(item.getId(), quantity)) {
                Label nameLabel = (Label) item.getChildren().get(0);
                error.setContentText(nameLabel.getText() + " does not have enough inventory.");
                error.showAndWait();
                return;
            }
            Label itemTotalLabel = (Label) item.getChildren().get(3);
            double itemTotal = Double.parseDouble(itemTotalLabel.getText().substring(1));
            total += itemTotal;
        }
        String pm = connector.getPaymentMethod();
        if (!pm.equals("Cancel")) {
            try {
                String insertTransactionSql = SQLBuilder.buildInsertTransactionSQL(transNum, dateTime, pm, email,
                        "", total);
                connector.sendSQL(insertTransactionSql);
                for (Node n : cartPane.getChildren()) {
                    FlowPane item = (FlowPane) n;
                    TextField quantityBox = (TextField) item.getChildren().get(2);
                    int quantity = Integer.parseInt(quantityBox.getText());
                    String insertIncludeSql = SQLBuilder.buildInsertIncludeSQL(transNum, item.getId(), quantity);
                    connector.sendSQL(insertIncludeSql);
                }
                connector.commit();
                cartPane.getChildren().clear();
                connector.showCheckoutComplete();
            } catch (Exception e) {
                connector.rollback(); // roll back if any error occurred
                error.setContentText("Error: " + e.getMessage());
                error.showAndWait();
            }
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
            // TODO
        }
        Label lname = new Label(pName);
        Label lprice = new Label(String.format("$ %.2f", price));
        Button addToCart = new Button("Add");
        item.setPrefSize(150.0, 250.0);
        lname.setPrefSize(150.0, 50.0);
        lprice.setPrefSize(100.0, 50.0);
        addToCart.setPrefSize(50.0, 50.0);
        item.getChildren().addAll(lthumbnail, lname, lprice, addToCart);
        addToCart.setOnMouseReleased(getAddHandler(pId, pName, price));
//        Label testXXX = new Label("Add");
//        testXXX.setPrefSize(50.0, 50.0);
//        item.getChildren().addAll(testXXX);
//        testXXX.setOnMouseReleased(getAddHandler(pId, pName, price));
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
                quantityBox.setOnKeyPressed(ke -> {
                    if (ke.getCode().equals(KeyCode.ENTER)) {
                        try {
                            int quantity = Integer.parseInt(quantityBox.getText());
                            double totalPrice = quantity * price;
                            cprice.setText(String.format("$%.2f", totalPrice));
                        } catch (NumberFormatException e) {
                            error.setContentText("Quantity has to be number.");
                            error.showAndWait();
                        }
                    }
                });
                remove.setOnMouseReleased(me -> cartPane.getChildren().remove(cartItem));
                cartItem.getChildren().addAll(cname, x, quantityBox, cprice, remove);
                cartPane.getChildren().add(cartItem);
            }
        };
    }

    private void initChoiceBox() {
        ObservableList<String> choices = FXCollections.observableArrayList(
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
        return res.next() && res.getInt("inventory") >= quantiry;
    }
}
