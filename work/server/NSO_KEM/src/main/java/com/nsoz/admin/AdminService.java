/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.admin;

import com.nsoz.clan.Clan;
import com.nsoz.constants.CMDInputDialog;
import com.nsoz.constants.CMDMenu;
import com.nsoz.constants.ItemName;
import com.nsoz.constants.NpcName;
import com.nsoz.convert.Converter;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.map.Map;
import com.nsoz.map.MapManager;
import com.nsoz.model.Char;
import com.nsoz.model.InputDialog;
import com.nsoz.model.Menu;
import com.nsoz.option.ItemOption;
import com.nsoz.server.AutoUpgrade;
import com.nsoz.server.GameData;
import com.nsoz.server.Ranked;
import com.nsoz.server.Server;
import com.nsoz.server.BestGear;
import com.nsoz.server.ServerManager;
import com.nsoz.skill.Skill;
import com.nsoz.stall.StallManager;
import com.nsoz.store.ItemStore;
import com.nsoz.store.MaxStore;
import com.nsoz.store.Store;
import com.nsoz.store.StoreManager;
import com.nsoz.util.NinjaUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class AdminService {

    private static final AdminService instance = new AdminService();

    public static AdminService getInstance() {
        return instance;
    }

    public void addEquipment(Char p, String[] args) {
        try {
            int level = -1;
            int upgrade = 0;
            int sys = p.getSys();
            int gender = p.gender;
            boolean max = true;
            int tl = 0;
            for (int i = 1; i < args.length; i++) {
                boolean hP = i + 1 <= args.length;
                if (hP) {
                    if (args[i].equals("lv")) {
                        level = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("up")) {
                        upgrade = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("he")) {
                        sys = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("max")) {
                        max = Integer.parseInt(args[++i]) == 1;
                    } else if (args[i].equals("nv")) {
                        gender = Integer.parseInt(args[++i]);
                    } else if (args[i].equals("tl")) {
                        tl = Integer.parseInt(args[++i]);
                    }
                }
            }
            if (level <= 10) {
                p.serverMessage("Hãy nhập level lớn hơn 10.");
                return;
            }
            if (upgrade < 0) {
                p.serverMessage("Hãy nhập upgrade lớn hơn 0.");
                return;
            }
            if (gender != 0 && gender != 1) {
                p.serverMessage("Giới tính là 0 hoặc 1");
                return;
            }
            if (tl < 0 || tl > 9) {
                p.serverMessage("Hãy nhập tinh luyện từ 0 đến 9");
                return;
            }
            if (sys < 0 || sys > 3) {
                p.serverMessage("Hệ không hợp lệ");
                return;
            }
            int sys2 = sys;
            if (level % 10 == 0) {
                sys2 = p.classId;
            }
            Item item = null;
            if (level >= 90 && level < 100) {
                int itemID = -1;
                int i = level % 10;
                switch (i) {
                    case 0:
                        switch (p.classId) {
                            case 1:
                                itemID = ItemName.THAI_DUONG_VO_CUC_KIEM;
                                break;
                            case 2:
                                itemID = ItemName.THAI_DUONG_THIEN_HOA_TIEU;
                                break;
                            case 3:
                                itemID = ItemName.THAI_DUONG_TANG_HON_DAO;
                                break;
                            case 4:
                                itemID = ItemName.THAI_DUONG_BANG_THAN_CUNG;
                                break;
                            case 5:
                                itemID = ItemName.THAI_DUONG_CHIEN_LUC_DAO;
                                break;
                            case 6:
                                itemID = ItemName.THAI_DUONG_HOANG_PHONG_PHIEN;
                                break;
                            default:
                                break;
                        }
                        break;

                    case 1:
                        if (gender == 1) {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_NGOA;
                        } else {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_NGOA_NU;
                        }
                        break;

                    case 2:
                        itemID = ItemName.THAI_DUONG_COT_NGOC_PHU;
                        break;

                    case 3:
                        if (gender == 1) {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_HA_GIAP;
                        } else {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_HA_GIAP_NU;
                        }
                        break;

                    case 4:
                        itemID = ItemName.THAI_DUONG_COT_NGOC_BOI;
                        break;

                    case 5:
                        if (gender == 1) {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_THU;
                        } else {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_THU_NU;
                        }
                        break;

                    case 6:
                        itemID = ItemName.THAI_DUONG_COT_NGOC_GIOI;
                        break;

                    case 7:
                        if (gender == 1) {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_GIAP;
                        } else {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_GIAP_NU;
                        }
                        break;

                    case 8:
                        itemID = ItemName.THAI_DUONG_COT_NGOC_LIEN;
                        break;

                    case 9:
                        if (gender == 1) {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_TUYEN;
                        } else {
                            itemID = ItemName.THAI_DUONG_COT_NGOC_TRAM;
                        }
                        break;

                }
                if (itemID != -1) {
                    item = ItemFactory.getInstance().newItem9X(itemID, max);
                }

            } else {
                ItemStore itemStore = StoreManager.getInstance().getEquipment(level, sys2, gender);
                if (itemStore != null) {
                    item = Converter.getInstance().toItem(itemStore,
                            max ? Converter.MAX_OPTION : Converter.RANDOM_OPTION);
                    p.addItemToBag(item);
                }
            }
            // option item
            if (item != null) {
                item.next(upgrade);
                item.setLock(true);
                if (tl > 0) {
                    ItemOption option = new ItemOption(85, 0);
                    item.options.add(option);
                    switch (item.template.type) {
                        case 0: {
                            int[] optionId = {95, 96, 97};
                            item.options.add(new ItemOption(optionId[item.sys - 1], 5));
                            item.options.add(new ItemOption(79, 5));
                            break;
                        }
                        case 1: {
                            item.options.add(new ItemOption(87, NinjaUtils.nextInt(250, 400)));
                            int[] optionId = {88, 89, 90};
                            item.options.add(new ItemOption(optionId[item.sys - 1], NinjaUtils.nextInt(350, 600)));
                            break;
                        }
                        case 2:
                            item.options.add(new ItemOption(80, NinjaUtils.nextInt(24, 28)));
                            item.options.add(new ItemOption(91, NinjaUtils.nextInt(10, 14)));
                            break;
                        case 3:
                            item.options.add(new ItemOption(81, 5));
                            item.options.add(new ItemOption(79, 5));
                            break;
                        case 4:
                            item.options.add(new ItemOption(86, NinjaUtils.nextInt(76, 124)));
                            item.options.add(new ItemOption(94, NinjaUtils.nextInt(76, 124)));
                            break;
                        case 5: {
                            int[] optionId = {95, 96, 97};
                            item.options.add(new ItemOption(optionId[item.sys - 1], 5));
                            item.options.add(new ItemOption(92, NinjaUtils.nextInt(9, 11)));
                            break;
                        }
                        case 6:
                            item.options.add(new ItemOption(83, NinjaUtils.nextInt(250, 450)));
                            item.options.add(new ItemOption(82, NinjaUtils.nextInt(250, 450)));
                            break;
                        case 7: {
                            int[] optionId = {95, 96, 97};
                            item.options.add(new ItemOption(optionId[item.sys - 1], 5));
                            optionId = new int[]{88, 89, 90};
                            item.options.add(new ItemOption(optionId[item.sys - 1], NinjaUtils.nextInt(350, 600)));
                            break;
                        }
                        case 8:
                            item.options.add(new ItemOption(83, NinjaUtils.nextInt(250, 450)));
                            item.options.add(new ItemOption(84, NinjaUtils.nextInt(76, 124)));
                            break;
                        case 9:
                            item.options.add(new ItemOption(84, NinjaUtils.nextInt(76, 124)));
                            item.options.add(new ItemOption(82, NinjaUtils.nextInt(250, 450)));
                            break;
                        default:
                            break;
                    }
                    for (int i = option.param; i < tl; i++) {
                        for (ItemOption option1 : item.options) {
                            if (option1.optionTemplate.type != 8 || option1.optionTemplate.id == 85) {
                                continue;
                            }
                            switch (option1.optionTemplate.id) {
                                case 94: {
                                    int[] percentIncreases = new int[]{10, 10, 10, 20, 20, 30, 40, 50, 60};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 86: {
                                    int[] percentIncreases = new int[]{25, 30, 35, 40, 50, 60, 80, 115, 165};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 87: {
                                    int[] percentIncreases = new int[]{50, 60, 70, 90, 130, 180, 250, 330,
                                            500};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 88:
                                case 89:
                                case 90: {
                                    int[] percentIncreases = new int[]{50, 70, 100, 140, 190, 250, 320, 400,
                                            500};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 92: {
                                    int[] percentIncreases = new int[]{5, 5, 5, 5, 5, 5, 10, 10, 20};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 95:
                                case 96:
                                case 97: {
                                    int[] percentIncreases = new int[]{5, 5, 5, 5, 5, 5, 10, 10, 15};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 82:
                                case 83: {
                                    int[] percentIncreases = new int[]{40, 60, 80, 100, 140, 220, 300, 420,
                                            590};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 84: {
                                    int[] percentIncreases = new int[]{25, 30, 35, 40, 50, 60, 80, 115, 165};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 79: {
                                    int[] percentIncreases = new int[]{1, 2, 2, 2, 2, 2, 3, 3, 4};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 81: {
                                    int[] percentIncreases = new int[]{1, 2, 2, 2, 2, 2, 3, 3, 4};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 80: {
                                    int[] percentIncreases = new int[]{5, 5, 5, 5, 10, 10, 15, 15, 20};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                case 91: {
                                    int[] percentIncreases = new int[]{5, 5, 5, 5, 5, 5, 10, 10, 15};
                                    option1.param += percentIncreases[option.param];
                                    break;
                                }
                                default:
                                    break;
                            }
                        }
                        option.param++;
                    }
                }
                p.addItemToBag(item);
            } else {
                p.serverMessage("Không tìm thấy vật phẩm này!");
            }
        } catch (NumberFormatException e) {
            p.serverMessage("Lệnh không hợp lệ! " + e.getMessage());
        }
    }

    // chỉ số ngọc
    public void addGem(Char p, String[] args) {
        int itemID = -1;
        int upgrade = 1;
        boolean max = true;
        for (int i = 1; i < args.length; i++) {
            boolean hP = i + 1 <= args.length;
            if (hP) {
                if (args[i].equals("id")) {
                    itemID = Integer.parseInt(args[++i]);
                } else if (args[i].equals("u")) {
                    upgrade = Integer.parseInt(args[++i]);
                } else if (args[i].equals("m")) {
                    max = Integer.parseInt(args[++i]) == 1;
                }
            }
        }
        if (itemID == -1) {
            p.serverMessage("Hãy nhập mã vật phẩm!");
            return;
        }
        if (itemID != ItemName.HUYEN_TINH_NGOC && itemID != ItemName.HUYET_NGOC && itemID != ItemName.LAM_TINH_NGOC
                && itemID != ItemName.LUC_NGOC) {
            p.serverMessage("Vật phẩm này không phải ngọc!");
            return;
        }
        if (upgrade < 1 || upgrade > 10) {
            p.serverMessage("Cấp ngọc từ 1 đến 10!");
            return;
        }
        Item item = ItemFactory.getInstance().newGem(itemID, max);
        item.setLock(true);
        for (int i = item.upgrade; i < upgrade; i++) {
            item.upgrade++;
            for (ItemOption option : item.options) {
                switch (option.optionTemplate.id) {
                    case 73:
                        // tấn công
                        if (option.param > 0) {
                            int[] paramUp = {0, 50, 100, 150, 200, 250, 300, 350, 400, 450};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 100;
                        }
                        break;
                    case 115:
                        // né đòn
                        if (option.param > 0) {
                            int[] paramUp = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 10;
                        }
                        break;
                    case 116:
                        // chính xác
                        if (option.param > 0) {
                            int[] paramUp = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 10;
                        }
                        break;
                    case 124:
                        // giảm trừ sát thương
                        if (option.param > 0) {
                            int[] paramUp = {0, 10, 15, 20, 25, 30, 35, 40, 45, 50};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 5;
                        }
                        break;
                    case 114:
                        // chí mạng
                        if (option.param > 0) {
                            int[] paramUp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 5;
                        }
                        break;
                    case 126:
                        // phản đòn
                        if (option.param > 0) {
                            int[] paramUp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 5;
                        }
                        break;
                    case 118:
                        // kháng tất cả
                        if (option.param > 0) {
                            int[] paramUp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 5;
                        }
                        break;
                    case 102:
                        // sát thương lên quái
                        if (option.param > 0) {
                            int[] paramUp = {0, 100, 200, 400, 600, 800, 1000, 1200, 1400, 1600};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 500;
                        }
                        break;
                    case 105:
                        // sát thương chí mạng
                        // lên 6 2600
                        if (option.param > 0) {
                            int[] paramUp = {0, 100, 200, 300, 400, 500, 600, 700, 900, 1200};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 500;
                        }
                        break;
                    case 103:
                        // sát thương lên người
                        // lên 4 900 , lên 5 1500 , lên 6 2300
                        if (option.param > 0) {
                            int[] paramUp = {0, 100, 200, 300, 400, 500, 600, 700, 900, 1200};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 200;
                        }
                        break;
                    case 121:
                        // kháng sát thương chí mạng
                        if (option.param > 0) {
                            int[] paramUp = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 5;
                        }
                        break;
                    case 117:
                    case 125:
                        // hp, mp tối đa
                        if (option.param > 0) {
                            int[] paramUp = {0, 100, 200, 300, 400, 500, 600, 700, 800, 900};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 50;
                        }
                        break;
                    case 119:
                    case 229:
                        // hồi phục hp,
                        // mp
                        if (option.param > 0) {
                            int[] paramUp = {0, 2, 4, 6, 8, 10, 12, 14, 16, 18};
                            option.param += paramUp[i];
                        } else {
                            option.param -= 10;
                        }
                        break;
                    case 123:
                        int[] giaKham = {800000, 1600000, 2400000, 3200000, 4800000, 7200000, 10800000, 15600000,
                                20100000, 28100000};
                        option.param = giaKham[i];
                        break;
                    default:
                        break;
                }
            }
        }
        p.addItemToBag(item);
    }

    public void setLevel(Char p, int level) {
        long exp = NinjaUtils.getExpFromLevel(level);
        exp -= p.exp;
        p.addExp(exp);
    }

    public void setSkillWithLevel(Char p, int level) {
        if (level > p.level) {
            p.serverMessage("Trình độ chưa đạt yêu cầu để học!");
            return;
        }
        Skill skill = GameData.getInstance().getSkillWithLevel(p.classId, level);
        if (skill != null) {
            if (!p.isHuman) {
                int skillTemplateID = skill.template.id;
                if (skillTemplateID >= 68 && skillTemplateID <= 72) {
                    p.serverMessage("Phân thân không thể học chiêu này.");
                    return;
                }
            }
            for (Skill my : p.vSkill) {
                if (my.template.id == skill.template.id) {
                    p.serverMessage("Chiêu này đã học!");
                    return;
                }
            }
            skill = Converter.getInstance().newSkill(skill);
            p.serverMessage("Học chiêu " + skill.template.name + " thành công.");
            p.vSkill.add(skill);
            if (skill.template.type == Skill.SKILL_AUTO_USE) {
                p.vSupportSkill.add(skill);
                p.setAbility();
            } else if ((skill.template.type == Skill.SKILL_CLICK_USE_ATTACK
                    || skill.template.type == Skill.SKILL_CLICK_LIVE
                    || skill.template.type == Skill.SKILL_CLICK_USE_BUFF
                    || skill.template.type == Skill.SKILL_CLICK_NPC)
                    && (skill.template.maxPoint == 0 || (skill.template.maxPoint > 0 && skill.point > 0))) {

                p.vSkillFight.add(skill);
            }
            p.getService().loadSkill();
        }
    }

    /**
     * Nâng cấp tự động, làm ngay trong game.
     *
     * Máy khách J2ME không sửa lại được (mã nguồn máy chủ cần Lombok để dịch, mà máy khách thì bị
     * làm rối tên), nhưng cũng không cần: máy chủ dựng được menu và hộp nhập trên máy người chơi.
     * Nên toàn bộ luồng dưới đây chỉ là ba bước menu -- chọn món, gõ mức muốn tới, chọn có bảo
     * hiểm hay không -- rồi gọi chung một AutoUpgrade với nút trên cửa sổ quản lý.
     */
    /** Các cửa hàng bán trang bị, cùng tên gọi dễ nhận ra hơn tên trong cơ sở dữ liệu. */
    /** Cửa hàng mở được bằng lệnh chat "shop", theo thứ tự hay dùng nhất. */
    private static final int[] QUICK_SHOP_TYPE = {
            StoreManager.TYPE_POTION, StoreManager.TYPE_FOOD, StoreManager.TYPE_MISCELLANEOUS,
            StoreManager.TYPE_WEAPON, StoreManager.TYPE_BOOK,
            StoreManager.TYPE_NECKLACE, StoreManager.TYPE_RING, StoreManager.TYPE_PEARL,
            StoreManager.TYPE_SPELL, StoreManager.TYPE_FASHION,
    };
    private static final String[] QUICK_SHOP_NAME = {
            "Dược phẩm", "Thức ăn", "Tạp hoá", "Vũ khí", "Sách",
            "Dây chuyền", "Nhẫn", "Ngọc bội", "Bùa", "Thời trang",
    };

    /**
     * Mở màn hình cửa hàng bằng hai gói tin, thiếu cái nào cũng hỏng:
     *
     *   store.show(p)  gửi danh sách hàng (lệnh 33). Máy khách chỉ CẤT mảng đó vào bộ nhớ theo
     *                  loại cửa hàng, không hiện gì cả.
     *   openUI(type)   mới là lệnh bảo máy khách bật màn hình cửa hàng lên.
     *
     * Đây đúng là cặp mà NPC bán hàng vẫn dùng (Char.npcKanata -> getService().openUI(...)).
     */
    private void openStore(Char p, int type) {
        Store store = StoreManager.getInstance().find((byte) type);
        if (store == null) {
            p.serverDialog("Không tìm thấy cửa hàng này.");
            return;
        }
        store.show(p);
        p.getService().openUI((byte) type);
    }

    /** Danh sách cửa hàng cho lệnh "shop": chọn một cái là mở ngay tại chỗ đang đứng. */
    /**
     * Đổi tỉ lệ kinh nghiệm toàn máy chủ. Việc đặt và ghi cấu hình nằm ở ExpAdmin, đây chỉ là
     * cửa vào bằng lệnh chat -- để nút bấm và lệnh chat không bao giờ hành xử khác nhau.
     */
    private void setExpRate(Char p, int rate) {
        if (rate < 1) {
            p.serverMessage("Tỉ lệ nhỏ nhất là 1.");
            return;
        }
        try {
            int told = com.nsoz.server.ExpAdmin.set(rate);
            p.serverMessage("Đã đặt x" + rate + " kinh nghiệm, báo cho " + told + " người.");
        } catch (Exception ex) {
            p.serverMessage("Không đặt được: " + ex);
        }
    }

    /**
     * Lệnh "item": đọc tham số rồi giao cho GiveItem.
     *
     * Số trần đứng ngay sau mã là số lượng, còn lại đi theo cặp khoá-giá trị giống lệnh "body"
     * sẵn có (up, he) để người quen lệnh cũ không phải nhớ thêm kiểu cú pháp thứ hai.
     */
    /**
     * Đổi giữa chủ thân và phân thân.
     *
     * Điều kiện chép đúng từ menu NPC Tajima chứ không nới ra: phân thân phải đang được triệu hồi,
     * chưa chết và còn hạn (timeCountDown > 0). Cờ isNhanBan lật khi đổi -- thân nào đang được điều
     * khiển thì cờ đó là false -- nên nó cũng là cách nhận biết mình đang ở thân nào.
     *
     * Nhắn tin báo lỗi phải gửi TRƯỚC khi đổi: sau lời gọi switchToMe thì p là cái thân vừa bị bỏ
     * lại, nhắn vào đó người chơi không thấy gì.
     */
    private void switchClone(Char p) {
        try {
            if (p.isHuman) {
                if (p.clone == null || !p.clone.isNhanBan || p.clone.isDead) {
                    p.serverMessage("Chưa triệu hồi phân thân.");
                    return;
                }
                if (p.timeCountDown <= 0) {
                    p.serverMessage("Phân thân đã hết hạn.");
                    return;
                }
                p.clone.switchToMe();
            } else if (!p.isNhanBan) {
                ((com.nsoz.model.CloneChar) p).human.switchToMe();
            } else {
                p.serverMessage("Không đổi được lúc này.");
            }
        } catch (Throwable ex) {
            System.out.println("switchClone: " + ex);
            p.serverMessage("Không đổi được: " + ex);
        }
    }

    private void giveItem(Char p, String[] args) {
        try {
            int id = Integer.parseInt(args[1]);
            int quantity = 1;
            int up = 0;
            int sys = -1;
            boolean boost = false;
            for (int i = 2; i < args.length; i++) {
                if (args[i].equals("vh")) {
                    boost = true;
                } else if (args[i].equals("up") && i + 1 < args.length) {
                    // "up max" là nâng tối đa của chính món đó, khỏi phải tra bảng.
                    up = args[i + 1].equals("max") ? -1 : Integer.parseInt(args[i + 1]);
                    i++;
                } else if (args[i].equals("he") && i + 1 < args.length) {
                    sys = Integer.parseInt(args[++i]);
                } else {
                    quantity = Integer.parseInt(args[i]);
                }
            }
            p.serverMessage(com.nsoz.server.GiveItem.give(p, id, quantity, up, sys, boost));
        } catch (NumberFormatException ex) {
            p.serverMessage("Cú pháp: item <mã> [số lượng] [up <n>] [he <n>] [vh]");
        }
    }

    private void shopMenu(Char p) {
        p.menus.clear();
        for (int i = 0; i < QUICK_SHOP_TYPE.length; i++) {
            final int type = QUICK_SHOP_TYPE[i];
            p.menus.add(new Menu(CMDMenu.EXECUTE, QUICK_SHOP_NAME[i], () -> {
                openStore(p, type);
            }));
        }
        p.getService().openUIMenu();
    }

    private static final int[] SHOP_TYPE = {
            StoreManager.TYPE_WEAPON, StoreManager.TYPE_MEN_HAT, StoreManager.TYPE_WOMEN_HAT,
            StoreManager.TYPE_MEN_SHIRT, StoreManager.TYPE_WOMEN_SHIRT,
            StoreManager.TYPE_MEN_GLOVES, StoreManager.TYPE_WOMEN_GLOVES,
            StoreManager.TYPE_MEN_PANT, StoreManager.TYPE_WOMEN_PANT,
            StoreManager.TYPE_MEN_SHOES, StoreManager.TYPE_WOMEN_SHOES,
            StoreManager.TYPE_NECKLACE, StoreManager.TYPE_RING,
            StoreManager.TYPE_PEARL, StoreManager.TYPE_SPELL };
    private static final String[] SHOP_NAME = {
            "Vũ khí", "Nón nam", "Nón nữ", "Áo nam", "Áo nữ", "Găng nam", "Găng nữ",
            "Quần nam", "Quần nữ", "Giày nam", "Giày nữ", "Liên", "Nhẫn", "Ngọc bội", "Phù" };

    /**
     * Cửa hàng đồ max: vẫn là màn hình cửa hàng thật của máy khách, có ảnh và chỉ số, chỉ khác ở
     * chỗ khi công tắc bật thì món mua ra lấy mức chỉ số cao nhất thay vì bốc ngẫu nhiên.
     */
    private void maxShopMenu(Char p) {
        p.menus.clear();
        boolean on = MaxStore.isOn(p);
        p.menus.add(new Menu(CMDMenu.EXECUTE,
                on ? "TẮT chế độ mua đồ max" : "BẬT chế độ mua đồ max", () -> {
            boolean now = MaxStore.toggle(p);
            p.serverDialog(now
                    ? "Đã bật. Từ giờ mọi thứ bạn mua ở cửa hàng trang bị đều max chỉ số."
                            + "\nNhớ tắt lại khi muốn chơi bình thường."
                    : "Đã tắt. Cửa hàng trở lại bốc chỉ số ngẫu nhiên như thường.");
        }));
        for (int i = 0; i < SHOP_TYPE.length; i++) {
            final int type = SHOP_TYPE[i];
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Mở: " + SHOP_NAME[i], () -> {
                openStore(p, type);
            }));
        }
        p.getService().openUIMenu();
    }

    private void autoUpgradeMenu(Char p) {
        p.menus.clear();
        int count = 0;
        for (int i = 0; i < p.numberCellBag; i++) {
            Item it = p.bag[i];
            if (!AutoUpgrade.canUpgrade(it)) {
                continue;
            }
            final int slot = i;
            final Item item = it;
            p.menus.add(new Menu(CMDMenu.EXECUTE,
                    item.template.name + " +" + item.upgrade + " (ô " + slot + ")",
                    () -> askTarget(p, slot)));
            count++;
        }
        if (count == 0) {
            p.serverDialog("Không có trang bị nào nâng cấp được trong hành trang."
                    + "\nMón đang mặc phải cởi ra bỏ vào túi mới nâng được.");
            return;
        }
        p.getService().openUIMenu();
    }

    private void askTarget(Char p, int slot) {
        Item item = p.bag[slot];
        if (!AutoUpgrade.canUpgrade(item)) {
            p.serverDialog("Món này không còn ở ô " + slot + " nữa.");
            return;
        }
        int upMax = item.template.getUpMax();
        p.setInput(new InputDialog(CMDInputDialog.EXECUTE,
                "Nâng " + item.template.name + " +" + item.upgrade + " lên mức (tối đa " + upMax + ")",
                () -> {
                    int want;
                    try {
                        want = p.getInput().intValue();
                    } catch (NumberFormatException e) {
                        p.serverDialog("Phải nhập một con số.");
                        return;
                    }
                    askInsurance(p, slot, want);
                }));
        p.getService().showInputDialog();
    }

    private void askInsurance(Char p, int slot, int target) {
        p.menus.clear();
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Có dùng bảo hiểm",
                () -> fire(p, slot, target, true)));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Không dùng (hỏng là tụt cấp)",
                () -> fire(p, slot, target, false)));
        p.getService().openUIMenu();
    }

    private void fire(Char p, int slot, int target, boolean insurance) {
        // Chạy ở luồng riêng: vòng nâng cấp ngủ giữa các lượt cho máy khách kịp vẽ, giữ nó trên
        // luồng đang xử lý gói tin thì cả kết nối của người chơi đứng hình theo.
        new Thread(() -> {
            AutoUpgrade.Result r = AutoUpgrade.run(p, slot, target, insurance, false, 50);
            StringBuilder b = new StringBuilder();
            b.append("+").append(r.from).append(" -> +").append(r.to)
             .append("\nSố lượt: ").append(r.tries)
             .append("\nĐá đã dùng: ").append(r.stones);
            if (r.insuranceUsed) {
                b.append("\nCó dùng bảo hiểm");
            }
            b.append("\n").append(r.stop);
            p.serverDialog(b.toString());
        }, "auto-upgrade-ingame").start();
    }

    public void openUIAdmin(Char p) {
        p.menus.clear();
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Thông tin", () -> {
            showServerInfo(p);
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Tìm người chơi", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Tìm người chơi", () -> {
                String text = p.getInput().getText();
                adminActionPlayer(p, text);
            }));
            p.getService().showInputDialog();
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Đi tới", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập ID MAP", () -> {
                InputDialog input = p.getInput();
                try {
                    int mapID = input.intValue();
                    Map map = MapManager.getInstance().find(mapID);
                    if (map != null) {
                        int zoneID = NinjaUtils.randomZoneId(mapID);
                        p.outZone();
                        short[] xy = NinjaUtils.getFirstPosition((short) mapID);
                        p.setXY(xy);
                        map.joinZone(p, zoneID);
                    } else {
                        p.serverDialog("Không tìm thấy map này!");
                    }
                } catch (Exception e) {
                    if (!input.isEmpty()) {
                        p.serverDialog(e.getMessage());
                    }
                }
            }));
            p.getService().showInputDialog();
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Lưu dữ liệu", () -> {
            NinjaUtils.setTimeout(() -> {
                Server.saveAllPlayer();
                Server.saveAll();
                p.serverDialog("Đã lưu dữ liệu!");
                System.out.println("Lưu dữ liệu người chơi xong");
            }, 0);
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Làm mới bxh", () -> {
            NinjaUtils.setTimeout(() -> {
                Ranked.refresh();
                p.serverDialog("Đã làm mới bxh!");
                System.out.println("Lưu dữ liệu người chơi xong");
            }, 0);
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Nâng cấp tự động", () -> {
            autoUpgradeMenu(p);
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE,
                "Cửa hàng đồ max" + (MaxStore.isOn(p) ? " [ĐANG BẬT]" : ""), () -> {
            maxShopMenu(p);
        }));

        p.menus.add(new Menu(CMDMenu.EXECUTE, "Bảo trì", () -> {
            NinjaUtils.setTimeout(() -> {
                Server.maintance();
                System.exit(0);
            }, 0);
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Trang bị", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Id item", () -> {
                String text = p.getInput().getText();
                if (!NinjaUtils.CheckString(text, "^[0-9]+$")) {
                    p.getService().serverDialog("Số  nhập vào không hợp lệ");
                    return;
                }
                int idItem = Integer.parseInt(text);
                // Danh sách và phần nhồi chỉ số nằm ở VuaHungList: phần cửa hàng giữ nguyên thứ
                // tự cũ, đồ cấp 90/100 nối vào cuối. Trả null là số vượt ngoài bảng -- trước đây
                // chỗ này gọi thẳng list.get() nên gõ quá tay là ném lỗi và menu im lặng.
                com.nsoz.item.Item itm = com.nsoz.server.VuaHungList.build(p, idItem);
                if (itm == null) {
                    int het = com.nsoz.server.VuaHungList.size(p);
                    p.getService().serverDialog("Danh sách chỉ có " + het
                            + " món, nhập từ 0 đến " + (het - 1)
                            + ".\nĐồ cấp 90 trở lên bắt đầu từ số "
                            + com.nsoz.server.VuaHungList.firstExtra(p) + ".");
                    return;
                }
                p.addItemToBag(itm);
            }));
            p.getService().showInputDialog();
        }));

        p.getService().openUIMenu();
    }

    public void showServerInfo(Char p) {
        long total, free, used;
        double mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        total = runtime.totalMemory();
        free = runtime.freeMemory();
        used = total - free;
//        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
//        double processCpuLoad = osBean.get() * 100;
        StringBuilder sb = new StringBuilder();
        sb.append("- Số luồng: ").append(Thread.activeCount()).append("\n\n");
//        sb.append("- CPU: ").append(String.format("%.2f%%",processCpuLoad)).append("\n\n");
        sb.append("- Số người đang online: ").append(ServerManager.getNumberOnline()).append("\n\n");
        sb.append("- Memory usage (JVM): ").append(String.format("%.1f/%.1f MB (%d%%)", used / mb, total / mb, (used * 100 / total))).append("\n\n");
        sb.append("- Update 22h\n\n");
        p.getService().showAlert("Thông tin", sb.toString());
    }

    public void adminActionPlayer(Char p, String name) {
        final Char player = ServerManager.findCharByName(name);
        if (player != null) {
            p.menus.clear();
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Thông tin", () -> {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("- Yên: %,d", player.yen)).append("\n");
                sb.append(String.format("- Xu: %,d", player.coin)).append("\n");
                sb.append(String.format("- Lượng: %,d", player.user.gold)).append("\n");
                p.getService().showAlert("Thông tin " + player.name, sb.toString());
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Khóa tài khoản", () -> {
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Thời hạn (giờ), bỏ trống nếu vĩnh viễn", () -> {
                    int hours = 0;
                    try {
                        hours = Integer.parseInt(p.getInput().getText());
                    } catch (Exception e) {
                    }
                    if (hours == 0) {
                        player.user.lock();
                    } else {
                        player.user.lock(hours);
                    }
                    p.serverMessage(String.format("Đã khóa %s!", player.name));
                }));
                p.getService().showInputDialog();
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Đi tới", () -> {
                if (player.zone != p.zone) {
                    p.outZone();
                    player.zone.join(p);
                } else {
                    p.getService().endDlg(true);
                }
                p.setXY(player.x, player.y);
                p.zone.getService().teleport(p);
            }));
            p.menus.add(new Menu(CMDMenu.EXECUTE, "Gửi vật phẩm", () -> { /// thêm buff item
                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập ID ITEM", () -> {
                    try {
                        int number = p.getInput().intValue();
                        sendItem(p, player, number);
                    } catch (NumberFormatException e) {
                        if (!p.getInput().isEmpty()) {
                            p.serverDialog(e.getMessage());
                        }
                    }
                }));
                p.getService().showInputDialog();
            }));
//            p.menus.add(new Menu(CMDMenu.EXECUTE, "Cộng yên", () -> {
//                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập số yên", () -> {
//                    try {
//                        int number = p.getInput().intValue();
//                        player.addYen(number);
//                    } catch (NumberFormatException e) {
//                        if (!p.getInput().isEmpty()) {
//                            p.serverDialog(e.getMessage());
//                        }
//                    }
//                }));
//                p.getService().showInputDialog();
//            }));
//            p.menus.add(new Menu(CMDMenu.EXECUTE, "Cộng xu", () -> {
//                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập số xu", () -> {
//                    try {
//                        int number = p.getInput().intValue();
//                        player.addCoin(number);
//                    } catch (NumberFormatException e) {
//                        if (!p.getInput().isEmpty()) {
//                            p.serverDialog(e.getMessage());
//                        }
//                    }
//                }));
//                p.getService().showInputDialog();
//            }));
//            p.menus.add(new Menu(CMDMenu.EXECUTE, "Cộng lượng", () -> {
//                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập số lượng", () -> {
//                    try {
//                        int number = p.getInput().intValue();
//                        player.addGold(number);
//                    } catch (NumberFormatException e) {
//                        if (!p.getInput().isEmpty()) {
//                            p.serverDialog(e.getMessage());
//                        }
//                    }
//                }));
//                p.getService().showInputDialog();
//            }));
//            p.menus.add(new Menu(CMDMenu.EXECUTE, "Level", () -> {
//                p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập cấp", () -> {
//                    try {
//                        int number = p.getInput().intValue();
//                        long exp = NinjaUtils.getExpFromLevel(number);
//                        exp -= p.exp;
//                        p.addExp(exp);
//                    } catch (NumberFormatException e) {
//                        if (!p.getInput().isEmpty()) {
//                            p.serverDialog(e.getMessage());
//                        }
//                    }
//                }));
//                p.getService().showInputDialog();
//            }));
            p.getService().openUIMenu();
        } else {
            p.serverDialog("Không tìm thấy người chơi này!");
        }
    }
    public void sendItem(Char p, Char player, int id) {   /// thêm buff item
        if (!p.user.isTien()) {
            return;
        }
        p.menus.clear();
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Không khóa", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập số lượng", () -> {
                int quantity = p.getInput().intValue();
                Item itemE = ItemFactory.getInstance().newItem(id);
                if (itemE.template.isUpToUp) {
                    itemE.setQuantity(quantity);
                    player.addItemToBag(itemE);
                } else {
                    for (int i = 0; i < quantity; i++) {
                        Item item = ItemFactory.getInstance().newItem(id);
                        item.setQuantity(1);
                        player.addItemToBag(item);
                    }
                }
            }));
            p.getService().showInputDialog();
        }));
        p.menus.add(new Menu(CMDMenu.EXECUTE, "Khóa", () -> {
            p.setInput(new InputDialog(CMDInputDialog.EXECUTE, "Nhập số lượng", () -> {
                int quantity = p.getInput().intValue();
                Item itemE = ItemFactory.getInstance().newItem(id);
                itemE.isLock = true;
                if (itemE.template.isUpToUp) {
                    itemE.setQuantity(quantity);
                    player.addItemToBag(itemE);
                } else {
                    for (int i = 0; i < quantity; i++) {
                        Item item = ItemFactory.getInstance().newItem(id);
                        item.isLock = true;
                        item.setQuantity(1);
                        player.addItemToBag(item);
                    }
                }
            }));
            p.getService().showInputDialog();
        }));
        p.getService().openUIMenu();
    }
    /**
     * Ai được dùng lệnh cấp đồ, cấp cấp độ.
     *
     * Sau khi mở lệnh chat cho mọi người chơi, cửa duy nhất còn lại là cột isTien trong bảng users
     * -- bật cho ai thì người đó thành quản trị, tắt là người chơi thường. Trước đây cửa này nằm
     * trong Char.chat và so cứng theo tên tài khoản, sửa được chỉ bằng cách vá bytecode.
     */
    private boolean isAdmin(Char p) {
        return p != null && p.user != null && p.user.isTien();
    }

    /**
     * Lệnh chat mở cho MỌI người chơi, chạy trước cửa quyền quản trị.
     *
     * Chỉ nhận những việc người chơi vốn đã tự làm được bằng đường trong game, lệnh ở đây chỉ đỡ
     * phải chạy tới nơi. Bất cứ thứ gì cấp phát, sửa chỉ số hay xem dữ liệu người khác thì phải
     * nằm ở {@link #process} sau cửa {@code isTien}, không được đưa lên đây.
     *
     * Không nuốt câu chat khi không có gì để làm: người chưa có phân thân gõ "tt" thì đó chỉ là
     * một câu nói bình thường, và nó hiện ra bình thường. Có thế mới không cướp mất một từ hay
     * dùng của cả máy chủ.
     */
    public boolean processPublic(Char p, String text) {
        if (p == null || text == null) {
            return false;
        }
        // tt   đổi qua lại chủ thân / thứ thân.
        //
        // Mở cho mọi người vì việc này người chơi nào cũng làm được sẵn: NPC Tajima ở làng Tone có
        // đúng hai mục "Thứ thân" và "Chủ thân", cùng hai lời gọi này và không đòi thêm điều kiện
        // nào. Lệnh chat chỉ bỏ đoạn chạy về làng Tone, không cho thêm quyền gì.
        if (text.equals("tt")) {
            if (p.isHuman) {
                if (p.clone != null && p.clone.isNhanBan && !p.clone.isDead && p.timeCountDown > 0) {
                    switchClone(p);
                    return true;
                }
                return false;
            }
            if (!p.isNhanBan) {
                switchClone(p);
                return true;
            }
            return false;
        }
        // gear            mặc bộ tốt nhất theo cấp hiện tại
        // gear <cấp>      theo cấp chỉ định
        // gear <cấp> <hệ> chọn luôn hệ của trang bị
        // Cờ "vh" (bộ chỉ số Vua Hùng) ĐÃ GỠ: nó phát đồ mạnh hơn hẳn bộ thường, biến lệnh
        // tiện ích thành đường lấy đồ khủng. Lệnh "gear" thường vẫn giữ nguyên.
        String[] args = text.split(" ");
        if (args[0].equals("gear")) {
            int level = p.level;
            byte sys = (byte) p.getSys();
            int so = 0;
            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("vh")) {
                    p.serverMessage("Lệnh 'gear vh' đã bị gỡ. Dùng 'gear' thường.");
                    return true;
                }
                // Người chơi gõ nhầm là chuyện thường, mà parseInt ném ra thì Char.chat nuốt luôn
                // câu nói và chỉ để lại một dòng log khó hiểu. Số hỏng thì coi như không có.
                Integer n = so(args[i]);
                if (n == null) {
                    continue;
                }
                if (so++ == 0) {
                    level = n.intValue();
                } else {
                    sys = (byte) n.intValue();
                }
            }
            p.serverMessage("Đang mặc đồ tốt nhất cấp " + level + "...");
            String report = BestGear.dress(p, level, sys, true, false);
            // Gửi từng dòng: hộp thoại của máy khách cắt chuỗi dài, mà dòng nào bỏ qua thì
            // lý do mới là thứ đáng đọc.
            for (String line : report.split("\n")) {
                if (!line.trim().isEmpty()) {
                    p.serverMessage(line);
                }
            }
            return true;
        }
        return false;
    }

    /** Số nguyên trong lệnh chat; null nếu không phải số, để nơi gọi tự quyết bỏ qua hay báo. */
    private static Integer so(String s) {
        try {
            return Integer.valueOf(Integer.parseInt(s));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean process(Char p, String text) {
        if (text.equals("tls") && p.user.isTien() && (p.user.username.equals("nsokeny"))) {
            MapManager.getInstance().talentShow.showMenu(p);
            return true;
        }

        String[] args = text.split(" ");
        // Nhóm lệnh cấp phát: chỉ quản trị. Người chơi thường gõ vào thì rơi xuống dưới và câu
        // chat hiện ra bình thường, không lộ là có lệnh ẩn.
        if (isAdmin(p)) {
            if (args[0].equals("body")) {
                addEquipment(p, args);
                return true;
            }
            if (args[0].equals("gem")) {
                addGem(p, args);
                return true;
            }
            if (args.length == 2 && args[0].equals("level")) {
                int level = Integer.parseInt(args[1]);
                setLevel(p, level);
                return true;
            }
            if (args.length == 2 && args[0].equals("skill")) {
                int level = Integer.parseInt(args[1]);
                setSkillWithLevel(p, level);
                return true;
            }
            // item <mã> [số lượng] [up <n>] [he <n>]
            // Lấy một vật phẩm bất kỳ theo mã trong danh sách vật phẩm.
            //   item 275        một Minh Mẫn Đan
            //   item 275 10     mười viên
            //   item 950 up 8   trang bị, nâng cấp +8 (up max là lấy mức tối đa của món)
            //   item 950 he 2   chọn hệ, món nào có nhiều dòng chỉ số theo hệ mới cần
            if (args.length >= 2 && args[0].equals("item")) {
                giveItem(p, args);
                return true;
            }
            // Lệnh "exp <số>" ĐÃ GỠ. Nó đổi tỉ lệ kinh nghiệm của CẢ máy chủ chỉ bằng một câu
            // chat -- gõ nhầm một con số là hỏng cân bằng của mọi người, mà không có bước xác
            // nhận nào. Cửa sổ "Kinh nghiệm" (ExpAdmin) vẫn làm được việc này, ở đó còn nhìn
            // thấy giá trị hiện tại trước khi đổi.
        }

        // Mở cửa hàng ở bất cứ đâu. Không cần NPC: store.show() đẩy thẳng màn hình cửa hàng
        // xuống máy khách, khác với việc bấm vào NPC vốn đòi NPC phải có mặt trong khu
        // (Char.menu -> zone.getNpc(...) -> "Không tìm thấy NPC này").
        if (text.equals("shop")) {
            shopMenu(p);
            return true;
        }
        // tt   đổi qua lại giữa chủ thân và phân thân (thứ thân).
        // Cùng hai lời gọi mà NPC Tajima dùng, chỉ bỏ đoạn chạy về làng Tone tìm ông ấy.
        //
        // KHÔNG đặt tên "pt": client đã có sẵn lệnh đó cho nhóm, mà lệnh của client xử lý ngay tại
        // máy khách và trả về true nên gói tin không bao giờ tới máy chủ -- đặt trùng là lệnh mới
        // vô hiệu mà không báo lỗi gì.
        if (text.equals("tt")) {
            switchClone(p);
            return true;
        }
        // Lệnh xem tình hình máy chủ: ai đang online, ở map nào. Không đổi gì trong game nên
        // an toàn để mở cho mọi người sau này.
        if (text.equals("sv")) {
            StringBuilder sb = new StringBuilder();
            int online = 0;
            for (Char c : ServerManager.getChars()) {
                // isCleaned/bag == null là nhân vật đã thoát nhưng vỏ chưa được dọn hết.
                if (c == null || !c.isHuman || c.isCleaned || c.bag == null) {
                    continue;
                }
                online++;
                sb.append("\n").append(c.name).append(" - cấp ").append(c.level)
                  .append(" - map ").append(c.mapId);
            }
            p.getService().serverDialog("Đang online: " + online + sb);
            return true;
        }
        // bang   bật/tắt bảng điều khiển nhân vật (trang web ở cổng 8765).
        //
        // Để mở được ngay trong game chứ không chỉ lúc máy chủ khởi động: nó là thứ chỉ dùng thi
        // thoảng, mà giữ một cổng nghe mở suốt thì không có lý do gì. Chạy trong tiến trình máy
        // chủ nên sửa được cả người đang online, khác với lúc chạy độc lập bằng
        // tools/run-charadmin.sh.
        if (text.equals("bang") && p.user.isTien()) {
            try {
                if (CharAdminHttp.isRunning()) {
                    CharAdminHttp.stop();
                    p.getService().serverDialog("Đã tắt bảng điều khiển nhân vật.");
                } else {
                    CharAdminHttp.startInServer(CharAdminHttp.PORT_MAC_DINH);
                    p.getService().serverDialog("Bảng điều khiển nhân vật:\nhttp://127.0.0.1:"
                            + CharAdminHttp.PORT_MAC_DINH + "/\n(và địa chỉ Tailscale của máy chủ)");
                }
            } catch (Exception ex) {
                p.getService().serverDialog("Không mở được: " + ex.getMessage());
            }
            return true;
        }
        if (text.equals("info")) {
            p.zone.getService().chat(p.id, "map: " + p.mapId + " x: " + p.x + " y: " + p.y);
            return true;
        }
        if (text.equals("admin") && p.user.isTien()) {
            openUIAdmin(p);
            return true;
        }
//        if (text.equals("lge")) {
//            p.isModeAdd = !p.isModeAdd;
//            if (p.isModeAdd) {
//                p.getService().serverDialog("Đã bật chế độ sáng tạo");
//            } else {
//                p.getService().serverDialog("Đã tắt chế độ sáng tạo");
//            }
//            return true;
//        }
//        if (text.equals("xxx") && p.user.isTien()) {
//            p.isModeCreate = !p.isModeCreate;
//            if (p.isModeCreate) {
//                p.getService().serverDialog("Đã bật chế độ sáng tạo");
//            } else {
//                p.isModeRemove = false;
//                p.getService().serverDialog("Đã tắt chế độ sáng tạo");
//            }
//            return true;
//        }
//        if (p.isModeCreate) {
//            if (text.equals("remove")) {
//                p.isModeRemove = !p.isModeRemove;
//                if (p.isModeRemove) {
//                    p.getService().serverDialog("Đã bật chế độ xóa quái");
//                } else {
//                    p.getService().serverDialog("Đã tắt chế độ xóa quái");
//                }
//            }
//            if (text.startsWith("addmob")) {
//                text = text.replace(" ", "").trim();
//                if (text.length() > 6) {
//                    int mobId = Integer.parseInt(text.substring(6));
//                    if (mobId > 256) {
//                        return true;
//                    }
//                    int id = p.zone.getMonsters().size();
//                    MobTemplate te = MobManager.getInstance().find(mobId);
//                    int t = 0;
//                    if (te.type == 4) {
//                        t = 40;
//                    }
//                    Mob monster = new Mob(id++, (short) mobId, te.hp, te.level, p.x, (short) (p.y - t), false,
//                            te.isBoss(), p.zone);
//                    p.zone.addMob(monster);
//                }
//                return true;
//            }
//            if (text.equals("save")) {
//                JSONArray json = new JSONArray();
//                List<Mob> mobs = p.zone.getMonsters();
//                for (Mob mob : mobs) {
//                    // if (mob.template.isBoss()) {
//                    // continue;
//                    // }
//                    JSONObject obj = new JSONObject();
//                    obj.put("templateId", mob.template.id);
//                    obj.put("x", mob.x);
//                    obj.put("y", mob.y);
//                    obj.put("status", 1);
//                    json.add(obj);
//                }
//                NinjaUtils.saveFile("mob.txt", json.toJSONString().getBytes());
//                p.getService().serverDialog("Đã lưu thành công");
//                return true;
//            }
//        }
        return false;
    }
}
