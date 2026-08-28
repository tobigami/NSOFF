package com.nsoz.model;

import com.mongodb.DB;
import com.nsoz.constants.ItemName;
import com.nsoz.constants.NpcName;
import com.nsoz.constants.SQLStatement;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.server.Config;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.*;

public class Giveaways {
    private static final Giveaways instance = new Giveaways();

    public void give(Char player) {
        try (Connection conn = DbManager.getConnection();) {

            PreparedStatement stmt = conn.prepareStatement(
                    SQLStatement.GET_GIVEAWAYS, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, player.name);
            stmt.setString(2, player.name);
            ResultSet res = stmt.executeQuery();
            try {
                if (res.next()) {


                    int id = res.getInt("id");
                    int gold = res.getInt("gold");
                    int yen = res.getInt("yen");
                    int coin = res.getInt("coin");

                    JSONArray arrItem = (JSONArray) (new JSONParser().parse(res.getString("items")));


                    int size = arrItem.size();

                    if (size > player.getSlotNull()) {
                        player.getService().serverDialog("Bạn không đủ chỗ trống trong hành trang.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Chúc mừng, bạn đã được tặng").append("\n\n");

                    if (gold > 0) {
                        player.addGold(gold);
                        sb.append(String.format("- %s lượng", NinjaUtils.getCurrency(gold))).append("\n");
                    }

                    if (yen > 0) {
                        player.addYen(yen);
                        sb.append(String.format("- %s yên", NinjaUtils.getCurrency(yen))).append("\n");
                    }

                    if (coin > 0) {
                        player.addCoin(coin);
                        sb.append(String.format("- %s xu", NinjaUtils.getCurrency(coin))).append("\n");
                    }

                    for (int i = 0; i < size; i++) {
                        JSONObject itemObj = (JSONObject) arrItem.get(i);
                        Item newItem = new Item(itemObj);

                        if (newItem.options.isEmpty()) {
                            newItem.initOption();
                        }
                        // hạn code
                        if (newItem.template.id == ItemName.V_VIP) {
                            long expire = System.currentTimeMillis() + (long) (86400000 * 3);
                            newItem.expire = expire;
                        } else {
                            if (newItem.expire != -1) {
                                long expire = System.currentTimeMillis() + newItem.expire;
                                newItem.expire = expire;
                            }

                        }
                        player.addItemToBag(newItem);
                        sb.append(
                                        String.format("- x%s %s", NinjaUtils.getCurrency(newItem.getQuantity()), newItem.template.name))
                                .append("\n");
                    }
                    JSONArray used = (JSONArray) (new JSONParser().parse(res.getString("used")));
                    used.add(player.name);
                    player.getService().showAlert("Phần quà", sb.toString());
                    DbManager.executeUpdate(SQLStatement.UPDATE_USED, used.toJSONString(), id);
                    player.saveData();
                } else {
                    player.getService().npcChat(NpcName.ADMIN, "Bạn phải đạt huy hiệu Fancung trên Fanpage.");
                }
            } catch (Exception e){
                Log.logException("Lỗi kiểm tra sử dụng Giveaways(1): ", Giveaways.class, e);

            } finally {
                res.close();
                stmt.close();
            }
        } catch (Exception ex) {
            Log.logException("Lỗi kiểm tra sử dụng Giveaways(2): ", Giveaways.class, ex);

        }
    }

    // ==== Accessor thay cho Lombok, viết tay cho khớp lớp gốc ====
    // Trường `instance` là static nên Lombok sinh getter TĨNH; sinh nhầm thành getter thường thì
    // mọi chỗ gọi Giveaways.getInstance() sẽ không dịch được.

    public static Giveaways getInstance() {
        return instance;
    }

}
