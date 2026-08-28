package com.nsoz.store;
// shop 16
import com.nsoz.constants.CMDConfirmPopup;
import com.nsoz.constants.ItemName;
import com.nsoz.constants.SQLStatement;
import com.nsoz.convert.Converter;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.event.Event;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemTemplate;
import com.nsoz.model.Char;
import com.nsoz.model.ConfirmPopup;
import com.nsoz.model.History;
import com.nsoz.option.ItemOption;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import com.nsoz.util.ProgressBar;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *@project Zone.java
 *@author soanlv
 *@created 11/$02/2025 - 10:57 PM
 */
public class StoreUpgradeManager {
    public List<ItemStore_upgrade> items = new ArrayList<>();

    @Getter
    private static final StoreUpgradeManager instance = new StoreUpgradeManager();

    public boolean load() {
        try (Connection conn = DbManager.getConnection();) {

            PreparedStatement stmt = conn.prepareStatement(SQLStatement.GET_ALL_STORE_UPGRADE,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.last();
            ProgressBar pb = new ProgressBar("Cửa hàng", resultSet.getRow());
            resultSet.beforeFirst();
            while (resultSet.next()) {
                try {
                    int id = resultSet.getInt("id");
                    int itemID = resultSet.getInt("item_id");
                    boolean lock = resultSet.getBoolean("isLock");
                    int coin = resultSet.getInt("coin");
                    int gold = resultSet.getInt("gold");
                    int upgrade = resultSet.getInt("upgrade");
                    byte sys = 0;
                    long expire = resultSet.getLong("expire");
                    JSONArray jArr = new JSONArray(resultSet.getString("options"));
                    List<ItemOption> options = new ArrayList<>();
                    if (jArr.length() > 0) {
                        for (int i = 0; i < jArr.length(); i++) {
                            JSONObject obj = jArr.getJSONObject(i);
                            int oID = obj.getInt("id");
                            int oParam = obj.getInt("param");
                            options.add(new ItemOption(oID, oParam));
                        }
                    } else {
                        ItemStore store = StoreManager.getInstance().getItemWithId(itemID);
                        if (store != null) {
                            List<ItemOption> maxOptions = store.getMaxOptions();
                            options = maxOptions;
                        }else{
                            Item newItem = new Item(itemID);
                            if (newItem.template.isTypeBody() && newItem.template.level >= 90 && newItem.template.level <= 99) {

                                newItem.randomOptionItem9x(true);
                            } else if (newItem.template.isTypeBody() && newItem.template.level >= 100 && newItem.template.level <= 120) {

                                newItem.randomOptionItem10x(true);
                            } else {
                                newItem.initOption();
                            }
                            options = newItem.options;
                        }
                    }
                    ItemStore_upgrade item = new ItemStore_upgrade(id, itemID, sys, coin, 0, gold, lock, expire, upgrade, options);
                    items.add(item);
                    pb.setExtraMessage(item.getItemID() + " finished!");
                    pb.step();
                } catch (Exception e) {
                    pb.setExtraMessage(e.getMessage());
                    pb.reportError();
                    return false;
                }
            }
            pb.setExtraMessage("Finished!");
            pb.reportSuccess();
            resultSet.close();
            stmt.close();
            return true;
        } catch (SQLException e) {
            Log.logException("Load data store error:", Store.class, e);

            return false;
        }
    }

    public ItemStore_upgrade get(int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }
        return items.get(index);
    }

    public void buyComfirm(Char p, int indexUI, int quantity) {
        ItemStore_upgrade item = get(indexUI);
        if (item == null) {
            return;
        }
        ItemTemplate template = item.getTemplate();
        int slotNull = p.getSlotNull();
        if ((template.isUpToUp && slotNull == 0) || (!template.isUpToUp && slotNull < quantity)) {
            p.warningBagFull();
            return;
        }
        long giaXu = ((long) item.getCoin()) * ((long) quantity);
        long giaYen = ((long) item.getYen()) * ((long) quantity);
        long giaLuong = ((long) item.getGold()) * ((long) quantity);
        if (giaXu < 0 || giaYen < 0 || giaLuong < 0) {
            return;
        }
        long gia = 0;
        String str = "";
        if (giaXu > 0) {
            gia = giaXu;
            str = "xu";
        }
        if (giaLuong > 0) {
            gia = giaLuong;
            str = "lượng";
        }
        if (giaYen > 0) {
            gia = giaYen;
            str = "yên";
        }
        p.setConfirmPopup(new ConfirmPopup(CMDConfirmPopup.BUY_STORE_UPGRADE, String.format("Bạn muốn mua %s với giá %s %s không?", item.getTemplate().name, NinjaUtils.getCurrency(gia), str)));
        p.getService().openUIConfirmID();
    }

    public void buy(Char p, int indexUI, int quantity) {
        ItemStore_upgrade item = get(indexUI);
        if (item == null) {
            return;
        }
        ItemTemplate template = item.getTemplate();
        int slotNull = p.getSlotNull();
        if ((template.isUpToUp && slotNull == 0) || (!template.isUpToUp && slotNull < quantity)) {
            p.warningBagFull();
            return;
        }
        long giaXu = ((long) item.getCoin()) * ((long) quantity);
        long giaYen = ((long) item.getYen()) * ((long) quantity);
        long giaLuong = ((long) item.getGold()) * ((long) quantity);
        if (giaXu < 0 || giaYen < 0 || giaLuong < 0) {
            return;
        }
        if (giaXu > p.coin || giaLuong > p.user.gold || giaYen > p.yen) {
            p.serverDialog(p.language.getString("NOT_ENOUGH_MONEY"));
            return;
        }
        History history = new History(p.id, History.MUA_VAT_PHAM);
        history.setPrice((int) giaXu, (int) giaYen, (int) giaLuong);
        history.setBefore(p.coin, p.user.gold, p.yen);
        if (Event.isVietnameseWomensDay() || Event.isInternationalWomensDay()) {
            int point = (int) ((giaLuong / 10)); //(giaXu / 1000000) +
            if (point > 0) {
                p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, point);
                p.serverMessage(String.format("Bạn nhận được %s điểm tiêu xài.", NinjaUtils.getCurrency(point)));
            }
        }
        if (Event.isLunarNewYear()) {
            int point = (int) ((giaLuong / 10));
            if (point > 0) {
                Item pieceFirework = ItemFactory.getInstance().newItem(ItemName.MANH_PHAO_HOA);
                pieceFirework.setQuantity(point);
                p.addItemToBag(pieceFirework);
            }
        }
        p.addCoin(-giaXu);
        p.addGold((int) -giaLuong);
        p.addYen(-giaYen);


        history.setAfter(p.coin, p.user.gold, p.yen);
        int n = quantity;
        if (template.isUpToUp) {
            n = 1;
        }
        for (int i = 0; i < n; i++) {
            Item newItem  = Converter.getInstance().toItem(item);
            if (template.isUpToUp) {
                newItem.setQuantity(quantity);
            } else {
                newItem.setQuantity(1);
            }
            if (giaYen > 0 || giaLuong > 0) {
                newItem.isLock = true;
            }
            p.addItemToBag(newItem);
            history.addItem(newItem);
        }
        history.setTime(System.currentTimeMillis());
        History.insert(history);
    }
}
