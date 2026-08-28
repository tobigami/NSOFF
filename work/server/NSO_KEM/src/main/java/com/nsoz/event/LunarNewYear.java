package com.nsoz.event;

import com.nsoz.bot.Bot;
import com.nsoz.bot.Principal;
import com.nsoz.bot.move.PrincipalMove;
import com.nsoz.constants.*;
import com.nsoz.effect.EffectAutoDataManager;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.Map;
import com.nsoz.map.MapManager;
import com.nsoz.map.Tree;
import com.nsoz.map.zones.Zone;
import com.nsoz.mob.Mob;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.model.Menu;
import com.nsoz.npc.Npc;
import com.nsoz.npc.NpcFactory;
import com.nsoz.option.ItemOption;
import com.nsoz.server.GlobalService;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;
import com.nsoz.util.NinjaUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class LunarNewYear extends Event {

    public static final String TOP_LUCKY_CHARM = "lucky_charm";
    public static final String TOP_LUCKY_CHARM_1 = "lucky_charm_1";
    public static final String TOP_MAKE_CHUNG_CAKE = "chung_cake";
    public static final String TOP_BANH_TET = "tet_cake";
    public static final String MYSTERY_BOX_LEFT = "mystery_box";
    public static final String ENVELOPE = "envelope";
    private static final int MAKE_CHUNG_CAKE = 0;
    private static final int MAKE_TET_CAKE = 1;
    private static final int MAKE_FIREWORK = 2;
    public RandomCollection<Integer> vipItems = new RandomCollection<>();
    public RandomCollection<Integer> itemsRecFromGoldItem_1 = new RandomCollection<>();
    private ZonedDateTime start, end;
    private void timerSpawnPrincipal() {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        start = zonedNow.withMonth(1).withDayOfMonth(22).withHour(0).withMinute(0).withSecond(0);
        end = zonedNow.withMonth(1).withDayOfMonth(24).withHour(23).withMinute(59).withSecond(59);
        if (zonedNow.isAfter(start) && zonedNow.isBefore(end)) {
            start = zonedNow.plusMinutes(5);// thời gian khởi động server
        }
        if (zonedNow.compareTo(start) <= 0) {
            Duration duration = Duration.between(zonedNow, start);
            long initalDelay = duration.getSeconds();
            Runnable runnable = new Runnable() {
                public void run() {
                    spawnPrincipal();
                }
            };
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(runnable, initalDelay, 24 * 60 * 60, TimeUnit.SECONDS);

        }
    }

    public LunarNewYear() {
        setId(Event.LUNAR_NEW_YEAR);
        endTime.set(2029, 1, 15, 23, 59, 59); // TODO: time chạy sự kiện, cứ để nó dài ra, mình tắt sk ở file config
        keyEventPoint.add(EventPoint.DIEM_TIEU_XAI);
        keyEventPoint.add(TOP_LUCKY_CHARM);
        keyEventPoint.add(TOP_LUCKY_CHARM_1);
        keyEventPoint.add(TOP_MAKE_CHUNG_CAKE);
        keyEventPoint.add(TOP_BANH_TET);
        keyEventPoint.add(MYSTERY_BOX_LEFT);
        keyEventPoint.add(ENVELOPE);

        // TODO: rơi nlsk khi đánh quái, thêm càng nhiều dòng nó ra càng nhiều
        itemsThrownFromMonsters.add(100, ItemName.NEP);
        itemsThrownFromMonsters.add(100, ItemName.LA_DONG);
        itemsThrownFromMonsters.add(100, ItemName.DAU_XANH2);
        itemsThrownFromMonsters.add(100, ItemName.LAT_TRE);
        itemsThrownFromMonsters.add(100, ItemName.NEP);
        itemsThrownFromMonsters.add(100, ItemName.LA_DONG);
        itemsThrownFromMonsters.add(100, ItemName.DAU_XANH2);
        itemsThrownFromMonsters.add(100, ItemName.LAT_TRE);
        itemsThrownFromMonsters.add(100, ItemName.NEP);
        itemsThrownFromMonsters.add(100, ItemName.LA_DONG);
        itemsThrownFromMonsters.add(100, ItemName.DAU_XANH2);
        itemsThrownFromMonsters.add(100, ItemName.LAT_TRE);
        itemsThrownFromMonsters.add(100, ItemName.NEP);
        itemsThrownFromMonsters.add(100, ItemName.LA_DONG);
        itemsThrownFromMonsters.add(100, ItemName.DAU_XANH2);
        itemsThrownFromMonsters.add(100, ItemName.LAT_TRE);
        initRandomItem();
        timerSpawnPrincipal();
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

        // TODO: item pháo hoa
        itemsRecFromGoldItem_1.add(20, ItemName.DA_CAP_11);
        itemsRecFromGoldItem_1.add(30, ItemName.DA_CAP_10);
        itemsRecFromGoldItem_1.add(50, ItemName.DA_CAP_9);
        itemsRecFromGoldItem_1.add(20, ItemName.DA_DANH_VONG_CAP_3);
        itemsRecFromGoldItem_1.add(20, ItemName.DA_DANH_VONG_CAP_4);
        itemsRecFromGoldItem_1.add(10, ItemName.DA_DANH_VONG_CAP_5);
        itemsRecFromGoldItem_1.add(20, ItemName.VIEN_LINH_HON_CAP_3);
        itemsRecFromGoldItem_1.add(20, ItemName.VIEN_LINH_HON_CAP_4);
        itemsRecFromGoldItem_1.add(10, ItemName.VIEN_LINH_HON_CAP_5);

    }

    // TODO: cửa hàng goosho
    @Override
    public void initStore() {
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(996)
                .itemID(ItemName.THIT_HEO)
                .gold(20)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(997)
                .itemID(ItemName.THIEP_CHUC_TET)
                .coin(70000)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(998)
                .itemID(ItemName.THIEP_CHUC_TET_DAC_BIET)
                .gold(20)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(999)
                .itemID(ItemName.BUA_MAY_MAN)
                .gold(25)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(1000)
                .itemID(ItemName.VUI_XUAN)
                .gold(25)
                .expire(ConstTime.FOREVER)
                .build());
    }

    // todo: sử dụng item sự kiện
    @Override
    public void useItem(Char p, Item item) {
        switch (item.id) {
            case ItemName.BANH_CHUNG:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem);
                    p.getEventPoint().addPoint(LunarNewYear.TOP_MAKE_CHUNG_CAKE, 1);
                break;
            case ItemName.BANH_TET:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromCoinItem);
                p.getEventPoint().addPoint(LunarNewYear.TOP_BANH_TET, 1);
                break;
            case ItemName.BUA_MAY_MAN:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                Npc npc = p.zone.getNpc(NpcName.HOA_MAI);
                if (npc == null) {
                    p.serverMessage("Đứng dưới cây mai để sử dụng");
                    return;
                }
                int distance = NinjaUtils.getDistance(npc.cx, npc.cy, p.x, p.y);
                if (distance > 100) {
                    p.serverMessage("Đứng dưới cây mai để sử dụng");
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem);
                p.getEventPoint().addPoint(LunarNewYear.TOP_LUCKY_CHARM, 1);
                p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, 1);
                break;

            case ItemName.PHAO_HOA:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem_1);
                p.getEventPoint().addPoint(LunarNewYear.TOP_LUCKY_CHARM_1, 1);
                p.zone.getService().addEffectAuto((byte) 0, (short) p.x, p.y, (byte) 0, (short) 5);
                break;

            case ItemName.BAO_LI_XI_LON:
            case ItemName.HOP_QUA_NOEL:
                if (p.getSlotNull() == 0) {
                    p.warningBagFull();
                    return;
                }
                useEventItem(p, item.id, itemsRecFromGoldItem);
                break;

            case ItemName.VUI_XUAN:
                p.getEventPoint().addPoint(LunarNewYear.MYSTERY_BOX_LEFT, 1);
                p.serverMessage(
                        "Số lần vui xuân hiện tại: " + p.getEventPoint().getPoint(LunarNewYear.MYSTERY_BOX_LEFT));
                p.removeItem(item.index, 1, true);
                break;
        }
    }

    // TODO: menu npc tiên nữ
    @Override
    public void menu(Char p) {
        p.menus.clear();
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm bánh", () -> {
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh chưng", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Bánh chưng", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, MAKE_CHUNG_CAKE, number);
                    } catch (NumberFormatException e) {
                        if (!input.isEmpty()) {
                            p.inputInvalid();
                        }
                    }
                }));
                p.getService().showInputDialog();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh tét", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Bánh tét", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, MAKE_TET_CAKE, number);
                    } catch (NumberFormatException e) {
                        if (!input.isEmpty()) {
                            p.inputInvalid();
                        }
                    }
                }));
                p.getService().showInputDialog();
            }));
            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm pháo hoa", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Làm pháo hoa", () -> {
                InputDialog input = p.getInput();
                try {
                    int number = input.intValue();
                    action(p, MAKE_FIREWORK, number);
                } catch (NumberFormatException e) {
                    if (!input.isEmpty()) {
                        p.inputInvalid();
                    }
                }
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Áo dài", () -> {
            exchangeAoDai(p);
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi lồng đèn", () -> {
            p.setCommandBox(Char.DOI_LONG_DEN_LUONG);
            List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP, ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
            p.getService().openUIShopTrungThu(list, "Đổi lồng đèn 500 lượng", "Đổi");
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Lì xì", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Tên người nhận", () -> {
                InputDialog input = p.getInput();
                luckyMoney(p, input.getText());
            }));
            p.getService().showInputDialog();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Quà ngày tết", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Lam sơn dạ", () -> {
                makePreciousTree(p, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Trúc bạch thiên lữ", () -> {
                makePreciousTree(p, 2);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Điểm sự kiện", () -> {
                p.getService().showAlert("Hướng dẫn", "- Điểm sự kiện: "
                        + NinjaUtils.getCurrency(p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI))
                        + "\n\nBạn có thể quy đổi điểm sự kiện như sau\n- Lam sơn dạ: 5.000 điểm\n- Trúc bạch thiên lữ: 20.000 điểm\n");
            }));
            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đua TOP", () -> {
            p.menus.clear();

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bùa may mắn", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_LUCKY_CHARM, "Bùa may mắn", "%d. %s ");
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                if (isEnded()) {
                    int ranking = getRanking(p, TOP_LUCKY_CHARM);
                    if (ranking <= 10 && p.getEventPoint().getRewarded(TOP_LUCKY_CHARM) == 0) {
                        p.menus.add(new Menu(CMDMenu.EXECUTE, String.format("Nhận Thưởng TOP %d", ranking), () -> {
                            receiveReward(p, TOP_LUCKY_CHARM);
                        }));
                    }
                }
                p.getService().openUIMenu();
            }));

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh chưng", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_MAKE_CHUNG_CAKE, "Làm bánh chưng", "%d. %s ");
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                if (isEnded()) {
                    int ranking = getRanking(p, TOP_MAKE_CHUNG_CAKE);
                    if (ranking <= 10 && p.getEventPoint().getRewarded(TOP_MAKE_CHUNG_CAKE) == 0) {
                        p.menus.add(new Menu(CMDMenu.EXECUTE, String.format("Nhận Thưởng TOP %d", ranking), () -> {
                            receiveReward(p, TOP_MAKE_CHUNG_CAKE);
                        }));
                    }
                }
                p.getService().openUIMenu();
            }));

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh tet", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_BANH_TET, "Bánh tet", "%d. %s ");
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                if (isEnded()) {
                    int ranking = getRanking(p, TOP_MAKE_CHUNG_CAKE);
                    if (ranking <= 10 && p.getEventPoint().getRewarded(TOP_MAKE_CHUNG_CAKE) == 0) {
                        p.menus.add(new Menu(CMDMenu.EXECUTE, String.format("Nhận Thưởng TOP %d", ranking), () -> {
                            receiveReward(p, TOP_MAKE_CHUNG_CAKE);
                        }));
                    }
                }
                p.getService().openUIMenu();
            }));

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Pháo hoa", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    viewTop(p, TOP_LUCKY_CHARM_1, "Pháo hoa", "%d. %s ");
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");
                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                if (isEnded()) {
                    int ranking = getRanking(p, TOP_MAKE_CHUNG_CAKE);
                    if (ranking <= 10 && p.getEventPoint().getRewarded(TOP_MAKE_CHUNG_CAKE) == 0) {
                        p.menus.add(new Menu(CMDMenu.EXECUTE, String.format("Nhận Thưởng TOP %d", ranking), () -> {
                            receiveReward(p, TOP_MAKE_CHUNG_CAKE);
                        }));
                    }
                }
                p.getService().openUIMenu();
            }));

            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Hướng dẫn", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("- Số bùa hái lộc : ")
                    .append(NinjaUtils.getCurrency(p.getEventPoint().getPoint(TOP_LUCKY_CHARM))).append("\n");
            sb.append("- Số bánh chưng đã làm : ")
                    .append(NinjaUtils.getCurrency(p.getEventPoint().getPoint(TOP_MAKE_CHUNG_CAKE))).append("\n");
            sb.append("===CÔNG THỨC===").append("\n\n");
            sb.append("......");
            p.getService().showAlert("Hướng Dẫn", sb.toString());
        }));

    }

    // TODO: các menu phụ
    @Override
    public void action(Char p, int type, int amount) {
        if (isEnded()) {
            p.serverMessage("Sự kiện đã kết thúc");
            return;
        }
        switch (type) {
            case MAKE_CHUNG_CAKE:
                makeChungCake(p, amount);
                break;
            case MAKE_TET_CAKE:
                makeTetCake(p, amount);
                break;

            case MAKE_FIREWORK:
                makeFirework(p, amount);
                break;
        }
    }

    private void makeChungCake(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.NEP, 1}, {ItemName.LA_DONG, 1}, {ItemName.DAU_XANH2, 1},
                {ItemName.LAT_TRE, 1}, {ItemName.THIT_HEO, 1}};
        int itemIdReceive = ItemName.BANH_CHUNG;
        boolean isDone = makeEventItem(p, amount, itemRequires, 0, 0, 0, itemIdReceive);
        if (isDone) {
            p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, 1);
        }
    }

    private void makeTetCake(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.NEP, 1}, {ItemName.LA_DONG, 1}, {ItemName.DAU_XANH2, 1},
                {ItemName.LAT_TRE, 1}};
        int itemIdReceive = ItemName.BANH_TET;
        boolean isDone = makeEventItem(p, amount, itemRequires, 0, 90000, 0, itemIdReceive);
        if (isDone) {
        p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, 1);
        }
    }

    private void makeFirework(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.MANH_PHAO_HOA, 10}};
        int itemIdReceive = ItemName.PHAO_HOA;
        boolean isDone = makeEventItem(p, amount, itemRequires, 20, 0, 0, itemIdReceive);
        if (isDone) {
            p.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, amount);
        }
    }

    private void exchangeAoDai(Char p) {
        int indexTetCake = p.getIndexItemByIdInBag(ItemName.BANH_TET);
        if (indexTetCake == -1 || p.bag[indexTetCake] == null || p.bag[indexTetCake].getQuantity() < 10) {
            p.getService().npcChat(NpcName.TIEN_NU, "Bạn cần có đủ 10 chiếc bánh tét ");
            return;
        }
        if (p.getSlotNull() == 0) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return;
        }
        if (p.user.gold < 500) {
            p.getService().npcChat(NpcName.TIEN_NU, "Bạn phải có đủ 500 lượng.");
            return;
        }

        int dressId = p.gender == 1 ? ItemName.AO_NGU_THAN : ItemName.AO_TAN_THOI;

        p.removeItem(indexTetCake, 10, true);
        p.addGold(-500);
        Item item = ItemFactory.getInstance().newItem(dressId);
        item.isLock = false;
        item.expire = System.currentTimeMillis() + 1296000000L;

        item.randomOptionTigerMask();

        p.addItemToBag(item);
    }

    private void luckyMoney(Char _char, String name) {
        if (_char.level < 20) {
            _char.getService().npcChat(NpcName.TIEN_NU, "Bạn cần đạt cấp 20");
            return;
        }

        if (name.equals("")) {
            _char.getService().npcChat(NpcName.TIEN_NU, "Người này không online hoặc không tồn tại!");
            return;
        }

        Char receiver = Char.findCharByName(name);

        if (receiver == null) {
            _char.serverMessage("Người này không online hoặc không tồn tại!");
            return;
        }

        if (_char == receiver) {
            _char.serverMessage("Bạn không thể lì xì cho chính bạn!");
            return;
        }

        if (receiver.level < 20) {
            _char.serverMessage("Đối phương cần đạt level 20!");
            return;
        }

        if (_char.user.gold < 20) {
            _char.serverMessage("Bạn cần tối thiểu 20 lượng");
            return;
        }

        if (_char.getSlotNull() == 0) {
            _char.warningBagFull();
            return;
        }

        boolean isDone = useEventItem(_char, 1, 20, 0, itemsRecFromGold2Item);
        if (isDone) {
            int yen = NinjaUtils.nextInt(50000, 200000);
            receiver.addYen(yen);
            receiver.serverMessage("Bạn được " + _char.name + " lì xì " + NinjaUtils.getCurrency(yen) + " yên");
        }
    }

    public void makePreciousTree(Char p, int type) {
        int point = type == 1 ? 5000 : 20000;
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

        Item item = ItemFactory.getInstance().newItem(type == 1 ? ItemName.LAM_SON_DA : ItemName.TRUC_BACH_THIEN_LU);
        p.addItemToBag(item);
        p.getEventPoint().subPoint(EventPoint.DIEM_TIEU_XAI, point);
    }
    class BotInfo {

        int id;
        int mapId;
        String name;
        int head;
        int body;
        int leg;

        public BotInfo(int mapId, String name, int head, int body, int leg) {
            this.id = -NinjaUtils.nextInt(100000, 200000);
            this.mapId = mapId;
            this.name = name;
            this.head = head;
            this.body = body;
            this.leg = leg;
        }

        public Bot toBot(Npc npc) {
            Bot bot = new Principal(id, name, head, body, leg);
            bot.setDefault();
            bot.recovery();
            bot.setXY((short) npc.cx, (short) npc.cy);
            bot.setMove(new PrincipalMove(npc));
            return bot;
        }

    }

    public void spawnPrincipal() {
        List<BotInfo> botInfoList = new ArrayList<>();
        botInfoList.add(new BotInfo(MapName.TRUONG_HIROSAKI, "Cô Toyotomi", 44, 45, 46));
        botInfoList.add(new BotInfo(MapName.TRUONG_OOKAZA, "Thầy Ookamesama", 53, 54, 55));
        botInfoList.add(new BotInfo(MapName.TRUONG_HARUNA, "Thầy Kazeto", 65, 66, 67));

        for (BotInfo info : botInfoList) {
            Map map = MapManager.getInstance().find(info.mapId);
            Zone z = map.rand();
            Npc npc = z.getNpc(NpcName.HOA_MAI);
            if (npc != null) {
                Bot bot = info.toBot(npc);
                GlobalService.getInstance().chat(bot.name,
                        "Chúc mừng năm mới, các con hãy tới gốc cây mai tại các trường để nhận quá nhé");
                z.join(bot);
            }
        }
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

        if (key == TOP_LUCKY_CHARM) {
            topDecorationGiftBox(ranking, p);
        } else if (key == TOP_MAKE_CHUNG_CAKE) {
            topMakeChungCake(ranking, p);
        }
        p.getEventPoint().setRewarded(key, 1);
    }

    public void topDecorationGiftBox(int ranking, Char p) {
        Item mount = ItemFactory.getInstance().newItem(ItemName.HOA_KY_LAN);
        int dressId = p.gender == 1 ? ItemName.AO_NGU_THAN : ItemName.AO_TAN_THOI;
        Item aoDai = ItemFactory.getInstance().newItem(dressId);
        Item tree = ItemFactory.getInstance().newItem(ItemName.TRUC_BACH_THIEN_LU);
        if (ranking == 1) {
            mount.options.add(new ItemOption(ItemOptionName.NE_DON_ADD_POINT_TYPE_1, 200));
            mount.options.add(new ItemOption(ItemOptionName.CHINH_XAC_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.TAN_CONG_KHI_DANH_CHI_MANG_POINT_PERCENT_TYPE_1, 100));
            mount.options.add(new ItemOption(ItemOptionName.CHI_MANG_ADD_POINT_TYPE_1, 100));
            mount.options.add(new ItemOption(58, 10));
            mount.options.add(new ItemOption(128, 10));
            mount.options.add(new ItemOption(127, 10));
            mount.options.add(new ItemOption(130, 10));
            mount.options.add(new ItemOption(131, 10));

            aoDai.options.add(new ItemOption(125, 3000));
            aoDai.options.add(new ItemOption(117, 3000));
            aoDai.options.add(new ItemOption(94, 10));
            aoDai.options.add(new ItemOption(136, 30));
            aoDai.options.add(new ItemOption(127, 10));
            aoDai.options.add(new ItemOption(130, 10));
            aoDai.options.add(new ItemOption(131, 10));

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
            aoDai.expire = System.currentTimeMillis() + ConstTime.DAY * 90L;
            tree.setQuantity(3);
            p.addItemToBag(tree);
            for (int i = 0; i < 2; i++) {
                Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
                p.addItemToBag(blueChest);
            }
        } else {
            mount.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            aoDai.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
            p.addItemToBag(blueChest);
        }

        p.addItemToBag(mount);
        p.addItemToBag(aoDai);
    }

    public void topMakeChungCake(int ranking, Char p) {
        Item pet = ItemFactory.getInstance().newItem(ItemName.PET_UNG_LONG);
        int tickId = p.gender == 1 ? ItemName.GAY_MAT_TRANG : ItemName.GAY_TRAI_TIM;
        Item fashionStick = ItemFactory.getInstance().newItem(tickId);
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
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 90L;
            tree.setQuantity(3);
            p.addItemToBag(tree);
            for (int i = 0; i < 2; i++) {
                Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
                p.addItemToBag(blueChest);
            }
        } else {
            pet.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            fashionStick.expire = System.currentTimeMillis() + ConstTime.DAY * 30L;
            Item blueChest = ItemFactory.getInstance().newItem(ItemName.RUONG_BACH_NGAN);
            p.addItemToBag(blueChest);
        }

        p.addItemToBag(pet);
        p.addItemToBag(fashionStick);
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
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 1426, 552, 0));
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_DAO).x((short) 1788).y((short) 672).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_DAO).x((short) 784).y((short) 648).build());
                break;
            case MapName.TRUONG_HARUNA:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 502, 408, 0));
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_MAI).x((short) 227).y((short) 408).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_DAO).x((short) 2398).y((short) 288).build());

                break;
            case MapName.TRUONG_HIROSAKI:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 1207, 168, 0));
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_MAI).x((short) 502).y((short) 408).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_DAO).x((short) 766).y((short) 384).build());
                break;

            case MapName.LANG_TONE:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 1427, 264, 0));
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_MAI).x((short) 1821).y((short) 264).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_DAO).x((short) 2153).y((short) 192).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_DAO).x((short) 459).y((short) 216).build());
                break;

            case MapName.LANG_KOJIN:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 621, 288, 0));
                break;

            case MapName.LANG_CHAI:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 1804, 384, 0));
                break;

            case MapName.LANG_SANZU:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 320, 288, 0));
                break;

            case MapName.LANG_CHAKUMI:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 626, 312, 0));
                break;

            case MapName.LANG_ECHIGO:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 360, 360, 0));
                break;

            case MapName.LANG_OSHIN:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 921, 408, 0));
                break;

            case MapName.LANG_SHIIBA:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 583, 408, 0));
                break;

            case MapName.LANG_FEARRI:
                zone.addNpc(NpcFactory.getInstance().newNpc(99, NpcName.HOA_MAI, 611, 312, 0));
                break;

            case MapName.CANH_DONG_FUKI:
                Mob monster = new Mob(zone.getMonsters().size(), (short) MobName.HOP_BI_AN, 20200, (byte) 10,
                        (short) 3355, (short) 240, false, true, zone);
                zone.addMob(monster);
                break;

            case MapName.RUNG_DAO_SAKURA:
                if (zone.id == 15) {
                    monster = new Mob(zone.getMonsters().size(), (short) MobName.CHUOT_CANH_TY, 1000000000, (byte) 100,
                            (short) 1928, (short) 240, false, true, zone);
                    zone.addMob(monster);
                }
                break;
        }
    }
}
