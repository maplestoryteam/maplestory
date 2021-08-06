package exts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface CharExt {

    static int findIdByName(String name) {
        int id = 0;
        try {
            PreparedStatement ps = ConnExt.getConn().prepareStatement("select c.id from characters as c where c.name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    static String findNameById(int id) {
        String name = "";
        try {
            PreparedStatement ps = ConnExt.getConn().prepareStatement("select c.name from characters as c where c.id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                name = rs.getString(1);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return name;
    }

}
