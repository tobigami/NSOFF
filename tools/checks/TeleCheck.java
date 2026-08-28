import com.nsoz.effect.EffectDataManager;
import com.nsoz.effect.EffectTemplateManager;
import com.nsoz.item.ItemManager;
import com.nsoz.map.Map;
import com.nsoz.map.MapManager;
import com.nsoz.mob.MobManager;
import com.nsoz.npc.NpcManager;
import com.nsoz.server.Config;
import com.nsoz.server.GameData;
import com.nsoz.store.StoreManager;
import com.nsoz.db.jdbc.DbManager;
import com.nsoz.util.NinjaUtils;

/**
 * Kiem cac diem den truoc khi nap: map co ton tai khong, co khu de vao khong, va diem vao mac
 * dinh nam o dau. 62/179 map khong co khu tinh nao -- nem nguoi vao do la ho dung ngoai moi khu.
 */
public class TeleCheck {
    static final int[] MAPS = { 22, 0, 60, 21, 24, 45 };

    public static void main(String[] a) throws Exception {
        if (!Config.getInstance().load() || !DbManager.start()) {
            System.out.println("!! khong nap duoc cau hinh/DB");
            return;
        }
        // Phai theo dung thu tu cua Server.init(): MapManager.init() dung quai va npc cho tung
        // khu, thieu mot manh la no nem ngoai le va BO DO ngay tu map dau tien -- luc do find()
        // tra ve null cho gan het map, nhin nhu "khong ton tai".
        NpcManager.getInstance().load();
        MobManager.getInstance().load();
        MapManager.getInstance().load();
        EffectTemplateManager.getInstance().init();
        EffectDataManager.getInstance().load();
        ItemManager.getInstance().load();
        GameData.getInstance().init();
        StoreManager.getInstance().init();
        StoreManager.getInstance().load();
        ItemManager.getInstance().init();
        MapManager.getInstance().init();
        for (int id : MAPS) {
            Map m = MapManager.getInstance().find(id);
            if (m == null) {
                System.out.printf("map %-4d KHONG TON TAI%n", id);
                continue;
            }
            int zone = -2;
            try {
                zone = NinjaUtils.randomZoneId(id);
            } catch (Exception ex) {
                zone = -2;
            }
            short[] xy = NinjaUtils.getFirstPosition((short) id);
            System.out.printf("map %-4d ten=%-24s zone=%-3d diem vao=(%d,%d)%n",
                    id, m.tilemap.name, zone, xy[0], xy[1]);
        }
        System.exit(0);
    }
}
