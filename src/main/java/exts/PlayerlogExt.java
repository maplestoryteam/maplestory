package exts;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PlayerlogExt {
    static void log(String characterName, String type, String remark1, String remark2, int num) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("INSERT INTO playerlog(character_name,`type`,remark1,remark2,num,logtime) VALUES (?,?,?,?,?,NOW())");
            ps.setString(1, characterName);
            ps.setString(2, type);
            ps.setString(3, remark1);
            ps.setString(4, remark2);
            ps.setInt(5, num);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
