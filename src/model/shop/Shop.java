package model.shop;

import java.util.List;

import model.shop.item.ShopItem;

public class Shop {
    private List<ShopItem> permanentItems;
    private List<ShopItem> dailyOffer;

    private List<ShopItem> generateDailyOffer() {
        return null;
    }

    private void setDailyOffer() {
        this.dailyOffer = generateDailyOffer();
    }
}
