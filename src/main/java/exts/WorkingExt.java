package exts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface WorkingExt {
    static int[] query(int characterId) {
        int[] is = new int[5];
        PreparedStatement ps;
        try {
            // character_id  item_id  item_count    times   total     day
            //------------  -------  ----------  ------  ------  --------
            //           3        1           1       1       1         1
            ps = ConnExt.getConn().prepareStatement(String.format("SELECT * FROM working AS w WHERE w.`character_id` = %d", characterId));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                is[0] = rs.getInt(2);
                is[1] = rs.getInt(3);
                is[2] = rs.getInt(4);
                is[3] = rs.getInt(5);
                is[4] = rs.getInt(6);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return is;
    }

    static void update(int characterId, int itemId, int itemCount, int times) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement(String.format("SELECT count(*) FROM working AS w WHERE w.`character_id` = %d", characterId));
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next() && rs.getInt(1) > 0;
            if (exists) {
                ps = ConnExt.getConn().prepareStatement("UPDATE working AS w SET w.`item_id` = ?,w.`item_count` = ?,w.`times` = ?, w.`day` = DATE_FORMAT(NOW(),'%d') WHERE w.`character_id` = ?");
                ps.setInt(1, itemId);
                ps.setInt(2, itemCount);
                ps.setInt(3, times);
                ps.setInt(4, characterId);
            } else {
                ps = ConnExt.getConn().prepareStatement("INSERT INTO working(character_id,item_id,item_count,`times`,`day`) VALUES(?,?,?,?,DATE_FORMAT(NOW(),'%d'))");
                ps.setInt(1, characterId);
                ps.setInt(2, itemId);
                ps.setInt(3, itemCount);
                ps.setInt(4, times);
            }
            ps.executeUpdate();
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void updateTotal(int characterId, int delta) {
        try {
            PreparedStatement ps = ConnExt.getConn().prepareStatement(String.format("UPDATE working AS w SET w.`total` = w.`total` + %d WHERE w.`character_id` = %d", delta, characterId));
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
