package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShiTu {
    private int id;
    private String name;
    private int masterId;
    private String masterName;
    private int secondId;
    private String secondName;
    private int contribution;
    private int reward;
    private int place;
    private int stock;
    private String note;
    private String createTime;
    private String expireTime;

    public ShiTu() {
    }

    public ShiTu(ResultSet rs) {
        try {
            this.id = rs.getInt("id");
            this.name = rs.getString("name");
            this.masterId = rs.getInt("master_id");
            this.masterName = rs.getString("master_name");
            this.secondId = rs.getInt("second_id");
            this.secondName = rs.getString("second_name");
            this.contribution = rs.getInt("contribution");
            this.reward = rs.getInt("reward");
            this.place = rs.getInt("place");
            this.stock = rs.getInt("stock");
            this.note = rs.getString("note");
            this.createTime = rs.getString("createtime");
            this.expireTime = rs.getString("expiretime");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMasterId() {
        return masterId;
    }

    public void setMasterId(int masterId) {
        this.masterId = masterId;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public int getSecondId() {
        return secondId;
    }

    public void setSecondId(int secondId) {
        this.secondId = secondId;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public int getContribution() {
        return contribution;
    }

    public void setContribution(int contribution) {
        this.contribution = contribution;
    }

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }
}
