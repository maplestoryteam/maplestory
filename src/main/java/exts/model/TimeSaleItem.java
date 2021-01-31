package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TimeSaleItem {
    private int type;
    private int itemId;
    private int itemCount;
    private int meso;
    private int cash;
    private String remark;

    public TimeSaleItem(int type, int itemId, int itemCount, int meso, int cash, String remark) {
        this.type = type;
        this.itemId = itemId;
        this.itemCount = itemCount;
        this.meso = meso;
        this.cash = cash;
        this.remark = remark;
    }

    public TimeSaleItem(ResultSet rs) {
        try {
            this.type = rs.getInt(1);
            this.itemId = rs.getInt(2);
            this.itemCount = rs.getInt(3);
            this.meso = rs.getInt(4);
            this.cash = rs.getInt(5);
            this.remark = rs.getString(6);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

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

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        this.meso = meso;
    }

    public int getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
