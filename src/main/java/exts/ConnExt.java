package exts;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnExt {

    private static Connection c;

    public static Connection getConn() {
        try {
            if (c == null || c.isClosed()) {
                c = DatabaseConnection.getConnection();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return c;
    }
}
