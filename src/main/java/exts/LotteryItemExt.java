package exts;

import database.DatabaseConnection;
import exts.model.LotteryItem;
import server.Randomizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public interface LotteryItemExt {

    static List<LotteryItem> query(int type) {
        return LotteryItemLoad.lotterItemMap.get(type);
    }

    static int lottery(int type) {
        List<LotteryItem> items = LotteryItemLoad.lotterItemMap.get(type);
        long count = items.parallelStream().filter(i -> {
            if (Randomizer.nextInt(i.getChance() + 1) == i.getChance()) {
                return true;
            }
            return false;
        }).count();
        int itemId = 0;//meso
        if (count <= 0) {
            Optional<LotteryItem> op = items.parallelStream().sorted(Comparator.comparingInt(LotteryItem::getChance)).findFirst();
            if (op.isPresent()) {
                itemId = op.get().getItemId();
            }
        }
        return itemId;
    }

    class LotteryItemLoad {
        public static Map<Integer, List<LotteryItem>> lotterItemMap;

        static {
            List<LotteryItem> lotteryItems = new ArrayList<>();
            try {
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT li.`type`,li.`item_id`,li.`item_level`,li.`item_type`,li.`chance` FROM lottery_item AS li");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    lotteryItems.add(new LotteryItem(rs));
                }
                rs.close();
                ps.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            lotterItemMap = lotteryItems.parallelStream().collect(Collectors.groupingBy(LotteryItem::getType));
        }
    }
}
