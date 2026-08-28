package com.nsoz.event;

import com.nsoz.constants.*;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.zones.Zone;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.store.StoreManager;
import com.nsoz.util.NinjaUtils;
import com.nsoz.store.ItemStore;
import com.nsoz.model.Menu;
import com.nsoz.option.ItemOption;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.nsoz.event.Halloween.TOP_KEO_TAO;

public class CoHon extends Event {
    public static final String TOP_MAM_CO_HON = "top_banh_mi";
    public static final String TOP_HO_LO = "top_banh_mi_bo";
    private static final int LAM_BANH_MI = 4;
    private static final int LAM_BANH_MI_BO = 5;
    private RandomCollection<Integer> vipItems = new RandomCollection<>();
    private ZonedDateTime start, end;
    public static final RandomCollection<Integer> ITEM_BANH_MI = new RandomCollection<>();
    public static final RandomCollection<Integer> ITEM_BANH_MI_BO = new RandomCollection<>();

    public CoHon() {
        setId(Event.CO_HON);
        endTime.set(2027, Calendar.SEPTEMBER, 31, 23, 59, 59); // TODO: time chạy sự kiện, cứ để nó dài ra, mình tắt sk ở file config
        keyEventPoint.add(TOP_MAM_CO_HON);
        keyEventPoint.add(TOP_HO_LO);

        // TODO: rơi nlsk khi đánh quái, thêm càng nhiều dòng nó ra càng nhiều
        itemsThrownFromMonsters.add(100, ItemName.LUA_MI_COHON);
        itemsThrownFromMonsters.add(100, ItemName.TRUNG_COHON);
        itemsThrownFromMonsters.add(100, ItemName.SUA_COHON);
        itemsThrownFromMonsters.add(100, ItemName.BO_COHON);
        initRandomItem();
    }

    // TODO: tỉ lệ ra item
    public void initRandomItem() {

        // todo: item xu

        itemsRecFromCoinItem.add(40, ItemName.DA_CAP_8);
        itemsRecFromCoinItem.add(30, ItemName.DA_CAP_9);
        itemsRecFromCoinItem.add(10, ItemName.DA_CAP_10);
        itemsRecFromCoinItem.add(10, ItemName.BAO_HIEM_TRUNG_CAP);
        itemsRecFromCoinItem.add(20, ItemName.HOAN_LUONG_CHI_THAO);
        itemsRecFromCoinItem.add(40, ItemName.GA_TAY);
        itemsRecFromCoinItem.add(40, ItemName.TOM_HUM);
        itemsRecFromCoinItem.add(30, ItemName.HAGGIS);
        itemsRecFromCoinItem.add(1, ItemName.LAN_SU_VU);
        itemsRecFromCoinItem.add(40, ItemName.DA_DANH_VONG_CAP_1);
        itemsRecFromCoinItem.add(30, ItemName.DA_DANH_VONG_CAP_2);
        itemsRecFromCoinItem.add(30, ItemName.DA_DANH_VONG_CAP_3);
        itemsRecFromCoinItem.add(20, ItemName.DA_DANH_VONG_CAP_4);
        itemsRecFromCoinItem.add(30, ItemName.MANH_NON_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_GANG_TAY_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_AO_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_QUAN_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_GIAY_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_DAY_CHUYEN_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_NGOC_BOI_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_PHU_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_NHAN_JIRAI_);
        itemsRecFromCoinItem.add(30, ItemName.MANH_NON_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_GANG_TAY_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_AO_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_QUAN_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_GIAY_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_DAY_CHUYEN_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_NGOC_BOI_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_PHU_JUMITO);
        itemsRecFromCoinItem.add(30, ItemName.MANH_NHAN_JUMITO);
        itemsRecFromCoinItem.add(50, ItemName.MINH_MAN_DAN);
        itemsRecFromCoinItem.add(50, ItemName.LONG_LUC_DAN);
        itemsRecFromCoinItem.add(50, ItemName.KHANG_THE_DAN);
        itemsRecFromCoinItem.add(50, ItemName.SINH_MENH_DAN);
        itemsRecFromCoinItem.add(15, ItemName.XICH_NHAN_NGAN_LANG);
        itemsRecFromCoinItem.add(6, ItemName.XE_MAY);
        itemsRecFromCoinItem.add(20, ItemName.HUYEN_TINH_NGOC);
        itemsRecFromCoinItem.add(20, ItemName.HUYET_NGOC);
        itemsRecFromCoinItem.add(20, ItemName.LAM_TINH_NGOC);
        itemsRecFromCoinItem.add(20, ItemName.LUC_NGOC);
        itemsRecFromCoinItem.add(15, ItemName.LONG_DEN_TRON);
        itemsRecFromCoinItem.add(10, ItemName.LONG_DEN_CA_CHEP);
        itemsRecFromCoinItem.add(50, ItemName.LONG_DEN_NGOI_SAO);
        itemsRecFromCoinItem.add(50, ItemName.LONG_DEN_MAT_TRANG);
        itemsRecFromCoinItem.add(0.7, ItemName.BAT_BAO);
        itemsRecFromCoinItem.add(0.3, ItemName.RUONG_BACH_NGAN);
        itemsRecFromCoinItem.add(1, ItemName.MAT_NA_SHIN_AH);
        itemsRecFromCoinItem.add(1, ItemName.MAT_NA_VO_DIEN);
        itemsRecFromCoinItem.add(1, ItemName.MAT_NA_ONI);
        itemsRecFromCoinItem.add(1, ItemName.MAT_NA_KUMA);
        itemsRecFromCoinItem.add(1, ItemName.MAT_NA_INU);
        itemsRecFromCoinItem.add(5, ItemName.HARLEY_DAVIDSON);
        itemsRecFromCoinItem.add(6, ItemName.XE_MAY);
        itemsRecFromCoinItem.add(50, ItemName.BANH_RANG);
        itemsRecFromCoinItem.add(3, ItemName.XICH_TU_MA);
        itemsRecFromCoinItem.add(3, ItemName.TA_LINH_MA);
        itemsRecFromCoinItem.add(3, ItemName.PHONG_THUONG_MA);
        itemsRecFromCoinItem.add(60, ItemName.LONG_KHI);
        itemsRecFromCoinItem.add(20, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_SO);
        itemsRecFromCoinItem.add(10, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_TRUNG);

        // todo: item lượng


        itemsRecFromGoldItem.add(30, ItemName.DA_CAP_9);
        itemsRecFromGoldItem.add(10, ItemName.DA_CAP_10);
        itemsRecFromGoldItem.add(20, ItemName.HOAN_LUONG_CHI_THAO);
        itemsRecFromGoldItem.add(50, ItemName.MINH_MAN_DAN);
        itemsRecFromGoldItem.add(50, ItemName.LONG_LUC_DAN);
        itemsRecFromGoldItem.add(50, ItemName.KHANG_THE_DAN);
        itemsRecFromGoldItem.add(50, ItemName.SINH_MENH_DAN);
        itemsRecFromGoldItem.add(30, ItemName.GA_TAY);
        itemsRecFromGoldItem.add(20, ItemName.TOM_HUM);
        itemsRecFromGoldItem.add(20, ItemName.HAGGIS);
        itemsRecFromGoldItem.add(20, ItemName.DA_DANH_VONG_CAP_3);
        itemsRecFromGoldItem.add(20, ItemName.DA_DANH_VONG_CAP_4);
        itemsRecFromGoldItem.add(10, ItemName.DA_DANH_VONG_CAP_5);
        itemsRecFromGoldItem.add(20, ItemName.VIEN_LINH_HON_CAP_3);
        itemsRecFromGoldItem.add(20, ItemName.VIEN_LINH_HON_CAP_4);
        itemsRecFromGoldItem.add(10, ItemName.VIEN_LINH_HON_CAP_5);
        itemsRecFromGoldItem.add(30, ItemName.BUI_LINH_HON);
        itemsRecFromGoldItem.add(10, ItemName.LONG_DEN_TRON);
        itemsRecFromGoldItem.add(15, ItemName.LONG_DEN_CA_CHEP);
        itemsRecFromGoldItem.add(50, ItemName.LONG_DEN_NGOI_SAO);
        itemsRecFromGoldItem.add(50, ItemName.LONG_DEN_MAT_TRANG);
        itemsRecFromGoldItem.add(35, ItemName.HUYEN_TINH_NGOC);
        itemsRecFromGoldItem.add(35, ItemName.HUYET_NGOC);
        itemsRecFromGoldItem.add(35, ItemName.LAM_TINH_NGOC);
        itemsRecFromGoldItem.add(35, ItemName.LUC_NGOC);
        itemsRecFromGoldItem.add(30, ItemName.HOA_TUYET);
        itemsRecFromGoldItem.add(30, ItemName.PHA_LE);
        itemsRecFromGoldItem.add(30, ItemName.NHAM_THACH_);
        itemsRecFromGoldItem.add(0.08, ItemName.BAT_BAO);
        itemsRecFromGoldItem.add(0.05, ItemName.RUONG_BACH_NGAN);
        itemsRecFromGoldItem.add(0.01, ItemName.RUONG_HUYEN_BI);
        itemsRecFromGoldItem.add(3, ItemName.PET_YEU_TINH);
        itemsRecFromGoldItem.add(1, ItemName.GAY_MAT_TRANG);
        itemsRecFromGoldItem.add(0.08, ItemName.GAY_TRAI_TIM);
        itemsRecFromGoldItem.add(3, ItemName.XICH_TU_MA);
        itemsRecFromGoldItem.add(3, ItemName.TA_LINH_MA);
        itemsRecFromGoldItem.add(3, ItemName.PHONG_THUONG_MA);
        itemsRecFromGoldItem.add(3, ItemName.BACH_HO);
        itemsRecFromGoldItem.add(1, ItemName.PHUONG_HOANG_BANG);
        itemsRecFromGoldItem.add(2, ItemName.HOA_KY_LAN);
        itemsRecFromGoldItem.add(5, ItemName.LAN_SU_VU);
        itemsRecFromGoldItem.add(20, ItemName.XICH_NHAN_NGAN_LANG);
        itemsRecFromGoldItem.add(10, ItemName.XE_MAY);
        itemsRecFromGoldItem.add(1, ItemName.MAT_NA_SHIN_AH);
        itemsRecFromGoldItem.add(1, ItemName.MAT_NA_VO_DIEN);
        itemsRecFromGoldItem.add(1, ItemName.MAT_NA_ONI);
        itemsRecFromGoldItem.add(1, ItemName.MAT_NA_KUMA);
        itemsRecFromGoldItem.add(1, ItemName.MAT_NA_INU);
        itemsRecFromGoldItem.add(1, ItemName.MAT_NA_INU);
        itemsRecFromGoldItem.add(30, ItemName.MAT_NA_SUPER_BROLY);
        itemsRecFromGoldItem.add(30, ItemName.MAT_NA_ONNA_BUGEISHA);
        itemsRecFromGoldItem.add(30, ItemName.DIA_LANG_THAO);
        itemsRecFromGoldItem.add(30, ItemName.TAM_LUC_DIEP);
        itemsRecFromGoldItem.add(40, ItemName.DA_PHUC_SINH);
        itemsRecFromGoldItem.add(40, ItemName.DA_VO_THUONG);
        itemsRecFromGoldItem.add(40, ItemName.DA_CHINH_PHUC);
        itemsRecFromGoldItem.add(10, ItemName.RUONG_DANH_VONG);
        
        itemsRecFromGoldItem.add(0.5, ItemName.TUAN_LOC);
        itemsRecFromGoldItem.add(0.2, ItemName.HAKAIRO_YOROI);
        itemsRecFromGoldItem.add(0.01, ItemName.LENH_BAI_HOAN_THANH);
        itemsRecFromGoldItem.add(60, ItemName.LONG_KHI);
        itemsRecFromGoldItem.add(20, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_SO);
        itemsRecFromGoldItem.add(10, ItemName.THE_BAI_KINH_NGHIEM_GIA_TOC_TRUNG);
        itemsRecFromGoldItem.add(1, ItemName.LINH_CHI_NGAN_NAM);
        itemsRecFromGoldItem.add(1, ItemName.LINH_CHI_VAN_NAM);


    }

    // TODO: cửa hàng goosho
    @Override
    public void initStore() {
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(996)
                .itemID(ItemName.MAM_CUNG_CO_HON_BAC)
                .coin(20000)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(996)
                .itemID(ItemName.MAM_CUNG_CO_HON_VANG)
                .gold(25)
                .expire(ConstTime.FOREVER)
                .build());

        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(996)
                .itemID(ItemName.MAY_DO_OAN_HON)
                .gold(100)
                .expire(ConstTime.FOREVER)
                .build());
    }

    // todo: sử dụng item sự kiện
    @Override
    public void useItem(Char p, Item item) {
        switch (item.id) {
            case ItemName.BANH_MI_COHON: {
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromCoinItem);
                p.getEventPoint().addPoint(TOP_MAM_CO_HON, 1);
            }
            return;
            case ItemName.BANH_MI_BO_COHON: {
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem);
                p.getEventPoint().addPoint(TOP_HO_LO, 1);
            }
            return;
            case ItemName.MAM_CUNG_CO_HON:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsMamCung);

                return;
            case ItemName.MAM_CUNG_CO_HON_BAC:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem);
                p.getEventPoint().addPoint(CoHon.TOP_HO_LO, 1);
                return;
            case ItemName.MAM_CUNG_CO_HON_VANG:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem);
                p.getEventPoint().addPoint(CoHon.TOP_MAM_CO_HON, 1);
                return;

            case ItemName.HOP_QUA_MAY_MAN: {
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemRuongMayMan);
            }
        }
    }

    // TODO: menu npc tiên nữ
    @Override
    public void menu(Char p) {
        p.menus.clear();

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm Bánh Mì", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Làm Bánh Mì", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, LAM_BANH_MI, number);
                } catch (NumberFormatException e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm Bánh Mì Bơ", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Làm Bánh Mì Bơ", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, LAM_BANH_MI_BO, number);
                } catch (NumberFormatException e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Pet Bóng Ma = 10 Bánh mì + 100 Lượng", () -> {
            int indexBanhMi = p.getIndexItemByIdInBag(ItemName.BANH_MI_COHON);
            if (indexBanhMi == -1 || p.bag[indexBanhMi] == null
                    || p.bag[indexBanhMi].getQuantity() < 10) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ 10 Bánh mì");
                return;
            }
            if (p.user.gold < 100) {
                p.getService().npcChat(NpcName.TIEN_NU, "Bạn phải có đủ 100 lượng.");
                return;
            }
            p.removeItem(indexBanhMi, 10, true);
            p.addGold(-100);
            //
            Item newItem = ItemFactory.getInstance().newItem(ItemName.PET_BONG_MA);
            newItem.expire = System.currentTimeMillis() + 60_000L * 60 * 24 * 7;
            p.addItemToBag(newItem);
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Pet Yêu Tinh = 10 Bánh mì bơ + 300 Lượng", () -> {
            int indexBanhMiBo = p.getIndexItemByIdInBag(ItemName.BANH_MI_BO_COHON);
            if (indexBanhMiBo == -1 || p.bag[indexBanhMiBo] == null
                    || p.bag[indexBanhMiBo].getQuantity() < 10) {
                p.getService().npcChat(NpcName.TIEN_NU, "Không đủ 10 Bánh mì bơ");
                return;
            }
            if (p.user.gold < 300) {
                p.getService().npcChat(NpcName.TIEN_NU, "Bạn phải có đủ 300 lượng.");
                return;
            }
            p.removeItem(indexBanhMiBo, 10, true);
            p.addGold(-300);
            Item newItem = ItemFactory.getInstance().newItem(ItemName.PET_YEU_TINH);
            newItem.expire = System.currentTimeMillis() + 60_000L * 60 * 24 * 7;
            p.addItemToBag(newItem);
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Top sự kiện", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Top Bánh Mì", () -> {
                if (p.user.isTien()) {
                    viewTop(p, TOP_MAM_CO_HON, "Top Bánh Mì", "%d. %s đã mở %s Bánh Mì");
                } else {
                    viewTop(p, TOP_MAM_CO_HON, "Top Bánh Mì", "%d. %s ");
                }
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Top Bánh Mì Bơ", () -> {
                if (p.user.isTien()) {
                    viewTop(p, TOP_HO_LO, "Top Bánh Mì Bơ", "%d. %s đã mở %s Bánh Mì Bơ");
                } else {
                    viewTop(p, TOP_HO_LO, "Top Bánh Mì Bơ", "%d. %s ");
                }
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Top Săn Boss", () -> {
                p.showRankedList(6);
            }));
            p.getService().openUIMenu();
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Hướng dẫn", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("-- HƯỚNG DẪN --");
            p.getService().showAlert("Hướng Dẫn", sb.toString());
        }));
    }

    // TODO: các menu phụ
    @Override
    public void action(Char p, int type, int amount) {
        if (isEnded()) {
            p.serverMessage("Sự kiện đã kết thúc!");
            return;
        }
        switch (type) {
            case LAM_BANH_MI:
                lamBanhMi(p, amount);
                break;
            case LAM_BANH_MI_BO:
                lamBanhMiBo(p, amount);
                break;
        }
    }

    private void lamBanhMi(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.LUA_MI_COHON, 3}, {ItemName.TRUNG_COHON, 3}, {ItemName.SUA_COHON, 3}};
        int itemIdReceive = ItemName.BANH_MI_COHON;
        makeEventItem(p, amount, itemRequires, 0, 100_000, 0, itemIdReceive);
    }

    private void lamBanhMiBo(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.LUA_MI_COHON, 3}, {ItemName.TRUNG_COHON, 3}, {ItemName.SUA_COHON, 3}, {ItemName.BO_COHON, 1}};
        int itemIdReceive = ItemName.BANH_MI_BO_COHON;
        makeEventItem(p, amount, itemRequires, 20, 0, 0, itemIdReceive);
    }

    // TODO: nhận quà tự động khi hết sự kiện
    public void receiveReward(Char p, String key) {
        int ranking = getRanking(p, key);
        if (ranking > 10) {
            p.getService().serverDialog("Bạn không đủ điều kiện nhận phần thưởng");
            return;
        }
        if (p.getEventPoint().getRewarded(key) == 1) {
            p.getService().serverDialog("Bạn đã nhận phần thưởng rồi");
            return;
        }
        if (p.getSlotNull() < 10) {
            p.getService().serverDialog("Bạn cần để hành trang trống tối thiểu 10 ô");
            return;
        }

        if (key == TOP_MAM_CO_HON) {
            topCoHon(ranking, p);
        } else if (key == TOP_HO_LO) {
            topHoLo(ranking, p);
        }
        p.getEventPoint().setRewarded(key, 1);
    }

    public void topCoHon(int ranking, Char p) {
        int tickId = p.gender == 1 ? ItemName.GAY_MAT_TRANG : ItemName.GAY_TRAI_TIM;
        Item fashionStick = ItemFactory.getInstance().newItem(tickId);
        Item petthanchet = ItemFactory.getInstance().newItem(ItemName.THAN_CHET);
        if (ranking == 1) {
            for (int i = 0; i < 5; i++) {
                Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(mysteryChest);
            }
            petthanchet.expire = System.currentTimeMillis() + ConstTime.DAY * 6000L;
            p.addItemToBag(petthanchet);
            p.addItemToBag(fashionStick);
        } else if (ranking == 2) {
            for (int i = 0; i < 3; i++) {
                Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(mysteryChest);
            }
            petthanchet.expire = System.currentTimeMillis() + ConstTime.DAY * 6000L;
            p.addItemToBag(petthanchet);
            p.addItemToBag(fashionStick);
        } else if (ranking == 3) {
            for (int i = 0; i < 3; i++) {
                Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(mysteryChest);
            }
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 6000L;
            p.addItemToBag(petthanchet);
        } else if (ranking >= 4 && ranking <= 6) {
            petthanchet.expire = System.currentTimeMillis() + ConstTime.DAY * 60L;
            p.addItemToBag(petthanchet);
        } else if (ranking >= 7 && ranking <= 10) {
            petthanchet.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            p.addItemToBag(petthanchet);
        }
    }

    public void topHoLo(int ranking, Char p) {
        Item mount = ItemFactory.getInstance().newItem(ItemName.HOA_KY_LAN);
        int tickId = p.gender == 1 ? ItemName.MAT_NA_HO : ItemName.MAT_NA_HO;
        Item fashionStick = ItemFactory.getInstance().newItem(tickId);
        if (ranking == 1) {
            mount.options.add(new ItemOption(ItemOptionName.NE_DON_ADD_POINT_TYPE_1, 200));
            mount.options.add(new ItemOption(ItemOptionName.CHINH_XAC_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.TAN_CONG_KHI_DANH_CHI_MANG_POINT_PERCENT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.CHI_MANG_ADD_POINT_TYPE_1, 100));
            p.addItemToBag(mount);
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 6000L;
            p.addItemToBag(fashionStick);
            for (int i = 0; i < 5; i++) {
                Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(mysteryChest);
            }
        } else if (ranking == 2) {
            for (int i = 0; i < 3; i++) {
                Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(mysteryChest);
            }
            mount.options.add(new ItemOption(ItemOptionName.NE_DON_ADD_POINT_TYPE_1, 200));
            mount.options.add(new ItemOption(ItemOptionName.CHINH_XAC_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.TAN_CONG_KHI_DANH_CHI_MANG_POINT_PERCENT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.CHI_MANG_ADD_POINT_TYPE_1, 100));
            p.addItemToBag(mount);
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 6000L;
            p.addItemToBag(fashionStick);
        } else if (ranking == 3) {
            mount.options.add(new ItemOption(ItemOptionName.NE_DON_ADD_POINT_TYPE_1, 200));
            mount.options.add(new ItemOption(ItemOptionName.CHINH_XAC_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.TAN_CONG_KHI_DANH_CHI_MANG_POINT_PERCENT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.CHI_MANG_ADD_POINT_TYPE_1, 100));
            p.addItemToBag(mount);
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 6000L;
            p.addItemToBag(fashionStick);
            for (int i = 0; i < 3; i++) {
                Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(blueChest);
            }
        } else if (ranking >= 4 && ranking <= 6) {
            mount.options.add(new ItemOption(ItemOptionName.NE_DON_ADD_POINT_TYPE_1, 200));
            mount.options.add(new ItemOption(ItemOptionName.CHINH_XAC_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.TAN_CONG_KHI_DANH_CHI_MANG_POINT_PERCENT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.CHI_MANG_ADD_POINT_TYPE_1, 100));
            mount.expire = System.currentTimeMillis() + ConstTime.DAY * 60L;
            p.addItemToBag(mount);
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            p.addItemToBag(fashionStick);
        } else if (ranking >= 7 && ranking <= 10) {
            mount.options.add(new ItemOption(ItemOptionName.NE_DON_ADD_POINT_TYPE_1, 200));
            mount.options.add(new ItemOption(ItemOptionName.CHINH_XAC_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.TAN_CONG_KHI_DANH_CHI_MANG_POINT_PERCENT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.CHI_MANG_ADD_POINT_TYPE_1, 100));
            mount.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            p.addItemToBag(mount);
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            p.addItemToBag(fashionStick);
        }
    }

    // TODO: hiệu ứng cây ở map
    @Override
    public void initMap(Zone zone) {
    }

}
