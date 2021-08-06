package exts;

import handling.channel.ChannelServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface ChongzhiExt {

    static void 推荐人找回(int accid, int charid, String charname) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("insert into chongzhi(accid, charid, charname, tjcharid, tjcharname)\n" +
                    "select t.accid,?,?,t.tjcharid,t.tjcharname from chongzhi as t where t.accid = ?");
            ps.setInt(1, charid);
            ps.setString(2, charname);
            ps.setInt(3, accid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static Object[] 推荐人查找(int charid) {
        PreparedStatement ps;
        Object[] result = {0, ""};
        try {
            ps = ConnExt.getConn().prepareStatement("select t.tjcharid,t.tjcharname from chongzhi as t where t.charid = ? limit 0,1");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = new Object[]{rs.getInt(1), rs.getString(2)};
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    static boolean 推荐人填写(int charid, int tjcharid, String tjcharname) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement("update chongzhi as t set t.tjcharid = ?,t.tjcharname = ? where t.charid = ? and t.tjcharid = 0");
            ps.setInt(1, tjcharid);
            ps.setString(2, tjcharname);
            ps.setInt(3, charid);
            if (ps.executeUpdate() > 0) {
                ps.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static Integer[] 充值查询(int charid) {
        PreparedStatement ps;
        Integer[] result = new Integer[]{0, 0};
        try {
            ps = ConnExt.getConn().prepareStatement("select t.shuliang,t.jifen from chongzhi as t where t.charid = ?");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = new Integer[]{rs.getInt(1), rs.getInt(2)};
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    static boolean 充值累计(int accid, int charid, String charname, int shuliang, int jifen) {
        PreparedStatement ps;
        {
            try {
                ps = ConnExt.getConn().prepareStatement("select count(*) from chongzhi as t where t.charid = ?");
                ps.setInt(1, charid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int c = rs.getInt(1);
                    if (c <= 0) {
                        ps = ConnExt.getConn().prepareStatement("insert into chongzhi(accid, charid, charname, tjcharid, tjcharname) values(?,?,?,0,'')");
                        ps.setInt(1, accid);
                        ps.setInt(2, charid);
                        ps.setString(3, charname);
                        ps.executeUpdate();
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        {
            try {
                ps = ConnExt.getConn().prepareStatement("update chongzhi as t\n" +
                        "set t.shuliang = t.shuliang + ?,\n" +
                        "    t.jifen    = t.jifen + ?\n" +
                        "where t.charid = ?\n" +
                        "  and (t.shuliang + ?) > 0\n" +
                        "  and (t.jifen + ?) > 0");
                ps.setInt(1, shuliang);
                ps.setInt(2, jifen);
                ps.setInt(3, charid);
                ps.setInt(4, shuliang);
                ps.setInt(5, jifen);
                if (ps.executeUpdate() > 0) {
                    ps.close();
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    static int 查看被邀请人数(int charid) {
        PreparedStatement ps;
        int count = 0;
        try {
            ps = ConnExt.getConn().prepareStatement("select count(*) from chongzhi as t where t.tjcharid = ?");
            ps.setInt(1, charid);
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

    static List<Object[]> 查看被邀请人(int charid) {
        PreparedStatement ps;
        List<Object[]> result = new ArrayList<>();
        try {
            ps = ConnExt.getConn().prepareStatement("select t.charname, c.level, t.shuliang,t.charid\n" +
                    "from chongzhi as t,\n" +
                    "     characters as c\n" +
                    "where t.tjcharid = ?\n" +
                    "  and t.charid = c.id");
            ps.setInt(1, charid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean loggin = false;
                for (ChannelServer cs : ChannelServer.getAllInstances()) {
                    if (loggin = cs.getPlayerStorage().getCharacterById(rs.getInt(4)) != null) {
                        break;
                    }
                }
                result.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getInt(3), loggin});
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}