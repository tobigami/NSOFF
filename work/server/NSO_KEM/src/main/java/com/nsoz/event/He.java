/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.event;

import com.nsoz.constants.*;
import com.nsoz.effect.EffectAutoDataManager;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.Map;
import com.nsoz.map.Tree;
import com.nsoz.map.zones.Zone;
import com.nsoz.mob.Mob;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.model.Menu;
import com.nsoz.model.RandomItem;
import com.nsoz.npc.Npc;
import com.nsoz.option.ItemOption;
import com.nsoz.server.Events;
import com.nsoz.npc.NpcFactory;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;
import com.nsoz.util.NinjaUtils;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.List;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class He extends Event {


    public static final String TOP_CAU_CA = "cauca_top";
    public static final String TOP_DIEU_VAI = "dieuvai_top";
    public static final String TOP_KEM = "kem_top";
    private static final int LAM_DIEU_GIAY = 0;
    private static final int LAM_DIEU_VAI = 1;
    private static final int LAM_HU_KEM = 2;

    public He() {
        setId(Event.HE);
        Events.event = Events.SUMMER;
        endTime.set(2028, 4, 20, 23, 59, 59); // TODO: time chạy sự kiện, cứ để nó dài ra, mình tắt sk ở file config
        keyEventPoint.add(EventPoint.DIEM_TIEU_XAI);
        keyEventPoint.add(TOP_CAU_CA);
        keyEventPoint.add(TOP_DIEU_VAI);
        keyEventPoint.add(TOP_KEM);

        // TODO: rơi nlsk khi đánh quái, thêm càng nhiều lần nó ra càng nhiều
        itemsThrownFromMonsters.add(100, ItemName.TRE);
        itemsThrownFromMonsters.add(100, ItemName.DAY);
        itemsThrownFromMonsters.add(100, ItemName.GIAY2);
        itemsThrownFromMonsters.add(100, ItemName.VAI);
        itemsThrownFromMonsters.add(100, ItemName.KEM_DAU);
        itemsThrownFromMonsters.add(100, ItemName.KEM_CHOCOLATE);
        itemsThrownFromMonsters.add(100, ItemName.KEM_OC_QUE);
        itemsThrownFromMonsters.add(100, ItemName.KEM_SUA);
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
        itemsRecFromCoinItem.add(0.5, ItemName.BUA_AITEMU);
        itemsRecFromCoinItem.add(0.5, ItemName.BUA_SOCHI);
        itemsRecFromCoinItem.add(0.5, ItemName.BUA_NORU);
        itemsRecFromCoinItem.add(0.1, ItemName.TRUNG_VI_THU);

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
        itemsRecFromGoldItem.add(0.1, ItemName.TRUNG_VI_THU);
        itemsRecFromGoldItem.add(0.5, ItemName.BUA_AITEMU);
        itemsRecFromGoldItem.add(0.5, ItemName.BUA_SOCHI);
        itemsRecFromGoldItem.add(0.5, ItemName.BUA_NORU);


    }

    // TODO: cửa hàng goosho
    @Override
    public void initStore() {
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(910)
                .itemID(ItemName.CAN_CAU_CA)
                .gold(30)
                .expire(ConstTime.FOREVER)
                .build());
    }

    // todo: sử dụng item sự kiện
    @Override
    public void useItem(Char p, Item item) {
        switch (item.id) {
            case ItemName.DIEU_VAI:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem);
                break;
            case ItemName.DIEU_GIAY:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromCoinItem);
                break;
            case ItemName.HU_KEM_DAM:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                Npc npc = p.zone.getNpc(NpcName.EM_BE);
                if (npc == null) {
                    p.serverMessage("Hãy đi tìm Em Bé.");
                    return;
                }
                int distance = NinjaUtils.getDistance(npc.cx, npc.cy, p.x, p.y);
                if (distance > 100) {
                    p.serverMessage("Hãy lại gần Em Bé để sử dụng.");
                    return;
                }
                useEventItem(p, item.id, itemsRecFromCoinItem);
                break;
        }
    }

    // TODO: menu npc tiên nữ
    @Override
    public void menu(Char p) {

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm diều giấy", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Làm diều giấy", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, LAM_DIEU_GIAY, number);
                } catch (NumberFormatException e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm diều vải", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "làm diều vải", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, LAM_DIEU_VAI, number);
                } catch (NumberFormatException e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm hũ kem", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "làm hũ kem", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, LAM_HU_KEM, number);
                } catch (NumberFormatException e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi lồng đèn", () -> {
            p.setCommandBox(Char.DOI_LONG_DEN_LUONG);
            List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP, ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
            p.getService().openUIShopTrungThu(list, "Đổi lồng đèn 500 lượng", "Đổi");
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi điểm tiêu sài", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi dế ngọc", () -> {
                makeDeNgoc(p, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi bọ vàng", () -> {
                makeBoVang(p, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi bướm vàng", () -> {
                makeBuomVang(p, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Điểm sự kiện", () -> {
                p.getService().showAlert("Hướng dẫn", "- Điểm tiêu sài : "
                        + NinjaUtils.getCurrency(p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI))
                        + "\ndế ngọc = 10.000 điểm\n" +
                        "bọ vàng = 10.000 điểm\n" +
                        "bướm vàng = 10.000 điểm");
            }));
            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đua TOP", () ->   {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Top thả diều", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_DIEU_VAI, "Top thả diều", "%d. %s ");
                    if (p.user.isTien()) {
                        viewTop(p, TOP_DIEU_VAI, "Top thả diều", "%d. %s đã thả %s diều");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                if (isEnded()) {
                    int ranking = getRanking(p, TOP_DIEU_VAI);
                    if (ranking <= 10 && p.getEventPoint().getRewarded(TOP_DIEU_VAI) == 0) {
                        p.menus.add(new Menu(CMDMenu.EXECUTE, String.format("Nhận Thưởng TOP %d", ranking), () -> {
                            receiveReward(p, TOP_DIEU_VAI);
                        }));
                    }
                }
                p.getService().openUIMenu();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Top kem - diều giấy", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_KEM, "Top kem - diều giấy", "%d. %s ");
                    if (p.user.isTien()) {
                        viewTop(p, TOP_KEM, "Top kem - diều giấy", "%d. %s đã làm %s kem");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                if (isEnded()) {
                    int ranking = getRanking(p, TOP_KEM);
                    if (ranking <= 10 && p.getEventPoint().getRewarded(TOP_KEM) == 0) {
                        p.menus.add(new Menu(CMDMenu.EXECUTE, String.format("Nhận Thưởng TOP %d", ranking), () -> {
                            receiveReward(p, TOP_KEM);
                        }));
                    }
                }
                p.getService().openUIMenu();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Câu cá", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_CAU_CA, "Câu cá", "%d. %s ");
                    if (p.user.isTien()) {
                        viewTop(p, TOP_CAU_CA, "Câu cá", "%d. %s đã câu %s lần");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                if (isEnded()) {
                    int ranking = getRanking(p, TOP_CAU_CA);
                    if (ranking <= 10 && p.getEventPoint().getRewarded(TOP_CAU_CA) == 0) {
                        p.menus.add(new Menu(CMDMenu.EXECUTE, String.format("Nhận Thưởng TOP %d", ranking), () -> {
                            receiveReward(p, TOP_CAU_CA);
                        }));
                    }
                }
                p.getService().openUIMenu();
            }));
            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Hướng dẫn", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("- Số diều đã làm: ").append(NinjaUtils.getCurrency(p.getEventPoint().getPoint(TOP_DIEU_VAI))).append("\n");
            sb.append("- Số kem đã làm: ").append(NinjaUtils.getCurrency(p.getEventPoint().getPoint(TOP_KEM))).append("\n");
            sb.append("- Số lần câu cá: ").append(NinjaUtils.getCurrency(p.getEventPoint().getPoint(TOP_CAU_CA))).append("\n");
            sb.append("Diều giấy : 5 tre, 6 dây, 5 giấy + 70.000 xu\n" +
                    "Diều vải : 5 tre, 6 dây, 5 vải + 25 lượng \n" +
                    "\n" +
                    "Hũ kem dầm : 3 kem ốc quế, 2 kem sữa, 3 kem chocolate, 2 kem dâu + 70.000 xu\n" +
                    "Tại các làng xuất hiện Em Bé, hãy giúp các em ăn kem để nhận lại phần quà giá trị\n" +
                    "\n" +
                    "Mang lồng đèn trang bị 1 tới để đổi lấy lồng đèn trang bị 2 nhiều chỉ số\n" +
                    "\n" +
                    "Dùng điểm tiêu sài để đổi lấy linh vật, điểm có khi bạn làm diều và hũ kem\n" +
                    "\n" +
                    "Sử dụng cần câu cá mua tại Goosho với giá 30 lượng, bạn sẽ câu được những phần quà giá trị").append("\n");
            p.getService().showAlert("Hướng Dẫn", sb.toString());
        }));

    }

    // todo: trao top
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

        if (key == TOP_CAU_CA) {
            topCauCa(ranking, p);
        } else if (key == TOP_DIEU_VAI) {
            topChocolateCake(ranking, p);
        } else if (key == TOP_KEM) {
            topChocolateCake(ranking, p);
        }
        p.getEventPoint().setRewarded(key, 1);
    }

    public void topCauCa(int ranking, Char p) {
        Item mount = ItemFactory.getInstance().newItem(ItemName.PHUONG_HOANG_BANG);
        int tickId = p.gender == 1 ? ItemName.GAY_MAT_TRANG : ItemName.GAY_TRAI_TIM;
        Item fashionStick = ItemFactory.getInstance().newItem(tickId);
        Item tree = ItemFactory.getInstance().newItem(ItemName.TRUC_BACH_THIEN_LU);
        if (ranking == 1) {
            mount.options.add(new ItemOption(ItemOptionName.NE_DON_ADD_POINT_TYPE_1, 200));
            mount.options.add(new ItemOption(ItemOptionName.CHINH_XAC_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.TAN_CONG_KHI_DANH_CHI_MANG_POINT_PERCENT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.CHI_MANG_ADD_POINT_TYPE_1, 100));

            tree.setQuantity(10);
            p.addItemToBag(tree);
            for (int i = 0; i < 3; i++) {
                Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(mysteryChest);
            }
        } else if (ranking == 2) {
            tree.setQuantity(5);
            p.addItemToBag(tree);
            Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
            p.addItemToBag(mysteryChest);
        } else if (ranking >= 3 && ranking <= 5) {
            mount.expire = System.currentTimeMillis() + ConstTime.DAY * 90L;
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 90L;
            tree.setQuantity(3);
            p.addItemToBag(tree);
            for (int i = 0; i < 2; i++) {
                Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
                p.addItemToBag(blueChest);
            }
        } else {
            mount.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
            p.addItemToBag(blueChest);
        }

        p.addItemToBag(mount);
        p.addItemToBag(fashionStick);
    }

    public void topChocolateCake(int ranking, Char p) {
        Item pet = ItemFactory.getInstance().newItem(ItemName.PET_UNG_LONG);
        int maskId = p.gender == 1 ? ItemName.SHIRAIJI : ItemName.HAJIRO;
        Item mask = ItemFactory.getInstance().newItem(maskId);
        Item tree = ItemFactory.getInstance().newItem(ItemName.TRUC_BACH_THIEN_LU);
        if (ranking == 1) {
            pet.options.add(new ItemOption(ItemOptionName.HP_TOI_DA_ADD_POINT_TYPE_1, 3000));
            pet.options.add(new ItemOption(ItemOptionName.MP_TOI_DA_ADD_POINT_TYPE_1, 3000));
            pet.options.add(new ItemOption(ItemOptionName.CHI_MANG_POINT_TYPE_1, 100)); // chi mang
            pet.options.add(new ItemOption(ItemOptionName.TAN_CONG_ADD_POINT_PERCENT_TYPE_8, 10));
            pet.options.add(new ItemOption(ItemOptionName.MOI_5_GIAY_PHUC_HOI_MP_POINT_TYPE_1, 200));
            pet.options.add(new ItemOption(ItemOptionName.MOI_5_GIAY_PHUC_HOI_HP_POINT_TYPE_1, 200));
            pet.options.add(new ItemOption(ItemOptionName.KHONG_NHAN_EXP_TYPE_0, 1));

            tree.setQuantity(10);
            p.addItemToBag(tree);
            for (int i = 0; i < 3; i++) {
                Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
                p.addItemToBag(mysteryChest);
            }
        } else if (ranking == 2) {
            tree.setQuantity(5);
            p.addItemToBag(tree);
            Item mysteryChest = ItemFactory.getInstance().newItem(ItemName.RUONG_HUYEN_BI);
            p.addItemToBag(mysteryChest);
        } else if (ranking >= 3 && ranking <= 5) {
            pet.expire = System.currentTimeMillis() + ConstTime.DAY * 90L;
            mask.expire = System.currentTimeMillis() + ConstTime.DAY * 90L;
            tree.setQuantity(3);
            p.addItemToBag(tree);
            for (int i = 0; i < 2; i++) {
                Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
                p.addItemToBag(blueChest);
            }
        } else {
            pet.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            mask.expire = System.currentTimeMillis() + ConstTime.DAY * 90L;
            Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
            p.addItemToBag(blueChest);
        }

        p.addItemToBag(pet);
        p.addItemToBag(mask);
    }

    // todo: các menu phụ
    @Override
    public void action(Char p, int type, int amount) {
        if (isEnded()) {
            p.serverMessage("Sự kiện đã kết thúc");
            return;
        }
        switch (type) {
            case LAM_DIEU_GIAY:
                makeDieuGiay(p, amount);
                break;
            case LAM_DIEU_VAI:
                makeDieuVai(p, amount);
                break;
            case LAM_HU_KEM:
                makeHuKem(p, amount);
                break;
        }
    }

    private void makeDieuGiay(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.TRE, 5}, {ItemName.DAY, 6}, {ItemName.GIAY2, 5}};
        int itemIdReceive = ItemName.DIEU_GIAY;
        boolean isDome = makeEventItem(p, amount, itemRequires, 0, 70000, 0, itemIdReceive);
        if (isDome) {
            p.getEventPoint().addPoint(He.TOP_KEM, amount);
            p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, amount);
        }
    }

    private void makeDieuVai(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.TRE, 6}, {ItemName.DAY, 6}, {ItemName.VAI, 5}};
        int itemIdReceive = ItemName.DIEU_VAI;
        boolean isDone = makeEventItem(p, amount, itemRequires, 25, 0, 0, itemIdReceive);
        if (isDone) {
            p.getEventPoint().addPoint(He.TOP_DIEU_VAI, amount);
            p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, amount);

        }
    }

    private void makeHuKem(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.KEM_OC_QUE, 3}, {ItemName.KEM_SUA, 2}, {ItemName.KEM_CHOCOLATE, 3}, {ItemName.KEM_DAU, 2}};
        int itemIdReceive = ItemName.HU_KEM_DAM;
        boolean isDone = makeEventItem(p, amount, itemRequires, 0, 70000, 0, itemIdReceive);
        if (isDone) {
            p.getEventPoint().addPoint(He.TOP_KEM, amount);
            p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, amount);
        }
    }

    public void makeDeNgoc(Char p, int type) {
        int point = type == 1 ? 10000 : 20000;
        if (p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI) < point) {
            p.getService().npcChat(NpcName.TIEN_NU,
                    "Bạn cần tối thiểu " + NinjaUtils.getCurrency(point)
                            + " điểm sự kiện mới có thể đổi được vật này.");
            return;
        }

        if (p.getSlotNull() == 0) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return;
        }

        Item item = ItemFactory.getInstance().newItem(ItemName.DE_NGOC);
        p.addItemToBag(item);
        p.getEventPoint().subPoint(EventPoint.DIEM_TIEU_XAI, point);
    }

    public void makeBoVang(Char p, int type) {
        int point = type == 1 ? 10000 : 20000;
        if (p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI) < point) {
            p.getService().npcChat(NpcName.TIEN_NU,
                    "Bạn cần tối thiểu " + NinjaUtils.getCurrency(point)
                            + " điểm sự kiện mới có thể đổi được vật này.");
            return;
        }

        if (p.getSlotNull() == 0) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return;
        }

        Item item = ItemFactory.getInstance().newItem(ItemName.BO_VANG);
        p.addItemToBag(item);
        p.getEventPoint().subPoint(EventPoint.DIEM_TIEU_XAI, point);
    }

    public void makeBuomVang(Char p, int type) {
        int point = type == 1 ? 10000 : 20000;
        if (p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI) < point) {
            p.getService().npcChat(NpcName.TIEN_NU,
                    "Bạn cần tối thiểu " + NinjaUtils.getCurrency(point)
                            + " điểm sự kiện mới có thể đổi được vật này.");
            return;
        }

        if (p.getSlotNull() == 0) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return;
        }

        Item item = ItemFactory.getInstance().newItem(ItemName.BUOM_VANG);
        p.addItemToBag(item);
        p.getEventPoint().subPoint(EventPoint.DIEM_TIEU_XAI, point);
    }

    // TODO: hiệu ứng cây ở map
    @Override
    public void initMap(Zone zone) {
        Map map = zone.map;
        int mapID = map.id;
        switch (mapID) {

            case MapName.LANG_KOJIN:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 621, 288, 0));
                break;

            case MapName.LANG_CHAI:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 1804, 384, 0));
                break;

            case MapName.LANG_SANZU:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 320, 288, 0));
                break;

            case MapName.LANG_CHAKUMI:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 626, 312, 0));
                break;

            case MapName.LANG_ECHIGO:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 360, 360, 0));
                break;

            case MapName.LANG_OSHIN:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 921, 408, 0));
                break;

            case MapName.LANG_SHIIBA:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 583, 408, 0));
                break;

            case MapName.LANG_FEARRI:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.EM_BE, 611, 312, 0));
                break;

        }
    }

}
