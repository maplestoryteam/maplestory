package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LotteryItem {
    private int type;
    private int itemId;
    private int itemLevel;
    private int chance;
    private String remark;

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

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LotteryItem(int type, int itemId, int itemLevel, int chance, String remark) {
        this.type = type;
        this.itemId = itemId;
        this.itemLevel = itemLevel;
        this.chance = chance;
        this.remark = remark;
    }

    public LotteryItem(ResultSet rs) {
        try {
            this.type = rs.getInt(1);
            this.itemId = rs.getInt(2);
            this.itemLevel = rs.getInt(3);
            this.chance = rs.getInt(4);
            this.remark = rs.getString(5);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}
