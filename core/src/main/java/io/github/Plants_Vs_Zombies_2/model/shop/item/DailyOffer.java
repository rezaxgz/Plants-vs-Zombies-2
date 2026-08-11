package io.github.Plants_Vs_Zombies_2.model.shop.item;

public class DailyOffer extends ShopItem {

    public DailyOffer(String itemName, ItemPrice price, int itemUnit, String itemId) {
        super(itemId, itemName, price, itemUnit, ItemType.DAILY_OFFER);
    }

}