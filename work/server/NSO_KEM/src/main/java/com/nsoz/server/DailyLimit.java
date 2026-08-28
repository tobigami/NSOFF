package com.nsoz.server;

import com.nsoz.map.world.Dungeon;
import com.nsoz.map.world.World;
import com.nsoz.model.AutoUseLogin;
import com.nsoz.model.Char;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Nới số lượt vào hang động trong ngày.
 *
 * countPB không phải bộ đếm mà là cờ: chỗ vào hang động kiểm "countPB == 1", và vào xong đặt về 0.
 * Đặt nó thành 10 trong cơ sở dữ liệu là hỏng -- không phải 0 nên không dùng được lệnh bài, cũng
 * không phải 1 nên không vào được. Nên cách đúng là giữ nguyên cờ và NẠP LẠI nó, tối đa mấy lần
 * một ngày tuỳ cấu hình.
 *
 * Nạp lại đi đúng những bước mà vật phẩm "Lệnh bài hang động" làm (Char.useItem, id 280): gỡ hang
 * động cũ, bật cờ, xoá điểm và cờ đã nhận thưởng -- có vậy lượt mới mới tính thưởng riêng.
 *
 * Chạy bằng một luồng nền vì không có chỗ móc nào khác: hàm xử lý lúc rời hang động nằm trong
 * Char.java, mà lớp đó dùng Lombok nên không biên dịch lại được.
 *
 * Cấu hình trong autouse.properties:  dungeon.per.day = 10   (1 là giữ nguyên luật gốc)
 */
public final class DailyLimit {

    private static final long PERIOD = 15000;
    /** charId -> {ngày trong năm, số lần đã nạp}. Sang ngày mới thì đếm lại từ đầu. */
    private static final Map<Integer, int[]> refills = new HashMap<>();
    private static boolean started;

    private DailyLimit() {
    }

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(PERIOD);
                        sweep();
                    } catch (InterruptedException ex) {
                        return;
                    } catch (Throwable ex) {
                        System.out.println("DailyLimit: " + ex);
                    }
                }
            }
        }, "daily-limit");
        t.setDaemon(true);
        t.start();
    }

    /** Số lượt hang động mỗi ngày theo cấu hình. 1 là giữ nguyên luật gốc. */
    public static int perDay() {
        return AutoUseLogin.setting("dungeon.per.day", 1);
    }

    /**
     * Số lượt hang động còn lại hôm nay của một nhân vật.
     *
     * Gồm lượt đang cầm trong tay (countPB == 1) cộng với số lần luồng này còn được phép nạp thêm.
     * Nhân vật chưa vào hang lần nào hôm nay thì chưa có bản ghi, tính là còn đủ cả ngày.
     */
    public static int leftToday(Char c) {
        int perDay = perDay();
        if (c == null || perDay <= 1) {
            return c != null && c.countPB == 1 ? 1 : 0;
        }
        int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int[] state = refills.get(Integer.valueOf(c.id));
        int daNap = (state == null || state[0] != today) ? 0 : state[1];
        int conNap = Math.max(0, perDay - 1 - daNap);
        return conNap + (c.countPB == 1 ? 1 : 0);
    }

    private static void sweep() {
        int perDay = perDay();
        if (perDay <= 1) {
            return;                       // giữ nguyên luật gốc, không đụng vào ai
        }
        int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        for (Char c : ServerManager.getChars()) {
            try {
                if (!TaskAdmin.alive(c) || !c.isHuman || c.countPB != 0) {
                    continue;
                }
                World w = c.findWorld(World.DUNGEON);
                if (w instanceof Dungeon && !((Dungeon) w).isClosed()) {
                    continue;             // đang ở trong hang động, chưa xong lượt hiện tại
                }
                int[] state = refills.get(Integer.valueOf(c.id));
                if (state == null || state[0] != today) {
                    state = new int[] { today, 0 };
                    refills.put(Integer.valueOf(c.id), state);
                }
                // Một lượt gốc mỗi ngày, phần còn lại là do đây nạp.
                if (state[1] >= perDay - 1) {
                    continue;
                }
                c.removeWorld(World.DUNGEON);
                c.countPB = 1;
                c.pointPB = 0;
                c.receivedRewardPB = false;
                state[1]++;
                c.serverMessage("Đã nạp lại lượt hang động (" + (state[1] + 1) + "/" + perDay + " hôm nay).");
            } catch (Throwable ex) {
                System.out.println("DailyLimit: " + ex);
            }
        }
    }
}
