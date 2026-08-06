package io.github.some_example_name.model.shop.item;

public class DailyOffer extends ShopItem {

    public DailyOffer(String itemName, ItemPrice price, int itemUnit, String itemId) {
        super(itemId, itemName, price, itemUnit, ItemType.DAILY_OFFER);
    }

}