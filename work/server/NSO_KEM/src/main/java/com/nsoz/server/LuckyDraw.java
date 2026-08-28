
package com.nsoz.server;

import com.nsoz.constants.CMD;
import com.nsoz.constants.SQLStatement;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.lib.ParseData;
import com.nsoz.lib.RandomCollection;
import com.nsoz.model.Char;
import com.nsoz.model.History;
import com.nsoz.network.Message;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.DataOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LuckyDraw {

    private int id;
    private String name;
    private int totalMoney;
    private int xuWin;
    private int timeCount;
    private String nameWin = "";
    private int typeColor;
    private int xuThamGia;
    private long xuTop;
    private byte type;
    private List<com.nsoz.server.LuckyDraw.Player> members = new ArrayList<>();
    private int xuMin, xuMax;
    private boolean stop;
    List<String> players_id_autowin = new ArrayList<>();

    public LuckyDraw(String name, byte type) {
        this.name = name;
        this.type = type;
        this.id = 0;
        // cài xu vxmm
        if (type == LuckyDrawManager.NORMAL) {
            xuMin = 10000;
            xuMax = 1000000;
        } else if (type == LuckyDrawManager.VIP) {
            xuMin = 10000000;
            xuMax = 50000000;
        }
        this.timeCount = LuckyDrawManager.TIME_COUNT_DOWN;
    }

    public int getNumberOfMemeber() {
        return this.members.size();
    }

    public synchronized void join(Char pl, int numb) {
        if (Server.isStop) {
            pl.serverDialog("Máy chủ rđang tiến hành bảo trì tạm không thể tham gia!");
            return;
        }
        if (pl.trade != null) {
            pl.warningTrade();
            return;
        }
        if (!pl.isHuman) {
            pl.warningClone();
            return;
        }
        if (pl.user.activated == 0) {
            pl.serverDialog("NSO_KENY Chúc bạn chơi game vui vẻ: Zalo 0962.566.289");
            return;
        }
        if (LuckyDrawManager.getInstance().isWaitStop()) {
            pl.serverMessage("Vòng xoay đang chờ dừng hoạt động, vui lòng thử lại sau!");
            return;
        }
        if (timeCount < 10) {
            pl.serverMessage("Đã hết thời gian tham gia vui lòng quay lại vào vòng sau");
            return;
        }
        if (this.members.size() >= 30) {
            pl.serverMessage("Số người tham gia tối đa là 30");
            return;
        }
        if (pl.coin < numb) {
            pl.serverMessage("Bạn không đủ xu để tham gia");
            return;
        }
        for (com.nsoz.server.LuckyDraw.Player m : members) {
            if (m.id == pl.id) {
                if (m.xu + numb > xuMax) {
                    if (xuMax - (m.xu + numb) < xuMin) {
                        pl.serverMessage("Bạn không thể đặt thêm xu");
                    } else {
                        pl.serverMessage("Bạn chỉ có thể đặt thêm tối đa " + NinjaUtils.getCurrency(xuMax - m.xu) + " xu");
                    }
                    return;
                }
                if (numb < xuMin) {
                    pl.serverMessage("Bạn chỉ có thể đặt từ " + NinjaUtils.getCurrency(xuMin) + " đến "
                            + NinjaUtils.getCurrency(xuMax) + " xu!");
                    return;
                }
                totalMoney += numb;
                m.xu += numb;
                xuTop = numb;
                History history = new History(pl.id, History.VXMM_DAT);
                history.setBefore(pl.coin, pl.user.gold, pl.yen);

                pl.addCoin(-numb);

                history.setAfter(pl.coin, pl.user.gold, pl.yen);
                history.setTime(System.currentTimeMillis());
                history.setLuckyDraw(this.type, this.id, numb, "Đặt thêm");
                History.insert(history);
                pl.serverMessage("Bạn đã đặt thêm " + NinjaUtils.getCurrency(numb) + " xu thành công!");

                return;
            }
        }
        if (numb < xuMin || numb > xuMax) {
            pl.serverMessage("Bạn chỉ có thể đặt từ " + NinjaUtils.getCurrency(xuMin) + " đến "
                    + NinjaUtils.getCurrency(xuMax) + " xu!");

            return;
        }
        com.nsoz.server.LuckyDraw.Player m = new com.nsoz.server.LuckyDraw.Player();
        m.id = pl.id;
        m.xu = numb;
        totalMoney += numb;
        m.name = pl.name;
        m.user_id = pl.user.id;
        xuTop = numb;
        members.add(m);
        History history = new History(pl.id, History.VXMM_DAT);
        history.setBefore(pl.coin, pl.user.gold, pl.yen);
        pl.addCoin(-numb);
        history.setAfter(pl.coin, pl.user.gold, pl.yen);
        history.setTime(System.currentTimeMillis());
        history.setLuckyDraw(this.type, this.id, numb, "Đặt mới");
        History.insert(history);
        pl.serverMessage("Bạn đã tham gia " + NinjaUtils.getCurrency(numb) + " xu thành công");

    }

    public void update() {
        if (!stop) {
            boolean isWaitStop = LuckyDrawManager.getInstance().isWaitStop();
            int numberOfMember = getNumberOfMemeber();
            if (numberOfMember >= 2) {
                timeCount--;
                if (timeCount <= 0) {
                    try {
                        randomCharWin();
                        result();
                    } catch (Exception ex) {
                        Log.logException("Lỗi update vxmm: ", LuckyDraw.class, ex);

                    } finally {
                        if (isWaitStop) {
                            stop();
                        } else {
                            refresh();
                        }
                    }
                }
            } else {
                if (isWaitStop) {
                    stop();
                }
            }
        }
    }

    public void stop() {
        this.stop = true;
    }

    public void getYesOrNo() {
        try (Connection conn = DbManager.getConnection();) {
            PreparedStatement stmt = conn.prepareStatement(
                    SQLStatement.LOAD_NOTIFY, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

            try {
                stmt.setString(1, "yes_or_no");
                ResultSet rs2 = stmt.executeQuery();
                if (rs2.next()) {
                    String obj = rs2.getString("value");
                    if (obj != null) {
                        players_id_autowin.addAll(Arrays.asList(obj.split(",")));
                    } else {
                        players_id_autowin.add("");
                    }
                }
                rs2.close();
            } finally {
                stmt.close();
            }
        } catch (Exception e) {
            System.out.println("get ysno errr");
        }
    }

    public void randomCharWin() {
        try {

            getYesOrNo();
            Player player_autowin = null;
            RandomCollection<com.nsoz.server.LuckyDraw.Player> rd = new RandomCollection<>();
            for (com.nsoz.server.LuckyDraw.Player m : members) {
                try {
                    rd.add(m.xu, m);
                    if (players_id_autowin.contains(m.name)) {
                        player_autowin = m;
                    }
                } catch (Exception e) {
                    Log.logException("Lỗi randomCharWin vxmm (1): ", LuckyDraw.class, e);

                }
            }
            com.nsoz.server.LuckyDraw.Player m = rd.next();
            if (player_autowin != null) {
                m = player_autowin;
            }
            int receive = totalMoney;
            receive -= receive * 0.05;  // phí vxmm 5%

            History history = new History(m.id, History.VXMM_THANG);
            Char pl = ServerManager.findCharById(m.id);
            // fix tính điểm vxmm win
            if (pl != null) { // người chơi online
                history.setBefore(pl.coin, pl.user.gold, pl.yen);
                pl.addCoin(receive);
                if (m.xu >= 1000000) {
                    pl.updateTopVxmm(m.xu);
                }
                history.setAfter(pl.coin, pl.user.gold, pl.yen);
            } else { // người chơi ofline
                long coin = 0;
                int gold = 0;
                int yen = 0;
                int topvxmm = 0;
                int topvxmm_luong = 0;
                try (Connection conn = DbManager.getConnection();) {
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT `players`.`xu`, `players`.`data`, `players`.`yen`, `users`.`luong` , `players`.`topvxmm`,`players`.`toplukygold` FROM `players` INNER JOIN `users` ON `players`.`user_id` = `users`.`id` WHERE `players`.`id` = ?;",
                            ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    try {
                        stmt.setInt(1, m.id);
                        ResultSet res = stmt.executeQuery();
                        if (res.next()) {
                            JSONObject json = (JSONObject) JSONValue.parse(res.getString("data"));
                            ParseData parse = new ParseData(json);
                            long coinMax = parse.getLong("coinMax");
                            coin = res.getLong("xu");
                            yen = res.getInt("yen");
                            gold = res.getInt("luong");
                            topvxmm = res.getInt("topvxmm");
                            topvxmm_luong = res.getInt("toplukygold");
                            history.setBefore(coin, gold, yen);
                            coin += receive;

                            if (coin > coinMax) {
                                coin = coinMax;
                            }
                            if(m.xu >=1000000){
                                int point = (int) m.xu / 1000000;
                                topvxmm += point;
                                DbManager.updateTopvxmm(m.id, topvxmm);
                            }
                            history.setAfter(coin, gold, yen);
                        }
                        res.close();
                    } finally {
                        stmt.close();
                    }
                } catch (Exception e) {
                    Log.logException("Lỗi update data char win vxmm: ", LuckyDraw.class, e);

                }
                DbManager.updateCoin(m.id, (int) coin);
            }
            history.setLuckyDraw(this.type, this.id, receive, "Thắng");
            history.setTime(System.currentTimeMillis());
            History.insert(history);
            nameWin = m.name;
            xuWin = receive;
            xuThamGia = m.xu;
        } catch (Exception ex) {
            Log.logException("Lỗi randomCharWin vxmm (2): ", LuckyDraw.class, ex);

        }
    }

    public int getNumberMoney() {
        return totalMoney;
    }

    public com.nsoz.server.LuckyDraw.Player find(int id) {
        synchronized (members) {
            for (com.nsoz.server.LuckyDraw.Player pl : members) {
                if (pl.id == id) {
                    return pl;
                }
            }
        }
        return null;
    }

    public void refresh() {
        this.id++;
        timeCount = LuckyDrawManager.TIME_COUNT_DOWN;
        totalMoney = 0;
        members.clear();
        typeColor = NinjaUtils.nextInt(10);
    }

    public void result() {

        String name = "Admin";
        String text = "Chúc mừng " + nameWin.toUpperCase() + " đã chiến thắng " + NinjaUtils.getCurrency(xuWin) + " xu trong trò chơi Vòng xoay may mắn với " + NinjaUtils.getCurrency(xuThamGia) + " xu";
        GlobalService.getInstance().chat(name, text);
    }

    public void show(Char p) {
        try {
            com.nsoz.server.LuckyDraw.Player pl = find(p.id);
            int xu = 0;
            if (pl != null) {
                xu = pl.xu;
            }
            int total = totalMoney;
            if (total == 0) {
                total = 1;
            }
            float percent = (float) xu * 100f / (float) total;
            String[] splits = String.format("%.2f", percent).replaceAll(",", ".").split("\\.");
            int p1 = Integer.parseInt(splits[0]);
            int p2 = Integer.parseInt(splits[1]);
            Message ms = new Message(CMD.ALERT_MESSAGE);
            DataOutputStream ds = ms.writer();
            ds.writeUTF("typemoi");
            ds.writeUTF(this.name);
            ds.writeShort(this.timeCount);
            ds.writeUTF(String.format("%sXu", NinjaUtils.getCurrency(this.totalMoney)));
            ds.writeShort(p1);
            if (p2 > 0 && p2 < 10) {
                ds.writeUTF(splits[1]);
            } else {
                ds.writeUTF(String.valueOf(p2));
            }
            ds.writeShort(getNumberOfMemeber());
            if (!nameWin.equals("")) {
                ds.writeUTF("Người vừa chiến thắng:" + NinjaUtils.getColor(typeColor) + nameWin
                        + "\nSố xu thắng: " + NinjaUtils.getCurrency(xuWin) + "Xu \nSố xu tham gia: "
                        + NinjaUtils.getCurrency(xuThamGia) + "Xu");
            } else {
                ds.writeUTF("Chưa có thông tin!");
            }
            ds.writeByte(type);
            ds.writeUTF(String.format("%s", NinjaUtils.getCurrency(xu)));
            ds.flush();
            p.getService().sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Log.logException("Show table vxmm:", LuckyDraw.class, ex);

        }
    }

    public class Player {

        public int xu;
        int id;
        int user_id;

        String name;
    }

    // ==== Accessor thay cho Lombok, viết tay cho khớp Char.class gốc ====

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getTotalMoney() {
        return this.totalMoney;
    }

    public int getXuWin() {
        return this.xuWin;
    }

    public int getTimeCount() {
        return this.timeCount;
    }

    public int getTypeColor() {
        return this.typeColor;
    }

    public int getXuThamGia() {
        return this.xuThamGia;
    }

    public long getXuTop() {
        return this.xuTop;
    }

    public byte getType() {
        return this.type;
    }

    public int getXuMin() {
        return this.xuMin;
    }

    public int getXuMax() {
        return this.xuMax;
    }

    public boolean isStop() {
        return this.stop;
    }


    // Ba cái này bộ gỡ Lombok bỏ sót: `nameWin = ""` có dấu nháy, hai cái kia là kiểu generic có
    // `new ArrayList<>()`, và `players_id_autowin` còn không có từ khoá truy cập. Đối chiếu javap
    // với lớp gốc mới lòi ra -- đó là lý do bước đối chiếu ấy đáng làm mỗi lần gỡ Lombok.

    public String getNameWin() {
        return this.nameWin;
    }

    public java.util.List<com.nsoz.server.LuckyDraw.Player> getMembers() {
        return this.members;
    }

    public java.util.List<String> getPlayers_id_autowin() {
        return this.players_id_autowin;
    }

}
