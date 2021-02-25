package exts.model;

public class PlayerMapTime {
    private String character;
    private int mapId;
    private long enter;
    private int minute;

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public int getMapId() {
        return mapId;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
    }

    public long getEnter() {
        return enter;
    }

    public void setEnter(long enter) {
        this.enter = enter;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public PlayerMapTime(String character, int mapId, long enter) {
        this.character = character;
        this.mapId = mapId;
        this.enter = enter;
    }
}