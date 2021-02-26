package exts;

import client.MapleCharacter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface CDKExt {
    Object LOCK = "lock";

    static int charge(MapleCharacter player, String cdk) {
        synchronized (LOCK) {
            PreparedStatement ps;
            try {
                ps = ConnExt.getConn().prepareStatement("SELECT ck.account_id,ck.value FROM cdkey AS ck WHERE ck.`cdkey` = ?");
                ps.setString(1, cdk);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int accountId = rs.getInt(1);
                    int value = rs.getInt(2);
                    rs.close();
                    ps.close();
                    if (accountId != 0) {
                        // used
                        return 0;
                    } else {
                        ps = ConnExt.getConn().prepareStatement("UPDATE cdkey AS ck SET ck.`account_id` = ?,ck.`character_id` = ?, ck.`character_name` = ?, ck.`use_time` = NOW() WHERE ck.`cdkey` = ?");
                        ps.setInt(1, player.getAccountID());
                        ps.setInt(2, player.getId());
                        ps.setString(3, player.getName());
                        ps.setString(4, cdk);
                        if (ps.executeUpdate() > 0) {
                            ps.close();
                            return value;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }
}
