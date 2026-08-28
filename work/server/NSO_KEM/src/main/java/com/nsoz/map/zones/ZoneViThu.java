package com.nsoz.map.zones;

import com.nsoz.map.Map;
import com.nsoz.map.TileMap;
import com.nsoz.map.Vithu;
import com.nsoz.map.Waypoint;
import com.nsoz.model.Char;
import com.nsoz.util.NinjaUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Soanlv
 */
public class ZoneViThu extends Zone {
    public ZoneViThu(int id, TileMap tilemap, Map map) {
        super(id, tilemap, map);
    }

    @Override
    public void requestChangeMap(@NotNull Char p) {
        Waypoint wp = tilemap.findWaypoint(p.x, p.y);
        if (wp == null) {
            return;
        }
        int nextID = wp.next;
        if (map.vithu != null && this.map.tilemap.isMapVithu()) {
            if (nextID == 169 && !map.vithu.isSanBoss) {
                p.returnToPreviousPostion(() -> {
//                    p.serverDialog(String.format("Map sẽ mở sau %s",NinjaUtils.timeAgo((int) (map.vithu.TIME_START-System.currentTimeMillis()/1000L ))));
                    p.serverDialog("Map sẽ mở vào 21h");
                });
                return;
            }
            if (nextID == 169 && p.delayvithu > System.currentTimeMillis()) {
                p.returnToPreviousPostion(() -> {
                    p.serverDialog("Chờ sau " + (p.delayvithu - System.currentTimeMillis()) / 1000L + "s.");
                });
                return;
            }

        }
        p.setXY(wp.x, wp.y);
        p.changeMap(nextID);
    }

    @Override
    public void join(Char p) {
        super.join(p);
        if (map.vithu != null && this.map.tilemap.isMapVithu()) {
            if (!map.vithu.users.contains(p)) {
                map.vithu.addUser(p);
            }
            if (map.vithu.isDieVithu) {
                p.setTypePk(Char.PK_PHE);
            }
        }

    }

    @Override
    public void returnTownFromDead(@NotNull Char p) {
        if ((this.map.id == 169 || this.map.id == 171) && System.currentTimeMillis() < map.vithu.timeVithu) {
            short[] xy = NinjaUtils.getXY(171);
            p.setXY(xy);
            p.changeMap(171);
            p.delayvithu = System.currentTimeMillis() + 20000L;
        }
    }

    @Override
    public void createMonster() {
        super.createMonster();
    }
}
