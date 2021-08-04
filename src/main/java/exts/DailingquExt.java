package exts;

import exts.model.Dailingqu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public interface DailingquExt {

    static boolean insert(int charid, int leibie, int shuliang, String beizhu) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("insert into dailingqu(charid, leibie, shuliang, beizhu) VALUES (?,?,?,?)");
            ps.setInt(1, charid);
            ps.setInt(2, leibie);
            ps.setInt(3, shuliang);
            ps.setString(4, beizhu);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean delete(int charid, int leibie) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("delete from dailingqu where charid = ? and leibie = ?");
            ps.setInt(1, charid);
            ps.setInt(2, leibie);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static List<Dailingqu> select(int charid) {
        PreparedStatement ps;
        List<Dailingqu> ss = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("select t.* from dailingqu as t where t.charid = ?");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ss.add(new Dailingqu(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ss;
    }
}
