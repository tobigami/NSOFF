package com.nsoz.map.zones;

import com.nsoz.map.Map;
import com.nsoz.map.MapService;
import com.nsoz.map.TileMap;
import com.nsoz.map.Waypoint;
import com.nsoz.mob.Mob;
import com.nsoz.model.Char;
import com.nsoz.npc.Npc;
import com.nsoz.npc.NpcFactory;
import com.nsoz.util.NinjaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Soanlv
 */
public class ClanBattleField extends Zone {
    public ClanBattleField(int id, TileMap tilemap, Map map) {
        super(id, tilemap, map);
    }




    @Override
    public void requestChangeMap(@NotNull Char p) {
        Waypoint wp = tilemap.findWaypoint(p.x, p.y);
        if (wp == null) {
            return;
        }
        int nextID = wp.next;
        if (map.clanWar != null && this.map.tilemap.isGTC()) {
            if ((nextID == 104 && p.clan.name == this.map.clanWar.clan1.name) || (nextID == 98 && p.clan.name == this.map.clanWar.clan2.name)) {
                p.returnToPreviousPostion(() -> {
                    p.serverDialog("Bạn không thể di chuyển tới Căn cứ địa của đối phương.");
                });
                return;
            }
            if ((nextID == 120 || nextID == 124) && this.map.clanWar.viewkq) {
                p.returnToPreviousPostion(() -> {
                    p.serverDialog("Gia tộc chiến đã kết thúc.");
                });
                return;
            }
            if ((nextID == 120 || nextID == 124) && !this.map.clanWar.start) {
                p.returnToPreviousPostion(() -> {
                    p.serverDialog("Trận đấu chưa được bắt đầu.");
                });
                return;
            }
        }
        p.setXY(wp.x, wp.y);
        p.changeMap(nextID);
    }


    @Override
    public void returnTownFromDead(@NotNull Char p) {
        int mapID = -1;
        if (this.map.clanWar != null && this.map.id != 117) {
            if (this.map.clanWar.gt1.contains(p)) {
                mapID = 98;
            } else {
                mapID = 104;
            }
            short[] xy = NinjaUtils.getXY(mapID);
            p.setXY(xy);
            p.changeMap(mapID);
        } else {
            short[] xy = NinjaUtils.getXY(p.saveCoordinate);
            p.setXY(xy[0], xy[1]);
            p.changeMap(p.saveCoordinate);
        }

    }

    @Override
    public void join(Char p) {
        super.join(p);
        if (this.map.tilemap.isGTC()) {
            if(p.isHuman){

                if (this.map.clanWar != null) {
                    if (this.map.clanWar.clan1.name.equals(p.clan.name)) {
                        if(!this.map.clanWar.gt1.contains(p)){
                            this.map.clanWar.gt1.add(p);
                        }
                    } else {
                        if(!this.map.clanWar.gt2.contains(p)){
                            this.map.clanWar.gt2.add(p);
                        }
                    }
                    if (this.map.clanWar.time != -1L) {
                        p.service.sendTimeInMap((int) (this.map.clanWar.time - System.currentTimeMillis()) / 1000);
                    }
                }
                p.getService().sendPointGTC();
            }
        }
    }

    @Override
    public void mobDead(Mob mob, Char killer) {
        if (killer != null) {
            if (mob.zone.map.clanWar != null && mob.zone.map.tilemap.isGTC()) {

//                killer.addWarClanPoint((short) 1);
            }
        }
    }
}
