/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.event;

import com.nsoz.constants.*;
import com.nsoz.effect.Effect;
import com.nsoz.effect.EffectAutoDataManager;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.Map;
import com.nsoz.map.Tree;
import com.nsoz.map.zones.Zone;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.model.Menu;
import com.nsoz.option.ItemOption;
import com.nsoz.server.GlobalService;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;
import com.nsoz.util.NinjaUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class Halloween extends Event {

    public static final String TOP_DEVIL_BOX = "devil_box";
    public static final String TOP_KEO_TAO = "keo_tao";
    public static final String INVITATION_NUMBER = "invitation_number";
    private static final int HOP_MA_QUY = 0;
    private static final int KEO_TAO = 1;
    private static final int CHIA_KHOA = 2;
    private static final int THOI_TRANG = 4;
    private static final int DOI_XICH_TU_MA = 5;
    private static final int DOI_TA_LINH_MA = 6;
    private static final int DOI_PHUONG_HOANG_BANG = 7;
    protected RandomCollection<Integer> itemsThrownFromMonsters2;
    private RandomCollection<Integer> rd = new RandomCollection<>();
    private RandomCollection<Integer> vipItems = new RandomCollection<>();

    public Halloween() {

        // TODO: time chạy sự kiện
        setId(Event.HALLOWEEN);
        endTime.set(2027, 11, 12, 8, 50, 59); // TODO: cứ để nó dài ra, mình tắt sk ở file config
        keyEventPoint.add(TOP_DEVIL_BOX);
        keyEventPoint.add(TOP_KEO_TAO);
        keyEventPoint.add(INVITATION_NUMBER);

        // TODO: rơi nlsk khi đánh quái, thêm càng nhiều dòng nó ra càng nhiều
        itemsThrownFromMonsters.add(100, ItemName.QUA_TAO);
        itemsThrownFromMonsters.add(100, ItemName.MAT_ONG);
        itemsThrownFromMonsters.add(100, ItemName.TAN_LINH);
        itemsThrownFromMonsters.add(100, ItemName.XUONG_THU);
        itemsThrownFromMonsters2 = new RandomCollection<>();
        itemsThrownFromMonsters2.add(100, ItemName.H);
        itemsThrownFromMonsters2.add(100, ItemName.A);
        itemsThrownFromMonsters2.add(100, ItemName.L);
        itemsThrownFromMonsters2.add(100, ItemName.O);
        itemsThrownFromMonsters2.add(100, ItemName.W);
        itemsThrownFromMonsters2.add(100, ItemName.E);
        itemsThrownFromMonsters2.add(100, ItemName.N);
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
                .id(998)
                .itemID(ItemName.CHIA_KHOA)
                .coin(20000)
                .expire(ConstTime.FOREVER)
                .build());

        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(999)
                .itemID(ItemName.MA_VAT)
                .gold(25)
                .expire(ConstTime.FOREVER)
                .build());

        List<ItemOption> options = new ArrayList<ItemOption>();
        options.add(new ItemOption(ItemOptionName.MOI_NUA_GIAY_HOI_PHUC_POINT_HP_VA_MP_TYPE_8, 400));
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(1000)
                .itemID(ItemName.BI_RE_HANH)
                .gold(500)
                .expire(ConstTime.NSO)
                .options(options)
                .build());

        List<ItemOption> options2 = new ArrayList<ItemOption>();
        options2.add(new ItemOption(ItemOptionName.HP_TOI_DA_POINT_TYPE_1, 2000));
        options2.add(new ItemOption(ItemOptionName.CONG_THEM_TIEM_NANG_ADD_POINT_PERCENT_TYPE_0, 25));
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(1001)
                .itemID(ItemName.JACK_HOLLOW)
                .gold(1000)
                .expire(ConstTime.NSO)
                .options(options2)
                .build());

    }


    // todo: sử dụng item sự kiện
    @Override
    public void useItem(Char _char, Item item) {

        if (item.id == ItemName.THU_MOI_LE_HOI) {
            _char.getEventPoint().addPoint(INVITATION_NUMBER, 1);
            _char.serverMessage(
                    "Số lượt tham gia lễ hội hoá trang: " + _char.getEventPoint().find(INVITATION_NUMBER).getPoint());
            _char.removeItem(item.index, 1, true);
        } else if (item.id == ItemName.BI_MA) {
            int time = 8 * 60 * 60 * 1000;
            short param = 2;
            byte templateID = 43;
            Effect eff = _char.getEm().findByID(templateID);
            if (eff != null) {
                eff.addTime(time);
                _char.getEm().setEffect(eff);
            } else {
                Effect effect = new Effect(templateID, time, param);
                effect.param2 = item.id;
                _char.getEm().setEffect(effect);
            }
            _char.removeItem(item.index, 1, true);

        } else if (item.id == ItemName.KEO_TAO) {
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }
            useEventItem(_char, item.id, itemsRecFromCoinItem);
            _char.getEventPoint().addPoint(TOP_KEO_TAO, 1);


        } else if (item.id == ItemName.HOP_MA_QUY) {
            int indexItem = _char.getIndexItemByIdInBag(ItemName.CHIA_KHOA);
            if (indexItem == -1) {
                _char.serverMessage("Hãy mua chìa khóa ở npc Goosho để mở hộp ma quỷ");
                return;
            }
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }
            int[][] itemRequires = new int[][]{{ItemName.HOP_MA_QUY, 1}, {ItemName.CHIA_KHOA, 1}};
            useEventItem(_char, 1, itemRequires, 0, 0, 0, itemsRecFromGoldItem);
            _char.getEventPoint().addPoint(TOP_DEVIL_BOX, 1);

        }
    }

    public int randomItemID2() {
        return itemsThrownFromMonsters2.next();
    }

    @Override
    public void action(Char p, int type, int amount) {
        if (isEnded()) {
            p.serverMessage("Sự kiện đã kết thúc");
            return;
        }
        switch (type) {
            case CHIA_KHOA:
                makeKey(p, amount);
                break;

            case HOP_MA_QUY:
                makeDevilBox(p, amount);
                break;

            case KEO_TAO:
                makeAppleCandy(p, amount);
                break;

            case THOI_TRANG:
                makeFashionItem(p);
                break;
            case DOI_XICH_TU_MA:
                doiXichTuMa(p);
                break;
            case DOI_TA_LINH_MA:
                doiTaLinhMa(p);
                break;
            case DOI_PHUONG_HOANG_BANG:
                doiPhuongHoangBang(p);
                break;
        }
    }

    // TODO: đổi hộp ma quỷ
    public void makeDevilBox(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.XUONG_THU, 5}, {ItemName.TAN_LINH, 2}, {ItemName.MA_VAT, 1}};
        int itemIdReceive = ItemName.HOP_MA_QUY;
        makeEventItem(p, amount, itemRequires, 0, 0, 0, itemIdReceive);

    }

    // TODO: đổi kẹo táo
    public void makeAppleCandy(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.QUA_TAO, 2}, {ItemName.MAT_ONG, 3}};
        int itemIdReceive = ItemName.KEO_TAO;
        makeEventItem(p, amount, itemRequires, 0, 100000, 0, itemIdReceive);
    }

    // TODO: đổi chữ lấy chìa khóa và ma vật
    public void makeKey(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.H, 1}, {ItemName.A, 1}, {ItemName.L, 1}, {ItemName.O, 1}, {ItemName.W, 1}, {ItemName.E, 1}, {ItemName.N, 1}};
        int itemIdReceive = ItemName.CHIA_KHOA;
        makeEventItem(p, amount, itemRequires, 0, 0, 0, itemIdReceive);
    }

    // TODO: đổi đồ thời trang
    public void makeFashionItem(Char p) {
        if (p.user.gold < 1000) {
            p.getService().npcChat(NpcName.TIEN_NU, "Cần 1000 lượng để đổi.");
            return;
        }
        int index = p.getIndexItemByIdInBag(ItemName.KEO_TAO);
        Item itm = null;
        if (index != -1) {
            itm = p.bag[index];
        }
        if (itm == null || !itm.has(100)) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ kẹo táo.");
            return;
        }
        p.addGold(-1000);
        p.removeItem(index, 100, true);
        int maskId = p.gender == 1 ? ItemName.THOI_TRANG_OBITO : ItemName.THOI_TRANG_SAKURA;
        Item item = ItemFactory.getInstance().newItem(maskId);
        item.expire = System.currentTimeMillis() + (long) (86400000 * 7);
        item.isLock = false;
        p.addItemToBag(item);
    }

    // TODO: đổi ngựa
    public void doiXichTuMa(Char p) {
        int amount = 100;
        List<Item> list = p.getListItemByID(ItemName.XICH_TU_MA);
        if (list.size() < amount) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ " + amount + " Xích Tử Mã hạn");
            return;
        }

        for (Item item : list.subList(0, amount)) {
            if (item.expire != -1) {
                p.removeItem(item.index, 1, true);
            }
        }
        Item item = ItemFactory.getInstance().newItem(ItemName.XICH_TU_MA);
        item.setQuantity(1);
        item.isLock = false;
        item.expire = -1;
        p.addItemToBag(item);
    }

    // TODO: đổi ngựa
    public void doiTaLinhMa(Char p) {
        int amount = 100;
        List<Item> list = p.getListItemByID(ItemName.TA_LINH_MA);
        if (list.size() < amount) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ " + amount + " Tà Linh Mã hạn");
            return;
        }

        for (Item item : list.subList(0, amount)) {
            if (item.expire != -1) {
                p.removeItem(item.index, 1, true);
            }
        }
        Item item = ItemFactory.getInstance().newItem(ItemName.TA_LINH_MA);
        item.setQuantity(1);
        item.isLock = false;
        item.expire = -1;
        p.addItemToBag(item);
    }

    // TODO: đổi phượng hoàng băng
    public void doiPhuongHoangBang(Char p) {
        int amount = 100;
        List<Item> list = p.getListItemByID(ItemName.PHUONG_HOANG_BANG);
        if (list.size() < amount) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ " + amount + " Phượng Hoàng Băng hạn");
            return;
        }

        for (Item item : list.subList(0, amount)) {
            if (item.expire != -1) {
                p.removeItem(item.index, 1, true);
            }
        }
        Item item = ItemFactory.getInstance().newItem(ItemName.PHUONG_HOANG_BANG);
        item.setQuantity(1);
        item.isLock = false;
        item.expire = -1;
        p.addItemToBag(item);
    }


    // TODO: menu npc tiên nữ
    @Override
    public void menu(Char p) {

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm hộp ma quỷ", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Hộp ma quỷ", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, HOP_MA_QUY, number);
                } catch (Exception e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm kẹo táo", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Kẹo táo", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, KEO_TAO, number);
                } catch (Exception e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi chìa khóa", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Đổi chìa khóa", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, CHIA_KHOA, number);
                } catch (Exception e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi đồ thời trang", () -> {
            action(p, THOI_TRANG, 1);
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi item hiếm", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Xích tử mã vv", () -> {
                action(p, DOI_XICH_TU_MA, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Tà linh mã vv", () -> {
                action(p, DOI_TA_LINH_MA, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Phượng hoàng băng vv", () -> {
                action(p, DOI_PHUONG_HOANG_BANG, 1);
            }));

            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi lồng đèn", () -> {
            p.setCommandBox(Char.DOI_LONG_DEN_LUONG);
            List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP, ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
            p.getService().openUIShopTrungThu(list, "Đổi lồng đèn 500 lượng", "Đổi");
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Top sự kiện", () -> {
            p.menus.clear();

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bxh hộp ma quỷ", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    if (p.user.isTien()) {
                        viewTop(p, TOP_DEVIL_BOX, "Top hộp ma quỷ", "%d. %s đã mở %s hộp ma quỷ");
                    } else {
                        viewTop(p, TOP_DEVIL_BOX, "Top hộp ma quỷ", "%d. %s ");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Top 1:....");
                }));
                p.getService().openUIMenu();
            }));

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bxh kẹo táo", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng kẹo táo", () -> {
                    if (p.user.isTien()) {
                        viewTop(p, TOP_KEO_TAO, "Top kẹo táo", "%d. %s đã mở %s kẹo táo");
                    } else {
                        viewTop(p, TOP_KEO_TAO, "Top kẹo táo", "%d. %s ");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Top 1:....");
                }));
                p.getService().openUIMenu();
            }));

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Mốc 8k hộp(cá nhân)", () -> {
                int pont = getPoint(p, TOP_DEVIL_BOX);
                if (p.getEventPoint().getRewarded(TOP_DEVIL_BOX) == 1) {
                    p.serverDialog("Bạn đã nhận thưởng mốc này rồi");
                    return;
                }
                if (pont >= 8000) {
                    int idItem = NinjaUtils.nextInt(814, 818);
                    Item mount = ItemFactory.getInstance().newItem(idItem);

                    ArrayList<ItemOption> options = new ArrayList<>();
                    options.add(new ItemOption(0, NinjaUtils.nextInt(200, 500))); // tấn công ngoai
                    options.add(new ItemOption(1, NinjaUtils.nextInt(200, 500))); // tấn công nội
                    options.add(new ItemOption(2, NinjaUtils.nextInt(100, 150))); // kháng
                    options.add(new ItemOption(3, NinjaUtils.nextInt(100, 150))); // kháng
                    options.add(new ItemOption(4, NinjaUtils.nextInt(100, 150))); // kháng

                    options.add(new ItemOption(5, NinjaUtils.nextInt(50, 100))); // né đòn
                    options.add(new ItemOption(6, NinjaUtils.nextInt(1000, 2000))); // hp tối đa

                    options.add(new ItemOption(8, NinjaUtils.nextInt(50, 200))); // vật công ngoại
                    options.add(new ItemOption(9, NinjaUtils.nextInt(50, 200))); // vật công nội

                    options.add(new ItemOption(57, NinjaUtils.nextInt(80, 120))); // cộng tiềm năng cho tất cả
                    options.add(new ItemOption(58, NinjaUtils.nextInt(20, 30))); // cộng % tiềm năng
                    options.add(new ItemOption(87, NinjaUtils.nextInt(1000, 5000))); // tấn công
                    mount.expire = System.currentTimeMillis() + ConstTime.DAY * 15;
                    mount.isLock = false;

                    for (int i = 1; i <= 7; i++) {
                        int indexRandom = NinjaUtils.nextInt(options.size());
                        mount.options.add(options.get(indexRandom));
                        options.remove(indexRandom);
                    }
                    if (p.getSlotNull() < 6) {
                        p.getService().npcChat(NpcName.TIEN_NU, "Hãy chừa 6 ô trống trong hành trang để nhận quà.");
                        return;
                    }
                    p.addItemToBag(mount);
                    p.getEventPoint().setRewarded(TOP_DEVIL_BOX, 1);
                } else {
                    p.serverDialog("Bạn mới chỉ sử dụng " + pont + " hộp ma quỷ, cần mở thêm " + (8000 - pont) + " hộp");
                }
            }));

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Mốc 40k hộp(cá nhân)", () -> {
                int pont = getPoint(p, TOP_DEVIL_BOX);
                if (p.getEventPoint().getRewarded50k(TOP_DEVIL_BOX) == 1) {
                    p.serverDialog("Bạn đã nhận thưởng mốc này rồi");
                    return;
                }
                if (pont >= 40000) {
                    int idItem = NinjaUtils.nextInt(814, 818);
                    Item mount = ItemFactory.getInstance().newItem(idItem);

                    ArrayList<ItemOption> options = new ArrayList<>();
                    options.add(new ItemOption(0, NinjaUtils.nextInt(200, 500))); // tấn công ngoai
                    options.add(new ItemOption(1, NinjaUtils.nextInt(200, 500))); // tấn công nội
                    options.add(new ItemOption(2, NinjaUtils.nextInt(100, 150))); // kháng
                    options.add(new ItemOption(3, NinjaUtils.nextInt(100, 150))); // kháng
                    options.add(new ItemOption(4, NinjaUtils.nextInt(100, 150))); // kháng

                    options.add(new ItemOption(5, NinjaUtils.nextInt(50, 100))); // né đòn
                    options.add(new ItemOption(6, NinjaUtils.nextInt(1000, 2000))); // hp tối đa

                    options.add(new ItemOption(8, NinjaUtils.nextInt(50, 200))); // vật công ngoại
                    options.add(new ItemOption(9, NinjaUtils.nextInt(50, 200))); // vật công nội

                    options.add(new ItemOption(57, NinjaUtils.nextInt(80, 120))); // cộng tiềm năng cho tất cả
                    options.add(new ItemOption(58, NinjaUtils.nextInt(20, 30))); // cộng % tiềm năng
                    options.add(new ItemOption(87, NinjaUtils.nextInt(1000, 5000))); // tấn công
                    mount.isLock = false;

                    for (int i = 1; i <= 7; i++) {
                        int indexRandom = NinjaUtils.nextInt(options.size());
                        mount.options.add(options.get(indexRandom));
                        options.remove(indexRandom);
                    }
                    if (p.getSlotNull() < 6) {
                        p.getService().npcChat(NpcName.TIEN_NU, "Hãy chừa 6 ô trống trong hành trang để nhận quà.");
                        return;
                    }
                    p.addItemToBag(mount);
                    p.getEventPoint().setRewarded50k(TOP_DEVIL_BOX, 1);
                } else {
                    p.serverDialog("Bạn mới chỉ sử dụng " + pont + " hộp ma quỷ, cần mở thêm " + (40000 - pont) + " hộp");
                }

            }));

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Mốc 50k hộp(tính cả server)", () -> {
                int pont = getPontAll(TOP_DEVIL_BOX);
                if (p.getEventPoint().getRewardedAll(TOP_DEVIL_BOX) == 1) {
                    p.serverDialog("Bạn đã nhận thưởng mốc này rồi");
                    return;
                }
                if (pont >= 50000) {
                    Item itm = ItemFactory.getInstance().newItem(ItemName.HOA_KY_LAN);
                    itm.isLock = false;
                    itm.expire = System.currentTimeMillis() + ConstTime.DAY * 5;
                    if (p.getSlotNull() < 6) {
                        p.getService().npcChat(NpcName.TIEN_NU, "Hãy chừa 6 ô trống trong hành trang để nhận quà.");
                        return;
                    }
                    p.addItemToBag(itm);
                    p.getEventPoint().setRewardedAll(TOP_DEVIL_BOX, 1);
                } else {
                    p.serverDialog("Server mới chỉ sử dụng " + pont + " hộp ma quỷ, cần mở thêm " + (50000 - pont) + " hộp");
                }
            }));

            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Hướng dẫn", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("--------------");
            p.getService().showAlert("Hướng Dẫn", sb.toString());
        }));
    }


    // TODO: hiệu ứng cây ở map
    @Override
    public void initMap(Zone zone) {
        Map map = zone.map;
        int mapID = map.id;
        switch (mapID) {
            case MapName.KHU_LUYEN_TAP:
                break;
            case MapName.TRUONG_OOKAZA:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 1426).y((short) 552).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 784).y((short) 648).build());
                break;
            case MapName.TRUONG_HARUNA:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 502).y((short) 408).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 1863).y((short) 360).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 2048).y((short) 360).build());
                break;
            case MapName.TRUONG_HIROSAKI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 1207).y((short) 168).build());
                break;

            case MapName.LANG_TONE:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 1427).y((short) 264).build());
                break;

            case MapName.LANG_KOJIN:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 621).y((short) 288).build());
                break;

            case MapName.LANG_CHAI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 1804).y((short) 384).build());
                break;

            case MapName.LANG_SANZU:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 320).y((short) 288).build());
                break;

            case MapName.LANG_CHAKUMI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 626).y((short) 312).build());
                break;

            case MapName.LANG_ECHIGO:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 360).y((short) 360).build());
                break;

            case MapName.LANG_OSHIN:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 921).y((short) 408).build());
                break;

            case MapName.LANG_SHIIBA:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 583).y((short) 408).build());
                break;

            case MapName.LANG_FEARRI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_HALLOWEEN).x((short) 611).y((short) 312).build());
                break;
        }
    }

}
