package model.shop.item;

public abstract class ShopItem {
    private final String itemId;
    private String itemName;
    private ItemPrice price;
    private final int itemUnit;
    private final ItemType type;

    public ShopItem(String itemName, ItemPrice price, int itemUnit, String itemId, ItemType type) {
        this.itemName = itemName;
        this.price = price;
        this.itemUnit = itemUnit;
        this.itemId = itemId;
        this.type = type;
    }

}
