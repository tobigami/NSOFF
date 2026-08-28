package com.nsoz.event;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nsoz.constants.*;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.event.eventpoint.Point;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemManager;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.War;
import com.nsoz.map.zones.Zone;
import com.nsoz.model.Char;
import com.nsoz.option.ItemOption;
import com.nsoz.server.Config;
import com.nsoz.server.GlobalService;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class Event {

    public static final int NGAY_PHU_NU_VIET_NAM = 0;
    public static final int KOROKING = 1;
    public static final int TRUNG_THU = 2;
    public static final int HALLOWEEN = 3;
    public static final int NOEL = 4;
    public static final int HE = 7;
    public static final int LUNAR_NEW_YEAR = 5;
    public static final int WOMENS_DAY = 6;
    public static final int CO_HON = 7;


    public static final byte DOI_BANG_LUONG = 0;
    public static final byte DOI_BANG_XU = 1;

    public static final long EXPIRE_1_DAY = 86400000L;
    public static final long EXPIRE_3_DAY = 259200000L;
    public static final long EXPIRE_7_DAY = 604800000L;
    public static final long EXPIRE_30_DAY = 2592000000L;

    public static final RandomCollection<Integer> DOI_PHAN_TU = new RandomCollection<>();
    private static Event instance;

    static {
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_BUOM_BUOM_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_HOA_SEN_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_KEO_QUAN_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_MAT_TRANG_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_MAT_TROI_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_NGOI_SAO_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_TRAI_TIM_THOI_TRANG);
        DOI_PHAN_TU.add(1, ItemName.LONG_DEN_TRON_THOI_TRANG);
    }

    @Getter
    @Setter
    protected int id;
    protected List<EventPoint> eventPoints;
    @Getter
    protected RandomCollection<Integer> itemsThrownFromMonsters;
    protected RandomCollection<Integer> itemsOanhonThrownFromMonsters;
    @Getter
    protected RandomCollection<Integer> itemsRecFromCoinItem;
    @Getter
    protected RandomCollection<Integer> itemsRecFromGoldItem;
    protected RandomCollection<Integer> itemsMamCung;
    @Getter
    protected RandomCollection<Integer> itemsRecFromGold2Item;
    @Getter
    protected RandomCollection<Integer> vipItems;
    @Getter
    protected RandomCollection<Integer> itemRuongMayMan;
    protected Set<String> keyEventPoint;
    protected Calendar endTime = Calendar.getInstance();


    public Event() {
        itemsThrownFromMonsters = new RandomCollection<>();
        itemsOanhonThrownFromMonsters = new RandomCollection<>();
        itemsRecFromCoinItem = new RandomCollection<>();
        itemsRecFromGoldItem = new RandomCollection<>();
        itemsRecFromGold2Item = new RandomCollection<>();
        vipItems = new RandomCollection<>();
        itemRuongMayMan = new RandomCollection<>();
        itemsMamCung = new RandomCollection<>();
        eventPoints = new ArrayList<>();
        keyEventPoint = new TreeSet<>();
    }

    public static void init() {
        if (Config.getInstance().getEvent() != null) {
            try {
                instance = (Event) Class.forName(Config.getInstance().getEvent()).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                 Log.logException("Init event err", Event.class, ex);
            }
        }
    }

    public static Event getEvent() {
        return instance;
    }

    public static boolean isEvent() {
        return (instance != null && !instance.isEnded());
    }

    public static boolean isTrungThu() {
        return isEvent() && instance instanceof TrungThuNew;
    }

    public static boolean isCoHon() {
        return isEvent() && instance instanceof CoHon;
    }

    public static boolean isKoroKing() {
        return isEvent() && instance instanceof KoroKing;
    }

    public static boolean isVietnameseWomensDay() {
        return isEvent() && instance instanceof VietnameseWomensDay;
    }

    public static boolean isInternationalWomensDay() {
        return isEvent() && instance instanceof InternationalWomensDay;
    }

    public static boolean isHalloween() {
        return isEvent() && instance instanceof Halloween;
    }

    public static boolean isNoel() {
        return isEvent() && instance instanceof Noel;
    }

    public static boolean isLunarNewYear() {
        return isEvent() && instance instanceof LunarNewYear;
    }

    public static LunarNewYear getLunarNewYear() {
        if (instance instanceof LunarNewYear) {
            return (LunarNewYear) instance;
        }
        return null;
    }

    //  item sự kiện trong class random
    public static void useVipEventItem(Char _char, int type, RandomCollection<Integer> rc) {
        int itemId = rc.next();
        Item itm = ItemFactory.getInstance().newItem(itemId);
        itm.isLock = false;
        itm.expire = System.currentTimeMillis();

        long month = NinjaUtils.nextInt(1, type == 2 ? 3 : 2);
        long expire = month * ConstTime.MONTH / 3;
        itm.expire += expire;

        if (type == 2 && NinjaUtils.nextInt(1, 35) == 1) {
            itm.expire += expire;
        }

        if (itm.id == ItemName.MAT_NA_HO) {
            itm.randomOptionTigerMask();
        }

        _char.addItemToBag(itm);
    }

    // chức năng đổi lồng đèn
    public void doiLongDen(Char p, byte type, int index) {
        List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP,
                ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
        if (index < 0 || index >= list.size()) {
            return;
        }
        if (!isEvent()) {
            p.getService().npcChat(NpcName.TIEN_NU, "Sự kiện đã kết thúc!");
            return;
        }
        if (type == DOI_BANG_LUONG) {
            if (p.user.gold < 500) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ 500 lượng!");
                return;
            }
            p.addGold(-500);
        }
        if (type == DOI_BANG_XU) {
            if (p.coin < 2000000) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ 2 triệu xu!");
                return;
            }
            p.addCoin(-2000000);
        }
        Item item = list.get(index);
        p.removeItem(item.index, 1, true);
        int itemID = DOI_PHAN_TU.next();
        Item itm = ItemFactory.getInstance().newItem(itemID);
        for (ItemOption o : item.options) {
            itm.options.add(o);
        }

        if (NinjaUtils.nextInt(200) == 0) {
            itm.options.add(new ItemOption(ItemOptionName.MIEN_GIAM_SAT_THUONG_POINT_PERCENT_TYPE_8,
                    NinjaUtils.nextInt(1, 30)));
        }

        if (type == DOI_BANG_LUONG) {
            itm.randomOptionLongDen();
        }

        itm.expire = System.currentTimeMillis() + EXPIRE_30_DAY;
        p.addItemToBag(itm);
        p.getService().endDlg(true);
    }

    public abstract void initStore();

    public int randomItemID() {
        return itemsThrownFromMonsters.next();
    }

    public int randomItemOanHonID() {
        return itemsOanhonThrownFromMonsters.next();
    }

    public abstract void action(Char p, int type, int amount);

    public abstract void menu(Char p);

    public void useItem(Char _char, Item item) {
    }

    public EventPoint createEventPoint() {
        EventPoint eventPoint = new EventPoint();
        keyEventPoint.forEach((key) -> {
            eventPoint.add(new Point(key, 0, 0, 0, 0));
        });
        return eventPoint;
    }

    public void loadEventPoint() {
        try (Connection conn = DbManager.getConnection()) {
            eventPoints.clear();
            PreparedStatement ps = conn
                    .prepareStatement(SQLStatement.LOAD_EVENT_POINT);
            ps.setInt(1, this.id);
            ps.setInt(2, Config.getInstance().getServerID());
            ResultSet rs = ps.executeQuery();
            Gson g = new Gson();
            while (rs.next()) {
                EventPoint eventPoint = createEventPoint();
                int id = rs.getInt("id");
                int playerID = rs.getInt("player_id");
                String name = rs.getString("name");
                ArrayList<Point> points = g.fromJson(rs.getString("point"), new TypeToken<ArrayList<Point>>() {
                }.getType());
                eventPoint.setId(id);
                eventPoint.setPlayerID(playerID);
                eventPoint.setPlayerName(name);
                eventPoint.setPoints(points);
                eventPoint.addIfMissing(keyEventPoint);
                eventPoints.add(eventPoint);
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            Log.logException("Lỗi load event point:", Event.class,ex);

        }
    }

    public void addEventPoint(EventPoint eventPoint) {
        synchronized (eventPoints) {
            eventPoints.add(eventPoint);
        }
    }

    public void removeEventPoint(EventPoint eventPoint) {
        synchronized (eventPoints) {
            eventPoints.remove(eventPoint);
        }
    }

    public EventPoint findEventPointByPlayerID(int playerID) {
        synchronized (eventPoints) {
            for (EventPoint ev : eventPoints) {
                if (ev.getPlayerID() == playerID) {
                    return ev;
                }
            }
            return null;
        }
    }

    public abstract void initMap(Zone zone);

    // sử dụng 1 item
    public boolean useEventItem(Char p, int itemId, RandomCollection<Integer> rc) {
        int[][] itemRequires = new int[][]{{itemId, 1}};
        return useEventItem(p, 1, itemRequires, 0, 0, 0, rc);
    }

    public boolean useEventItem(Char p, int number, int[][] itemRequire, int gold, int coin, int yen,
                                RandomCollection<Integer> rc) {
        return makeEventItem(p, number, itemRequire, gold, coin, yen, rc, -1);
    }

    public boolean useEventItem(Char p, int number, int gold, int coin, RandomCollection<Integer> rc) {
        return makeEventItem(p, number, new int[][]{}, gold, coin, 0, rc, -1);
    }

    public boolean makeEventItem(Char p, int number, int[][] itemRequire, int gold, int coin, int yen,
                                 int itemIdReceive) {
        return makeEventItem(p, number, itemRequire, gold, coin, yen, null, itemIdReceive);
    }

    // menu nhập số lượng tiên nữ
    public boolean makeEventItem(Char p, int number, int[][] itemRequire, int gold, int coin, int yen,
                                 RandomCollection<Integer> rc, int itemIdReceive) {
        if (number < 1) {
            p.getService().npcChat(NpcName.TIEN_NU, "Số lượng tối thiểu là 1.");
            return false;
        }

        if (number > 1000) {
            p.getService().npcChat(NpcName.TIEN_NU, "Số lượng tối đa là 1.000.");
            return false;
        }

        int priceGold = number * gold;
        int priceCoin = number * coin;
        int priceYen = number * yen;

        for (int i = 0; i < itemRequire.length; i++) {
            int itemId = itemRequire[i][0];
            int amount = itemRequire[i][1] * number;
            int index = p.getIndexItemByIdInBag(itemId);
            if (index == -1 || p.bag[index] == null || !p.bag[index].has(amount)) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ " + ItemManager.getInstance().getItemName(itemId));
                return false;
            }
        }
        if (p.yen < priceYen) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ yên");
            return false;
        } else if (p.user.gold < priceGold) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ lượng");
            return false;
        } else if (p.coin < priceCoin) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ xu");
            return false;
        } else if (rc != null && p.getSlotNull() < number) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return false;
        } else if (itemIdReceive != -1 && p.getSlotNull() < 1) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return false;
        }

        if (priceYen > 0) {
            p.addYen(-priceYen);
        }
        if (priceGold > 0) {
            p.addGold(-priceGold);
        }

        if (priceCoin > 0) {
            p.addCoin(-priceCoin);
        }
        String itemName = "";
        Item itemRequite = null;
        for (int i = 0; i < itemRequire.length; i++) {
            int itemId = itemRequire[i][0];
            Item itm = ItemFactory.getInstance().newItem(itemId);
            // gọi tên vật phẩm sự kiện
//            if (itm.id == ItemName.DIEU_GIAY || itm.id == ItemName.DIEU_VAI || itm.id == ItemName.HU_KEM_DAM || itm.id == ItemName.CAN_CAU_CA) {
            itemName = itm.template.name;
//            }
            itemRequite = itm;
            int amount = itemRequire[i][1] * number;
            int index = p.getIndexItemByIdInBag(itemId);
            p.removeItem(index, amount, true);
        }


        // todo : exp sự kiện và quà lên ktg
        if (rc != null) {
            for (int i = 0; i < number; i++) {
                int exp = 0;
                int yen_collect = 0;
                if (itemRequite != null) {
                    if (itemRequite.id == ItemName.BANH_MI_COHON) {
                        exp = 5000000;
                        yen_collect = NinjaUtils.nextInt(1, 51) * 1000;
                    } else if (itemRequite.id == ItemName.BANH_MI_BO_COHON) {
                        exp = 15000000;
                        yen_collect = NinjaUtils.nextInt(50, 101) * 1000;
                    } else {
                        exp = NinjaUtils.nextInt(10000000, 15000000);
                    }
                }
                p.addExp(exp);
                if (yen_collect > 0) {
                    p.addYen(yen_collect);
                }
                int itemId = rc.next();
                Item itm = ItemFactory.getInstance().newItem(itemId);
                itm.initExpire();

                // todo : chỉnh lại chỉ số item khi mở ở sự kiện
                if (itm.id == ItemName.THONG_LINH_THAO) {
                    itm.setQuantity(NinjaUtils.nextInt(5, 10));


                    // todo : nếu nick chưa kích hoạt sẽ thay item dưới thành exp
                } else if (p.user.activated == 0 && (itm.id == ItemName.RUONG_HUYEN_BI || itm.id == ItemName.RUONG_BACH_NGAN
                        || itm.id == ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_SO || itm.id == ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_TRUNG || itm.id == ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_CAO)) {
                    p.addExp(exp);
                    // todo : ăn sự kiện hiện kênh thế giới
                } else if (p.user.activated == 1 && (itm.id == ItemName.RUONG_HUYEN_BI || itm.id == ItemName.RUONG_BACH_NGAN || itm.id == ItemName.BAT_BAO)) {
                    GlobalService.getInstance().chat("server", "Chúc mừng " + p.name + " mở " + itemName + " nhận được " + itm.template.name);
                    p.addItemToBag(itm);
                } else {
                    if (p.user.activated == 1) {
                        if (itemId == ItemName.LAN_SU_VU ) {
                            if (NinjaUtils.nextInt(0, 10000) < 10) {  // todo : tỉ lệ mở ,ví dụ nó random 0 tới 10000, thì vào số từ 1 tới 10 thì sẽ nhận vv, vậy mình giảm số 10 đi - tỉ lệ sẽ khó hơn
                                itm.expire = -1;
                                GlobalService.getInstance().chat("server", "Chúc mừng " + p.name + " mở " + itemName + " nhận được " + itm.template.name + " vĩnh viễn");
                            }
                        }
                        if (itemId == ItemName.GAY_MAT_TRANG || itemId == ItemName.GAY_TRAI_TIM) {
                            if (NinjaUtils.nextInt(0, 10000) < 10) {
                                itm.expire = -1;
                                GlobalService.getInstance().chat("server", "Chúc mừng " + p.name + " mở " + itemName + " nhận được " + itm.template.name + " vĩnh viễn");
                            }
                        }
                        if (itemId == ItemName.BACH_HO || itemId == ItemName.PHUONG_HOANG_BANG || itemId == ItemName.HOA_KY_LAN ) {
                            if (NinjaUtils.nextInt(0, 10000) < 3) {
                                itm.expire = -1;
                                GlobalService.getInstance().chat("server", "Chúc mừng " + p.name + " mở " + itemName + " nhận được " + itm.template.name + " vĩnh viễn");
                            }
                        }

                        if (itemId == ItemName.BACH_NGAN_LANG || itemId == ItemName.BACH_SU_VUONG ) { // thêm pet mới
                            if (NinjaUtils.nextInt(0, 10000) < 3) {
                                itm.expire = -1;
                                GlobalService.getInstance().chat("server", "Chúc mừng " + p.name + " mở " + itemName + " nhận được " + itm.template.name + " vĩnh viễn");
                            }
                        }

                        if (itemId == ItemName.MAT_NA_SHIN_AH || itemId == ItemName.MAT_NA_VO_DIEN || itemId == ItemName.MAT_NA_ONI || itemId == ItemName.MAT_NA_KUMA || itemId == ItemName.MAT_NA_INU) {
                            if (NinjaUtils.nextInt(0, 10000) < 5) {
                                itm.expire = -1;
                                GlobalService.getInstance().chat("server", "Chúc mừng " + p.name + " mở " + itemName + " nhận được " + itm.template.name + " vĩnh viễn");
                            }
                        }

                    }
                    if (itemId == ItemName.HUYET_SAC_HUNG_LANG || itemId == ItemName.CHIM_TINH_ANH) {
                        itm.expire = System.currentTimeMillis() + EXPIRE_7_DAY;
                    }

                    p.addItemToBag(itm);
                }
            }

        } else if (itemIdReceive != -1) {
            Item itm = ItemFactory.getInstance().newItem(itemIdReceive);
            itm.setQuantity(number);
//            itm.setLock(true); // fix làm item sk khóa
            p.addItemToBag(itm);
            if (priceGold > 0) {
                p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, 1);
            }
        }
        return true;
    }

    public void viewTop(Char p, String key, String title, String format) {
        List<EventPoint> list = eventPoints.stream().sorted((o1, o2) -> {
            int p1 = o1.getPoint(key);
            int p2 = o2.getPoint(key);
            return p2 - p1;
        }).limit(10).filter(t -> t.getPoint(key) > 0).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        int rank = 1;

        for (EventPoint t : list) {
            sb.append(String.format(format, rank++, t.getPlayerName(), NinjaUtils.getCurrency(t.getPoint(key))))
                    .append("\n");

        }
        sb.append("==============");
        sb.append(String.format("\nĐiểm cá nhân %d", getPoint(p, key)));
        p.getService().showAlert(title, sb.toString());
    }

    public String getName(String name) {
        int length = name.length();
        int middle = length / 2;

        String middleTwoChars;

        if (length % 2 == 0) {
            middleTwoChars = name.substring(middle - 1, middle + 1);
        } else {

            middleTwoChars = name.substring(middle, middle + 2);
        }

        return "xx_" + middleTwoChars + "_xx";
    }
//    public int getPoint(Char p, String key) {
//        List<EventPoint> list = eventPoints.stream().sorted((o1, o2) -> {
//            int p1 = o1.getPoint(key);
//            int p2 = o2.getPoint(key);
//            return p2 - p1;
//        }).limit(10).filter(t -> t.getPoint(key) > 0).collect(Collectors.toList());
//        StringBuilder sb = new StringBuilder();
//
//        for (EventPoint t : list) {
//            if (t.getPlayerName().equals(p.name)) {
//                return t.getPoint(key);
//            }
//
//        }
//        return 0;
//    }


    public int getPoint(Char p, String key) {
        for (EventPoint t : eventPoints) {
            if (t.getPlayerName().equals(p.name)) {
                return t.getPoint(key);
            }
        }
        return 0;
    }

//    public int getPontAll(String key) {
//        List<EventPoint> list = eventPoints.stream().sorted((o1, o2) -> {
//            int p1 = o1.getPoint(key);
//            int p2 = o2.getPoint(key);
//            return p2 - p1;
//        }).limit(10).filter(t -> t.getPoint(key) > 0).collect(Collectors.toList());
//        StringBuilder sb = new StringBuilder();
//
//        for (EventPoint t : list) {
//
//            return t.getPoint(key);
//
//
//        }
//        return 0;
//    }

    public int getPontAll(String key) {
        int point = 0;
        for (EventPoint t : eventPoints) {
            point += t.getPoint(key);
        }
        return point;
    }

    public int getRanking(Char p, String key) {
        List<EventPoint> list = eventPoints.stream().sorted((o1, o2) -> {
            int p1 = o1.getPoint(key);
            int p2 = o2.getPoint(key);
            return p2 - p1;
        }).limit(10).filter(t -> t.getPoint(key) > 0).collect(Collectors.toList());
        int rank = 0;
        for (EventPoint t : list) {
            rank++;
            if (t.getPlayerName().equals(p.name)) {
                return rank;
            }
        }
        return 99;
    }

    public boolean isEnded() {
        return endTime.getTime().getTime() - System.currentTimeMillis() <= 0;
    }



}
