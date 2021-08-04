package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShiTuRenwu {
    private int renwuId;
    private int shituId;
    private int itemId;
    private int shuliang;
    private int leiji;
    private int riqi;

    public ShiTuRenwu() {
    }

    public ShiTuRenwu(ResultSet rs) {
        try {
            this.renwuId = rs.getInt("renwu_id");
            this.shituId = rs.getInt("shitu_id");
            this.itemId = rs.getInt("item_id");
            this.shuliang = rs.getInt("shuliang");
            this.leiji = rs.getInt("leiji");
            this.riqi = rs.getInt("riqi");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getRenwuId() {
        return renwuId;
    }

    public void setRenwuId(int renwuId) {
        this.renwuId = renwuId;
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

    public int getShuliang() {
        return shuliang;
    }

    public void setShuliang(int shuliang) {
        this.shuliang = shuliang;
    }

    public int getLeiji() {
        return leiji;
    }

    public void setLeiji(int leiji) {
        this.leiji = leiji;
    }

    public int getRiqi() {
        return riqi;
    }

    public void setRiqi(int riqi) {
        this.riqi = riqi;
    }
}
