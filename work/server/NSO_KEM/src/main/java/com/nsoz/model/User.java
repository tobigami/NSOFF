package com.nsoz.model;

import java.io.EOFException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import com.nsoz.ability.AbilityFromEquip;
import com.nsoz.clan.Clan;
import com.nsoz.clan.Member;
import com.nsoz.constants.SQLStatement;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.fashion.FashionFromEquip;
import com.nsoz.item.Equip;
import com.nsoz.item.Item;
import com.nsoz.item.ItemTemplate;
import com.nsoz.item.Mount;
import com.nsoz.map.Map;
import com.nsoz.map.MapManager;
import com.nsoz.network.Controller;
import com.nsoz.network.Message;
import com.nsoz.network.Service;
import com.nsoz.network.Session;
import com.nsoz.server.Config;
import com.nsoz.server.GlobalService;
import com.nsoz.server.NinjaSchool;
import com.nsoz.server.ServerManager;
import com.nsoz.task.TaskOrder;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import com.nsoz.util.StringUtils;

public class User {
    private static Object LOCK = new Object();
    private static int a = 0;
    public int upDieEndMp;
    public Session session;
    public Service service;
    public Vector<Char> chars;
    public int id;
    public String username;
    public String name;
    public String password;
    public String random;
    public int activated;
    private boolean isCheckLock;
    public Timestamp banUntil;
    public int gold;
    public int tongnap;
    public int first_gift;
    public Char sltChar;
    public int receivedFirstGift;
    public int SilverRank;
    public int GoldRank;
    public int DiamondRank;
    public int FirstGift;
    public String phone;
    public long lastAttendance;
    public long firstAttendance;
    public boolean isLoadFinish;
    public boolean isEntered;
    public boolean isCleaned;

    public boolean isDuplicate;
    public int[] levelRewards = new int[5];
    public int isTien = 0;
    public int role;
    public ArrayList<String> IPAddress;
    public boolean SVIP;
    public boolean rank200k;
    public boolean rank500k;
    public boolean rankSilver;
    public boolean rankGold;
    public boolean rankDiamond;
    public boolean rankFirstGift;
    private int status;
    private boolean saving;
    public static HashMap<String, Long> timeWaitLogin = new HashMap();
    public int[] rewardTongnap; /// quà nạp
    public int[] giftLogin;
    public int dayLogin;
    public long lastLogoutTime;
    public long lastLoginTime;

    public User(Session client, String username, String password, String random) {
        this.session = client;
        this.service = client.getService();
        this.username = username;
        this.password = password;
        this.random = random;
    }

    public static void newPlay(String rand, User us) {
        try (Connection conn = DbManager.getConnection();) {

            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM `users` WHERE `username` = ? LIMIT 1;",
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

            try {
                stmt.setString(1, rand);
                ResultSet result = stmt.executeQuery();
                if (!result.next()) {
                    PreparedStatement stmt2 = conn.prepareStatement(
                            "INSERT INTO `users`(`username`, `password`, `online`, `luong`) VALUES (?, ?, ?, ?);");
                    try {
                        stmt2.setString(1, rand);
                        stmt2.setString(2, "kitakeyos");
                        stmt2.setInt(3, 0);
                        stmt2.setInt(4, 999);
                        stmt2.executeUpdate();
                    } finally {
                        stmt2.close();
                    }
                }
                result.close();
            } finally {
                stmt.close();
            }
        } catch (SQLException ex) {
        }
    }

    public HashMap<String, Object> getUserMap() {

        try (Connection conn = DbManager.getConnection();) {
            ArrayList<HashMap<String, Object>> list;

            PreparedStatement stmt = conn.prepareStatement(SQLStatement.GET_USER, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setString(1, this.username);
            ResultSet data = stmt.executeQuery();
            try {
                list = DbManager.convertResultSetToList(data);
            } finally {
                data.close();
                stmt.close();
            }
            if (list.isEmpty()) {
                return null;
            }
            HashMap<String, Object> map = list.get(0);
            if (map != null) {
                String passwordHash = (String) map.get("password");
                if (!StringUtils.checkPassword(passwordHash, password)) {
                    return null;
                }
            }
            return map;
        } catch (SQLException e) {
            Log.logException("getUserMap() err", User.class, e);
        }
        return null;
    }

    public void login() {

        if (a == 0) {

            a++;
        }

        try {
            if (username.equals("-1") && password.equals("12345")) {
                service.serverDialog("Hãy liên hệ ADMIN để tạo tài khoản");
                return;
            }
            Pattern p = Pattern.compile("^[a-zA-Z0-9]+$");
            Matcher m1 = p.matcher(username);
            if (!m1.find()) {
                service.serverDialog("Tên tài khoản có kí tự lạ.");
                return;
            }

            HashMap<String, Object> map = getUserMap();
            if (map == null) {
                if (Config.getInstance().getServerID() == 2) {
                    this.username = this.username + "sv2";
                    map = getUserMap();
                    this.isDuplicate = true;
                }
                if (map == null) {
                    // Ghi lại để phân biệt hai chuyện nhìn từ ngoài giống hệt nhau: máy khách
                    // không tới được máy chủ (sẽ không có dòng nào), hay tới được nhưng gõ sai
                    // tài khoản (có dòng này kèm đúng tên đã gõ và địa chỉ máy gửi).
                    System.out.println("ĐĂNG NHẬP HỎNG: tài khoản \"" + username + "\" từ "
                            + (session == null ? "?" : session.IPAddress));
                    service.serverDialog("Tài khoản hoặc mật khẩu không chính xác.");
                    return;
                }
            }

            this.id = (int) (map.get("id"));
            this.phone = (String) map.get("phone");
            this.role = (int) map.get("role");
            this.lastAttendance = (long) map.get("last_attendance_at");
            this.receivedFirstGift = (int) map.get("gift");
            this.SilverRank = (int) map.get("silver_rank");
            this.GoldRank = (int) map.get("gold_rank");
            this.DiamondRank = (int) map.get("diamond_rank");
//            this.gold = (int) map.get("luong");
            this.tongnap = (int) map.get("tongnap");
            this.FirstGift = (int) map.get("first_gift");
            if (this.tongnap >= 2000000) {
                this.rankSilver = true;
            }
            if (this.tongnap >= 5000000) {
                this.rankGold = true;
            }
            if (this.tongnap >= 10000000) {
                this.rankDiamond = true;
            }
            if (this.tongnap >= 30000) {
                this.rankFirstGift = true;
            }

            this.isTien = (int) map.get("isTien");
            this.status = (int) map.get("status");
            this.activated = (int) map.get("activated");
//            this.upDieEndMp = ((int)map.get("upDieEndMp"));

            int status = (int) map.get("status");
//            byte lock = (byte) map.get("lock");
            ArrayList<String> names = getName();
            long time = getTimeBaotri();
            if (time > 0 && names != null) {

                long now = System.currentTimeMillis();
                long timeRemaining = time - now;
                if (!names.contains(username)) {
                    if (timeRemaining > 0) {
                        service.serverDialog(String.format("Server sẽ được mở sau %s", NinjaUtils.timeAgo((int) (timeRemaining / 1000))));
//                        service.serverDialog(String.format("Server đang bảo trì ra sự kiện"));
                        return;
                    }
                }
            }


            Object obj = map.get("ban_until");
            if (obj != null) {
                this.banUntil = (Timestamp) obj;
                long now = System.currentTimeMillis();
                long timeRemaining = banUntil.getTime() - now;
                if (timeRemaining > 0) {
                    service.serverDialog(String.format("Tài khoản bị khóa trong %s. Vui lòng liên hệ admin để biết thêm chi tiết.", NinjaUtils.timeAgo((int) (timeRemaining / 1000))));
                    return;
                }
            }
//            if (this.activated != 5) {
//                service.serverDialog("đang bảo trì chút ạ");
//                return;
//            }
            if (this.status == 1) {
                service.serverDialog("Tài khoản của bạn đã bị khóa . Để biết thêm thông tin hãy liên hệ ADMIN");
                return;
            }

            // if (Config.getInstance().SERVER == 2) {
            JSONArray rewards = (JSONArray) JSONValue.parse(map.get("level_reward").toString());
            for (int i = 0; i < rewards.size(); i++) {
                this.levelRewards[i] = Integer.parseInt(rewards.get(i).toString());
            }
            // }

            JSONArray rewardNap = (JSONArray) JSONValue.parse(map.get("nap_reward").toString()); /// quà nạp
            this.rewardTongnap = new int[rewardNap.size()];
            for (int i = 0; i < rewardNap.size(); i++) {
                this.rewardTongnap[i] = Integer.parseInt(rewardNap.get(i).toString());
            }
            this.dayLogin =  (int) map.get("dayLogin");
            JSONArray rewardLogin = (JSONArray) JSONValue.parse(map.get("giftLogin").toString()); /// quà nạp
            this.giftLogin = new int[rewardLogin.size()];
            for (int i = 0; i < rewardLogin.size(); i++) {
                this.giftLogin[i] = Integer.parseInt(rewardLogin.get(i).toString());
            }
            this.lastLoginTime = (long) map.get("last_login_time");
            this.IPAddress = new ArrayList<>();
            obj = map.get("ip_address");
            if (obj != null) {
                String str = obj.toString();
                if (!str.equals("")) {
                    JSONArray jArr = (JSONArray) JSONValue.parse(str);
                    int size = jArr.size();
                    for (int i = 0; i < size; i++) {
                        IPAddress.add(jArr.get(i).toString());
                    }
                }
            }
            if (!IPAddress.contains(session.IPAddress)) {
                IPAddress.add(session.IPAddress);
            }
            // fix login nhiều lần
//            if (timeWaitLogin.containsKey(username)) {
//                if (System.currentTimeMillis() < (Long) timeWaitLogin.get(username)) {
//                    service.serverDialog("Bạn chỉ có thể đăng nhập lại vào tài khoản sau " + ((Long) timeWaitLogin.get(username) - System.currentTimeMillis()) / 1000L + "s nữa");
//                    return;
//                }
//                timeWaitLogin.remove(username);
//            }

            synchronized (LOCK) {

                User u = ServerManager.findUserByUsername(this.username);
                if (u != null && !u.isCleaned) {
                    service.serverDialog("Tài khoản đã có người đăng nhập.");
                    if (u.session != null && u.session.getService() != null) {
                        u.session.getService().serverDialog("Có người đăng nhập vào tài khoản của bạn.");
                    }
                    NinjaUtils.setTimeout(() -> {
                        try {
                            if (!u.isCleaned) {
                                u.session.disconnect();
                            }
                        } catch (Exception e) {
                        } finally {
                            ServerManager.removeUser(u);
                        }
                    }, 1000);
                    return;
                }
                ServerManager.addUser(this);
            }

//            boolean isOnline = ((int) map.get("online")) == 1;
//             if (isOnline) {
//                service.serverDialog("Tài khoản đang có người đăng nhập kìa");
//                forceOutOtherServer();
//                return;
//            }
//
//            setNinjaOnline();
            if (a == 1) {

                a++;
            }
            this.isLoadFinish = true;
        } catch (Exception ex) {
            Log.logException("login err", User.class, ex);
        }
    }

    public int getCountCheckout() {
        try (Connection conn = DbManager.getConnection()) {
            PreparedStatement stmt1 = conn.prepareStatement(
                    SQLStatement.getCountCheckOut, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt1.setString(1, this.username);
            ResultSet rs1 = stmt1.executeQuery();
            try {
                if (rs1.next()) {
                    String obj = rs1.getString("countCheckout");
                    if (obj != null) {
                        return Integer.parseInt(obj);
                    } else {
                        return 0;
                    }
                }
            } finally {
                stmt1.close();
                rs1.close();
            }
        } catch (SQLException ex) {
            Log.logException("Lỗi lấy số lần đểm danh: ", User.class, ex);

            return 0;
        }
        return 0;
    }

    public long getTimeBaotri() {
        try (Connection conn = DbManager.getConnection()) {
            PreparedStatement stmt1 = conn.prepareStatement(
                    SQLStatement.LOAD_NOTIFY, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt1.setString(1, "timebaotri");
            ResultSet rs1 = stmt1.executeQuery();
            try {
                if (rs1.next()) {
                    Timestamp obj = rs1.getTimestamp("value");
                    if (obj != null) {
                        return obj.getTime();
                    } else {
                        return 0;
                    }
                }
            } finally {
                stmt1.close();
                rs1.close();
            }
        } catch (SQLException ex) {
            Log.logException("Lỗi lấy Thông tin giờ bảo trì: ", User.class, ex);

            return 0;
        }
        return 0;
    }

    public ArrayList<String> getName() {
        try (Connection conn = DbManager.getConnection()) {
            PreparedStatement stmt1 = conn.prepareStatement(
                    SQLStatement.LOAD_NOTIFY, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt1.setString(1, "name_baotri");
            ResultSet rs1 = stmt1.executeQuery();
            ArrayList<String> names = new ArrayList<>();
            try {
                if (rs1.next()) {
                    String obj = rs1.getString("value");
                    name = obj;
                    if (obj != null) {
                        names.addAll(Arrays.asList(obj.split(",")));

                    }
                    return names;
                }
            } finally {
                stmt1.close();
                rs1.close();
            }
        } catch (SQLException ex) {
            Log.logException("Lỗi lấy Thông tin tên bảo trì: ", User.class, ex);

            return null;
        }
        return null;
    }

    public void forceOutOtherServer() {
        try {
            if (this.sltChar != null) {
                if (!this.isCleaned) {
                    ((Service) this.session.getService()).serverDialog("Có người đăng nhập vào tài khoản của bạn.");
                    this.session.disconnect();
                }
            }
            setOffline();
        } catch (Exception e) {

        }
    }

    public void setOffline() {
        try {

            DbManager.executeUpdate("UPDATE `users` SET `online` = ? WHERE `id`=?", 0, this.id);
            if (this.sltChar != null) {
                DbManager.executeUpdate("UPDATE `players` SET `online` = ? WHERE `id` = ?", 0, this.sltChar.id);
            }

        } catch (Exception e) {
            Log.logException("Lỗi set offline all: ", User.class, e);

        }
    }

    public void initCharacterList() {

        try (Connection conn = DbManager.getConnection();) {
            PreparedStatement stmt = conn.prepareStatement("SELECT `players`.`id`, `players`.`name`, `players`.`gender`, `players`.`class`, `players`.`last_logout_time`, `players`.`head`, `players`.`head2`, `players`.`body`, `players`.`weapon`, `players`.`leg`, `players`.`online`, CAST(JSON_EXTRACT(data, \"$.exp\") AS INT) AS `exp` FROM `players` WHERE `players`.`user_id` = ? AND `players`.`server_id` = ? ORDER BY `players`.`last_logout_time` DESC LIMIT 3;");
            stmt.setInt(1, this.id);
            stmt.setInt(2, Config.getInstance().getServerID());
            ResultSet data = stmt.executeQuery();
            try {
                this.chars = new Vector<>();
                while (data.next()) {
                    int id = data.getInt("id");
                    Char _char = new Char(id);
                    _char.loadDisplay(data);
                    this.chars.add(_char);
                }
            } finally {
                data.close();
                stmt.close();

            }
        } catch (Exception ex) {
            Log.logException("Lỗi init nhân vật:", User.class, ex);

        }
    }


    public void createCharacter(Message ms) {
        try (Connection conn = DbManager.getConnection();) {
//            if (true) {
//                service.serverDialog("Chức năng này tạm bảo trì.");
//                return;
//            }
            if (this.chars.size() >= 3) {
                service.serverDialog("Bạn chỉ được tạo tối đa 3 nhân vật.");
                return;
            }
            String name = ms.reader().readUTF();
            Pattern p = Pattern.compile("^[a-z0-9]+$");
            Matcher m1 = p.matcher(name);
            if (!m1.find()) {
                service.serverDialog("Tên nhân vật không được chứa ký tự đặc biệt!");
                return;
            }
            byte gender = ms.reader().readByte();
            byte head = ms.reader().readByte();
            byte[] h = null;
            if (gender == 0) {
                h = new byte[]{11, 26, 27, 28};
                gender = 0;
            } else {
                h = new byte[]{2, 23, 24, 25};
                gender = 1;
            }
            byte temp = h[0];
            for (byte b : h) {
                if (head == b) {
                    temp = b;
                    break;
                }
            }
            head = temp;
            if (name.length() < 5 || name.length() > 15) {
                service.serverDialog("Tên tài khoản chỉ cho phép từ 5 đến 15 ký tự!");
                return;
            }


            try (Connection conn2 = DbManager.getConnection()) {
                PreparedStatement statement = conn2.prepareStatement("SELECT * FROM `players` WHERE `user_id` = ?;", ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
                statement.setInt(1, this.id);
                ResultSet check = statement.executeQuery();

                if (check.last()) {
                    if (check.getRow() >= 3) {
                        service.serverDialog("Bạn đã tạo tối đa số nhân vật!");
                        return;
                    }
                }
                check.close();
                statement.close();
            } catch (Exception e) {
                Log.logException("Lỗi tạo nhân vật: ", User.class, e);


            }


            try (Connection conn3 = DbManager.getConnection()) {

                PreparedStatement statement = conn3.prepareStatement("SELECT * FROM `players` WHERE `name` = ?;", ResultSet.TYPE_SCROLL_SENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);
                statement.setString(1, name);
                ResultSet check = statement.executeQuery();

                if (check.last()) {
                    if (check.getRow() > 0) {
                        service.serverDialog("Tên nhân vật đã tồn tại!");
                        return;
                    }
                }


                check.close();
                statement.close();
            } catch (Exception e) {
                Log.logException("Lỗi kiểm tra  nhân vật: ", User.class, e);

            }

            try {
                DbManager.executeUpdate(
                        "INSERT INTO players(`user_id`, `server_id`, `name`, `gender`, `head`, `xu`, `yen`, `skill`, `equiped`, `bag`, `box`, `mount`, `effect`, `friends`,`data`,`fashion`,`bijuu`,`enemies`) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?)"
                        , this.id, Config.getInstance().getServerID(), name, gender, head, 0, 0, "[{\"id\":0,\"point\":0}]", "[]", "[]", "[]", "[]", "[]", "[]",
                        "{\"numberUseExpanedBag\":0,\"pointAo\":0,\"limitKyNangSo\":0,\"auto\":{\"type_pick_item\":0,\"range\":-1},\"pointPhu\":0,\"pointVuKhi\":0,\"countLoopBoss\":2,\"pointUyDanh\":0,\"pointNon\":0,\"countPB\":0,\"hieuChien\":0,\"limitBangHoa\":0,\"pointNgocBoi\":0,\"exp\":0,\"pointPB\":0,\"pointTinhTu\":0,\"reward\":\"[false,false,false,false,false]\",\"limitPhongLoi\":0,\"pointGangTay\":0,\"pointNhan\":0,\"pointLien\":0,\"pointGiay\":0,\"countDoneTaskDay\":20,\"countFinishDay\":20,\"levelUpTime\":0,\"expDown\":0,\"limitTiemNangSo\":0,\"pointQuan\":0}",
                        "[]", "[]", "[]");


            } catch (Exception e) {
                Log.logException("Lỗi thêm  nhân vật: ", User.class, e);

                throw new RuntimeException(e);
            }
            initCharacterList();


            service.selectChar(chars);
        } catch (IOException | SQLException e) {
            Log.logException("create char err", User.class, e);
            service.serverDialog("Tạo nhân vật thất bại!");
        }
    }

    public Char getCharByName(String name) {
        for (Char _char : this.chars) {
            if (_char.name.equals(name)) {
                return _char;
            }
        }
        return null;
    }

    public void selectChar(Message ms) {
        try {

            if (NinjaSchool.isStop) {
                service.serverDialog("Máy chủ bảo trì vui lòng thoát game để tránh mất dữ liệu.");
                Thread.sleep(1000);
                if (!isCleaned) {
                    session.disconnect();
                }
                return;
            }
            if (isEntered) {
                return;
            }
            String name = "";
            try {
                name = ms.reader().readUTF();
            } catch (EOFException e) {
                return;
            }

            if (chars == null) {
                return;
            }

            if (this.SVIP) {
                name = "[SVIP] " + name;
            }
            forceOutOtherServer();
            sltChar = getCharByName(name);
            if (sltChar == null) {
                System.out.println("[vao-game] ngat: khong tim thay nhan vat ten '" + name + "'");
                session.disconnect();
                return;
            }
            if (sltChar.online) {
                service.serverDialog("Nhân vật chưa lưu xong dữ liệu.");
                Thread.sleep(1000);
                if (!isCleaned) {
                    session.disconnect();
                }
                return;
            }
            chars = null;
            if (sltChar != null) {
                long now = System.currentTimeMillis();
                long lastTime = sltChar.lastLogoutTime + 8000;
                int num = (int) ((lastTime - now) / 1000);
//                if (num > 0) {
//                    service.serverDialog("Bạn chỉ có thể vào lại game sau " + num + " giây nữa");
//                    return;
//                }
                sltChar.user = this;
                if (!sltChar.load()) {
                    System.out.println("[vao-game] ngat: " + sltChar.name + " -- Char.load() tra ve false");
                    session.disconnect();
                    return;
                }

                if (sltChar.coin < 0 || sltChar.coinInBox < 0 || sltChar.yen < 0 || this.gold < 0) {
                    System.out.println("[vao-game] khoa: " + sltChar.name + " co so am -- xu " + sltChar.coin
                            + " ruong " + sltChar.coinInBox + " yen " + sltChar.yen + " luong " + this.gold);
                    lock();
                    return;
                }
                Controller controller = (Controller) session.getMessageHandler();
                controller.setChar(sltChar);
                sltChar.setService(this.service);
                sltChar.setLanguage(session.language);
                service.setChar(this.sltChar);
                byte zoneId = 0;
                int map = sltChar.mapId;
                Map m = MapManager.getInstance().find(map);
                if (m.tilemap.isNotSave()) {
                    map = sltChar.saveCoordinate;
                }
                boolean isException = false;
                try {
                    zoneId = NinjaUtils.randomZoneId(map);
                    if (zoneId == -1) {
                        isException = true;
                    }
                } catch (Exception e) {
                    isException = true;
                }
                if (isException) {
                    map = sltChar.saveCoordinate;
                    zoneId = NinjaUtils.randomZoneId(map);
                    short[] xy = NinjaUtils.getXY(map);
                    sltChar.setXY(xy[0], xy[1]);
                }
                System.out.println("[vao-game] " + sltChar.name + " nap xong, dang dung chi so...");
                this.sltChar.setFashionStrategy(new FashionFromEquip());
                this.sltChar.setAbilityStrategy(new AbilityFromEquip());
                this.sltChar.setAbility();
                this.sltChar.hp = this.sltChar.maxHP;
                this.sltChar.mp = this.sltChar.maxMP;
                sltChar.setFashion();
                sltChar.invite = new Invite();
                ServerManager.addChar(sltChar);
                AutoUseLogin.apply(sltChar);
                History history = new History(sltChar.id, History.ONLINE);
                for (Item item : sltChar.bag) {
                    if (item != null) {
                        history.addItem(History.HANH_TRANG, item);
                    }
                }
                for (Item item : sltChar.box) {
                    if (item != null) {
                        history.addItem(History.RUONG_DO, item);
                    }
                }
                for (Equip item : sltChar.equipment) {
                    if (item != null) {
                        history.addItem(History.TRANG_BI, item);
                    }
                }
                for (Mount item : sltChar.mount) {
                    if (item != null) {
                        history.addItem(History.THU_CUOI, item);
                    }
                }
                history.setBefore(sltChar.coin, this.gold, sltChar.yen);
                history.setAfter(sltChar.coin, this.gold, sltChar.yen);
                history.setIPAddress(session.IPAddress);
                history.setTime(System.currentTimeMillis());
                History.insert(history);
                this.service.sendDataBox();
                this.service.loadAll();
                this.service.meLoadActive();
                MapManager.getInstance().joinZone(sltChar, map, zoneId);
                service.onBijuuInfo(this.id, sltChar.bijuu);
                isEntered = true;
                sltChar.getEm().displayAllEffect(service, null, sltChar);
                for (Item item : sltChar.bag) {
                    if (item != null) {
                        if (item.template.isTypeBody() || item.template.isTypeMount()
                                || item.template.isTypeNgocKham()) {
                            service.itemInfo(item, (byte) 3, (byte) item.index);
                        }
                    }
                }
                if (sltChar.equipment[ItemTemplate.TYPE_THUNUOI] != null) {
                    sltChar.getEm().setEffectPet();
                }
                Clan clan = sltChar.clan;
                if (clan != null) {
                    Member mem = clan.getMemberByName(name);
                    if (mem != null) {
                        mem.setOnline(true);
                        mem.setChar(sltChar);
                    }
                    clan.getClanService().requestClanMember();
                }
                service.sendSkillShortcut("OSkill", sltChar.onOSkill, (byte) 0);
                service.sendSkillShortcut("KSkill", sltChar.onKSkill, (byte) 0);
                service.sendSkillShortcut("CSkill", sltChar.onCSkill, (byte) 0);
                if (sltChar.taskMain != null) {
                    sltChar.updateTaskLevelUp();
                    this.service.sendTaskInfo();
                }
                for (TaskOrder task : sltChar.taskOrders) {
                    service.sendTaskOrder(task);
                }
                loadNotify();
//                if (!sltChar.message.equals("")) {
//                    this.service.showAlert("server", sltChar.message);
//                    sltChar.message = "";
//                } else {
//                    String notification = Config.getInstance().getNotification();
//                    if (notification != null) {
//                        this.service.showAlert("Thông Báo", notification);
//                    }
//                }
                DbManager.executeUpdate("UPDATE `users` SET `online` = ? WHERE `id` = ?", 1, this.id);


                sltChar.lastLoginTime = System.currentTimeMillis();
                this.lastLoginTime = System.currentTimeMillis();
                DbManager.executeUpdate("UPDATE `players` SET `online` = ?, `last_login_time` = ? WHERE `id` = ? LIMIT 1;", 1, sltChar.lastLoginTime, sltChar.id);
                DbManager.executeUpdate("UPDATE `users` SET `last_login_time` = ? WHERE `id` = ? LIMIT 1;",  this.lastLoginTime, this.id);
//                notifySvipGlobal();
//                sltChar.giftcodeUnpaid();
                sltChar.goldUnpaid();
                sltChar.yenUnpaid();
                sltChar.coinUnpaid();
                sltChar.checkExpireMount();
                session.setName(sltChar.name);
                if (sltChar.isCool()) {
                    sltChar.serverMessage("Lạnh quá, sức đánh và khả năng hồi phục của bạn bị giảm đi 50%, hãy tìm gosho để mua lãnh dược!");
                }
                // ecuong fix vdmq

            } else {
                session.disconnect();
            }

        } catch (Exception ex) {
            Log.logException("Lỗi select nhân vật:", User.class, ex);

        }
    }

    private void loadNotify() {
        try (Connection conn = DbManager.getConnection();) {
            PreparedStatement stmt1 = conn.prepareStatement(
                    SQLStatement.LOAD_NOTIFY, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt1.setString(1, "thongbaogame");
            ResultSet rs1 = stmt1.executeQuery();
            String content = "";
            try {
                if (rs1.next()) {
                    Object obj1 = rs1.getObject("value");
                    if (obj1 != null) {
                        content = obj1.toString();
                    }
                }
            } finally {
                rs1.close();
                stmt1.close();
            }
            // Phần chữ tay lấy từ cơ sở dữ liệu ở trên chỉ là dòng đầu; những số liệu còn lại do
            // Welcome ghép vào lúc chạy (lượt hang động của chính người này, tỉ lệ kinh nghiệm,
            // sự kiện, số người đang chơi).
            this.service.showAlert("server", com.nsoz.server.Welcome.text(this.sltChar, content));
        } catch (Exception e) {

            Log.logException("Load notifi error:", User.class, e);

        }
    }


    public boolean isTien() {
        return this.isTien == 0 ? false : true;
    }


    public void lock() {
        this.lock("");
    }

    public void lock(String message) {
        try {
            DbManager.executeUpdate("UPDATE `users` SET `status` = 1 WHERE `id` = ? LIMIT 1;", this.id);
            session.disconnect();
        } catch (Exception e) {
        }
    }

    // fix đổi lượng và band web


    public void lock(int hours) {
        try {
            DbManager.executeUpdate("UPDATE `users` SET `ban_until` = ? WHERE `id` = ? LIMIT 1;", new Timestamp(System.currentTimeMillis() + hours * 60 * 60 * 1000), this.id);
            session.disconnect();
        } catch (Exception e) {
        }
    }

    public void saveData() {
        try {
            if (isLoadFinish && !saving) {
                saving = true;
                try {
                    JSONArray list = new JSONArray();
                    for (String ip : IPAddress) {
                        list.add(ip);
                    }
                    JSONArray rewards = new JSONArray();
                    for (int i = 0; i < 5; i++) {
                        rewards.add(levelRewards[i]);
                    }

                    JSONArray rewardnaps = new JSONArray(); /// quà nạp
                    for (int i = 0; i < 10; i++) {
                        rewardnaps.add(rewardTongnap[i]);
                    }

                    JSONArray rewardLogin = new JSONArray(); /// quà nạp
                    for (int i = 0; i < giftLogin.length; i++) {
                        rewardLogin.add(giftLogin[i]);
                    }

                    String jList = list.toJSONString();
                    String jRewards = rewards.toJSONString();
                    String jRewardNaps = rewardnaps.toJSONString(); /// quà nạp
                    String jRewardLogins = rewardLogin.toJSONString(); /// quà nạp
                    DbManager.executeUpdate("UPDATE `users` SET `luong` = ?," +
                            "  `gift` = ?," +
                            " `last_attendance_at` = ?," +
                            " `ip_address` = ?," +
                            " `level_reward` = ?, " +
                            "`nap_reward` = ?," +
                            "`dayLogin`=?," +
                            "`giftLogin`=? WHERE `id` = ? LIMIT 1;",
                            this.gold,
                            this.receivedFirstGift,
                            this.lastAttendance,
                            jList,
                            jRewards,
                            jRewardNaps,
                            this.dayLogin,
                            jRewardLogins,
                            this.id

                    );
                } finally {
                    saving = false;
                }
            }
        } catch (Exception e) {
            Log.logException("save data user: " + username, User.class, e);
        }
    }


    public void addGold(int gold) {
        long sum = (long) this.gold + (long) gold;
        int pre = this.gold;
        if (sum > 2000000000) {
            this.gold = 2000000000;
        } else {
            this.gold += gold;
        }
        if (this.gold < 0) {
            this.gold = 0;
        }
        gold = (this.gold - pre);// ttt
        service.addGold(gold);
    }

    public void cleanUp() {
        this.isCleaned = true;
        this.sltChar = null;
        this.chars = null;
        this.session = null;
        this.service = null;
        Log.debug("clean user " + this.username);
    }

    @Override
    public String toString() {
        JSONObject obj = new JSONObject();
        obj.put("luong", this.gold);
        obj.put("gift", this.receivedFirstGift);
        obj.put("last_attendance_at", this.lastAttendance);
        obj.put("id", this.id);
        return obj.toJSONString();
    }


    public void addLog(String name, String description) {
        try (Connection conn = DbManager.getConnection();) {

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO `user_logs`(`user_id`, `type`, `description`, `created_at`, `updated_at`) VALUES (?, ?, ?, ?, ?);");
            stmt.setInt(1, this.id);
            stmt.setInt(2, 1);
            stmt.setString(3, String.format("%s: %s", name, description));
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            Log.logException("add log err", User.class, e);
        }
    }

}
