/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nsoz.server;

import com.nsoz.clan.Clan;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.model.Char;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author PC
 */
public class Ranked  {

    // tính bxh top
    /// t7 đen tối
    public static final String[] NAME = {
            "Top Đại gia", //0
            "Top Cao Thủ", //1
            "Top Gia tộc", //2
            "Top Hang động",//3
            "Top Nạp",//4
            "Top VXMM", //5
            "Top Boss",//6
            "Top Đại Chiến"//7
    };

    public static final String[] RANKED_NAME = {"%d. %s có %s yên", //0
            "%d. %s trình độ cấp %d vào ngày %s",//1
            "%d. Gia tộc %s có trình độ cấp %d do %s làm tộc trưởng, thành viên %d/%d",//2
            "%d. %s nhận được %s rương",//3
            "%d. %s", "%d. %s có %s điểm", //4
            "%d. %s có %s điểm",//5
            "%d. %s đang có %s điểm", //6
            "%d. %s đang có %s điểm",//7
    };
// có tổng 7 cái => cái RANKED  phải khơởi taạo với độ dài là 8  not 9
    public static final Vector[] RANKED = new Vector[8];

    // load bxh
    public static void init() {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                refresh();
            }
        };
        long delay = 5 * 60 * 1000; // 5 phút = 1800000 milliseconds   load bxh
        Timer timer = new Timer("Ranked");
        timer.schedule(timerTask, 0, delay);
    }

    public static void rsetTopFor1Week() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Lấy thời gian hiện tại
        Calendar now = Calendar.getInstance();

//         Tính thời gian cần đến chủ nhật tiếp theo
        Calendar nextSunday = new GregorianCalendar();
        nextSunday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        nextSunday.set(Calendar.HOUR_OF_DAY,0);
        nextSunday.set(Calendar.MINUTE, 4);
        nextSunday.set(Calendar.SECOND, 0);
        nextSunday.set(Calendar.MILLISECOND, 0);


        if (now.after(nextSunday)) {
            nextSunday.add(Calendar.WEEK_OF_YEAR, 1);
        }

        // Tính thời gian cần delay đến thời điểm đó
        long delay = nextSunday.getTimeInMillis() - now.getTimeInMillis();

        // Lập lịch reset bảng xếp hạng
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Reset BXH top tuần");
            resetAll();

        }, delay, 7 * 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS); // Lập lịch reset sau mỗi 7 ngày


    }


    public static void refresh() {
        initTopDaiGia();
        initTopCaoThu();
        initTopGiaToc();
        initTopHangDong();
//        initTopNap();
        initTopVXMM();
//        initTopBoss();
        initTopGiaTocThu7(); /// t7 đen tối
    }

    public static void resetAll() {
        saveTop();

    }

    public static void saveTop() {
        try {
            DbManager.executeUpdate("UPDATE `players` SET `top_week_vxmm` = ?", 0);
//            DbManager.executeUpdate("UPDATE `players` SET `top_week_nap` = ?", 0);
//            DbManager.executeUpdate("UPDATE `players` SET `top_week_boss` = ?", 0);
//            DbManager.executeUpdate("UPDATE `players` SET `top_week_than_nong` = ?", 0);
        } catch (Exception e) {
            Log.logException("Lỗi save Top (1)  ",Ranked.class,e);

        }
        try {


            ResultSet res = DbManager.executeQuery("SELECT `id`, `topvxmm` FROM `players` WHERE `topvxmm` >= 1000 AND `server_id` = ? ORDER BY `topvxmm` DESC LIMIT 10;", Config.getInstance().getServerID());
            int i1 = 1;
            while (res.next()) {
                DbManager.executeUpdate("UPDATE `players` SET `top_week_vxmm` = ? WHERE `id`=?", i1, res.getInt("id"));
                i1++;
            }
            res.close();


//            res = DbManager.executeQuery("SELECT `id`, `topBoss` FROM `players` WHERE `topBoss` >= 200 AND `server_id` = ? ORDER BY `topBoss` DESC LIMIT 10;", Config.getInstance().getServerID());
//            int i2 = 1;
//            while (res.next()) {
//
//                DbManager.executeUpdate("UPDATE `players` SET `top_week_boss` = ? WHERE `id`=?", i2, res.getInt("id"));
//                i2++;
//
//            }
//            res.close();
//
//            res = DbManager.executeQuery("SELECT players.id, users.tongnap FROM players JOIN users ON players.user_id = users.id WHERE users.tongnap >= 2000000 ORDER BY users.tongnap DESC LIMIT 10;");
//
//            int i3 = 1;
//            while (res.next()) {
//                DbManager.executeUpdate("UPDATE `players` SET `top_week_nap` = ? WHERE `id`=?", i3, res.getInt("id"));
//                i3++;
//            }
//            res.close();
//
//
//            res = DbManager.executeQuery("SELECT `id`, `thannong` FROM `players` WHERE `thannong` >= 50000 AND `server_id` = ? ORDER BY `thannong` DESC LIMIT 10;", Config.getInstance().getServerID());
//
//            int i4 = 1;
//            while (res.next()) {
//                DbManager.executeUpdate("UPDATE `players` SET `top_week_than_nong` = ? WHERE `id`=?", i4, res.getInt("id"));
//                i4++;
//            }
//            res.close();

        } catch (Exception e) {
            Log.logException("Lỗi save Top (2)  ",Ranked.class,e);

        } finally {
            try {
                List<Char> chars = ServerManager.getChars();
                for (Char _char : chars) {
                    try {
                        if (_char != null && !_char.isCleaned) {
                            _char.setTopvxmmLuong(0);
                            _char.setTopvxmm(0);
                            _char.setTopBoss(0);
                            _char.setTopThanNong(0);
                            _char.saveData();
                        }
                    } catch (Exception ex) {
                        Log.logException("Lỗi save Top (3)  ",Ranked.class,ex);
                    }
                }
            } catch (Exception e) {
                Log.logException("Lỗi save Top (4)  ",Ranked.class,e);

            }
            try {
                DbManager.executeUpdate("UPDATE `players` SET `topvxmm` = ?", 0);
//                DbManager.executeUpdate("UPDATE `players` SET `topBoss` = ?", 0);
//                DbManager.executeUpdate("UPDATE `users` SET `tongnap` = ?", 0);
//                DbManager.executeUpdate("UPDATE `players` SET `thannong` = ?", 0);
            } catch (Exception e) {
                Log.logException("Lỗi save Top (5)  ",Ranked.class,e);

            }

            System.out.println("Update bxh top tuần xong");
        }
    }

    public static void initTopDaiGia() {
        try ( Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `name`, `yen` FROM `players` WHERE `yen` > 0 AND `server_id` = ? ORDER BY `yen` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                ranked.add(String.format(RANKED_NAME[0], i, res.getString("name"),
                        NinjaUtils.getCurrency(res.getInt("yen"))));
                i++;
            }
            res.close();
            stmt.close();
            RANKED[0] = ranked;
        } catch (SQLException ex) {
            Log.logException("init top dai gia err",Ranked.class,ex);

        }
    }


    public static void initTopCaoThu() {
        try(Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();
// soan
//            String query = "SELECT `name`, CAST(JSON_EXTRACT(data, \"$.exp\") AS SIGNED) AS `exp`, CAST(JSON_EXTRACT(data, \"$.levelUpTime\") AS SIGNED) AS `levelUpTime`\n" +
//                    "FROM players\n" +
//                    "WHERE `server_id` = ?\n" +
//                    "ORDER BY `exp` DESC, `levelUpTime` ASC\n" +
//                    "LIMIT 100;";

            // cuong
            String query =   "SELECT `name`, CAST(JSON_EXTRACT(data, \"$.exp\") AS SIGNED) AS `exp`, CAST(JSON_EXTRACT(data, \"$.levelUpTime\") AS SIGNED) AS `levelUpTime`\n" +
                    "FROM players\n" +
                    "WHERE `server_id` = ?\n" +
                    "ORDER BY `exp` DESC, `levelUpTime` ASC\n" +
                    "LIMIT 100;";

            // soan moi
//            String query = "SELECT name, " +
//                    "CAST(JSON_EXTRACT(data, '$.exp') AS SIGNED) AS exp, " +
//                    "CAST(JSON_EXTRACT(data, '$.levelUpTime') AS SIGNED) AS levelUpTime, " +
//                    "CAST(JSON_EXTRACT(data, '$.level') AS SIGNED) AS level " +  // Xóa dấu phẩy thừa ở đây
//                    "FROM players " +
//                    "WHERE server_id = ? " +
//                    "AND CAST(JSON_EXTRACT(data, '$.level') AS SIGNED) = 99 " +
//                    "ORDER BY levelUpTime ASC " +
//                    "LIMIT 10;";


//            PreparedStatement stmt = conn.prepareStatement(
//                    "SELECT `name`, CAST(JSON_EXTRACT(data, \"$.exp\") AS INT) AS `exp`, CAST(JSON_EXTRACT(data, \"$.levelUpTime\") AS INT) AS `levelUpTime` FROM players where `server_id` = ? ORDER BY `exp` DESC, `levelUpTime` ASC LIMIT 10;");
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            ArrayList<CaoThu> list = new ArrayList<>();
            while (res.next()) {
                CaoThu rank = new CaoThu();
                rank.level = NinjaUtils.getLevel(res.getLong("exp"));
                rank.time = res.getLong("levelUpTime");
                rank.name = res.getString("name");
                list.add(rank);
            }
            order(list);
            int i = 1;
            for (CaoThu c : list) {
                int level = c.level;
                String time = NinjaUtils.milliSecondsToDateString(c.time, "yyyy/MM/dd HH:mm:ss aa");
                ranked.add(String.format(RANKED_NAME[1], i, c.name, level, time));
                i++;
            }
            res.close();
            stmt.close();
            RANKED[1] = ranked;
        } catch (SQLException ex) {
            Log.logException("init top cao thu err",Ranked.class,ex);

        }

    }

    public static void initTopGiaToc() {
        try(Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();
            PreparedStatement stmt = conn
                    .prepareStatement("SELECT `id` FROM `clan` WHERE `level` > 1 AND `server_id` = ? ORDER BY `level` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                int id = res.getInt("id");
                Optional<Clan> g = Clan.getClanDAO().get(id);
                if (g != null && g.isPresent()) {
                    Clan clan = g.get();
                    ranked.add(String.format(RANKED_NAME[2], i, clan.getName(), clan.getLevel(), clan.getMainName(),
                            clan.getNumberMember(), clan.getMemberMax()));
                    i++;
                }
            }
            res.close();
            stmt.close();
            RANKED[2] = ranked;
        } catch (SQLException ex) {
            Log.logException("init top gia toc err",Ranked.class,ex);


        }
    }

    public static void initTopHangDong() {
        try(Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `name`, `rewardPB` FROM `players` WHERE `rewardPB` > 0 AND `server_id` = ? ORDER BY `rewardPB` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                ranked.add(String.format(RANKED_NAME[3], i, res.getString("name"),
                        NinjaUtils.getCurrency(res.getInt("rewardPB"))));
                i++;
            }
            res.close();
            stmt.close();
            RANKED[3] = ranked;
        } catch (SQLException ex) {
            Log.logException("init top hang dong err",Ranked.class,ex);

        }
    }




    private static void initTopNap() {
        try ( Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `name`, `tongnap` FROM `users` WHERE `tongnap` > 0 AND `id` = ? ORDER BY `tongnap` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                ranked.add(String.format(RANKED_NAME[4], i, res.getString("name"),
                        NinjaUtils.getCurrency(res.getInt("tongnap"))));
                i++;
            }
            res.close();
            stmt.close();
            RANKED[4] = ranked;
        } catch (SQLException ex) {
            Log.logException("init top nap err",Ranked.class,ex);

        }
    }

    private static void initTopVXMM() {
        try ( Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `name`, `topvxmm` FROM `players` WHERE `topvxmm` > 0 AND `server_id` = ? ORDER BY `topvxmm` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                ranked.add(String.format(RANKED_NAME[5], i, res.getString("name"),
                        NinjaUtils.getCurrency(res.getInt("topvxmm"))));
                i++;
            }
            res.close();
            stmt.close();
            RANKED[5] = ranked;
        } catch (SQLException ex) {
            Log.logException("init top vxmm err",Ranked.class,ex);

        }
    }

    private static void initTopBoss() {
        try ( Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `name`, `topBoss` FROM `players` WHERE `topBoss` > 0 AND `server_id` = ? ORDER BY `topBoss` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                ranked.add(String.format(RANKED_NAME[6], i, res.getString("name"),
                        NinjaUtils.getCurrency(res.getInt("topBoss"))));
                i++;
            }
            res.close();
            stmt.close();
            RANKED[6] = ranked;
        } catch (SQLException ex) {
            Log.logException("init top boss err",Ranked.class,ex);

        }
    }


    public static void initTopGiaTocThu7() { /// t7 đen tối
        try ( Connection conn = DbManager.getConnection();) {
            Vector<String> ranked = new Vector<>();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT `id` FROM `clan` WHERE `pointWarWeek` > 1 AND `server_id` = ? ORDER BY `pointWarWeek` DESC LIMIT 10;");
            stmt.setInt(1, Config.getInstance().getServerID());
            ResultSet res = stmt.executeQuery();
            int i = 1;
            while (res.next()) {
                int id = res.getInt("id");
                Optional<Clan> g = Clan.getClanDAO().get(id);
                if (g != null && g.isPresent()) {
                    Clan clan = g.get();
                    ranked.add(String.format(RANKED_NAME[7], i, clan.getName(),
                            NinjaUtils.getCurrency(clan.pointWarWeek)));
                    i++;
                }
            }
            res.close();
            stmt.close();
            RANKED[7] = ranked;
        } catch (SQLException ex) {
            Log.error("init top t7 den toi", ex);
        }
    }

    private static void order(List<CaoThu> ranks) {

        Collections.sort(ranks, new Comparator() {

            public int compare(Object o1, Object o2) {

                Integer level1 = ((CaoThu) o1).level;
                Integer level2 = ((CaoThu) o2).level;
                int sComp = level2.compareTo(level1);
                if (sComp != 0) {
                    return sComp;
                }
                Long x1 = ((CaoThu) o1).time;
                Long x2 = ((CaoThu) o2).time;
                return x1.compareTo(x2);
            }
        });
    }

}

class CaoThu {

    public String name;
    public long time;
    public int level;
}
