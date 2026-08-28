package com.nsoz.mob;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.nsoz.server.SpawnBossManager;
import org.jetbrains.annotations.NotNull;
import com.nsoz.constants.ItemName;
import com.nsoz.constants.MapName;
import com.nsoz.constants.MobName;
import com.nsoz.constants.TaskName;
import com.nsoz.convert.Converter;
import com.nsoz.effect.Effect;
import com.nsoz.event.Event;
import com.nsoz.event.Halloween;
import com.nsoz.event.KoroKing;
import com.nsoz.event.Noel;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.lib.RandomCollection;
import com.nsoz.map.item.ItemMap;
import com.nsoz.map.item.ItemMapFactory;
import com.nsoz.map.world.Territory;
import com.nsoz.map.zones.Zone;
import com.nsoz.model.Char;
import com.nsoz.model.Figurehead;
import com.nsoz.model.RandomItem;
import com.nsoz.party.Group;
import com.nsoz.server.GlobalService;
import com.nsoz.server.ServerManager;
import com.nsoz.server.SpawnBossManager;
import com.nsoz.store.ItemStore;
import com.nsoz.store.StoreManager;
import com.nsoz.task.GloryTask;
import com.nsoz.task.TaskOrder;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Mob {

    public static final byte YEN = 0;
    public static final byte ITEM = 1;
    public static final byte ITEM_TASK = 2;
    public static final byte SUSHI = 3;
    public static final byte BOSS = 4;

    public static final byte EQUIP = 5;
    public static final byte LANG_CO = 6;

    public static final byte VDMQ = 8;
    public static final byte EVENT = 7;
    public static final byte LANG_TRUYEN_THUYET = 9;
    public static final byte CHIEN_TRUONG = 10;
    public static final byte CHIA_KHOA_CO_QUAN = 11;
    public static final byte LAM_THAO_DUOC = 12;
    public static final byte BOSS_LDGT = 13;
    public static final byte LANH_DIA_GIA_TOC = 14;
    public static final byte BI_MA = 15;
    public static final byte VKHD3x = 16; // ecuong fix
    public static final byte VKBOSS = 17; // ecuong fix
    public static final byte SVCBOSS = 18; // ecuong fix
    public static final byte MAP_BOSS_EVENT = 19; // ecuong fix
    public static final byte OAN_HON_EVENT = 20; // ecuong fix
    public static final byte LUONG = 21; // ecuong fix
    public static final byte PHIEU_MAY_MAN = 22;
    public static final byte PHAN_THAN_LENH = 23;
    public static final byte BOSS_VDMQ = 24;
    public static final byte BOSS_VDMQ_RBN = 25;
    public static final byte BOSS_LANG_CO_TTTS = 26;
    public static final byte BOSS_LANG_CO_TTTT = 27;
    public static final byte BOSS_LANG_CO_CTT = 28;
    public static final byte BOSS_COHON = 29;
    public static final byte DA_9 = 30;
    public static final byte MANH_SACH_MOI = 31; // skill 125
    public static final byte RUONG_MA_QUAI = 31;
    /**
     * Ba boss cấp 130 (Tử Hạ Ma Thần, Tướng giặc, Mỹ Hầu Vương) ăn bao nhiêu phần trăm sát thương
     * mà công thức chung tính ra. NÚM CHỈNH DUY NHẤT -- sửa số này là xong, không phải tìm chỗ khác.
     *
     * Công thức chung cho cấp 130 ra 70.200. Trước đây chỗ này để 5, tức chỉ còn 3.510 -- thấp hơn
     * cả boss cấp 45. Sau khi nhân hệ số map của Làng truyền thuyết (x4) thì người chơi chỉ ăn
     * 14.040, trong khi boss LÀNG CỔ cấp 100 đánh 168.000. Boss cấp cao nhất game lại vô hại nhất.
     *
     * Để 75: gốc thành 52.650, vào Làng truyền thuyết (x4) là 210.600 -- cao hơn boss làng cổ
     * khoảng 25%, tức đúng ngôi mạnh nhất mà không nhảy vọt lên 280.800 như khi bỏ hẳn mức cắt.
     * Ở Quỷ Ngục (không nhân hệ số) Tướng giặc thành 52.650.
     *
     * NHỚ: đây là số TRƯỚC khi nhân hệ số map trong Mob.attack -- Làng cổ / Fujuka / Làng truyền
     * thuyết x4, Vùng đất ma quỷ x2, hang động x2, đấu trường/lôi đài x0,1. Đổi số ở đây thì nhân
     * lên bốn lần mới ra thứ người chơi thật sự hứng.
     */
    public static final int PHAN_TRAM_DAME_BOSS_130 = 75;

    public static final byte MANH_NON_100_NAM = 32;
    public static final byte MANH_AO_100_NAM = 33;
    public static final byte MANH_GANG_100_NAM = 34;
    public static final byte MANH_QUAN_100_NAM = 35;
    public static final byte MANH_GIAY_100_NAM = 36;
    public static final byte MANH_NON_100_NU = 37;
    public static final byte MANH_GANG_100_NU = 38;
    public static final byte MANH_QUAN_100_NU = 39;
    public static final byte MANH_GIAY_100_NU = 40;
    public static final byte MANH_PHU_100 = 41;
    public static final byte MANH_BOI_100 = 42;
    public static final byte MANH_NHAN_100 = 43;
    public static final byte MANH_LIEN_100 = 44;
    public static final byte MANH_AO_100_NU = 45;
    public static final byte LANG_DIA_NGUC = 46; // lang dia nguc
    public int id;
    public boolean isDisable;
    public boolean isDontMove;
    public boolean isFire;
    public boolean isIce;
    public boolean isWind;
    public byte sys;
    public int hp;
    public int maxHP;
    public int originalHp;
    public short level;
    public short x;
    public short y;
    public MobTemplate template;
    public byte status;
    public byte levelBoss;
    public boolean isBoss;
    public long lastTimeAttack;
    public long attackDelay = 3000;
    public int recoveryTimeCount;
    public Vector<Integer> chars;
    public boolean isDead;
    public int damageOnPlayer, damageOnPlayer2;
    public int damageOnMob, damageOnMob2;
    public Zone zone;
    public ItemMap itemMap;
    public boolean isBusyAttackSokeOne;
    public boolean isCantRespawn;
    public Lock lock = new ReentrantLock();
    public Hashtable<Byte, Effect> effects = new Hashtable<>();
    private boolean isBeast;

    // Ba hàm dưới trước đây do Lombok sinh ra từ @Setter/@Getter. Bỏ Lombok đi vì máy này không
    // có lombok.jar và dự án chạy ngoại tuyến, nên nap.sh (dịch bằng javac trần) không xử lý được
    // annotation -- sửa bất cứ dòng nào trong tệp này cũng không dịch nổi. Viết tay ba hàm thì
    // tệp tự dịch được, không phụ thuộc thư viện ngoài.
    //
    // Giữ ĐÚNG tên Lombok từng sinh: isBeast() chứ không phải getBeast(), setBeast() chứ không
    // phải setIsBeast() -- Lombok cắt tiền tố "is" của trường boolean khi đặt tên setter. Đổi tên
    // là bốn chỗ gọi bên MobFactory/WarMobFactory/TerritoryMobFactory/Zone gãy theo.
    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public void setBeast(boolean isBeast) {
        this.isBeast = isBeast;
    }

    public boolean isBeast() {
        return this.isBeast;
    }
    @NotNull
    private final HashMap<@NotNull Char, @NotNull Integer> nFight; // fix dame boss

    public Mob(short templateId, boolean isBoss) {
        this.id = -1;
        this.template = MobManager.getInstance().find(templateId);
        this.isBoss = isBoss;
        this.hp = 0;
        this.maxHP = 0;
        this.isDisable = false;
        this.isDontMove = false;
        this.isFire = false;
        this.isIce = false;
        this.isWind = false;
        this.sys = 1;
        this.status = 4;
        this.levelBoss = 0;
        this.zone = null;
        this.nFight = new HashMap<>(); // fix dame boss
    }

    public Mob(int id, short templateId, int hp, short level, short x, short y, boolean isBeast, boolean isBoss) {
        this.id = id;
        this.template = MobManager.getInstance().find(templateId);
        this.originalHp = hp;
        this.level = level;
        this.x = x;
        this.y = y;
        this.isDisable = false;
        this.isDontMove = false;
        this.status = 5;
        this.isBoss = isBoss;
        this.levelBoss = 0;
        this.isFire = this.isIce = this.isWind = false;
        this.isDead = false;
        this.nFight = new HashMap<>(); // fix dame boss
        this.chars = new Vector<>();
        setBeast(isBeast);
        setClass();
        if (templateId == MobName.HEO_RUNG || templateId == MobName.HEO_MOI || zone.tilemap.isThatThuAi()) {
            this.isCantRespawn = true;
        }
        setLevelBoss();
        setHP();
        setDamage();
    }

    public Mob(int id, short templateId, int hp, short level, short x, short y, boolean isBeast, boolean isBoss, Zone zone) {
        this.id = id;
        this.template = MobManager.getInstance().find(templateId);
        this.originalHp = hp;
        this.level = level;
        this.x = x;
        this.y = y;
        this.isDisable = false;
        this.isDontMove = false;
        this.status = 5;
        this.isBoss = isBoss;
        this.levelBoss = 0;
        this.isFire = this.isIce = this.isWind = false;
        this.isDead = false;
        this.nFight = new HashMap<>(); // fix dame boss
        this.chars = new Vector<>();
        setZone(zone);
        setBeast(isBeast);
        setClass();
        if (templateId == MobName.HEO_RUNG || templateId == MobName.HEO_MOI || zone.tilemap.isThatThuAi() || zone.tilemap.isMapVithu()) {
            this.isCantRespawn = true;
        }
        setLevelBoss();
        setHP();
        setDamage();
    }

    public void setClass() {
        this.sys = (byte) NinjaUtils.nextInt(1, 3);
    }

    public void setLevelBoss() {
        if (template.id == MobName.HEO_RUNG || template.id == MobName.HEO_MOI) {
            this.levelBoss = 2;
        } else if (template.id == MobName.MOC_NHAN) {
            this.levelBoss = 0;
        } else if (zone.tilemap.isDungeo()) {
            this.levelBoss = 0;
            if (zone.tilemap.id == 116) {
                if (this.id == 82 || this.id == 85) {
                    this.levelBoss = 1;
                }
            } else {
                if (template.id == MobName.NINJA_HAC_AM || template.id == MobName.THIEN_VUONG || template.id == MobName.NGAN_LANG_VUONG) {
                    this.levelBoss = 2;
                }
            }
        } else if (zone.tilemap.isDungeoClan()) {
            this.levelBoss = 0;
            if (this.template.id == MobName.BAO_QUAN) {
                this.levelBoss = 2;
            }
        } else if (zone.tilemap.id == MapName.DIA_DAO_CHIKATOYA || zone.tilemap.id == MapName.THAT_THU_AI) {
            this.levelBoss = 0;
        } else {
            if (this.levelBoss == 3) {
                return;
            }
            if (isBeast) {
                this.levelBoss = 3;
            } else if (zone.numberChief < 1 && NinjaUtils.nextInt(100) == 1 && this.level >= 10 && !this.isBoss) {
                this.levelBoss = 2;
                zone.numberChief++;
            } else if (zone.numberElitez < 2 && NinjaUtils.nextInt(50) == 1 && this.level >= 10 && !this.isBoss) {
                this.levelBoss = 1;
                zone.numberElitez++;
            } else {
                this.levelBoss = 0;
            }
        }
    }

    // dame boss
    public void setDamage() {
        this.damageOnPlayer = (int) (this.level + (Math.pow(this.level, 2) / 5));
        if (this.isBoss) {
            this.damageOnPlayer *= 20;
        } else if (this.levelBoss == 1) {
            this.damageOnPlayer *= 2;
        } else if (this.levelBoss == 2) {
            this.damageOnPlayer *= 3;
        }
        if (this.template.id == MobName.CO_HON) {
            int level = 130;
            damageOnPlayer = ((int) (level + (Math.pow(level, 2) / 5))) * 20;
        }
        if (this.template.id == MobName.TU_HA_MA_THAN || this.template.id == MobName.TUONG_GIAC
                || this.template.id == MobName.MY_HAU_VUONG) {
            this.damageOnPlayer = this.damageOnPlayer / 100 * PHAN_TRAM_DAME_BOSS_130;
        }
        this.damageOnPlayer2 = this.damageOnPlayer - this.damageOnPlayer / 10;
    }

    public void setHP() {
        if (this.levelBoss == 1) {
            this.hp = this.maxHP = this.originalHp * 10;
        } else if (this.levelBoss == 2) {
            this.hp = this.maxHP = this.originalHp * 100;
        } else if (this.levelBoss == 3) {
            this.hp = this.maxHP = this.originalHp * 200;
        } else {
            this.hp = this.maxHP = this.originalHp;
        }
        if (template.id == MobName.HEO_RUNG || template.id == MobName.HEO_MOI) {
            this.hp = this.maxHP = this.originalHp;
        }
        if (this.maxHP < 0) {
            this.hp = this.maxHP = Integer.MAX_VALUE;
        }
    }

    public void recovery() {
        this.itemMap = null;
        this.isDead = false;
        setClass();
        setLevelBoss();
        setHP();
        setDamage();
        this.status = 5;
        this.isFire = false;
        this.isIce = false;
        this.isWind = false;
        this.isDontMove = false;
        this.isDisable = false;
        this.effects.clear();
        this.ClearFight(); // fix dame boss
    }

    public void die() { /// dev fix 13/8
        switch (this.levelBoss) {
            case 3:
                break;
            case 2:
                zone.numberChief--;
                break;
            case 1:
                zone.numberElitez--;
                break;
            default:
                break;
        }
        if (zone.numberChief < 0) {
            zone.numberChief = 0;
        }
        if (zone.numberElitez < 0) {
            zone.numberElitez = 0;
        }
        this.hp = 0;
        this.status = 0;
        this.isDead = true;
        this.recoveryTimeCount = 20;
        if (isBeast) {
            this.recoveryTimeCount = 300;
        }
        if (this.zone.tilemap.isChienTruong()) {
            this.recoveryTimeCount = 300;
            if (this.template.id == MobName.BACH_LONG_TRU || this.template.id == MobName.HAC_LONG_TRU) {
                this.recoveryTimeCount += 300;
            }
        } else if (this.template.id == MobName.HOP_BI_AN) {
            this.recoveryTimeCount = 65;
        } else if (this.template.id == MobName.NGUOI_TUYET || this.template.id == MobName.CHUOT_CANH_TY) {
            this.recoveryTimeCount = 900;
        } else if (this.zone.tilemap.isDungeo9x()) {
            if (!this.isBoss) {
                this.recoveryTimeCount = 40;
            }
        }
        this.chars.clear();
        if (this.template.id == 237 && this.zone.map.id == 169) {
            this.handleAfterKillBoss();
        }
    }

    public void handleAfterKillBoss() {
        int x = 0;
        int y = 0;
        try {
            this.zone.map.vithu.isDieVithu = true;
            this.zone.map.vithu.timeVithu = System.currentTimeMillis() + 600000L;
            GlobalService.getInstance().chat("server", "Boss Thập Vĩ đã bị tiêu diệt. Hang Karasumori sẽ đóng lại sau 10 phút.");
            for (Char player : this.zone.map.vithu.users) {
                int zoneid = 0;
                if (player.level >= 50 && player.level <= 70) {
                    zoneid = 1;
                } else if (player.level <= 100) {
                    zoneid = 2;
                } else if (player.level <= 130) {
                    zoneid = 3;
                } else if (player.level <= 150) {
                    zoneid = 4;
                }
                if (this.zone.getNumberChar() < 99) {
                    player.setTypePk(Char.PK_PHE);
                    player.outZone();
                    player.joinZone(this.zone.map.id, zoneid, -1);
                }
            }
            x = this.x;
            y = this.y;
            for (int i = 1; i < 5; i++) {
                for (int j = 0; j < 10; j++) {
                    LeaveItem(this.zone.map.getZoneById(i), ItemName.TRUNG_VI_THU, NinjaUtils.nextInt(x - 200, x + 200), y, 1);
                }
            }
        } catch (IOException ex) {
            Log.logException("Lỗi kill boss handleAfterKillBoss() : ", Mob.class, ex);

        }
    }

    public ItemMap LeaveItem(Zone zone, int id, int njX, int njY, int quantity) throws IOException {//vithu

        int rand = 0;
        if (zone.getNumberItem() > 100) {
            return null;
        }
        Item data = ItemFactory.getInstance().newItem(id);

        ItemMap itemMap = ItemMapFactory.getInstance().builder().id(zone.numberDropItem++).x((short) njX).y((short) njY).build();
        if (data != null) {
            itemMap.setItem(data);
            zone.addItemMap(itemMap);
            zone.getService().addItemMap(itemMap);
        }
        return itemMap;
    }

    public int randomItemID() {
        int itemID = RandomItem.ITEM.next();
        if (!isBoss && itemID == ItemName.DA_CAP_1) {
            itemID = this.level / 15;
            itemID = itemID > ItemName.DA_CAP_5 ? ItemName.DA_CAP_5 : itemID;
        } else if (itemID == ItemName.BINH_HP_CUC_TIEU) {
            if (this.level < 10) {
                itemID = ItemName.BINH_HP_CUC_TIEU;
            } else if (this.level < 30) {
                itemID = ItemName.BINH_HP_TIEU;
            } else if (this.level < 40) {
                itemID = ItemName.BINH_HP_VUA;
            } else if (this.level < 70) {
                itemID = ItemName.BINH_HP_LON;
            } else if (this.level < 90) {
                itemID = ItemName.BINH_HP_CUC_LON;
            } else {
                itemID = ItemName.BINH_HP_CAO_CAP;
            }
        } else if (itemID == ItemName.BINH_MP_CUC_TIEU) {
            if (this.level < 10) {
                itemID = ItemName.BINH_MP_CUC_TIEU;
            } else if (this.level < 30) {
                itemID = ItemName.BINH_MP_TIEU;
            } else if (this.level < 40) {
                itemID = ItemName.BINH_MP_VUA;
            } else if (this.level < 70) {
                itemID = ItemName.BINH_MP_LON;
            } else if (this.level < 90) {
                itemID = ItemName.BINH_MP_CUC_LON;
            } else {
                itemID = ItemName.BINH_MP_CAO_CAP;
            }
        }
        return itemID;
    }

    public void dropItem(Char owner, byte type, Char master) { // fix dame boss
        try {

            if (zone.getNumberItem() > 100) {
                return;
            }
            Item itm = null;
            int itemId = 0;
            if (type == ITEM) {
                itemId = randomItemID();
            } else if (type == LANG_CO) {
                if (owner.user.activated == 0) {
                    return;
                }
                itemId = RandomItem.LANG_CO.next();
                if (this.levelBoss == 1 && NinjaUtils.nextInt(1000) == 0) {
                    itemId = ItemName.HARLEY_DAVIDSON;
                }
            } else if (type == LANG_TRUYEN_THUYET) {

                itemId = RandomItem.LANG_TRUYEN_THUYET.next();
                if (itemId == ItemName.HOAN_CHUYEN_CAO_CAP || itemId == ItemName.BAO_HIEM_CAO_CAP) {
                    return;
                }
                if (this.levelBoss == 1 && NinjaUtils.nextInt(1000) == 0) {
                    itemId = ItemName.HARLEY_DAVIDSON;
                }

            } else if (type == LANG_DIA_NGUC) { // lang dia nguc
                itemId = RandomItem.LANG_DIA_NGUC.next();
                if (itemId == ItemName.HOAN_CHUYEN_CAO_CAP || itemId == ItemName.BAO_HIEM_CAO_CAP) {
                    return;
                }
                if (zone.tilemap.isLangDiaNguc()) {
                    // Tinh Anh Thủ Lĩnh (levelBoss == 1) sẽ rơi xu 200 và vàng 2 lượng
                    if (this.levelBoss == 1) {
                        owner.addCoin(200);  // Tinh Anh Thủ Lĩnh luôn rơi 200 xu
                        owner.addGold(2);    // Tinh Anh Thủ Lĩnh luôn rơi 2 lượng vàng
                    } // Thủ Lĩnh (levelBoss == 2) sẽ rơi xu 300 và vàng 3 lượng
                    else if (this.levelBoss == 2) {
                        owner.addCoin(500);  // Thủ Lĩnh luôn rơi 500 xu
                        owner.addGold(5);    // Thủ Lĩnh luôn rơi 5 lượng vàng
                    } // Các quái khác sẽ rơi xu và vàng ngẫu nhiên
                    else {
                        owner.addCoin(NinjaUtils.nextInt(20, 100)); // Xu từ quái thường
                        owner.addGold(NinjaUtils.nextInt(1, 2));   // Lượng vàng từ quái thường
                    }
                    // Kiểm tra tỷ lệ rơi item đặc biệt cho boss level 1
                    if (this.levelBoss == 1 && NinjaUtils.nextInt(1000) == 0) {
                        itemId = ItemName.HARLEY_DAVIDSON;  // Rơi item đặc biệt cho Tinh Anh Thủ Lĩnh
                    }
                }


            } else if (type == VDMQ) {
                if (owner.user.activated == 0) {
                    return;
                }
                itemId = RandomItem.VDMQ.next();
                if (itemId == ItemName.PHAN_THAN_LENH && this.level < 100) {
                    return;
                }
                if (itemId == ItemName.OAN_HON_SK && !owner.isMayDo) {
                    return;
                }
            } else if (type == MAP_BOSS_EVENT) {
                itemId = RandomItem.BOSS_SU_KIEN.next();
            } else if (type == LANH_DIA_GIA_TOC) {
                itemId = RandomItem.LANH_DIA_GIA_TOC.next();
            } else if (type == BOSS_LDGT) {
                itemId = RandomItem.BOSS_LDGT.next();
            } else if (type == CHIEN_TRUONG) {
                itemId = 846;// chìa khóa
            } else if (type == CHIA_KHOA_CO_QUAN) {
                itemId = ItemName.CHIA_KHOA_LANH_DIA_GIA_TOC;// chìa khóa
            } else if (type == LAM_THAO_DUOC) {
                itemId = ItemName.LAM_THAO_DUOC;// lam thảo dược
            } else if (type == YEN) {
                if (!isBoss && this.levelBoss != 1 && this.levelBoss != 2 && Math.abs(owner.level - (int) this.level) > 7) {  /// chỉnh ta tl ko giới hạn level
                    return;
                }
                if (!isBoss && zone.tilemap.isDungeo9x()) {
                    return;
                }

                itemId = ItemName.YEN;
            } else if (type == LUONG) {
                int luong = 0;
                if (this.levelBoss == 1) {
                    luong = 2;
                } else if (this.levelBoss == 2) {
                    luong = 3;
                } else {
                    luong = 1;
                }

                owner.addGold(luong);
                owner.serverMessage("Bạn nhận được " + luong + " lượng .");
                owner.addTopThanNong(luong);
            } else if (type == ITEM_TASK) {
                itemId = owner.getIdItemTask(template.id);
                if (itemId == -1) {
                    return;
                }
            } else if (type == EVENT) {
                itemId = Event.getEvent().randomItemID();
                if (itemId == -1) {
                    return;
                }
            } else if (type == BI_MA) {
                Halloween halloween = (Halloween) Event.getEvent();
                itemId = halloween.randomItemID2();
            } else if (type == SUSHI) {
                itemId = ItemName.SUSHI;
            } else if (type == BOSS) {
                if (zone.map.id == 167) {
                    itemId = RandomItem.BOSS_LDGT.next();
                } else if (zone.map.id >= 162) {
                    itemId = RandomItem.BOSS_LANG_TRUYEN_THUYET.next();

                } else if (zone.map.id >= 177) {
                    itemId = RandomItem.BOSS_LANG_DIA_NGUC.next(); // lang dia nguc

                } else if (zone.map.id == MapName.DONG_HACHI || zone.map.id == MapName.KY_TUC_XA_HARUNA || zone.map.id == MapName.SONG_BANG_YAMATO) {
                    itemId = RandomItem.BOSS_SU_KIEN.next();
                } else {

                    itemId = RandomItem.BOSS.next();


                }
            } else if (type == BOSS_VDMQ) {

                itemId = RandomItem.BOSS_VDMQ.next();

            } else if (type == BOSS_COHON) {
                int rand = NinjaUtils.nextInt(0, 100);
                if (rand <= 5) {
                    itemId = ItemName.XICH_NHAN_NGAN_LANG;
                }


            } else if (type == DA_9) {

                itemId = ItemName.DA_CAP_9;

            } else if (type == MANH_NON_100_NAM) {
                itemId = ItemName.MANH_NON_100_NAM;
            } else if (type == MANH_AO_100_NAM) {
                itemId = ItemName.MANH_AO_100_NAM;
            } else if (type == MANH_GANG_100_NAM) {
                itemId = ItemName.MANH_GANG_100_NAM;
            } else if (type == MANH_QUAN_100_NAM) {
                itemId = ItemName.MANH_QUAN_100_NAM;
            } else if (type == MANH_GIAY_100_NAM) {
                itemId = ItemName.MANH_GIAY_100_NAM;
            } else if (type == MANH_NON_100_NU) {
                itemId = ItemName.MANH_NON_100_NU;
            } else if (type == MANH_AO_100_NU) {
                itemId = ItemName.MANH_AO_100_NU;
            } else if (type == MANH_GANG_100_NU) {
                itemId = ItemName.MANH_GANG_100_NU;
            } else if (type == MANH_QUAN_100_NU) {
                itemId = ItemName.MANH_QUAN_100_NU;
            } else if (type == MANH_GIAY_100_NU) {
                itemId = ItemName.MANH_GIAY_100_NU;
            } else if (type == MANH_PHU_100) {
                itemId = ItemName.MANH_PHU_100;
            } else if (type == MANH_BOI_100) {
                itemId = ItemName.MANH_BOI_100;
            } else if (type == MANH_NHAN_100) {
                itemId = ItemName.MANH_NHAN_100;
            } else if (type == MANH_LIEN_100) {
                itemId = ItemName.MANH_LIEN_100;
            } else if (type == MANH_SACH_MOI) {

                itemId = ItemName.MANH_SACH_MOI;

            } else if (type == BOSS_LANG_CO_TTTS) {

                itemId = ItemName.TU_TINH_THACH_SO_CAP;

            } else if (type == BOSS_LANG_CO_TTTT) {

                itemId = ItemName.TU_TINH_THACH_TRUNG_CAP;

            } else if (type == BOSS_LANG_CO_CTT) {

                itemId = ItemName.CHUYEN_TINH_THACH;

            } else if (type == BOSS_VDMQ_RBN) {
                int random = NinjaUtils.nextInt(0, 100);
                if (random == 0) {
                    itemId = ItemName.RUONG_BACH_NGAN;
                }

            } else if (type == EQUIP) {
                int levelMin = this.level / 10 * 10;
                int levelMax = levelMin + 9;
                if (levelMax >= 70) {
                    levelMax = 69;
                }
                List<ItemStore> list = StoreManager.getInstance().getListEquipmentWithLevelRange(levelMin, levelMax);
                if (list.isEmpty()) {
                    return;
                }
                int rd = NinjaUtils.nextInt(list.size());
                ItemStore itemStore = list.get(rd);
                if (itemStore == null) {
                    return;
                }
                itm = Converter.getInstance().toItem(itemStore, Converter.RANDOM_OPTION);
                int n = NinjaUtils.nextInt(itm.options.size() - 1);
                for (int i = 0; i < n; i++) {
                    int index = NinjaUtils.nextInt(itm.options.size());
                    itm.options.remove(index);
                }
                if (n > 0) {
                    itm.yen = 5;
                }
            } else if (type == PHIEU_MAY_MAN) {
                itemId = ItemName.PHIEU_MAY_MAN;
            } else if (type == PHAN_THAN_LENH) {
                itemId = ItemName.PHAN_THAN_LENH;
            } else if (type == VKHD3x) {
                short[] arid = new short[]{96, 101, 106, 111, 116, 121};
                int randomIndex = NinjaUtils.nextInt(arid.length);
                itemId = randomIndex;
                int levelMin = this.level / 10 * 10;
                int levelMax = levelMin + 9;
                if (levelMax > 90) {
                    levelMax = 89;
                }
                List<ItemStore> list = StoreManager.getInstance().getListEquipmentWithLevelRange(levelMin, levelMax);
                if (list.isEmpty()) {
                    return;
                }
                int rd = NinjaUtils.nextInt(arid.length);
                ItemStore itemStore = list.get(rd);
                if (itemStore == null) {
                    return;
                }
                itm = Converter.getInstance().toItem(itemStore, Converter.MAX_OPTION);


                // ecuong fix item boss
            } else if (type == VKBOSS) {

                short[] arid = new short[]{94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 331, 332, 333, 334, 335, 336, 369, 370, 371, 372, 373, 374, 506, 507, 508, 509, 510, 511};
                int randomIndex = NinjaUtils.nextInt(arid.length);
                itemId = randomIndex;

                int levelMin = 10;
                int levelMax = 80;
                List<ItemStore> list = StoreManager.getInstance().getListEquipmentWithLevelRange(levelMin, levelMax);
                if (list.isEmpty()) {
                    return;
                }
                int rd = NinjaUtils.nextInt(arid.length);
                ItemStore itemStore = list.get(rd);
                if (itemStore == null) {
                    return;
                }
                itm = Converter.getInstance().toItem(itemStore, Converter.RANDOM_OPTION);
            } else if (type == SVCBOSS) {
                RandomCollection<Integer> SVC = new RandomCollection<>();
                SVC.add(0.5, ItemName.SACH_VO_CONG_KIEM_100);
                SVC.add(0.5, ItemName.SACH_VO_CONG_TIEU_100);
                SVC.add(0.5, ItemName.SACH_VO_CONG_KUNAI_100);
                SVC.add(1, ItemName.SACH_VO_CONG_CUNG_100);
                SVC.add(1, ItemName.SACH_VO_CONG_DAO_100);
                SVC.add(1, ItemName.SACH_VO_CONG_QUAT_100);

                // fix svc boss ko ra
                itemId = SVC.next();
            }
            Item item = null;
            if (type == EQUIP || type == VKBOSS || type == VKHD3x) {
                item = itm;


            } else {
                item = ItemFactory.getInstance().newItem(itemId);
            }
            if (item.id < 12 && !this.isBoss && this.levelBoss == 0) {
                item.isLock = true;
            }
            if (item.template.type == 25) {
                item.isLock = true;
            }
            if (item.id == ItemName.SUSHI) {
                if (owner.selectedSkill != null) {
                    item.setQuantity(owner.selectedSkill.options[0].param);
                }
            } else if (item.id == ItemName.YEN) {
                if (type == LANG_CO) {
                    item.setQuantity(10000);
                } else if (this.zone.tilemap.isDungeo9x()) {
                    item.setQuantity(50000);
                } else {
                    item.setQuantity(NinjaUtils.nextInt(this.level * 15, (this.level + 10) * 15));
                }
                if (this.isBoss) {
                    if (this.template.id == MobName.CO_HON) {
                        item.setQuantity(1000000);
                    } else {
                        item.setQuantity(50000);
                    }

                } else if (this.levelBoss == 1) {
                    item.setQuantity(item.getQuantity() * 20);
                } else if (this.levelBoss == 2) {
                    item.setQuantity(item.getQuantity() * 30);
                }
                // đồ boss rơi
                if (!this.isBoss && type != LANG_CO) {
                    if (owner != null) {
                        owner.addYen(item.getQuantity());
                        owner.serverMessage("Bạn nhận được " + item.getQuantity() + " yên.");

                        if (owner.gloryTask != null && owner.gloryTask.type == GloryTask.NHAT_YEN) {
                            owner.gloryTask.updateProgress(item.getQuantity());
                        }

                        return;
                    }
                }

//            } else if (item.id == ItemName.PHIEU_MAY_MAN) {
//                if (this.zone.tilemap.isVDMQ()) { // nếu rơi ở vdmq
//                    item.setQuantity(30);
//                    item.expire = -1;
//                } else {
//                    item.setQuantity(20);
//                    item.expire = -1;
//                }

            } else if (item.id == ItemName.LONG_KHI) {
                item.setQuantity(NinjaUtils.nextInt(10, 50));
                item.expire = -1;
            } else if (item.id == ItemName.SON_TINH || item.id == ItemName.THUY_TINH) {
                item.expire = System.currentTimeMillis() + (long) (86400000 * 5);
            } else if (item.id == ItemName.VAI || item.id == ItemName.KEM_SUA || item.id == ItemName.KEM_CHOCOLATE) {
                item.setQuantity(NinjaUtils.nextInt(100, 200));
            } else if (item.id == ItemName.DIEU_VAI || item.id == ItemName.HU_KEM_DAM) {
                item.setQuantity(NinjaUtils.nextInt(5, 10));
            } else {
                item.setQuantity(1);
            }
            if (type == EVENT || type == BI_MA) {
                if (owner != null) {
                    if (owner.getSlotNull() > 0) {
                        owner.addItemToBag(item);
                        return;
                    }
                }
            }
            if (type == VKBOSS) {
                item.next((byte) NinjaUtils.nextInt(0, 10));
                item.isLock = false;
            }
            item.expire = -1;

            if (type == CHIA_KHOA_CO_QUAN || type == LAM_THAO_DUOC) {
                item.expire = System.currentTimeMillis() + (600000 * 3); //
            }
            short x = this.x;
            short y = this.y;
            if (type != SUSHI) {
                x = (short) NinjaUtils.nextInt(this.x - 20, this.x + 20);
            }
            int temp = 0;
            if (x < 50) {
                x = 50;
            } else if (x > (temp = (zone.tilemap.tmw * 24))) {
                x = (short) (temp - 50);
            }
            y = zone.tilemap.collisionY(x, (short) (this.y / 24 * 24));

            ItemMap itemMap = ItemMapFactory.getInstance().builder().id(zone.numberDropItem++).x(x).y(y).build();
            if (master != null) {
                itemMap.setOwnerID(master.id); // fix dame boss
            } else {
                itemMap.setOwnerID(-1);
            }
            if (type == BOSS_LDGT) {
                itemMap.setOwnerID(-1);
            }
            if (item != null) {
                itemMap.setItem(item);
                zone.addItemMap(itemMap);
                if (type == SUSHI || type == ITEM_TASK) {
                    this.itemMap = itemMap;
                } else {
                    zone.getService().addItemMap(itemMap);
                }
            }
        } catch (Exception e) {
            Log.logException("mob drop item err", Mob.class, e);
        }
    }

    private void attack() {
        Figurehead[] buNhins = zone.getBuNhins();
        for (int j = 0; j < buNhins.length; j++) {
            Figurehead buNhin = buNhins[j];
            int distance = NinjaUtils.getDistance(this.x, this.y, buNhin.x, buNhin.y);
            if ((this.isBoss && distance > 300) || (!this.isBoss && distance > 300)) {
                continue;
            }
            zone.getService().npcAttackBuNhin(this, j);
            return;
        }
        Vector<Char> list = new Vector<Char>();
        Vector<Char> chars = getChars();
        for (Char _char : chars) {
            if (_char.isCleaned) {
                continue;
            }
            if (_char.isInvisible()) {
                continue;
            }
            if (_char.isNhanBan) {
                continue;
            }
            if (_char.isModeCreate) {
                continue;
            }
            int distance = NinjaUtils.getDistance(this.x, this.y, _char.x, _char.y);
            if ((this.isBoss && distance > 600) || (!this.isBoss && distance > 300)) {
                continue;
            }
            list.add(_char);
        }
        if (list.isEmpty()) {
            return;
        }
        int rand = NinjaUtils.nextInt(list.size());
        Char pl = list.get(rand);
        attack(pl, null);
    }

    // dame quái bắn
    public void attack(Char pl, Char owner) {
        if (pl != null && !pl.isDead) {
            pl.lock.lock();
            try {
                boolean isMobMe = (owner != null);

                int dameHp = NinjaUtils.nextInt(this.damageOnPlayer2, this.damageOnPlayer);
                if (!isMobMe) {
                    if (zone.tilemap.isDungeo()) {
                        dameHp *= 2;
                    } else if (zone.tilemap.isDungeoClan()) {
                        dameHp = pl.hp * 80 / 100;
                        if (this.isBoss) {
                            dameHp *= 20;
                        } else if (this.levelBoss == 1) {
                            dameHp *= 2;
                        } else if (this.levelBoss == 2) {
                            dameHp *= 3;
                        }
                    } else if (zone.tilemap.isDauTruong() || zone.tilemap.isLoiDai()) {
                        dameHp = dameHp * 10 / 100;
                    } else if (zone.tilemap.isLangCo() || zone.tilemap.isFujukaSanctuary() || zone.tilemap.isLangTruyenThuyet()) {
                        dameHp = dameHp * 4; // dame quái làng cổ
                    } else if (zone.tilemap.isVDMQ()) {
                        dameHp = dameHp * 2; // dame quái vdmq
                    }
                }
                dameHp -= pl.dameDown;
                if (pl.isReductionDame) {
                    dameHp -= dameHp * pl.options[136] / 100;
                }
                switch (sys) {
                    case 1:
                        dameHp -= dameHp * pl.options[127] / 100;
                        dameHp -= pl.resFire;
                        dameHp -= pl.options[48];
                        break;

                    case 2:
                        dameHp -= dameHp * pl.options[130] / 100;
                        dameHp -= pl.resIce;
                        dameHp -= pl.options[49];
                        break;

                    case 3:
                        dameHp -= dameHp * pl.options[131] / 100;
                        dameHp -= pl.resWind;
                        dameHp -= pl.options[50];
                        break;
                }

                Effect eff2 = pl.getEm().findByID((byte) 37);
                if (eff2 != null) {
                    dameHp -= dameHp * eff2.param / 100;
                }
                if (pl.isFire) {
                    dameHp += dameHp;
                }
                int level = this.level;
                // level = this.level > 0 ? this.level : 1;
                int exactly = NinjaUtils.nextInt((level * 10) + 100);
                int miss = NinjaUtils.nextInt(pl.miss + 100);
                boolean isMiss = exactly < miss;
                if (!isMobMe) {
                    if (zone.tilemap.isDungeoClan()) {
                        int effectId = -1;
                        int downTimeEffectId = -1;
                        int randEffectId = NinjaUtils.nextInt(3);
                        if (zone.tilemap.id == 84 || ((zone.tilemap.id == 90 || zone.tilemap.id == 167) && randEffectId == 1)) {
                            effectId = 5;
                            downTimeEffectId = 40;
                        } else if (zone.tilemap.id == 85 || ((zone.tilemap.id == 90 || zone.tilemap.id == 167) && randEffectId == 2)) {
                            effectId = 7;
                            downTimeEffectId = 42;
                        } else if (zone.tilemap.id == 86 || ((zone.tilemap.id == 90 || zone.tilemap.id == 167) && randEffectId == 3)) {
                            effectId = 6;
                            downTimeEffectId = 41;
                        }

                        int randEffect = NinjaUtils.nextInt(100);
                        if (effectId != -1 && downTimeEffectId != -1 && (randEffect < 10 || (randEffect < 50 && zone.tilemap.id == 90) || (randEffect < 20 && zone.tilemap.id == 167))) {
                            Effect eff = new Effect(effectId, 3000, 0);
                            eff.addTime(-pl.options[downTimeEffectId] * 1000);
                            pl.getEm().setEffect(eff);
                        }
                    }
                }

                if (pl.isMiss) {
                    isMiss = true;
                }
                if (isMiss) {
                    dameHp = -1;
                } else {
                    if (dameHp <= 0) {
                        dameHp = 1;
                    }
                }
                int dameMp = 0;
                if (pl.isShieldMana) {
                    Effect eff = pl.getEm().findByType((byte) 6);
                    if (eff != null) {
                        if ((pl.mp * 100 / pl.maxMP) >= 10) {
                            dameMp = dameHp * eff.param / 100;
                            dameHp -= dameMp;
                            pl.addMp(-dameMp);
                        }
                    }
                }
                if ((pl.taskId == TaskName.NV_LAY_NUOC_HANG_SAU || pl.taskId == TaskName.NV_LAY_NUOC_HANG_SAU || pl.taskId == TaskName.NV_HAI_NAM) && pl.isCatchItem) {
                    pl.isFailure = true;
                }
                if (isMobMe) {

                    if (dameHp > 0) {
                        owner.zone.getService().mobMeAttack(owner, pl);

                        owner.zone.getService().attackCharacter(dameHp, dameMp, pl);
                    }
                } else {
                    attack(pl, dameHp, dameMp);
                }
                dameHp = dameHp - dameHp * 80 / 100;

                if (dameHp < 0) {
                    dameHp = -1;
                }
                if (dameHp > 0) {
                    pl.addHp(-dameHp);
                    if (pl.hp <= 0) {
                        pl.startDie();
                    }
                }

            } finally {
                pl.lock.unlock();
            }
        }
    }

    public Mob getMob(Char pl, int id) {
        short i;
        if (pl.zone != null && pl.zone.monsters != null) {
            for (i = 0; i < pl.zone.monsters.size(); i++) {
                if (pl.zone.monsters.get(i) != null && pl.zone.monsters.get(i).id == id) {
                    return pl.zone.monsters.get(i);
                }
            }
        }

        return null;
    }

    public void attack(Char pl) {  // fix quái không hồi sinh

        if (pl != null && pl.mobMe != null) {
            int dameHp = NinjaUtils.nextInt(this.damageOnMob2, this.damageOnMob);
            try {
                if (pl.mobAtk != -1 && pl.mobMe.template.id >= 211 && pl.mobMe.template.id <= 217) {
                    Mob mob = this.getMob(pl, pl.mobAtk);
                    if (mob != null && !mob.isDead) {
                        if (dameHp > 0) {
                            long now = System.currentTimeMillis();
                            if (now - pl.mobMe.lastTimeAttack > (pl.mobMe.attackDelay + 2000)) {
                                pl.mobMe.lastTimeAttack = now;
                                pl.zone.getService().mobMeAttack(pl, mob);

                                if (!mob.isDead && mob.hp > 0) {
                                    int preHP = mob.hp;
                                    mob.addHp(-dameHp);

                                    if (mob.hp < 0) {
                                        mob.hp = 0;
                                    }
                                    int nextHP = mob.hp;
                                    int hpz = Math.abs(nextHP - preHP);
                                    pl.addExp(mob, hpz);
                                    if (mob.hp <= 0) {
                                        mob.die();
                                        pl.mobAtk = -1;
                                    }
                                    if (mob.isDead) {
                                        Char master = mob.sortNinjaFight();
                                        Char killer = pl.getOriginChar();
                                        mob.dead(killer, master);
                                    }
                                }
                                pl.zone.getService().attackMonster(dameHp, false, mob);
                            }
                        }
                    }
                }
            } catch (Exception var4) {
                Log.logException("Lỗi mob me attack  : ", Mob.class, var4);

            }
        }

    }

    public void dead(Char killer, Char master) { // fix dame boss
        if (killer != null) {
            if (zone != null) {
                zone.mobDead(this, killer);
            }

            int dLevel = Math.abs(this.level - killer.level);
            if (Event.isKoroKing() && dLevel <= 10) {
                if (NinjaUtils.nextInt(2000) == -1) {
                    ((KoroKing) Event.getEvent()).bornKoroKing(this);
                }
                if (NinjaUtils.nextInt(2000) == 1) {
                    ((KoroKing) Event.getEvent()).infection(killer);
                }
            }
            if (killer.taskOrders != null) {
                for (TaskOrder task : killer.taskOrders) {
                    if (task.isComplete()) {
                        continue;
                    }
                    if (task.killId == this.template.id) {
                        if (task.taskId == TaskOrder.TASK_DAY) {
                            task.updateTask(1);
                        }
                        if (task.taskId == TaskOrder.TASK_BOSS) {
                            if (this.levelBoss == 3) {
                                task.updateTask(1);
                                //
                                Group group = killer.getGroup();
                                if (group != null) {
                                    List<Char> chars = group.getCharsInZone(killer.mapId, zone.id);
                                    for (Char _char : chars) {
                                        if (_char != null && _char != killer && !_char.isDead) {
                                            if (_char.taskOrders != null) {
                                                for (TaskOrder task2 : _char.taskOrders) {
                                                    if (task2.isComplete()) {
                                                        continue;
                                                    }
                                                    if (task2.killId == this.template.id) {
                                                        if (task2.taskId == TaskOrder.TASK_BOSS) {
                                                            task2.updateTask(1);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
            if (killer.taskMain != null) {
                if (killer.taskId != TaskName.NV_BAT_KHA_THI || ((killer.taskMain.index == 1 && this.levelBoss == 1) || (killer.taskMain.index == 2 && this.levelBoss == 2))) {
                    killer.updateTaskKillMonster(this);
                    Group group = killer.getGroup();
                    if (group != null) {
                        List<Char> chars = group.getCharsInZone(killer.mapId, zone.id);
                        for (Char _char : chars) {
                            if (_char != null && _char != killer && !_char.isDead) {
                                if (_char.taskMain != null) {
                                    if (_char.taskMain.taskId == killer.taskMain.taskId && _char.taskMain.index == killer.taskMain.index) {
                                        _char.updateTaskKillMonster(this);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (this.template.id == MobName.NGUOI_TUYET) {
                if (killer.clan != null) {
                    for (Char mem : killer.clan.getOnlineMembers()) {
                        mem.serverMessage(killer.name + " đã tiêu diệt người tuyết");
                        mem.getEventPoint().addPoint(Noel.TOP_KILL_SNOWMAN, 100);
                    }
                }
            } else if (this.template.id == MobName.HOP_BI_AN) {
                killer.addBossVuiXuan(this.x, this.y);
            } else if (this.template.id == MobName.QUAI_VAT) {
                killer.rewardVuiXuan();
            } else if (this.template.id == MobName.CHUOT_CANH_TY) {
                // killer.addEventPoint(1, Events.TOP_CHUOT);
            } else if (this.template.id == MobName.BOSS_TUAN_LOC) {
                killer.getEventPoint().addPoint(Noel.TOP_KILL_REINDEER_KING, 1);
                killer.addExp(8000000);
                if (killer.getSlotNull() == 0) {
                    return;
                }
                RandomCollection<Integer> rc = RandomItem.VUA_TUAN_LOC;
                int itemId = rc.next();
                Item itm = ItemFactory.getInstance().newItem(itemId);
                killer.addItemToBag(itm);

//            } else if (zone.tilemap.isLangCo() || zone.tilemap.isLangTruyenThuyet()) {  // đánh quái rơi lượng
//                if (NinjaUtils.nextInt(2) == 1) {
//                    int gold = 1; // luong quai thuong
//                    if (this.levelBoss == 1) {
//                        gold = 2;//luong ta
//                    } else if (this.levelBoss == 2) {
//                        gold = 3;// luong tl
//                    }
//                    killer.addGold(gold);
//                }

            } else if (this.template.id != MobName.BU_NHIN) {
                if (this.isBoss && this.template.id != MobName.JUUBI_SHINJU) {
                    if (this.template.id == MobName.CO_HON) {
                        killer.updateTopBoss(1);
                        this.dropItem(killer, Mob.BOSS_COHON, master);
                        for (int i = 0; i < 10; i++) {
                            dropItem(killer, Mob.YEN, master);
                        }
                        this.dropItem(killer, Mob.DA_9, master);
                        Calendar calendar = Calendar.getInstance();
                        int currentHour = calendar.get(Calendar.HOUR_OF_DAY); // Lấy giờ hiện tại trong ngày (24 giờ)
                        int currentMinute = calendar.get(Calendar.MINUTE); // Lấy giờ hiện tại trong ngày (24 giờ)
                        int minuteSpw = currentMinute + 10; // Lấy giờ hiện tại trong ngày (24 giờ)
                        if (minuteSpw >= 60) {
                            int timeSpw = minuteSpw - 60; // Tính phút sau khi điều chỉnh
                            int newHour = currentHour + 1; // Tăng giờ lên 1
                            SpawnBossManager.getInstance().spawn(newHour, timeSpw, 0, SpawnBossManager.BOSS_COHON, SpawnBossManager.ALL);

                        } else {
                            SpawnBossManager.getInstance().spawn(currentHour, minuteSpw, 0, SpawnBossManager.BOSS_COHON, SpawnBossManager.ALL);

                        }
                        return;
                    }

                    if (this.template.id == MobName.KORO_KING) {
                        int itemIndex = killer.getIndexItemByIdInBag(ItemName.VIEN_THUOC_THAN_KY);
                        killer.removeItem(itemIndex, 1, true);
                        killer.addExp(5000000);
                        if (killer.getSlotNull() > 0) {
                            RandomCollection<Integer> rc = RandomItem.BUA_MAY_MAN;
                            int itemId = rc.next();
                            Item itm = ItemFactory.getInstance().newItem(itemId);
                            itm.initExpire();
                            killer.addItemToBag(itm);
                        }
                        return;
                    }
                    if (zone.tilemap.isMapEvent()) {
                        this.dropItem(killer, Mob.MAP_BOSS_EVENT, master);
                    }
                    // TODO: chỉnh thêm item boss ngoài
                    if (zone.tilemap.isNormal()) {
                        if (this.level == 45 ||  this.level == 55 || this.level == 65 || this.level == 75) {
                            for (int i = 0; i < 5; i++) {
                                dropItem(killer, Mob.VKBOSS, master); // rơi random vũ khí
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.PHIEU_MAY_MAN, master); // rơi random phiếu may măn
                            }
                        }
                    }


                    // TODO: chỉnh thêm item boss vdmq
                    if (zone.tilemap.isVDMQ()) {
                        if (this.level == 90 || this.level == 100 || this.level == 110) {
                            for (int i = 0; i < 5; i++) {
                                dropItem(killer, Mob.VKBOSS, master); // rơi random vũ khí
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.PHAN_THAN_LENH, master); // rơi random ptl
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.PHIEU_MAY_MAN, master); // rơi random phiếu may măn
                            }
                            dropItem(killer, Mob.SVCBOSS, master); // random svc 100 ở đây
                            for (int i = 0; i < 30; i++) {
                                dropItem(killer, Mob.BOSS_VDMQ, master);  // sẽ lấy random item ở trong class Randomitem
                            }
                        }
                    }

                    // TODO: chỉnh thêm item boss sự kiện
                    if (zone.tilemap.isMapEvent()) {
                        if (this.level == 80) {

                        for (int i = 0; i < 20; i++) {
                            dropItem(killer, Mob.PHIEU_MAY_MAN, master); // rơi random phiếu may măn
                        }

                        }
                    }

                    if (zone.tilemap.isLangTruyenThuyet()) {  // skill 125
                        int random = NinjaUtils.nextInt(3, 5); // rơi 3 tới 5 mảnh sách mới
                        for (int i = 0; i < random; i++) {
                            dropItem(killer, Mob.MANH_SACH_MOI, master);
                        }
                    }

                    // TODO: chỉnh thêm item boss Làng địa ngục
                    if (zone.tilemap.isLangDiaNguc()) { // lang dia nguc
                        if (this.level == 79) {
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_NON_100_NAM, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_AO_100_NAM, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_GANG_100_NAM, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_QUAN_100_NAM, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_GIAY_100_NAM, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_NON_100_NU, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_AO_100_NU, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_GANG_100_NU, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_QUAN_100_NU, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_GIAY_100_NU, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_PHU_100, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_BOI_100, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_NHAN_100, master);
                            }
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.MANH_LIEN_100, master);
                            }
                        }
                    }


                    if (zone.tilemap.isNormal() && !zone.tilemap.isLangCo()) {
                        if (killer.mapId >= 162) {
                            for (int i = 0; i < 20; i++) {
                                dropItem(killer, Mob.BOSS, master);
                            }
                        } else {
                            for (int i = 0; i < 10; i++) {
                                dropItem(killer, Mob.BOSS, master);
                            }

                        }
                    } else if (zone.tilemap.id == 167) {
                        for (int i = 0; i < 20; i++) {
                            dropItem(killer, Mob.BOSS_LDGT, master);
                        }
                    }

                    if (this.zone.map.tilemap.isLangCo()) {
                        for (int i = 0; i < 5; i++) {
                            dropItem(killer, Mob.BOSS_LANG_CO_TTTS, master);
                        }
                        for (int i = 0; i < 3; i++) {
                            dropItem(killer, Mob.BOSS_LANG_CO_TTTT, master);
                        }
                        for (int i = 0; i < 2; i++) {
                            dropItem(killer, Mob.BOSS_LANG_CO_CTT, master);
                        }
                    }
                    if (zone.tilemap.isDungeo9x()) {
                        for (int i = 0; i < 20; i++) {
                            dropItem(killer, Mob.YEN, master);
                        }
                    } else if (this.zone.map.tilemap.isDungeo()) {
                        int yen = NinjaUtils.nextInt(500000);
                        killer.addYen(yen);
                        killer.serverMessage("Bạn nhận được " + yen + " Yên");
                    } else if (!this.zone.map.tilemap.isLangCo()) {
                        for (int i = 0; i < 20; i++) {
                            dropItem(killer, Mob.YEN, master);
                        }
                    }
                    if (Event.isVietnameseWomensDay() || Event.isInternationalWomensDay()) {
                        for (int i = 0; i < 20; i++) {
                            dropItem(killer, Mob.EVENT, master);
                        }
                    }


                } else {
                    int idItem = killer.getIdItemTask(this.template.id);
                    if (idItem != -1 && (NinjaUtils.nextInt(3) == 0 || this.template.id == MobName.HEO_RUNG)) {
                        dropItem(killer, Mob.ITEM_TASK, master);
                    }

                    if (zone.tilemap.isLangCo()) {
                        if (NinjaUtils.nextInt(8) == 0) {
                            dropItem(killer, Mob.LANG_CO, master);
                        }
                    } else if (zone.tilemap.isLangTruyenThuyet()) {
                        if (NinjaUtils.nextInt(12) == 0) {
                            this.dropItem(killer, Mob.LANG_TRUYEN_THUYET, master);
                        }

                    } else if (zone.tilemap.isLangDiaNguc()) { // lang dia nguc
                        if (NinjaUtils.nextInt(12) == 0) {
                            this.dropItem(killer, Mob.LANG_DIA_NGUC, master);
                        }


                    } else if (zone.tilemap.isChienTruong()) {
                        if (NinjaUtils.nextInt(100) == 0) {
                            dropItem(killer, Mob.CHIEN_TRUONG, master);
                        }
                    } else if (zone.tilemap.isDungeoClan() && zone.isLastBossWasBorn && this.levelBoss == 1) {
                        dropItem(killer, Mob.CHIA_KHOA_CO_QUAN, master);
                    } else if (zone.tilemap.isDungeoClan() && this.template.id == 81) {
                        if (NinjaUtils.nextInt(10) == 5) {
                            dropItem(killer, Mob.LAM_THAO_DUOC, master);
                        }
                    } else if (zone.tilemap.isDungeoClan() && (this.template.id == MobName.BAO_QUAN || this.template.id == MobName.TU_HA_MA_THAN)) {
                        for (int i = 0; i < 20; i++) {
                            dropItem(killer, Mob.BOSS_LDGT, master);
                        }
                    } else if (dLevel <= 7) {
                        int[] percents = {10, 10, 5, 75};
                        byte[] types = {Mob.YEN, Mob.ITEM, Mob.EQUIP, -1};
                        int index = NinjaUtils.randomWithRate(percents, 100);
                        byte type = types[index];
                        if (type == -1) {
                            if (zone.tilemap.isVDMQ()) {
                                if (killer.isTNP && NinjaUtils.nextInt(250) < 5 || killer.isMayDo && NinjaUtils.nextInt(250) < 5) {
                                    type = Mob.VDMQ;
                                }
                                if (killer.isKNP && NinjaUtils.nextInt(250) == 0) {
                                    type = Mob.VDMQ;
                                }
                            }
                            if (zone.tilemap.isDungeoClan()) {
                                if (NinjaUtils.nextInt(100) < 20) {
                                    type = Mob.LANH_DIA_GIA_TOC;
                                }
                            }
                        }
                        if (type != -1) {
                            dropItem(killer, type, master);
                        }
                    }
                    if (Event.isEvent()) {
                        int distance = 5;
                        int levelnlsk = 7; // fix level up nlsk
                        int percentage = 10;
                        if (killer.isTNP || killer.isKNP || killer.isMayDo) {
                            distance = 10;
                        }
                        if (killer.isTNP) { /// up tnp ra thêm nlsk
                            percentage += 5;
                        } else if (killer.isKNP) {
                            percentage += 3;
                        }
                        if (zone.tilemap.isLangCo() || zone.tilemap.isVDMQ()) {
                            percentage += 2;
                        }
                        if (killer.user.activated != 1) { /// fix chưa kích hoạt giảm tỉ lệ up nlsk
                            percentage -= 5;
                        }
                        if (dLevel <= levelnlsk) {
                            int r = NinjaUtils.nextInt(100);
                            if (r < percentage) {
                                dropItem(killer, EVENT, master);
                            }
                        }

                        if (killer.isBiMa && Event.isHalloween()) {
                            if (dLevel <= 10) {
                                int r = NinjaUtils.nextInt(100);
                                if (r <= 5) {
                                    dropItem(killer, BI_MA, master);
                                }
                            }
                        }
//                        if (killer.isMayDo && Event.isCoHon()) {
//                            System.out.println("907-Mob");
//                            if (dLevel <= 10) {
//                                int r = NinjaUtils.nextInt(100);
//                                if (r <= 5) {
//                                    dropItem(killer,VDMQ, master);
//                                }
//                            }
//                        }
                    }
                    if ((this.levelBoss == 1 || this.levelBoss == 2) && template.id != MobName.HEO_RUNG && template.id != MobName.HEO_MOI) {
                        this.dropItem(killer, Mob.YEN, master);
                    }
                }
            }
            // isHuman
            if (killer.gloryTask != null) {
                if (Math.abs(killer.level - this.level) <= 10) {
                    if (this.levelBoss == 1) {
                        if (killer.gloryTask.type == GloryTask.TIEU_DIET_TINH_ANH) {
                            killer.gloryTask.updateProgress(1);
                        }
                    } else if (this.levelBoss == 2) {
                        if (killer.gloryTask.type == GloryTask.TIEU_DIET_THU_LINH) {
                            killer.gloryTask.updateProgress(1);
                        }
                    }
                }
            }
            if (killer.isModeRemove) {
                zone.waitingListDelete.add(this);
            } else {
                if (zone.tilemap.isDungeoClan()) {
                    if (this.template.id == MobName.LAM_THAO) {
                        zone.addMobForWatingListRespawn(this);
                    }
                } else if (zone.tilemap.isDungeo9x()) {
                    if (!this.isBoss) {
                        zone.addMobForWatingListRespawn(this);
                    }
                } else if (!zone.tilemap.isDungeo()) {
                    if (!this.isBoss || this.template.id == MobName.HOP_BI_AN) {
                        zone.addMobForWatingListRespawn(this);
                    } else {
                        if (this.template.id == MobName.BOSS_TUAN_LOC || this.template.id == MobName.QUAI_VAT) {
                            killer.mob = null;
                        } else {
                            zone.waitingListDelete.add(this);
                        }
                    }
                }
            }
        }
    }

    public void attack(Char p, int dameHp, int dameMp) {
        try {
            p.getService().npcAttackMe(this, dameHp, dameMp);
            zone.getService().npcAttackPlayer(this, p);
        } catch (Exception e) {
            Log.logException("mob attack er", Mob.class, e);
        }
    }

    public void addCharId(int charId) {
        if (!chars.contains(charId)) {
            chars.add(charId);
        }
    }

    public synchronized boolean checkExist(int charId) {
        for (int id : chars) {
            if (id == charId) {
                return true;
            }
        }
        return false;
    }

    public Vector<Char> getChars() {
        Vector<Char> chars = new Vector<>();
        Vector<Integer> clone = (Vector<Integer>) this.chars.clone();
        for (int id : clone) {
            Char _char = zone.findCharById(id);
            if (_char != null) {
                chars.addElement(_char);
            }
        }
        return chars;
    }

    public Char randomChar() {
        Char _char = null;
        Vector<Integer> chars = (Vector<Integer>) this.chars.clone();
        do {
            int size = chars.size();
            if (size == 0) {
                break;
            }
            int index = NinjaUtils.nextInt(size);
            int id = chars.get(index);
            Char tmp = zone.findCharById(id);
            if (tmp == null) {
                chars.remove(index);
            } else {
                if (!tmp.isCleaned && !tmp.isDead && !tmp.isInvisible()) {
                    int distance = NinjaUtils.getDistance(this.x, this.y, tmp.x, tmp.y);
                    if ((this.isBoss && distance > 600) || (!this.isBoss && distance > 300)) {
                        continue;
                    }
                    _char = tmp;
                    break;
                }
            }
        } while (_char == null);
        return _char;
    }

    public void update() {
        if (!this.isDead) {
            if (template.id != MobName.BACH_LONG_TRU && template.id != MobName.HAC_LONG_TRU) {
                if (template.id != MobName.BOSS_TUAN_LOC && template.id != MobName.NGUOI_TUYET && template.id != MobName.HOP_BI_AN && template.id != MobName.QUAI_VAT) {
                    List<Char> list = zone.getChars();
                    if (!list.isEmpty()) {
                        int add = 0;
                        if (isBoss) {
                            add = 100;
                        }
                        for (Char _char : list) {
                            if (_char.isDead) {
                                continue;
                            }
                            if ((_char.faction == 0 && zone.tilemap.id == 99) || (_char.faction == 1 && zone.tilemap.id == 103) || _char.faction == 2) {
                                continue;
                            }
                            if (template.type == 4) {
                                int range = NinjaUtils.getDistance(this.x, this.y, _char.x, _char.y);
                                if (range < template.rangeMove + 50 + add) {
                                    if (!_char.isInvisible()) {
                                        addCharId(_char.id);
                                    }
                                }
                            } else {
                                if (this.y == _char.y && Math.abs(this.x - _char.x) < template.rangeMove + 20 + add) {
                                    if (!_char.isInvisible()) {
                                        addCharId(_char.id);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!isIce && !isWind && !isDisable && template.id != MobName.BU_NHIN && template.id != MobName.MOC_NHAN && template.id != MobName.THAO_DUOC && chars.size() > 0) {
                    long now = System.currentTimeMillis();
                    if (now - this.lastTimeAttack > this.attackDelay) {
                        this.lastTimeAttack = now;

                        attack();
                    }
                }
            }
            Effect eff5 = effects.get((byte) 5);
            if (eff5 != null) {
                callFireEffect(eff5);
            }


//            Vector<Byte> removeEffect = new Vector<>();
//            Iterator<Map.Entry<Byte, Effect>> iterator = this.effects.entrySet().iterator();
//
//            while (iterator.hasNext()) {
//                Map.Entry<Byte, Effect> entry = iterator.next();
//                Effect eff = entry.getValue();
//
//                if (eff == null || eff.isExpired()) {
//                    removeEffect.add(entry.getKey());
//                    iterator.remove(); // Remove safely using the iterator
//                }
//            }

            // fix quái ko ra
            Vector<Byte> removeEffect = new Vector<>();
            for (Entry<Byte, Effect> entry : this.effects.entrySet()) {
                Effect eff = entry.getValue();
                if (eff == null || eff.isExpired()) {
                    removeEffect.add(entry.getKey());
                }
            }


            for (byte b : removeEffect) {
                this.effects.remove(b);
                if (b == 1) {
                    zone.setFire(this, false);
                } else if (b == 2) {
                    zone.setIce(this, false);
                } else if (b == 3) {
                    zone.setWind(this, false);
                } else if (b == 14) {
                    zone.setMove(this, false);
                } else if (b == 0) {
                    zone.setDisable(this, false);
                }
            }
        }
    }

    public void callFireEffect(Effect eff5) { // hieu ung dot
        lock.lock();
        try {
            int charId = eff5.param2;
            int damage = eff5.param;

            zone.getService().callEffectNpc(this);
            int preHP = this.hp;
            Char p = zone.findCharById(charId);
            if (p == null) {
                return;
            }

            if (this.template.id == MobName.NGUOI_TUYET) {
                if (p.clan != null) {
                    damage = 1;
                } else {
                    damage = 0;
                }
            } else if (this.zone.tilemap.isDungeoClan() && this.hp - damage <= 0) {
                damage = 0;
            }

            if (this.template.id == MobName.BU_NHIN) {
                addHp(-(this.maxHP / 5));
            } else {
                addHp(-damage);
            }
            this.Fight(p, damage); // fix dame boss
            zone.getService().attackMonster(damage, false, this);
            int nextHP = this.hp;
            int hp = Math.abs(nextHP - preHP);
            p.addExp(this, hp);

            if (this.hp <= 0) {
                this.die();
            }
            if (this.isDead) {
                Char master = sortNinjaFight(); // fix dame boss
                Char killer = p.getOriginChar();
                this.dead(killer, master);  // fix dame boss
            }

            if (zone.tilemap.isDungeoClan()) {
                Territory.checkEveryAttack(p);
            }
        } finally {
            lock.unlock();
        }
    }

    public void addHp(int add) {
        this.hp += add;
    }

    public void ClearFight() {// fix dame boss
        this.nFight.clear();
    }

    public Char sortNinjaFight() { // fix dame boss
        int dameMax = 0;
        Char topUser = null; // Lưu trữ nhân vật có dame lớn nhất

        for (Char value : this.nFight.keySet()) {
            final int dame = this.nFight.get(value);
            Char user = ServerManager.findCharById(value.id);
            if (user != null && dame > dameMax) {
                dameMax = dame;
                topUser = user.getOriginChar(); // Cập nhật nhân vật có dame lớn nhất
            }
        }

        return topUser; // Trả về nhân vật có dame lớn nhất
    }

    public void Fight(Char p, int dame) { // fix dame boss
        this.nFight.merge(p, dame, Integer::sum);
    }
}
