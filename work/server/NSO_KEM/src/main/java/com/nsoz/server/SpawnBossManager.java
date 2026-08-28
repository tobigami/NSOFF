/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.server;

import com.nsoz.constants.MapName;
import com.nsoz.constants.MobName;
import com.nsoz.map.MapManager;
import com.nsoz.util.NinjaUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author kitakeyos - Hoàng Hữu Dũng
 */
public class SpawnBossManager {

    public static final String VUNG_DAT_MA_QUY = "vdmq";
    public static final String  THUONG = "normal";
    public static final String LANG_DIA_NGUC = "LangDiaNguc"; // lang dia nguc
    public static final String LANG_TRUYEN_THUYET = "ltt";
    public static final String MAP_BOSS_SU_KIEN = "mbsk";
    public static final String BOSS_COHON = "CO_HON";
    public static final String LANG_CO = "langco";
    public static final byte ALL = 0;
    public static final byte RANDOM = 1;

    private static final SpawnBossManager instance = new SpawnBossManager();
    private final HashMap<String, List<SpawnBoss>> spawnBosses = new HashMap<>();

    public static SpawnBossManager getInstance() {
        return instance;
    }

    public void init() {
        // boss vdmq
        final List<SpawnBoss> VDMQ = new ArrayList<>();
        SpawnBoss spawnBoss1 = create(1, MapName.DOAN_SON, (short) 1308, (short) 576);
        spawnBoss1.add(1, MobName.KEN_KEN_VUONG);
        VDMQ.add(spawnBoss1);

        SpawnBoss spawnBoss2 = create(2, MapName.DAO_QUY, (short) 1164, (short) 552);
        spawnBoss2.add(1, MobName.U_MINH_KHUYEN);
        VDMQ.add(spawnBoss2);

        SpawnBoss spawnBoss3 = create(3, MapName.SINH_TU_KIEU, (short) 972, (short) 696);
        spawnBoss3.add(1, MobName.DAI_LUC_SI);
        VDMQ.add(spawnBoss3);
        spawnBosses.put(VUNG_DAT_MA_QUY, VDMQ);


        // boss ngoài
        final List<SpawnBoss> NORMAL = new ArrayList<>();
        SpawnBoss spawnBoss4 = create(1, MapName.RUNG_AOKIGAHARA, (short) 1476, (short) 96);
        spawnBoss4.add(1, MobName.XICH_PHIEN_THIEN_LONG); // boss 45
        NORMAL.add(spawnBoss4);

        SpawnBoss spawnBoss5 = create(2, MapName.VACH_NUI_ITO, (short) 876, (short) 216);
        spawnBoss5.add(1, MobName.XICH_PHIEN_THIEN_LONG); // boss 45
        NORMAL.add(spawnBoss5);

        SpawnBoss spawnBoss6 = create(3, MapName.THUNG_LUNG_TAIRA, (short) 492, (short) 384);
        spawnBoss6.add(1, MobName.XICH_PHIEN_THIEN_LONG); // boss 45
        NORMAL.add(spawnBoss6);

        SpawnBoss spawnBoss7 = create(4, MapName.NGOI_DEN_OROCHI, (short) 996, (short) 264);
        spawnBoss7.add(1, MobName.HOA_NGUU_VUONG); // boss 75
        NORMAL.add(spawnBoss7);

        SpawnBoss spawnBoss8 = create(5, MapName.DINH_ICHIDAI, (short) 1284, (short) 432);
        spawnBoss8.add(1, MobName.SAMURAI_CHIEN_TUONG); // boss 65
        NORMAL.add(spawnBoss8);

        SpawnBoss spawnBoss9 = create(6, MapName.DAO_HEBI, (short) 2028, (short) 240);
        spawnBoss9.add(1, MobName.XICH_PHIEN_THIEN_LONG); // boss 45
        NORMAL.add(spawnBoss9);

        SpawnBoss spawnBoss10 = create(7, MapName.HANG_MEIRO, (short) 732, (short) 528);
        spawnBoss10.add(1, MobName.XICH_PHIEN_THIEN_LONG); // boss 45
        NORMAL.add(spawnBoss10);

        SpawnBoss spawnBoss11 = create(8, MapName.DONG_KISEI, (short) 1476, (short) 264);
        spawnBoss11.add(1, MobName.HOA_NGUU_VUONG); // boss 55
        NORMAL.add(spawnBoss11);

        SpawnBoss spawnBoss12 = create(9, MapName.KHU_DA_DO_AKAI, (short) 1020, (short) 336);
        spawnBoss12.add(1, MobName.SAMURAI_CHIEN_TUONG); // boss 65
        NORMAL.add(spawnBoss12);

        SpawnBoss spawnBoss13 = create(10, MapName.DINH_OKAMA, (short) 924, (short) 648);
        spawnBoss13.add(1, MobName.THAN_THO); // boss 55
        NORMAL.add(spawnBoss13);

        SpawnBoss spawnBoss14 = create(11, MapName.HANG_NUI_KURAI, (short) 1044, (short) 672);
        spawnBoss14.add(1, MobName.SAMURAI_CHIEN_TUONG); // boss 65
        NORMAL.add(spawnBoss14);

        SpawnBoss spawnBoss15 = create(12, MapName.RUNG_KAPPA, (short) 780, (short) 240);
        spawnBoss15.add(1, MobName.XICH_PHIEN_THIEN_LONG); // boss 45
        NORMAL.add(spawnBoss15);

        SpawnBoss spawnBoss16 = create(13, MapName.DEN_HARUMOTO, (short) 516, (short) 192);
        spawnBoss16.add(1, MobName.HOA_NGUU_VUONG); // boss 55
        NORMAL.add(spawnBoss16);

        SpawnBoss spawnBoss17 = create(14, MapName.MUI_HONE, (short) 996, (short) 288);
        spawnBoss17.add(1, MobName.SAMURAI_CHIEN_TUONG); // boss 65
        NORMAL.add(spawnBoss17);

        SpawnBoss spawnBoss18 = create(15, MapName.NUI_ONTAKE, (short) 540, (short) 864);
        spawnBoss18.add(1, MobName.THAN_THO); // boss 55
        NORMAL.add(spawnBoss18);

        SpawnBoss spawnBoss19 = create(16, MapName.NUI_ANZEN, (short) 660, (short) 360);
        spawnBoss19.add(1, MobName.XICH_PHIEN_THIEN_LONG); // boss 45
        NORMAL.add(spawnBoss19);
        spawnBosses.put(THUONG, NORMAL);

        // boss làng truyền thuyết
        final List<SpawnBoss> LTT = new ArrayList<>();
        SpawnBoss spawnBoss20 = create(16, MapName.DAO_BAY_NOKKI, (short) 600, (short) 400);
        spawnBoss20.add(1, MobName.TU_HA_MA_THAN);
        LTT.add(spawnBoss20);

        SpawnBoss spawnBoss21 = create(16, MapName.DEN_HASHI, (short) 800, (short) 432);
        spawnBoss21.add(1, MobName.MY_HAU_VUONG);
        LTT.add(spawnBoss21);

        SpawnBoss spawnBoss22 = create(16, MapName.HANG_KISO, (short) 832, (short) 240);
        spawnBoss22.add(1, MobName.TUONG_GIAC);
        LTT.add(spawnBoss22);
        spawnBosses.put(LANG_TRUYEN_THUYET, LTT);

        // boss sự kiện ///
        final List<SpawnBoss> BOSS_SK = new ArrayList<>();
        SpawnBoss spawnBoss23 = create(17, MapName.DONG_HACHI, (short) 869, (short) 312);
        spawnBoss23.add(1, MobName.MY_HAU_TUONG);
        BOSS_SK.add(spawnBoss23);

        SpawnBoss spawnBoss24 = create(17, MapName.KY_TUC_XA_HARUNA, (short) 369, (short) 240);
        spawnBoss24.add(1, MobName.MY_HAU_TUONG);
        BOSS_SK.add(spawnBoss24);

        SpawnBoss spawnBoss25 = create(17, MapName.SONG_BANG_YAMATO, (short) 1037, (short) 240);
        spawnBoss25.add(1, MobName.MY_HAU_TUONG);
        BOSS_SK.add(spawnBoss25);
        spawnBosses.put(MAP_BOSS_SU_KIEN, BOSS_SK);

        // boss làng cổ
        final List<SpawnBoss> LANGCO = new ArrayList<>();
        SpawnBoss spawnBoss26 = create(18, MapName.RUNG_MAJO, (short) 132, (short) 214);
        spawnBoss26.add(1, MobName.PHU_THUY_BI_NGO_2    );
        LANGCO.add(spawnBoss26);

        SpawnBoss spawnBoss27 = create(18, MapName.DONG_KINGU, (short) 2693, (short) 408);
        spawnBoss27.add(1, MobName.BANG_DE);
        LANGCO.add(spawnBoss27);

        SpawnBoss spawnBoss28 = create(18, MapName.VUC_YUNIKOON, (short) 551, (short) 144);
        spawnBoss28.add(1, MobName.HOA_KY_LAN_2);
        LANGCO.add(spawnBoss28);

        SpawnBoss spawnBoss29 = create(18, MapName.NUI_DORAGON, (short) 534, (short) 118);
        spawnBoss29.add(1, MobName.TU_LOI_DIEU_THIEN_LONG_2);
        LANGCO.add(spawnBoss29);
        spawnBosses.put(LANG_CO, LANGCO);

        final List<SpawnBoss> CO_HON = new ArrayList<>();
        SpawnBoss spawnBossCoHon = create(19, MapName.CUA_HANG_AKA, (short) 214, (short) 240);
        spawnBossCoHon.add(1, MobName.CO_HON);
        CO_HON.add(spawnBossCoHon);
        spawnBosses.put(BOSS_COHON, CO_HON);

        // BOSS LÀNG ĐỊA NGỤC
        final List<SpawnBoss> LDN = new ArrayList<>();    // lang dia nguc
        SpawnBoss spawnBoss30 = create(16, MapName.QUY_NGUC, (short) 1481, (short) 312);
        spawnBoss30.add(1, MobName.TUONG_GIAC);
        LDN.add(spawnBoss30);
        spawnBosses.put(LANG_DIA_NGUC, LDN);


    }

    public SpawnBoss create(int id, int mapID, short x, short y) {
        SpawnBoss spawnBoss = new SpawnBoss(0, MapManager.getInstance().find(mapID), x, y);
        return spawnBoss;
    }

    public List<SpawnBoss> getListSpawnBoss(String key) {
        return spawnBosses.get(key);
    }

    private void spawnRandom(String key) {
        List<SpawnBoss> list = getListSpawnBoss(key);
        int rand = NinjaUtils.nextInt(list.size());
        SpawnBoss sp = list.get(rand);
        sp.spawn();
    }

    private void spawnAll(String key) {
        List<SpawnBoss> list = getListSpawnBoss(key);
        for (SpawnBoss spawn : list) {
            spawn.spawn();
        }
    }

    /**
     * Gọi một tốp boss RA NGAY, không chờ tới khung giờ.
     *
     * spawnAll/spawnRandom vốn để private nên chỉ hàm hẹn giờ trong lớp này gọi được. Mở một cửa
     * công khai thay vì đổi hai hàm kia thành public: ở đây kiểm tra được tên tốp có thật không
     * và trả về số boss đã gọi, nên bên gọi biết chuyện gì đã xảy ra thay vì im lặng không làm gì.
     *
     * @return số điểm spawn đã kích, hoặc -1 nếu không có tốp nào mang tên đó
     */
    public int goiNgay(String key, byte type) {
        List<SpawnBoss> list = getListSpawnBoss(key);
        if (list == null || list.isEmpty()) {
            return -1;
        }
        if (type == RANDOM) {
            spawnRandom(key);
            return 1;
        }
        spawnAll(key);
        return list.size();
    }

    /** Tên các tốp boss, để bảng điều khiển khỏi phải chép cứng lại. */
    public static String[] cacTop() {
        return new String[] {THUONG, VUNG_DAT_MA_QUY, LANG_DIA_NGUC, LANG_TRUYEN_THUYET,
            MAP_BOSS_SU_KIEN, LANG_CO, BOSS_COHON};
    }

    public void spawn(int hours, int minutes, int seconds, String key, byte type) {
        NinjaUtils.schedule(() -> {
            if (type == ALL) {
                spawnAll(key);
            }
            if (type == RANDOM) {
                spawnRandom(key);
            }
        }, hours, minutes, seconds);
    }

    public void spawnRepeat(String key, int hourlyDelay, byte type) {
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        int hours = zonedNow.getHour() + 1;
        if (hours % hourlyDelay != 0) {
            hours = ((hours / hourlyDelay + 1) * hourlyDelay);
        }
        if (hours >= 24) {
            hours = 0;
        }
        ZonedDateTime zonedNext5 = zonedNow.withHour(hours).withMinute(0).withSecond(0);
        if (zonedNow.compareTo(zonedNext5) > 0) {
            zonedNext5 = zonedNext5.plusDays(1);
        }

        Duration duration = Duration.between(zonedNow, zonedNext5);
        long initalDelay = duration.getSeconds();
        Runnable runnable = new Runnable() {
            public void run() {
                if (type == ALL) {
                    spawnAll(key);
                }
                if (type == RANDOM) {
                    spawnRandom(key);
                }
            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, initalDelay, hourlyDelay * 60 * 60, TimeUnit.SECONDS);
    }
}
