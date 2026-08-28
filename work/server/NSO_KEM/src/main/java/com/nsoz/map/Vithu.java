package com.nsoz.map;

import com.nsoz.model.Char;
import com.nsoz.server.GlobalService;
import com.nsoz.util.NinjaUtils;

import java.time.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Soanlv
 */
public class Vithu {
    public boolean isSanBoss;
    public boolean isDieVithu;

    public boolean dangKyVithu;

    // time mở cửa hang vĩ thú
    public long TIME_REGISTER = 300000L; // 5 phút
    public long TIME_START = 300000L; // 5 phút
    public long TIME_OPEN = 4500000L; // 1h15 phút
    public long TIME_END = 3600000L; // 1h

    public long timeVithu;
    public long lastTimeGK;
    public ArrayList<Char> users;

    public Vithu() {
        users = new ArrayList<>();
        this.notify("Hang Karasumori xuất hiện boss Thập Vĩ cực mạnh. Hãy mua vĩ thú lệnh ở Tabemono để Báo Danh vào hang từ 20h55p-21h00p");
    }

    public void notify(String text) {
        GlobalService.getInstance().chat("server", text);
    }

    public void initMap() {
        for (Map map : MapManager.getInstance().getMaps()) {
            if (map.id >= 169 && map.id <= 175) {
                map.setVithu(this);
                map.initZone();
            }
        }
    }

    public void addUser(Char _c) {
        users.add(_c);
    }

    public void removeUsers(Char _c) {
        users.remove(_c);
    }


    public void reSendNoty() {
        if (NinjaUtils.canDoWithTime(this.lastTimeGK, 5 * 60000)) {
            final Calendar rightNow = Calendar.getInstance();
            short phut = (short) rightNow.get(Calendar.MINUTE);
            if (phut == 5 || phut == 10 || phut == 15 || phut == 20 || phut == 25 || phut == 30 || phut == 35 || phut == 40) {
                this.notify("Hang Karasumori xuất hiện boss Thập Vĩ cực mạnh. Hãy mua vĩ thú lệnh ở Tabemono để Báo Danh vào hang từ 20h55p-21h00p");
                this.lastTimeGK = System.currentTimeMillis();
            }
        }
    }

    public void register() {
        this.isSanBoss = false;
        this.isDieVithu = false;
        this.dangKyVithu = true;
        this.timeVithu = System.currentTimeMillis() + TIME_OPEN;
        this.notify("Lối vào Hang Karasumori đã mở");
    }

    public void start() {
        this.isSanBoss = true;
        this.dangKyVithu = false;
        this.notify("Hang Karasumori đã mở");
    }

    public void end() {
        this.isSanBoss = false;
        this.isDieVithu = false;
        this.dangKyVithu = false;
        this.timeVithu = -1;
    }


    public void End() {
        final Calendar rightNow = Calendar.getInstance();
        short hour = (short) rightNow.get(Calendar.HOUR_OF_DAY);
        short phut = (short) rightNow.get(Calendar.MINUTE);
        if (hour == 20 && phut == 00) {
            end();
        }
    }

    public static void timer(int hours, int minutes, int seconds) {
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
//                    if (NinjaUtils.isToday(DayOfWeek.SUNDAY)) { /// vĩ thú mở vào chủ nhật
                        Vithu war = MapManager.getInstance().vithu = new Vithu();
                        war.initMap();
                        Thread.sleep(war.TIME_REGISTER);
                        war.register();
                        Thread.sleep(war.TIME_START);
                        war.start();
                        Thread.sleep(war.TIME_END);
                        war.End();
//                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(War.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, initalDelay, 1 * 24 * 60 * 60, TimeUnit.SECONDS);

    }
}
