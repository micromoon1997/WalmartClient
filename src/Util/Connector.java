package Util;
import java.sql.*;


public class Connector {
    private static Connector connector = new Connector();
    private static Connection connection;
    private static String account;

    public static Connector getInstance() {
        return connector;
    }

    private Connector() {
        String url = "jdbc:oracle:thin:@localhost:1522:ug";
        String username = "ora_xxxxx";
        String password = "adddddddd";
        try{
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Database connected.");
        } catch (SQLException e) {
            System.err.println("Fail to connect to database: " + e.getErrorCode());
            System.exit(0);
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
}

