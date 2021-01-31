package exts;

import exts.model.TimeSaleItem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface TimeSaleItemExt {
    static List<TimeSaleItem> query(int type) {
        List<TimeSaleItem> tsItems = new ArrayList<>();
        PreparedStatement ps;
        try {
            //   type  item_id  item_count    meso    cash  remark
            //------  -------  ----------  ------  ------  --------
            ps = ConnExt.getConn().prepareStatement(String.format("SELECT * FROM timesale_item AS tsi WHERE tsi.type = %d", type));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tsItems.add(new TimeSaleItem(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tsItems;
    }

    static TimeSaleItem query(int type, int itemId) {
        PreparedStatement ps;
        try {
            //   type  item_id  item_count    meso    cash  remark
            //------  -------  ----------  ------  ------  --------
            ps = ConnExt.getConn().prepareStatement(String.format("SELECT * FROM timesale_item AS tsi WHERE tsi.type = %d AND tsi.item_id = %d", type, itemId));
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            rs.close();
            ps.close();
            return new TimeSaleItem(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    Object LOCK = 0;

    static void update(int type, int itemId, int itemCount) {
        synchronized (LOCK) {
            PreparedStatement ps;
            try {
                ps = ConnExt.getConn().prepareStatement(String.format("UPDATE timesale_item AS tsi SET tsi.`item_count` = tsi.`item_count` - %d WHERE tsi.type = %d AND tsi.`item_id` = %d AND tsi.`item_count` >= %d", itemCount, type, itemId, itemCount));
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
