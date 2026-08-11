package io.github.Plants_Vs_Zombies_2.model.shop;

import java.util.Arrays;
import java.util.List;

import io.github.Plants_Vs_Zombies_2.model.enums.CurrencyType;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ItemPrice;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ItemType;
import io.github.Plants_Vs_Zombies_2.model.shop.item.ShopItem;

public class Shop {
    public static final List<ShopItem> PERMANENT_ITEMS = Arrays.asList(
            new ShopItem("pot", "Pot", new ItemPrice(2000, CurrencyType.COIN), 1, ItemType.POT),
            new ShopItem("plant_food", "Plant Food", new ItemPrice(3, CurrencyType.DIAMOND), 1, ItemType.PLANT_FOOD),
            new ShopItem("random_seed", "Random Seed Packet", new ItemPrice(1000, CurrencyType.COIN), 5,
                    ItemType.RANDOM_SEED_PACK),
            new ShopItem("selective_seed", "Selective Seed Packet", new ItemPrice(5, CurrencyType.DIAMOND), 10,
                    ItemType.SELECTIVE_SEED_PACK),
            new ShopItem("exchange", "Currency Exchange", new ItemPrice(5, CurrencyType.DIAMOND), 500,
                    ItemType.CURRENCY_EXCHANGE));

    public static ShopItem getItemById(String id) {
        for (ShopItem item : PERMANENT_ITEMS) {
            if (item.getId().equalsIgnoreCase(id)) {
                return item;
            }
        }
        return null;
    }
}