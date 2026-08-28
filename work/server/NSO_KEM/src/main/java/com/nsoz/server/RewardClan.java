package com.nsoz.server;

import com.nsoz.clan.Clan;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// t7 đen tối


public class RewardClan {
    
    private static final RewardClan instance = new RewardClan();

    public static RewardClan getInstance() {
        return instance;
    }
    
    public void reward(int hours, int minutes, int seconds) {
//        NinjaUtils.schedule(() -> {
//            if (NinjaUtils.getDay(Date.from(Instant.now())).equals("Saturday")) {
//                resetTOP();
//            }
//        }, hours, minutes, seconds);

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            // Lấy thời gian hiện tại
            Calendar now = Calendar.getInstance();

//         Tính thời gian cần đến t7 tiếp theo
        Calendar nextSaturday = new GregorianCalendar();
        nextSaturday.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        nextSaturday.set(Calendar.HOUR_OF_DAY, hours);
        nextSaturday.set(Calendar.MINUTE, minutes);
        nextSaturday.set(Calendar.SECOND, seconds);
        nextSaturday.set(Calendar.MILLISECOND, 0);


        if (now.after(nextSaturday)) {
            nextSaturday.add(Calendar.WEEK_OF_YEAR, 1);
        }

            // Tính thời gian cần delay đến thời điểm đó
        long delay = nextSaturday.getTimeInMillis() - now.getTimeInMillis();


        // Lập lịch reset bảng xếp hạng
            scheduler.scheduleAtFixedRate(() -> {
                System.out.println("Reset BXH top t7");
                resetTOP();

            }, delay, 7 * 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS); // Lập lịch reset sau mỗi 7 ngày



    }
    
    private void resetTOP() {
        try ( Connection conn = DbManager.getConnection();) {

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `name`, `pointWarWeek` FROM `clan` WHERE `pointWarWeek` > 0 AND `server_id` = ? ORDER BY `pointWarWeek` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                String name = res.getString("name");
                Clan clan = Clan.mapClan.get(name);
                if (clan != null) {
                    clan.topWarWeek = i;
                }
                i++;
            }
            res.close();
            stmt.close();

            List<Clan> clans = Clan.getClanDAO().getAll();
            synchronized (clans) {
                for (Clan clan : clans) {
                    clan.pointWarWeek = 0;
                    Clan.getClanDAO().update(clan);
                }
            }
        } catch (SQLException ex) {
            Log.error("init qua thu 7 den toi", ex);
        }    
    }
}
