package Util;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


public class Connector {
    private static Connector connector = new Connector();
    private static Connection connection;
    private static String account;

    public static Connector getInstance() {
        return connector;
    }

    private Connector() {
        try{
            String url = "jdbc:oracle:thin:@localhost:1522:ug";
            BufferedReader login = new BufferedReader(new FileReader("login"));
            String username = login.readLine();
            String password = login.readLine();

            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Database connected.");
        } catch (SQLException e) {
            System.err.println("Fail to connect to database: " + e.getErrorCode());
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Login file not found.");
        }
    }

    public ResultSet sendSQL(String sqlCMD) throws SQLException{
        PreparedStatement ps = connection.prepareCall(sqlCMD);
        System.out.println(sqlCMD);
        ps.execute();
        return ps.getResultSet();
    }

    public void commit() throws SQLException{
        sendSQL("commit");
    }

    public void rollback() throws SQLException {
        sendSQL("rollback");
    }

    public void setAccount(String acc) {
        account = acc;
    }

    public String getAccount() {
        return account;
    }

    public boolean validateAccount(String acc, String pw, char type) throws SQLException{
        String sql = SQLBuilder.buildLoginSQL(acc, type);
        ResultSet res = connector.sendSQL(sql);
        String returnedPassword = null;
        if (res.next()) {
            returnedPassword = res.getString("password");
            // System.out.println(returnedPassword);
        }
        return (returnedPassword != null) && pw.equals(returnedPassword);
    }

    // Aggregation query
    public String getNextTransNum() throws SQLException {
        String maxTransNumSql = "SELECT MAX(TRANSACTIONNUM) FROM TRANSACTION_DEALWITH_PAY";
        ResultSet rs = connector.sendSQL(maxTransNumSql);
        String transNum = "0000000000";
        rs.next();
        String maxTransNum = rs.getString("MAX(TRANSACTIONNUM)");
        if (maxTransNum != null) {
            transNum = String.format("%010d", Integer.parseInt(maxTransNum) + 1);
        }
        return transNum;
    }

    public String getPKColName(String table) throws SQLException {
        String sql = String.format("select * from %s where rownum=1", table);
        ResultSet rs = sendSQL(sql);
        rs.next();
        return rs.getMetaData().getColumnName(1);
    }

    public String getDateTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return now.format(dtf);

    }

    public String getPaymentMethod() {
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
        return result.get().getText();
    }

    public void showCompleteDialog() {
        Alert successInfo = new Alert(Alert.AlertType.INFORMATION);
        successInfo.setTitle("Completed");
        successInfo.setHeaderText(null);
        successInfo.setContentText("Completed. Thank you!");
        successInfo.showAndWait();
    }
}

