package com.nsoz.server;
// chanle
import com.nsoz.constants.CMD;
import com.nsoz.constants.CMDMenu;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.nsoz.db.jdbc.DbManager;
import com.nsoz.item.Item;
import com.nsoz.model.Char;
import com.nsoz.model.History;
import com.nsoz.util.NinjaUtils;
import com.nsoz.lib.ParseData;
import com.nsoz.lib.RandomCollection;
import com.nsoz.model.Menu;
import com.nsoz.network.Message;
import java.io.DataOutputStream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public class ChanLe {

    public class Player {

        int id;
        public int xu;
        public int typeDat;
        String name;
    }

    private int id;
    private String name;
    private int totalMoney;
    private int timeCount;
    private String nameWin = "";
    private int typeColor;
    private int xuThamGia;
    private byte type;
    private List<Player> members = new ArrayList<>();
    private int xuMin, xuMax;
    private boolean stop;
    public int kqTrao, kq, kq1, kq2;
    public String KetQua;

    public ChanLe(String name, byte type) {
        this.name = name;
        this.type = type;
        this.id = 0;
        if (type == ChanLeManager.NORMAL) {
            xuMin = 1000000;
            xuMax = 50000000;
        }
        this.timeCount = ChanLeManager.TIME_COUNT_DOWN;
    }

    public int getNumberOfMemeber() {
        return this.members.size();
    }

    public synchronized void join(Char pl, int numb, int type) {
        pl.typeVXMM = 2;
        if (pl.trade != null) {
            pl.warningTrade();
            return;
        }
        if (!pl.isHuman) {
            pl.warningClone();
            return;
        }
        if (pl.user.activated != 1){
            pl.serverDialog("Tài khoản của bạn chưa thể thực hiện chức năng này!Vui Lòng truy cập nsosum.com để kích hoạt tài khoản");
            return;
        }
        if (timeCount < 10) {
            pl.serverMessage("Đã hết thời gian tham gia vui lòng quay lại vào vòng sau");
            return;
        }
        if (this.members.size() >= 100) {
            pl.serverMessage("Số người tham gia tối đa là 100");
            return;
        }
        if (pl.coin < numb) {
            pl.serverMessage("Bạn không đủ xu để tham gia");
            return;
        }
        for (Player m : members) {
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
                pl.addCoin(-numb);                       
                pl.serverMessage("Bạn đã đặt thêm " + NinjaUtils.getCurrency(numb) + " xu thành công!");
                return;
            }
        }
        if (numb < xuMin || numb > xuMax) {
            pl.serverMessage("Bạn chỉ có thể đặt từ " + NinjaUtils.getCurrency(xuMin) + " đến "
                    + NinjaUtils.getCurrency(xuMax) + " xu!");
            return;
        }
        Player m = new Player();
        m.id = pl.id;
        m.xu = numb;
        totalMoney += numb;
        m.name = pl.name;
        m.typeDat = type;
        members.add(m);
        pl.addCoin(-numb);
        if (type == 0) {
            pl.serverMessage("Bạn đã tham gia " + NinjaUtils.getCurrency(numb) + " xu thành công vào CHAN");
        } else {
            pl.serverMessage("Bạn đã tham gia " + NinjaUtils.getCurrency(numb) + " xu thành công vào LE");
        }
    }
    
    public boolean isCheck(int id) {
        for (Player m : members) {
            if (m.id == id) {
                return true;
            }
        }
        return false;
    }
    
    public static int sumOfDigits(int number) {
        int sum = 0;
        while (number != 0) {
            sum += number % 10;
            number /= 10; 
        }
        return sum;
    }

    public void update() {
        if (!stop) {
            boolean isWaitStop = ChanLeManager.getInstance().isWaitStop();
            int numberOfMember = getNumberOfMemeber();
            if (numberOfMember >= 2) {
                timeCount--;
                if (timeCount == 5) {
                    kq1 = NinjaUtils.nextInt(1000000, 2000000);
                    kq2 = NinjaUtils.nextInt(10000, 100000);
                }
                if (timeCount <= 0) {
                    try {
                        randomCharWin();
                    } catch (Exception ex) {
                        ex.printStackTrace();
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

    public void randomCharWin() {
        try {
            int sum1 = sumOfDigits(kq1);
            int sum2 = sumOfDigits(kq2);
            kq = sum1 + sum2; 
            kqTrao = kq % 10;
            if (kqTrao == 0 || kqTrao == 2 || kqTrao == 4 || kqTrao == 6 || kqTrao == 8) {
                KetQua = "CHAN";
            }
            if (kqTrao == 1 || kqTrao == 3 || kqTrao == 5 || kqTrao == 7 || kqTrao == 9) {
                KetQua = "LE";
            }
            for (Player m : members) {
                try {
                    if (kqTrao == 0 || kqTrao == 2 || kqTrao == 4 || kqTrao == 6 || kqTrao == 8) {
                        if (m.typeDat == 0) {//chan
                            Char pl = ServerManager.findCharById(m.id);
                            if (pl != null) {
                                int xuAn = m.xu + (m.xu / 100 * 90);
                                pl.addCoin(xuAn);
                                pl.serverMessage("Chúc mừng bạn nhận được " + NinjaUtils.getCurrency(xuAn) + " từ CLTX");
                                pl.xuWin = xuAn;
                            }
                        }
                    }
                    if (kqTrao == 1 || kqTrao == 3 || kqTrao == 5 || kqTrao == 7 || kqTrao == 9) {
                        if (m.typeDat == 1) {//le
                            Char pl = ServerManager.findCharById(m.id);
                            if (pl != null) {
                                int xuAn = m.xu + (m.xu / 100 * 90);
                                pl.addCoin(xuAn);
                                pl.serverMessage("Chúc mừng bạn nhận được " + NinjaUtils.getCurrency(xuAn) + " từ CLTX");
                                pl.xuWin = xuAn;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }          
        } catch (Exception ex) {
            Logger.getLogger(ChanLe.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getNumberMoney() {
        return totalMoney;
    }

    public Player find(int id) {
        synchronized (members) {
            for (Player pl : members) {
                if (pl.id == id) {
                    return pl;
                }
            }
        }
        return null;
    }

    public void refresh() {
        this.id++;
        timeCount = ChanLeManager.TIME_COUNT_DOWN;
        totalMoney = 0;
        members.clear();
        typeColor = NinjaUtils.nextInt(10);
    }

    public void show(Char p) {
        try {
            Player pl = find(p.id);
            int xu = 0;
            int xucheck = 0;
            int xuWin = p.xuWin;
            String typeDat = "";
            if (pl != null) {
                xu = pl.xu;
                xucheck = 1;
                if (pl.typeDat == 0) {
                    typeDat = "CHAN";
                } else if (pl.typeDat == 1) {
                    typeDat = "LE";
                }
            }
            float percent = (float) xucheck * 100f / (float) 2;
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
            ds.writeUTF("Xu thắng: " + NinjaUtils.getCurrency(xuWin) + " xu"
                    + "\nKết quả: (" + KetQua + ")"
                    + "\n" + kq1 + " + " + kq2 + " = " + kq
                    + "\nCửa hiện tại đặt: " + typeDat);
            ds.writeByte(type);
            ds.writeUTF(String.format("%s", NinjaUtils.getCurrency(xu)));
            ds.flush();
            p.getService().sendMessage(ms);
            ms.cleanup();
        } catch (Exception ex) {
            Logger.getLogger(ChanLe.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }
}
