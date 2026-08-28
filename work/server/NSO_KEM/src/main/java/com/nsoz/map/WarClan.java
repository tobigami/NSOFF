package com.nsoz.map;

import com.nsoz.clan.Clan;
import com.nsoz.constants.ItemName;
import com.nsoz.item.Item;
import com.nsoz.model.Char;
import com.nsoz.util.NinjaUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class WarClan {
    private static int idbase;
    public int gtcID;
    public long time;
    public ArrayList<Char> gt1;
    public ArrayList<Char> gt2;
    public boolean rest;
    public boolean start;
    public boolean viewkq;
    public boolean isDatCuoc;
    public long tienCuoc1;
    public long tienCuoc2;
    public Clan clan1;
    public Clan clan2;
    public Clan clanWin;
    public int pointClan1;
    public int pointClan2;
    public static HashMap<Integer, WarClan> gtcs = new HashMap();

    public WarClan() {
        this.gtcID = idbase++;
        this.time = System.currentTimeMillis() + 10000L;
        this.start = false;
        this.isDatCuoc = true;
        this.gt1 = new ArrayList<>();
        this.gt2 = new ArrayList<>();
        this.rest = false;
        this.viewkq = false;
        this.tienCuoc1 = 0;
        this.tienCuoc2 = 0;
        this.clan1 = null;
        this.clan2 = null;
        this.clanWin = null;
        this.pointClan1 = 0;
        this.pointClan2 = 0;
        this.initMap();

//        for(byte i = 0; i < this.map.length; ++i) {
//            this.map[i].timeMap = this.time;
//            this.map[i].update();
//        }
        gtcs.put(this.gtcID, this);
    }

    public void initMap() {
        for (Map map : MapManager.getInstance().getMaps()) {
            if (map.id == 117 || map.id == 120 || map.id == 121 || map.id == 122 || map.id == 123 || map.id == 124 || map.id == 98 || map.id == 104) {
                map.setClanWar(this);
                map.initZone();
            }
        }
    }

    public void reward(Char _char) {
        if (viewkq && !_char.isRewardedGtc) {
            int pointCT = _char.pointGTC;
//            if (pointCT < 200) {
//                return;
//            }
            ArrayList<Char> list = new ArrayList<>();
            WarClan gtc = null;
            if (_char.clan.gtcID != -1) {
                if (WarClan.gtcs.containsKey(_char.clan.gtcID)) {
                    gtc = WarClan.gtcs.get(_char.clan.gtcID);
                }
            }
            if (gtc != null) {
                for (Char mem : gtc.gt1) {

                    if (mem.pointGTC < 0) {
                        mem.pointGTC = 0;
                    }
                    list.add(mem);
                }
                for (Char mem2 : gtc.gt2) {
                    if (mem2.pointGTC < 0) {
                        mem2.pointGTC = 0;
                    }
                    list.add(mem2);
                }
            }
            list.sort((m1, m2) -> (Integer.compare(m2.pointGTC, m1.pointGTC)));
            _char.isRewardedGtc = true;
            // quà gtc
//            for (int i = 0; i < list.size(); i++) {
//                Char mem = list.get(i);
//                if (i == 0) {
//                    if (mem != null && mem.id == _char.id) {
//                        mem.addGold(10000);
//                        mem.addCoin(3000000);
//                        Item item = new Item(ItemName.RUONG_BACH_NGAN);
//                        item.setQuantity(1);
//                        mem.addItemToBag(item);
//                        mem.getService().serverMessage(String.format("Bạn nhận được %s lượng %s xu %s", "10k", "3tr", item.template.name));
//                    }
//                }
//                if (i == 1) {
//                    if (mem != null && mem.id == _char.id) {
//                        mem.addGold(7000);
//                        mem.addCoin(2000000);
//                        Item item = new Item(ItemName.RUONG_BACH_NGAN);
//                        item.setQuantity(1);
//                        mem.addItemToBag(item);
//                        mem.getService().serverMessage(String.format("Bạn nhận được %s lượng %s xu %s", "7k", "2tr", item.template.name));
//
//                    }
//                }
//                if (i == 2) {
//                    if (mem != null && mem.id == _char.id) {
//                        mem.addGold(4000);
//                        mem.addCoin(1000000);
//                        Item item = new Item(ItemName.BAT_BAO);
//                        item.setQuantity(1);
//                        mem.addItemToBag(item);
//                        mem.getService().serverMessage(String.format("Bạn nhận được %s lượng %s xu %s", "4k", "1tr", item.template.name));
//
//                    }
//                }
//
//            }
        }
    }

    public void viewTop(Char _char) {
        String info = "";

        int whitePoint = this.pointClan1;
        if (whitePoint < 0) {
            whitePoint = 0;
        }
        int blackPoint = this.pointClan2;
        if (blackPoint < 0) {
            blackPoint = 0;
        }
        boolean checkWin = whitePoint > blackPoint;
        info += "Bạch giả: " + whitePoint;
        info += "\n";
        info += "\nHắc giả: " + blackPoint;
        boolean reward = false;

        info += "\n--------------------------";


        info += "\nĐiểm của bạn: " + _char.pointGTC;
        info += "\nK/D: " + _char.nKillGtc + "/" + _char.nDeadGTC;

        ArrayList<Char> list = new ArrayList<>();
        WarClan gtc = null;
        if (_char.clan.gtcID != -1) {
            if (WarClan.gtcs.containsKey(_char.clan.gtcID)) {
                gtc = WarClan.gtcs.get(_char.clan.gtcID);
            }
        }
        if (gtc != null) {
            for (Char mem : gtc.gt1) {

                if (mem.pointGTC < 0) {
                    mem.pointGTC = 0;
                }
                list.add(mem);
            }
            for (Char mem2 : gtc.gt2) {
                if (mem2.pointGTC < 0) {
                    mem2.pointGTC = 0;
                }
                list.add(mem2);
            }
        }

        list.sort((m1, m2) -> (Integer.compare(m2.pointGTC, m1.pointGTC)));
        int size = list.size();
        if (size > 10) {
            size = 10;
        }
        for (int i = 0; i < size; i++) {
            Char mem = list.get(i);
            info += "\n" + (i + 1) + ". " + mem.name + ": " + mem.pointGTC + " (" + (mem.pheGtc == Char.PK_PHE1 ? "Bạch" : "Hắc") + ") - " + mem.getRank();

        }
        _char.getService().reviewCT(info, viewkq);
    }

    public void rest() {
        if (!this.rest) {
            this.rest = true;
            try {
                synchronized (this) {
                    if (this.isDatCuoc && !this.start) {
                        if (tienCuoc1 > 0) {
                            clan1.coin += tienCuoc1;
                            Clan.getClanDAO().update(clan1);
                        }
                        if (tienCuoc2 > 0) {
                            clan2.coin += tienCuoc2;
                            Clan.getClanDAO().update(clan2);
                        }
                    }
                    if (this.start) {
                        if (this.pointClan1 == this.pointClan2) {
                            this.clan1.coin += this.tienCuoc1 * 95 / 100;
                            this.clan2.coin += this.tienCuoc2 * 95 / 100;
                            Clan.getClanDAO().update(clan1);
                            Clan.getClanDAO().update(clan2);
                        } else {
                            if (this.pointClan1 > this.pointClan2) {
                                this.clanWin = this.clan1;
                            } else if (this.pointClan1 < this.pointClan2) {
                                this.clanWin = this.clan2;
                            }
                            long total = this.tienCuoc1 + this.tienCuoc2;
                            this.clanWin.coin += total - 15 / 100;
                            Clan.getClanDAO().update(clanWin);
                        }
                    }

                    this.clan1.gtcID = -1;
                    this.clan2.gtcID = -1;
                    this.clan1.gtcClanName = null;
                    this.clan2.gtcClanName = null;

                    Map ma;
                    Char _char;
                    while (!this.gt1.isEmpty()) {
                        _char = this.gt1.remove(0);
                        if (_char != null) {
                            if (this.start) {
                                if (this.clanWin == null) {
                                    _char.service.serverMessage("Gia tộc chiến đã kết thúc. Hai gia tộc có kết quả hoà.");
                                } else {
                                    _char.service.serverMessage("Gia tộc chiến đã kết thúc. Gia tộc " + this.clanWin.name + " đã giành chiến thắng.");
                                }

                            } else {
                                _char.service.serverMessage("Gia tộc chiến đã kết thúc.");
                            }
                            _char.pheGtc = -1;
                            _char.pointGTC = 0;
                            _char.addWarClanPoint((short) 0);
                            _char.gtcId = -1;
                            _char.setTypePk(Char.PK_NORMAL);
                            short[] xy = NinjaUtils.getXY(_char.mapBeforeEnterPB);
                            _char.setXY(xy);
                            _char.changeMap(_char.mapBeforeEnterPB);
                        }
                    }
                    while (!this.gt2.isEmpty()) {
                        _char = this.gt2.remove(0);
                        if (_char != null) {
                            if (this.start) {
                                if (this.clanWin == null) {
                                    _char.service.serverMessage("Gia tộc chiến đã kết thúc. Hai gia tộc có kết quả hoà.");
                                } else {
                                    _char.service.serverMessage("Gia tộc chiến đã kết thúc. Gia tộc " + this.clanWin.name + " đã giành chiến thắng.");
                                }
                            } else {
                                _char.service.serverMessage("Gia tộc chiến đã kết thúc.");
                            }
                            _char.pheGtc = -1;
                            _char.pointGTC = 0;
                            _char.addWarClanPoint((short) 0);
                            _char.gtcId = -1;
                            _char.setTypePk(Char.PK_NORMAL);
                            short[] xy = NinjaUtils.getXY(_char.mapBeforeEnterPB);
                            _char.setXY(xy);
                            _char.changeMap(_char.mapBeforeEnterPB);
                        }
                    }
                }
                synchronized (WarClan.gtcs) {
                    if (WarClan.gtcs.containsKey(this.gtcID)) {
                        WarClan.gtcs.remove(this.gtcID);
                    }
                }
            } catch (Exception e) {
                byte i;
                synchronized (WarClan.gtcs) {
                    if (WarClan.gtcs.containsKey(this.gtcID)) {
                        WarClan.gtcs.remove(this.gtcID);
                    }
                }
            }
        }
    }

    public void viewKetqua() {
        if (!viewkq) {
            this.viewkq = true;
            synchronized (this) {
                this.time = System.currentTimeMillis() + 120000L;
                int i;
                Char _char;
                synchronized (this.gt1) {
                    for (i = 0; i < this.gt1.size(); ++i) {
                        _char = this.gt1.get(i);
                        if (_char != null) {
                            if (_char.zone.tilemap.isGTC() && _char.zone.map.id != 98) {
                                short[] xy = NinjaUtils.getXY(98);
                                _char.setXY(xy);
                                _char.changeMap(98);
                            }
                            _char.service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                            _char.service.serverMessage("Gia tộc chiến đã kết thúc bạn có 2 phút để xem kết quả và nhận thưởng");

                        }
                    }
                }
                synchronized (this.gt2) {
                    for (i = 0; i < this.gt2.size(); ++i) {
                        _char = this.gt2.get(i);
                        if (_char != null) {
                            if (_char.zone.tilemap.isGTC() && _char.zone.map.id != 104) {
                                short[] xy = NinjaUtils.getXY(104);
                                _char.setXY(xy);
                                _char.changeMap(104);
                            }
                            _char.service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                            _char.service.serverMessage("Gia tộc chiến đã kết thúc bạn có 2 phút để xem kết quả và nhận thưởng");
                        }
                    }
                }
            }
        }
    }

    public void start() {  // time đánh gia tộc chiến
        if (!this.start) {
            this.start = true;
            synchronized (this) {
                //this.time = System.currentTimeMillis() + 3600000L;
                this.time = System.currentTimeMillis() + 1800000L;
                int i;
                Char _char;
                synchronized (this.gt1) {
                    for (i = 0; i < this.gt1.size(); ++i) {
                        _char = this.gt1.get(i);
                        if (_char != null) {
                            _char.service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                            _char.service.serverMessage("Gia tộc chiến đã bắt đầu.");

                        }
                    }
                }
                synchronized (this.gt2) {
                    for (i = 0; i < this.gt2.size(); ++i) {
                        _char = this.gt2.get(i);
                        if (_char != null) {
                            _char.service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                            _char.service.serverMessage("Gia tộc chiến đã bắt đầu.");
                        }
                    }
                }
            }
        }
    }

    public void invite() { // time chờ 2 bên mời gia tộc
        if (this.isDatCuoc) {
            synchronized (this) {
                this.isDatCuoc = false;
                this.time = System.currentTimeMillis() + 300000L;
//                this.time = System.currentTimeMillis() + 10000L;
                int i;
                Char _char;
                synchronized (this.gt1) {
                    for (i = this.gt1.size() - 1; i >= 0; i--) {
                        _char = this.gt1.get(i);
                        if (_char != null) {
                            _char.gtcId = this.gtcID;
                            _char.pheGtc = Char.PK_PHE1;
                            _char.setTypePk(Char.PK_PHE1);
                            short[] xy = NinjaUtils.getXY(98);
                            _char.setXY(xy);
                            _char.changeMap(98);
                            _char.pointGTC = 0;
                            _char.isRewardedGtc = false;
                            _char.clan.openGtc -= 1;
                            _char.service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                            _char.service.serverMessage("Bạn có 5 phút để triệu tập thành viên tham gia Gia tộc chiến.");
                        }
                    }
                }
                synchronized (this.gt2) {
                    for (i = this.gt2.size() - 1; i >= 0; i--) {
                        _char = this.gt2.get(i);
                        if (_char != null) {
                            _char.pheGtc = Char.PK_PHE2;
                            _char.gtcId = this.gtcID;
                            _char.setTypePk(Char.PK_PHE2);
                            short[] xy = NinjaUtils.getXY(104);
                            _char.setXY(xy);
                            _char.changeMap(104);
                            _char.pointGTC = 0;
                            _char.isRewardedGtc = false;
                            _char.clan.openGtc -= 1;
                            _char.service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                            _char.service.serverMessage("Bạn có 5 phút để triệu tập thành viên tham gia Gia tộc chiến.");
                        }
                    }
                }
            }
        }
    }

    public void join() { // time phòng chờ cược
        synchronized (this) {
            this.time = System.currentTimeMillis() + 120000L;
//            this.time = System.currentTimeMillis() + 10000L;
            int i;
            synchronized (this.gt1) {
                for (i = this.gt1.size() - 1; i >= 0; i--) {
                    if (this.gt1.get(i) != null) {
                        (this.gt1.get(i)).service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                    }
                }
            }
            synchronized (this.gt2) {
                for (i = this.gt2.size() - 1; i >= 0; i--) {
                    if (this.gt2.get(i) != null) {
                        (this.gt2.get(i)).service.sendTimeInMap((int) (this.time - System.currentTimeMillis()) / 1000);
                    }
                }
            }
        }
    }

    public void leave() {
        synchronized (this) {
            int i;
            if (!this.gt1.isEmpty()) {
                for (i = this.gt1.size() - 1; i >= 0; i--) {
                    if (this.gt1.get(i) != null) {
                        this.gt1.get(i).service.serverMessage("Gia tộc đối phương đã huỷ bỏ cuộc so găng.");
                    }
                }
            }
            if (!this.gt2.isEmpty()) {
                for (i = this.gt2.size() - 1; i >= 0; i--) {
                    if (this.gt2.get(i) != null) {
                        this.gt2.get(i).service.serverMessage("Gia tộc đối phương đã huỷ bỏ cuộc so găng.");
                    }
                }
            }
            if (tienCuoc1 > 0) {
                clan1.coin += tienCuoc1;
                Clan.getClanDAO().update(clan1);
            }
            if (tienCuoc2 > 0) {
                clan2.coin += tienCuoc2;
                Clan.getClanDAO().update(clan2);
            }
            this.isDatCuoc = false;
            this.rest();
        }
    }
}
