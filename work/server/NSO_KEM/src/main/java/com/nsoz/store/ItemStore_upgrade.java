/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.store;
// shop 16
import com.nsoz.item.ItemManager;
import com.nsoz.item.ItemTemplate;
import com.nsoz.option.ItemOption;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Administrator
 */
@Getter
@Setter
public class ItemStore_upgrade {

    private int id;
    private int itemID;
    private byte sys;
    private int coin;
    private int yen;
    private int gold;
    private boolean isLock;
    private long expire;
    private List<ItemOption> options;
    private ItemTemplate template;
    private int upgrade;
    @Builder
    public ItemStore_upgrade(int id, int itemID, byte sys, int coin, int yen, int gold, boolean isLock, long expire,int upgrade,
                             List<ItemOption> options) {
        this.id = id;
        this.itemID = itemID;
        this.sys = sys;
        this.coin = coin;
        this.yen = yen;
        this.gold = gold;
        this.isLock = isLock;
        this.expire = expire;
       this.options = options;
       this.upgrade = upgrade;
        this.template = ItemManager.getInstance().getItemTemplate(itemID);
    }


}
