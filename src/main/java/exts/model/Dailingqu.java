package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Dailingqu {

    private int charid;
    private int leibie;
    private int shuliang;
    private String beizhu;

    public Dailingqu(ResultSet rs) throws SQLException {
        this.charid = rs.getInt("charid");
        this.leibie = rs.getInt("leibie");
        this.shuliang = rs.getInt("shuliang");
        this.beizhu = rs.getString("beizhu");
    }

    public int getCharid() {
        return charid;
    }

    public void setCharid(int charid) {
        this.charid = charid;
    }

    public int getLeibie() {
        return leibie;
    }

    public void setLeibie(int leibie) {
        this.leibie = leibie;
    }

    public int getShuliang() {
        return shuliang;
    }

    public void setShuliang(int shuliang) {
        this.shuliang = shuliang;
    }

    public String getBeizhu() {
        return beizhu;
    }

    public void setBeizhu(String beizhu) {
        this.beizhu = beizhu;
    }
}
