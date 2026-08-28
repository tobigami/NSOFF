package com.nsoz.model;

import com.nsoz.server.GlobalService;
import com.nsoz.server.ServerManager;
import com.nsoz.util.NinjaUtils;

import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/// t7 đen tối

public class BlackSaturday {
    
    public int status;
    
    private static final BlackSaturday instance = new BlackSaturday();

    public static BlackSaturday getInstance() {
        return instance;
    }
    
    public void start() {
        this.status = 1;
        GlobalService.getInstance().chat("Hệ Thống", "Thứ 7 đen tối đã chính thức bắt đầu");
    }
    
    public void end() {
        this.status = 2;
        List<Char> chars = ServerManager.getChars();
        for (Char _char : chars) {
            try {
                if (_char != null) {
                    if (_char.clan != null && _char.typePk == _char.PK_PHE) {
                        _char.setTypePk(_char.PK_NORMAL);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void timerStart(int hours, int minutes, int seconds) {
        if (!NinjaUtils.getDay(Date.from(Instant.now())).equals("Saturday")) {
            return;
        }
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId currentZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
        ZonedDateTime zonedNext5 = zonedNow.withHour(hours).withMinute(minutes).withSecond(seconds);
        if (zonedNow.compareTo(zonedNext5) > 0) {
            zonedNext5 = zonedNext5.plusDays(1);
        }

        Duration duration = Duration.between(zonedNow, zonedNext5);
        long initalDelay = duration.getSeconds();
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    BlackSaturday.getInstance().start();
                    Thread.sleep(10800000L);
                    BlackSaturday.getInstance().end();
                } catch (InterruptedException ex) {
                    Logger.getLogger(BlackSaturday.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, initalDelay, 1 * 24 * 60 * 60, TimeUnit.SECONDS);
    }
    
}
