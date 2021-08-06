package exts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface VarsExt {

    int MAX_ATTACK = 10000000;//1000w

    static void initMaxLevel() {
        try {
            Connection conn = ConnExt.getConn();
            PreparedStatement ps = conn.prepareStatement("select c.conf from config as c where c.name = 'max_level'");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LEVEL.MAX = Integer.parseInt(rs.getString(1));
                System.out.println("初始化最高等级======>" + LEVEL.MAX);
            }
            rs.close();
            ps.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void saveMaxLevel() {
        try {
            Connection conn = ConnExt.getConn();
            PreparedStatement ps = conn.prepareStatement("update config as c set c.conf = ? where c.name = 'max_level'");
            ps.setInt(1, LEVEL.MAX);
            if (ps.executeUpdate() > 0) {
                System.out.println("保存最高等级======>" + LEVEL.MAX);
            }
            ps.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class LEVEL {
        public static int MAX = 255;
    }
}

