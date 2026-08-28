package com.nsoz.util;

import com.nsoz.constants.ItemName;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.item.ItemManager;
import com.nsoz.model.GiftCode;
import com.nsoz.server.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class CreateCode {

    public static void main(String[] args) {
        if (Config.getInstance().load()) {
            if (!DbManager.start()) {
                return;
            }
            ItemManager.getInstance().load();
            ItemManager.getInstance().init();
            ItemManager.getInstance().setData();
            Item item = new Item(ItemName.CHUYEN_TINH_THACH);

            JSONObject items = new JSONObject();
            JSONObject options = new JSONObject();
            items.put("id",item.id);
            items.put("options",item.options);
            System.out.println(items);
//            items.add(new Item())
            Gitcode gitcode = new  Gitcode("1",1000000000,100000000,1000000000,items);
            System.out.println(
                    gitcode
            );

        }
    }
}
