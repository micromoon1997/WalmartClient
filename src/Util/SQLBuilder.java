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

    public static String buildSearchProductSQL(String keys) {
        String sql = "select * from product ";
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

    public static String buildSearchCustomerSQL(String keys) {
        String sql = "select * from customer ";
        if (keys == null || keys.isEmpty()) {
            return sql;
        }
        String[] keyArray = keys.split("\\s+");
        sql += "where ";
        for (String key: keyArray) {
            sql += String.format("(email like '%s' or ", key);
            sql += String.format("c_name like '%%%s%%' or ", key);
            sql += String.format("address like '%%%s%%') and ", key);
        }
        return sql.substring(0, sql.length() - 5);
    }

    public static String buildSearchEmployeeSQL(String keys) {
        String sql = "select * from employee ";
        if (keys == null || keys.isEmpty()) {
            return sql;
        }
        String[] keyArray = keys.split("\\s+");
        sql += "where ";
        for (String key: keyArray) {
            sql += String.format("(e_id like '%s' or ", key);
            sql += String.format("e_name like '%%%s%%' or ", key);
            sql += String.format("startdate like '%%%s%%' or ", key);
            sql += String.format("e_type like '%%%s%%') and ", key);
        }
        return sql.substring(0, sql.length() - 5);
    }

    public static String buildProductSQL(String pid, String category, double sp, double pp,
                                         int inventory, String pname, String psize, String color,
                                         double rating, String brandname, String tn, char IorU) {
        if (!pid.isEmpty()) {
            pid = "'" + pid + "'";
            if (category != null) category = "'" + category + "'";
            if (pname != null) pname = "'" + pname + "'";
            if (psize != null) psize = "'" + psize + "'";
            if (color != null) color = "'" + color + "'";
            if (brandname != null) brandname = "'" + brandname + "'";
            if (tn != null) tn = "'" + tn + "'";
            if (IorU == 'u') {
                return String.format("update product set category = %s, saleprice = %.2f," +
                                " purchaseprice = %.2f, inventory = %d, p_name = %s, p_size = %s, color = %s," +
                                " rating = %.2f, brandname = %s, thumbnail = %s where p_id like %s",
                        category, sp, pp, inventory, pname, psize, color, rating, brandname, tn, pid);
            } else if (IorU == 'i') {
                return String.format("insert into product values (%s, %s, %.2f, %.2f, %d, %s, %s, %s, %.2f, %s, %s)",
                        pid, category, sp, pp, inventory, pname, psize, color, rating, brandname, tn);
            }
            return null;
        } else
            return null;
    }

    public static String buildCustomerSQL(String email, String cname, String address, String password, char IorU) {
        if (!email.isEmpty()) {
            email = "'" + email + "'";
            if (cname != null) cname = "'" + cname + "'";
            if (address != null) address = "'" + address + "'";
            if (password != null) password = "'" + password + "'";
            if (IorU== 'u') {
                return String.format("update customer set c_name = %s, address = %s, password = %s where email like %s",
                        cname, address, password, email);
            } else if (IorU == 'i') {
                return String.format("insert into customer values (%s, %s, %s, %s)", email, cname, address, password);
            }
            return null;
        } else
            return null;
    }

    public static String buildEmployeeSQL(String eid, String ename, double salary, String startdate, String etype,
                                          String password, char IorU) {
        if (!eid.isEmpty()) {
            eid = "'" + eid + "'";
            if (ename != null) ename = "'" + ename + "'";
            if (startdate != null) startdate = "'" + startdate + "'";
            if (etype != null) etype = "'" + etype + "'";
            if (password != null) password = "'" + password + "'";
            if (IorU == 'u') {
                return String.format("update employee set e_name = %s, salary = %.2f, startdate = %s, e_type = %s, " +
                        "password = %s where e_id like %s", ename, salary, startdate, etype, password, eid);
            } else if (IorU == 'i') {
                return String.format("insert into employee values (%s, %s, %.2f, %s, %s, %s)",
                        eid, ename, salary, startdate, etype, password);
            }
            return null;
        } else
            return null;
    }

    public static String buildDivision() {
        return "select p_id from PRODUCT p where not exists ((select distinct c.EMAIL from CUSTOMER c) minus (select distinct EMAIL from TRANSACTION_DEALWITH_PAY tdp natural join INCLUDE i where p.P_ID like i.P_ID))";
    }

    public static String buildBestSeller() {
        return "select P_ID, total from (select P_ID, sum(QUANTITY) total from INCLUDE group by P_ID order by total desc) where rownum=1";
    }

    public static String buildSellingCategory(boolean isBest) {
        String sql = "select CATEGORY, avg\n" +
                "from (select CATEGORY, avg(QUANTITY) avg\n" +
                "      from INCLUDE i natural join PRODUCT p\n" +
                "      group by CATEGORY\n" +
                "      order by avg ";
        if (isBest) sql += "desc";
        sql += ") where ROWNUM=1";
        return sql;
    }

    public static String buildDeleteSQL(String key, String table, String colName) {
        return String.format("delete from %s where %s like '%s'", table, colName, key);
    }

    public static String buildInsertTransactionSQL(String tansNum, String dateTime, String pm,
                                          String email, String eid, double total) {
        String sql = "insert into TRANSACTION_DEALWITH_PAY VALUES ";
        sql += String.format("('%s', '%s', '%s', ", tansNum, dateTime, pm);
        if (email == null || email.isEmpty()) {
            sql += "null, ";
        } else {
            sql += String.format("'%s', ", email);
        }
        if (eid == null || eid.isEmpty()) {
            sql += "null, ";
        } else {
            sql += String.format("'%s', ", eid);
        }
        sql += String.format("%.2f)", total);
        return sql;
    }

    public static String buildInsertIncludeSQL(String transNum, String pid, int quantity) {
        return String.format("insert into Include values ('%s', '%s', %d)", transNum, pid, quantity);
    }

    public static String buildSelectInventorySQL(String pId) {
        return String.format("SELECT INVENTORY from PRODUCT WHERE P_ID like '%s'", pId);
    }
}
