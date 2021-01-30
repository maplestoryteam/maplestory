package exts.model;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Lottery {
    private int type;
    private int characterId;
    private int itemId;
    private int itemType;
    private int itemCount;

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

    public Lottery(int type, int characterId, int itemId, int itemType, int itemCount) {
        this.type = type;
        this.characterId = characterId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.itemCount = itemCount;
    }

    public Lottery(ResultSet rs) {
        try {
            //  type  character_id  item_id  item_type  item_count
            this.type = rs.getInt(1);
            this.characterId = rs.getInt(2);
            this.itemId = rs.getInt(3);
            this.itemType = rs.getInt(4);
            this.itemCount = rs.getInt(5);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

}
