package io.github.Plants_Vs_Zombies_2.model.shop.item;

public class ShopItem {
    private final String id;
    private final String name;
    private final ItemPrice price;
    private final int unit;
    private final ItemType type;

    public ShopItem(String id, String name, ItemPrice price, int unit, ItemType type) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.unit = unit;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ItemPrice getPrice() {
        return price;
    }

    public int getUnit() {
        return unit;
    }

    public ItemType getType() {
        return type;
    }
}