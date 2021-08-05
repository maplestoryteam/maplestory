package exts.model;

import handling.channel.ChannelServer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShiTuCharacter {
    private int shituId;
    private int characterId;
    private String characterName;
    private int state;
    private int contribution;
    private int meso;
    private String joinTime;
    private int level;
    private int loggin;

    public ShiTuCharacter() {
    }

    public ShiTuCharacter(ResultSet rs) {
        try {
            this.shituId = rs.getInt("shitu_id");
            this.characterId = rs.getInt("character_id");
            this.characterName = rs.getString("character_name");
            this.state = rs.getInt("state");
            this.contribution = rs.getInt("contribution");
            this.meso = rs.getInt("meso");
            this.joinTime = rs.getString("jointime");
            this.level = rs.getInt("level");
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                if (cs.getPlayerStorage().getCharacterById(this.characterId) != null) {
                    this.loggin = 1;
                    return;
                }
            }
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

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    public String getCharacterName() {
        return characterName;
    }

    public void setCharacterName(String characterName) {
        this.characterName = characterName;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getContribution() {
        return contribution;
    }

    public void setContribution(int contribution) {
        this.contribution = contribution;
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        this.meso = meso;
    }

    public String getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(String joinTime) {
        this.joinTime = joinTime;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLoggin() {
        return loggin;
    }

    public void setLoggin(int loggin) {
        this.loggin = loggin;
    }
}
