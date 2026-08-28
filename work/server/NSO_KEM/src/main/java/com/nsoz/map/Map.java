package com.nsoz.map;
// fix quái ko ra
import com.nsoz.model.IUpdate;
import com.nsoz.server.Server;
import com.nsoz.constants.MapName;
import com.nsoz.map.zones.*;
import com.nsoz.model.Char;
import com.nsoz.util.Log;
import com.nsoz.util.NinjaUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Map {

    public static boolean running = true;
    public short id;
    public TileMap tilemap;
    public Thread threadUpdateChar, threadUpdateOther;
    @Setter
    public War war;
    @Setter
    public Vithu vithu;
    @Setter
    public WarClan clanWar;
    @Getter
    private List<Zone> zones = new ArrayList<>();
    private ReadWriteLock lock;

    public Map(short id) {
        this.id = id;
        this.tilemap = MapManager.getInstance().getTileMap(id);
        lock = new ReentrantReadWriteLock();
        if (tilemap.tileId > 0) {
            if (!(id >= 98 && id <= 104)) {
                initZone();
            }
//            if (tilemap.tileId != 0) {
//                update();
//            }
        }
    }

    public void initZone() {
        zones.clear();
        for (int i = 0; i < this.tilemap.zoneNumber; i++) {
            Zone z = null;
            if (id == MapName.HANG_AKA) {
                z = new AkaCave((byte) i, tilemap, this);
            } else if (id == MapName.HANG_KUGYOU) {
                z = new KugyouCave((byte) i, tilemap, this);
            } else if (id == MapName.KHU_DA_DO_AIKO) {
                z = new AikoRedRockArea((byte) i, tilemap, this);
            } else if (tilemap.isChienTruong()) {
                z = new Battlefield((byte) i, this.tilemap, this);
            } else if (tilemap.isGTC()) {
                z = new ClanBattleField((byte) i, this.tilemap, this);
            } else if (tilemap.isMapVithu()) {
                z = new ZoneViThu((byte) i, this.tilemap, this);
            } else if (id == MapName.DAU_TRUONG) {
                z = new TalentShow((byte) i, this);
                MapManager.getInstance().talentShow = (TalentShow) z;
            } else {
                z = new Zone((byte) i, this.tilemap, this);
            }
        }
    }

    public void addZone(Zone z) {
        lock.writeLock().lock();
        try {
            zones.add(z);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeZone(Zone z) {
        lock.writeLock().lock();
        try {
            zones.remove(z);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Zone getZoneById(int index) {
        if (index < 0 || index >= zones.size()) {
            return null;
        }
        return this.zones.get(index);
    }

    public void joinZone(Char _char, int zoneId) {
        try {
            Zone z = getZoneById(zoneId);
            if (z != null) {
                z.join(_char);
            }
        } catch (Exception e) {
            Log.logException(String.format("Char: %s, Map: %s, Equiped is null: %b, Cleaned: %b", _char.name, this.id, _char.equipment == null, _char.isCleaned), Map.class, e);
        }
    }


    public void close() {
        lock.readLock().lock();
        try {
            if (this.zones != null) {
                for (Zone z : this.zones) {
                    z.running = false;
                }
            }

        } finally {
            lock.readLock().unlock();
        }

    }

    public Zone rand() {
        return zones.get(NinjaUtils.nextInt(zones.size()));
    }

}
