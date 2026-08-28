package com.nsoz.event;

import com.nsoz.constants.CMDInputDialog;
import com.nsoz.constants.CMDMenu;
import com.nsoz.constants.ItemName;
import com.nsoz.constants.NpcName;
import com.nsoz.event.eventpoint.EventPoint;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.zones.Zone;
import com.nsoz.mob.Mob;
import com.nsoz.mob.MobManager;
import com.nsoz.mob.MobTemplate;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.model.Menu;
import com.nsoz.model.RandomItem;
import com.nsoz.npc.Npc;
import com.nsoz.util.NinjaUtils;

public class KoroKing extends Event {

    public static final int TOP_LUCKY_START = 0;

    public KoroKing() {
        setId(Event.KOROKING);

        // TODO: rơi nlsk khi đánh quái, thêm càng nhiều dòng nó ra càng nhiều
        itemsThrownFromMonsters.add(3, ItemName.TO_DIEP);
        itemsThrownFromMonsters.add(3, ItemName.NGU_TINH_THAO);
        itemsThrownFromMonsters.add(1, ItemName.CAY_KEO_MUT);
        itemsThrownFromMonsters.add(1, ItemName.HOP_BANH_NGOT);
        itemsThrownFromMonsters.add(1, ItemName.QUA_BONG_BONG);
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
    }

    // todo: sử dụng item sự kiện
    @Override
    public void useItem(Char _char, Item item) {
        if (item.id == ItemName.TINH_DAU_TO_DIEP || item.id == ItemName.TINH_DAU_NGU_THAO
                || item.id == ItemName.NGOI_SAO_NHO || item.id == ItemName.NGOI_SAO_MAY_MAN) {
            if (_char.getSlotNull() == 0) {
                _char.warningBagFull();
                return;
            }

            if (item.id > ItemName.TINH_DAU_TO_DIEP) {
                Npc npc = _char.zone.getNpc(NpcName.KIRIKO);

                if (npc == null) {
                    _char.serverMessage("Vui tìm Kiriko để giao vật phẩm này.");
                    return;
                }

                int distance = NinjaUtils.getDistance(npc.cx, npc.cy, _char.x, _char.y);
                if (distance > 100) {
                    _char.serverMessage("Vui tìm Kiriko để giao vật phẩm này.");
                    return;
                }
            }

            RandomCollection<Integer> rc;
            if (item.id == ItemName.NGOI_SAO_MAY_MAN) {
                rc = itemsRecFromGold2Item;
            } else if (item.id == ItemName.TINH_DAU_TO_DIEP) {
                rc = itemsRecFromGoldItem;
            } else {
                rc = itemsRecFromCoinItem;
            }

            boolean isDone = useEventItem(_char, item.id, rc);
        }
    }

    // TODO: menu npc tiên nữ
    public void menu(Char _char) {
        _char.menus.add(new Menu(CMDMenu.EXECUTE, "Tinh dầu", () -> {
            _char.menus.clear();
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Ngư thảo", () -> {
                InputDialog input = new InputDialog(CMDInputDialog.NGU_THAO, "Tinh dầu ngư thảo");
                _char.setInput(input);
                _char.getService().showInputDialog();
            }));
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Tô diệp", () -> {
                InputDialog input = new InputDialog(CMDInputDialog.TO_DIEP, "Tinh dầu tô diệp");
                _char.setInput(input);
                _char.getService().showInputDialog();
            }));
            _char.getService().openUIMenu();
        }));

        _char.menus.add(new Menu(CMDMenu.EXECUTE, "Ngôi sao may mắn", () -> {
            _char.menus.clear();
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Ngôi sao nhỏ", () -> {
                InputDialog input = new InputDialog(CMDInputDialog.NGOI_SAO_NHO, "Ngôi sao nhỏ");
                _char.setInput(input);
                _char.getService().showInputDialog();
            }));
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Ngôi sao may mắn", () -> {
                InputDialog input = new InputDialog(CMDInputDialog.NGOI_SAO_MAY_MAN, "Ngôi sao may mắn");
                _char.setInput(input);
                _char.getService().showInputDialog();
            }));
            _char.getService().openUIMenu();
        }));

        _char.menus.add(new Menu(CMDMenu.EXECUTE, "Đua Top", () -> {
            _char.menus.clear();
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Ngôi sao may mắn", () -> {
                Ranking.showLeaderBoard(_char, TOP_LUCKY_START, "đã sử dụng %d ngôi sao may mắn");
            }));
            _char.getService().openUIMenu();
        }));

        _char.menus.add(new Menu(CMDMenu.EXECUTE, "Hoa phục sinh", () -> {
            _char.menus.clear();
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Hoa thiên diệu", () -> {
                doiHoaPhucSinh(_char, 1);
            }));
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Hoa dạ yến", () -> {
                doiHoaPhucSinh(_char, 2);
            }));
            _char.menus.add(new Menu(CMDMenu.EXECUTE, "Điểm sự kiện", () -> {
                _char.getService().showAlert("Hướng dẫn", "- Điểm sự kiện: "
                        + NinjaUtils.getCurrency(_char.getEventPoint().getPoint(EventPoint.DIEM_TIEU_XAI))
                        + "\n\nBạn có thể quy đổi điểm sự kiện như sau\n- Hoa thiên diệu: 5.000 điểm\n- Hoa dạ yến: 20.000 điểm\n");
            }));
            _char.getService().openUIMenu();
        }));
    }

    // TODO: các menu phụ
    public static void addTrophy(Char _char) {
        addTrophy(_char, 1);
    }

    public static void addTrophy(Char _char, int amount) {
        Item item = ItemFactory.getInstance().newItem(ItemName.HUY_HIEU);
        item.setQuantity(amount);
        _char.addItemToBag(item);
    }

    public void action(Char _char, int type, int amount) {
        switch (type) {
            case 1:
                nguThao(_char, amount);
                break;
            case 2:
                toDiep(_char, amount);
                break;
            case 3:
                ngoiSaoNho(_char, amount);
                break;
            case 4:
                ngoiSaoMayMan(_char, amount);
                break;
            case 5:
                hoaPhucSinh(_char, amount);
                break;
        }
    }

    public void nguThao(Char _char, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.NGU_TINH_THAO, 3}};
        int itemIdReceive = ItemName.TINH_DAU_NGU_THAO;
        makeEventItem(_char, amount, itemRequires, 0, 100000, 0, itemIdReceive);
    }

    public void toDiep(Char _char, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.TO_DIEP, 3}};
        int itemIdReceive = ItemName.TINH_DAU_TO_DIEP;
        makeEventItem(_char, amount, itemRequires, 20, 0, 0, itemIdReceive);
    }

    public void ngoiSaoNho(Char _char, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.HUY_HIEU, 1}, {ItemName.CAY_KEO_MUT, 1},
                {ItemName.HOP_BANH_NGOT, 1}, {ItemName.QUA_BONG_BONG, 1}};
        int itemIdReceive = ItemName.NGOI_SAO_NHO;
        makeEventItem(_char, amount, itemRequires, 0, 100000, 0, itemIdReceive);
    }

    public void ngoiSaoMayMan(Char _char, int amount) {
        int[][] itemRequires = new int[][]{{ItemName.HUY_HIEU, 1}, {ItemName.CAY_KEO_MUT, 1},
                {ItemName.HOP_BANH_NGOT, 1}, {ItemName.QUA_BONG_BONG, 1}};
        int itemIdReceive = ItemName.NGOI_SAO_MAY_MAN;
        makeEventItem(_char, amount, itemRequires, 20, 0, 0, itemIdReceive);
    }

    public void hoaPhucSinh(Char _char, int itemId) {
        if (_char.getSlotNull() == 0) {
            _char.warningBagFull();
            return;
        }

        int itemIndex = _char.getIndexItemByIdInBag(itemId);

        if (itemIndex != -1) {
            RandomCollection<Integer> rc = RandomItem.LINH_VAT;
            useVipEventItem(_char, 2, rc);
            _char.removeItem(itemIndex, 1, true);
        } else {
            _char.getService().npcChat((short) NpcName.KIRIKO, "Hãy tìm đúng loài hoa rồi đến gặp ta");
        }
    }

    public void doiHoaPhucSinh(Char p, int type) {
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

        Item item = ItemFactory.getInstance().newItem(type == 1 ? ItemName.HOA_THIEN_DIEU : ItemName.HOA_DA_YEN);
        p.addItemToBag(item);
        p.getEventPoint().subPoint(EventPoint.DIEM_TIEU_XAI, point);
    }

    public void bornKoroKing(Mob mob) {
        int incrementId = mob.zone.getMonsters().size();
        MobTemplate template = MobManager.getInstance().find(232);
        Mob monster = new Mob(incrementId++, (short) template.id, mob.maxHP, mob.level, mob.x, mob.y, false,
                template.isBoss(), mob.zone);
        mob.zone.addMob(monster);
    }

    public void infection(Char _char) {
        if (_char.fashion[11] == null || _char.fashion[11].id != ItemName.KHAU_TRANG) {
            _char.infection();
            _char.zone.getService().chat(_char.id, "Khụ khụ !!!");
            _char.serverMessage("Bạn đã bị dính Virus");
        }
    }

    @Override
    public EventPoint createEventPoint() {
        return null;
    }

    // TODO: hiệu ứng cây ở map
    @Override
    public void initMap(Zone zone) {
    }


}
