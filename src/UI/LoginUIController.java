package UI;

import Util.Connector;
import Util.SQLBuilder;
import com.sun.tools.corba.se.idl.PragmaEntry;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginUIController {
    private static final ToggleGroup group = new ToggleGroup();
    private EventHandler<MouseEvent> customerLoginEvent, employeeLoginEvent;
    private static Connector connector = Connector.getInstance();
    private Alert error = new Alert(Alert.AlertType.ERROR);

    @FXML
    private TextField account;
    @FXML
    private PasswordField password;
    @FXML
    private RadioButton customerButton, employeeButton;
    @FXML
    private Button loginButton;

    @FXML
    void initialize() {
        customerButton.setToggleGroup(group);
        employeeButton.setToggleGroup(group);
        customerButton.setSelected(true);
        customerLoginEvent = getCustomerLoginEvent();
        employeeLoginEvent = getEmployeeLoginEvent();
        loginButton.setOnMouseReleased(customerLoginEvent);
        error.setTitle("Error");
        error.setHeaderText(null);
    }

    @FXML // customer toggle button
    private void handleCustomerButton() {
        account.setPromptText("email");
        account.clear();
        password.clear();
        loginButton.setOnMouseReleased(customerLoginEvent);
    }

    @FXML // employee toggle button
    private void handleEmployeeButton() {
        account.setPromptText("ID");
        account.clear();
        password.clear();
        loginButton.setOnMouseReleased(employeeLoginEvent);
    }

    private EventHandler<MouseEvent> getCustomerLoginEvent() {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    String enteredAccount = account.getText();
                    String enteredPassword = password.getText();
                    if (connector.validateAccount(enteredAccount, enteredPassword, 'c')) {
                        String alter1 = "ALTER SESSION SET NLS_COMP=LINGUISTIC";
                        String alter2 = "ALTER SESSION SET NLS_SORT=BINARY_CI";
                        connector.sendSQL(alter1);
                        connector.sendSQL(alter2);  // Set case insensitive
                        connector.setAccount(enteredAccount);
                        Parent customerViewParent = FXMLLoader.load(getClass().getResource("CustomerUI.fxml"));
                        Scene customerViewScene = new Scene(customerViewParent);
                        Stage loginStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        loginStage.close();
                        Stage customerStage = new Stage();
                        customerStage.setTitle("Walmart Client");
                        customerStage.setScene(customerViewScene);
                        customerStage.show();
                    } else {
                        error.setContentText("Username or password incorrect.");
                        error.showAndWait();
                    }
                } catch (IOException e) {
                    error.setContentText("Fatal error.");
                    error.showAndWait();
                } catch (SQLException e) {
                    error.setContentText("Database not connected: " + e.getErrorCode());
                    error.showAndWait();
                }
            }
        };
    }

    private EventHandler<MouseEvent> getEmployeeLoginEvent() {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    String enteredAccount = account.getText();
                    String enteredPassword = password.getText();
                    if (connector.validateAccount(enteredAccount, enteredPassword, 'e')) {
                        String alter1 = "ALTER SESSION SET NLS_COMP=LINGUISTIC";
                        String alter2 = "ALTER SESSION SET NLS_SORT=BINARY_CI";
                        connector.sendSQL(alter1);
                        connector.sendSQL(alter2);  // Set case insensitive
                        connector.setAccount(enteredAccount);
                        Parent employeeViewParent= FXMLLoader.load(getClass().getResource("EmployeeUI.fxml"));
                        Scene employeeViewScene = new Scene(employeeViewParent);
                        Stage loginStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        loginStage.close();
                        Stage employeeStage = new Stage();
                        employeeStage.setTitle("Walmart Cashier System");
                        employeeStage.setScene(employeeViewScene);
                        employeeStage.show();
                    } else {
                        error.setContentText("Username or password incorrect.");
                        error.showAndWait();
                    }
                } catch (IOException e) {
                    error.setContentText("Fatal error.");
                    error.showAndWait();
                } catch (SQLException e) {
                    error.setContentText("Database not connected: " + e.getErrorCode());
                    error.showAndWait();
                }
            }
        };
    }

}
