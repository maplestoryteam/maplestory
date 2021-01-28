package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Lottery {
    private int id;
    private int type;
    private int characterId;
    private int itemId;
    private int itemType;
    private int itemCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCharacterId() {
        return characterId;
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public Lottery(int id, int type, int characterId, int itemId, int itemType, int itemCount) {
        this.id = id;
        this.type = type;
        this.characterId = characterId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemCount = itemCount;
    }


    public Lottery(ResultSet rs) {
        try {
            this.id = rs.getInt(1);
            this.type = rs.getInt(2);
            this.itemId = rs.getInt(3);
            this.characterId = rs.getInt(4);
            this.itemId = rs.getInt(5);
            this.itemType = rs.getInt(6);
            this.itemCount = rs.getInt(7);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}
