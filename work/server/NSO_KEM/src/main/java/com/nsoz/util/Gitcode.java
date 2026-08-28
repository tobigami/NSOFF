package com.nsoz.util;

import com.nsoz.item.Item;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class Gitcode {
    String code;
    long xu;
    long yen;
    long luong;
    JSONObject items;

    public Gitcode(String code, long xu, long yen, long luong,JSONObject items) {
        this.code = code;
        this.xu = xu;
        this.yen = yen;
        this.luong = luong;
        this.items = items;

    }
}
