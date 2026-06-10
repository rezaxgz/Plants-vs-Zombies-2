package model.shop.item;

import model.shop.Shop;

public class DailyOffer extends ShopItem {

    public DailyOffer(String itemName, ItemPrice price, int itemUnit, String itemId) {
        super(itemName, price, itemUnit, itemId, ItemType.DAILY_OFFER);
    }

}