package Util;

public class SQLBuilder {

    public static String buildLoginSQL(String account, char type) {
        String sql = "select * from ";
        if (type == 'c') {
            sql += "customer where email like ";
        } else if (type == 'e') {
            sql += "employee where e_id like ";
        }
        sql += String.format("'%s'", account);
        return sql;
    }

    public static String buildSearchProductSQL(String category, String keys) {
        String sql = "select * from ";
        if (category == null || category.isEmpty()) {
            sql += "product ";
        } else {
            sql += String.format("(select * from product where category like '%s') ", category);
        }
        if (keys == null || keys.isEmpty()) {
            return sql;
        }
        String[] keyArray = keys.split("\\s+");
        sql += "where ";
        for (String key: keyArray) {
            sql += String.format("(P_id like '%s' or ", key);
            sql += String.format("P_size like '%%%s%%' or ", key);
            sql += String.format("P_name like '%%%s%%' or ", key);
            sql += String.format("Category like '%%%s%%' or ", key);
            sql += String.format("BrandName like '%%%s%%' or ", key);
            sql += String.format("color like '%%%s%%') and ", key);
    }
        return sql.substring(0, sql.length() - 5);
    }

    public static String buildInsertTransactionSQL(String tansNum, String dateTime, String pm,
                                          String email, String eid, double total) {
        String sql = "insert into TRANSACTION_DEALWITH_PAY VALUES ";
        if (eid.isEmpty()) {
            sql += String.format("('%s', '%s', '%s', '%s', null, %.2f)",
                    tansNum, dateTime, pm, email, total);
        } else {
            sql += String.format("('%s', '%s', '%s', '%s', '%s', %.2f)",
                    tansNum, dateTime, pm, email, eid, total);
        }
        return sql;
    }

    public static String buildInsertIncludeSQL(String transNum, String pid) {
        return String.format("insert into Include values ('%s', '%s')", transNum, pid);
    }

    public static String buildSelectInventorySQL(String pId) {
        return String.format("SELECT INVENTORY from PRODUCT WHERE P_ID like '%s'", pId);
    }
}
