/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.map.item;

import com.nsoz.util.TimeUtils;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class MagicItem extends ItemMap {

    public MagicItem(short id) {
        super(id);
    }

    @Override
    public boolean isExpired() {
        return TimeUtils.canDoWithTime(createdAt, 900000);
    }

}
