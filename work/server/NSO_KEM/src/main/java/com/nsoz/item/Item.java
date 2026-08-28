package com.nsoz.item;

import com.nsoz.constants.ItemName;
import com.nsoz.lib.ParseData;
import com.nsoz.option.ItemOption;
import com.nsoz.server.Config;
import com.nsoz.util.NinjaUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Vector;

public class Item {

    public static final int[] GIA_KHAM = {800000, 1600000, 2400000, 3200000, 4800000, 7200000, 10800000, 15600000, 20100000, 28100000};

    public int id;
    public int index;
    public long expire;
    public byte upgrade;
    public byte sys;
    public boolean isLock;
    public int yen;
    public ArrayList<ItemOption> options;
    public ArrayList<Item> gems;
    public ItemTemplate template;
    protected int quantity;
    protected long updatedAt;
    protected long createdAt;
    protected boolean isNew;
    protected boolean isGiahan;

    private int productID;
    private int productUniqueId;
    private String productSeller;
    private int productPrice;
    private byte productStatus;
    private int productTime;
    private boolean productChanged;

    public Item(int id) {
        this.id = id;
        init();
        this.quantity = 1;
        this.upgrade = 0;
        this.sys = 0;
        this.options = new ArrayList<>();
        if (this.template.isTypeAdorn() || this.template.isTypeClothe() || this.template.isTypeWeapon()) {
            this.gems = new ArrayList<>();
        }
        this.isLock = false;
        this.isGiahan = false;
        this.expire = -1;
        initOption();
        initYen();
    }

    public Item(JSONObject obj) {
        load(obj);
    }

    public void add(int amount) {
        this.quantity += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean has() {
        return has(1);
    }

    public boolean has(int amount) {
        return this.quantity >= amount;
    }

    public void reduce(int amount) {
        this.quantity -= amount;
        this.updatedAt = System.currentTimeMillis();
    }

    private void init() {
        this.template = ItemManager.getInstance().getItemTemplate(id);
    }

    public void update() {
        this.productTime--;
        setProductChanged(true);
    }

    public ArrayList<ItemOption> getOptions() {
        ArrayList<ItemOption> list = new ArrayList<>();
        int indexOption = -1;
        list.addAll(this.options);
        for (int i = 0; i < list.size(); i++) {
            int type = list.get(i).optionTemplate.type;
            int id = list.get(i).optionTemplate.id;
            if (type >= 0 && type <= 8) {
                continue;
            }
            indexOption = i;
            break;
        }
        if (indexOption == -1) {
            indexOption = list.size();
        }
        if (this.gems != null && this.gems.size() > 0) {
            ItemOption optionYenThaoNgoc = new ItemOption(122, 0);
            list.add(indexOption, optionYenThaoNgoc);
            for (Item g : this.gems) {
                optionYenThaoNgoc.param += GIA_KHAM[g.upgrade - 1] / 2;
                int optionID = -1;
                if (template.isTypeWeapon()) {
                    optionID = 106;
                } else if (template.isTypeClothe()) {
                    optionID = 107;
                } else if (template.isTypeAdorn()) {
                    optionID = 108;
                }

                list.add(indexOption + 1, new ItemOption(g.id - 543, 0));

                for (int i = 0; i < g.options.size(); i++) {

                    ItemOption o = g.options.get(i);
                    if (o.optionTemplate.id == optionID) {
                        ItemOption o1 = g.options.get(i + 1);
                        ItemOption o2 = g.options.get(i + 2);
                        list.add(indexOption + 2, new ItemOption(o1.optionTemplate.id, o1.param));
                        list.add(indexOption + 3, new ItemOption(o2.optionTemplate.id, o2.param));
                        break;
                    }
                }
            }
        }
        return list;
    }

    public ArrayList<ItemOption> getDisplayOptions() {
        int indexOption = -1;
        ArrayList<ItemOption> list = new ArrayList<>();
        list.addAll(this.options);
        for (int i = 0; i < list.size(); i++) {
            int type = list.get(i).optionTemplate.type;
            int id = list.get(i).optionTemplate.id;
            if (type >= 0 && type <= 8 && id != 156 && id != 157) {
                continue;
            }
            indexOption = i;
            break;
        }
        if (indexOption == -1) {
            indexOption = list.size();
        }
        if (this.gems != null && this.gems.size() > 0) {
            ItemOption optionYenThaoNgoc = new ItemOption(122, 0);
            list.add(indexOption, optionYenThaoNgoc);
            for (Item g : this.gems) {
                optionYenThaoNgoc.param += GIA_KHAM[g.upgrade - 1] / 2;
                int optionID = -1;
                if (template.isTypeWeapon()) {
                    optionID = 106;
                } else if (template.isTypeClothe()) {
                    optionID = 107;
                } else if (template.isTypeAdorn()) {
                    optionID = 108;
                }
                list.add(indexOption + 1, new ItemOption(g.id - 543, 0));
                for (int i = 0; i < g.options.size(); i++) {
                    ItemOption o = g.options.get(i);
                    if (o.optionTemplate.id == optionID) {
                        ItemOption o1 = g.options.get(i + 1);
                        ItemOption o2 = g.options.get(i + 2);
                        list.add(indexOption + 2, new ItemOption(o1.optionTemplate.id, o1.param));
                        list.add(indexOption + 3, new ItemOption(o2.optionTemplate.id, o2.param));
                        break;
                    }
                }
            }
        }
        return list;
    }

    // chỉ số item
    public void initOption() {
        this.options.clear();
        if (initOptionTrangPhucMoi()) {
            return;
        }
        if (initOptionCauLucDao()) {
            return;
        }
        if (this.template.isTypeWeapon()) {
            if (this.id == 799 || this.id == 800) {
                this.options.add(new ItemOption(94, 15));
                this.options.add(new ItemOption(92, 100));
                this.options.add(new ItemOption(86, 200));
            }
            if (this.id == 1156 || this.id == 1157) {
                this.options.add(new ItemOption(94, 15));
                this.options.add(new ItemOption(92, 100));
                this.options.add(new ItemOption(86, 200));
            }
        } else if (this.template.type == ItemTemplate.TYPE_MON4) {// Thú cưỡi
            this.options.add(new ItemOption(65, 0));// kinh nghiệm
            this.options.add(new ItemOption(66, 1000));// thể lực
        } else if (this.template.isTypeBijuu()) { // option vĩ thú
            if (NinjaUtils.nextInt(90) == 0) {
                expire = -1;
            } else {
                long expire = System.currentTimeMillis() + (long) (86400000 * NinjaUtils.nextInt(1, 3)); // hạn ví thũ
                this.expire = expire;
            }
            if (this.template.id >= 994 && this.template.id <= 1011) {// trung cấp
                this.options.add(new ItemOption(73, NinjaUtils.nextInt(2000, 3000)));
                this.options.add(new ItemOption(6, NinjaUtils.nextInt(2000, 3000)));
                this.options.add(new ItemOption(7, NinjaUtils.nextInt(2000, 3000)));
                this.options.add(new ItemOption(69, NinjaUtils.nextInt(10, 30)));
                this.options.add(new ItemOption(118, NinjaUtils.nextInt(50, 80)));
                this.options.add(new ItemOption(68, NinjaUtils.nextInt(50, 150)));
                this.options.add(new ItemOption(10, NinjaUtils.nextInt(50, 150)));
                this.options.add(new ItemOption(58, 20));
            } else if (this.template.id >= 1012 && this.template.id <= 1029) {// cao cấp
                this.options.add(new ItemOption(73, NinjaUtils.nextInt(3000, 5000)));
                this.options.add(new ItemOption(6, NinjaUtils.nextInt(3000, 5000)));
                this.options.add(new ItemOption(7, NinjaUtils.nextInt(3000, 5000)));
                this.options.add(new ItemOption(69, NinjaUtils.nextInt(20, 40)));
                this.options.add(new ItemOption(118, NinjaUtils.nextInt(50, 80)));
                this.options.add(new ItemOption(68, NinjaUtils.nextInt(50, 150)));
                this.options.add(new ItemOption(10, NinjaUtils.nextInt(50, 150)));
                this.options.add(new ItemOption(58, 20));
            } else if (this.template.id >= 1030 && this.template.id <= 1047) {// siêu cấp
                this.options.add(new ItemOption(73, NinjaUtils.nextInt(5000, 8000)));
                this.options.add(new ItemOption(6, NinjaUtils.nextInt(5000, 8000)));
                this.options.add(new ItemOption(7, NinjaUtils.nextInt(5000, 8000)));
                this.options.add(new ItemOption(69, NinjaUtils.nextInt(40, 60)));
                this.options.add(new ItemOption(118, NinjaUtils.nextInt(50, 80)));
                this.options.add(new ItemOption(68, NinjaUtils.nextInt(50, 150)));
                this.options.add(new ItemOption(10, NinjaUtils.nextInt(50, 150)));
                this.options.add(new ItemOption(58, 20));
            }


        } else if (this.template.isTypeEquipmentBijuu()) {
            long expire = System.currentTimeMillis() + (long) 2104800000;
            this.expire = expire;
            this.options.add(new ItemOption(85, 9));
            if (this.template.type == 35) {
                this.options.add(new ItemOption(94, 112));
            } else if (this.template.type == 36) {
                this.options.add(new ItemOption(92, 78));
            } else if (this.template.type == 37) {
                this.options.add(new ItemOption(80, 750));
            } else if (this.template.type == 38) {
                this.options.add(new ItemOption(91, 152));
            }
        } else if (this.template.type == ItemTemplate.TYPE_AOCHOANG) {// áo choàng
            this.options.add(new ItemOption(85, 0));
            if (this.id == 797) {
                this.options.add(new ItemOption(82, 1000));
                this.options.add(new ItemOption(83, 350));
                this.options.add(new ItemOption(84, 100));
                this.options.add(new ItemOption(81, 10));
                this.options.add(new ItemOption(80, 30));
            } else {
                this.options.add(new ItemOption(82, 350));
                this.options.add(new ItemOption(83, 350));
                this.options.add(new ItemOption(84, 100));
                this.options.add(new ItemOption(81, 5));
                this.options.add(new ItemOption(80, 25));
                this.options.add(new ItemOption(79, 5));
            }

        } else if (this.template.isTypeMount()) {// Trang bị thú cưới
            int rand = NinjaUtils.nextInt(0, 100);
            if (rand <= 30) {
                this.yen = 5;
                this.options.add(new ItemOption(85, 0));
                if (this.template.type == 29) {
                    this.options.add(new ItemOption(86, 50));
                } else if (this.template.type == 30) {
                    this.options.add(new ItemOption(87, 500));
                } else if (this.template.type == 31) {
                    this.options.add(new ItemOption(82, 500));
                } else if (this.template.type == 32) {
                    this.options.add(new ItemOption(84, 50));
                }
            } else {
                this.yen = 0;
            }

        } else if (this.template.type == ItemTemplate.TYPE_BAOTAY) {  // chỉ số ấn tộc
            if (this.template.id == 870) { // cấp 1
                this.options.add(new ItemOption(6, 1000));
                this.options.add(new ItemOption(118, 50));
                this.upgrade = 1;
            } else if (this.template.id == 871) {
                this.options.add(new ItemOption(6, 2000));
                this.options.add(new ItemOption(118, 100));
                this.upgrade = 2;
            } else if (this.template.id == 872) {
                this.options.add(new ItemOption(6, 3000));
                this.options.add(new ItemOption(118, 150));
                this.upgrade = 3;
            } else if (this.template.id == 873) {
                this.options.add(new ItemOption(6, 4000));
                this.options.add(new ItemOption(118, 200));
                this.upgrade = 4;
            } else if (this.template.id == 874) { // cấp 5
                this.options.add(new ItemOption(6, 5000));
                this.options.add(new ItemOption(118, 250));
                this.options.add(new ItemOption(79, 10));
                this.upgrade = 5;
            } else if (this.template.id == 875) {
                this.options.add(new ItemOption(6, 6000));
                this.options.add(new ItemOption(118, 300));
                this.options.add(new ItemOption(79, 12));

                this.options.add(new ItemOption(127, 5));
                this.options.add(new ItemOption(130, 5));
                this.options.add(new ItemOption(131, 5));
                this.upgrade = 6;
            } else if (this.template.id == 876) {
                this.options.add(new ItemOption(6, 7000));
                this.options.add(new ItemOption(118, 350));
                this.options.add(new ItemOption(79, 15));

                this.options.add(new ItemOption(127, 7));
                this.options.add(new ItemOption(130, 7));
                this.options.add(new ItemOption(131, 7));
                this.upgrade = 7;
            } else if (this.template.id == 877) {
                this.options.add(new ItemOption(6, 8000));
                this.options.add(new ItemOption(118, 400));
                this.options.add(new ItemOption(79, 18));

                this.options.add(new ItemOption(127, 10));
                this.options.add(new ItemOption(130, 10));
                this.options.add(new ItemOption(131, 10));
                this.upgrade = 8;
            } else if (this.template.id == 878) {
                this.options.add(new ItemOption(6, 9000));
                this.options.add(new ItemOption(118, 450));
                this.options.add(new ItemOption(79, 22));

                this.options.add(new ItemOption(127, 12));
                this.options.add(new ItemOption(130, 12));
                this.options.add(new ItemOption(131, 12));
                this.upgrade = 9;
            } else if (this.template.id == 879) {
                this.options.add(new ItemOption(6, 10000));
                this.options.add(new ItemOption(118, 500));
                this.options.add(new ItemOption(79, 25));

                this.options.add(new ItemOption(127, 15));
                this.options.add(new ItemOption(130, 15));
                this.options.add(new ItemOption(131, 15));
                this.upgrade = 10;
            } else if (DanhHieu.co(this.template.id)) {
                // Susano (Itachi 1096, Sasuke 1266) ở cùng ô ấn tộc nhưng chỉ số nằm trong bảng
                // DanhHieu chứ không hardcode ở đây. Hỏi bảng thay vì liệt kê id: thêm Susanoo
                // mới chỉ cần một dòng dat(...) bên DanhHieu, khỏi đụng vào tệp này.
                // Món chưa có chỉ số thì co() trả false, nhánh này bỏ qua -- đúng ý.
                DanhHieu.nap(this);
            }
        } else if (this.template.type == ItemTemplate.TYPE_MATTHAN) {// mắt danh vọng
            if (this.template.id == 685) {
                this.options.add(new ItemOption(6, 1000));
                this.options.add(new ItemOption(87, 500));
                this.upgrade = 1;
            } else if (this.template.id == 686) {
                this.options.add(new ItemOption(6, 2000));
                this.options.add(new ItemOption(87, 750));
                this.upgrade = 2;
            } else if (this.template.id == 687) {
                this.options.add(new ItemOption(6, 3000));
                this.options.add(new ItemOption(87, 1000));
                this.options.add(new ItemOption(79, 25));
                this.upgrade = 3;
            } else if (this.template.id == 688) {
                this.options.add(new ItemOption(6, 4000));
                this.options.add(new ItemOption(87, 1250));
                this.options.add(new ItemOption(79, 25));
                this.upgrade = 4;
            } else if (this.template.id == 689) {
                this.options.add(new ItemOption(6, 5000));
                this.options.add(new ItemOption(87, 1500));
                this.options.add(new ItemOption(79, 25));
                this.upgrade = 5;
            } else if (this.template.id == 690) {
                this.options.add(new ItemOption(6, 6000));
                this.options.add(new ItemOption(87, 1750));
                this.options.add(new ItemOption(79, 25));
                this.options.add(new ItemOption(64, 0));
                this.upgrade = 6;
            } else if (this.template.id == 691) {
                this.options.add(new ItemOption(6, 7000));
                this.options.add(new ItemOption(87, 2000));
                this.options.add(new ItemOption(79, 25));
                this.options.add(new ItemOption(64, 0));
                this.upgrade = 7;
            } else if (this.template.id == 692) {
                this.options.add(new ItemOption(6, 8000));
                this.options.add(new ItemOption(87, 2250));
                this.options.add(new ItemOption(79, 25));
                this.options.add(new ItemOption(64, 0));
                this.upgrade = 8;
            } else if (this.template.id == 693) {
                this.options.add(new ItemOption(6, 9000));
                this.options.add(new ItemOption(87, 2500));
                this.options.add(new ItemOption(79, 25));
                this.options.add(new ItemOption(64, 0));
                this.upgrade = 9;
            } else if (this.template.id == 694) {
                this.options.add(new ItemOption(6, 10000));
                this.options.add(new ItemOption(87, 2750));
                this.options.add(new ItemOption(79, 25));
                this.options.add(new ItemOption(64, 0));
                this.options.add(new ItemOption(113, 5000));
                this.upgrade = 10;
            }
            // chỉ số áo
        } else if (this.template.type == ItemTemplate.TYPE_AO) {
            if (this.template.id == 849) {
                this.options.add(new ItemOption(6, 3000));
                this.options.add(new ItemOption(7, 3000));
                this.options.add(new ItemOption(94, 10));
            }
            if (this.id == 805) {
                long expire = System.currentTimeMillis() + (long) (86400000 * 3);
                this.expire = expire;
            }
            if (this.id == 806) {
                long expire = System.currentTimeMillis() + (long) (86400000 * 3);
                this.expire = expire;
            }
            if (this.id == ItemName.AO_BUFFALO || this.id == ItemName.AO_RUBE
                    || this.id == ItemName.AO_FAIRIES || this.id == ItemName.AO_SIXGIRL) {
                long expire = System.currentTimeMillis() + (long) (86400000 * 3);
                this.expire = expire;

                this.options.add(new ItemOption(125, 3000));
                this.options.add(new ItemOption(117, 3000));
                this.options.add(new ItemOption(94, 10));
                this.options.add(new ItemOption(136, NinjaUtils.nextInt(0, 80)));
                this.options.add(new ItemOption(127, 10));
                this.options.add(new ItemOption(130, 10));
                this.options.add(new ItemOption(131, 10));

            }

        } else if (this.template.type == ItemTemplate.TYPE_QUAN) {

            if (this.id == ItemName.QUAN_BUFFALO || this.id == ItemName.QUAN_RUBE
                    || this.id == ItemName.QUAN_FAIRIES || this.id == ItemName.QUAN_SIXGIRL) {
                long expire = System.currentTimeMillis() + (long) (86400000 * 3);
                this.expire = expire;

                this.options.add(new ItemOption(125, 3000));
                this.options.add(new ItemOption(117, 3000));
                this.options.add(new ItemOption(94, 10));
                this.options.add(new ItemOption(136, NinjaUtils.nextInt(0, 80)));
                this.options.add(new ItemOption(127, 10));
                this.options.add(new ItemOption(130, 10));
                this.options.add(new ItemOption(131, 10));
            }

            // chỉ số mặt nạ
        } else if (this.template.type == ItemTemplate.TYPE_MATNA) {
            if (this.id == 344 || this.id == 346) {
                this.options.add(new ItemOption(57, 40));
            } else if (this.id == 403 || this.id == 404) {
                this.options.add(new ItemOption(57, 80));
            } else if (this.id == 407 || this.id == 408) {
                this.options.add(new ItemOption(58, 20));
                this.options.add(new ItemOption(6, 500));
            } else if (this.id == 337 || this.id == 338) {
                this.options.add(new ItemOption(58, 25));
                this.options.add(new ItemOption(6, 500));
            } else if (this.id == ItemName.TON_HANH_GIA) {
                this.options.add(new ItemOption(82, 3000));
                this.options.add(new ItemOption(87, 3000));
                this.options.add(new ItemOption(92, 10));
                this.options.add(new ItemOption(58, 26));
            } else if (this.id == ItemName.CAI_TRANG_BUFFALO || this.id == ItemName.CAI_TRANG_RUBE
                    || this.id == ItemName.CAI_TRANG_FAIRIES || this.id == ItemName.CAI_TRANG_SIXGIRL) {
                long expire = System.currentTimeMillis() + (long) (86400000 * 3);
                this.expire = expire;
                this.options.add(new ItemOption(82, 1000));
                this.options.add(new ItemOption(87, 1000));
                this.options.add(new ItemOption(69, 10));
                this.options.add(new ItemOption(58, 20));

            } else if (this.id == ItemName.MAT_NA_JIRAI_ || this.id == ItemName.MAT_NA_JUMITO) {
                if (this.upgrade == 4 || this.upgrade == 5 || this.upgrade == 6) {
                    this.options.add(new ItemOption(125, 1000));
                } else if (this.upgrade == 7 || this.upgrade == 8 || this.upgrade == 9) {
                    this.options.add(new ItemOption(125, 2000));
                } else if (this.upgrade == 10) {
                    this.options.add(new ItemOption(125, 3000));
                }
                this.options.add(new ItemOption(100, this.upgrade * 5));
            } else if (this.id == ItemName.SANTA_CLAUS) {
                this.options.add(new ItemOption(127, this.upgrade * 3));
                this.options.add(new ItemOption(100, 50));
            } else if (this.id == ItemName.SUMIMURA_) {
                this.options.add(new ItemOption(130, this.upgrade * 3));
                this.options.add(new ItemOption(100, 50));
            } else if (this.id == ItemName.YUKIMURA_) {
                this.options.add(new ItemOption(131, this.upgrade * 3));
                this.options.add(new ItemOption(100, 50));
            } else if (this.id == ItemName.AKATSUKI_NU || this.id == ItemName.AKATSUKI_NAM) {
                if (this.sys == 2) { // băng
                    this.options.add(new ItemOption(130, this.upgrade * 3));
                    this.options.add(new ItemOption(100, 50));
                } else if (this.sys == 1) { // hỏa
                    this.options.add(new ItemOption(127, this.upgrade * 3));
                    this.options.add(new ItemOption(100, 50));
                } else if (this.sys == 3) { // phong
                    this.options.add(new ItemOption(131, this.upgrade * 3));
                    this.options.add(new ItemOption(100, 50));
                }
            }
        } else if (this.template.type == ItemTemplate.TYPE_THUNUOI) {
            if (this.id == 419) {
                this.options.add(new ItemOption(0, 1000));
                this.options.add(new ItemOption(1, 1000));
            } else if (this.id == 568) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                this.options.add(new ItemOption(100, 30));
            } else if (this.id == 569) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                this.options.add(new ItemOption(99, 500));
            } else if (this.id == 570) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                this.options.add(new ItemOption(98, 20));
            } else if (this.id == 571) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                this.options.add(new ItemOption(101, 20));
            } else if (this.id == ItemName.PET_BONG_MA) {
                this.options.add(new ItemOption(6, 3000));
                this.options.add(new ItemOption(7, 2000));
                this.options.add(new ItemOption(98, 15));
            } else if (this.id == ItemName.PET_YEU_TINH) {
                this.options.add(new ItemOption(6, 3000));
                this.options.add(new ItemOption(7, 2000));
                this.options.add(new ItemOption(94, 10));
            } else if (this.id == ItemName.THAN_CHET) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                this.options.add(new ItemOption(73, 3000));
                this.options.add(new ItemOption(6, 3000));
                this.options.add(new ItemOption(7, 3000));
            } else if (this.id == ItemName.PET_UNG_LONG) { // chỉ số pet vv
                this.options.add(new ItemOption(6, 3000));
                this.options.add(new ItemOption(7, 3000));
                this.options.add(new ItemOption(69, 100));
                this.options.add(new ItemOption(94, 10));
                this.options.add(new ItemOption(119, 200));
                this.options.add(new ItemOption(120, 200));
            } else if (this.id == ItemName.PET_BORU) {
                this.options.add(new ItemOption(6, 5000));
                this.options.add(new ItemOption(87, 5000));
            } else if (this.id == ItemName.TUAN_LOC) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                this.options.add(new ItemOption(73, 5000));
                this.options.add(new ItemOption(6, 5000));
            } else if (this.id == ItemName.TUAN_LOC_PK) {
                long expire = System.currentTimeMillis() + (long) 86400000L;
                this.expire = expire;
                this.options.add(new ItemOption(73, 5000));
                this.options.add(new ItemOption(6, 5000));
                this.options.add(new ItemOption(153, 0));
            } else if (this.id == ItemName.PET_BORU_2) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                this.options.add(new ItemOption(73, 5000));
                this.options.add(new ItemOption(6, 5000));
            } else if (this.id == ItemName.PET_THANH_LONG) {
                long expire = System.currentTimeMillis() + (long) (86400000 * 7);
                this.expire = expire;
                this.options.add(new ItemOption(6, 2000));
                this.options.add(new ItemOption(7, 3000));
                this.options.add(new ItemOption(92, 100));
                this.options.add(new ItemOption(94, 10));
                this.options.add(new ItemOption(119, 200));
                this.options.add(new ItemOption(120, 200));

            } else if (this.id == ItemName.LONG_DEN_RONG) {
                long expire = System.currentTimeMillis() + (long) 604800000;
                this.expire = expire;
                int random = NinjaUtils.nextInt(1, 4);
                ArrayList<ItemOption> randomOptions = new ArrayList<>();
                randomOptions.add(new ItemOption(8, NinjaUtils.nextInt(100, 200)));
                randomOptions.add(new ItemOption(9, NinjaUtils.nextInt(100, 200)));
                randomOptions.add(new ItemOption(73, NinjaUtils.nextInt(1500, 3000)));
                randomOptions.add(new ItemOption(79, 5));

                for (int i = 0; i < random; i++) {
                    int indexRandom = NinjaUtils.nextInt(randomOptions.size());
                    this.options.add(randomOptions.get(indexRandom));
                    randomOptions.remove(indexRandom);
                }
            }
        } else if (this.template.type == ItemTemplate.TYPE_BIKIP) {
            // Chỉ số của bí kíp danh hiệu nằm ở com.nsoz.item.DanhHieu -- một bảng, xếp cạnh nhau
            // để nhìn ra ngay danh hiệu nào ăn vào ô nào. Trước đây mỗi cái một nhánh if ở đây,
            // thêm một danh hiệu là thêm một nhánh và không ai soát được vai có trùng nhau không.
            //
            // Bí kíp môn phái (397-402) không nằm trong bảng: chúng ra đời rỗng và nhận chỉ số khi
            // luyện ở NPC Tashino, đó là thiết kế gốc chứ không phải thiếu sót.
            DanhHieu.nap(this);

        } else if (this.template.type == ItemTemplate.TYPE_GANGTAY) {
            if (this.id == 1149) {
                long expire = System.currentTimeMillis() + (long) 2144800000;
                this.expire = expire;
                this.options.add(new ItemOption(87, 1000));
                this.options.add(new ItemOption(99, 100));
            }
        } else if (this.template.type == ItemTemplate.TYPE_NGOC_KHAM) {
            if (this.id == 652) {
                this.options.add(new ItemOption(106, 0));
                this.options.add(new ItemOption(102, NinjaUtils.nextInt(1, 500)));
                this.options.add(new ItemOption(115, -NinjaUtils.nextInt(1, 10)));
                this.options.add(new ItemOption(107, 0));
                this.options.add(new ItemOption(126, NinjaUtils.nextInt(1, 5)));
                this.options.add(new ItemOption(105, -NinjaUtils.nextInt(1, 500)));
                this.options.add(new ItemOption(108, 0));
                this.options.add(new ItemOption(114, NinjaUtils.nextInt(1, 5)));
                this.options.add(new ItemOption(118, -NinjaUtils.nextInt(1, 10)));
            } else if (this.id == 653) {
                this.options.add(new ItemOption(106, 0));
                this.options.add(new ItemOption(73, NinjaUtils.nextInt(1, 100)));
                this.options.add(new ItemOption(114, -NinjaUtils.nextInt(1, 5)));
                this.options.add(new ItemOption(107, 0));
                this.options.add(new ItemOption(124, NinjaUtils.nextInt(1, 10)));
                this.options.add(new ItemOption(114, -NinjaUtils.nextInt(1, 100)));
                this.options.add(new ItemOption(108, 0));
                this.options.add(new ItemOption(115, NinjaUtils.nextInt(1, 10)));
                this.options.add(new ItemOption(119, -NinjaUtils.nextInt(1, 10)));
            } else if (this.id == 654) {
                this.options.add(new ItemOption(106, 0));
                this.options.add(new ItemOption(103, NinjaUtils.nextInt(1, 200)));
                this.options.add(new ItemOption(125, -NinjaUtils.nextInt(1, 50)));
                this.options.add(new ItemOption(107, 0));
                this.options.add(new ItemOption(121, NinjaUtils.nextInt(1, 5)));
                this.options.add(new ItemOption(120, -NinjaUtils.nextInt(1, 10)));
                this.options.add(new ItemOption(108, 0));
                this.options.add(new ItemOption(116, NinjaUtils.nextInt(1, 10)));
                this.options.add(new ItemOption(126, -NinjaUtils.nextInt(1, 5)));
            } else if (this.id == 655) {
                this.options.add(new ItemOption(106, 0));
                this.options.add(new ItemOption(105, NinjaUtils.nextInt(1, 500)));
                this.options.add(new ItemOption(116, -NinjaUtils.nextInt(1, 10)));
                this.options.add(new ItemOption(107, 0));
                this.options.add(new ItemOption(125, NinjaUtils.nextInt(1, 50)));
                this.options.add(new ItemOption(117, -NinjaUtils.nextInt(1, 50)));
                this.options.add(new ItemOption(108, 0));
                this.options.add(new ItemOption(117, NinjaUtils.nextInt(1, 50)));
                this.options.add(new ItemOption(124, -NinjaUtils.nextInt(1, 10)));
            }
            this.options.add(new ItemOption(104, 0));
            this.options.add(new ItemOption(123, 800000));
            this.upgrade = 1;
        }
    }

    // hạn item từ sự kiện
    public void initExpire() {
        // pet
        if (this.id == ItemName.LAN_SU_VU || this.id == ItemName.BACH_HO
                || this.id == ItemName.SIEU_XE_HALLOWEEN
                || this.id == ItemName.PHUONG_HOANG_BANG || this.id == ItemName.HOA_KY_LAN
                || this.id == ItemName.BACH_NGAN_LANG || this.id == ItemName.BACH_SU_VUONG  // thêm pet mới
                || this.id == ItemName.XICH_TU_MA || this.id == ItemName.TA_LINH_MA || this.id == ItemName.PHONG_THUONG_MA
                || this.id == ItemName.PET_UNG_LONG || this.id == ItemName.PET_BORU
                || this.id == ItemName.PET_BONG_MA || this.id == ItemName.PET_YEU_TINH) {
            long expire = System.currentTimeMillis() + (long) (86400000L * NinjaUtils.nextInt(3, 10));
            this.expire = expire;
            // trang bị 2
        } else if (this.id == ItemName.GAY_MAT_TRANG || this.id == ItemName.GAY_TRAI_TIM
                || this.id == ItemName.SHIRAIJI || this.id == ItemName.HAJIRO
                || this.id == ItemName.AO_KAGE_NAM || this.id == ItemName.AO_KAGE_NU // ct kage
                || this.id == ItemName.THOI_TRANG_OBITO || this.id == ItemName.THOI_TRANG_SAKURA) {
            long expire = System.currentTimeMillis() + (long) (86400000 * 7);
            this.expire = expire;
            // mặt nạ
        } else if (this.id == ItemName.MAT_NA_SUPER_BROLY || this.id == ItemName.MAT_NA_ONNA_BUGEISHA
                || this.id == ItemName.MAT_NA_THO || this.id == ItemName.MAT_NA_THO_NU
                || this.id == ItemName.MAT_NA_SHIN_AH || this.id == ItemName.MAT_NA_VO_DIEN
                || this.id == ItemName.MAT_NA_ONI || this.id == ItemName.MAT_NA_KUMA
                || this.id == ItemName.SON_TINH || this.id == ItemName.THUY_TINH
                || this.id == ItemName.MAT_NA_INU || this.id == ItemName.MAT_NA_HO ||  this.id == ItemName.TON_HANH_GIA) {
            long expire = System.currentTimeMillis() + (long) (86400000 * 7);
            this.expire = expire;
            // item khác
        } else if (this.id == ItemName.LONG_DEN_TRON || this.id == ItemName.LONG_DEN_CA_CHEP
                || this.id == ItemName.LONG_DEN_NGOI_SAO || this.id == ItemName.LONG_DEN_MAT_TRANG
                || this.id == ItemName.HAKAIRO_YOROI || this.id == ItemName.KHAU_TRANG
                || this.id == ItemName.BUA_AITEMU || this.id == ItemName.BUA_SOCHI || this.id == ItemName.BUA_NORU) {
            long expire = System.currentTimeMillis() + (long) (86400000 * 7);
            this.expire = expire;
        }
    }


    public void initYen() {
        if (this.template.isTypeMount()) {
            return;
        }
        this.yen = this.template.level / 10 * 100;

        if (this.yen == 0) {
            this.yen = 60;
        }
        if (this.template.isTypeClothe()) {
            this.yen *= 3;
        } else if (this.template.isTypeAdorn()) {
            this.yen *= 4;
        } else if (this.template.isTypeWeapon()) {
            this.yen *= 5;
        } else if (this.template.isTypeBody() || this.template.isTypeNgocKham()
                || this.template.isTypeEquipmentBijuu()) {
            this.yen = 5;
        } else {

            this.yen = 0;
        }
    }

    public void load(JSONObject obj) {
        ParseData parse = new ParseData(obj);
        loadHeader(parse);
        init();
        try {
            this.index = parse.getInt("index");
        } catch (Exception e) {
        }
        this.isLock = parse.getBoolean("isLock");
        if (template.type == 13) {
            if (hasExpire()) {
                int remaining = (int) ((getExpire() - System.currentTimeMillis()) / 1000 / 60 / 60 / 24 / 30);
                if (remaining > 1) {
                    this.expire = (7 * 24 * 60 * 60 * 1000) + System.currentTimeMillis();
                }
            }
        }
        this.yen = parse.getInt("yen");
        this.isGiahan = parse.getBoolean("isExtend");
        this.options = new ArrayList<>();
        if (this.template.isTypeBody() || this.template.isTypeMount() || this.template.isTypeNgocKham()
                || this.template.isTypeEquipmentBijuu()) {
//            System.out.println(this.template.name);
            this.sys = parse.getByte("sys");
            this.upgrade = parse.getByte("upgrade");
            JSONArray ability = parse.getJSONArray("options");
            int size2 = ability.size();
            for (int c = 0; c < size2; c++) {
                JSONArray jAbility = (JSONArray) ability.get(c);
                int templateId = Integer.parseInt(jAbility.get(0).toString());
                int param = Integer.parseInt(jAbility.get(1).toString());
                if (templateId == 46 && param == 800) {
                    param = 55;
                }
                this.options.add(new ItemOption(templateId, param));
            }
            if (this.template.isTypeAdorn() || this.template.isTypeClothe() || this.template.isTypeWeapon()) {
                this.gems = new ArrayList<>();
                if (parse.containsKey("gems")) {
                    JSONArray gems = parse.getJSONArray("gems");
                    for (int i = 0; i < gems.size(); i++) {
                        Item gem = new Item((JSONObject) gems.get(i));
                        if (gem.template.isTypeNgocKham()) {
                            addGem(gem);
                        }
                    }
                }
                removeOptionGems();
            }
        } else {
            this.upgrade = 0;
        }
        if (this.template.isUpToUp) {
            if (parse.containsKey("quantity")) {
                this.quantity = parse.getInt("quantity");
            } else {
                this.quantity = 1;
            }
        } else {
            this.quantity = 1;
        }
    }

    public boolean isExpired() {
        return this.expire != -1 && this.expire < System.currentTimeMillis();
    }

    public int getMaxUpgradeGem() {
        int max = 0;
        for (Item item : this.gems) {
            if (item.upgrade > max) {
                max = item.upgrade;
            }
        }
        return max;
    }

    public void removeOptionGems() {
        Vector<ItemOption> options = new Vector<>();
        boolean isArrivedLineGem = false;
        for (ItemOption option : this.options) {
            if (option.optionTemplate.id == 122 || option.optionTemplate.id == 109 || option.optionTemplate.id == 110
                    || option.optionTemplate.id == 111 || option.optionTemplate.id == 112) {
                isArrivedLineGem = true;
                options.add(option);
                continue;
            }
            if (isArrivedLineGem) {
                options.add(option);
            }
        }
        this.options.removeAll(options);
    }

    public boolean addGem(Item item) {
        // kiểm tra xem tồn tại ngọc này chưa
        for (Item itm : this.gems) {
            if (itm.id == item.id) {
                return false;
            }
        }
        item.isLock = true;
        this.gems.add(item);
        return true;
    }

    public void next(int next) {
        if (next == 0) {
            return;
        }
        this.isLock = true;
        this.upgrade += next;
        if (this.options != null) {
            for (int i = 0; i < this.options.size(); i++) {
                ItemOption itemOption = this.options.get(i);
                if (itemOption.optionTemplate.id == 6 || itemOption.optionTemplate.id == 7) {
                    itemOption.param += (int) ((short) (15 * next));
                } else if (itemOption.optionTemplate.id == 8 || itemOption.optionTemplate.id == 9
                        || itemOption.optionTemplate.id == 19) {
                    itemOption.param += (int) ((short) (10 * next));
                } else if (itemOption.optionTemplate.id == 10 || itemOption.optionTemplate.id == 11
                        || itemOption.optionTemplate.id == 12 || itemOption.optionTemplate.id == 13
                        || itemOption.optionTemplate.id == 14 || itemOption.optionTemplate.id == 15
                        || itemOption.optionTemplate.id == 17 || itemOption.optionTemplate.id == 18
                        || itemOption.optionTemplate.id == 20) {
                    itemOption.param += (int) ((short) (5 * next));
                } else if (itemOption.optionTemplate.id == 21 || itemOption.optionTemplate.id == 22
                        || itemOption.optionTemplate.id == 23 || itemOption.optionTemplate.id == 24
                        || itemOption.optionTemplate.id == 25 || itemOption.optionTemplate.id == 26) {
                    itemOption.param += (int) ((short) (150 * next));
                } else if (itemOption.optionTemplate.id == 16) {
                    itemOption.param += (int) ((short) (3 * next));
                }
            }
        }
    }
    public void nextUpdate2(int next) {
        if (next == 0) {
            return;
        }
        this.isLock = true;
        if (this.options != null) {
            for (int i = 0; i < this.options.size(); i++) {
                ItemOption itemOption = this.options.get(i);
                if (itemOption.optionTemplate.id == 6 || itemOption.optionTemplate.id == 7) {
                    itemOption.param += (int) ((short) (15 * next));
                } else if (itemOption.optionTemplate.id == 8 || itemOption.optionTemplate.id == 9
                        || itemOption.optionTemplate.id == 19) {
                    itemOption.param += (int) ((short) (10 * next));
                } else if (itemOption.optionTemplate.id == 10 || itemOption.optionTemplate.id == 11
                        || itemOption.optionTemplate.id == 12 || itemOption.optionTemplate.id == 13
                        || itemOption.optionTemplate.id == 14 || itemOption.optionTemplate.id == 15
                        || itemOption.optionTemplate.id == 17 || itemOption.optionTemplate.id == 18
                        || itemOption.optionTemplate.id == 20) {
                    itemOption.param += (int) ((short) (5 * next));
                } else if (itemOption.optionTemplate.id == 21 || itemOption.optionTemplate.id == 22
                        || itemOption.optionTemplate.id == 23 || itemOption.optionTemplate.id == 24
                        || itemOption.optionTemplate.id == 25 || itemOption.optionTemplate.id == 26) {
                    itemOption.param += (int) ((short) (150 * next));
                } else if (itemOption.optionTemplate.id == 16) {
                    itemOption.param += (int) ((short) (3 * next));
                }
            }
        }
    }
    public void saveHeader(JSONObject obj) {
        obj.put("id", this.id);
        obj.put("name", this.template.name);
        obj.put("isUpToUp", this.template.isUpToUp);
        obj.put("expire", this.expire);
        obj.put("new", this.isNew);
        obj.put("updated_at", this.updatedAt);
        obj.put("created_at", this.createdAt);
    }

    public void loadHeader(ParseData parse) {
        this.id = parse.getInt("id");
        this.expire = parse.getLong("expire");
        if (parse.containsKey("new")) {
            this.isNew = parse.getBoolean("new");
            this.createdAt = parse.getLong("created_at");
            this.updatedAt = parse.getLong("updated_at");
        } else {
            this.isNew = false;
            this.createdAt = this.updatedAt = System.currentTimeMillis();
        }
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        saveHeader(obj);
        obj.put("isLock", this.isLock);
        obj.put("yen", this.yen);
        obj.put("index", this.index);

        obj.put("isExtend", this.isGiahan);
        if (this.template.isTypeBody() || this.template.isTypeMount() || this.template.isTypeNgocKham()
                || this.template.isTypeEquipmentBijuu()) {
            obj.put("sys", this.sys);
            obj.put("upgrade", this.upgrade);
            JSONArray abilitys = new JSONArray();
            if (this.options != null) {
                for (ItemOption option : this.options) {
                    JSONArray ability = new JSONArray();
                    ability.add(option.optionTemplate.id);
                    ability.add(option.param);
                    abilitys.add(ability);
                }
            }
            obj.put("options", abilitys);
            if (this.template.isTypeAdorn() || this.template.isTypeClothe() || this.template.isTypeWeapon()) {
                JSONArray gems = new JSONArray();
                for (Item gem : this.gems) {
                    gems.add(gem.toJSONObject());
                }
                obj.put("gems", gems);
            }
        }
        if (this.template.isUpToUp) {
            obj.put("quantity", this.quantity);
        } else {
            obj.put("quantity", 1);
        }
        return obj;
    }

    public int getQuantityDisplay() {
        int quantity = this.quantity;
        int maxQuantity = Config.getInstance().getMaxQuantity();
        quantity = Math.min(quantity, maxQuantity);
        return quantity;
    }

    public boolean hasExpire() {
        return !isForever();
    }

    public boolean isForever() {
        return this.expire == -1;
    }

    public boolean isPieceJirai() {
        return this.id == ItemName.MANH_AO_JIRAI_ || this.id == ItemName.MANH_NON_JIRAI_
                || this.id == ItemName.MANH_GANG_TAY_JIRAI_ || this.id == ItemName.MANH_QUAN_JIRAI_
                || this.id == ItemName.MANH_GIAY_JIRAI_ || this.id == ItemName.MANH_NGOC_BOI_JIRAI_
                || this.id == ItemName.MANH_DAY_CHUYEN_JIRAI_ || this.id == ItemName.MANH_NHAN_JIRAI_
                || this.id == ItemName.MANH_PHU_JIRAI_;
    }

    public boolean isPieceJumito() {
        return this.id == ItemName.MANH_AO_JUMITO || this.id == ItemName.MANH_NON_JUMITO
                || this.id == ItemName.MANH_GANG_TAY_JUMITO || this.id == ItemName.MANH_QUAN_JUMITO
                || this.id == ItemName.MANH_GIAY_JUMITO || this.id == ItemName.MANH_NGOC_BOI_JUMITO
                || this.id == ItemName.MANH_DAY_CHUYEN_JUMITO || this.id == ItemName.MANH_NHAN_JUMITO
                || this.id == ItemName.MANH_PHU_JUMITO;
    }

    public boolean isJirai() {
        return this.id == ItemName.AO_JIRAI || this.id == ItemName.NON_JIRAI || this.id == ItemName.GANG_TAY_JIRAI
                || this.id == ItemName.QUAN_JIRAI_ || this.id == ItemName.GIAY_JIRAI
                || this.id == ItemName.NGOC_BOI_JIRAI || this.id == ItemName.DAY_CHUYEN_JIRAI
                || this.id == ItemName.NHAN_JIRAI || this.id == ItemName.PHU_JIRAI;
    }

    public boolean isJumito() {
        return this.id == ItemName.AO_JUMITO || this.id == ItemName.NON_JUMITO || this.id == ItemName.GANG_TAY_JUMITO
                || this.id == ItemName.QUAN_JUMITO || this.id == ItemName.GIAY_JUMITO
                || this.id == ItemName.NGOC_BOI_JUMITO || this.id == ItemName.DAY_CHUYEN_JUMITO
                || this.id == ItemName.NHAN_JUMITO || this.id == ItemName.PHU_JUMITO;
    }

    public boolean isPieceCollection() {
        return isPieceJirai() || isPieceJumito();
    }

    public boolean isRemoveItem() {
        return isRemoveItem(false);
    }

    public boolean isRemoveItem(boolean onlyBag) {
        if (this.id == ItemName.KHAU_TRANG && this.isForever()) {
            long expire = System.currentTimeMillis() + (long) (86400000 * 7);
            this.expire = expire;
        }
        // if (this.id == ItemName.HOA_KY_LAN && this.isForever()) {
        // long expire = System.currentTimeMillis() + (long) (86400000 * 7);
        // this.expire = expire;
        // }
        // if(onlyBag && this.template.isTypeWeapon() && this.upgrade == 0){
        // ItemStore itemStore = StoreData.getItemBody(this.template.level, this.sys,
        // 2);
        // if(itemStore != null){
        // for (int a = 0; a < itemStore.option_max.length; a++) {
        // int templateId = itemStore.option_max[a][0];
        // int param = itemStore.option_max[a][1];
        // ItemOption itemOption = getItemOption(templateId);
        // if(itemOption != null && itemOption.param != param){
        // return false;
        // }
        // }
        // return true;
        // }
        // }
        return false;
    }

    public ItemOption getItemOption(int templateId) {
        for (ItemOption itemOption : options) {
            if (itemOption.optionTemplate.id == templateId) {
                return itemOption;
            }
        }
        return null;
    }

    // random chỉ số mặt nạ trang bị 2
    public void randomOptionHalloween() {
        // fix tạo option mn
        if (!this.options.isEmpty()) {
            return;
        }
        int random = NinjaUtils.nextInt(1, 4);
        ArrayList<ItemOption> randomOptions = new ArrayList<>();
        randomOptions.add(new ItemOption(0, NinjaUtils.nextInt(200, 500))); // tấn công ngoai
        randomOptions.add(new ItemOption(1, NinjaUtils.nextInt(200, 500))); // tấn công nội
        randomOptions.add(new ItemOption(2, NinjaUtils.nextInt(100, 150))); // kháng
        randomOptions.add(new ItemOption(3, NinjaUtils.nextInt(100, 150))); // kháng
        randomOptions.add(new ItemOption(4, NinjaUtils.nextInt(100, 150))); // kháng
        randomOptions.add(new ItemOption(5, NinjaUtils.nextInt(50, 100))); // né đòn
        randomOptions.add(new ItemOption(6, NinjaUtils.nextInt(1000, 2000))); // hp tối đa
        randomOptions.add(new ItemOption(8, NinjaUtils.nextInt(50, 200))); // vật công ngoại
        randomOptions.add(new ItemOption(9, NinjaUtils.nextInt(50, 200))); // vật công nội
        randomOptions.add(new ItemOption(57, NinjaUtils.nextInt(80, 120))); // cộng tiềm năng cho tất cả
        randomOptions.add(new ItemOption(58, NinjaUtils.nextInt(20, 30))); // cộng % tiềm năng
        randomOptions.add(new ItemOption(87, NinjaUtils.nextInt(1000, 5000))); // tấn công

        for (int i = 0; i < random; i++) {
            int indexRandom = NinjaUtils.nextInt(randomOptions.size());
            this.options.add(randomOptions.get(indexRandom));
            randomOptions.remove(indexRandom);
        }
    }

    // random chỉ số lồng đèn trang bị 2
    public void randomOptionLongDen() {
        int random = NinjaUtils.nextInt(1, 4);
        ArrayList<ItemOption> randomOptions = new ArrayList<>();        randomOptions.add(new ItemOption(1, NinjaUtils.nextInt(200, 500))); // tấn công nội
        randomOptions.add(new ItemOption(2, NinjaUtils.nextInt(100, 150))); // kháng
        randomOptions.add(new ItemOption(3, NinjaUtils.nextInt(100, 150))); // kháng
        randomOptions.add(new ItemOption(4, NinjaUtils.nextInt(100, 150))); // kháng
        randomOptions.add(new ItemOption(5, NinjaUtils.nextInt(50, 100))); // né đòn
        randomOptions.add(new ItemOption(6, NinjaUtils.nextInt(1000, 2000))); // hp tối đa
        randomOptions.add(new ItemOption(8, NinjaUtils.nextInt(50, 200))); // vật công ngoại
        randomOptions.add(new ItemOption(9, NinjaUtils.nextInt(50, 200))); // vật công nội
        randomOptions.add(new ItemOption(57, NinjaUtils.nextInt(80, 120))); // cộng tiềm năng cho tất cả
        randomOptions.add(new ItemOption(58, NinjaUtils.nextInt(20, 30))); // cộng % tiềm năng
        randomOptions.add(new ItemOption(87, NinjaUtils.nextInt(1000, 5000))); // tấn công
        randomOptions.add(new ItemOption(0, NinjaUtils.nextInt(200, 500))); // tấn công ngoai


        for (int i = 0; i < random; i++) {
            int indexRandom = NinjaUtils.nextInt(randomOptions.size());
            this.options.add(randomOptions.get(indexRandom));
            randomOptions.remove(indexRandom);
        }
    }

    public void randomOptionTigerMask() {

        this.options.add(new ItemOption(125, 3000));
        this.options.add(new ItemOption(117, 3000));
        this.options.add(new ItemOption(94, 10));
        // random thêm tỉ lệ chỉ số
        if (NinjaUtils.nextInt(0, 5) == 0) {
            this.options.add(new ItemOption(136, NinjaUtils.nextInt(0, 80)));
        }
        if (NinjaUtils.nextInt(0, 5) == 0) {
            this.options.add(new ItemOption(127, 10));
            this.options.add(new ItemOption(130, 10));
            this.options.add(new ItemOption(131, 10));
        }

    }

    public void randomOptionTigerMask1() {
        this.options.add(new ItemOption(125, 3000));
        this.options.add(new ItemOption(117, 3000));
        this.options.add(new ItemOption(94, 10));
        this.options.add(new ItemOption(136, 100));
        this.options.add(new ItemOption(127, 10));
        this.options.add(new ItemOption(130, 10));
        this.options.add(new ItemOption(131, 10));

    }

    public void randomOptionTigerMask2() {  // ct kage
        this.options.add(new ItemOption(125, 3000));
        this.options.add(new ItemOption(117, 3000));
        this.options.add(new ItemOption(94, 10));
        this.options.add(new ItemOption(136, 100));
        this.options.add(new ItemOption(127, 10));
        this.options.add(new ItemOption(130, 10));
        this.options.add(new ItemOption(131, 10));

    }


    // pet ứng long có hạn random chỉ số
    public void randomOptionUngLong() {
        this.options.add(new ItemOption(69, 10));
        if (NinjaUtils.nextInt(0, 3) == 0 || hasExpire()) {
            this.options.add(new ItemOption(114, NinjaUtils.nextInt(0, 100)));
        }
        if (NinjaUtils.nextInt(0, 3) == 0 || hasExpire()) {
            this.options.add(new ItemOption(94, 10));
        }
        if (NinjaUtils.nextInt(0, 2) == 0 || hasExpire()) {
            this.options.add(new ItemOption(119, 200));
            this.options.add(new ItemOption(120, 200));
        }
    }

    public void randomOptionMount() {
        this.randomOptionMount(false);
    }

    public void randomOptionMount(boolean isSpecial) {
        Vector<int[]> opt = new Vector<>();
        opt.add(new int[]{6, 50});
        opt.add(new int[]{7, 50});
        opt.add(new int[]{10, 10});
        opt.add(new int[]{67, 5});
        opt.add(new int[]{69, 10});
        opt.add(new int[]{68, 10});
        opt.add(new int[]{70, 5});
        opt.add(new int[]{71, 5});
        opt.add(new int[]{72, 5});
        opt.add(new int[]{73, 100});
        opt.add(new int[]{74, 50});

        if (this.id == 776 || this.id == 777) {
            // item.options.add(new ItemOption(128, 5));
        }

        if (isSpecial) {
            opt.remove(4);
            opt.remove(3);
            this.options.add(new ItemOption(67, 5));
            this.options.add(new ItemOption(69, 10));
        }
        int randomHe = NinjaUtils.nextInt(1, 3);
        int length = isSpecial ? 2 : 4;
        if (this.id == ItemName.HOA_KY_LAN) {
            if (randomHe == 1) {
                this.options.add(new ItemOption(94, 12));
                this.options.add(new ItemOption(69, 120));
                this.options.add(new ItemOption(10, 120));
                this.options.add(new ItemOption(67, 60));
            } else if (randomHe == 2) {
                this.options.add(new ItemOption(74, 600));
                this.options.add(new ItemOption(124, 600));
                this.options.add(new ItemOption(6, 600));
                this.options.add(new ItemOption(118, 24));
            } else {
                this.options.add(new ItemOption(119, 600));
                this.options.add(new ItemOption(120, 600));
                this.options.add(new ItemOption(68, 120));
                this.options.add(new ItemOption(58, 25));
            }
        } else {
            for (int i = 0; i < length; i++) {
                int rd = NinjaUtils.nextInt(opt.size());
                int[] option = opt.get(rd);
                //    fix chỉ số pet gấp x lần sói thường khi mở
                if (this.id == ItemName.HUYET_SAC_HUNG_LANG
                        || this.id == ItemName.XICH_TU_MA || this.id == ItemName.TA_LINH_MA || this.id == ItemName.PHONG_THUONG_MA) {
                    this.options.add(new ItemOption(option[0], option[1] * 10));
                } else if (this.id == ItemName.PHUONG_HOANG_BANG) {
                    this.options.add(new ItemOption(option[0], option[1] * 12));
                } else if (this.id == ItemName.BACH_HO || this.id == ItemName.SIEU_XE_HALLOWEEN ) {
                    this.options.add(new ItemOption(option[0], option[1] * 11));
                } else if ( this.id == ItemName.BACH_NGAN_LANG  || this.id == ItemName.BACH_SU_VUONG  ) { // thêm pet mới
                    this.options.add(new ItemOption(option[0], option[1] * 11));

                } else {
                    this.options.add(new ItemOption(option[0], option[1]));
                }
                opt.remove(rd);
            }
        }

        // chỉ số phụ của pet
        // chỗ này là random ra cs phụ cuar Lân
        if (this.id == ItemName.LAN_SU_VU) {
            // if (isForever()) {
            this.options.add(new ItemOption(119, 200));//Mỗi 5 giây phục hồi MP: #
            this.options.add(new ItemOption(120, 200)); //Mỗi 5 giây phục hồi HP: #
            // } else {
            //     if ((int) NinjaUtils.nextInt(0, 3) == 0) {
            //         this.options.add(new ItemOption(119, 200));//Mỗi 5 giây phục hồi MP: #
            //     }
            //     if ((int) NinjaUtils.nextInt(0, 3) == 0) {
            //         this.options.add(new ItemOption(120, 200)); //Mỗi 5 giây phục hồi HP: #
            //     }
            // }

        }
        // chỗ này là random ra cs phụ của hổ
        if (this.id == ItemName.BACH_HO || this.id == ItemName.SIEU_XE_HALLOWEEN) {
            if (isForever()) {
                this.options.add(new ItemOption(58, 20));// Cộng thêm tiềm năng: +#%
                this.options.add(new ItemOption(94, 15)); //Tấn công: +#%
            } else {
                if ((int) NinjaUtils.nextInt(0, 4) == 0) {
                    this.options.add(new ItemOption(58, 20));// Cộng thêm tiềm năng: +#%
                }
                if ((int) NinjaUtils.nextInt(0, 5) == 0) {
                    this.options.add(new ItemOption(94, 15)); //Tấn công: +#%
                }
            }

        }


        if ( this.id == ItemName.BACH_NGAN_LANG || this.id == ItemName.BACH_SU_VUONG ) {   // thêm pet mới
            if (isForever()) {
                this.options.add(new ItemOption(58, 20));// Cộng thêm tiềm năng: +#%
                this.options.add(new ItemOption(94, 15)); //Tấn công: +#%
            } else {
                if ((int) NinjaUtils.nextInt(0, 4) == 0) {
                    this.options.add(new ItemOption(58, 20));// Cộng thêm tiềm năng: +#%
                }
                if ((int) NinjaUtils.nextInt(0, 5) == 0) {
                    this.options.add(new ItemOption(94, 15)); //Tấn công: +#%
                }
            }

        }

        if (this.id == ItemName.HOA_KY_LAN) {
            if (isForever()) {
                int random = NinjaUtils.nextInt(5);
                if (randomHe == 1) {
                    this.options.add(new ItemOption(158, (int) NinjaUtils.nextInt(1, 10)));//Kỹ năng Hỏa Kích:

                } else if (randomHe == 2) {
                    this.options.add(new ItemOption(160, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Cường Thân:

                } else {
                    this.options.add(new ItemOption(159, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Hộ Thể:

                }


            } else {

                if (randomHe == 1 && (int) NinjaUtils.nextInt(0, 3) == 0) {
                    this.options.add(new ItemOption(158, (int) NinjaUtils.nextInt(1, 10)));//Kỹ năng Hỏa Kích:
                } else if (randomHe == 2 && (int) NinjaUtils.nextInt(0, 3) == 0) {
                    this.options.add(new ItemOption(160, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Cường Thân:
                } else if ((int) NinjaUtils.nextInt(0, 3) == 0) {
                    this.options.add(new ItemOption(159, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Hộ Thể:
                }
            }

        }

        if (this.id == ItemName.PHUONG_HOANG_BANG) {
            if (isForever()) {

                this.options.add(new ItemOption(134, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Mưa Băng

                this.options.add(new ItemOption(135, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Vụ Nổ Băng Giá

            } else {
                if ((int) NinjaUtils.nextInt(0, 5) == 0) {
                    this.options.add(new ItemOption(134, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Mưa Băng
                }
                if ((int) NinjaUtils.nextInt(0, 5) == 0) {
                    this.options.add(new ItemOption(135, (int) NinjaUtils.nextInt(1, 10))); //Kỹ năng Vụ Nổ Băng Giá
                }
            }
            // skill

        }
        if (this.id == ItemName.XICH_TU_MA || this.id == ItemName.PHONG_THUONG_MA || this.id == ItemName.TA_LINH_MA) {
            // 3 loại kháng của ngựa
            if (isForever()) {

                this.options.add(new ItemOption(127, 10)); //Kháng st hệ hoả: #%

                this.options.add(new ItemOption(130, 10)); //Kháng st hệ băng: #%

                this.options.add(new ItemOption(131, 10)); //Kháng st hệ phong: #%

            } else {
                if ((int) NinjaUtils.nextInt(0, 3) == 0) {
                    this.options.add(new ItemOption(127, 10)); //Kháng st hệ hoả: #%
                }
                if ((int) NinjaUtils.nextInt(0, 3) == 0) {
                    this.options.add(new ItemOption(130, 10)); //Kháng st hệ băng: #%
                }
                if ((int) NinjaUtils.nextInt(0, 3) == 0) {
                    this.options.add(new ItemOption(131, 10)); //Kháng st hệ phong: #%
                }
            }


        }


    }


    // random chỉ số trang bị 2
    public void randomOption() {
        if (((this.id >= 814 && this.id <= 818) || this.id == ItemName.KHAU_TRANG) && this.options.isEmpty()) { // Halloween
            randomOptionHalloween();
        } else if (this.id == ItemName.PET_UNG_LONG && this.options.size() == 2) {
//            randomOptionUngLong(); // fix pet ứng long (nếu bật luyện pet thì tắt cái này)
        } else if (this.template.isTypeMount() && this.options.size() == 2) {
            randomOptionMount();
        } else if ((this.id == ItemName.NHAT_TU_LAM_PHONG || this.id == ItemName.THIEN_NGUYET_CHI_NU || this.id == ItemName.MAT_NA_HO
                || this.id == ItemName.SHIRAIJI || this.id == ItemName.HAJIRO
                || this.id == ItemName.AO_TAN_THOI || this.id == ItemName.AO_NGU_THAN)
                && this.options.isEmpty()) {
            randomOptionTigerMask();
        } else if ((this.id == ItemName.THOI_TRANG_SAKURA || this.id == ItemName.THOI_TRANG_OBITO) && this.options.isEmpty()) {
            randomOptionTigerMask1();
        } else if ((this.id == ItemName.AO_KAGE_NAM || this.id == ItemName.AO_KAGE_NU) && this.options.isEmpty()) { // ct kage
            randomOptionTigerMask2();
        }
    }

    // random trang bị 9x
    public void randomOptionItem9x(boolean isMaxOption) {
        this.isLock = false;
        if (id >= 618 && id <= 627) { // quần áo
            int[][][] itemOptionIds = {
                    {{47, 2, 6, 7, 11, 17, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 12, 17, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 17, 27, 28, 29, 34, 49}},
                    {{47, 2, 6, 7, 11, 17, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 11, 17, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 17, 27, 28, 29, 34, 49}},
                    {{47, 4, 6, 7, 13, 15, 27, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 15, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 35, 50}},
                    {{47, 4, 6, 7, 13, 17, 25, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 15, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 35, 50}},
                    {{47, 2, 6, 7, 11, 15, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 15, 27, 28, 29, 34, 49}},
                    {{47, 2, 6, 7, 11, 15, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 15, 27, 28, 29, 34, 49}},
                    {{47, 3, 6, 7, 12, 18, 27, 28, 29, 34, 49},
                            {47, 4, 6, 7, 13, 18, 27, 28, 29, 35, 50},
                            {47, 2, 6, 7, 11, 18, 27, 28, 29, 33, 48}},
                    {{47, 3, 6, 7, 12, 18, 27, 28, 29, 34, 49},
                            {47, 4, 6, 7, 13, 18, 27, 28, 29, 35, 50},
                            {47, 2, 6, 7, 11, 18, 27, 28, 29, 33, 48}},
                    {{47, 4, 6, 7, 13, 16, 27, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 16, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 16, 27, 28, 29, 35, 50}},
                    {{47, 4, 6, 7, 13, 16, 27, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 16, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 16, 27, 28, 29, 35, 50}}

            };
            int[] itemOptionParams = {40, 60, 150, 150, 60, 60, 16, 9, 550, 180, 500}; // mcs
            byte randomSys = (byte) NinjaUtils.nextInt(1, 3);
            int indexItem = id - 618;
            int[] itemOptionId = itemOptionIds[indexItem][randomSys - 1];
            int[][] option_max = new int[itemOptionId.length][2];
            for (int i = 0; i < itemOptionId.length; i++) {
                int optionId = itemOptionId[i];
                int param = itemOptionParams[i];
                option_max[i][0] = optionId;
                if (optionId == 16) {
                    option_max[i][1] = 36;
                } else {
                    option_max[i][1] = param;
                }
            }
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = randomSys;
        } else if (id >= 628 && id <= 631) { // trang sức
            int[][][] itemOptionIds = {
                    {{47, 5, 6, 7, 12, 20, 30, 31, 32, 36, 46},
                            {47, 5, 6, 7, 13, 20, 30, 31, 32, 36, 46},
                            {47, 5, 6, 7, 11, 20, 30, 31, 32, 36, 46}},
                    {{47, 5, 6, 7, 13, 14, 30, 31, 32, 33, 51},
                            {47, 5, 6, 7, 11, 14, 30, 31, 32, 34, 52},
                            {47, 5, 6, 7, 12, 14, 30, 31, 32, 35, 53}},
                    {{47, 5, 6, 7, 11, 17, 30, 31, 32, 35, 53},
                            {47, 5, 6, 7, 12, 17, 30, 31, 32, 33, 51},
                            {47, 5, 6, 7, 13, 17, 30, 31, 32, 34, 52}},
                    {{47, 5, 6, 7, 12, 14, 30, 31, 32, 34, 52},
                            {47, 5, 6, 7, 13, 14, 30, 31, 32, 35, 53},
                            {47, 5, 6, 7, 11, 14, 30, 31, 32, 33, 51}}

            };
            int[] itemOptionParams = {20, 60, 150, 150, 60, 60, 16, 9, 550, 180, 800};
            byte randomSys = (byte) NinjaUtils.nextInt(1, 3);
            int indexItem = id - 628;
            int[] itemOptionId = itemOptionIds[indexItem][randomSys - 1];
            int[][] option_max = new int[itemOptionId.length][2];
            for (int i = 0; i < itemOptionId.length; i++) {
                int optionId = itemOptionId[i];
                int param = itemOptionParams[i];
                if (optionId == 46) {
                    param = 55;
                }
                option_max[i][0] = optionId;
                option_max[i][1] = param;
            }
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = randomSys;

            // vũ khí 9x
        } else if (id == 632) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 500;
            option_max[1][0] = 1;
            option_max[1][1] = 500;
            option_max[2][0] = 8;
            option_max[2][1] = 150;
            option_max[3][0] = 10;
            option_max[3][1] = 60;
            option_max[4][0] = 21;
            option_max[4][1] = 1800;
            option_max[5][0] = 19;
            option_max[5][1] = 150;
            option_max[6][0] = 27;
            option_max[6][1] = 16;
            option_max[7][0] = 37;
            option_max[7][1] = 90;
            option_max[8][0] = 38;
            option_max[8][1] = 900;
            option_max[9][0] = 39;
            option_max[9][1] = 140;
            option_max[10][0] = 55;
            option_max[10][1] = 40;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 1;
        } else if (id == 633) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 500;
            option_max[1][0] = 1;
            option_max[1][1] = 500;
            option_max[2][0] = 9;
            option_max[2][1] = 150;
            option_max[3][0] = 10;
            option_max[3][1] = 60;
            option_max[4][0] = 22;
            option_max[4][1] = 1800;
            option_max[5][0] = 19;
            option_max[5][1] = 150;
            option_max[6][0] = 27;
            option_max[6][1] = 16;
            option_max[7][0] = 37;
            option_max[7][1] = 90;
            option_max[8][0] = 38;
            option_max[8][1] = 900;
            option_max[9][0] = 39;
            option_max[9][1] = 140;
            option_max[10][0] = 55;
            option_max[10][1] = 40;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 1;
        } else if (id == 634) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 500;
            option_max[1][0] = 1;
            option_max[1][1] = 500;
            option_max[2][0] = 8;
            option_max[2][1] = 150;
            option_max[3][0] = 10;
            option_max[3][1] = 60;
            option_max[4][0] = 23;
            option_max[4][1] = 1800;
            option_max[5][0] = 19;
            option_max[5][1] = 150;
            option_max[6][0] = 27;
            option_max[6][1] = 16;
            option_max[7][0] = 37;
            option_max[7][1] = 90;
            option_max[8][0] = 38;
            option_max[8][1] = 900;
            option_max[9][0] = 39;
            option_max[9][1] = 140;
            option_max[10][0] = 56;
            option_max[10][1] = 40;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 2;
        } else if (id == 635) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 500;
            option_max[1][0] = 1;
            option_max[1][1] = 500;
            option_max[2][0] = 9;
            option_max[2][1] = 150;
            option_max[3][0] = 10;
            option_max[3][1] = 60;
            option_max[4][0] = 24;
            option_max[4][1] = 1800;
            option_max[5][0] = 19;
            option_max[5][1] = 150;
            option_max[6][0] = 27;
            option_max[6][1] = 16;
            option_max[7][0] = 37;
            option_max[7][1] = 90;
            option_max[8][0] = 38;
            option_max[8][1] = 900;
            option_max[9][0] = 39;
            option_max[9][1] = 140;
            option_max[10][0] = 56;
            option_max[10][1] = 40;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 2;
        } else if (id == 636) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 500;
            option_max[1][0] = 1;
            option_max[1][1] = 500;
            option_max[2][0] = 8;
            option_max[2][1] = 150;
            option_max[3][0] = 10;
            option_max[3][1] = 60;
            option_max[4][0] = 25;
            option_max[4][1] = 1800;
            option_max[5][0] = 19;
            option_max[5][1] = 150;
            option_max[6][0] = 27;
            option_max[6][1] = 16;
            option_max[7][0] = 37;
            option_max[7][1] = 90;
            option_max[8][0] = 38;
            option_max[8][1] = 900;
            option_max[9][0] = 39;
            option_max[9][1] = 140;
            option_max[10][0] = 54;
            option_max[10][1] = 40;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 3;
        } else if (id == 637) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 500;
            option_max[1][0] = 1;
            option_max[1][1] = 500;
            option_max[2][0] = 9;
            option_max[2][1] = 150;
            option_max[3][0] = 10;
            option_max[3][1] = 60;
            option_max[4][0] = 26;
            option_max[4][1] = 1800;
            option_max[5][0] = 19;
            option_max[5][1] = 150;
            option_max[6][0] = 27;
            option_max[6][1] = 16;
            option_max[7][0] = 37;
            option_max[7][1] = 90;
            option_max[8][0] = 38;
            option_max[8][1] = 900;
            option_max[9][0] = 39;
            option_max[9][1] = 140;
            option_max[10][0] = 54;
            option_max[10][1] = 40;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 3;
        }
    }

    public void randomOptionItem10x(boolean isMaxOption) {
        this.isLock = false;
        if (id >= 1163 && id <= 1172) { // quần áo
            int[][][] itemOptionIds = {
                    {{47, 2, 6, 7, 11, 17, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 12, 17, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 17, 27, 28, 29, 34, 49}},
                    {{47, 2, 6, 7, 11, 17, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 11, 17, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 17, 27, 28, 29, 34, 49}},
                    {{47, 4, 6, 7, 13, 15, 27, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 15, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 35, 50}},
                    {{47, 4, 6, 7, 13, 17, 25, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 15, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 35, 50}},
                    {{47, 2, 6, 7, 11, 15, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 15, 27, 28, 29, 34, 49}},
                    {{47, 2, 6, 7, 11, 15, 27, 28, 29, 35, 50},
                            {47, 3, 6, 7, 12, 15, 27, 28, 29, 33, 48},
                            {47, 4, 6, 7, 13, 15, 27, 28, 29, 34, 49}},
                    {{47, 3, 6, 7, 12, 18, 27, 28, 29, 34, 49},
                            {47, 4, 6, 7, 13, 18, 27, 28, 29, 35, 50},
                            {47, 2, 6, 7, 11, 18, 27, 28, 29, 33, 48}},
                    {{47, 3, 6, 7, 12, 18, 27, 28, 29, 34, 49},
                            {47, 4, 6, 7, 13, 18, 27, 28, 29, 35, 50},
                            {47, 2, 6, 7, 11, 18, 27, 28, 29, 33, 48}},
                    {{47, 4, 6, 7, 13, 16, 27, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 16, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 16, 27, 28, 29, 35, 50}},
                    {{47, 4, 6, 7, 13, 16, 27, 28, 29, 33, 48},
                            {47, 2, 6, 7, 11, 16, 27, 28, 29, 34, 49},
                            {47, 3, 6, 7, 12, 16, 27, 28, 29, 35, 50}}

            };
            int[] itemOptionParams = {60, 80, 200, 200, 80, 80, 24, 13, 600, 220, 600}; // mcs
            byte randomSys = (byte) NinjaUtils.nextInt(1, 3);
            int indexItem = id - 1163;
            int[] itemOptionId = itemOptionIds[indexItem][randomSys - 1];
            int[][] option_max = new int[itemOptionId.length][2];
            for (int i = 0; i < itemOptionId.length; i++) {
                int optionId = itemOptionId[i];
                int param = itemOptionParams[i];
                option_max[i][0] = optionId;
                if (optionId == 16) {
                    option_max[i][1] = 36;
                } else {
                    option_max[i][1] = param;
                }
            }
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = randomSys;
        } else if (id >= 1173 && id <= 1176) { // trang sức
            int[][][] itemOptionIds = {
                    {{47, 5, 6, 7, 12, 20, 30, 31, 32, 36, 46},
                            {47, 5, 6, 7, 13, 20, 30, 31, 32, 36, 46},
                            {47, 5, 6, 7, 11, 20, 30, 31, 32, 36, 46}},
                    {{47, 5, 6, 7, 13, 14, 30, 31, 32, 33, 51},
                            {47, 5, 6, 7, 11, 14, 30, 31, 32, 34, 52},
                            {47, 5, 6, 7, 12, 14, 30, 31, 32, 35, 53}},
                    {{47, 5, 6, 7, 11, 17, 30, 31, 32, 35, 53},
                            {47, 5, 6, 7, 12, 17, 30, 31, 32, 33, 51},
                            {47, 5, 6, 7, 13, 17, 30, 31, 32, 34, 52}},
                    {{47, 5, 6, 7, 12, 14, 30, 31, 32, 34, 52},
                            {47, 5, 6, 7, 13, 14, 30, 31, 32, 35, 53},
                            {47, 5, 6, 7, 11, 14, 30, 31, 32, 33, 51}}

            };
            int[] itemOptionParams = {30, 80, 200, 200, 80, 80, 24, 13, 600, 220, 900};
            byte randomSys = (byte) NinjaUtils.nextInt(1, 3);
            int indexItem = id - 1173;
            int[] itemOptionId = itemOptionIds[indexItem][randomSys - 1];
            int[][] option_max = new int[itemOptionId.length][2];
            for (int i = 0; i < itemOptionId.length; i++) {
                int optionId = itemOptionId[i];
                int param = itemOptionParams[i];
                if (optionId == 46) {
                    param = 55;
                }
                option_max[i][0] = optionId;
                option_max[i][1] = param;
            }
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = randomSys;

            // chỉ số vũ khí 100
        } else if (id == ItemName.VU_KHI_KIEM_100) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 600;
            option_max[1][0] = 1;
            option_max[1][1] = 600;
            option_max[2][0] = 8;
            option_max[2][1] = 170;
            option_max[3][0] = 10;
            option_max[3][1] = 70;
            option_max[4][0] = 21;
            option_max[4][1] = 2000;
            option_max[5][0] = 19;
            option_max[5][1] = 170;
            option_max[6][0] = 27;
            option_max[6][1] = 18;
            option_max[7][0] = 37;
            option_max[7][1] = 100;
            option_max[8][0] = 38;
            option_max[8][1] = 1000;
            option_max[9][0] = 39;
            option_max[9][1] = 150;
            option_max[10][0] = 54;
            option_max[10][1] = 60;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 1;
        } else if (id == ItemName.VU_KHI_TIEU_100) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 600;
            option_max[1][0] = 1;
            option_max[1][1] = 600;
            option_max[2][0] = 9;
            option_max[2][1] = 170;
            option_max[3][0] = 10;
            option_max[3][1] = 70;
            option_max[4][0] = 22;
            option_max[4][1] = 2000;
            option_max[5][0] = 19;
            option_max[5][1] = 170;
            option_max[6][0] = 27;
            option_max[6][1] = 18;
            option_max[7][0] = 37;
            option_max[7][1] = 100;
            option_max[8][0] = 38;
            option_max[8][1] = 1000;
            option_max[9][0] = 39;
            option_max[9][1] = 150;
            option_max[10][0] = 54;
            option_max[10][1] = 60;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 1;
        } else if (id == ItemName.VU_KHI_KUNAI_100) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 600;
            option_max[1][0] = 1;
            option_max[1][1] = 600;
            option_max[2][0] = 8;
            option_max[2][1] = 170;
            option_max[3][0] = 10;
            option_max[3][1] = 70;
            option_max[4][0] = 23;
            option_max[4][1] = 2000;
            option_max[5][0] = 19;
            option_max[5][1] = 170;
            option_max[6][0] = 27;
            option_max[6][1] = 18;
            option_max[7][0] = 37;
            option_max[7][1] = 100;
            option_max[8][0] = 38;
            option_max[8][1] = 1000;
            option_max[9][0] = 39;
            option_max[9][1] = 150;
            option_max[10][0] = 55;
            option_max[10][1] = 60;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 2;
        } else if (id == ItemName.VU_KHI_CUNG_100) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 600;
            option_max[1][0] = 1;
            option_max[1][1] = 600;
            option_max[2][0] = 9;
            option_max[2][1] = 170;
            option_max[3][0] = 10;
            option_max[3][1] = 70;
            option_max[4][0] = 24;
            option_max[4][1] = 2000;
            option_max[5][0] = 19;
            option_max[5][1] = 170;
            option_max[6][0] = 27;
            option_max[6][1] = 18;
            option_max[7][0] = 37;
            option_max[7][1] = 100;
            option_max[8][0] = 38;
            option_max[8][1] = 1000;
            option_max[9][0] = 39;
            option_max[9][1] = 150;
            option_max[10][0] = 55;
            option_max[10][1] = 60;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 2;
        } else if (id == ItemName.VU_KHI_DAO_100) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 600;
            option_max[1][0] = 1;
            option_max[1][1] = 600;
            option_max[2][0] = 8;
            option_max[2][1] = 170;
            option_max[3][0] = 10;
            option_max[3][1] = 70;
            option_max[4][0] = 25;
            option_max[4][1] = 2000;
            option_max[5][0] = 19;
            option_max[5][1] = 170;
            option_max[6][0] = 27;
            option_max[6][1] = 18;
            option_max[7][0] = 37;
            option_max[7][1] = 100;
            option_max[8][0] = 38;
            option_max[8][1] = 1000;
            option_max[9][0] = 39;
            option_max[9][1] = 150;
            option_max[10][0] = 56;
            option_max[10][1] = 60;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 3;
        } else if (id == ItemName.VU_KHI_QUAT_100) {
            int[][] option_max = new int[11][2];
            option_max[0][0] = 0;
            option_max[0][1] = 600;
            option_max[1][0] = 1;
            option_max[1][1] = 600;
            option_max[2][0] = 9;
            option_max[2][1] = 170;
            option_max[3][0] = 10;
            option_max[3][1] = 70;
            option_max[4][0] = 26;
            option_max[4][1] = 2000;
            option_max[5][0] = 19;
            option_max[5][1] = 170;
            option_max[6][0] = 27;
            option_max[6][1] = 18;
            option_max[7][0] = 37;
            option_max[7][1] = 100;
            option_max[8][0] = 38;
            option_max[8][1] = 1000;
            option_max[9][0] = 39;
            option_max[9][1] = 150;
            option_max[10][0] = 56;
            option_max[10][1] = 60;
            int[][] option_min = NinjaUtils.getOptionShop(option_max);
            for (int i = 0; i < option_max.length; i++) {
                int param = isMaxOption ? option_max[i][1] : NinjaUtils.nextInt(option_min[i][1], option_max[i][1]);
                this.options.add(new ItemOption(option_max[i][0], param));
            }
            this.sys = 3;
        }
    }

    public boolean isSaveHistory() {
        return has(1000) || upgrade > 0
                || ((template.isTypeClothe() || template.isTypeAdorn() || template.isTypeWeapon()) && !gems.isEmpty())
                || template.type == ItemTemplate.TYPE_MON4 || template.type == ItemTemplate.TYPE_DRAGONBALL
                || template.isTypeNgocKham();
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", index=" + index +
                ", quantity=" + quantity +
                ", expire=" + expire +
                ", upgrade=" + upgrade +
                ", sys=" + sys +
                ", isLock=" + isLock +
                ", yen=" + yen +
                ", options=" + options +
                ", gems=" + gems +
                ", template=" + template +
                ", updatedAt=" + updatedAt +
                ", createdAt=" + createdAt +
                ", isNew=" + isNew +
                ", productID=" + productID +
                ", productUniqueId=" + productUniqueId +
                ", productSeller='" + productSeller + '\'' +
                ", productPrice=" + productPrice +
                ", productStatus=" + productStatus +
                ", productTime=" + productTime +
                ", productChanged=" + productChanged +
                '}';
    }

    // ---- accessor Lombok từng sinh, nay viết thẳng ra để dịch được không cần Lombok ----

    public long getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpire() {
        return this.expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public java.util.ArrayList<com.nsoz.item.Item> getGems() {
        return this.gems;
    }

    public void setGems(java.util.ArrayList<com.nsoz.item.Item> gems) {
        this.gems = gems;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }


    public void setOptions(java.util.ArrayList<com.nsoz.option.ItemOption> options) {
        this.options = options;
    }

    public boolean isProductChanged() {
        return this.productChanged;
    }

    public void setProductChanged(boolean productChanged) {
        this.productChanged = productChanged;
    }

    public int getProductID() {
        return this.productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public int getProductPrice() {
        return this.productPrice;
    }

    public void setProductPrice(int productPrice) {
        this.productPrice = productPrice;
    }

    public java.lang.String getProductSeller() {
        return this.productSeller;
    }

    public void setProductSeller(java.lang.String productSeller) {
        this.productSeller = productSeller;
    }

    public byte getProductStatus() {
        return this.productStatus;
    }

    public void setProductStatus(byte productStatus) {
        this.productStatus = productStatus;
    }

    public int getProductTime() {
        return this.productTime;
    }

    public void setProductTime(int productTime) {
        this.productTime = productTime;
    }

    public int getProductUniqueId() {
        return this.productUniqueId;
    }

    public void setProductUniqueId(int productUniqueId) {
        this.productUniqueId = productUniqueId;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public byte getSys() {
        return this.sys;
    }

    public void setSys(byte sys) {
        this.sys = sys;
    }

    public com.nsoz.item.ItemTemplate getTemplate() {
        return this.template;
    }

    public void setTemplate(com.nsoz.item.ItemTemplate template) {
        this.template = template;
    }

    public long getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public byte getUpgrade() {
        return this.upgrade;
    }

    public void setUpgrade(byte upgrade) {
        this.upgrade = upgrade;
    }

    public int getYen() {
        return this.yen;
    }

    public void setYen(int yen) {
        this.yen = yen;
    }

    public boolean isGiahan() {
        return this.isGiahan;
    }

    public void setGiahan(boolean isGiahan) {
        this.isGiahan = isGiahan;
    }

    public boolean isLock() {
        return this.isLock;
    }

    public void setLock(boolean isLock) {
        this.isLock = isLock;
    }

    public boolean isNew() {
        return this.isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * Chỉ số cho mấy bộ trang phục trang bị 2 tự thêm (1236 và 1241-1262), yêu cầu cấp 100.
     *
     * Dùng lại đúng bộ mã chỉ số mà máy chủ vốn đã dành cho trang phục trang bị 2 -- bộ
     * Fairies/Sixgirl/Buffalo xài đúng ngần ấy mã, chỉ khác giá trị -- nên client hiển thị y
     * như đồ có sẵn, không phải đụng gì tới phần vẽ bảng chỉ số.
     *
     * Bỏ hai thứ của bộ cũ: hạn ba ngày (đồ này vĩnh viễn) và mã 136 quay số ngẫu nhiên. Đổi lại
     * số nhỉnh hơn cho đúng tầm cấp 100 -- ba mảnh cộng lại hơn một bộ Fairies đủ bộ chừng bảy
     * phần mười, mà bộ kia lại hết hạn sau ba ngày.
     *
     * Mặt nạ đứng một mình (Pain, Madara) nên ăn trọn phần của mảnh đầu, giống cách bộ cũ tính
     * cho món cải trang.
     */
    private boolean initOptionTrangPhucMoi() {
        if (this.id != 1236 && (this.id < 1241 || this.id > 1262)) {
            return false;
        }
        if (this.template.type == ItemTemplate.TYPE_MATNA) {
            this.options.add(new ItemOption(82, 2000));    // HP tối đa
            this.options.add(new ItemOption(87, 2000));    // tấn công
            this.options.add(new ItemOption(69, 12));      // chí mạng
            this.options.add(new ItemOption(58, 25));      // cộng thêm tiềm năng: +25%
        } else {
            this.options.add(new ItemOption(125, 5000));   // HP tối đa
            this.options.add(new ItemOption(117, 5000));   // MP tối đa
            this.options.add(new ItemOption(94, 12));      // tấn công: +12%
            this.options.add(new ItemOption(127, 12));     // kháng sát thương hệ hoả
            this.options.add(new ItemOption(130, 12));     // kháng sát thương hệ băng
            this.options.add(new ItemOption(131, 12));     // kháng sát thương hệ phong
        }
        return true;
    }

    /**
     * Chỉ số cho Cầu lục đạo (1263 tím, 1264 đen), món trang bị 2 cấp 130.
     *
     * Bộ mặc định của áo choàng (HP 350, MP 350, né 100...) quá nhẹ so với cấp 130 -- nó vốn dành
     * cho áo choàng cấp 70. Bộ dưới đây chọn theo **công thức thật** trong Char.java chứ không theo
     * cảm tính:
     *
     *   - Chí mạng (mã 114 -> trường `fatal`): `fatal > nextInt(1000)`, tức **chia 10 ra phần
     *     trăm**. Cho 80 là +8% chí mạng.
     *   - Né đòn (mã 115 -> `miss`) CỐ Ý cho ít: khi đánh quái, máy chủ tính né bằng
     *     `nextInt(mob.level + 10)` -- **hoàn toàn không đọc chỉ số né của người chơi**. Né chỉ có
     *     tác dụng khi đánh nhau với người. Nhồi né vào món cày quái là ném điểm đi.
     *   - Sát thương chí mạng (105) đi cặp với chí mạng: tăng tỉ lệ mà không tăng lực thì phí.
     *
     * Hai màu lệch nhau một chút cho có tính cách: tím thiên sát thương, đen thiên chống chịu.
     */
    private boolean initOptionCauLucDao() {
        if (this.id != 1263 && this.id != 1264) {
            return false;
        }
        // Món cấp 130, chiếm ô trang bị 2 -- đặt ngang tầm vĩ thú siêu cấp (tấn công 5000-8000,
        // tiềm năng +20%) chứ không phải tầm trang phục thường, nếu không thì không ai đổi.
        //
        // Chỉ dùng những mã chỉ số máy chủ thật sự đọc. Đã dò lại từng mã:
        //   87  -> owner.damage      (AbilityFromEquip:171) cộng thẳng
        //   94  -> owner.damage      (:172) nhưng tính trên tiềm năng*4, không phải tổng sát thương
        //   58  -> nhân tiềm năng    (:142-146) nên ăn lan sang cả sát thương, chính xác và né
        //   113 -> dameHit           (Char:7443 đánh quái, :8337 đánh người) -- hiếm, ăn cả hai
        //   67  -> percentFatalDame  (:242)
        //   105 -> fatalDame         (:241)
        // Mã 103 "St lên người" và 101 "bỏ qua kháng" đều đã bị chú thích chết trong Char.java,
        // đặt vào chỉ tổ hiện chữ mà không có tác dụng gì.
        this.options.add(new ItemOption(125, this.id == 1263 ? 5000 : 9000)); // HP tối đa
        this.options.add(new ItemOption(117, 5000));                         // MP tối đa
        if (this.id == 1263) {                            // tím: dồn hết vào sát thương
            this.options.add(new ItemOption(58, 15));     // cộng thêm tiềm năng %
            this.options.add(new ItemOption(87, 6000));   // tấn công
            this.options.add(new ItemOption(94, 25));     // tấn công %
            this.options.add(new ItemOption(113, 800));   // sát thương chuẩn
            this.options.add(new ItemOption(114, 120));   // chí mạng 12%
            this.options.add(new ItemOption(67, 50));     // tấn công khi chí mạng %
            this.options.add(new ItemOption(105, 3000));  // sát thương chí mạng
        } else {                                          // đen: vẫn đánh đau, thêm phần chịu đòn
            this.options.add(new ItemOption(58, 10));
            this.options.add(new ItemOption(87, 4500));
            this.options.add(new ItemOption(94, 18));
            this.options.add(new ItemOption(113, 500));
            this.options.add(new ItemOption(114, 80));    // chí mạng 8%
            this.options.add(new ItemOption(116, 300));   // chính xác
            this.options.add(new ItemOption(124, 600));   // giảm trừ sát thương
            this.options.add(new ItemOption(121, 20));    // kháng sát thương chí mạng %
        }
        return true;
    }
}
