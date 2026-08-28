/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.event;

import com.nsoz.constants.*;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.map.zones.Zone;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.model.Menu;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;
import com.nsoz.util.NinjaUtils;

import java.util.List;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class InternationalWomensDay extends Event {
    public static final String TOP_LUCKY_CHARM = "lucky_charm";
    public static final String TOP_MAKE_CHUNG_CAKE = "chung_cake";
    private static final int DOI_HOA_HONG_XANH = 0;
    private static final int BO_HOA_HONG_DO = 1;
    private static final int BO_HOA_HONG_VANG = 2;
    private static final int BO_HOA_HONG_XANH = 3;

    public InternationalWomensDay() {  // todo:  sự kiện ngày phụ nữ 8/3
        setId(Event.WOMENS_DAY);
        endTime.set(2028, 3, 5, 23, 59, 59); // TODO: time chạy sự kiện, cứ để nó dài ra, mình tắt sk ở file config
        keyEventPoint.add(EventPoint.DIEM_TIEU_XAI);
        keyEventPoint.add(TOP_LUCKY_CHARM);
        keyEventPoint.add(TOP_MAKE_CHUNG_CAKE);

        // TODO: rơi nlsk khi đánh quái, thêm càng nhiều dòng nó ra càng nhiều
        itemsThrownFromMonsters.add(100, ItemName.HOA_HONG_DO);
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
                .itemID(ItemName.GIAY_MAU)
                .coin(100000)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(999)
                .itemID(ItemName.RUY_BANG)
                .gold(25)
                .expire(ConstTime.FOREVER)
                .build());
    }

    // todo: sử dụng item sự kiện
    @Override
    public void useItem(Char _char, Item item) {
        if (item.id == ItemName.BO_HOA_HONG_VANG) {
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }
            useEventItem(_char, item.id, itemsRecFromGoldItem);
            _char.getEventPoint().addPoint(InternationalWomensDay.TOP_MAKE_CHUNG_CAKE, 1);
        } else if (item.id == ItemName.BO_HOA_HONG_DO || item.id == ItemName.BO_HOA_HONG_XANH) {
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }
            useEventItem(_char, item.id, itemsRecFromGoldItem);
            _char.getEventPoint().addPoint(InternationalWomensDay.TOP_LUCKY_CHARM, 1);
        }
    }

    @Override
    public void action(Char p, int type, int amount) {
        if (isEnded()) {
            p.serverMessage("Sự kiện đã kết thúc");
            return;
        }
        switch (type) {
            case DOI_HOA_HONG_XANH:
                doiHoaHongXanh(p, amount);
                break;

            case BO_HOA_HONG_DO:
                boHoaHongDo(p, amount);
                break;

            case BO_HOA_HONG_VANG:
                boHoaHongVang(p, amount);
                break;

            case BO_HOA_HONG_XANH:
                boHoaHongXanh(p, amount);
                break;
        }
    }

    public void doiHoaHongXanh(Char p, int amount) {
        if (amount < 1) {
            p.getService().npcChat(NpcName.TIEN_NU, "Số lượng tối thiểu là 1.");
            return;
        }

        if (amount > 1000) {
            p.getService().npcChat(NpcName.TIEN_NU, "Số lượng tối đa là 1.000.");
            return;
        }
        int requiredPoint = 10 * amount;
        int point = p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI);
        if (point < requiredPoint) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ điểm tiêu xài.");
            return;
        }
        p.getEventPoint().subPoint(EventPoint.DIEM_TIEU_XAI, requiredPoint);
        Item item = ItemFactory.getInstance().newItem(ItemName.HOA_HONG_XANH);
        item.setQuantity(amount);
        p.addItemToBag(item);
    }

    public void boHoaHongDo(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.HOA_HONG_DO, 8}, {ItemName.RUY_BANG, 1}};
        int itemIdReceive = ItemName.BO_HOA_HONG_DO;
        makeEventItem(p, amount, itemRequires, 0, 0, 0, itemIdReceive);
    }

    public void boHoaHongVang(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.HOA_HONG_DO, 10}, {ItemName.GIAY_MAU, 1}};
        int itemIdReceive = ItemName.BO_HOA_HONG_VANG;
        makeEventItem(p, amount, itemRequires, 0, 0, 0, itemIdReceive);
    }

    public void boHoaHongXanh(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.HOA_HONG_XANH, 50}};
        int itemIdReceive = ItemName.BO_HOA_HONG_XANH;
        makeEventItem(p, amount, itemRequires, 0, 0, 500000, itemIdReceive);
    }

    // TODO: menu npc tiên nữ
    @Override
    public void menu(Char p) {
        p.menus.clear();
        if (!isEnded()) {
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi Hoa Hồng Xanh", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Hoa Hồng Xanh", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, DOI_HOA_HONG_XANH, number);
                    } catch (Exception e) {
                        if (!input.isEmpty()) {
                            p.inputInvalid();
                        }
                    }
                }));
                p.getService().showInputDialog();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi Bó Hoa", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bó Hoa Hồng Đỏ", () -> {
                    p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Số Bó Hoa Hồng Đỏ", () -> {
                        InputDialog input = p.getInput();
                        try {
                            int number = input.intValue();
                            action(p, BO_HOA_HONG_DO, number);
                        } catch (Exception e) {
                            if (!input.isEmpty()) {
                                p.inputInvalid();
                            }
                        }
                    }));
                    p.getService().showInputDialog();
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bó Hoa Hồng Vàng", () -> {
                    p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Số Bó Hoa Hồng Vàng", () -> {
                        InputDialog input = p.getInput();
                        try {
                            int number = input.intValue();
                            action(p, BO_HOA_HONG_VANG, number);
                        } catch (Exception e) {
                            if (!input.isEmpty()) {
                                p.inputInvalid();
                            }
                        }
                    }));
                    p.getService().showInputDialog();
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bó Hoa Hồng Xanh", () -> {
                    p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Số Bó Hoa Hồng Xanh", () -> {
                        InputDialog input = p.getInput();
                        try {
                            int number = input.intValue();
                            action(p, BO_HOA_HONG_XANH, number);
                        } catch (Exception e) {
                            if (!input.isEmpty()) {
                                p.inputInvalid();
                            }
                        }
                    }));
                    p.getService().showInputDialog();
                }));
                p.getService().openUIMenu();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi lồng đèn trang bị 2", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "2 triệu xu", () -> {
                    p.setCommandBox(Char.DOI_LONG_DEN_XU);
                    List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP,
                            ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
                    p.getService().openUIShopTrungThu(list, "Đổi lồng đèn 2 triệu xu", "Đổi (2 triệu xu)");
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "500 lượng", () -> {
                    p.setCommandBox(Char.DOI_LONG_DEN_LUONG);
                    List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP,
                            ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
                    p.getService().openUIShopTrungThu(list, "Đổi lồng đèn 500 lượng", "Đổi (500l)");
                }));
                p.getService().openUIMenu();
            }));
        }


        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đua TOP", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bó hoa đỏ", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {

                    viewTop(p, TOP_LUCKY_CHARM, "Bó hoa đỏ", "%d. %s ");
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                p.getService().openUIMenu();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bó hoa vàng", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_MAKE_CHUNG_CAKE, "Bó hoa vàng", "%d. %s ");
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                p.getService().openUIMenu();
            }));

            p.getService().openUIMenu();
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Hướng dẫn", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("- Điểm tiêu xài : ").append(p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI)).append("\n");
            sb.append("- Điểm bó hoa đỏ : ").append(NinjaUtils.getCurrency(p.getEventPoint().getPoint(TOP_LUCKY_CHARM))).append("\n");
            sb.append("- Điểm bó hoa vàng : ").append(NinjaUtils.getCurrency(p.getEventPoint().getPoint(TOP_MAKE_CHUNG_CAKE))).append("\n");
            sb.append("- 10 điểm tiêu xài = 1 Hoa hồng xanh\n" +
                    "Bó hoa đỏ = 8 hoa hồng đỏ + 1 ruy băng\n" +
                    "Bó hoa vàng = 10 hoa hồng đỏ + 1 giấy màu\n" +
                    "Bó hoa xanh = 50 hoa hồng xanh + 500k yên");
            p.getService().showAlert("Hướng Dẫn", sb.toString());
        }));
    }

    // TODO: hiệu ứng cây ở map
    @Override
    public void initMap(Zone zone) {

    }

}
