/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.stall;

import com.nsoz.constants.CMD;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.model.Char;
import com.nsoz.model.History;
import com.nsoz.network.Message;
import com.nsoz.server.Config;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class Stall {

    private int id;
    private byte type;
    private String name;
    private final List<Item> productList;
    private final List<Item> expiredProductList;
    private final List<Item> listOfSoldProducts;
    private boolean saving;

    public Stall(int id, byte type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.productList = new ArrayList<>();
        this.expiredProductList = new ArrayList<>();
        this.listOfSoldProducts = new ArrayList<>();
    }

    public void add(Item item) {
        synchronized (productList) {
            productList.add(item);
        }
    }

    public void remove(Item item) {
        synchronized (productList) {
            productList.remove(item);
        }
    }

    public Item find(int id) {
        synchronized (productList) {
            for (Item item : productList) {
                if (item.getProductStatus() == StallManager.STATUS_ON_SALE && item.getProductID() == id) {
                    return item;
                }
            }
            return null;
        }
    }

    public int getTotalProduct() {
        synchronized (productList) {
            return productList.size();
        }
    }

    public int getTotalProductBySeller(String seller) {
        synchronized (productList) {
            int count = 0;
            for (Item item : productList) {
                if (item.getProductSeller().equals(seller)) {
                    count++;
                }
            }
            return count;
        }
    }

    public void buy(Char p, int id) {
        try {
            if (p.user.activated == 0) {
                p.serverDialog("NSO_KENY Chúc bạn chơi game vui vẻ: Zalo 0962.566.289");
                return;
            }
            Item item = find(id);
            if (item != null) {
                int price = item.getProductPrice();
                if (p.coin < price) {
                    p.serverDialog("Không đủ xu!");
                    return;
                }
                if (item.template.isBlackListItem()) {
                    p.serverDialog("Không thể mua vật phẩm này.");
                    return;
                }
                String seller = item.getProductSeller();
                item.setProductStatus(StallManager.STATUS_BOUGHT);
                DbManager.updateProduct(item);
                item.setProductChanged(false);
                p.addItemToBag(item);
                remove(item);
                addToSellList(item);
                p.getService().endDlg(true);
                price = (int) (price - (price * 0.1));

                History history = new History(this.id, History.SHINWA_MUA);
                history.setPrice(price, 0, 0);
                history.setBefore(p.coin, p.user.gold, p.yen);
                p.addCoin(-price);
                history.setAfter(p.coin, p.user.gold, p.yen);
                history.addItem(item);
                history.setTime(System.currentTimeMillis());
                History.insert(history);
                Char _char = Char.findCharByName(seller);
                String text = "Bạn nhân được " + NinjaUtils.getCurrency(price) + " xu";
                if (_char != null) {
                    History history2 = new History(_char.id, History.SHINWA_BAN_DUOC);
                    history2.setBefore(_char.coin, _char.user.gold, _char.yen);
                    _char.addCoin(price);
                    history2.setAfter(_char.coin, _char.user.gold, _char.yen);
                    history2.addItem(item);
                    history2.setTime(System.currentTimeMillis());
                    History.insert(history2);
                    _char.getService().showAlert("server", text);
                } else {
                    try {

                        ResultSet res = DbManager.executeQueryType("SELECT `players`.`id`,`players`.`xu`, `players`.`yen`, `players`.`luong` FROM `players` WHERE `players`.`name` = ? AND `players`.`server_id` = ?", seller, Config.getInstance().getServerID());
                        try {
                            if (res.next()) {
                                int charId = res.getInt("id");
                                DbManager.addCoin(charId, price);
                                DbManager.updateMessage(charId, text);

                                // ADD HISTORY
                                History history2 = new History(charId, History.SHINWA_BAN_DUOC);
                                long coin = res.getLong("xu");
                                long yen = res.getLong("yen");
                                int gold = res.getInt("luong");
                                history2.setBefore(coin, (int) gold, yen);
                                coin += price;
                                if (coin > 2000000000) {
                                    coin = 2000000000;
                                }
                                history2.setAfter(coin, (int) gold, yen);
                                history2.addItem(item);
                                history2.setTime(System.currentTimeMillis());
                                History.insert(history2);
                                // END ADD HISTORY

                            }
                        } finally {
                            res.close();
                        }

                    } catch (SQLException e) {
                        Log.logException("buy stall err (1)", Stall.class, e);
                        throw new RuntimeException(e);
                    } finally {

                    }

                }
            } else {
                p.serverDialog("Vật phẩm đã bán hoặc hết hạn");
            }
        } catch (Exception e) {
            Log.logException("buy stall err", Stall.class, e);
        }
    }

    public void save() {
        if (!saving) {
            saving = true;
            try {
                LinkedList<Item> list = new LinkedList<>();
                synchronized (productList) {
                    for (Item item : productList) {
                        if (item.isProductChanged()) {
                            list.add(item);
                        }
                    }
                }
                synchronized (expiredProductList) {
                    for (Item item : expiredProductList) {
                        if (item.isProductChanged()) {
                            list.add(item);
                        }
                    }
                }
                synchronized (listOfSoldProducts) {
                    for (Item item : listOfSoldProducts) {
                        if (item.isProductChanged()) {
                            list.add(item);
                        }
                    }
                }
                list.forEach((item) -> {
                    DbManager.updateProduct(item);
                    item.setProductChanged(false);
                });
            } finally {
                saving = false;
            }
        }

    }

    public void addToSellList(Item item) {
        synchronized (listOfSoldProducts) {
            listOfSoldProducts.add(item);
        }
    }

    public void update() {
        List<Item> expiredProductList = new ArrayList<>();
        synchronized (productList) {
            productList.forEach((t) -> {
                if (t.getProductTime() > 0) {
                    t.update();
                } else {
                    expiredProductList.add(t);
                }
            });
            this.productList.removeAll(expiredProductList);
        }
        synchronized (this.expiredProductList) {
            this.expiredProductList.addAll(expiredProductList);
        }
    }

    public void show(Char player) {
        try {
            player.setViewAuctionTab((byte) this.id);
            Message ms = new Message(CMD.LOAD_ITEM_AUCTION);
            DataOutputStream ds = ms.writer();
            ds.writeByte(this.id);
            int lent = productList.size();
            ds.writeInt(lent);
            for (Item item : productList) {
                ds.writeInt(item.getProductID());
                ds.writeInt(item.getProductTime());
                ds.writeShort(item.getQuantity());
                ds.writeUTF(item.getProductSeller());
                ds.writeInt(item.getProductPrice());
                ds.writeShort(item.getId());
            }
            ds.flush();
            player.getService().sendMessage(ms);
            ms.cleanup();
        } catch (IOException ex) {
            Log.logException("show stall err", Stall.class, ex);
        }
    }

    public List<Item> getProductListBySeller(String seller) {
        List<Item> list = new ArrayList<>();
        synchronized (productList) {
            productList.forEach((t) -> {
                if (t.getProductSeller().equals(seller)) {
                    list.add(t);
                }
            });
        }
        return list;
    }

    public List<Item> getExpiredProductListBySeller(String seller) {
        List<Item> list = new ArrayList<>();
        synchronized (expiredProductList) {
            expiredProductList.forEach((t) -> {
                if (t.getProductSeller().equals(seller)) {
                    list.add(t);
                }
            });
        }
        return list;
    }


    // ==== Accessor thay cho Lombok, viết tay cho khớp Char.class gốc ====

    public int getId() {
        return this.id;
    }

    public byte getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

}
