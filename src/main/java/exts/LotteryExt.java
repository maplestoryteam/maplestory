package exts;

import exts.model.Lottery;
import exts.model.LotteryItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface LotteryExt {

    static boolean queryItemExists(int characterId, int type, int itemId) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement(String.format("SELECT COUNT(*) FROM lottery AS l WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_id` = %d", characterId, type, itemId));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean addItem(int characterId, int type, int itemId, int itemCount, int itemType) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement(String.format("INSERT INTO `lottery`(`character_id`,`item_id`,`item_type`,`item_count`,`type`) VALUES (%d,%d,%d,%d,%d)", characterId, itemId, itemType, itemCount, type));
            int rt = ps.executeUpdate();
            ps.close();
            return rt > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean updateItem(int characterId, int type, int itemId, int itemCount) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement(String.format("UPDATE lottery AS l SET l.`item_count` = l.`item_count` + %d WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_id` = %d", itemCount, characterId, type, itemId));
            int rt = ps.executeUpdate();
            ps.close();
            return rt > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean deleteItem(int characterId, int type, int itemId, int itemCount) {
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement(String.format("UPDATE lottery AS l SET l.`item_count` = l.`item_count` - %d WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_id` = %d", itemCount, characterId, type, itemId));
            int ret = ps.executeUpdate();
            ps.close();

            ps = ConnExt.getConn().prepareStatement(String.format("DELETE FROM lottery WHERE `item_count` <= 0 AND `character_id` = %d AND `type` = %d AND `item_id` = %d", characterId, type, itemId));
            ps.executeUpdate();
            ps.close();

            return ret > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static List<Lottery> query(int characterId, int type, int itemType) {
        List<Lottery> list = new ArrayList<>();
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement(String.format("SELECT l.* FROM lottery AS l WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_type` = %d", characterId, type, itemType));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Lottery(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    static int query(int characterId, int itemId) {
        int count = 0;
        PreparedStatement ps;
        try {
            ps = ConnExt.getConn().prepareStatement(String.format("SELECT l.`item_count` FROM lottery AS l WHERE l.`character_id` = ? AND l.`item_id` = ?", characterId, itemId));
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

    static String getLotteryResultString(List<LotteryItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("您抽奖的物品如下\r\n");
        sb.append("--------------------------------------\r\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append((i + 1) + ")#v" + items.get(i).getItemId() + "##z" + items.get(i).getItemId() + "#\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    static String getLotteryMessage(int type, String name, int count) {
        if (type == 0) {
            return "[幸运抽奖] : 恭喜玩家 " + name + " 在幸运抽奖 " + count + " 连抽中获得稀有物品!~";
        } else if (type == 1) {
            return "[飞天猪抽奖] : 恭喜玩家 " + name + " 在飞天猪抽奖 " + count + " 连抽中获得稀有物品!~";
        }
        return "";
    }

}