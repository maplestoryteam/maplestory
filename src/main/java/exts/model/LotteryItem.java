package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LotteryItem {
    private int type;
    private int itemId;
    private int itemLevel;
    private int itemType;
    private int chance;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }


    public LotteryItem(int type, int itemId, int itemLevel, int itemType, int chance) {
        this.type = type;
        this.itemId = itemId;
        this.itemLevel = itemLevel;
        this.itemType = itemType;
        this.chance = chance;
    }

    public LotteryItem(ResultSet rs) {
        try {
            this.type = rs.getInt(1);
            this.itemId = rs.getInt(2);
            this.itemLevel = rs.getInt(3);
            this.itemType = rs.getInt(4);
            this.chance = rs.getInt(5);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}
