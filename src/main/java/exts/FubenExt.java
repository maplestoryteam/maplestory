package exts;

import exts.model.ShiTu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface FubenExt {

    /**
     * 挑战查询("副本名称")
     * 根据副本名称和玩家ID 返回 数量   如果不存在就新建一个  数量为0
     * 挑战增加("副本名称"，数量加减)
     * 根据副本名称和玩家ID 数量+或者-
     *
     * @return
     */

    static int select(int playerId, String fuben) {
        PreparedStatement ps;
        int count = 0;
        try {
            ps = ConnExt.getConn().prepareStatement("select t.shuliang from fuben_tiaozhan as t where t.charid = ? and t.fuben = ?");
            ps.setInt(1, playerId);
            ps.setString(2, fuben);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    static int insert(int playerId, String playerName, String fuben, int count) {
        PreparedStatement ps;
        try {
            boolean have;
            {
                int cc = 0;
                ps = ConnExt.getConn().prepareStatement("select count(*) from fuben_tiaozhan as t where t.charid = ? and t.fuben = ?");
                ps.setInt(1, playerId);
                ps.setString(2, playerName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cc = rs.getInt(1);
                }
                rs.close();
                ps.close();
                have = cc > 0;
            }

            {
                if (have) {
                    ps = ConnExt.getConn().prepareStatement("update fuben_tiaozhan as t set t.shuliang = t.shuliang + ? where t.charid = ? and t.fuben = ?");
                    ps.setInt(1, count);
                    ps.setInt(2, playerId);
                    ps.setString(3, fuben);
                    ps.executeUpdate();
                    ps.close();
                } else {
                    ps = ConnExt.getConn().prepareStatement("insert into fuben_tiaozhan(charid, charname, fuben, shuliang) values (?,?,?,?)");
                    ps.setInt(1, playerId);
                    ps.setString(2, playerName);
                    ps.setString(3, fuben);
                    ps.setInt(4, count);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                    rs.close();
                    ps.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
}
