package exts;

import database.DatabaseConnection;
import exts.model.FishReward;
import server.Randomizer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public interface FishExt {
    List<FishReward> fishRewards = new ArrayList<>();

    static void refreshFishRewareds() {
        try {
            System.out.println("开始读取抽奖物品");
            Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT t.itemid,t.chance,t.count,t.name FROM fishing_rewards AS t");
            fishRewards.clear();
            while (rs.next()) {
                int itemid = rs.getInt(1);
                int chance = rs.getInt(2);
                byte count = rs.getByte(3);
                String itemName = rs.getString(4);
                fishRewards.add(new FishReward(itemid, itemName, chance, count));
            }
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            System.out.println("抽奖物品装载完毕");
        } catch (SQLException ex) {
            System.err.println("Error refreshFishRewareds " + ex);
        }
    }

    static FishReward randomItem() {
        if (fishRewards.size() <= 0) {
            return null;
        }
        return fishRewards.get(Randomizer.nextInt(fishRewards.size()));
    }

}
