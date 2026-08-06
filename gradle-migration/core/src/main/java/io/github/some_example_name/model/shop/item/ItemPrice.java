package io.github.some_example_name.model.shop.item;

import io.github.some_example_name.model.enums.CurrencyType;

public class ItemPrice {
    private final int amount;
    private final CurrencyType type;

    public ItemPrice(int amount, CurrencyType type) {
        this.amount = amount;
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public CurrencyType getType() {
        return type;
    }
}