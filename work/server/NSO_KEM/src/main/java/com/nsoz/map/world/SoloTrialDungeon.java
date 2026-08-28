package com.nsoz.map.world;

import com.nsoz.constants.MobName;
import com.nsoz.item.Item;
import com.nsoz.item.ItemFactory;
import com.nsoz.map.Map;
import com.nsoz.map.MapManager;
import com.nsoz.map.zones.Cave;
import com.nsoz.map.zones.Zone;
import com.nsoz.mob.Mob;
import com.nsoz.model.Char;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SoloTrialDungeon extends World {

    public static final int[] MAP_IDS = {180, 181, 182, 183};
    public static final int[] TIME_LIMITS = {300, 420, 420, 600};
    public static final int[] BOSS_PER_STAGE = {10, 10, 5, 3};
    public static final List<SoloTrialDungeon> dungeons = new ArrayList<>();

    // Tọa độ boss từ dữ liệu Navicat
    private static final int[][] BOSS_COORDINATES = {
            {182, 216}, {329, 216}, {525, 240}, {637, 240}, {777, 168},
            {987, 192}, {1093, 240}, {1240, 216}, {996, 96}, {395, 264}
    };

    public static void addDungeon(SoloTrialDungeon dungeon) {
        synchronized (dungeons) {
            dungeons.add(dungeon);
            Log.debug("Thêm SoloTrialDungeon ID: " + dungeon.id + " cho người chơi " + (dungeon.player != null ? dungeon.player.name : "null"));
        }
    }

    public static SoloTrialDungeon findDungeonById(int id) {
        synchronized (dungeons) {
            for (SoloTrialDungeon dungeon : dungeons) {
                if (dungeon.id == id) {
                    return dungeon;
                }
            }
        }
        Log.error("Không tìm thấy SoloTrialDungeon với ID: " + id);
        return null;
    }

    private Char player;
    private int currentStage;
    public long timeCreate;
    private boolean finished;
    private boolean isTransitioning; // Trạng thái chuyển stage
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public SoloTrialDungeon(Char player) {
        setType(World.DUNGEON);
        this.name = "Solo Trial Dungeon";
        generateId();
        this.player = player;
        this.countDown = TIME_LIMITS[0];
        this.currentStage = 0;
        this.finished = false;
        this.isTransitioning = false;
        this.timeCreate = System.currentTimeMillis();
        Log.debug("Tạo SoloTrialDungeon ID: " + this.id + " cho người chơi " + player.name);
        open();
        initFinished = true;
        addMember(player);
        addDungeon(this);
    }

//    @Override
//    public void update() {
//        if (countDown <= 0 || (player != null && player.isDead)) {
//            Log.debug("Đóng phó bản: countDown=" + countDown + ", player=" + (player == null ? "null" : player.name) + ", isDead=" + (player != null && player.isDead));
//            close();
//            return;
//        }
//
//        // Nếu đang chuyển stage, bỏ qua việc cập nhật
//        if (isTransitioning) {
//            Log.debug("Đang chuyển stage, bỏ qua update cho SoloTrialDungeon ID: " + this.id);
//            return;
//        }
//
//        Zone zone = zones.get(0); // Luôn lấy zone đầu tiên vì zones chỉ chứa 1 zone tại một thời điểm
//        if (zone == null) {
//            Log.error("Zone null tại stage " + currentStage + " trong SoloTrialDungeon ID: " + this.id);
//            close();
//            return;
//        }
//
//        // Kiểm tra danh sách boss còn sống (chỉ tính mob là boss)
//        List<Mob> livingBosses = zone.getLivingMonsters().stream()
//                .filter(mob -> mob.isBoss && !mob.isDead)
//                .toList();
//        Log.debug("Kiểm tra vượt ải: livingBosses.size=" + livingBosses.size() + ", stage=" + currentStage);
//        livingBosses.forEach(boss -> Log.debug("Boss còn sống: ID=" + boss.id + ", isDead=" + boss.isDead + ", isBoss=" + boss.isBoss));
//        if (livingBosses.isEmpty()) {
//            Log.debug("Đã thỏa mãn điều kiện vượt ải tại stage " + currentStage);
//            distributeStageRewards();
//            if (currentStage < 3) {
//                service.serverMessage("Bạn đã vượt qua Ải " + (currentStage + 1) + "! Chuẩn bị chuyển sang Ải " + (currentStage + 2) + " trong 5 giây.");
//                isTransitioning = true; // Đánh dấu đang chuyển stage
//                scheduler.schedule(() -> {
//                    if (!finished && !isClosed) {
//                        currentStage++;
//                        if (currentStage >= MAP_IDS.length) {
//                            Log.error("currentStage " + currentStage + " vượt quá số lượng stage hợp lệ " + MAP_IDS.length);
//                            finish();
//                            return;
//                        }
//                        countDown = TIME_LIMITS[currentStage];
//                        // Xóa zone cũ và tạo zone mới
//                        Zone oldZone = zones.get(0);
//                        zones.clear();
//                        oldZone.close(); // Đóng zone cũ
//                        open();
//                        if (player != null && !player.isDead) {
//                            player.setXY((short) 626, (short) 144);
//                            Log.debug("Đặt tọa độ người chơi " + player.name + " tại x: 626, y: 144 trong SoloTrialDungeon ID: " + this.id);
//                            joinZone(player, MAP_IDS[currentStage]);
//                            service.serverMessage("Bắt đầu Ải " + (currentStage + 1) + "!");
//                        }
//                        isTransitioning = false; // Kết thúc chuyển stage
//                    }
//                }, 5, TimeUnit.SECONDS);
//            } else {
//                finish();
//            }
//        }
//
//        countDown--;
//        service.sendTimeInMap(countDown);
//        updatePlayerStatus();
//    }

    public void open() {
        int mapId = MAP_IDS[currentStage];
        Map map = MapManager.getInstance().find(mapId);
        if (map == null) {
            Log.error("Không tìm thấy map ID: " + mapId);
            close();
            return;
        }
        Cave cave = new Cave(0, map.tilemap, map);
        cave.setWorld(this);
        addZone(cave);
        if (zones.isEmpty() || !zones.contains(cave)) {
            Log.error("Không thể thêm zone cho map ID: " + mapId + ", zones.size=" + zones.size());
            close();
            return;
        }
        Log.debug("Đã thêm zone cho stage " + currentStage + ", zones.size=" + zones.size());
        service.serverMessage("Ải " + (currentStage + 1) + " (" + map.tilemap.name + ") đã mở. Thời gian còn lại: " + countDown + " giây.");

        Zone zone = zones.get(0); // Chỉ số 0 vì chỉ có 1 zone tại một thời điểm
        spawnBosses(zone);
    }

    private void spawnBosses(Zone zone) {
        int bossCount = BOSS_PER_STAGE[currentStage];
        boolean isFinalStage = (currentStage == 3);
        int mobId = isFinalStage ? MobName.BANG_DE : 222; // Boss đặc biệt cho ải 4, còn lại dùng ID 222
        int hp = switch (currentStage) {
            case 0 -> 150000000; 
            case 1 -> 30000000; 
            case 2 -> 1100000000; 
            case 3 -> 1800000000; 
            default -> 200;
        };
        int level = switch (currentStage) {
            case 0 -> 50;
            case 1 -> 60;
            case 2 -> 70;
            case 3 -> 80;
            default -> 50;
        };

        List<int[]> availableCoords = new ArrayList<>();
        for (int[] coord : BOSS_COORDINATES) {
            availableCoords.add(coord);
        }

        for (int i = 0; i < bossCount; i++) {
            if (availableCoords.isEmpty()) {
                Log.error("Hết tọa độ để sinh boss cho ải " + (currentStage + 1));
                break;
            }
            // Chọn tọa độ ngẫu nhiên
            int randIndex = NinjaUtils.nextInt(availableCoords.size());
            int[] coord = availableCoords.remove(randIndex);
            short x = (short) coord[0];
            short y = (short) coord[1];

            // Sinh boss
            Mob mob = new Mob(zone.getMonsters().size(), (short) mobId, hp, (short) level, x, y, false, true, zone);
            zone.addMob(mob);
            Log.debug("Sinh boss ID: " + mobId + " tại ải " + (currentStage + 1) + ", x: " + x + ", y: " + y + ", HP: " + hp);
        }

        // Kiểm tra số lượng boss
        long actualBossCount = zone.getMonsters().stream().filter(mob -> mob.isBoss).count();
        if (actualBossCount != bossCount) {
            Log.error("Số lượng boss trong ải " + (currentStage + 1) + " là " + actualBossCount +
                    ", không khớp với yêu cầu " + bossCount);
        }
    }

    private void updatePlayerStatus() {
        Zone zone = zones.get(0);
        long livingBosses = zone.getLivingMonsters().stream().filter(mob -> mob.isBoss && !mob.isDead).count();
        if (countDown % 30 == 0) {
            service.serverMessage("Còn " + livingBosses + " boss trong Ải " + (currentStage + 1) + ". Thời gian còn lại: " + countDown + " giây.");
        }
    }

    public void bossKilled(Char killer, boolean isSpecialBoss) {
        if (killer == null || killer.isDead || finished) {
            Log.debug("Không thể xử lý: killer=" + (killer == null ? "null" : killer.name) + ", finished=" + finished);
            return;
        }
        killer.serverMessage("Bạn đã tiêu diệt " + (isSpecialBoss ? "Đại Vương Vượt Ải" : "Boss Vượt Ải") + "!");
        Log.debug("Người chơi " + killer.name + " tiêu diệt " + (isSpecialBoss ? "đại vương" : "boss") +
                " trong SoloTrialDungeon ID: " + this.id);
    }

//    private void distributeStageRewards() {
//        if (player == null || player.isDead) {
//            Log.debug("Không thể trao phần thưởng: player=" + (player == null ? "null" : player.name) +
//                    ", isDead=" + (player != null && player.isDead));
//            return;
//        }
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("Hoàn thành Ải ").append(currentStage + 1).append("!\n");
//
//        if (currentStage < 3) {
//            int stoneQuantity = switch (currentStage) {
//                case 0 -> 2;
//                case 1 -> 5;
//                case 2 -> 8;
//                default -> 0;
//            };
//            int coinAmount = switch (currentStage) {
//                case 0 -> 3000000;
//                case 1 -> 7000000;
//                case 2 -> 12000000;
//                default -> 0;
//            };
//            int coinAmount1 = switch (currentStage) {
//                case 0 -> 200;
//                case 1 -> 700;
//                case 2 -> 800;
//                default -> 0;
//            };
//
//            if (stoneQuantity > 0) {
//                Item rewardItem = ItemFactory.getInstance().newItem(1384);
//                rewardItem.setQuantity(stoneQuantity);
//                player.addItemToBag(rewardItem);
//                sb.append("- Nhận được Đá Thiên Tự x").append(stoneQuantity).append("\n");
//            }
//            if (coinAmount > 0) {
//                player.addCoin(coinAmount);
//                sb.append("- Nhận được ").append(coinAmount).append(" Xu\n");
//            }
//            if (coinAmount1 > 0) {
//                player.addRuby(coinAmount1);
//                sb.append("- Nhận được ").append(coinAmount1).append(" Coin\n");
//            }
//        } else {
//            // Phần thưởng cho ải cuối (stage 3): 20% cơ hội nhận 1 item
//            if (NinjaUtils.nextInt(100) < 20) {
//                int[] possibleItems = {1453, 1454, 1455, 1456, 1457, 1458}; // Sách Võ Công 125
//                int randomItemId = possibleItems[new Random().nextInt(possibleItems.length)];
//                Item randomItem = ItemFactory.getInstance().newItem(randomItemId);
//                randomItem.setQuantity(1);
//                player.addItemToBag(randomItem);
//                sb.append("- Nhận được Sách Võ Công 125 (ID: ").append(randomItemId).append(")\n");
//            } else {
//                sb.append("- Không nhận được phần thưởng\n");
//            }
//        }
//
//        service.showAlert("Phần Thưởng Ải " + (currentStage + 1), sb.toString());
//        Log.debug("Người chơi " + player.name + " nhận phần thưởng Ải " + (currentStage + 1));
//    }

//    public void finish() {
//        if (finished) {
//            return;
//        }
//        finished = true;
//        service.serverMessage("Bạn đã vượt qua Thử Thách Vượt Ải! Nhận phần thưởng cuối và rời phó bản sau 20 giây.");
//        countDown = Math.max(countDown, 240);
//        service.sendTimeInMap(countDown);
//        if (player != null) {
//            player.hasCompletedSoloTrialToday = true;
//            player.updateSoloTrialStateToDB();
//            Log.debug("Người chơi " + player.name + " đã hoàn thành SoloTrialDungeon ID: " + this.id +
//                    ", trạng thái hasCompletedSoloTrialToday = true");
//        }
//
//        scheduler.schedule(() -> {
//            if (!isClosed) {
//                close();
//            }
//        }, 20, TimeUnit.SECONDS);
//    }

    @Override
    public void addMember(Char _char) {
        if (player != null && _char != player) {
            _char.serverMessage("Phó bản này chỉ cho phép một người chơi!");
            return;
        }
        super.addMember(_char);
        Log.debug("Thêm người chơi " + _char.name + " vào SoloTrialDungeon ID: " + this.id);
    }

    public void joinZone(Char _char, int map) {
        if (_char != player) {
            _char.serverMessage("Bạn không được phép vào map này!");
            return;
        }
        for (Zone z : zones) {
            if (z.tilemap.id == map) {
                z.join(_char);
                Log.debug("Người chơi " + _char.name + " vào zone map ID: " + map + " trong SoloTrialDungeon ID: " + this.id);
                return;
            }
        }
        Log.error("Không tìm thấy zone với map ID: " + map + " trong SoloTrialDungeon ID: " + this.id);
        close();
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }
        if (player != null && !player.isCleaned) {
            short[] xy = NinjaUtils.getXY(player.mapBeforeEnterPB);
            player.setXY(xy[0], xy[1]);
            player.changeMap(player.mapBeforeEnterPB);
            player.serverMessage("Phó bản Thử Thách Vượt Ải đã kết thúc.");
        }
        synchronized (dungeons) {
            dungeons.remove(this);
            Log.debug("Xóa SoloTrialDungeon ID: " + this.id);
        }
        scheduler.shutdown();
        super.close();
    }

    public int getCurrentStage() {
        return currentStage;
    }

    @Override
    public boolean enterWorld(Zone pre, Zone next) {
        return !pre.tilemap.isDungeo() && next.tilemap.isDungeo();
    }

    @Override
    public boolean leaveWorld(Zone pre, Zone next) {
        return pre.tilemap.isDungeo() && !next.tilemap.isDungeo();
    }
}