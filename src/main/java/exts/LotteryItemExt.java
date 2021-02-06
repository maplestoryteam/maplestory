package exts;

import exts.model.LotteryItem;
import server.Randomizer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface LotteryItemExt {
    Map<Integer, List<LotteryItem>> lotterItemMap = new HashMap<>();

    static List<LotteryItem> query(int type) {
        return lotterItemMap.get(type);
    }

    static LotteryItem lottery(int type) {
        List<LotteryItem> items = lotterItemMap.get(type);
        for (int i = 0; i < items.size(); i++) {
            LotteryItem item = items.get(Randomizer.nextInt(items.size()));
            if (Randomizer.nextInt(item.getChance() + 1) == item.getChance()) {
                return item;
            }
        }
        return null;
    }

    static void refreshLotteryItem() {
        synchronized (lotterItemMap) {
            lotterItemMap.clear();
            System.out.println("开始读取抽奖物品");
            List<LotteryItem> lotteryItems = new ArrayList<>();
            try {
                PreparedStatement ps = ConnExt.getConn().prepareStatement("SELECT li.`type`,li.`item_id`,li.`item_level`,li.`item_type`,li.`chance` FROM lottery_item AS li");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    lotteryItems.add(new LotteryItem(rs));
                }
                rs.close();
                ps.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            lotterItemMap.putAll(lotteryItems.parallelStream().collect(Collectors.groupingBy(LotteryItem::getType)));
            System.out.println("抽奖物品装载完毕");
        }
    }
}
