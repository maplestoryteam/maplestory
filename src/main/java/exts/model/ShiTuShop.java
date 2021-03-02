package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShiTuShop {
    private int shituId;
    private int itemId;
    private int itemCount;
    private int itemLevel;
    private int saleType;
    private int salePrice;

    public ShiTuShop() {
    }

    public ShiTuShop(ResultSet rs) {
        try {
            this.shituId = rs.getInt("shitu_id");
            this.itemId = rs.getInt("item_id");
            this.itemCount = rs.getInt("item_count");
            this.itemLevel = rs.getInt("item_level");
            this.saleType = rs.getInt("sale_type");
            this.salePrice = rs.getInt("sale_price");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getShituId() {
        return shituId;
    }

    public void setShituId(int shituId) {
        this.shituId = shituId;
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

    public int getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(int itemLevel) {
        this.itemLevel = itemLevel;
    }

    public int getSaleType() {
        return saleType;
    }

    public void setSaleType(int saleType) {
        this.saleType = saleType;
    }

    public int getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(int salePrice) {
        this.salePrice = salePrice;
    }
}
