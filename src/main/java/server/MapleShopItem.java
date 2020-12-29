package server;

public class MapleShopItem {

    private final short buyable;
    private final int itemId;
    private final int price;

    public MapleShopItem(short buyable, int itemId, int price) {
        this.buyable = buyable;
        this.itemId = itemId;
        this.price = price;
    }

    public short getBuyable() {
        return buyable;
    }

    public int getItemId() {
        return itemId;
    }

    public int getPrice() {
        return price;
    }

}
