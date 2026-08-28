package com.nsoz.event;

import com.nsoz.constants.*;
import com.nsoz.effect.EffectAutoDataManager;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.item.ItemManager;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.Map;
import com.nsoz.map.Tree;
import com.nsoz.map.zones.Zone;
import com.nsoz.mob.Mob;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.model.Menu;
import com.nsoz.model.RandomItem;
import com.nsoz.npc.NpcFactory;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;
import com.nsoz.util.NinjaUtils;

import java.util.List;

public class TrungThuNew extends Event {
    public static final int HOA_PHUC_SINH = 9;
    public static final String TOP_LONG_DEN = "release_lanterns";
    public static final String TOP_BANH_TRUNG_THU = "use_moon_cake";
    public static final String TOP_BANH_TRUNG_THU_THUONG = "use_moon_cake_nomal";
    public static final long EXPIRE_7_DAY = 604800000L;
    public static final long EXPIRE_30_DAY = 2592000000L;
    private static final int DOI_BACH_HO = 0;
    private static final int VU_KHI_THOI_TRANG_7_NGAY = 1;
    private static final int VU_KHI_THOI_TRANG_30_NGAY = 2;
    private static final int QUA_DAC_BIET = 9;
    private static final int BANH_THAP_CAM = 3;
    private static final int BANH_DEO = 4;
    private static final int BANH_DAU_XANH = 5;
    private static final int BANH_PIA = 6;
    private static final int HOP_BANH_THUONG = 7;
    private static final int HOP_BANH_THUONG_HANG = 8;

    public TrungThuNew() {
        setId(Event.TRUNG_THU);
        keyEventPoint.add(EventPoint.DIEM_TIEU_XAI);
        keyEventPoint.add(TOP_BANH_TRUNG_THU);
        keyEventPoint.add(TOP_BANH_TRUNG_THU_THUONG);
        keyEventPoint.add(TOP_LONG_DEN);

        // TODO: rơi nlsk khi đánh quái, thêm càng nhiều dòng nó ra càng nhiều
        endTime.set(2028, 12, 12, 23, 59, 59); // TODO: time chạy sự kiện, cứ để nó dài ra, mình tắt sk ở file config
        itemsThrownFromMonsters.add(100, ItemName.TRUNG);
        itemsThrownFromMonsters.add(100, ItemName.BOT_MI);
        itemsThrownFromMonsters.add(100, ItemName.HAT_SEN);
        itemsThrownFromMonsters.add(100, ItemName.DUONG);
        itemsThrownFromMonsters.add(100, ItemName.DAU_XANH);
        itemsThrownFromMonsters.add(100, ItemName.MUT);
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
        itemsRecFromGoldItem.add(5, ItemName.BACH_NGAN_LANG); // thêm pet mới
        itemsRecFromGoldItem.add(5, ItemName.BACH_SU_VUONG);


    }

    // TODO: cửa hàng goosho
    @Override
    public void initStore() {
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(998)
                .itemID(ItemName.GIAY_GOI_THUONG)
                .coin(60000)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(999)
                .itemID(ItemName.GIAY_GOI_CAO_CAP)
                .gold(25)
                .expire(ConstTime.FOREVER)
                .build());
        StoreManager.getInstance().addItem((byte) StoreManager.TYPE_MISCELLANEOUS, ItemStore.builder()
                .id(665)
                .itemID(ItemName.LONG_DEN)
                .coin(100000)
                .expire(ConstTime.FOREVER)
                .build());
    }

    // todo: sử dụng item sự kiện
    @Override
    public void useItem(Char _char, Item item) {
        if (item.id == ItemName.HOP_BANH_THUONG) {
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }
            useEventItem(_char, item.id, itemsRecFromCoinItem);
            _char.getEventPoint().addPoint(TrungThuNew.TOP_BANH_TRUNG_THU_THUONG, 1);
            _char.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, 1);
        } else if (item.id == ItemName.HOP_BANH_THUONG_HANG) {
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }
            useEventItem(_char, item.id, itemsRecFromGoldItem);
            _char.getEventPoint().addPoint(TrungThuNew.TOP_BANH_TRUNG_THU, 1);
            _char.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, 1);

        } else if (item.id == ItemName.LONG_DEN) {  // k ra item
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }
            useEventItem(_char, item.id, itemsRecFromGold2Item);
            _char.getEventPoint().addPoint(TrungThuNew.TOP_LONG_DEN, 1);
            _char.getEventPoint().addPoint(EventPoint.DIEM_TIEU_XAI, 1);
            _char.zone.getService().addEffectAuto((byte) 7, (short) _char.x, _char.y, (byte) 0, (short) 1);
        }
    }

    // TODO: menu npc tiên nữ
    @Override
    public void menu(Char p) {
        p.menus.clear();
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm bánh", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh thập cẩm", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Bánh thập cẩm", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, BANH_THAP_CAM, number);
                    } catch (Exception e) {
                        if (!input.isEmpty()) {
                            p.inputInvalid();
                        }
                    }
                }));
                p.getService().showInputDialog();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh dẻo", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Bánh dẻo", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, BANH_DEO, number);
                    } catch (Exception e) {
                        if (!input.isEmpty()) {
                            p.inputInvalid();
                        }
                    }
                }));
                p.getService().showInputDialog();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh đậu xanh", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Bánh đậu xanh", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, BANH_DAU_XANH, number);
                    } catch (Exception e) {
                        if (!input.isEmpty()) {
                            p.inputInvalid();
                        }
                    }
                }));
                p.getService().showInputDialog();

            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bánh pía", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Bánh pía", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, BANH_PIA, number);
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

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm hộp bánh", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Hộp bánh thường", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Hộp bánh thường", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, HOP_BANH_THUONG, number);
                    } catch (Exception e) {
                        if (!input.isEmpty()) {
                            p.inputInvalid();
                        }
                    }
                }));
                p.getService().showInputDialog();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Hộp bánh thượng hạng", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Hộp bánh thượng hạng", () -> {
                    InputDialog input = p.getInput();
                    try {
                        int number = input.intValue();
                        action(p, HOP_BANH_THUONG_HANG, number);
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

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Gửi bánh", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Bạch hổ 30 ngày", () -> {
                action(p, DOI_BACH_HO, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Vũ khí thời trang 7 ngày", () -> {
                action(p, VU_KHI_THOI_TRANG_7_NGAY, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Vũ khí thời trang 30 ngày", () -> {
                action(p, VU_KHI_THOI_TRANG_30_NGAY, 1);
            }));
            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đổi lồng đèn", () -> {
            p.setCommandBox(Char.DOI_LONG_DEN_LUONG);
            List<Item> list = p.getListItemByID(ItemName.LONG_DEN_TRON, ItemName.LONG_DEN_CA_CHEP, ItemName.LONG_DEN_MAT_TRANG, ItemName.LONG_DEN_NGOI_SAO);
            p.getService().openUIShopTrungThu(list, "Đổi lồng đèn 500 lượng", "Đổi");
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Hoa phục sinh", () -> {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Hoa thiên diệu", () -> {
                doiHoaPhucSinh(p, 1);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Hoa dạ yến", () -> {
                doiHoaPhucSinh(p, 2);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Điểm tiêu sài", () -> {
                p.getService().showAlert("Hướng dẫn", "- Điểm sự kiện: " + NinjaUtils.getCurrency(p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI))
                        + "\n\nBạn có thể quy đổi điểm sự kiện như sau\n- Hoa thiên diệu: 10.000 điểm\n- Hoa dạ yến: 10.000 điểm\n");
            }));
            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đua Top", () -> {
            p.menus.clear();

            p.menus.add(new Menu(CMDMenu.EXECUTE, "Thả Lồng Đèn", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    if (p.user.isTien()) {
                        viewTop(p, TOP_LONG_DEN, "Thả Lồng Đèn", "%d. %s đã thả %s lồng đèn");
                    } else {
                        viewTop(p, TOP_LONG_DEN, "Thả Lồng Đèn", "%d. %s");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("......");


                }));
                p.getService().openUIMenu();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Hộp bánh thường", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    if (p.user.isTien()) {
                        viewTop(p, TOP_BANH_TRUNG_THU_THUONG, "Hộp bánh thường", "%d. %s đã làm %s hộp bánh");
                    } else {
                        viewTop(p, TOP_BANH_TRUNG_THU_THUONG, "Hộp bánh thường", "%d. %s");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(".....");

                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                p.getService().openUIMenu();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Hộp bánh thượng hạng", () -> {
                p.menus.clear();
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảng xếp hạng", () -> {
                    if (p.user.isTien()) {
                        viewTop(p, TOP_BANH_TRUNG_THU, "Hộp bánh thượng hạng", "%d. %s đã làm %s hộp bánh");
                    } else {
                        viewTop(p, TOP_BANH_TRUNG_THU, "Hộp bánh thượng hạng", "%d. %s");
                    }
                }));
                p.menus.add(new Menu(CMDMenu.EXECUTE, "Phần thưởng", () -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("......");

                    p.getService().showAlert("Phần thưởng", sb.toString());
                }));
                p.getService().openUIMenu();
            }));
            p.getService().openUIMenu();
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Hướng dẫn", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("☑\uFE0F Bánh Thập Cẩm = 3 Bột + 2 Trứng + 2 Hạt sen + 2 Đường + 2 Mứt + yên\n" +
                    "\n" +
                    "☑\uFE0F Bánh Dẻo = 3 Bột + 2 Hạt sen + 2 Đường + 2 Mứt + yên\n" +
                    "\n" +
                    "☑\uFE0F Bánh Đậu xanh = 3 Bột + 2 Trứng + 2 Đường + 2 Đậu xanh + yên\n" +
                    "\n" +
                    "☑\uFE0F Bánh Pía = 3 Bột + 2 Trứng + 2 Đường + 2 Mứt + yên\n" +
                    "\n" +
                    " \n" +
                    "\n" +
                    "☑\uFE0F Hộp bánh thường = 4 loại bánh + 1 giấy gói thường. Có thể giao dịch\n" +
                    "\n" +
                    "☑\uFE0F Hộp bánh thượng hạng = 4 loại bánh + 1 giấy gói cao cấp. Có thể giao dịch");


            p.getService().showAlert("Hướng dẫn", sb.toString());
        }));
    }

    // TODO: các menu phụ
    @Override
    public void action(Char p, int type, int amount) {
        switch (type) {
            case BANH_THAP_CAM:
                banhThapCam(p, amount);
                break;
            case BANH_DAU_XANH:
                banhDauXanh(p, amount);
                break;
            case BANH_DEO:
                banhDeo(p, amount);
                break;
            case BANH_PIA:
                banhPia(p, amount);
                break;
            case HOP_BANH_THUONG:
                hopBanhThuong(p, amount);
                break;
            case HOP_BANH_THUONG_HANG:
                hopBanhThuongHang(p, amount);
                break;
            case HOA_PHUC_SINH:
                hoaPhucSinh(p, amount);
                break;
            case DOI_BACH_HO:
                doiBachHo(p);
                break;
            case VU_KHI_THOI_TRANG_7_NGAY:
                doiVuKhiThoiTrang(p, ItemName.BANH_TRUNG_THU_PHONG_LOI, 5, EXPIRE_7_DAY);
                break;
            case VU_KHI_THOI_TRANG_30_NGAY:
                doiVuKhiThoiTrang(p, ItemName.BANH_TRUNG_THU_BANG_HOA, 10, EXPIRE_30_DAY);
                break;
        }
    }

    public void doiBachHo(Char p) {
        int amount = 5;
        List<Item> list = p.getListItemByID(ItemName.BANH_TRUNG_THU_PHONG_LOI);
        if (list.size() < amount) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ " + amount + "Bánh trung thu phong lôi");
            return;
        }

        for (Item item : list.subList(0, amount)) {
            p.removeItem(item.index, 1, true);
        }
        Item item = ItemFactory.getInstance().newItem(ItemName.BACH_HO);
        item.setQuantity(1);
        item.isLock = false;
        item.expire = System.currentTimeMillis() + EXPIRE_30_DAY;
        p.addItemToBag(item);
    }

    public void doiVuKhiThoiTrang(Char p, int itemID, int amount, long expire) {
        List<Item> list = p.getListItemByID(itemID);
        if (list.size() < amount) {
            p.getService().npcChat(NpcName.TIEN_NU, "Không đủ " + amount +  ItemManager.getInstance().getItemName(itemID));
            return;
        }

        for (Item item : list.subList(0, amount)) {
            p.removeItem(item.index, 1, true);
        }

        if (p.gender == 1) {
            itemID = ItemName.GAY_MAT_TRANG;
        } else {
            itemID = ItemName.GAY_TRAI_TIM;
        }
        Item item = ItemFactory.getInstance().newItem(itemID);
        item.setQuantity(1);
        item.isLock = false;
        if (expire == -1) {
            item.expire = -1;
        } else {
            item.expire = System.currentTimeMillis() + expire;
        }
        p.addItemToBag(item);
    }

    public void banhThapCam(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.BOT_MI, 3}, {ItemName.TRUNG, 2}, {ItemName.DUONG, 2}, {ItemName.HAT_SEN, 2}, {ItemName.MUT, 2}};
        int itemIdReceive = ItemName.BANH_THAP_CAM;
        makeEventItem(p, amount, itemRequires, 0, 0, 15000, itemIdReceive);
    }

    public void banhDeo(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.BOT_MI, 3}, {ItemName.HAT_SEN, 2}, {ItemName.DUONG, 2}, {ItemName.MUT, 2}};
        int itemIdReceive = ItemName.BANH_DEO;
        makeEventItem(p, amount, itemRequires, 0, 0, 15000, itemIdReceive);
    }

    public void banhDauXanh(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.BOT_MI, 3}, {ItemName.TRUNG, 2}, {ItemName.DUONG, 2}, {ItemName.DAU_XANH, 2}};
        int itemIdReceive = ItemName.BANH_DAU_XANH;
        makeEventItem(p, amount, itemRequires, 0, 0, 15000, itemIdReceive);
    }

    public void banhPia(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.BOT_MI, 3}, {ItemName.TRUNG, 2}, {ItemName.DUONG, 2}, {ItemName.MUT, 2}};
        int itemIdReceive = ItemName.BANH_PIA;
        makeEventItem(p, amount, itemRequires, 0, 0, 15000, itemIdReceive);
    }

    public void hopBanhThuong(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.GIAY_GOI_THUONG, 1}, {ItemName.BANH_THAP_CAM, 1}, {ItemName.BANH_DEO, 1}, {ItemName.BANH_DAU_XANH, 1}, {ItemName.BANH_PIA, 1}};
        int itemIdReceive = ItemName.HOP_BANH_THUONG;
        makeEventItem(p, amount, itemRequires, 0, 0, 0, itemIdReceive);
    }

    public void hopBanhThuongHang(Char p, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.GIAY_GOI_CAO_CAP, 1}, {ItemName.BANH_THAP_CAM, 1}, {ItemName.BANH_DEO, 1}, {ItemName.BANH_DAU_XANH, 1}, {ItemName.BANH_PIA, 1}};
        int itemIdReceive = ItemName.HOP_BANH_THUONG_HANG;
        makeEventItem(p, amount, itemRequires, 0, 0, 0, itemIdReceive);
    }

    public void doiHoaPhucSinh(Char p, int type) {
        int point = type == 1 ? 10000 : 10000;
        if (p.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI) < point) {
            p.getService().npcChat(NpcName.TIEN_NU,
                    "Bạn cần tối thiểu " + NinjaUtils.getCurrency(point) + " điểm sự kiện mới có thể đổi được vật này.");
            return;
        }

        if (p.getSlotNull() == 0) {
            p.getService().npcChat(NpcName.TIEN_NU, p.language.getString("BAG_FULL"));
            return;
        }

        Item item = ItemFactory.getInstance().newItem(type == 1 ? ItemName.HOA_THIEN_DIEU : ItemName.HOA_DA_YEN);
        p.addItemToBag(item);
        p.getEventPoint().subPoint(EventPoint.DIEM_TIEU_XAI, point);
    }

    public void hoaPhucSinh(Char _char, int itemId) {
        if (_char.getSlotNull() == 0) {
            _char.warningBagFull();
            return;
        }

        int itemIndex = _char.getIndexItemByIdInBag(itemId);

        if (itemIndex != -1) {
            RandomCollection<Integer> rc = RandomItem.LINH_VAT;
            useVipEventItem(_char, itemId == ItemName.HOA_THIEN_DIEU ? 1 : 2, rc);
            _char.removeItem(itemIndex, 1, true);
        } else {
            _char.getService().npcChat((short) NpcName.KIRIKO, "Hãy tìm đúng loài hoa rồi đến gặp ta");
        }
    }

    public void escortFinish(Char p) {
        RandomCollection<Integer> rc = itemsRecFromGold2Item;
        p.addExp(15000000);
        int itemId = rc.next();
        Item itm = ItemFactory.getInstance().newItem(itemId);
        itm.initExpire();
        if (itm.id == ItemName.THONG_LINH_THAO) {
            itm.setQuantity(NinjaUtils.nextInt(5, 10));
        }
        p.addItemToBag(itm);
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
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 1426).y((short) 552).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU_2).x((short) 784).y((short) 648).build());
                break;
            case MapName.TRUONG_HARUNA:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 502).y((short) 408).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU_2).x((short) 1863).y((short) 360).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.THA_LONG_DEN).x((short) 2614).y((short) 384).build());
                break;
            case MapName.TRUONG_HIROSAKI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 1207).y((short) 168).build());
                break;

            case MapName.LANG_TONE:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 1427).y((short) 264).build());
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU_2).x((short) 472).y((short) 216).build());
                break;

            case MapName.LANG_KOJIN:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 621).y((short) 288).build());
                break;

            case MapName.LANG_CHAI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 1804).y((short) 384).build());
                break;

            case MapName.LANG_SANZU:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 320).y((short) 288).build());
                break;

            case MapName.LANG_CHAKUMI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 626).y((short) 312).build());
                break;

            case MapName.LANG_ECHIGO:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 360).y((short) 360).build());
                break;

            case MapName.LANG_OSHIN:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 921).y((short) 408).build());
                break;

            case MapName.LANG_SHIIBA:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 583).y((short) 408).build());
                break;

            case MapName.LANG_FEARRI:
                zone.addTree(Tree.builder().id(EffectAutoDataManager.CAY_TRUNG_THU).x((short) 611).y((short) 312).build());
                break;

        }
    }
}
