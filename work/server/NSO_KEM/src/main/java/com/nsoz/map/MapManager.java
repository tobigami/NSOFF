package com.nsoz.map;

import com.nsoz.constants.NpcName;
import com.nsoz.constants.SQLStatement;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.event.Event;
import com.nsoz.lib.ParseData;
import com.nsoz.map.world.CandyBattlefield;
import com.nsoz.map.world.SevenBeasts;
import com.nsoz.map.zones.TalentShow;
import com.nsoz.map.zones.Zone;
import com.nsoz.mob.Mob;
import com.nsoz.mob.MobPosition;
import com.nsoz.model.Char;
import com.nsoz.npc.Npc;
import com.nsoz.npc.NpcFactory;
import com.nsoz.server.Config;
import com.nsoz.server.Events;
import com.nsoz.util.Log;
import com.nsoz.util.ProgressBar;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MapManager {

    private static final MapManager instance = new MapManager();
    @Getter
    private final List<TileMap> tileMaps = new ArrayList<>();
    @Getter
    private final ArrayList<Map> maps = new ArrayList<>();
    private final ArrayList<SevenBeasts> sevenBeastses = new ArrayList<>();
    public War normalWar ; // Mảng để lưu trữ 4 đối tượng War khác nhau
    public Vithu vithu;
    public War talentWar;
    public TalentShow talentShow;
    @Getter
    @Setter
    private CandyBattlefield candyBattlefield;

    public static MapManager getInstance() {
        return instance;
    }

    public void addSevenBeasts(SevenBeasts sevenBeasts) {
        synchronized (sevenBeastses) {
            sevenBeastses.add(sevenBeasts);
        }
    }

    public SevenBeasts findSevenBeasts(int id) {
        synchronized (sevenBeastses) {
            for (SevenBeasts sevenBeasts : sevenBeastses) {
                if (sevenBeasts.getId() == id) {
                    return sevenBeasts;
                }
            }
        }
        return null;
    }

    public void removeSevenBeasts(SevenBeasts sevenBeasts) {
        synchronized (sevenBeastses) {
            sevenBeastses.remove(sevenBeasts);
        }
    }

    public boolean load() {
        try (Connection conn = DbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQLStatement.GET_ALL_MAP, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet resultSet = stmt.executeQuery()) {

            resultSet.last();
            ProgressBar pb = new ProgressBar("Loading Map", resultSet.getRow());
            resultSet.beforeFirst();

            while (resultSet.next()) {
                try {
                    TileMap map = parseMap(resultSet);
                    tileMaps.add(map);
                    pb.setExtraMessage(map.name + " finished!");
                    pb.step();
                } catch (Exception e) {
                    pb.setExtraMessage(e.getMessage());
                    pb.reportError();
                     Log.logException("Loading map from Db err: ", MapService.class,e);
                    return false;
                }
            }
            pb.setExtraMessage("Finished!");
            pb.reportSuccess();
        } catch (Exception e) {
            Log.logException("Load map lỗi", MapManager.class,e);
            return false;
        }
        return true;
    }

    private TileMap parseMap(ResultSet resultSet) throws SQLException {
        TileMap map = new TileMap();
        map.id = resultSet.getInt("id");
        map.name = resultSet.getString("name");
        map.tileId = resultSet.getByte("tileId");
        map.bgId = resultSet.getByte("bgId");
        map.type = resultSet.getByte("type");
        map.zoneNumber = resultSet.getByte("zone_number");
        map.npcs = parseNpcs(resultSet.getString("npc"));
        map.monsterCoordinates = parseMonsters(resultSet.getString("monster"));
        map.waypoints = parseWaypoints(resultSet.getString("waypoint"));
        map.locationStand = parseLocationStand(resultSet.getString("locationStand"));
        map.items = parseItems(resultSet.getString("item"));
        map.vItemTreeBehind = parseItemTrees(resultSet.getString("behind"));
        map.vItemTreeBetwen = parseItemTrees(resultSet.getString("betwen"));
        map.vItemTreeFront = parseItemTrees(resultSet.getString("front"));
        map.loadMapFromResource();
        return map;
    }

    private List<Npc> parseNpcs(String npcJson) {
        List<Npc> npcs = new ArrayList<>();
        JSONArray jArr = (JSONArray) JSONValue.parse(npcJson);
        for (Object obj : jArr) {
            JSONObject jsonObj = (JSONObject) obj;
            int status = ((Long) jsonObj.get("status")).byteValue();
            int cx = ((Long) jsonObj.get("x")).shortValue();
            int cy = ((Long) jsonObj.get("y")).shortValue();
            int npcTemplateId = ((Long) jsonObj.get("templateId")).shortValue();

            if (isNpcRelevant(npcTemplateId)) {
                npcs.add(NpcFactory.getInstance().newNpc(npcs.size(), npcTemplateId, cx, cy, status));
            }
        }
        return npcs;
    }

    private boolean isNpcRelevant(int npcTemplateId) {
        return !(Events.event != Events.TET && npcTemplateId == 41) &&
                !(Events.event != Events.SUMMER && npcTemplateId == 42) &&
                !(Event.isTrungThu() && npcTemplateId == NpcName.LONG_DEN_2);
    }

    private List<MobPosition> parseMonsters(String monsterJson) {
        List<MobPosition> monsters = new ArrayList<>();
        JSONArray jArr = (JSONArray) JSONValue.parse(monsterJson);
        for (Object obj : jArr) {
            JSONObject jsonObj = (JSONObject) obj;
            short templateId = ((Long) jsonObj.get("templateId")).shortValue();
            short x = ((Long) jsonObj.get("x")).shortValue();
            short y = ((Long) jsonObj.get("y")).shortValue();
            boolean boss = (boolean) jsonObj.get("boss");

            if (Events.event != Events.TET && templateId == 225) {
                continue;
            }
            monsters.add(new MobPosition(templateId, x, y, boss));
        }
        return monsters;
    }

    private List<Waypoint> parseWaypoints(String waypointJson) {
        List<Waypoint> waypoints = new ArrayList<>();
        JSONArray jArr = (JSONArray) JSONValue.parse(waypointJson);
        for (Object obj : jArr) {
            ParseData p = new ParseData((JSONObject) obj);
            Waypoint waypoint = new Waypoint();
            waypoint.next = p.getShort("next");
            waypoint.minX = p.getShort("minX");
            waypoint.minY = p.getShort("minY");
            waypoint.maxX = p.getShort("maxX");
            waypoint.maxY = p.getShort("maxY");
            waypoint.x = p.getShort("x");
            waypoint.y = p.getShort("y");
            waypoints.add(waypoint);
        }
        return waypoints;
    }

    private List<int[]> parseLocationStand(String locationStandJson) {
        List<int[]> locationStand = new ArrayList<>();
        JSONArray jArr = (JSONArray) JSONValue.parse(locationStandJson);
        for (Object obj : jArr) {
            JSONArray arr = (JSONArray) obj;
            int x = Integer.parseInt(arr.get(0).toString());
            int y = Integer.parseInt(arr.get(1).toString());
            locationStand.add(new int[]{x, y});
        }
        return locationStand;
    }

    private List<Integer> parseItems(String itemsJson) {
        List<Integer> items = new ArrayList<>();
        JSONArray jArr = (JSONArray) JSONValue.parse(itemsJson);
        for (Object obj : jArr) {
            int idItem = Integer.parseInt(obj.toString());
            items.add(idItem);
        }
        return items;
    }

    private List<ItemTree> parseItemTrees(String itemTreesJson) {
        List<ItemTree> itemTrees = new ArrayList<>();
        JSONArray jArr = (JSONArray) JSONValue.parse(itemTreesJson);
        for (Object obj : jArr) {
            JSONObject jsonObj = (JSONObject) obj;
            int idItem = Integer.parseInt(jsonObj.get("id").toString());
            int x = Integer.parseInt(jsonObj.get("x").toString());
            int y = Integer.parseInt(jsonObj.get("y").toString());
            ItemTree iTree = new ItemTree(x, y);
            iTree.idTree = idItem;
            itemTrees.add(iTree);
        }
        return itemTrees;
    }

    public void add(Map map) {
        maps.add(map);
    }

    public void remove(Map map) {
        maps.remove(map);
    }

    public Map find(int id) {
        for (Map map : maps) {
            if (map.id == id) {
                return map;
            }
        }
        return null;
    }

    public TileMap getTileMap(int index) {
        return tileMaps.get(index);
    }

    public void init() {
        ProgressBar pb = new ProgressBar("Init Map", tileMaps.size());
        for (TileMap tile : tileMaps) {
            try {
                pb.setExtraMessage("Đang tạo " + tile.name);
                add(new Map((short) tile.id));
                pb.setExtraMessage("Tạo hoàn tất " + tile.name);
                pb.step();
            } catch (Exception e) {
                pb.setExtraMessage(e.getMessage());
                pb.reportError();
                return;
            }
        }
        pb.setExtraMessage("Finished!");
        pb.reportSuccess();
    }

    public void joinZone(Char pl, int mapId, int zoneId) {
        Map m = find(mapId);
        if (m != null) {
            m.joinZone(pl, zoneId);
        }
    }

    public void outZone(Char pl) {
        if (pl != null && pl.zone != null) {
            Zone z = pl.zone;
            z.out(pl);
        }
    }

    public void close() {
        Map.running = false;
        synchronized (maps) {
            for (Map map : maps) {
                map.close();
            }
        }
    }

}
