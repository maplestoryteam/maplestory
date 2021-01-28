package exts;

import database.DatabaseConnection;
import exts.model.Lottery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface LotteryExt {

    static boolean queryItemExists(int characterId, int type, int itemId) {
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(String.format("SELECT COUNT(*) FROM lottery AS l WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_id` = %d", characterId, type, itemId));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean addItem(int characterId, int type, int itemId, int itemCount, int itemType) {
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(String.format("INSERT INTO `lottery`(`character_id`,`item_id`,`item_type`,`item_count`,`type`) VALUES (%d,%d,%d,%d,%d)", characterId, itemId, itemType, itemCount, type));
            int rt = ps.executeUpdate();
            ps.close();
            conn.close();
            return rt > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean updateItem(int characterId, int type, int itemId, int itemCount) {
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(String.format("UPDATE lottery AS l SET l.`item_count` = l.`item_count` + %d WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_id` = %d", itemCount, characterId, type, itemId));
            int rt = ps.executeUpdate();
            ps.close();
            conn.close();
            return rt > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static boolean deleteItem(int characterId, int type, int itemId, int itemCount) {
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(String.format("UPDATE lottery AS l SET l.`item_count` = l.`item_count` - %d WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_id` = %d", itemCount, characterId, type, itemId));
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement(String.format("DELETE FROM lottery WHERE `item_count` <= 0 AND `character_id` = %d AND `type` = %d AND `item_id` = %d", characterId, type, itemId));
            ps.executeUpdate();
            ps.close();

            conn.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static List<Lottery> query(int characterId, int type, int itemType) {
        List<Lottery> list = new ArrayList<>();
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(String.format("SELECT l.* FROM lottery AS l WHERE l.`character_id` = %d AND l.`type` = %d AND l.`item_type` = %d", characterId, type, itemType));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Lottery(rs));
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    static int query(int characterId, int itemId) {
        int count = 0;
        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement(String.format("SELECT l.`item_count` FROM lottery AS l WHERE l.`character_id` = ? AND l.`item_id` = ?", characterId, itemId));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }
}
