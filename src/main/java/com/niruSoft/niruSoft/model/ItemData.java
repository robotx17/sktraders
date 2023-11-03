package com.niruSoft.niruSoft.model;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
public class ItemData {
    private String item;
    private int qty;
    private int rate;
    private int count;
    private int amount;

    public ItemData(String item, int qty, int rate, int count, int amount) {
        this.item = item;
        this.qty = qty;
        this.rate = rate;
        this.count = count;
        this.amount = amount;
    }

    public String getItem() {
        return item;
    }

    public int getQty() {
        return qty;
    }

    public int getRate() {
        return rate;
    }

    public int getCount() {
        return count;
    }

    public int getAmount() {
        return amount;
    }

//    private String items;
//    @Getter
//    private int qty;
//    private int rate;
//    private int count;
//    private int amount;
//
//    public ItemData(String item, int qty, int rate, int count, int amount) {
//        this.items = item;
//        this.qty = qty;
//        this.rate = rate;
//        this.count = count;
//        this.amount = amount;
//    }

}
