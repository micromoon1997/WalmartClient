package UI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    private String searchPid;

    @FXML
    private CheckBox categoryCB, nameCB, colorCB, sizeCB, ratingCB, brandCB, thumbnailCB;
    @FXML
    private Button searchButton, myCart, checkout;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private ChoiceBox<String> categoryChoice, ratingChoice;
    @FXML
    private AnchorPane topPane;
    @FXML
    private TextField nameTF, colorTF, sizeTF, ratingTF, brandTF;

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
        categoryChoice.setValue("");
        ratingChoice.setValue(">=");
        searchPid = null;
    }

    @FXML
    private void handleSearch() throws SQLException{
        itemPane.getChildren().clear();
        String sql = "select p_id, p_name, saleprice";
        if (categoryCB.isSelected()) sql += ", category";
        if (colorCB.isSelected()) sql += ", color";
        if (sizeCB.isSelected()) sql += ", p_size";
        if (ratingCB.isSelected()) sql += ", rating";
        if (brandCB.isSelected()) sql += ", brandname";
        if (thumbnailCB.isSelected()) sql += ", thumbnail";
        sql += " from product where ";
        if (searchPid != null) {
            sql += String.format("p_id like '%s' and ", searchPid);
            searchPid = null;
        } else {
            String val = categoryChoice.getValue();
            if (!val.equals("")) sql += String.format("category like '%s' and ", val);
            val = nameTF.getText();
            if (!val.isEmpty()) sql += String.format("p_name like '%%%s%%' and ", val);
            val = colorTF.getText();
            if (!val.isEmpty()) sql += String.format("color like '%%%s%%' and ", val);
            val = sizeTF.getText();
            if (!val.isEmpty()) sql += String.format("p_size like '%%%s%%' and ", val);
            val = ratingTF.getText();
            String operator = ratingChoice.getValue();
            if (!val.isEmpty()) sql += String.format("rating %s %.2f and ", operator, Double.parseDouble(val));
            val = brandTF.getText();
            if (!val.isEmpty()) sql += String.format("brandname like '%%%s%%' and ", val);
        }
        ResultSet rs = connector.sendSQL(sql.substring(0, sql.length() - 5));
        while (rs.next()) {
            String pid, category = null, pname, psize = null, color = null, brand = null, thumbnail = null;
            double price = -1, rating = -1;
            pid = rs.getString("p_id");
            pname = rs.getString("p_name");
            price = rs.getDouble("saleprice");
            if (categoryCB.isSelected()) category = rs.getString("category");
            if (colorCB.isSelected()) color = rs.getString("color");
            if (sizeCB.isSelected()) psize = rs.getString("p_size");
            if (ratingCB.isSelected()) rating = rs.getDouble("rating");
            if (brandCB.isSelected()) brand = rs.getString("brandname");
            if (thumbnailCB.isSelected()) thumbnail = rs.getString("thumbnail");
            FlowPane item = buildItem(pid, category, price, pname, psize, color, rating, brand, thumbnail);
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
                connector.showCompleteDialog();
            } catch (Exception e) {
                connector.rollback(); // roll back if any error occurred
                error.setContentText("Error: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    @FXML
    private void handleDivision() throws SQLException{
        String sql = SQLBuilder.buildDivision();
        ResultSet rs = connector.sendSQL(sql);
        while (rs.next()) {
            searchPid = rs.getString("p_id");
            handleSearch();
        }
    }

    @FXML
    private void handleBestSeller() throws SQLException{
        ResultSet rs = connector.sendSQL(SQLBuilder.buildBestSeller());
        rs.next();
        searchPid = rs.getString("p_id");
        handleSearch();
    }


    private FlowPane buildItem(String pId, String category, double price, String pname, String psize,
                               String color, double rating, String brand, String thumbnail) {
        FlowPane item = new FlowPane();
        item.setPrefWidth(150);
        if (thumbnail != null) {
            ImageView thumbnailLabel = new ImageView();
            thumbnailLabel.setFitHeight(150);
            thumbnailLabel.setFitWidth(150);
            try {
                Image img = new Image(thumbnail, true);
                thumbnailLabel.setImage(img);
                item.getChildren().add(thumbnailLabel);
            } catch (IllegalArgumentException e) {
                // TODO
            }
        }
        if (category != null) {
            Label categoryLabel = new Label(category);
            categoryLabel.setPrefSize(150, 50);
            item.getChildren().add(categoryLabel);
        }
        if (psize != null) {
            Label psizeLabel = new Label(psize);
            psizeLabel.setPrefSize(150, 50);
            item.getChildren().add(psizeLabel);
        }
        if (color != null) {
            Label colorLabel = new Label(color);
            colorLabel.setPrefSize(150, 50);
            item.getChildren().add(colorLabel);
        }
        if (rating != -1) {
            Label ratingLabel = new Label(String.format("%.2f", rating));
            ratingLabel.setPrefSize(150, 50);
            item.getChildren().add(ratingLabel);
        }
        if (brand != null) {
            Label brandLabel = new Label(brand);
            brandLabel.setPrefSize(150, 50);
            item.getChildren().add(brandLabel);
        }
        Label pnameLabel = new Label(pname);
        Label priceLabel = new Label(String.format("$ %.2f", price));
        Button addToCart = new Button("Add");
        pnameLabel.setPrefSize(150, 50);
        priceLabel.setPrefSize(100.0, 50.0);
        addToCart.setPrefSize(50.0, 50.0);
        addToCart.setOnMouseReleased(getAddHandler(pId, pname, price));
        item.getChildren().addAll(pnameLabel, priceLabel, addToCart);
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
        ObservableList<String> choices2 = FXCollections.observableArrayList(
                ">=",
                "<=",
                "="
        );
        categoryChoice.setItems(choices);
        ratingChoice.setItems(choices2);
    }


    private boolean checkInventory(String pId, int quantiry) throws SQLException{
        String sql = SQLBuilder.buildSelectInventorySQL(pId);
        ResultSet rs = connector.sendSQL(sql);
        return rs.next() && rs.getInt("inventory") >= quantiry;
    }
}
