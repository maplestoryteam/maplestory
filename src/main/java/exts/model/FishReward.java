package exts.model;

public class FishReward {
    private int itemId;
    private String itemName;
    private int chance;
    private byte count;

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public byte getCount() {
        return count;
    }

    public void setCount(byte count) {
        this.count = count;
    }

    public FishReward(int itemId, String itemName, int chance, byte count) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.chance = chance;
        this.count = count;
    }
}
